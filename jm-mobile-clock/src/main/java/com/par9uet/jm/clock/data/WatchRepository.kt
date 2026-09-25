package com.par9uet.jm.clock.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.Log
import com.par9uet.jm.clock.network.applyWatchTlsCompat
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.api.enums.OrderBy
import io.github.jukomu.jmcomic.api.model.FavoriteQuery
import io.github.jukomu.jmcomic.api.model.JmAlbum
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta
import io.github.jukomu.jmcomic.api.model.JmUserInfo
import io.github.jukomu.jmcomic.api.model.SearchQuery
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Watch-only data layer.
 *
 * It intentionally has no image cache, download queue, database, or file-backed reader.
 * Account state and reading progress are user features; image bytes remain in memory only
 * and the current bitmap is released when the page changes.
 */
class WatchRepository {
    private val apiClientLock = Any()
    private var activeApiClient: JmApiClient? = null
    private val apiClient: JmApiClient
        get() = synchronized(apiClientLock) {
            activeApiClient ?: createApiClient().also { activeApiClient = it }
        }

    private val imageClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClient.Builder()
            .cache(null)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .applyWatchTlsCompat()
            .build()
    }

    suspend fun latest(): Result<List<WatchComic>> = ioResult {
        apiClient.getLatest(1).content().orEmpty()
            .mapNotNull { it.toWatchComic() }
            .distinctBy(WatchComic::id)
            .take(HOME_ITEM_LIMIT)
            .ifEmpty { error("首页暂未返回内容") }
    }

    suspend fun login(username: String, password: String): Result<WatchAccount> = ioResult {
        require(username.isNotBlank()) { "请输入用户名" }
        require(password.isNotBlank()) { "请输入密码" }
        apiClient.login(username.trim(), password).toWatchAccount()
    }

    fun clearSession() {
        synchronized(apiClientLock) {
            activeApiClient = null
        }
    }

    suspend fun favorites(page: Int): Result<WatchFavoritePage> = ioResult {
        require(page > 0) { "收藏页码无效" }
        val favoritePage = apiClient.getFavorites(
            FavoriteQuery.Builder()
                .folderId(0)
                .page(page)
                .build(),
        )
        val comics = favoritePage.content().orEmpty()
            .mapNotNull { it.toWatchComic() }
            .distinctBy(WatchComic::id)
        WatchFavoritePage(
            comics = comics,
            page = page,
            total = favoritePage.totalItems(),
        )
    }

    suspend fun toggleFavorite(comicId: Int): Result<Unit> = ioResult {
        require(comicId > 0) { "漫画编号无效" }
        apiClient.toggleAlbumFavorite(comicId.toString(), "0")
    }

    suspend fun search(query: String): Result<List<WatchComic>> = ioResult {
        val normalized = query.trim()
        require(normalized.isNotEmpty()) { "请输入搜索内容" }

        val directId = normalized
            .removePrefix("JM")
            .removePrefix("jm")
            .toIntOrNull()
        if (directId != null) {
            listOf(apiClient.getAlbum(directId.toString()).toWatchComic())
        } else {
            val searchQuery = SearchQuery.Builder()
                .text(normalized)
                .page(1)
                .orderBy(OrderBy.LATEST)
                .build()
            apiClient.search(searchQuery).content().orEmpty()
                .mapNotNull { it.toWatchComic() }
                .distinctBy(WatchComic::id)
                .take(SEARCH_ITEM_LIMIT)
                .ifEmpty { error("没有找到相关作品") }
        }
    }

    suspend fun detail(comicId: Int): Result<WatchComicDetail> = ioResult {
        require(comicId > 0) { "漫画编号无效" }
        apiClient.getAlbum(comicId.toString()).toWatchDetail()
    }

    suspend fun chapterPages(chapterId: Int): Result<WatchChapterPages> = ioResult {
        require(chapterId > 0) { "章节编号无效" }
        val photo = try {
            apiClient.getPhoto(chapterId.toString())
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        val directImages = photo?.images().orEmpty()
        val belongsToChapter: (List<io.github.jukomu.jmcomic.api.model.JmImage>) -> Boolean = { images ->
            images.isNotEmpty() && images.all { it.photoId() == chapterId.toString() }
        }
        val images = if (belongsToChapter(directImages)) {
            directImages
        } else {
            apiClient.getComicRead(chapterId.toString()).images().orEmpty()
                .takeIf(belongsToChapter)
                .orEmpty()
        }
        if (images.isEmpty()) error("该章节没有可在线阅读的图片")

        WatchChapterPages(
            chapterId = chapterId,
            scrambleId = photo?.scrambleId()?.toIntOrNull()
                ?: images.firstOrNull()?.scrambleId()?.toIntOrNull()
                ?: 0,
            urls = images.map { normalizeNestedUrl(it.getDownloadUrl()) },
        )
    }

    suspend fun loadCover(comic: WatchComic, targetWidthPx: Int): Result<Bitmap> = ioResult {
        val bytes = downloadFirst(coverUrls(comic))
        decodeSampledBitmap(bytes, targetWidthPx)
            ?: error("封面解码失败")
    }

    suspend fun loadPage(
        chapterId: Int,
        pageUrl: String,
        scrambleId: Int,
        targetWidthPx: Int,
    ): Result<Bitmap> = ioResult {
        val bytes = downloadFirst(imageUrlCandidates(pageUrl))
        val original = decodeSampledBitmap(bytes, targetWidthPx)
            ?: error("图片解码失败")
        if (pageUrl.substringBefore('?').endsWith(".gif", ignoreCase = true) ||
            chapterId <= scrambleId
        ) {
            original
        } else {
            descramblePage(original, chapterId, extractPageToken(pageUrl))
        }
    }

    fun coverUrls(comic: WatchComic): List<String> {
        val source = comic.coverPath.trim()
        val relative = source.takeIf {
            it.isNotBlank() && !it.isAbsoluteHttpUrl() && !it.startsWith("//")
        }
            ?.let { if (it.startsWith('/')) it else "/$it" }
        return buildList {
            when {
                source.isAbsoluteHttpUrl() -> add(source)
                source.startsWith("//") -> add("https:$source")
            }
            if (relative != null) {
                IMAGE_HOSTS.forEach { add("https://$it$relative") }
            }
            IMAGE_HOSTS.forEach { add("https://$it/media/albums/${comic.id}_3x4.jpg") }
        }.distinct()
    }

    private fun createApiClient(): JmApiClient {
        val legacyAndroid = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
        val configBuilder = JmConfiguration.Builder()
            .clientType(ClientType.API)
            .timeout(Duration.ofSeconds(12))
            .imageTimeout(Duration.ofSeconds(45))
            .downloadThreadPoolSize(1)
            .domainProbeTimeoutMs(1500)
        if (legacyAndroid) {
            configBuilder.apiDomains(LEGACY_API_DOMAINS)
        }
        val config = configBuilder.build()
        val context = OkHttpBuilder.build(config)
        val client = context.client.newBuilder()
            .cache(null)
            .applyWatchTlsCompat()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Cache-Control", "no-store, no-cache, max-age=0")
                    .header("Pragma", "no-cache")
                    .build()
                chain.proceed(request).newBuilder()
                    .header("Cache-Control", "no-store")
                    .build()
            }
            .build()
        val jmClient = JmApiClient(
            config,
            client,
            context.cookieManager,
            context.domainManager,
        )

        if (legacyAndroid) {
            Thread({
                try {
                    Thread.sleep(8000)
                    if (!context.domainManager.isInitialized) {
                        context.domainManager.setInitialized(true)
                    }
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }, "jm-clock-domain-guard").apply {
                isDaemon = true
                start()
            }
        }
        return jmClient
    }

    private suspend fun <T> ioResult(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            currentCoroutineContext().ensureActive()
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.w("JmClockRepository", error.message, error)
            Result.failure(error)
        }
    }

    private fun JmAlbumMeta.toWatchComic(): WatchComic? {
        val comicId = id().orEmpty().toIntOrNull() ?: return null
        return WatchComic(
            id = comicId,
            title = title().orEmpty().ifBlank { "JM$comicId" },
            author = authors().orEmpty().joinToString("、").ifBlank { "未知作者" },
            coverPath = image().orEmpty(),
            description = description().orEmpty(),
        )
    }

    private fun JmAlbum.toWatchComic(): WatchComic {
        val comicId = id().orEmpty().toIntOrNull() ?: error("漫画编号无效")
        return WatchComic(
            id = comicId,
            title = title().orEmpty().ifBlank { "JM$comicId" },
            author = authors().orEmpty().joinToString("、").ifBlank { "未知作者" },
            coverPath = image().orEmpty(),
            description = description().orEmpty(),
        )
    }

    private fun JmAlbum.toWatchDetail(): WatchComicDetail {
        val comicId = id().orEmpty().toIntOrNull() ?: error("漫画编号无效")
        val chapters = photoMetas().orEmpty()
            .sortedBy { it.sortOrder() }
            .mapNotNull { photo ->
                val chapterId = photo.id().orEmpty().toIntOrNull() ?: return@mapNotNull null
                WatchChapter(
                    id = chapterId,
                    title = photo.title().orEmpty().ifBlank { "章节 $chapterId" },
                )
            }
        return WatchComicDetail(
            id = comicId,
            title = title().orEmpty().ifBlank { "JM$comicId" },
            description = description().orEmpty(),
            authors = authors().orEmpty().filter(String::isNotBlank),
            tags = tags().orEmpty().filter(String::isNotBlank).distinct(),
            coverPath = image().orEmpty(),
            chapters = chapters,
            isFavorite = isFavorite,
        )
    }

    private fun JmUserInfo.toWatchAccount(): WatchAccount = WatchAccount(
        id = uid.toIntOrNull() ?: 0,
        username = username,
        avatarUrl = avatarUrl,
        level = level,
        levelName = levelName,
        favoriteCount = albumFavorites,
        favoriteLimit = maxAlbumFavorites,
        coins = coin.toString().toIntOrNull() ?: 0,
    )

    private fun downloadFirst(urls: List<String>): ByteArray {
        var lastError: Throwable? = null
        urls.forEach { url ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", WATCH_USER_AGENT)
                    .header("Referer", "https://18comic.vip")
                    .header("Cache-Control", "no-store, no-cache, max-age=0")
                    .header("Pragma", "no-cache")
                    .build()
                imageClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = IllegalStateException("HTTP ${response.code}")
                        return@use
                    }
                    val bytes = response.body?.bytes()
                    if (bytes == null || bytes.isEmpty()) {
                        lastError = IllegalStateException("服务器返回空图片")
                    } else if (isHtmlResponse(response.header("Content-Type"), bytes)) {
                        lastError = IllegalStateException("图片地址返回了网页")
                    } else {
                        return bytes
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("没有可用的图片地址")
    }

    private fun decodeSampledBitmap(bytes: ByteArray, targetWidthPx: Int): Bitmap? {
        val safeTargetWidth = max(targetWidthPx, 240)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        val maxPixels = safeTargetWidth.toLong() * safeTargetWidth.toLong() * 6L
        while (
            bounds.outWidth / sampleSize > safeTargetWidth * 2 ||
            bounds.outWidth.toLong() / sampleSize * (bounds.outHeight.toLong() / sampleSize) > maxPixels
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
            inDither = true
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun descramblePage(original: Bitmap, chapterId: Int, pageToken: String): Bitmap {
        val seed = calculateSeed(chapterId, pageToken)
        val width = original.width
        val height = original.height
        if (height < seed) return original

        val remainder = height % seed
        val decoded = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(decoded)
        val paint = Paint().apply { isAntiAlias = false }
        for (index in 0 until seed) {
            var sliceHeight = height / seed
            var destinationY = sliceHeight * index
            val sourceY = height - sliceHeight * (index + 1) - remainder
            if (index == 0) {
                sliceHeight += remainder
            } else {
                destinationY += remainder
            }
            canvas.drawBitmap(
                original,
                Rect(0, sourceY, width, sourceY + sliceHeight),
                Rect(0, destinationY, width, destinationY + sliceHeight),
                paint,
            )
        }
        if (!original.isRecycled) original.recycle()
        return decoded
    }

    private fun calculateSeed(chapterId: Int, pageToken: String): Int {
        val hash = java.security.MessageDigest.getInstance("MD5")
            .digest("$chapterId$pageToken".toByteArray())
            .joinToString("") { "%02x".format(it) }
        var code = hash.last().code
        when {
            chapterId in 268850..421925 -> code %= 10
            chapterId >= 421926 -> code %= 8
        }
        return SEED_MAP.getOrNull(code) ?: 10
    }

    private fun extractPageToken(url: String): String =
        url.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')

    private fun imageUrlCandidates(url: String): List<String> {
        val normalized = normalizeNestedUrl(url)
        val parsed = normalized.toHttpUrlOrNull() ?: return listOf(normalized)
        if (!parsed.encodedPath.contains("/media/photos/")) return listOf(normalized)
        return buildList {
            add(normalized)
            IMAGE_HOSTS.forEach { host ->
                if (!host.equals(parsed.host, ignoreCase = true)) {
                    add(parsed.newBuilder().host(host).build().toString())
                }
            }
        }.distinct()
    }

    private fun normalizeNestedUrl(url: String): String {
        val value = url.trim()
        val httpsIndex = value.lastIndexOf("https://")
        val httpIndex = value.lastIndexOf("http://")
        val nestedIndex = max(httpsIndex, httpIndex)
        return if (nestedIndex > 0) value.substring(nestedIndex) else value
    }

    private fun isHtmlResponse(contentType: String?, bytes: ByteArray): Boolean {
        if (contentType?.contains("text/html", ignoreCase = true) == true) return true
        val prefix = String(bytes, 0, minOf(bytes.size, 64), Charsets.UTF_8).trimStart()
        return prefix.startsWith("<!doctype", ignoreCase = true) ||
            prefix.startsWith("<html", ignoreCase = true)
    }

    private fun String.isAbsoluteHttpUrl(): Boolean =
        startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

    private companion object {
        const val HOME_ITEM_LIMIT = 12
        const val SEARCH_ITEM_LIMIT = 30
        const val WATCH_USER_AGENT =
            "Mozilla/5.0 (Linux; Android; Wear OS) AppleWebKit/537.36 Mobile Safari/537.36"

        val SEED_MAP = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20)
        val IMAGE_HOSTS = listOf(
            "cdn-msp.jmapiproxy1.cc",
            "cdn-msp.jmapiproxy2.cc",
            "cdn-msp2.jmapiproxy2.cc",
            "cdn-msp3.jmapiproxy2.cc",
            "cdn-msp.jmapinodeudzn.net",
            "cdn-msp3.jmapinodeudzn.net",
        )
        val LEGACY_API_DOMAINS = listOf(
            "www.cdnaspa.vip",
            "www.cdnaspa.club",
            "www.cdnplaystation6.org",
            "www.cdnplaystation6.vip",
            "www.cdnplaystation6.cc",
        )
    }
}
