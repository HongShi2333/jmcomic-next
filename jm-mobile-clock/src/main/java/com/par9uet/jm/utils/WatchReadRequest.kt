package com.par9uet.jm.utils

import com.par9uet.jm.retrofit.model.NetWorkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Timeout is an error; leaving the screen still cancels instead of becoming an error. */
suspend fun <T> watchReadRequest(label: String, timeoutMillis: Long = 35_000L,
    request: suspend () -> NetWorkResult<T>): NetWorkResult<T> = try {
    withTimeoutOrNull(timeoutMillis.coerceAtLeast(1)) { request() }
        ?: NetWorkResult.Error("$label 超时，请检查网络后重试")
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    NetWorkResult.Error("$label 失败：${error.message ?: "未知错误"}")
}
