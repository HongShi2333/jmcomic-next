package com.par9uet.jm.retrofit.interceptor

import com.par9uet.jm.retrofit.API_TOKEN_HASH
import com.par9uet.jm.retrofit.API_TS
import com.par9uet.jm.retrofit.API_VERSION
import com.par9uet.jm.retrofit.ApiContext
import com.par9uet.jm.retrofit.BUILTIN_APP_VERSION
import com.par9uet.jm.retrofit.BUILTIN_TOKEN_SECRET
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.utils.md5
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class TokenInterceptor(
    private val localSettingManager: LocalSettingManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val isBuiltin = localSettingManager.localSettingState.value.comicApiSource == "builtin"

        val timestamp: Long
        val token: String
        val tokenParam: String

        if (isBuiltin) {
            // 内置 API 模式：每请求时间戳，token 密钥 = 18comicAPP
            timestamp = System.currentTimeMillis() / 1000
            token = md5("${timestamp}${BUILTIN_TOKEN_SECRET}")
            tokenParam = "${timestamp},${BUILTIN_APP_VERSION}"
        } else {
            // 官方 API 模式：固定时间戳，token 密钥 = APP_DATA_SECRET（185Hcomic3PAPP7R）
            timestamp = API_TS
            token = API_TOKEN_HASH
            tokenParam = "${API_TS},${API_VERSION}"
        }

        // 设置 ThreadLocal 供 ResponseConverterFactory 解密使用
        ApiContext.setTimestamp(timestamp)

        val newRequest = originalRequest.newBuilder()
            .addHeader("tokenparam", tokenParam)
            .addHeader("token", token)
            .build()
        return chain.proceed(newRequest)
    }
}
