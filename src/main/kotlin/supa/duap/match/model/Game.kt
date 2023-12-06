package supa.duap.match.model

import kotlinx.serialization.Serializable

sealed interface Game {
    val id : Long
    val player1Id : Long
    val player2Id : Long
    val player1WinCount : Int
    val player2WinCount : Int

    @Serializable
    data class CasualGame(
        override val id : Long,
        override val player1Id : Long,
        override val player2Id : Long,
        override val player1WinCount : Int = 0,
        override val player2WinCount : Int = 0
    ) : Game

    @Serializable
    data class RankGame(
        override val id : Long,
        override val player1Id : Long,
        override val player2Id : Long,
        override val player1WinCount : Int = 0,
        override val player2WinCount : Int = 0,
        val player1EstimateWinRate : Double,
        val player2EstimateWinRate : Double
    ) : Game
}
