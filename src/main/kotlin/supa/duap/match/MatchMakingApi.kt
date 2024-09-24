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

    @DELETE("player/profile")
    suspend fun deleteProfile(
        @Header("playerId") id : Long
    ) : APIResponse<PlayerProfile?>

    @FormUrlEncoded
    @POST("match/create")
    suspend fun createMatch(
        @Field("typeCode") typeCode : Int,
        @Field("player1Id") player1Id : Long,
        @Field("player2Id") player2Id : Long
    ) : APIResponse<RunningMatch?>

    @GET("match/running")
    suspend fun getRunningMatch(
        @Query("playerId") playerId : Long
    ) : APIResponse<RunningMatch?>

    @POST("match/cancel")
    suspend fun cancelRunningMatch(
        @Query("playerId") playerId : Long
    ) : APIResponse<MatchResult?>

    @FormUrlEncoded
    @POST("match/score")
    suspend fun registerMatchScore(
        @Field("playerId") playerId : Long,
        @Field("player1WinCount") player1WinCount : Int,
        @Field("player2WinCount") player2WinCount : Int
    ) : APIResponse<MatchResult?>

    @GET("player/ranking")
    suspend fun getPlayerRanking(
        @Query("playerId") playerId : Long
    ) : APIResponse<PlayerRank?>

    @FormUrlEncoded
    @POST("player/grade")
    suspend fun setPlayerGrade(
        @Field("playerId") playerId : Long,
        @Field("grade") grade : Int
    ) : APIResponse<PlayerProfile?>
}
