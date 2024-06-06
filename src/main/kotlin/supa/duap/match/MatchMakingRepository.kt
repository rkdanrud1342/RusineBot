package supa.duap.match

import supa.duap.api.request
import supa.duap.match.model.Grade

class MatchMakingRepository(private val api : MatchMakingApi) {

    suspend fun createPlayer(
        id : Long,
        name : String?,
        grade : Grade
    ) = request { api.registerUser(id, name.orEmpty(), grade.ordinal) }

    suspend fun getPlayer(
        id : Long
    ) = request { api.getPlayer(id) }

    suspend fun getProfile(
        id : Long
    ) = request { api.getProfile(id) }

    suspend fun getRunningGame(
        playerId : Long
    ) = request { api.getRunningGame(playerId) }

    suspend fun createGame(
        typeCode : Int,
        player1Id : Long,
        player2Id : Long,
    ) = request { api.createGame(typeCode, player1Id, player2Id) }

    suspend fun registerGameScore(
        playerId : Long,
        p1Score : Int,
        p2Score : Int
    ) = request { api.registerGameScore(playerId, p1Score, p2Score) }

    suspend fun getPlayerRanking(
        playerId: Long
    ) = request { api.getPlayerRanking(playerId) }
}
