package supa.duap.match

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import supa.duap.BaseCoroutine
import supa.duap.match.model.*
import supa.duap.match.model.GameType.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class MatchMakingManager(private val repo : MatchMakingRepository) {
    private val matchMakingScope : CoroutineScope = CoroutineScope(BaseCoroutine.default)

    private val casualGamePool : MutableMap<Player, MatchArgs> = ConcurrentHashMap()
    private val rankGamePool : MutableMap<Player, MatchArgs> = ConcurrentHashMap()

    private val casualGameChannel : Channel<Pair<Player, MatchArgs>> = Channel()
    private val rankGameChannel : Channel<Pair<Player, MatchArgs>> = Channel()

    private val awaitingJobs : MutableMap<Player, Job> = mutableMapOf()

    private val matchResultListeners = mutableMapOf<Player, (suspend (RunningGame?) -> Unit)?>()

    init {
        makeChannel(CASUAL)
        makeChannel(RANK)
    }

    private suspend fun onNoMatched(p : Pair<Player, MatchArgs>, gameType : GameType) {
        awaitingJobs[p.first] = matchMakingScope.launch {
            delay(30000L)

            p.second.phase++

            if (p.second.isAwaitOver()) {
                val player1 = p.first
                when (gameType) {
                    RANK -> rankGamePool
                    CASUAL -> casualGamePool
                }.remove(player1)
                matchResultListeners.remove(player1)?.invoke(null)
                return@launch
            }

            when (gameType) {
                RANK -> rankGameChannel
                CASUAL -> casualGameChannel
            }.send(p)
        }
    }

    fun addOnGameCreateListener(key : Player, listener : (suspend (RunningGame?) -> Unit)?) {
        matchResultListeners[key] = listener
    }

    fun enqueue(player : Player, matchArgs : MatchArgs, gameType : GameType) {
        matchMakingScope.launch {
            when (gameType) {
                CASUAL -> {
                    casualGamePool[player] = matchArgs
                    casualGameChannel.send(player to matchArgs)
                }

                RANK -> {
                    rankGamePool[player] = matchArgs
                    rankGameChannel.send(player to matchArgs)
                }
            }
        }
    }

    fun dequeue(player : Player) : Boolean =
        casualGamePool.remove(player) != null ||
                rankGamePool.remove(player) != null ||
                awaitingJobs[player]?.also { it.cancel() } != null

    fun isRegistered(player : Player) : Boolean =
        casualGamePool[player] != null || rankGamePool[player] != null

    suspend fun createProfile(id : Long, nickname : String?, grade : Grade) = repo.createPlayer(id, nickname, grade)
    suspend fun getProfile(id : Long) = repo.getProfile(id)
    suspend fun getPlayer(id : Long) = repo.getPlayer(id)
    suspend fun getRunningGame(playerId : Long) = repo.getRunningGame(playerId)
    suspend fun registerGameScore(playerId : Long, p1Score : Int, p2Score : Int) = repo.registerGameScore(playerId, p1Score, p2Score)
    suspend fun getPlayerRanking(playerId : Long) = repo.getPlayerRanking(playerId)

    private suspend fun makeGame(
        type : GameType,
        player1 : Player,
        player2 : Player
    ) = repo.createGame(type.name, player1.id, player2.id)
        .take(1)
        .catch { emit(null) }
        .single()

    private fun checkGameArgs(
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

    private fun makeChannel(gameType : GameType) {
        val (channel, pool) = when (gameType) {
            CASUAL -> casualGameChannel to casualGamePool
            RANK -> rankGameChannel to rankGamePool
        }

        matchMakingScope.launch {
            for (player1MatchArgsPair in channel) {
                if (pool[player1MatchArgsPair.first] == null) {
                    continue
                }

                var player2MatchArgsPair : Pair<Player, MatchArgs>? = null

                for (e in pool) {
                    if (player1MatchArgsPair.first == e.key || !checkGameArgs(player1MatchArgsPair, e.toPair())) {
                        // not matched. compare with next player.
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

                    awaitingJobs.remove(player1)?.cancel()
                    awaitingJobs.remove(player2)?.cancel()

                    pool.remove(player1)
                    pool.remove(player2)

                    val game = makeGame(gameType, player1, player2) ?: return@let null

                    matchResultListeners.remove(player1)?.invoke(game)
                    matchResultListeners.remove(player2)
                } ?: run {
                    // when no matched
                    onNoMatched(player1MatchArgsPair, gameType)
                }
            }
        }
    }
}
