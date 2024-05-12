package supa.duap.api

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

/**
 * API 요청시에 넘긴 클래스로 파싱할 수 있는 어댑터 클래스
 *
 * @param T API 요청시에 넘긴 파싱 클래스
 * @param successType T 클래스 타입
 */
class APIResponseAdapter<T>(private val successType: Type): CallAdapter<JsonObject, Call<APIResponse<T>>> {

    /**
     * 응답 타입을 반환
     *
     * @return T 의 클래스 타입
     */
    override fun responseType() = successType

    /**
     * @param call API 응답을 갖고있는 Retrofit Call 인터페이스 오브잭트
     * @return API 응답 파싱을위한 Call 인터페이스 오브잭트
     */
    override fun adapt(call : Call<JsonObject>) = APIResponseCall<T>(call, successType)
}
