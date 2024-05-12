package supa.duap.api

data class RusineBotAPICommonResponse<T>(
    val code : Int,
    val message : String?,
    val data : T?
)
