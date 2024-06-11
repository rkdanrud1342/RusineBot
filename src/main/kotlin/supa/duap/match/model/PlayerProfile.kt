package supa.duap.match.model

import supa.duap.Grade


data class Player(
    val id : Long,
    val name : String,
    val eloScore : Int
) {
    val grade : Grade
        get() = Grade.getGrade(eloScore)
}

data class PlayerProfile(
    val id : Long,
    val name : String,
    val casualWinCount : Int,
    val casualLoseCount : Int,
    val rankWinCount : Int,
    val rankLoseCount : Int,
    val eloScore : Int
) {
    val grade : Grade
        get() = Grade.getGrade(eloScore)
}

data class PlayerRank(
    val top10 : List<Player>,
    val player : Player,
    val rank : Int
)
