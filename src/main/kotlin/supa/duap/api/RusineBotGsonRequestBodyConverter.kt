package supa.duap.api

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import retrofit2.Converter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.Charset

/**
 * GsonRequestBodyConverter 의 카피
 *
 * @param T RequestBody 로 변환될 Target 타입
 * @param gson JsonWriter 생성을 위한 Gson 오브잭트
 * @param adapter Gson 오브잭트를 통해 만들어진 JsonWriter 와 같이 넘겨받은 value 를 갖고 JSON 값을 생성
 */
class RusineBotGsonRequestBodyConverter<T>(
    private val gson: Gson,
    private val adapter: TypeAdapter<T>
) :
    Converter<T, RequestBody> {
    @Throws(IOException::class)
    override fun convert(value: T): RequestBody {
        val buffer = Buffer()
        val writer: Writer =
            OutputStreamWriter(buffer.outputStream(), UTF_8)
        val jsonWriter = gson.newJsonWriter(writer)
        adapter.write(jsonWriter, value)
        jsonWriter.close()
        return buffer.readByteString()
            .toRequestBody(MEDIA_TYPE)
    }

    companion object {
        private val MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()
        private val UTF_8 = Charset.forName("UTF-8")
    }
}