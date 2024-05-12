package supa.duap.api

sealed class APIResponse<T> {
    abstract val code : Int
    abstract val message : String?

    data class Success<T>(
        override val code : Int,
        override val message : String?,
        val data : T?
    ) : APIResponse<T>()

    class Fail<T> : APIResponse<T> {
        override val code : Int
        override val message : String

        constructor (code : Int = -1, message : String) {
            this.code = code
            this.message = message
        }

        constructor(commonResponse : RusineBotAPICommonResponse<*>) {
            code = commonResponse.code
            message = commonResponse.message!!
        }
    }
}
