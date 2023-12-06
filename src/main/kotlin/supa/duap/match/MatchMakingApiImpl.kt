package supa.duap.match

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import supa.duap.match.model.GameType

internal class MatchMakingApiImpl(private val client : HttpClient) : MatchMakingApi {
    @InternalAPI
    override suspend fun createPlayer(playerId : ULong) : HttpResponse {
        val formData = Parameters.build {
            append("id", "$playerId")
        }

        return client.post("rusine-bot-api:8080/player/") {
            body = FormDataContent(formData)
        }
    }

    @InternalAPI
    override suspend fun getPlayer(playerId : ULong) : HttpResponse =
        client.get("rusine-bot-api:8080/player/$playerId")

    @InternalAPI
    override suspend fun createGame(
        gameType : GameType,
        player1Id : ULong,
        player2Id : ULong
    ) : HttpResponse {
        val formData = Parameters.build {
            append("player1Id", "$player1Id")
            append("player2Id", "$player2Id")
        }

        return client.post("rusine-bot-api:8080/${gameType.typeName}/create") {
            method = HttpMethod.Post
            body = FormDataContent(formData)
        }
    }
}
