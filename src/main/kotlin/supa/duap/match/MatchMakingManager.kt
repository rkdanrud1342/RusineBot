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
import kotlin.math.abs

class MatchMakingManager(private val repo : MatchMakingRepository) {
    private val matchMakingScope : CoroutineScope = CoroutineScope(BaseCoroutine.default)

    private val casualGamePool : MutableMap<Player, MatchArgs> = mutableMapOf()
    private val rankGamePool : MutableMap<Player, MatchArgs> = mutableMapOf()

    private val casualGameChannel : Channel<Pair<Player, MatchArgs>> = Channel()
    private val rankGameChannel : Channel<Pair<Player, MatchArgs>> = Channel()

    private val awaitingJobs : MutableMap<Player, Job> = mutableMapOf()

    private var onGameCreated : (suspend (Game?) -> Unit)? = null
    private var onGameNotCreated : (suspend () -> Unit)? = null

    init {
        matchMakingScope.launch {
            for (e1 in casualGameChannel) {
                if (casualGamePool[e1.first] == null) {
                    continue
                }

                var e2 : Map.Entry<Player, MatchArgs>? = null

                for (e in casualGamePool) {
                    if (e1.first == e.key || !checkGameArgs(e1, e.toPair())) {
                        // not matched. compare with next player.
                        continue
                    }

                    // matched. init player2 info and break loop.
                    e2 = e
                    break
                }

                // when there was no player that can be matched.
                e2?.let {
                    val player1 = e1.first
                    val player2 = it.key

                    awaitingJobs.remove(player1)?.cancel()
                    awaitingJobs.remove(player2)?.cancel()

                    casualGamePool.remove(player1)
                    casualGamePool.remove(player2)

                    val game = makeGame(e1, it.toPair(), CASUAL)
                    onGameCreated?.invoke(game)
                } ?: run { onNoMatched(e1, CASUAL) }
            }
        }

        matchMakingScope.launch {
            for (e1 in rankGameChannel) {
                if (rankGamePool[e1.first] == null) {
                    continue
                }

                var e2 : Map.Entry<Player, MatchArgs>? = null

                for (e in rankGamePool) {
                    if (e1.first == e.key || !checkGameArgs(e1, e.toPair())) {
                        // not matched. compare with next player.
                        continue
                    }

                    // matched. init player2 info and break loop.
                    e2 = e
                    break
                }

                e2?.let {
                    val player1 = e1.first
                    val player2 = it.key

                    awaitingJobs.remove(player1)?.cancel()
                    awaitingJobs.remove(player2)?.cancel()

                    rankGamePool.remove(player1)
                    rankGamePool.remove(player2)

                    val game = makeGame(e1, it.toPair(), RANK)
                    onGameCreated?.invoke(game)
                } ?: run {
                    // when there was no player that can be matched.
                    onNoMatched(e1, RANK)
                }
            }
        }
    }

    private suspend fun onNoMatched(p : Pair<Player, MatchArgs>, gameType : GameType) {
        if (p.second.phase >= 5) {
            onGameNotCreated?.invoke()
            return
        }

        awaitingJobs[p.first] = matchMakingScope.launch {
            delay(30000L)
            when (gameType) {
                RANK -> rankGameChannel
                CASUAL -> casualGameChannel
            }.send(p)
        }
    }

    fun setOnGameCreatedListener(listener : (suspend (Game?) -> Unit)?) {
        onGameCreated = listener
    }

    fun setOnGameNotCreatedListener(listener : (suspend () -> Unit)?) {
        onGameNotCreated = listener
    }

    fun addQueue(player : Player, matchArgs : MatchArgs, gameType : GameType) {
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

    suspend fun createProfile(id : ULong) : Player? =
        repo.createPlayer(id)
            .take(1)
            .catch { emit(null) }
            .single()

    suspend fun getProfile(id : ULong) : Player? =
        repo.getPlayer(id)
            .take(1)
            .catch { emit(null) }
            .single()

    private suspend fun makeGame(
        p1 : Pair<Player, MatchArgs>,
        p2 : Pair<Player, MatchArgs>,
        type : GameType
    ) : Game? {
        if (!checkGameArgs(p1, p2)) {
            return null
        }

        return repo.createGame(type, p1.first.id, p2.first.id)
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
}
