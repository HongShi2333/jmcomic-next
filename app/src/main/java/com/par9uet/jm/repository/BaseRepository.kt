package com.par9uet.jm.repository

import android.util.Log
import coil.network.HttpException
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.ResponseWrapper
import com.par9uet.jm.store.InitManager
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
                NetWorkResult.Success(response.data!!)
            } else {
                NetWorkResult.Error(response.errorMsg!!)
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    suspend fun safeStringCall(apiCall: suspend () -> String): NetWorkResult<String> {
        return try {
            val response = apiCall()
            NetWorkResult.Success(response)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private fun handleException(e: Exception): NetWorkResult.Error {
        Log.d("api", e.stackTraceToString())
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
}
