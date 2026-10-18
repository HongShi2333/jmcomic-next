package com.par9uet.jm.retrofit.interceptor

import com.par9uet.jm.store.LocalSettingManager
import okhttp3.Interceptor
import okhttp3.Response

class BaseUrlInterceptor(
    private val localSettingManager: LocalSettingManager
) : Interceptor {

    private fun getBaseUrl() = localSettingManager.localSettingState.value.api

    override fun intercept(chain: Interceptor.Chain): Response {
        val baseUrl = getBaseUrl()
        var request = chain.request()
        val newUrl = request.url.newBuilder()
            .scheme(baseUrl.split("://")[0]) // 处理 http 或 https
            .host(baseUrl.split("://")[1].removeSuffix("/"))
            .build()

        request = request.newBuilder().url(newUrl).build()
        return chain.proceed(request)
    }
}