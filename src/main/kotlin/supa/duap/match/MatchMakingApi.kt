package supa.duap.match

import retrofit2.http.*
import supa.duap.api.APIResponse
import supa.duap.match.model.Game
import supa.duap.match.model.PlayerProfile
import supa.duap.match.model.Player

interface MatchMakingApi {

    @FormUrlEncoded
    @POST("player/register")
    suspend fun registerUser(
        @Field("id") id : Long,
        @Field("playerName") name : String
    ) : APIResponse<PlayerProfile?>

    @GET("player/info")
    suspend fun getPlayer(
        @Query("id") id : Long
    ) : APIResponse<Player?>

    @GET("player/profile")
    suspend fun getProfile(
        @Query("id") id : Long
    ) : APIResponse<PlayerProfile?>

    @GET("player/all")
    suspend fun getAllPlayers() : APIResponse<List<PlayerProfile>?>

    @FormUrlEncoded
    @POST("match/casual/create")
    suspend fun createCasualGame(
        @Field("player1Id") player1Id : Long,
        @Field("player2Id") player2Id : Long
    ) : APIResponse<Game.CasualGame?>

    @FormUrlEncoded
    @POST("match/casual/score")
    suspend fun registerCasualGameScore(
        @Field("gameId") gameId : Long,
        @Field("player1WinCount") player1WinCount : Int,
        @Field("player2WinCount") player2WinCount : Int
    ) : APIResponse<Any>

    @GET("match/casual/resent10")
    suspend fun getResent10CasualMatch(
        @Query("playerId") id : Long
    ) : APIResponse<List<Game.CasualGame>?>

    @FormUrlEncoded
    @POST("match/rank/create")
    suspend fun createRankGame(
        @Field("player1Id") player1Id : Long,
        @Field("player2Id") player2Id : Long
    ) : APIResponse<Game.RankGame?>

    @FormUrlEncoded
    @POST("match/rank/score")
    suspend fun registerRankGameScore(
        @Field("gameId") gameId : Long,
        @Field("player1WinCount") player1WinCount : Int,
        @Field("player2WinCount") player2WinCount : Int
    ) : APIResponse<Any>

    @FormUrlEncoded
    @POST("player/score/update")
    suspend fun registerScore(
        playerId : Long,
        eloScore : Double
    ) : APIResponse<Unit>
}
