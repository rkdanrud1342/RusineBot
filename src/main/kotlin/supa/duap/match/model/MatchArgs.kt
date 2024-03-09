package supa.duap.match.model

data class MatchArgs(
    val playerId : Any,
    val rankAvailableRange : Int = 1,
    var phase : Int = 0
)
