package supa.duap.match.model

import com.google.gson.annotations.SerializedName

data class RunningMatch(
    val id : Long,
    val player1 : PlayerProfile,
    val player2 : PlayerProfile,
    val player1WinCount : Int = 0,
    val player2WinCount : Int = 0,

    @SerializedName("gameType")
    val matchType : MatchType
)