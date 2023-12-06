package supa.duap.match

import io.ktor.client.statement.*
import supa.duap.match.model.GameType

interface MatchMakingApi {
    suspend fun createPlayer(
        playerId : ULong
    ) : HttpResponse

    suspend fun getPlayer(
        playerId : ULong
    ): HttpResponse

    suspend fun createGame(
        gameType : GameType,
        player1Id : ULong,
        player2Id : ULong
    ) : HttpResponse
}