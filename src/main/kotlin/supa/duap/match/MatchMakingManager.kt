package supa.duap.match

import co.touchlab.stately.collections.ConcurrentMutableList
import kotlinx.coroutines.CoroutineScope
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

    private val casualGamePool : ConcurrentMutableList<Pair<Player, MatchArgs>> = ConcurrentMutableList()
    private val rankGamePool : ConcurrentMutableList<Pair<Player, MatchArgs>> = ConcurrentMutableList()

    private var onGameCreated : (suspend (Game?) -> Unit)? = null

    init {
        matchMakingScope.launch {
            while (true) {
                if (casualGamePool.size <= 1) {
                    delay(1000L)
                    continue
                }

                casualGamePool.forEach player1@ { p1Info ->
                    casualGamePool.forEach player2@ { p2Info ->
                        if (p1Info == p2Info) {
                            return@player2
                        }

                        if (!checkGameArgs(p1Info, p2Info)) {
                            return@player2
                        }

                        // make game
                        val game = makeGame(p1Info, p2Info, CASUAL)
                        onGameCreated?.invoke(game)
                        casualGamePool.remove(p1Info)
                        casualGamePool.remove(p2Info)
                        return@player1
                    }

                    if (p1Info.second.phase < 5)
                        p1Info.second.phase++
                }
            }
        }

        matchMakingScope.launch {
            rankGamePool.forEach player1@ { p1Info ->
                rankGamePool.forEach player2@ { p2Info ->
                    if (p1Info == p2Info) {
                        return@player2
                    }

                    if (!checkGameArgs(p1Info, p2Info)) {
                        return@player2
                    }

                    // make game
                    makeGame(p1Info, p2Info, RANK)
                    return@player1
                }

                if (p1Info.second.phase < 5)
                    p1Info.second.phase++
            }
        }
    }

    fun setOnGameCreatedListener(listener : (suspend (Game?) -> Unit)?) {
        onGameCreated = listener
    }

    fun addQueue(player : Player, matchArgs : MatchArgs, gameType : GameType) =
        when(gameType) {
            RANK -> rankGamePool
            CASUAL -> casualGamePool
        }
            .add(player to matchArgs)

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
