package supa.duap.match

import retrofit2.http.*
import supa.duap.api.APIResponse
import supa.duap.match.model.Game
import supa.duap.match.model.Player

interface MatchMakingApi {

    @FormUrlEncoded
    @POST("player/register")
    suspend fun registerUser(
        @Field("id") id : Long,
        @Field("playerName") name : String
    ) : APIResponse<Player?>

    @GET("player/info")
    suspend fun getPlayer(
        @Query("id") id : Long
    ) : APIResponse<Player?>

    @GET("player/all")
    suspend fun getAllPlayers() : APIResponse<List<Player>?>

    @FormUrlEncoded
    @POST("match/casual/create")
    suspend fun createCasualGame(
        @Field("player1Id") player1Id : Long,
        @Field("player2Id") player2Id : Long
    ) : APIResponse<Game.CasualGame?>

    @FormUrlEncoded
    @POST("match/casual/score")
    suspend fun registerCasualMatchScore(
        @Field("gameId") player1Id : Long,
        @Field("player1WinCount") player1WinCount : Int,
        @Field("player2WinCount") player2WinCount : Int
    ) : APIResponse<Any>

    @GET("match/casual/resent10")
    suspend fun getResent10CasualMatch(
        @Query("playerId") id : Long
    ) : APIResponse<List<Game.CasualGame>?>

    @FormUrlEncoded
    @POST("rank/game")
    suspend fun createRankGame(
        @Field("player1_id") player1Id : Long,
        @Field("player2_id") player2Id : Long
    ) : APIResponse<Game.RankGame?>
}