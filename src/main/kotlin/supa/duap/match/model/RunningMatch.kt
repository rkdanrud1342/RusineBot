package supa.duap.match.model

data class RunningMatch(
    val id : Long,
    val player1 : PlayerProfile,
    val player2 : PlayerProfile,
    val player1WinCount : Int = 0,
    val player2WinCount : Int = 0,
    private val matchTypeCode : Int
) {
    val matchType = MatchType.typeCodeOf(matchTypeCode)
}