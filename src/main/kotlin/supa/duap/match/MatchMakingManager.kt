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

    private val matchResultListeners = mutableMapOf<Player, (suspend (Game?) -> Unit)?>()

    private val games : MutableMap<Long, Game> = ConcurrentHashMap()

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

    fun addOnGameCreateListener(key : Player, listener : (suspend (Game?) -> Unit)?) {
        matchResultListeners[key] = listener
    }

    fun enqueue(player : Player, matchArgs : MatchArgs, gameType : GameType) : Boolean {
        if (isRegistered(player)) {
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

    fun dequeue(player : Player) {
        casualGamePool.remove(player)
        rankGamePool.remove(player)
        awaitingJobs[player]?.cancel()
    }

    suspend fun createProfile(id : Long, nickname : String?, grade : Grade) = repo.createPlayer(id, nickname, grade)
    suspend fun getProfile(id : Long) = repo.getProfile(id)
    suspend fun getPlayer(id : Long) = repo.getPlayer(id)

    suspend fun registerGameScore(id : Long, p1Score : Int, p2Score : Int) : Flow<Pair<Game, Game?>> {
        val game = games[id] ?: throw Exception("진행중인 게임이 없습니다.")

        return repo.registerGameScore(game, p1Score, p2Score)
                .map {
                    games.remove(game.player1.id)
                    games.remove(game.player2.id)

                    game to it
                }
    }

    private suspend fun makeGame(
        p1 : Pair<Player, MatchArgs>,
        p2 : Pair<Player, MatchArgs>,
        type : GameType
    ) : Game? = when (type) {
        CASUAL -> repo.createCasualGame(p1.first.id, p2.first.id)
        RANK -> repo.createRankGame(player1Id = p1.first.id, player2Id = p2.first.id)
    }
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

    private fun isRegistered(player : Player) =
        casualGamePool[player] != null || rankGamePool[player] != null

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

                    val game = makeGame(player1MatchArgsPair, it, gameType) ?: return@let null

                    games[player1.id] = game
                    games[player2.id] = game

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
