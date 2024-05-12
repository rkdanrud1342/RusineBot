package supa.duap.api

import kotlinx.coroutines.flow.flow

fun <T> request(
    requestBlock : suspend () -> APIResponse<T>,
) = flow {
    when (val apiResult = requestBlock.invoke()) {
        is APIResponse.Success<T> -> emit(apiResult.data)
        is APIResponse.Fail -> throw APIRequestFailException(apiResult).also { it.printStackTrace() }
    }
}

class APIRequestFailException(apiResult : APIResponse.Fail<*>) : Exception(apiResult.message) {
    val code : Int
    override val message : String

    init {
        code = apiResult.code
        message = apiResult.message
    }
}
