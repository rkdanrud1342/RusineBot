package supa.duap.match.model

data class Player(
    val id : Long,
    val name : String,
    val grade : Int,
    val casualWinCount : Int,
    val casualLoseCount : Int,
    val rankWinCount : Int,
    val rankLoseCount : Int,
    val afkCount : Int,
    val eloScore : Double
)
