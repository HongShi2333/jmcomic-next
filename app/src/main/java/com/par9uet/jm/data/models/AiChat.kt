package com.par9uet.jm.data.models

data class AiChatMessage(
    val id: String = "",
    val role: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long? = null,
    val branches: List<AiChatMessageBranch> = emptyList(),
    val activeBranchIndex: Int = 0
)

data class AiChatMessageBranch(
    val content: String = "",
    val followingMessages: List<AiChatMessage> = emptyList()
)

data class AiChatConversation(
    val id: String = "",
    val title: String = "新对话",
    val messages: List<AiChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AiSearchEngine(val label: String) {
    AUTO("自动"),
    BING("Bing CN"),
    SOGOU("Sogou"),
    BAIDU("Baidu"),
    SEARXNG("SearXNG")
}

data class AiSearchSettings(
    val engine: AiSearchEngine = AiSearchEngine.AUTO,
    val resultCount: Int = 5,
    val searxngBaseUrl: String = "",
    val searxngLanguage: String = "zh-CN",
    val searxngCategories: String = "general"
) {
    fun normalized(): AiSearchSettings {
        return copy(
            resultCount = resultCount.coerceIn(1, 10),
            searxngBaseUrl = searxngBaseUrl.trim().trimEnd('/'),
            searxngLanguage = searxngLanguage.ifBlank { "zh-CN" }.trim(),
            searxngCategories = searxngCategories.ifBlank { "general" }.trim()
        )
    }
}
