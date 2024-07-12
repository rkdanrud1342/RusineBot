package supa.duap.match

import dev.kord.core.Kord
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import supa.duap.BaseCoroutine
import supa.duap.Grade
import supa.duap.match.model.*
import supa.duap.match.model.MatchType.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class MatchMakingManager(
    private val kord : Kord,
    private val repo : MatchMakingRepository
) {
    private val logger = LoggerFactory.getLogger(MatchMakingManager::class.java)

    private val casualMatchQueue : MutableMap<Player, MatchArgs> = ConcurrentHashMap()
    private val rankedMatchQueue : MutableMap<Player, MatchArgs> = ConcurrentHashMap()

    private val casualMatchChannel : Channel<Pair<Player, MatchArgs>> = Channel()
    private val rankedMatchChannel : Channel<Pair<Player, MatchArgs>> = Channel()

    private val awaitingJobs : MutableMap<Player, Job> = mutableMapOf()

    private val matchResultListeners = mutableMapOf<Player, suspend (RunningMatch?, Boolean) -> Unit>()
    private val notMatchedAtOnceListeners = mutableMapOf<Player, suspend () -> Unit>()

    init {
        makeChannel(CASUAL)
        makeChannel(RANK)
    }

    private suspend fun onNoMatched(p : Pair<Player, MatchArgs>, matchType : MatchType) {
        notMatchedAtOnceListeners.remove(p.first)?.invoke()

        awaitingJobs[p.first] = kord.launch(BaseCoroutine.default) {
            delay(30000L)

            p.second.phase++

            if (p.second.isAwaitOver()) {
                val player1 = p.first
                when (matchType) {
                    RANK -> rankedMatchQueue
                    CASUAL -> casualMatchQueue
                }.remove(player1)
                matchResultListeners.remove(player1)?.invoke(null, true)
                return@launch
            }

            when (matchType) {
                RANK -> rankedMatchChannel
                CASUAL -> casualMatchChannel
            }.send(p)
        }
    }

    fun addOnMatchCreateListener(key : Player, listener : suspend (RunningMatch?, Boolean) -> Unit) {
        matchResultListeners[key] = listener
    }

    fun addOnNotMatchedAtOnceListener(key : Player, listener : suspend () -> Unit) {
        notMatchedAtOnceListeners[key] = listener
    }

    fun enqueue(player : Player, matchArgs : MatchArgs, matchType : MatchType) {
        kord.launch(BaseCoroutine.default) {
            when (matchType) {
                CASUAL -> {
                    casualMatchQueue[player] = matchArgs
                    casualMatchChannel.send(player to matchArgs)
                }

                RANK -> {
                    rankedMatchQueue[player] = matchArgs
                    rankedMatchChannel.send(player to matchArgs)
                }
            }
        }
    }

    fun dequeue(player : Player) : Boolean =
        casualMatchQueue.remove(player) != null ||
                rankedMatchQueue.remove(player) != null ||
                awaitingJobs[player]?.also { it.cancel() } != null

    fun isRegistered(player : Player) : Boolean =
        casualMatchQueue[player] != null || rankedMatchQueue[player] != null

    suspend fun createProfile(id : Long, nickname : String?, grade : Grade) = repo.createPlayer(id, nickname, grade)
    suspend fun getProfile(id : Long) = repo.getProfile(id)
    suspend fun getPlayer(id : Long) = repo.getPlayer(id)
    suspend fun getRunningMatch(playerId : Long) = repo.getRunningMatch(playerId)
    suspend fun cancelRunningMatch(playerId : Long) = repo.cancelRunningMatch(playerId)
    suspend fun registerMatchScore(playerId : Long, p1Score : Int, p2Score : Int) = repo.registerMatchScore(playerId, p1Score, p2Score)
    suspend fun getPlayerRanking(playerId : Long) = repo.getPlayerRanking(playerId)
    suspend fun setPlayerGrade(playerId: Long, grade : Int) = repo.setPlayerGrade(playerId, grade)

    private suspend fun makeMatch(
        matchType : MatchType,
        player1 : Player,
        player2 : Player
    ) = when (matchType) {
        CASUAL -> RunningMatch(
            id = -1,
            player1 = player1.toDummyProfile(),
            player2 = player2.toDummyProfile(),
            matchType = CASUAL
        )

        RANK -> repo.createMatch(RANK.typeCode, player1.id, player2.id)
            .take(1)
            .catch { emit(null) }
            .single()
    }

    private fun canBothPlayerBeMatched(
        p1 : Pair<Player, MatchArgs>,
        p2 : Pair<Player, MatchArgs>
    ) : Boolean {
        val p1Grade = p1.first.grade
        val p2Grade = p2.first.grade

        val p1AvailableRange = p1.second.rankAvailableRange
        val p2AvailableRange = p2.second.rankAvailableRange

        val diff = abs(p1Grade - p2Grade)

        return (p1AvailableRange < 0 || diff <= p1AvailableRange) && (p2AvailableRange < 0 || diff <= p2AvailableRange)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun makeChannel(matchType : MatchType) {
        kord.launch(BaseCoroutine.default) {
            val (channel, pool) = when (matchType) {
                CASUAL -> casualMatchChannel to casualMatchQueue
                RANK -> rankedMatchChannel to rankedMatchQueue
            }

            yield()

            while (!channel.isClosedForReceive) {
                channel.receiveAsFlow()
                    .onEach { player1MatchArgsPair ->
                        var player2MatchArgsPair : Pair<Player, MatchArgs>? = null

                        for (e in pool) {
                            if (player1MatchArgsPair.first == e.key) {
                                // same player
                                continue
                            }

                            if (!canBothPlayerBeMatched(player1MatchArgsPair, e.toPair())) {
                                // cannot be matched
                                continue
                            }

                            // matched. init player2 info and break loop.
                            player2MatchArgsPair = e.toPair()
                            break
                        }

                        // when matched
                        player2MatchArgsPair?.let {
                            val player1 = player1MatchArgsPair.first
                            val player2 = player2MatchArgsPair.first

                            val match = makeMatch(matchType, player1, player2) ?: return@let null

                            awaitingJobs.remove(player1)?.takeIf { !it.isCancelled }?.cancel()
                            awaitingJobs.remove(player2)?.takeIf { !it.isCancelled }?.cancel()

                            pool.remove(player1)
                            pool.remove(player2)

                            matchResultListeners.remove(player1)?.invoke(match, true)
                            matchResultListeners.remove(player2)?.invoke(match, false)
                        } ?: run {
                            // when no matched
                            onNoMatched(player1MatchArgsPair, matchType)
                        }
                    }
                    .catch { e ->
                        logger.error("아니 도대체 무슨 익셉션인거야??")
                        logger.error(e)
                    }
                    .flowOn(BaseCoroutine.default)
                    .collect()
            }
        }
    }

    private fun Player.toDummyProfile() = PlayerProfile(
        id = id,
        name = name,
        casualWinCount = 0,
        casualLoseCount = 0,
        rankWinCount = 0,
        rankLoseCount = 0,
        eloScore = 0
    )
}
