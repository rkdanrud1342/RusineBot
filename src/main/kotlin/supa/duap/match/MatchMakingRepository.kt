package supa.duap.match

import supa.duap.api.request
import supa.duap.match.model.Game
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

    suspend fun createCasualGame(
        player1Id : Long,
        player2Id : Long
    ) = request { api.createCasualGame(player1Id, player2Id) }

    suspend fun createRankGame(
        player1Id : Long,
        player2Id : Long,
    ) = request { api.createRankGame(player1Id, player2Id) }

    suspend fun registerGameScore(
        game : Game,
        p1Score : Int,
        p2Score : Int
    ) = request {
        val apiFunc = when (game) {
            is Game.CasualGame -> api::registerCasualGameScore
            is Game.RankGame -> api::registerRankGameScore
        }

        apiFunc(
            game.id,
            p1Score,
            p2Score
        )
    }
}
