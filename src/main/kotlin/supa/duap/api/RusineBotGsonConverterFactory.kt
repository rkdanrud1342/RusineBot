package supa.duap.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.NullPointerException
import java.lang.reflect.Type

/**
 * API 응답을 json 형태로 받고 지정된 형태로 파싱하기 위해 커스텀된 GsonRequestBodyConverter, GsonResponseBodyConverter 를 생성하는 팩토리 클래스
 *
 * @param gson API 응답을 지정된 형태로 파싱하기 위한 Gson 오브젝트
 */
class RusineBotGsonConverterFactory private constructor(private val gson: Gson) : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type, annotations: Array<Annotation>,
        retrofit: Retrofit
    ): RusineBotGsonResponseBodyConverter {
        return RusineBotGsonResponseBodyConverter(gson)
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): RusineBotGsonRequestBodyConverter<*> {
        val adapter = gson.getAdapter(TypeToken.get(type))
        return RusineBotGsonRequestBodyConverter(gson, adapter)
    }

    companion object {
        @JvmOverloads  // Guarding public API nullability.
        fun create(gson: Gson? = Gson()): RusineBotGsonConverterFactory {
            if (gson == null) throw NullPointerException("gson == null")
            return RusineBotGsonConverterFactory(gson)
        }
    }
}