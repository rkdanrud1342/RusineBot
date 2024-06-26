package supa.duap.match

import supa.duap.api.request
import supa.duap.Grade

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

    suspend fun getRunningMatch(
        playerId : Long
    ) = request { api.getRunningMatch(playerId) }

    suspend fun cancelRunningMatch(
        playerId : Long
    ) = request { api.cancelRunningMatch(playerId) }

    suspend fun createMatch(
        typeCode : Int,
        player1Id : Long,
        player2Id : Long,
    ) = request { api.createMatch(typeCode, player1Id, player2Id) }

    suspend fun registerMatchScore(
        playerId : Long,
        p1Score : Int,
        p2Score : Int
    ) = request { api.registerMatchScore(playerId, p1Score, p2Score) }

    suspend fun getPlayerRanking(
        playerId : Long
    ) = request { api.getPlayerRanking(playerId) }

    suspend fun setPlayerGrade(
        playerId : Long,
        grade : Int
    ) = request { api.setPlayerGrade(playerId, grade) }
}
