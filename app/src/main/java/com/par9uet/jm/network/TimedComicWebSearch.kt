package com.par9uet.jm.network

import com.par9uet.jm.data.models.ComicRankCategory
import com.par9uet.jm.data.models.ComicRankPeriod
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Mobile /search ignores t; the website exposes it on /search/photos.
 * Keep this transport separate from API signing/cookies and never fall back to all-time search.
 */
const val TIMED_WEB_PAGE_SIZE = 12

class TimedComicWebSearch(
    sharedClient: OkHttpClient,
    private val fallbackHosts: (suspend () -> List<String>)? = null,
) {
    private val client = sharedClient.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()
    @Volatile private var workingHost: String? = null
    @Volatile private var publishedHosts: List<String> = emptyList()
    @Volatile private var publishedAt: Long = 0L

    suspend fun load(
        webHost: String,
        page: Int,
        query: String,
        mainTag: Int,
        period: ComicRankPeriod,
        category: ComicRankCategory? = null,
    ): NetWorkResult<ComicListResponse> {
        return try {
            val data = withTimeoutOrNull(20_000L) {
                suspend fun fromHost(host: String): ComicListResponse? = try {
                    val url = buildTimedComicSearchUrl(host, page, query, mainTag, period, category)
                    val html = client.newCall(webRequest(url)).awaitSearchHtml(url)
                    withContext(Dispatchers.Default) { parseTimedComicSearchPage(html, url, query) }
                        .also { workingHost = host }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
                val firstHost = workingHost ?: webHost
                fromHost(firstHost) ?: coroutineScope {
                    val alternatives = (fallbackHosts?.invoke() ?: loadPublishedHosts())
                        .filterNot { it.equals(firstHost, ignoreCase = true) }.take(2)
                        .map { host -> async { fromHost(host) } }.toMutableList()
                    try {
                        var found: ComicListResponse? = null
                        while (alternatives.isNotEmpty() && found == null) {
                            val (finished, result) = select {
                                alternatives.forEach { task -> task.onAwait { task to it } }
                            }
                            alternatives.remove(finished)
                            found = result
                        }
                        found
                    } finally {
                        alternatives.forEach { it.cancel() }
                    }
                }
            }
            if (data == null) NetWorkResult.Error("官网暂时无法提供本次${period.label}，请重试或切换“最新”。")
            else NetWorkResult.Success(data)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            NetWorkResult.Error("官网暂时无法提供本次${period.label}，请重试或切换“最新”。未使用全部时间的结果替代。")
        }
    }

    private suspend fun loadPublishedHosts(): List<String> {
        if (publishedHosts.isNotEmpty() && System.currentTimeMillis() - publishedAt < 60 * 60 * 1000L) return publishedHosts
        val url = requireNotNull("https://jmcomicgo.org/".toHttpUrlOrNull())
        val html = client.newCall(webRequest(url)).awaitSearchHtml(url)
        val domains = withContext(Dispatchers.Default) {
            Jsoup.parse(html).select("span").map { it.ownText().trim() }
                .filter { it.matches(Regex("[a-zA-Z0-9-]+\\.[a-zA-Z0-9.-]+")) }
                .filter { it.startsWith("comic18j-") || it.startsWith("jmcomic-") || it.startsWith("18comic.") }
                .distinct()
                // Direct alternatives avoid retrying the international domain that just failed.
                .sortedBy { if (it.startsWith("comic18j-") && it.endsWith(".work")) 0 else 1 }
        }
        publishedHosts = domains
        publishedAt = System.currentTimeMillis()
        return domains
    }

    private fun webRequest(url: HttpUrl): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        .header("Accept", "text/html")
        .build()
}

internal fun buildTimedComicSearchUrl(
    webHost: String,
    page: Int,
    query: String,
    mainTag: Int,
    period: ComicRankPeriod,
    category: ComicRankCategory? = null,
): HttpUrl {
    val host = webHost.trim().ifBlank { "18comic.vip" }
    val base = (if (host.contains("://")) host else "https://$host").toHttpUrlOrNull()
        ?: throw IOException("官网地址无效")
    require(base.isHttps && base.username.isEmpty() && base.password.isEmpty())
    return base.newBuilder()
        .encodedPath("/search/photos")
        .query(null)
        .fragment(null)
        .apply {
            category?.takeIf { it.supportsCategoryRanking }?.let { addPathSegment(it.apiValue) }
        }
        .addQueryParameter("main_tag", if (category != null) "0" else mainTag.toString())
        .addQueryParameter("search_query", query)
        .addQueryParameter("page", page.coerceAtLeast(1).toString())
        .addQueryParameter("o", "mv")
        .addQueryParameter("t", period.apiValue)
        .build()
}

private suspend fun Call.awaitSearchHtml(expected: HttpUrl): String =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val html = response.use {
                        if (!it.isSuccessful) throw IOException("官网请求失败：${it.code}")
                        val actual = it.request.url
                        if (actual.encodedPath.trimEnd('/') != expected.encodedPath.trimEnd('/') ||
                            listOf("main_tag", "search_query", "page", "o", "t").any { key ->
                                actual.queryParameter(key) != expected.queryParameter(key)
                            }
                        ) throw IOException("官网重定向丢失了搜索或时间条件")
                        it.body?.string() ?: throw IOException("官网响应为空")
                    }
                    if (continuation.isActive) continuation.resume(html)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        })
    }

internal fun parseTimedComicSearchPage(html: String, url: HttpUrl, query: String): ComicListResponse {
    val doc = Jsoup.parse(html, url.toString())
    if (doc.selectFirst("#challenge-form, #cf-challenge-running, .cf-turnstile, .g-recaptcha") != null ||
        doc.selectFirst(".alert-danger") != null
    ) throw IOException("官网验证页或错误页不能作为榜单")

    // Recommendations and ads also use thumb-overlay. Only this column owns search results.
    val results = doc.selectFirst("div.col-xs-12.col-md-9.col-sm-8")
        ?: throw IOException("未识别到官网搜索结果区")
    val summary = results.children().firstOrNull { it.hasClass("well") && it.hasClass("well-sm") }
        ?: throw IOException("未识别到官网搜索结果摘要")
    val emptyResult = summary.select(".text-danger").any {
        it.text().contains(Regex("没发现A漫|沒發現A漫|没有找到|沒有找到"))
    } || summary.select("h3").any { it.text().contains("找不到和") }
    if (emptyResult) return ComicListResponse(query, "0", null, emptyList())
    // Count is a direct child of the summary; a numeric keyword is nested one level deeper.
    val count = summary.children().firstOrNull {
        it.hasClass("text-white") && it.text().matches(Regex("[\\d,]+\\+?"))
    }?.text()?.takeUnless { it.endsWith("+") }?.replace(",", "")?.toIntOrNull()
        ?: throw IOException("未识别到官网搜索结果总数")
    val cards = results.select(".list-col > .well > .thumb-overlay, .list-col > .well > .thumb-overlay-albums")
    val items = cards.mapNotNull { card ->
        val link = card.select("a[href]").firstOrNull { it.attr("href").contains(Regex("/album/\\d+")) }
            ?: return@mapNotNull null
        val id = Regex("/album/(\\d+)").find(link.attr("href"))?.groupValues?.get(1)
            ?.takeIf { it.toIntOrNull()?.let { value -> value > 0 } == true } ?: return@mapNotNull null
        val image = link.selectFirst("img") ?: return@mapNotNull null
        val parent = card.parent() ?: return@mapNotNull null
        val name = image.attr("title").ifBlank { parent.selectFirst(".video-title")?.text().orEmpty() }
            .ifBlank { image.attr("alt") }
        if (name.isBlank()) return@mapNotNull null
        val cover = image.absUrl("data-original").ifBlank { image.absUrl("data-src") }.ifBlank { image.absUrl("src") }
        val authors = parent.select(".title-truncate:not(.tags):not(.video-title) a")
            .map { it.text().trim() }.filter { it.isNotEmpty() }.distinct()
        val tags = parent.select(".tags a").map { it.text().trim() }.filter { it.isNotEmpty() }.distinct()
        ComicListResponse.ContentListItem(
            id = id,
            author = authors.joinToString(", "),
            description = null,
            name = name,
            image = cover,
            category = ComicListResponse.ContentListItem.Category(null, null),
            category_sub = ComicListResponse.ContentListItem.Category(null, null),
            liked = false,
            is_favorite = false,
            update_at = 0,
            tags = tags,
        )
    }.distinctBy { it.id }
    if (cards.isNotEmpty() && items.size != cards.size) {
        throw IOException("官网漫画列表解析不完整")
    }
    if (items.isEmpty() && count != 0) {
        // A challenge, login, empty body or changed markup is not a successful empty result.
        throw IOException("未识别到官网搜索结果")
    }
    return ComicListResponse(
        search_query = query,
        total = count.toString(),
        redirect_aid = null,
        content = items,
    )
}
