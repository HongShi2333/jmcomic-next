package com.par9uet.jm.repository

import coil.network.HttpException
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.ResponseWrapper
import com.par9uet.jm.store.InitManager
import com.par9uet.jm.utils.logError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

open class BaseRepository(
    private val initManager: InitManager
) {

    suspend fun <T> safeApiCall(apiCall: suspend () -> ResponseWrapper<T>): NetWorkResult<T> {
        return try {
            val response = apiCall()
            if (response.code == 200) {
                response.data?.let { NetWorkResult.Success(it) }
                    ?: NetWorkResult.Error("响应数据为空")
            } else {
                val errMsg = response.errorMsg ?: "未知错误"
                logError(this::class.java.simpleName, "API 返回错误: $errMsg")
                NetWorkResult.Error(errMsg)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun safeStringCall(apiCall: suspend () -> String): NetWorkResult<String> {
        return try {
            val response = apiCall()
            NetWorkResult.Success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * Retry idempotent comic/data reads in one place. The first call plus three
     * retries share the same caller cancellation and end in the existing manual
     * retry state when all attempts fail.
     */
    protected suspend fun <T> retryNetworkRequest(
        operation: String,
        request: suspend () -> NetWorkResult<T>,
    ): NetWorkResult<T> {
        var lastError: NetWorkResult.Error? = null
        repeat(AUTO_RETRY_COUNT + 1) { attempt ->
            currentCoroutineContext().ensureActive()
            val result = try {
                request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetWorkResult.Error(e.message ?: "${operation}请求失败")
            }

            when (result) {
                is NetWorkResult.Success -> return result
                is NetWorkResult.Error -> {
                    lastError = result
                    if (attempt < AUTO_RETRY_COUNT) {
                        currentCoroutineContext().ensureActive()
                        val retryNumber = attempt + 1
                        logError(
                            this::class.java.simpleName,
                            "${operation}失败，自动重试第$retryNumber/${AUTO_RETRY_COUNT}次：${result.message}",
                        )
                        delay(RETRY_DELAYS_MS[attempt])
                    }
                }
            }
        }

        val error = lastError
        return NetWorkResult.Error(
            message = "${operation}失败，已自动重试${AUTO_RETRY_COUNT}次，请手动重试" +
                (error?.message?.takeIf { it.isNotBlank() }?.let { "：$it" } ?: ""),
            code = error?.code ?: -1,
        )
    }

    private fun handleException(e: Exception): NetWorkResult.Error {
        logError(this::class.java.simpleName, "请求异常: ${e.stackTraceToString()}")
        return when (e) {
            is SocketTimeoutException -> NetWorkResult.Error("网络连接超时")
            is ConnectException -> NetWorkResult.Error("网络连接失败")
            is UnknownHostException -> NetWorkResult.Error("网络不可用")
            is HttpException -> {
                val errMsg = when (e.response.code) {
                    401 -> "账号或密码错误，请重新输入"
                    else -> "网络错误：${e.response.code}"
                }
                NetWorkResult.Error(errMsg)
            }

            else -> NetWorkResult.Error(
                e.message ?: "未知错误"
            )
        }
    }

    private companion object {
        const val AUTO_RETRY_COUNT = 3
        val RETRY_DELAYS_MS = longArrayOf(350L, 700L, 1_200L)
    }
}
