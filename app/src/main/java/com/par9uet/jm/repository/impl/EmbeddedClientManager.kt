package com.par9uet.jm.repository.impl

import android.os.Build
import com.par9uet.jm.storage.CookieStorage
import com.par9uet.jm.network.DohManager
import com.par9uet.jm.utils.log
import com.par9uet.jm.utils.applyTlsCompat
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import okhttp3.Cookie
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * 共享内置 API 客户端管理器。
 * 确保 UserRepositoryImpl 和 ComicRepositoryImpl 使用同一个 JmApiClient 实例，
 * 这样 login() 设置的 loggedInUserName 和内部 Cookie 状态可以被所有 Repository 共享。
 * 解决内置 API 模式下 POST 请求（如创建收藏夹）返回 401 "請先登入會員" 的问题。
 *
 * Android 6 兼容：JmDomainManager 的域名探活使用 CompletableFuture.runAsync（ForkJoinPool），
 * 在 Android 6 上可能初始化失败导致 blockUntilInitialized 永久阻塞。
 * 此处在创建客户端后启动守护线程，超时后强制解除阻塞。
 */
class EmbeddedClientManager(
    private val cookieStorage: CookieStorage,
    private val dohManager: DohManager,
) {
    /** Android 6 fallback list; Android 7+ can use the library's dynamic list. */
    private val androidCompatibleApiDomains = listOf(
        "www.cdnaspa.vip",
        "www.cdnaspa.club",
        "www.cdnplaystation6.org",
        "www.cdnplaystation6.vip",
        "www.cdnplaystation6.cc",
    )

    @Volatile
    private var favoriteOrder: String = "mr"

    @Volatile
    private var client: JmApiClient? = null

    fun getClient(): JmApiClient {
        return client ?: synchronized(this) {
            client ?: createClient().also { client = it }
        }
    }

    fun setFavoriteOrder(order: String) {
        favoriteOrder = order
    }

    private fun createClient(): JmApiClient {
        val legacyAndroid = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
        val configBuilder = JmConfiguration.Builder()
            .clientType(ClientType.API)
            .timeout(Duration.ofSeconds(12))
            .imageTimeout(Duration.ofSeconds(60))
            .downloadThreadPoolSize(2)
            .domainProbeTimeoutMs(1500)
        if (legacyAndroid) {
            // Android 6 lacks the platform Java 9/CompletableFuture surface used
            // by the library's dynamic domain bootstrap. Desugaring still lets
            // requests run, but explicit domains avoid the fragile bootstrap.
            configBuilder.apiDomains(androidCompatibleApiDomains)
        }
        val config = configBuilder.build()
        val context = OkHttpBuilder.build(config)
        val domainManager = context.domainManager
        val clientWithCookieInjection = context.client.newBuilder()
            .dns(dohManager)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply {
                // 收藏详情标签按批并发请求；将同主机上限与批量规模对齐，
                // 避免默认 5 个连接把并发请求额外排队。
                maxRequests = 16
                maxRequestsPerHost = 8
            })
            .applyTlsCompat()
            .addInterceptor { chain ->
                val cookies = cookieStorage.get()
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                if (original.url.encodedPath.endsWith("/favorite")) {
                    requestBuilder.url(
                        original.url.newBuilder()
                            .setQueryParameter("o", favoriteOrder)
                            .build()
                    )
                }
                if (cookies.isNotEmpty()) {
                    val cookieHeader = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    requestBuilder
                        .header("Cookie", cookieHeader)
                }
                val request = requestBuilder.build()
                val response = chain.proceed(request)
                // 从响应头提取 Set-Cookie，同步到 cookieStorage，保证登录态持久化
                val setCookieHeaders = response.headers("Set-Cookie")
                if (setCookieHeaders.isNotEmpty()) {
                    val newCookies = setCookieHeaders.mapNotNull { Cookie.parse(request.url, it) }
                    if (newCookies.isNotEmpty()) {
                        val existing = cookieStorage.get().toMutableList()
                        val newKeys = newCookies.map { "${it.domain}:${it.path}:${it.name}" }.toSet()
                        existing.removeAll { "${it.domain}:${it.path}:${it.name}" in newKeys }
                        existing.addAll(newCookies)
                        cookieStorage.set(existing)
                    }
                }
                response
            }
            .build()
        val jmClient = JmApiClient(config, clientWithCookieInjection, context.cookieManager, domainManager)

        if (legacyAndroid) {
            // Android 6 only: domain probing can block on the desugared
            // CompletableFuture/ForkJoin implementation. Newer Android keeps
            // the library's normal probing and retry behavior.
            Thread({
                try {
                    Thread.sleep(8000)
                    if (!domainManager.isInitialized) {
                        log("EmbeddedClientManager: 域名探活初始化超时，强制解除阻塞")
                        domainManager.setInitialized(true)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }, "embedded-domain-init-guard").apply {
                isDaemon = true
                start()
            }
        }

        return jmClient
    }
}
