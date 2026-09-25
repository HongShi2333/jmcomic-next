package com.par9uet.jm.data.models

import java.util.UUID

/**
 * AI 人格面具：用户可自定义 AI 的名字、职业、年龄、简介、输出格式等。
 * 所有字段均可选，留空表示不覆盖默认行为。
 */
data class AiPersona(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                  // AI 名字（如 "小明助手"）
    val profession: String = "",            // 职业（如 "前端工程师"）
    val age: String = "",                   // 年龄（字符串，允许 "不透露" 之类的描述）
    val bio: String = "",                   // 简介/性格设定
    val outputFormat: String = "",          // 输出格式偏好（如 "Markdown 列表"、"简短口语"）
    val systemPromptExtra: String = "",     // 用户自定义追加的系统提示词
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 判断是否为空白人格（所有可影响回答的字段都为空）。
     */
    val isEmpty: Boolean
        get() = name.isBlank() && profession.isBlank() && age.isBlank() &&
            bio.isBlank() && outputFormat.isBlank() && systemPromptExtra.isBlank()

    /**
     * 生成人格描述片段，用于注入系统提示词。空人格返回空字符串。
     */
    fun toPromptSegment(): String {
        if (isEmpty) return ""
        val parts = mutableListOf<String>()
        if (name.isNotBlank()) parts += "你的名字是「$name」"
        if (profession.isNotBlank()) parts += "你的职业/身份是：$profession"
        if (age.isNotBlank()) parts += "你的年龄设定：$age"
        if (bio.isNotBlank()) parts += "你的性格与背景：$bio"
        if (outputFormat.isNotBlank()) parts += "输出格式要求：$outputFormat"
        if (systemPromptExtra.isNotBlank()) parts += systemPromptExtra.trim()
        return parts.joinToString("\n").trim()
    }

    fun touch(): AiPersona = copy(updatedAt = System.currentTimeMillis())
}

/**
 * 联网搜索引擎提供方。
 * - AUTO：客户端按可用性自动选择
 * - TAVILY：Tavily API（需要 Key，针对 LLM 优化）
 * - DUCKDUCKGO：DuckDuckGo（无 Key，HTML 解析）
 * - BING/SOGOU/BAIDU：国内搜索引擎 HTML 解析
 * - SEARXNG：自建 SearXNG 实例 JSON 接口
 */
enum class AiSearchEngineProvider(val label: String, val requiresKey: Boolean) {
    AUTO("自动", false),
    TAVILY("Tavily", true),
    DUCKDUCKGO("DuckDuckGo", false),
    BING("Bing CN", false),
    SOGOU("Sogou", false),
    BAIDU("Baidu", false),
    SEARXNG("SearXNG", false)
}

/** AI 对话模型。模型切换只影响后续请求，不改变已有对话记录。 */
enum class AiChatModel(
    val id: String,
    val label: String,
    val subtitle: String
) {
    UnlimitedAi("unlimitedai", "Unlimited AI", "默认模型"),
    NoTrackAi("notrack-ai", "NoTrack AI", "匿名备用模型");

    companion object {
        fun fromId(id: String?): AiChatModel {
            return entries.firstOrNull { it.id == id } ?: UnlimitedAi
        }
    }
}

/**
 * 联网搜索设置。
 *
 * 新增能力：
 * - [provider] 切换搜索引擎提供方（支持 Tavily/DuckDuckGo）。
 * - [tavilyApiKey] Tavily API Key（仅 provider=TAVILY 时使用）。
 * - [aiAutoSearch] 是否允许 AI 自主决策是否触发联网搜索（默认开启）。
 *   与用户手动开关 [AiChatViewModel.webSearchEnabled] 取并集：任一为真即可触发搜索。
 * - [searchDepth] Tavily 搜索深度：basic / advanced。
 */
data class AiSearchSettings(
    val provider: AiSearchEngineProvider = AiSearchEngineProvider.AUTO,
    val tavilyApiKey: String = "",
    val tavilySearchDepth: String = "basic",     // basic / advanced
    val aiAutoSearch: Boolean = true,
    val engine: AiSearchEngine = AiSearchEngine.AUTO,  // 兼容旧字段：仅当 provider=AUTO 时作为备选引擎顺序参考
    val resultCount: Int = 5,
    val searxngBaseUrl: String = "",
    val searxngLanguage: String = "zh-CN",
    val searxngCategories: String = "general"
) {
    fun normalized(): AiSearchSettings {
        return copy(
            resultCount = resultCount.coerceIn(1, 10),
            tavilyApiKey = tavilyApiKey.trim(),
            tavilySearchDepth = tavilySearchDepth.trim().ifBlank { "basic" }
                .let { if (it == "advanced") "advanced" else "basic" },
            searxngBaseUrl = searxngBaseUrl.trim().trimEnd('/'),
            searxngLanguage = searxngLanguage.ifBlank { "zh-CN" }.trim(),
            searxngCategories = searxngCategories.ifBlank { "general" }.trim()
        )
    }

    /**
     * Tavily 是否可用：选择了 Tavily 且配置了 Key。
     */
    val tavilyReady: Boolean
        get() = provider == AiSearchEngineProvider.TAVILY && tavilyApiKey.isNotBlank()
}

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

/**
 * AI 对话。
 *
 * [personaId] 绑定本次对话使用的 AI 人格面具；空字符串表示使用默认人格（无注入）。
 * 切换人格不影响历史消息，只影响后续生成。
 */
data class AiChatConversation(
    val id: String = "",
    val title: String = "新对话",
    val messages: List<AiChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val personaId: String = ""
)

/**
 * 兼容旧代码保留的枚举（仅用于 [AiSearchSettings.engine] 字段）。
 */
enum class AiSearchEngine(val label: String) {
    AUTO("自动"),
    BING("Bing CN"),
    SOGOU("Sogou"),
    BAIDU("Baidu"),
    SEARXNG("SearXNG")
}
