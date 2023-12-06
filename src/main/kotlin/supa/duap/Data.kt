package supa.duap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    @SerialName("token")
    val token : String,

    @SerialName("app_id")
    val appId : String,
)

