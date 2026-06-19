package com.par9uet.jm.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                val delta = json.get("delta")?.asString.orEmpty()
                if (type == "delta" && delta.isNotEmpty()) {
                    onDelta(delta)
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
            "selectedChatModel" to "chat-model-reasoning",
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
