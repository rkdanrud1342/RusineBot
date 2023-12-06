package supa.duap.modules

import dev.kord.core.Kord
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.dsl.module
import supa.duap.Data
import java.io.File

val kordModule = module {
    single {
        runBlocking {
            val serializer = Json {
                prettyPrint = true
                encodeDefaults = true
                ignoreUnknownKeys = true
                serializersModule = SerializersModule {}
            }

            val file = File("./app_info.txt")
            val data = serializer.decodeFromString<Data>(file.readText())

            Kord(data.token)
        }
    }
}