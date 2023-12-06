package supa.duap.match.model

data class Player(
    val id : ULong,
    val grade : Int,
    val winCount : Int,
    val loseCount : Int,
    val afkCount : Int,
    val eloScore : Double,
)
