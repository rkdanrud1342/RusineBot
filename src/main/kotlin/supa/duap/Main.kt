package supa.duap

import dev.kord.common.annotation.KordVoice
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.File

@Serializable
data class Data(
    @SerialName("token")
    val token : String,

    @SerialName("app_id")
    val appId : String,
)

@KordVoice
suspend fun main() {

    val serializer = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {}
    }

    val file = File("./app_info.txt")

    val data = serializer.decodeFromString<Data>(file.readText())

    val kord = Kord(data.token)

    InteractionManager(kord).start()

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents = Intents.nonPrivileged + Intent.MessageContent
    }
}
