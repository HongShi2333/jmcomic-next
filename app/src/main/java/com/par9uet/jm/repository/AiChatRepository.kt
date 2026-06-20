package com.par9uet.jm.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.data.models.AiSearchEngine
import com.par9uet.jm.data.models.AiSearchSettings
import kotlinx.coroutines.Dispatchers
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

class AiChatRepository(
    private val gson: Gson
) {
    companion object {
        private const val TARGET_API = "https://app.unlimitedai.chat/api/chat"
        private const val DEVICE_ID = ""
        private const val COOKIES = ""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun searchWebContext(
        query: String,
        settings: AiSearchSettings
    ): String? = withContext(Dispatchers.IO) {
        val normalized = settings.normalized()
        val providers = when (normalized.engine) {
            AiSearchEngine.AUTO -> listOf(
                { searchSearxngContext(query, normalized) },
                { searchBingContext(query, normalized.resultCount) },
                { searchSogouContext(query, normalized.resultCount) },
                { searchBaiduContext(query, normalized.resultCount) }
            )
            AiSearchEngine.BING -> listOf({ searchBingContext(query, normalized.resultCount) })
            AiSearchEngine.SOGOU -> listOf({ searchSogouContext(query, normalized.resultCount) })
            AiSearchEngine.BAIDU -> listOf({ searchBaiduContext(query, normalized.resultCount) })
            AiSearchEngine.SEARXNG -> listOf({ searchSearxngContext(query, normalized) })
        }
        providers.firstNotNullOfOrNull { provider ->
            runCatching { provider() }.getOrNull()
        }
    }

    private fun searchBingContext(query: String, resultCount: Int): String? {
        val url = "https://cn.bing.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("setlang", "zh-CN")
            .addQueryParameter("cc", "CN")
            .build()
        return fetchSearchPage(url.toString())?.toBingSearchContext(resultCount)
    }

    private fun searchSogouContext(query: String, resultCount: Int): String? {
        val url = "https://www.sogou.com/web".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("ie", "utf8")
            .build()
        return fetchSearchPage(url.toString())?.toSogouSearchContext(resultCount)
    }

    private fun searchBaiduContext(query: String, resultCount: Int): String? {
        val url = "https://www.baidu.com/s".toHttpUrl().newBuilder()
            .addQueryParameter("wd", query)
            .addQueryParameter("rn", resultCount.toString())
            .addQueryParameter("ie", "utf-8")
            .build()
        return fetchSearchPage(url.toString())?.toBaiduSearchContext(resultCount)
    }

    private fun searchSearxngContext(query: String, settings: AiSearchSettings): String? {
        val endpoint = settings.searxngBaseUrl.toSearxngSearchEndpoint() ?: return null
        val url = endpoint.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .addQueryParameter("language", settings.searxngLanguage)
            .addQueryParameter("categories", settings.searxngCategories)
            .build()
        return fetchSearchPage(
            url = url.toString(),
            accept = "application/json,text/plain,*/*"
        )?.toSearxngSearchContext(settings.resultCount)
    }

    private fun fetchSearchPage(
        url: String,
        accept: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    ): String? {
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
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            response.body.string()
        }
    }

    suspend fun streamChat(
        messages: List<OpenAiChatMessage>,
        onDelta: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
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

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message = response.body.string().ifBlank { "HTTP ${response.code}" }
                throw IllegalStateException("AI 请求失败：$message")
            }

            val bodySource = response.body.source()
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
                val answerDelta = if (type.contains("reason", ignoreCase = true)) "" else delta

                if (answerDelta.isNotEmpty()) {
                    onDelta(answerDelta)
                }
            }
        }
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

data class OpenAiChatMessage(
    val role: String,
    val content: String
)

private data class SearchItem(
    val title: String,
    val text: String,
    val url: String
)

private fun String.toBingSearchContext(resultCount: Int): String? {
    val blockRegex = Regex("""<li\s+class="b_algo"[\s\S]*?</li>""", RegexOption.IGNORE_CASE)
    val linkRegex = Regex("""<h2[^>]*>[\s\S]*?<a[^>]+href="([^"]+)"[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
    val snippetRegex = Regex("""<p[^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
    val results = blockRegex.findAll(this)
        .mapNotNull { block ->
            val link = linkRegex.find(block.value) ?: return@mapNotNull null
            val url = link.groupValues[1].decodeHtml().trim()
            val title = link.groupValues[2].stripHtml()
            val snippet = snippetRegex.find(block.value)?.groupValues?.get(1)?.stripHtml().orEmpty()
            if (url.isBlank() || title.isBlank()) return@mapNotNull null
            SearchItem(title = title, text = snippet, url = url)
        }
        .distinctBy { it.url }
        .take(resultCount)
        .toList()

    if (results.isEmpty()) return null
    return results.mapIndexed { index, item ->
        val snippet = if (item.text.isBlank()) "" else " - ${item.text}"
        "${index + 1}. ${item.title}$snippet 来源：${item.url}"
    }.joinToString("\n")
}

private fun String.toSogouSearchContext(resultCount: Int): String? {
    val linkRegex = Regex(
        """<h3[^>]*>[\s\S]*?<a[^>]+href="([^"]+)"[^>]*>([\s\S]*?)</a>[\s\S]*?</h3>""",
        RegexOption.IGNORE_CASE
    )
    val results = linkRegex.findAll(this)
        .mapNotNull { match ->
            val url = match.groupValues[1].decodeHtml().trim()
            val title = match.groupValues[2].stripHtml()
            val blockEnd = minOf(length, match.range.last + 520)
            val text = substring(match.range.first, blockEnd)
                .stripHtml()
                .removePrefix(title)
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(180)
            if (!url.startsWith("http") || title.isBlank()) return@mapNotNull null
            SearchItem(title = title, text = text, url = url)
        }
        .distinctBy { it.url }
        .take(resultCount)
        .toList()

    if (results.isEmpty()) return null
    return results.mapIndexed { index, item ->
        val snippet = if (item.text.isBlank()) "" else " - ${item.text}"
        "${index + 1}. ${item.title}$snippet 来源：${item.url}"
    }.joinToString("\n")
}

private fun String.toBaiduSearchContext(resultCount: Int): String? {
    val blockRegex = Regex(
        """<div[^>]+class="[^"]*(?:result|c-container)[^"]*"[\s\S]*?(?=<div[^>]+class="[^"]*(?:result|c-container)|$)""",
        RegexOption.IGNORE_CASE
    )
    val linkRegex = Regex(
        """<h3[^>]*>[\s\S]*?<a[^>]+href="([^"]+)"[^>]*>([\s\S]*?)</a>[\s\S]*?</h3>""",
        RegexOption.IGNORE_CASE
    )
    val results = blockRegex.findAll(this)
        .mapNotNull { block ->
            val link = linkRegex.find(block.value) ?: return@mapNotNull null
            val url = link.groupValues[1].decodeHtml().trim()
            val title = link.groupValues[2].stripHtml()
            val text = block.value.stripHtml()
                .removePrefix(title)
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(180)
            if (url.isBlank() || title.isBlank()) return@mapNotNull null
            SearchItem(title = title, text = text, url = url)
        }
        .distinctBy { it.url }
        .take(resultCount)
        .toList()

    if (results.isEmpty()) return null
    return results.mapIndexed { index, item ->
        val snippet = if (item.text.isBlank()) "" else " - ${item.text}"
        "${index + 1}. ${item.title}$snippet 来源：${item.url}"
    }.joinToString("\n")
}

private fun String.toSearxngSearchContext(resultCount: Int): String? {
    val root = runCatching { JsonParser.parseString(this).asJsonObject }.getOrNull() ?: return null
    val results = root.getAsJsonArray("results") ?: return null
    val items = results
        .mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val item = element.asJsonObject
            val title = item.stringValue("title").orEmpty().stripHtml()
            val url = item.stringValue("url").orEmpty().trim()
            val text = item.stringValue("content").orEmpty().stripHtml().take(180)
            if (!url.startsWith("http") || title.isBlank()) return@mapNotNull null
            SearchItem(title = title, text = text, url = url)
        }
        .distinctBy { it.url }
        .take(resultCount)
        .toList()

    if (items.isEmpty()) return null
    return items.mapIndexed { index, item ->
        val snippet = if (item.text.isBlank()) "" else " - ${item.text}"
        "${index + 1}. ${item.title}$snippet 来源：${item.url}"
    }.joinToString("\n")
}

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
