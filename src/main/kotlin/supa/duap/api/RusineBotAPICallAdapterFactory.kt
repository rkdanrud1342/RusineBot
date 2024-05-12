package supa.duap.api

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class RusineBotAPICallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType : Type,
        annotations : Array<out Annotation>,
        retrofit : Retrofit
    ) : APIResponseAdapter<*>? {
        if (getRawType(returnType) != Call::class.java) {
            return null
        }

        check(returnType is ParameterizedType)

        val responseType = getParameterUpperBound(0, returnType)

        if (getRawType(responseType) != APIResponse::class.java) {
            return null
        }

        check(responseType is ParameterizedType)

        val successType = getParameterUpperBound(0, responseType)

        return APIResponseAdapter<Any>(successType)
    }
}