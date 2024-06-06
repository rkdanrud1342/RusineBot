package supa.duap.match.model

data class GameResult(
    val gameType : GameType,

    val player1Name : String,
    val player1WinCount : Int,
    val player1EloScore : Double,
    val player1EloScoreChange : Double,

    val player2Name : String,
    val player2WinCount : Int,
    val player2EloScore : Double,
    val player2EloScoreChange : Double,
)
