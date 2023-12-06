package supa.duap

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

inline fun <reified T> request(
    crossinline block : suspend () -> HttpResponse
) : Flow<T> = flow {
    emit(block.invoke().body())
}