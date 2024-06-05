package supa.duap.match.model

data class MatchArgs(
    val playerId : Any,
    val rankAvailableRange : Int = 1,
    val awaitTimeMinutes : Int = 0,
    var phase : Int = 0
) {
    override fun equals(other : Any?) : Boolean {
        if (other == null || other !is MatchArgs) {
            return false
        }

        return playerId == other.playerId
                && rankAvailableRange == other.rankAvailableRange
    }

    override fun hashCode() : Int {
        var result = playerId.hashCode()
        result = 31 * result + rankAvailableRange
        return result
    }

    fun isAwaitOver() : Boolean = awaitTimeMinutes > 0 && phase * 30 >= awaitTimeMinutes * 60
}
