package supa.duap.match

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import supa.duap.BaseCoroutine
import supa.duap.match.model.Game
import supa.duap.match.model.GameType
import supa.duap.match.model.GameType.*
import supa.duap.match.model.Player
import supa.duap.match.model.MatchArgs
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class MatchMakingManager(private val repo : MatchMakingRepository) {
    private val matchMakingScope : CoroutineScope = CoroutineScope(BaseCoroutine.default)

    private val casualGamePool : MutableMap<Player, MatchArgs> = ConcurrentHashMap()
    private val rankGamePool : MutableMap<Player, MatchArgs> = ConcurrentHashMap()

    private val casualGameChannel : Channel<Pair<Player, MatchArgs>> = Channel()
    private val rankGameChannel : Channel<Pair<Player, MatchArgs>> = Channel()

    private val awaitingJobs : MutableMap<Player, Job> = mutableMapOf()

    private val matchResultListener = mutableMapOf<Player, (suspend (Game?) -> Unit)?>()

    init {
        makeChannel(CASUAL)
        makeChannel(RANK)
    }

    private suspend fun onNoMatched(p : Pair<Player, MatchArgs>, gameType : GameType) {
        if (p.second.phase >= 5) {
            val player1 = p.first
            awaitingJobs.remove(player1)?.takeIf { it.isActive }?.cancel()
            when (gameType) {
                RANK -> rankGamePool
                CASUAL -> casualGamePool
            }.remove(player1)
            matchResultListener.remove(player1)?.invoke(null)
            return
        }

        awaitingJobs[p.first] = matchMakingScope.launch {
            p.second.phase++
            delay(30000L)
            when (gameType) {
                RANK -> rankGameChannel
                CASUAL -> casualGameChannel
            }.send(p)
        }
    }

    fun addOnGameCreateListener(key : Player, listener : (suspend (Game?) -> Unit)?) {
        matchResultListener[key] = listener
    }

    fun addQueue(player : Player, matchArgs : MatchArgs, gameType : GameType) : Boolean {
        if (isRegistered(player, matchArgs, gameType)) {
            return false
        }

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

        return true
    }

    suspend fun createProfile(id : Long, nickname : String?) = repo.createPlayer(id, nickname)
    suspend fun getProfile(id : Long) = repo.getPlayer(id)

    private suspend fun makeGame(
        p1 : Pair<Player, MatchArgs>,
        p2 : Pair<Player, MatchArgs>,
        type : GameType
    ) : Game? {
        if (!checkGameArgs(p1, p2)) {
            return null
        }

        return when (type) {
            CASUAL -> repo.createCasualGame(p1.first.id, p2.first.id)
            RANK -> repo.createRankGame(p1.first.id, p2.first.id)
        }
            .take(1)
            .catch { emit(null) }
            .single()
    }

    private fun checkGameArgs(
        p1 : Pair<Player, MatchArgs>,
        p2 : Pair<Player, MatchArgs>
    ) : Boolean {
        val p1Grade = p1.first.grade
        val p2Grade = p2.first.grade

        val p1Phase = p1.second.phase
        val p2Phase = p2.second.phase

        val diff = abs(p1Grade - p2Grade)

        return (p1Phase < 0 || diff <= p1Phase) && (p2Phase < 0 || diff <= p2Phase)
    }

    private fun isRegistered(player : Player, matchArgs : MatchArgs, gameType : GameType) =
        matchArgs == when (gameType) {
            CASUAL -> casualGamePool[player]
            RANK -> rankGamePool[player]
        }

    private fun makeChannel(gameType : GameType) {
        val (channel, pool) = when (gameType) {
            CASUAL -> casualGameChannel to casualGamePool
            RANK -> rankGameChannel to rankGamePool
        }

        matchMakingScope.launch {
            for (e1 in channel) {
                if (pool[e1.first] == null) {
                    continue
                }

                var e2 : Map.Entry<Player, MatchArgs>? = null

                for (e in pool) {
                    if (e1.first == e.key || !checkGameArgs(e1, e.toPair())) {
                        // not matched. compare with next player.
                        continue
                    }

                    // matched. init player2 info and break loop.
                    e2 = e
                    break
                }

                // when matched
                e2?.let {
                    val player1 = e1.first
                    val player2 = it.key

                    awaitingJobs.remove(player1)?.cancel()
                    awaitingJobs.remove(player2)?.cancel()

                    pool.remove(player1)
                    pool.remove(player2)

                    val game = makeGame(e1, it.toPair(), gameType)
                    matchResultListener.remove(player1)?.invoke(game)
                    matchResultListener.remove(player2)
                } ?: run {
                    // when no matched
                    onNoMatched(e1, gameType)
                }
            }
        }
    }
}
