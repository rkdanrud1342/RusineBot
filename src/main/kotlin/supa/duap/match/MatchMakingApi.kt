package supa.duap.match

import retrofit2.http.*
import supa.duap.api.APIResponse
import supa.duap.match.model.*

interface MatchMakingApi {

    @FormUrlEncoded
    @POST("player/register")
    suspend fun registerUser(
        @Field("id") id : Long,
        @Field("playerName") name : String,
        @Field("grade") grade : Int
    ) : APIResponse<PlayerProfile?>

    @GET("player/info")
    suspend fun getPlayer(
        @Query("id") id : Long
    ) : APIResponse<Player?>

    @GET("player/profile")
    suspend fun getProfile(
        @Query("id") id : Long
    ) : APIResponse<PlayerProfile?>

    @FormUrlEncoded
    @POST("match/create")
    suspend fun createGame(
        @Field("gameType") gameType : String,
        @Field("player1Id") player1Id : Long,
        @Field("player2Id") player2Id : Long
    ) : APIResponse<RunningGame?>

    @GET("match/running")
    suspend fun getRunningGame(
        @Query("playerId") playerId : Long
    ) : APIResponse<RunningGame?>

    @FormUrlEncoded
    @POST("match/score")
    suspend fun registerGameScore(
        @Field("playerId") playerId : Long,
        @Field("player1WinCount") player1WinCount : Int,
        @Field("player2WinCount") player2WinCount : Int
    ) : APIResponse<GameResult?>

    @GET("player/ranking")
    suspend fun getPlayerRanking(
        @Query("playerId") playerId : Long
    ) : APIResponse<PlayerRank?>
}
