package supa.duap.api

import com.google.gson.*
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.IOException

/**
 * API 응답을 특정 json 형태로 변환하는 클래스
 *
 * @param gson json 을 생성하기 위한 Gson 오브잭트
 */
class RusineBotGsonResponseBodyConverter(private val gson : Gson) :
    Converter<ResponseBody, JsonObject> {

    @Throws(IOException::class)
    override fun convert(value : ResponseBody) : JsonObject? {
        val jsonString = value.charStream().readText()
        return value.use { gson.fromJson(jsonString, JsonObject::class.java) }
    }
}