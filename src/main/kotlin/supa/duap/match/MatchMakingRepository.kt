package supa.duap.match

import supa.duap.api.request

class MatchMakingRepository(private val api : MatchMakingApi) {

    suspend fun createPlayer(
        id : Long,
        name : String?
    ) = request { api.registerUser(id, name.orEmpty()) }

    suspend fun getPlayer(
        id : Long
    ) = request { api.getPlayer(id) }

    suspend fun createCasualGame(
        player1Id : Long,
        player2Id : Long
    ) = request { api.createCasualGame(player1Id, player2Id) }

    suspend fun createRankGame(
        player1Id : Long,
        player2Id : Long
    ) = request { api.createRankGame(player1Id, player2Id) }
}
