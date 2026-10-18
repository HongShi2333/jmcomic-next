package com.par9uet.jm.network

import android.util.Base64
import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.storage.SecureStorage
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.utils.applyTlsCompat
import com.par9uet.jm.utils.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.UnknownHostException
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import okhttp3.ConnectionPool
import okhttp3.Dispatcher

data class DohRuntimeStatus(
    val active: Boolean = false,
    val serverName: String = "",
    val serverUrl: String = "",
    val cacheEntryCount: Int = 0,
    val lastError: String = "",
)

data class DohLatencyResult(
    val elapsedMs: Long? = null,
    val error: String = "",
)

/**
 * A process-wide DNS implementation for all app-owned OkHttp clients.
 * The DoH bootstrap client deliberately uses system DNS (or a fixed bootstrap IP)
 * so resolving the DNS server never recurses into this resolver.
 */
class DohManager(
    private val localSettingManager: LocalSettingManager,
    private val secureStorage: SecureStorage,
) : Dns, AppInitTask {
    private val random = SecureRandom()
    private val _status = MutableStateFlow(DohRuntimeStatus())
    val status = _status.asStateFlow()
    private val _latencyState = MutableStateFlow<Map<String, DohLatencyResult>>(emptyMap())
    val latencyState = _latencyState.asStateFlow()

    @Volatile
    private var sessionEnabled = false

    @Volatile
    private var resolverKey = ""

    @Volatile
    private var resolver: DohResolver? = null

    @Volatile
    private var resolverFailure: String = ""

    @Volatile
    private var cacheStorageKey = ""

    // 写入加密持久化缓存不能阻塞 OkHttp 的 DNS 线程。DNS 解析只更新内存，
    // 由单线程后台队列合并写入最近快照，避免首页/收藏并发解析时反复加密落盘。
    private val cachePersistenceExecutor = Executors.newSingleThreadExecutor()
    private val pendingCacheSnapshots = ConcurrentHashMap<String, Map<String, PersistedDohCacheEntry>>()
    private val cachePersistenceScheduled = AtomicBoolean(false)

    // 在 DoH 配置完成后异步解析主 API 域名，让首个真实请求直接复用已缓存的地址，
    // 同时不阻塞应用启动；generation 用于让清理缓存时忽略已经排队的预热任务。
    private val resolverWarmupExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "jm-doh-warmup").apply { isDaemon = true }
    }
    private val resolverGeneration = AtomicLong(0L)

    override fun lookup(hostname: String): List<InetAddress> {
        val setting = localSettingManager.localSettingState.value
        val selectedResolver = ensureResolver()
        if (selectedResolver != null) {
            return try {
                selectedResolver.lookup(hostname)
            } catch (error: Throwable) {
                publishStatus(lastError = error.message ?: "DoH 解析失败")
                // A DoH endpoint can be temporarily unreachable even while
                // the device network is healthy. Keep requests usable by
                // falling back to the platform resolver, while retaining the
                // IPv4-only preference when IPv6 is disabled.
                runCatching {
                    Dns.SYSTEM.lookup(hostname)
                        .filter { setting.dohPreferIpv6 || it is Inet4Address }
                        .sortedWith(compareBy<InetAddress> { it is Inet6Address })
                }.getOrElse { throw (error as? Exception ?: UnknownHostException(error.message ?: "DoH 解析失败")) }
            }
        }
        return Dns.SYSTEM.lookup(hostname)
            .filter { address -> localSettingManager.localSettingState.value.dohPreferIpv6 || address is Inet4Address }
            .sortedWith(compareBy<InetAddress> { it is Inet6Address })
    }

    fun setEnabled(enabled: Boolean) {
        localSettingManager.updateDohEnabled(enabled)
        sessionEnabled = enabled
        rebuildResolver()
    }

    fun setAutoStart(enabled: Boolean) {
        localSettingManager.updateDohAutoStart(enabled)
    }

    fun selectServer(serverId: String) {
        localSettingManager.updateDohServer(serverId)
        rebuildResolver()
    }

    fun saveCustomServer(name: String, url: String) {
        require(isValidDohUrl(url)) { "请输入 HTTPS DoH 地址" }
        localSettingManager.updateDohCustomServer(name, url)
        rebuildResolver()
    }

    fun setUseDeviceCertificates(enabled: Boolean) {
        localSettingManager.updateDohUseDeviceCertificates(enabled)
        rebuildResolver()
    }

    fun setPreferIpv6(enabled: Boolean) {
        localSettingManager.updateDohPreferIpv6(enabled)
        rebuildResolver()
    }

    fun clearCache() {
        resolverGeneration.incrementAndGet()
        runCatching { resolver?.clearCache() }
        pendingCacheSnapshots.remove(cacheStorageKey)
        runCatching {
            if (cacheStorageKey.isNotBlank()) secureStorage.remove(cacheStorageKey)
        }.onFailure { error ->
            logError("DohManager", "清理 DoH 持久化缓存失败：${error.message}")
        }
        publishStatus(lastError = "")
    }

    suspend fun testServer(server: DohServer): DohLatencyResult = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        var testResolver: DohResolver? = null
        val result = runCatching {
            val setting = localSettingManager.localSettingState.value
            val resolver = DohResolver(
                server = server,
                preferIpv6 = setting.dohPreferIpv6,
                useDeviceCertificates = setting.dohUseDeviceCertificates,
                random = random,
            )
            testResolver = resolver
            resolver.lookup("www.qq.com")
            DohLatencyResult(elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt))
        }.getOrElse { DohLatencyResult(error = it.message ?: "测试失败") }.also {
            testResolver?.close()
        }
        _latencyState.value = _latencyState.value + (server.id to result)
        result
    }

    fun selectedServer(): DohServer = localSettingManager.localSettingState.value.toDohServer()

    override suspend fun init() {
        // DoH is opt-in on every fresh process. Auto-start restores the prior choice.
        sessionEnabled = localSettingManager.localSettingState.value.let {
            it.dohEnabled && it.dohAutoStart
        }
        rebuildResolver()
    }

    override fun getAppTaskInfo(): AppTaskInfo = AppTaskInfo(
        taskName = "DoH 网络配置",
        sort = 1,
    )

    private fun ensureResolver(): DohResolver? {
        val setting = localSettingManager.localSettingState.value
        val key = setting.dohResolverKey(sessionEnabled)
        if (key != resolverKey) rebuildResolver()
        return resolver
    }

    @Synchronized
    private fun rebuildResolver() {
        val setting = localSettingManager.localSettingState.value
        val key = setting.dohResolverKey(sessionEnabled)
        val nextCacheStorageKey = setting.dohCacheStorageKey()
        val generation = resolverGeneration.incrementAndGet()
        val previousResolver = resolver
        var nextFailure = ""
        val nextResolver = if (sessionEnabled && setting.dohEnabled) {
            runCatching {
                DohResolver(
                    server = setting.toDohServer(),
                    preferIpv6 = setting.dohPreferIpv6,
                    useDeviceCertificates = setting.dohUseDeviceCertificates,
                    random = random,
                    persistedCache = loadPersistentCache(nextCacheStorageKey),
                    onCacheChanged = { snapshot -> schedulePersistCache(nextCacheStorageKey, snapshot) },
                )
            }.onFailure { error ->
                nextFailure = error.message ?: "DoH 解析器初始化失败"
            }.getOrNull()
        } else {
            null
        }
        // Build the replacement before closing the old client. A failed
        // reconfiguration must leave the existing network resolver usable.
        resolverKey = key
        cacheStorageKey = nextCacheStorageKey
        resolverFailure = nextFailure
        resolver = nextResolver
        scheduleApiWarmup(nextResolver, key, generation, setting.api)
        runCatching { previousResolver?.close() }
            .onFailure { error -> logError("DohManager", "关闭旧 DoH 解析器失败：${error.message}") }
        publishStatus(lastError = resolverFailure)
    }

    private fun scheduleApiWarmup(
        target: DohResolver?,
        expectedKey: String,
        expectedGeneration: Long,
        apiUrl: String,
    ) {
        val warmupResolver = target ?: return
        val host = apiUrl.toHttpUrlOrNull()?.host?.takeIf { it.isNotBlank() } ?: return
        resolverWarmupExecutor.execute {
            if (resolverGeneration.get() != expectedGeneration ||
                resolverKey != expectedKey ||
                resolver !== warmupResolver
            ) {
                return@execute
            }
            // 预热失败不改变状态；真实请求仍会走 DohManager 的系统 DNS 回退。
            runCatching { warmupResolver.lookup(host) }
        }
    }

    private fun publishStatus(lastError: String = _status.value.lastError) {
        val activeResolver = resolver
        _status.value = DohRuntimeStatus(
            active = activeResolver != null,
            serverName = activeResolver?.server?.name.orEmpty(),
            serverUrl = activeResolver?.server?.displayUrl.orEmpty(),
            cacheEntryCount = activeResolver?.cacheSize ?: 0,
            lastError = lastError,
        )
    }

    private fun loadPersistentCache(storageKey: String): Map<String, PersistedDohCacheEntry> {
        val now = System.currentTimeMillis()
        val stored = runCatching {
            secureStorage.get<Map<String, PersistedDohCacheEntry>>(
                storageKey,
                object : TypeToken<Map<String, PersistedDohCacheEntry>>() {}.type,
            )
        }.getOrNull().orEmpty()
        return stored.mapNotNull { (hostname, entry) ->
            runCatching {
                val expiresAt = entry.expiresAt
                val addresses = entry.addresses.orEmpty().filter { it.isNotBlank() }
                if (hostname.isBlank() || expiresAt <= now || addresses.isEmpty()) {
                    null
                } else {
                    hostname.lowercase() to PersistedDohCacheEntry(addresses, expiresAt)
                }
            }.getOrNull()
        }.toMap()
    }

    private fun persistCache(
        storageKey: String,
        snapshot: Map<String, PersistedDohCacheEntry>,
    ) {
        runCatching {
            val now = System.currentTimeMillis()
            val compactSnapshot = snapshot
                .filterValues { entry ->
                    entry.expiresAt > now && entry.addresses.orEmpty().isNotEmpty()
                }
                .toList()
                .sortedByDescending { (_, entry) -> entry.expiresAt }
                .take(MAX_PERSISTED_CACHE_ENTRIES)
                .toMap()
            if (compactSnapshot.isEmpty()) {
                secureStorage.remove(storageKey)
            } else {
                secureStorage.set(storageKey, compactSnapshot)
            }
        }.onFailure { error ->
            // DNS cache persistence is optional; it must never fail a real request.
            logError("DohManager", "保存 DoH 持久化缓存失败：${error.message}")
        }
    }

    private fun schedulePersistCache(
        storageKey: String,
        snapshot: Map<String, PersistedDohCacheEntry>,
    ) {
        pendingCacheSnapshots[storageKey] = snapshot
        if (!cachePersistenceScheduled.compareAndSet(false, true)) return
        cachePersistenceExecutor.execute {
            try {
                while (true) {
                    val next = pendingCacheSnapshots.entries.firstOrNull()
                        ?: break
                    pendingCacheSnapshots.remove(next.key, next.value)
                    persistCache(next.key, next.value)
                }
            } finally {
                cachePersistenceScheduled.set(false)
                if (pendingCacheSnapshots.isNotEmpty()) {
                    pendingCacheSnapshots.entries.firstOrNull()?.let { entry ->
                        schedulePersistCache(entry.key, entry.value)
                    }
                }
            }
        }
    }
}

private data class PersistedDohCacheEntry(
    val addresses: List<String>? = null,
    val expiresAt: Long = 0L,
)

private class DohResolver(
    val server: DohServer,
    private val preferIpv6: Boolean,
    useDeviceCertificates: Boolean,
    private val random: SecureRandom,
    persistedCache: Map<String, PersistedDohCacheEntry> = emptyMap(),
    private val onCacheChanged: (Map<String, PersistedDohCacheEntry>) -> Unit = {},
) : Dns {
    private data class CachedAddress(
        val addresses: List<InetAddress>,
        val expiresAt: Long,
    )

    private val cache = ConcurrentHashMap<String, CachedAddress>().apply {
            persistedCache.forEach { (hostname, entry) ->
            val addresses = entry.addresses.orEmpty().mapNotNull { address ->
                runCatching { InetAddress.getByName(address) }.getOrNull()
            }.filter { address -> preferIpv6 || address is Inet4Address }
            if (addresses.isNotEmpty() && entry.expiresAt > System.currentTimeMillis()) {
                this[hostname] = CachedAddress(addresses, entry.expiresAt)
            }
        }
    }
    private val lookupLocks = ConcurrentHashMap<String, Any>()
    private val failedUntil = ConcurrentHashMap<String, Long>()
    @Volatile
    private var dohUnavailableUntil = 0L
    val cacheSize: Int get() = cache.size

    private val bootstrapDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            if (server.bootstrapHost != null && hostname.equals(server.bootstrapHost, ignoreCase = true)) {
                server.bootstrapIps.map { InetAddress.getByName(it) }
            } else {
                Dns.SYSTEM.lookup(hostname).let { addresses ->
                    if (preferIpv6) addresses else addresses.filterIsInstance<Inet4Address>()
                }
            }
        }
    private val client = OkHttpClient.Builder()
        .dns(bootstrapDns)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .dispatcher(Dispatcher().apply {
            // 多个 API/图片域名可能同时触发解析，避免 DoH 客户端默认的
            // 5 个同主机并发上限把请求排队放大成首屏等待。
            maxRequests = 16
            maxRequestsPerHost = 16
        })
        // DoH 只负责解析域名；失败后上层会立即回退到系统 DNS，
        // 因此不应让一次异常的 DoH 请求拖慢整个 API 请求链路。
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .applyTlsCompat(includeDeviceCertificates = useDeviceCertificates)
        .build()

    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.isIpLiteral()) {
            val address = InetAddress.getByName(hostname)
            if (!preferIpv6 && address !is Inet4Address) {
                throw UnknownHostException("IPv6 已关闭：$hostname")
            }
            return listOf(address)
        }
        val key = hostname.lowercase()
        val now = System.currentTimeMillis()
        cache[key]?.takeIf { it.expiresAt > now }?.let { return it.addresses }
        if (dohUnavailableUntil > now) return lookupWithSystemDns(hostname)
        failedUntil[key]?.takeIf { it > now }?.let {
            throw UnknownHostException("DoH 暂时跳过重复失败解析：$hostname")
        }

        // OkHttp may call Dns.lookup concurrently for many requests to the same
        // host. Share one in-flight resolution so a slow DoH endpoint is queried
        // once instead of once per home/favorite item.
        val lock = lookupLocks.getOrPut(key) { Any() }
        synchronized(lock) {
            val secondNow = System.currentTimeMillis()
            cache[key]?.takeIf { it.expiresAt > secondNow }?.let { return it.addresses }
            if (dohUnavailableUntil > secondNow) return lookupWithSystemDns(hostname)
            failedUntil[key]?.takeIf { it > secondNow }?.let {
                throw UnknownHostException("DoH 暂时跳过重复失败解析：$hostname")
            }

            // IPv6 is opt-in. When disabled, do not query AAAA records.
            val types = if (preferIpv6) listOf(TYPE_AAAA, TYPE_A) else listOf(TYPE_A)
            var lastResolveError: Exception? = null
            val records = types.flatMap { type ->
                runCatching { resolve(hostname, type) }
                    .onFailure { error -> lastResolveError = error as? Exception }
                    .getOrDefault(emptyList())
            }.distinctBy { it.address.hostAddress }
            if (records.isEmpty()) {
                failedUntil[key] = System.currentTimeMillis() + FAILURE_COOLDOWN_MS
                dohUnavailableUntil = System.currentTimeMillis() + DOH_FAILURE_COOLDOWN_MS
                throw UnknownHostException(lastResolveError?.message ?: "DoH 未返回 $hostname 的地址")
            }
            failedUntil.remove(key)
            dohUnavailableUntil = 0L
            val ttlSeconds = records.minOf { it.ttlSeconds }.coerceIn(20L, 24 * 60 * 60L)
            val addresses = records.map { it.address }
            cache[key] = CachedAddress(addresses, System.currentTimeMillis() + ttlSeconds * 1000L)
            onCacheChanged(cacheSnapshot())
            return addresses
        }
    }

    fun clearCache() {
        cache.clear()
        failedUntil.clear()
        dohUnavailableUntil = 0L
    }

    private fun cacheSnapshot(): Map<String, PersistedDohCacheEntry> =
        cache.mapValues { (_, entry) ->
            PersistedDohCacheEntry(
                addresses = entry.addresses.mapNotNull { it.hostAddress },
                expiresAt = entry.expiresAt,
            )
        }

    private fun lookupWithSystemDns(hostname: String): List<InetAddress> =
        Dns.SYSTEM.lookup(hostname).let { addresses ->
            if (preferIpv6) addresses else addresses.filterIsInstance<Inet4Address>()
        }.sortedWith(compareBy<InetAddress> { it is Inet6Address })

    fun close() {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun resolve(hostname: String, type: Int): List<DnsRecord> {
        val query = createQuery(hostname, type)
        val encoded = Base64.encodeToString(query, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val endpoint = server.endpointUrl.toHttpUrlOrNull()
            ?: throw UnknownHostException("DoH 地址无效")
        val request = Request.Builder()
            .url(endpoint.newBuilder().addQueryParameter("dns", encoded).build())
            .header("Accept", "application/dns-message")
            .header("User-Agent", "JM-Mobile-DoH")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw UnknownHostException("DoH 服务返回 HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: throw UnknownHostException("DoH 服务返回空数据")
            return parseResponse(bytes, type)
        }
    }

    private fun createQuery(hostname: String, type: Int): ByteArray {
        val labels = hostname.trimEnd('.').split('.')
        if (labels.isEmpty() || labels.any { it.isBlank() || it.length > 63 }) {
            throw UnknownHostException("域名格式无效：$hostname")
        }
        return ByteArrayOutputStream().apply {
            val id = random.nextInt(0x10000)
            writeU16(id)
            writeU16(0x0100) // Recursion desired.
            writeU16(1)
            writeU16(0)
            writeU16(0)
            writeU16(0)
            labels.forEach { label ->
                val bytes = label.toByteArray(Charsets.US_ASCII)
                write(bytes.size)
                write(bytes)
            }
            write(0)
            writeU16(type)
            writeU16(CLASS_IN)
        }.toByteArray()
    }

    private fun parseResponse(bytes: ByteArray, requestedType: Int): List<DnsRecord> {
        if (bytes.size < DNS_HEADER_SIZE) throw UnknownHostException("DoH 响应不完整")
        val flags = bytes.u16(2)
        if ((flags and 0x000f) != 0) throw UnknownHostException("DoH 解析失败，rcode=${flags and 0x000f}")
        val questionCount = bytes.u16(4)
        val answerCount = bytes.u16(6)
        var offset = DNS_HEADER_SIZE
        repeat(questionCount) {
            offset = bytes.skipName(offset)
            offset += 4
            if (offset > bytes.size) throw UnknownHostException("DoH 问题段无效")
        }
        val records = mutableListOf<DnsRecord>()
        repeat(answerCount) {
            offset = bytes.skipName(offset)
            if (offset + 10 > bytes.size) throw UnknownHostException("DoH 回答段无效")
            val type = bytes.u16(offset)
            val recordClass = bytes.u16(offset + 2)
            val ttl = bytes.u32(offset + 4)
            val length = bytes.u16(offset + 8)
            offset += 10
            if (offset + length > bytes.size) throw UnknownHostException("DoH 地址数据无效")
            if (recordClass == CLASS_IN && type == requestedType &&
                ((type == TYPE_A && length == 4) || (type == TYPE_AAAA && length == 16))
            ) {
                records += DnsRecord(InetAddress.getByAddress(bytes.copyOfRange(offset, offset + length)), ttl)
            }
            offset += length
        }
        return records
    }

    private data class DnsRecord(val address: InetAddress, val ttlSeconds: Long)

    private fun ByteArray.u16(offset: Int): Int =
        ((this[offset].toInt() and 0xff) shl 8) or (this[offset + 1].toInt() and 0xff)

    private fun ByteArray.u32(offset: Int): Long =
        ((this[offset].toLong() and 0xff) shl 24) or
            ((this[offset + 1].toLong() and 0xff) shl 16) or
            ((this[offset + 2].toLong() and 0xff) shl 8) or
            (this[offset + 3].toLong() and 0xff)

    private fun ByteArray.skipName(start: Int): Int {
        var offset = start
        while (offset < size) {
            val sizeByte = this[offset].toInt() and 0xff
            when {
                sizeByte == 0 -> return offset + 1
                sizeByte and 0xc0 == 0xc0 -> return offset + 2
                sizeByte and 0xc0 != 0 || offset + sizeByte >= size -> throw UnknownHostException("DoH 域名压缩格式无效")
                else -> offset += sizeByte + 1
            }
        }
        throw UnknownHostException("DoH 域名超出响应范围")
    }

    private fun ByteArrayOutputStream.writeU16(value: Int) {
        write((value shr 8) and 0xff)
        write(value and 0xff)
    }

    companion object {
        private const val DNS_HEADER_SIZE = 12
        private const val CLASS_IN = 1
        private const val TYPE_A = 1
        private const val TYPE_AAAA = 28
        private const val FAILURE_COOLDOWN_MS = 2_000L
        private const val DOH_FAILURE_COOLDOWN_MS = 30_000L
    }
}

private const val DOH_CACHE_STORAGE_PREFIX = "dohDnsCache_"
private const val MAX_PERSISTED_CACHE_ENTRIES = 64

private fun LocalSetting.toDohServer(): DohServer = resolveDohServer(
    selectedId = dohServerId,
    customName = dohCustomServerName,
    customUrl = dohCustomServerUrl,
)

private fun LocalSetting.dohResolverKey(sessionEnabled: Boolean): String = listOf(
    dohEnabled,
    sessionEnabled,
    dohServerId,
    dohCustomServerName,
    dohCustomServerUrl,
    dohUseDeviceCertificates,
    dohPreferIpv6,
).joinToString("|")

private fun LocalSetting.dohCacheStorageKey(): String =
    DOH_CACHE_STORAGE_PREFIX + listOf(
        toDohServer().id,
        toDohServer().endpointUrl,
        dohPreferIpv6,
    ).joinToString("|").hashCode().toUInt().toString(16)

private fun String.isIpLiteral(): Boolean =
    matches(Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}$")) || contains(':')
