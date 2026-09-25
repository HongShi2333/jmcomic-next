package com.par9uet.jm.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.data.models.AiChatModel
import com.par9uet.jm.data.models.AiSearchEngine
import com.par9uet.jm.data.models.AiSearchEngineProvider
import com.par9uet.jm.data.models.AiSearchSettings
import com.par9uet.jm.network.DohManager
import com.par9uet.jm.utils.applyTlsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 联网搜索结构化结果项：用于 UI 引用卡片展示与上下文拼接。
 */
data class WebSearchResult(
    val title: String,
    val snippet: String,
    val url: String
)

/**
 * 联网搜索产物：拼接好的上下文字符串 + 结构化结果列表。
 */
data class WebSearchContext(
    val text: String,
    val results: List<WebSearchResult>
) {
    companion object {
        val EMPTY = WebSearchContext(text = "", results = emptyList())
    }
}

/**
 * 搜索进度阶段：用于 UI 可视化（类似 Lobe Chat）。
 */
sealed class SearchProgress {
    /** 开始搜索：携带最终查询词 */
    data class Start(val query: String) : SearchProgress()
    /** 找到 N 条结果 */
    data class Found(val count: Int, val results: List<WebSearchResult>) : SearchProgress()
    /** 完成上下文构建 */
    data class Done(val context: WebSearchContext) : SearchProgress()
    /** 失败：携带错误信息（不抛异常，由调用方决定如何呈现） */
    data class Failed(val message: String) : SearchProgress()
}

class AiChatRepository(
    private val gson: Gson,
    private val dohManager: DohManager,
) {
    companion object {
        private const val TARGET_API = "https://app.unlimitedai.chat/api/chat"
        private const val DEVICE_ID = ""
        private const val COOKIES = ""
        private const val NOTRACK_BASE = "https://notrack.ai"
        private const val NOTRACK_CHAT_PAGE = "$NOTRACK_BASE/zh-CN/chat"
        private const val NOTRACK_DISPATCH = "$NOTRACK_BASE/api/dispatch"
        private const val NOTRACK_MODEL = "C"
        const val THINK_OPEN = "\u003Cthink\u003E"
        const val THINK_CLOSE = "\u003C/think\u003E"

        private const val TAVILY_ENDPOINT = "https://api.tavily.com/search"
        private const val DUCKDUCKGO_ENDPOINT = "https://html.duckduckgo.com/html/"

        /**
         * 触发 AI 自主联网搜索的关键词集合。
         * 当 [AiSearchSettings.aiAutoSearch] 为 true 且用户消息命中任一关键词时，自动联网。
         */
        private val AUTO_SEARCH_KEYWORDS = listOf(
            "最新", "今天", "现在", "目前", "当前", "近期", "最近",
            "新闻", "新闻联播", "时讯", "时事",
            "2024", "2025", "2026", "2027",
            "价格", "股价", "汇率", "金价", "油价", "行情",
            "版本", "发布", "更新", "升级", "Release",
            "政策", "法规", "法案", "新规",
            "比赛", "赛果", "比分", "战报", "战绩",
            "天气", "气温", "降水",
            "职位", "任职", "担任", "辞去",
            "逝世", "去世", "去世了", "出生",
            "票房", "收视率", "排名", "榜单",
            "公告", "通知", "声明", "公报"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .dns(dohManager)
        .applyTlsCompat()
        .build()

    private val searchClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .dns(dohManager)
        .applyTlsCompat()
        .build()

    @Volatile
    private var noTrackCookie: String = ""

    /**
     * 判断用户消息是否应该触发联网搜索（AI 自主决策）。
     *
     * 规则：
     * - [manualEnabled] 用户手动开启联网搜索 -> true
     * - [settings.aiAutoSearch] 关闭 -> false
     * - 命中 [AUTO_SEARCH_KEYWORDS] 任一关键词 -> true
     * - 否则 false
     */
    fun shouldAutoSearch(text: String, manualEnabled: Boolean, settings: AiSearchSettings): Boolean {
        if (manualEnabled) return true
        if (!settings.aiAutoSearch) return false
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return false
        return AUTO_SEARCH_KEYWORDS.any { keyword ->
            normalized.contains(keyword.lowercase())
        }
    }

    /**
     * 联网搜索（带进度回调）：类似 Lobe Chat，过程可视化。
     *
     * 调用方通过 [onProgress] 接收 4 个阶段：Start → Found → Done（或 Failed）。
     * 失败时不抛异常，由调用方根据 [SearchProgress.Failed] 决定后续行为。
     */
    suspend fun searchWebContextWithProgress(
        query: String,
        settings: AiSearchSettings,
        onProgress: suspend (SearchProgress) -> Unit
    ): WebSearchContext = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            onProgress(SearchProgress.Failed("查询为空"))
            return@withContext WebSearchContext.EMPTY
        }
        val normalized = settings.normalized()
        onProgress(SearchProgress.Start(query))

        val results = runCatching { searchByProvider(query, normalized) }
            .getOrElse {
                onProgress(SearchProgress.Failed(it.message ?: "搜索失败"))
                return@withContext WebSearchContext.EMPTY
            }

        if (results.isEmpty()) {
            onProgress(SearchProgress.Failed("未找到可用结果"))
            return@withContext WebSearchContext.EMPTY
        }

        onProgress(SearchProgress.Found(results.size, results))
        val context = buildContextFromResults(query, results)
        onProgress(SearchProgress.Done(context))
        context
    }

    /**
     * 兼容旧调用方：无进度回调的搜索接口。
     */
    suspend fun searchWebContext(
        query: String,
        settings: AiSearchSettings
    ): String? {
        val context = searchWebContextWithProgress(query, settings) { /* no-op */ }
        return context.takeIf { it.text.isNotBlank() }?.text
    }

    private suspend fun searchByProvider(
        query: String,
        settings: AiSearchSettings
    ): List<WebSearchResult> {
        return when (settings.provider) {
            AiSearchEngineProvider.TAVILY -> {
                if (settings.tavilyReady) searchTavily(query, settings)
                else emptyList()
            }
            AiSearchEngineProvider.DUCKDUCKGO -> searchDuckDuckGo(query, settings.resultCount)
            AiSearchEngineProvider.BING -> parseBing(fetchSearchPage(bingUrl(query)) ?: "", settings.resultCount)
            AiSearchEngineProvider.SOGOU -> parseSogou(fetchSearchPage(sogouUrl(query)) ?: "", settings.resultCount)
            AiSearchEngineProvider.BAIDU -> parseBaidu(fetchSearchPage(baiduUrl(query, settings.resultCount)) ?: "", settings.resultCount)
            AiSearchEngineProvider.SEARXNG -> parseSearxng(fetchSearchPage(searxngUrl(query, settings)) ?: "", settings.resultCount)
            AiSearchEngineProvider.AUTO -> {
                // 自动模式：按可用性顺序尝试
                if (settings.tavilyReady) {
                    searchTavily(query, settings).takeIf { it.isNotEmpty() }
                        ?: searchDuckDuckGo(query, settings.resultCount).takeIf { it.isNotEmpty() }
                        ?: parseBing(fetchSearchPage(bingUrl(query)) ?: "", settings.resultCount).takeIf { it.isNotEmpty() }
                        ?: parseSogou(fetchSearchPage(sogouUrl(query)) ?: "", settings.resultCount).takeIf { it.isNotEmpty() }
                        ?: parseBaidu(fetchSearchPage(baiduUrl(query, settings.resultCount)) ?: "", settings.resultCount)
                } else {
                    searchDuckDuckGo(query, settings.resultCount).takeIf { it.isNotEmpty() }
                        ?: parseBing(fetchSearchPage(bingUrl(query)) ?: "", settings.resultCount).takeIf { it.isNotEmpty() }
                        ?: parseSogou(fetchSearchPage(sogouUrl(query)) ?: "", settings.resultCount).takeIf { it.isNotEmpty() }
                        ?: parseBaidu(fetchSearchPage(baiduUrl(query, settings.resultCount)) ?: "", settings.resultCount)
                }
            }
        }
    }

    // ============== Tavily ==============

    private fun searchTavily(query: String, settings: AiSearchSettings): List<WebSearchResult> {
        val payload = JsonObject().apply {
            addProperty("api_key", settings.tavilyApiKey)
            addProperty("query", query)
            addProperty("search_depth", settings.tavilySearchDepth)
            addProperty("max_results", settings.resultCount)
            addProperty("include_answer", "basic")
            addProperty("include_raw_content", false)
        }
        val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(TAVILY_ENDPOINT)
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .post(body)
            .build()
        return searchClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use emptyList()
            val text = response.body?.string() ?: return@use emptyList()
            val root = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return@use emptyList()
            val results = root.getAsJsonArray("results") ?: return@use emptyList()
            results.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val obj = element.asJsonObject
                val title = obj.stringValue("title").orEmpty().stripHtml()
                val url = obj.stringValue("url").orEmpty().trim()
                val content = obj.stringValue("content").orEmpty().stripHtml().take(280)
                if (url.isBlank() || title.isBlank()) null
                else WebSearchResult(title = title, snippet = content, url = url)
            }.distinctBy { it.url }.take(settings.resultCount)
        }
    }

    // ============== DuckDuckGo ==============

    private fun searchDuckDuckGo(query: String, resultCount: Int): List<WebSearchResult> {
        val url = DUCKDUCKGO_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("kl", "cn-zh")
            .build()
        val html = fetchSearchPage(
            url = url.toString(),
            accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        ) ?: return emptyList()
        return parseDuckDuckGo(html, resultCount)
    }

    private fun parseDuckDuckGo(html: String, resultCount: Int): List<WebSearchResult> {
        // DuckDuckGo HTML 版结果块：<a class="result__a" href="...">title</a>
        // 摘要：<a class="result__snippet">...</a>
        val linkRegex = Regex(
            """<a[^>]+class="[^"]*result__a[^"]*"[^>]+href="([^"]+)"[^>]*>([\s\S]*?)</a>""",
            RegexOption.IGNORE_CASE
        )
        val snippetRegex = Regex(
            """<a[^>]+class="[^"]*result__snippet[^"]*"[^>]*>([\s\S]*?)</a>""",
            RegexOption.IGNORE_CASE
        )
        val results = linkRegex.findAll(html).mapNotNull { match ->
            val rawUrl = match.groupValues[1].decodeHtml().trim()
            val title = match.groupValues[2].stripHtml()
            // DuckDuckGo 的跳转链接形如 //duckduckgo.com/l/?uddg=<encoded>
            val resolved = resolveDdgRedirect(rawUrl)
            if (resolved.isBlank() || title.isBlank()) return@mapNotNull null
            // 在链接附近找摘要
            val tail = html.substring(match.range.last, minOf(html.length, match.range.last + 800))
            val snippet = snippetRegex.find(tail)?.groupValues?.get(1)?.stripHtml().orEmpty()
            WebSearchResult(title = title, snippet = snippet, url = resolved)
        }.distinctBy { it.url }.take(resultCount).toList()
        return results
    }

    private fun resolveDdgRedirect(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("//")) return "https:$url"
        // 解析 uddg= 参数
        val match = Regex("""uddg=([^&]+)""").find(url) ?: return url
        return runCatching { java.net.URLDecoder.decode(match.groupValues[1], "UTF-8") }.getOrDefault(url)
    }

    // ============== 传统搜索引擎 ==============

    private fun bingUrl(query: String): String =
        "https://cn.bing.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("setlang", "zh-CN")
            .addQueryParameter("cc", "CN")
            .build().toString()

    private fun sogouUrl(query: String): String =
        "https://www.sogou.com/web".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("ie", "utf8")
            .build().toString()

    private fun baiduUrl(query: String, resultCount: Int): String =
        "https://www.baidu.com/s".toHttpUrl().newBuilder()
            .addQueryParameter("wd", query)
            .addQueryParameter("rn", resultCount.toString())
            .addQueryParameter("ie", "utf-8")
            .build().toString()

    private fun searxngUrl(query: String, settings: AiSearchSettings): String {
        val endpoint = settings.searxngBaseUrl.toSearxngSearchEndpoint() ?: return ""
        return endpoint.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .addQueryParameter("language", settings.searxngLanguage)
            .addQueryParameter("categories", settings.searxngCategories)
            .build().toString()
    }

    private fun fetchSearchPage(
        url: String,
        accept: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    ): String? {
        if (url.isBlank()) return null
        val request = Request.Builder()
            .url(url)
            .header("accept", accept)
            .header("accept-language", "zh-CN,zh;q=0.9")
            .header(
                "user-agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Mobile Safari/537.36"
            )
            .get()
            .build()
        return searchClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body?.string()
        }
    }

    private fun parseBing(html: String, resultCount: Int): List<WebSearchResult> {
        val blockRegex = Regex("""<li\s+class="b_algo"[\s\S]*?</li>""", RegexOption.IGNORE_CASE)
        val linkRegex = Regex("""<h2[^>]*>[\s\S]*?<a[^>]+href="([^"]+)"[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
        val snippetRegex = Regex("""<p[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
        return blockRegex.findAll(html).mapNotNull { block ->
            val link = linkRegex.find(block.value) ?: return@mapNotNull null
            val url = link.groupValues[1].decodeHtml().trim()
            val title = link.groupValues[2].stripHtml()
            val snippet = snippetRegex.find(block.value)?.groupValues?.get(1)?.stripHtml().orEmpty()
            if (url.isBlank() || title.isBlank()) return@mapNotNull null
            WebSearchResult(title = title, snippet = snippet, url = url)
        }.distinctBy { it.url }.take(resultCount).toList()
    }

    private fun parseSogou(html: String, resultCount: Int): List<WebSearchResult> {
        val linkRegex = Regex(
            """<h3[^>]*>[\s\S]*?<a[^>]+href="([^"]+)"[^>]*>([\s\S]*?)</a>[\s\S]*?</h3>""",
            RegexOption.IGNORE_CASE
        )
        return linkRegex.findAll(html).mapNotNull { match ->
            val url = match.groupValues[1].decodeHtml().trim()
            val title = match.groupValues[2].stripHtml()
            val blockEnd = minOf(html.length, match.range.last + 520)
            val text = html.substring(match.range.first, blockEnd)
                .stripHtml()
                .removePrefix(title)
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(180)
            if (!url.startsWith("http") || title.isBlank()) return@mapNotNull null
            WebSearchResult(title = title, snippet = text, url = url)
        }.distinctBy { it.url }.take(resultCount).toList()
    }

    private fun parseBaidu(html: String, resultCount: Int): List<WebSearchResult> {
        val blockRegex = Regex(
            """<div[^>]+class="[^"]*(?:result|c-container)[^"]*"[\s\S]*?(?=<div[^>]+class="[^"]*(?:result|c-container)|$)""",
            RegexOption.IGNORE_CASE
        )
        val linkRegex = Regex(
            """<h3[^>]*>[\s\S]*?<a[^>]+href="([^"]+)"[^>]*>([\s\S]*?)</a>[\s\S]*?</h3>""",
            RegexOption.IGNORE_CASE
        )
        return blockRegex.findAll(html).mapNotNull { block ->
            val link = linkRegex.find(block.value) ?: return@mapNotNull null
            val url = link.groupValues[1].decodeHtml().trim()
            val title = link.groupValues[2].stripHtml()
            val text = block.value.stripHtml()
                .removePrefix(title)
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(180)
            if (url.isBlank() || title.isBlank()) return@mapNotNull null
            WebSearchResult(title = title, snippet = text, url = url)
        }.distinctBy { it.url }.take(resultCount).toList()
    }

    private fun parseSearxng(html: String, resultCount: Int): List<WebSearchResult> {
        val root = runCatching { JsonParser.parseString(html).asJsonObject }.getOrNull() ?: return emptyList()
        val results = root.getAsJsonArray("results") ?: return emptyList()
        return results.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val item = element.asJsonObject
            val title = item.stringValue("title").orEmpty().stripHtml()
            val url = item.stringValue("url").orEmpty().trim()
            val text = item.stringValue("content").orEmpty().stripHtml().take(180)
            if (!url.startsWith("http") || title.isBlank()) return@mapNotNull null
            WebSearchResult(title = title, snippet = text, url = url)
        }.distinctBy { it.url }.take(resultCount).toList()
    }

    private fun buildContextFromResults(query: String, results: List<WebSearchResult>): WebSearchContext {
        val text = results.mapIndexed { index, item ->
            val snippet = if (item.snippet.isBlank()) "" else " - ${item.snippet}"
            "${index + 1}. ${item.title}$snippet 来源：${item.url}"
        }.joinToString("\n")
        return WebSearchContext(text = text, results = results)
    }

    // ============== AI 流式对话 ==============

    suspend fun streamChat(
        model: String = AiChatModel.UnlimitedAi.id,
        messages: List<OpenAiChatMessage>,
        onDelta: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        when (AiChatModel.fromId(model)) {
            AiChatModel.UnlimitedAi -> try {
                streamUnlimitedChat(messages, onDelta)
            } catch (_: AiRateLimitedException) {
                // 限流时自动切换到匿名备用模型，避免伪造或随机生成鉴权数据。
                streamNoTrackChat(messages, onDelta)
            }
            AiChatModel.NoTrackAi -> streamNoTrackChat(messages, onDelta)
        }
    }

    private suspend fun streamUnlimitedChat(
        messages: List<OpenAiChatMessage>,
        onDelta: suspend (String) -> Unit
    ) {
        val body = gson.toJson(toUpstreamPayload(messages))
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(TARGET_API)
            .header("accept", "*/*")
            .header("content-type", "application/json")
            .header("cookie", COOKIES)
            .header("origin", "https://app.unlimitedai.chat")
            .header("referer", "https://app.unlimitedai.chat/zh")
            .header(
                "user-agent",
                "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome Mobile Safari/537.36"
            )
            .header("x-next-intl-locale", "zh")
            .header("sec-fetch-dest", "empty")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-site", "same-origin")
            .post(body)
            .build()

        for (attempt in 0 until 3) {
            val response = client.newCall(request).execute()
            if (response.code == 429) {
                response.close()
                if (attempt < 2) {
                    delay(1_000L shl attempt)
                    continue
                }
                throw AiRateLimitedException()
            }

            response.use { response ->
            val responseBody = response.body ?: throw IllegalStateException("AI 服务返回空响应")
            if (!response.isSuccessful) {
                val message = responseBody.string().ifBlank { "HTTP ${response.code}" }
                throw IllegalStateException("AI 请求失败：$message")
            }

            val bodySource = responseBody.source()
            var inReasoning = false
            while (!bodySource.exhausted()) {
                val line = bodySource.readUtf8Line() ?: continue
                val text = line.trim()
                if (text.isBlank()) continue
                if (text.startsWith("<!DOCTYPE") || text.startsWith("<html")) {
                    throw IllegalStateException("AI 服务返回 HTML，可能是上游限制或 Cookie 失效")
                }

                val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
                    ?: continue
                if (json.has("error")) {
                    val message = json.getAsJsonObject("error")
                        ?.get("message")
                        ?.asString
                        ?: "AI 服务返回错误"
                    throw IllegalStateException(message)
                }

                val type = json.get("type")?.asString.orEmpty()
                val delta = json.stringValue("delta", "text", "content").orEmpty()
                val isReasoning = type.contains("reason", ignoreCase = true)

                if (isReasoning && !inReasoning) {
                    onDelta(THINK_OPEN)
                    inReasoning = true
                }
                if (!isReasoning && inReasoning) {
                    onDelta(THINK_CLOSE)
                    inReasoning = false
                }

                if (delta.isNotEmpty()) {
                    onDelta(delta)
                }
            }
            if (inReasoning) {
                onDelta(THINK_CLOSE)
            }
            }
            return
        }
    }

    /**
     * 直接调用 NoTrack 的匿名聊天接口。Cookie 只从聊天页的 Set-Cookie 中读取，
     * 不生成、不伪造鉴权数据；401/403 时丢弃旧 uid 并重新获取一次。
     */
    private fun toNoTrackInput(messages: List<OpenAiChatMessage>): String {
        val validMessages = messages.filter { it.content.isNotBlank() }
        if (validMessages.isEmpty()) return ""
        if (validMessages.size == 1 && validMessages.first().role == "user") {
            return validMessages.first().content.take(3_800)
        }

        val systemText = validMessages
            .filter { it.role == "system" }
            .joinToString("\n\n") { it.content.trim() }
        val latestUser = validMessages.lastOrNull { it.role == "user" }
        val latestUserLine = latestUser?.let { "[User]\n${it.content.trim()}" }.orEmpty()
        val recentMessages = validMessages
            .filter { it.role != "system" && it !== latestUser }
            .asReversed()
        val selectedRecent = mutableListOf<String>()
        var remaining = 3_800 - systemText.length - latestUserLine.length - 8
        for (message in recentMessages) {
            if (remaining <= 100) break
            val role = message.role.replaceFirstChar { it.uppercase() }
            val line = "[$role]\n${message.content.trim()}"
            if (line.length > remaining) break
            selectedRecent.add(0, line)
            remaining -= line.length + 4
        }

        val parts = buildList {
            if (systemText.isNotBlank()) add("[System]\n$systemText")
            addAll(selectedRecent)
            if (latestUserLine.isNotBlank()) add(latestUserLine)
        }
        return parts.joinToString("\n\n").take(3_800)
    }

    private suspend fun streamNoTrackChat(
        messages: List<OpenAiChatMessage>,
        onDelta: suspend (String) -> Unit
    ) {
        val payload = mapOf<String, Any?>(
            "user_input" to toNoTrackInput(messages),
            "mode" to "usual",
            "model" to NOTRACK_MODEL,
            "persona" to "normal",
            "max_turns" to 6,
            "chat_id" to null,
            "attachments" to emptyList<Any>(),
            "regenerate" to false,
            "edit" to false,
            "edit_mid" to null
        )
        val body = gson.toJson(payload)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        for (attempt in 0 until 2) {
            if (noTrackCookie.isBlank()) {
                refreshNoTrackCookie()
            }
            val requestBuilder = Request.Builder()
                .url(NOTRACK_DISPATCH)
                .header("accept", "text/event-stream")
                .header("content-type", "application/json")
                .header("origin", NOTRACK_BASE)
                .header("referer", NOTRACK_CHAT_PAGE)
                .header(
                    "user-agent",
                    "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome Mobile Safari/537.36"
                )
                .post(body)
            if (noTrackCookie.isNotBlank()) {
                requestBuilder.header("cookie", noTrackCookie)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.code == 401 || response.code == 403) {
                response.close()
                noTrackCookie = ""
                if (attempt == 0) continue
                throw IllegalStateException("NoTrack AI 身份已失效，请稍后重试")
            }

            response.use { response ->
                val responseBody = response.body ?: throw IllegalStateException("NoTrack AI 返回空响应")
                if (!response.isSuccessful) {
                    val message = responseBody.string().ifBlank { "HTTP ${response.code}" }
                    throw IllegalStateException("NoTrack AI 请求失败：$message")
                }

                val source = responseBody.source()
                var inReasoning = false
                var emittedContent = false
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    val raw = line.trim()
                    if (raw.isBlank() || raw.startsWith(":")) continue
                    val data = when {
                        raw.startsWith("data:") -> raw.substringAfter("data:").trim()
                        raw.startsWith("{") -> raw
                        else -> continue
                    }
                    if (data.isBlank() || data == "[DONE]") continue
                    val json = runCatching { JsonParser.parseString(data).asJsonObject }.getOrNull()
                        ?: continue
                    if (json.has("error")) {
                        val message = json.getAsJsonObject("error")
                            ?.get("message")
                            ?.asString
                            ?: "NoTrack AI 返回错误"
                        throw IllegalStateException(message)
                    }

                    val type = json.stringValue("type").orEmpty()
                    if (type.equals("done", ignoreCase = true)) break
                    if (type.equals("chat_meta", ignoreCase = true) ||
                        type.equals("user", ignoreCase = true)
                    ) continue
                    val isReasoning = type.contains("thinking", ignoreCase = true) ||
                        type.contains("reason", ignoreCase = true)
                    if (isReasoning && !inReasoning) {
                        onDelta(THINK_OPEN)
                        inReasoning = true
                    } else if (!isReasoning && inReasoning) {
                        onDelta(THINK_CLOSE)
                        inReasoning = false
                    }

                    val choices = json.getAsJsonArray("choices")
                    val choiceDelta = if (choices != null && choices.size() > 0 && choices[0].isJsonObject) {
                        choices[0].asJsonObject.getAsJsonObject("delta")
                            ?.stringValue("content", "text")
                            .orEmpty()
                    } else {
                        ""
                    }
                    val delta = json.stringValue("chunk", "delta", "content", "text")
                        .orEmpty()
                        .ifBlank { choiceDelta }
                    if (delta.isNotEmpty() && (type != "message" || !emittedContent)) {
                        onDelta(delta)
                        emittedContent = true
                    }
                }
                if (inReasoning) onDelta(THINK_CLOSE)
            }
            return
        }
        throw IllegalStateException("NoTrack AI 身份已失效，请稍后重试")
    }

    private fun refreshNoTrackCookie() {
        val request = Request.Builder()
            .url(NOTRACK_CHAT_PAGE)
            .header("accept", "text/html,application/xhtml+xml")
            .header(
                "user-agent",
                "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome Mobile Safari/537.36"
            )
            .get()
            .build()
        noTrackCookie = runCatching {
            searchClient.newCall(request).execute().use { response ->
                val cookies = response.headers.values("Set-Cookie").joinToString(";")
                Regex("uid=([a-f0-9-]+)", RegexOption.IGNORE_CASE)
                    .find(cookies)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let { "uid=$it" }
                    .orEmpty()
            }
        }.getOrDefault("")
    }

    private fun toUpstreamPayload(messages: List<OpenAiChatMessage>): Map<String, Any?> {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val upstreamMessages = messages.map { message ->
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "role" to message.role,
                "content" to message.content,
                "parts" to listOf(
                    mapOf(
                        "type" to "text",
                        "text" to message.content
                    )
                ),
                "createdAt" to now
            )
        }
        return mapOf(
            "chatId" to UUID.randomUUID().toString(),
            "messages" to upstreamMessages,
            "selectedChatModel" to "chat-model",
            "selectedCharacter" to null,
            "selectedStory" to null,
            "deviceId" to DEVICE_ID,
            "locale" to "zh"
        )
    }
}

private class AiRateLimitedException : IllegalStateException("Unlimited AI 当前触发限流")

data class OpenAiChatMessage(
    val role: String,
    val content: String
)

private fun String.stripHtml(): String {
    return replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<[^>]+>"""), " ")
        .decodeHtml()
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun String.decodeHtml(): String {
    return replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
}

private fun JsonObject.stringValue(vararg names: String): String? {
    for (name in names) {
        if (!has(name)) continue
        val element = get(name)
        if (element == null || element.isJsonNull) continue
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            return element.asString
        }
    }
    return null
}

private fun String.toSearxngSearchEndpoint(): String? {
    val normalized = trim().ifBlank { return null }
        .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
        .trimEnd('/')
    val endpoint = if (normalized.endsWith("/search")) normalized else "$normalized/search"
    return endpoint.toHttpUrlOrNull()?.toString()
}
