package supa.duap.match

import supa.duap.match.model.Game
import supa.duap.match.model.GameType
import supa.duap.match.model.Player
import supa.duap.request

class MatchMakingRepository(private val api : MatchMakingApi) {

    suspend fun createPlayer(
        id : ULong
    ) = request<Player?> { api.createPlayer(id) }

    suspend fun getPlayer(
        id : ULong
    ) = request<Player?> { api.getPlayer(id) }

    suspend fun createGame(
        gameType : GameType,
        player1Id : ULong,
        player2Id : ULong
    ) = request<Game?> { api.createGame(gameType, player1Id, player2Id) }
}
