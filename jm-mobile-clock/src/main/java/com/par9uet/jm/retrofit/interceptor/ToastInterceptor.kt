package com.par9uet.jm.retrofit.interceptor

import com.par9uet.jm.store.ToastManager
import okhttp3.Interceptor
import okhttp3.Response

class ToastInterceptor(
    private val toastManager: ToastManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (!response.isSuccessful) {
            toastManager.showAsync("网络错误: ${response.code}")
        }
        return response
    }
}
