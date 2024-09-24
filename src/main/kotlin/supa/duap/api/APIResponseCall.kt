package supa.duap.api

import com.google.gson.*
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import supa.duap.api.serialization.LocalDateTimeDeserializer
import supa.duap.api.serialization.LocalDateTimeSerializer
import java.lang.reflect.Type
import java.time.LocalDateTime

/**
 * API 요청을 파싱하는 클래스
 *
 * @param T API 응답이 파싱될 형태의 클래스
 * @param callDelegate API 응답을 갖고 있는 레트로핏 Call 인스턴스
 * @param successType T 클래스 타입
 */
class APIResponseCall<T>(
    private val callDelegate: Call<JsonObject>,
    private val successType: Type
) : Call<APIResponse<T>> {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
        .create()

    /**
     * 백그라운드에서 API 응답을 처리하는 메소드
     *
     * @param callback API 응답을 처리하기위한 Callback 인터페이스 오브잭트
     */
    override fun enqueue(callback: Callback<APIResponse<T>>) =
        callDelegate.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                try {
                    if (!response.isSuccessful) {
                        val httpResponseCode = response.code()

                        when (httpResponseCode) {
                            in 400 .. 499 -> {
                                callback.onResponse(
                                    this@APIResponseCall,
                                    Response.success(APIResponse.Fail(message = "요청이 잘못되었습니다."))
                                )
                            }

                            in 500..599 -> {
                                callback.onResponse(
                                    this@APIResponseCall,
                                    Response.success(APIResponse.Fail(message = "서버가 응답할 수 없는 상태입니다."))
                                )
                            }
                        }

                        return
                    }

                    response.body()?.let { resBody ->
                        try {
                            if (resBody.size() == 0) {
                                callback.onResponse(
                                    this@APIResponseCall,
                                    Response.success(APIResponse.Fail(message = "요청에 대한 결과가 없습니다."))
                                )
                                return
                            }

                            val code = resBody.get("code").asInt
                            val message = resBody.get("message")?.asString

                            if (code < 0) {
                                callback.onResponse(
                                    this@APIResponseCall,
                                    Response.success(APIResponse.Fail(message = message ?: "알 수 없는 오류입니다."))
                                )
                                return
                            }

                            val data =
                                if (this@APIResponseCall.callDelegate.request().method != "DELETE") {
                                    gson.fromJson<T>(resBody.get("data"), successType)
                                } else {
                                    null
                                }

                            callback.onResponse(
                                this@APIResponseCall,
                                Response.success(
                                    APIResponse.Success(
                                        code,
                                        message,
                                        data
                                    )
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            callback.onResponse(
                                this@APIResponseCall,
                                Response.success(APIResponse.Fail(message = "데이터 처리 도중 오류가 발생했습니다."))
                            )
                        }
                    } ?: run {
                        callback.onResponse(
                            this@APIResponseCall, Response.success(
                                APIResponse.Fail(message = "데이터 없음")
                            )
                        )
                    }
                    // endregion
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback.onResponse(
                        this@APIResponseCall, Response.success(
                            APIResponse.Fail(message = e.message ?: "알 수 없는 에러")
                        )
                    )
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                t.printStackTrace()

                callback.onResponse(
                    this@APIResponseCall,
                    Response.success(
                        APIResponse.Fail(message = t.message ?: "알 수 없는 에러")
                    )
                )

                call.cancel()
            }
        })

    /**
     * 복제 메소드
     *
     * @return 복제된 APIResponseCall 오브잭트
     */
    override fun clone() = APIResponseCall<T>(callDelegate.clone(), successType)

    /**
     * API 응답을 동기적으로 처리하기 위한 메소드
     * 동기 처리는 지원하지 않으므로 UnsupportedOperationException 을 throw 하도록 구현됨
     *
     * @throws UnsupportedOperationException
     */
    override fun execute() =
        throw UnsupportedOperationException("ResponseCall은 execute를 지원하지 않습니다.")

    /**
     * execute 호출이 되었는지 여부를 반환
     *
     * @return callDelegate.isExecuted 를 그대로 반환
     */
    override fun isExecuted() = callDelegate.isExecuted

    /**
     * API 파싱 작업을 취소하는 메소드
     */
    override fun cancel() = callDelegate.cancel()

    /**
     * cancel() 호출이 되었는지 여부를 반환
     *
     * @return callDelegate.isCanceled 를 그대로 반환
     */
    override fun isCanceled() = callDelegate.isCanceled

    /**
     * API 요청정보
     *
     * @return API 요청정보
     */
    override fun request(): Request = callDelegate.request()

    /**
     * API 요청 및 응답 파싱 등에 걸리는 전체 Timeout
     *
     * @return API 요청 및 응답 파싱 등에 걸리는 전체 Timeout 정보를 반환
     */
    override fun timeout(): Timeout = callDelegate.timeout()
}