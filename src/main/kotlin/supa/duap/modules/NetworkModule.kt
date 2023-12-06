package supa.duap.modules

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        encodeDefaults = true
                        ignoreUnknownKeys = true
                        serializersModule = SerializersModule {}
                    }
                )
            }
        }
    }
}