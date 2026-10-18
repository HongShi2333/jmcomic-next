package com.par9uet.jm.retrofit.model

data class ResponseWrapper<out T>(
    val code: Int,
    val data: T? = null,
    val errorMsg: String? = null
) {
    companion object {
        fun <T> Success(data: T, code: Int = 200): ResponseWrapper<T> = ResponseWrapper(
            code, data,
        )

        fun Error(errorMsg: String, code: Int = 500): ResponseWrapper<Nothing> = ResponseWrapper(
            code, null, errorMsg
        )
    }
}