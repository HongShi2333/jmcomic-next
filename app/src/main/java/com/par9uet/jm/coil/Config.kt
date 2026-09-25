package com.par9uet.jm.coil

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import com.par9uet.jm.cache.getComicCoverCacheDir
import com.par9uet.jm.utils.applyTlsCompat
import okhttp3.ConnectionSpec
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Dns
import okhttp3.TlsVersion
import java.util.concurrent.TimeUnit

private val cdnHeaderInterceptor = Interceptor { chain ->
    val request = chain.request().newBuilder()
        .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; V1938CT Build/PQ3A.190705.11211812; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Safari/537.36")
        .header("Referer", "https://18comic.vip")
        .build()
    chain.proceed(request)
}

fun createAsyncImageLoader(context: Context, dns: Dns): ImageLoader {
    return ImageLoader.Builder(context)
        .okHttpClient {
            OkHttpClient.Builder()
                .addInterceptor(cdnHeaderInterceptor)
                .dns(dns)
                .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                .dispatcher(Dispatcher().apply {
                    // 收藏页和首页会同时加载多张封面；适度提高同主机并发，
                    // 避免 DoH 已命中后请求仍在 OkHttp 队列中排队。
                    maxRequests = 24
                    maxRequestsPerHost = 8
                })
                .applyTlsCompat()
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(getComicCoverCacheDir(context))
                .maxSizeBytes(1024L * 1024 * 1024) // 1GB，上限由 cacheDir 管理
                .build()
        }
        .build()
}
