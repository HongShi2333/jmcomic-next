package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.data.models.AiChatMessageBranch
import com.par9uet.jm.data.models.AiSearchSettings
import com.par9uet.jm.repository.AiChatRepository
import com.par9uet.jm.repository.OpenAiChatMessage
import com.par9uet.jm.storage.AiChatStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AiChatViewModel(
    private val aiChatRepository: AiChatRepository,
    private val aiChatStorage: AiChatStorage
) : ViewModel() {
    enum class RetryMode {
        Regenerate,
        Detailed,
        Concise
    }

    data class AiChatUiState(
        val conversations: List<AiChatConversation> = emptyList(),
        val activeConversationId: String = "",
        val input: String = "",
        val webSearchEnabled: Boolean = false,
        val searchSettings: AiSearchSettings = AiSearchSettings(),
        val isSending: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState = _uiState.asStateFlow()

    private var sendJob: Job? = null

    init {
        val conversations = aiChatStorage.get()
        val normalized = conversations.ifEmpty { listOf(newConversationModel()) }
            .sortedByDescending { it.updatedAt }
        aiChatStorage.set(normalized)
        _uiState.update {
            it.copy(
                conversations = normalized,
                activeConversationId = normalized.first().id,
                searchSettings = aiChatStorage.getSearchSettings()
            )
        }
    }

    fun changeInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun changeWebSearchEnabled(value: Boolean) {
        _uiState.update { it.copy(webSearchEnabled = value) }
    }

    fun changeSearchSettings(settings: AiSearchSettings) {
        val normalized = settings.normalized()
        aiChatStorage.setSearchSettings(normalized)
        _uiState.update { it.copy(searchSettings = normalized) }
    }

    fun createConversation() {
        val conversation = newConversationModel()
        val conversations = listOf(conversation) + _uiState.value.conversations
        persist(conversations)
        _uiState.update {
            it.copy(
                conversations = conversations,
                activeConversationId = conversation.id,
                input = "",
                errorMessage = null
            )
        }
    }

    fun selectConversation(id: String) {
        _uiState.update {
            it.copy(activeConversationId = id, input = "", errorMessage = null)
        }
    }

    fun deleteConversation(id: String) {
        val current = _uiState.value
        val remaining = current.conversations.filterNot { it.id == id }
            .ifEmpty { listOf(newConversationModel()) }
            .sortedByDescending { it.updatedAt }
        val activeId = if (remaining.any { it.id == current.activeConversationId }) {
            current.activeConversationId
        } else {
            remaining.first().id
        }
        persist(remaining)
        _uiState.update {
            it.copy(
                conversations = remaining,
                activeConversationId = activeId,
                errorMessage = null
            )
        }
    }

    fun stopGenerating() {
        sendJob?.cancel()
        sendJob = null
        _uiState.update { it.copy(isSending = false) }
    }

    fun send() {
        val current = _uiState.value
        val text = current.input.trim()
        if (text.isBlank() || current.isSending) return

        val active = current.conversations.firstOrNull { it.id == current.activeConversationId }
            ?: newConversationModel()
        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text
        )
        val assistantMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            role = "assistant",
            content = ""
        )

        val nextActive = active.copy(
            title = titleFor(active, text),
            messages = active.messages + userMessage + assistantMessage,
            updatedAt = System.currentTimeMillis()
        ).withSyncedActiveBranch()
        val nextConversations = upsertConversation(current.conversations, nextActive)
        persist(nextConversations)
        _uiState.update {
            it.copy(
                conversations = nextConversations,
                activeConversationId = nextActive.id,
                input = "",
                isSending = true,
                errorMessage = null
            )
        }

        startAssistantRequest(
            conversationId = nextActive.id,
            assistantMessageId = assistantMessage.id,
            assistantStartedAt = assistantMessage.createdAt,
            requestSourceMessages = active.messages + userMessage,
            userQuery = text,
            retryInstruction = null,
            webSearchEnabled = current.webSearchEnabled,
            searchSettings = current.searchSettings
        )
    }

    fun retry(messageId: String, mode: RetryMode) {
        val current = _uiState.value
        if (current.isSending) return

        val active = current.conversations.firstOrNull { it.id == current.activeConversationId }
            ?: return
        val assistantIndex = active.messages.indexOfFirst {
            it.id == messageId && it.role == "assistant"
        }
        if (assistantIndex <= 0) return

        val requestSourceMessages = active.messages.take(assistantIndex)
        val previousUserMessage = requestSourceMessages.lastOrNull { it.role == "user" } ?: return
        val assistantMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            role = "assistant",
            content = ""
        )
        val nextActive = active.copy(
            messages = requestSourceMessages + assistantMessage,
            updatedAt = System.currentTimeMillis()
        ).withSyncedActiveBranch()
        val nextConversations = upsertConversation(current.conversations, nextActive)
        persist(nextConversations)
        _uiState.update {
            it.copy(
                conversations = nextConversations,
                activeConversationId = nextActive.id,
                isSending = true,
                errorMessage = null
            )
        }

        startAssistantRequest(
            conversationId = nextActive.id,
            assistantMessageId = assistantMessage.id,
            assistantStartedAt = assistantMessage.createdAt,
            requestSourceMessages = requestSourceMessages,
            userQuery = previousUserMessage.content,
            retryInstruction = retryInstruction(mode),
            webSearchEnabled = current.webSearchEnabled,
            searchSettings = current.searchSettings
        )
    }

    fun editUserMessage(messageId: String, newContent: String) {
        val current = _uiState.value
        val text = newContent.trim()
        if (text.isBlank() || current.isSending) return

        val active = current.conversations.firstOrNull { it.id == current.activeConversationId }
            ?.withSyncedActiveBranch()
            ?: return
        val userIndex = active.messages.indexOfFirst {
            it.id == messageId && it.role == "user"
        }
        if (userIndex < 0) return

        val userMessage = active.messages[userIndex]
        val currentFollowing = active.messages.drop(userIndex + 1)
        val existingBranches = userMessage.branches.ifEmpty {
            listOf(
                AiChatMessageBranch(
                    content = userMessage.content,
                    followingMessages = currentFollowing
                )
            )
        }
        val assistantMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            role = "assistant",
            content = ""
        )
        val nextBranchIndex = existingBranches.size
        val editedUserMessage = userMessage.copy(
            content = text,
            branches = existingBranches + AiChatMessageBranch(
                content = text,
                followingMessages = listOf(assistantMessage)
            ),
            activeBranchIndex = nextBranchIndex
        )
        val nextMessages = active.messages.take(userIndex) + editedUserMessage + assistantMessage
        val nextActive = active.copy(
            messages = nextMessages,
            updatedAt = System.currentTimeMillis()
        ).withSyncedActiveBranch()
        val nextConversations = upsertConversation(current.conversations, nextActive)
        persist(nextConversations)
        _uiState.update {
            it.copy(
                conversations = nextConversations,
                activeConversationId = nextActive.id,
                isSending = true,
                errorMessage = null
            )
        }

        startAssistantRequest(
            conversationId = nextActive.id,
            assistantMessageId = assistantMessage.id,
            assistantStartedAt = assistantMessage.createdAt,
            requestSourceMessages = nextMessages.take(userIndex) + editedUserMessage.copy(
                branches = emptyList(),
                activeBranchIndex = 0
            ),
            userQuery = text,
            retryInstruction = null,
            webSearchEnabled = current.webSearchEnabled,
            searchSettings = current.searchSettings
        )
    }

    fun switchUserBranch(messageId: String, targetIndex: Int) {
        val current = _uiState.value
        if (current.isSending) return

        val active = current.conversations.firstOrNull { it.id == current.activeConversationId }
            ?.withSyncedActiveBranch()
            ?: return
        val userIndex = active.messages.indexOfFirst {
            it.id == messageId && it.role == "user"
        }
        if (userIndex < 0) return

        val userMessage = active.messages[userIndex]
        val branches = userMessage.branches
        if (branches.isEmpty() || targetIndex !in branches.indices) return

        val targetBranch = branches[targetIndex]
        val switchedUserMessage = userMessage.copy(
            content = targetBranch.content,
            activeBranchIndex = targetIndex,
            branches = branches
        )
        val nextActive = active.copy(
            messages = active.messages.take(userIndex) + switchedUserMessage + targetBranch.followingMessages,
            updatedAt = System.currentTimeMillis()
        ).withSyncedActiveBranch()
        val nextConversations = upsertConversation(current.conversations, nextActive)
        persist(nextConversations)
        _uiState.update {
            it.copy(
                conversations = nextConversations,
                activeConversationId = nextActive.id,
                errorMessage = null
            )
        }
    }

    private fun startAssistantRequest(
        conversationId: String,
        assistantMessageId: String,
        assistantStartedAt: Long,
        requestSourceMessages: List<AiChatMessage>,
        userQuery: String,
        retryInstruction: String?,
        webSearchEnabled: Boolean,
        searchSettings: AiSearchSettings
    ) {
        sendJob = viewModelScope.launch {
            try {
                val requestMessages = buildRequestMessages(
                    messages = requestSourceMessages,
                    webContext = resolveWebContext(
                        text = userQuery,
                        webSearchEnabled = webSearchEnabled,
                        settings = searchSettings
                    ),
                    retryInstruction = retryInstruction
                )
                aiChatRepository.streamChat(messages = requestMessages) { delta ->
                    appendAssistantDelta(conversationId, assistantMessageId, delta)
                }
                markAssistantFinished(conversationId, assistantMessageId, assistantStartedAt)
                _uiState.update { it.copy(isSending = false) }
            } catch (_: CancellationException) {
                markAssistantFinished(conversationId, assistantMessageId, assistantStartedAt)
                _uiState.update { it.copy(isSending = false) }
            } catch (e: Exception) {
                markAssistantFinished(conversationId, assistantMessageId, assistantStartedAt)
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = e.message ?: "AI 请求失败"
                    )
                }
            } finally {
                sendJob = null
                persist(_uiState.value.conversations)
            }
        }
    }

    private fun appendAssistantDelta(conversationId: String, messageId: String, delta: String) {
        _uiState.update { state ->
            val conversations = state.conversations.map { conversation ->
                if (conversation.id != conversationId) return@map conversation
                conversation.copy(
                    messages = conversation.messages.map { message ->
                        if (message.id == messageId) {
                            message.copy(content = message.content + delta)
                        } else {
                            message
                        }
                    },
                    updatedAt = System.currentTimeMillis()
                ).withSyncedActiveBranch()
            }.sortedByDescending { it.updatedAt }
            state.copy(conversations = conversations)
        }
    }

    private fun markAssistantFinished(conversationId: String, messageId: String, startedAt: Long) {
        val duration = (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
        _uiState.update { state ->
            val conversations = state.conversations.map { conversation ->
                if (conversation.id != conversationId) return@map conversation
                conversation.copy(
                    messages = conversation.messages.map { message ->
                        if (message.id == messageId) {
                            message.copy(durationMs = duration)
                        } else {
                            message
                        }
                    },
                    updatedAt = System.currentTimeMillis()
                ).withSyncedActiveBranch()
            }.sortedByDescending { it.updatedAt }
            state.copy(conversations = conversations)
        }
    }

    private fun buildRequestMessages(
        messages: List<AiChatMessage>,
        webContext: String?,
        retryInstruction: String?
    ): List<OpenAiChatMessage> {
        val today = todayText()
        val requestMessages = messages.map {
            OpenAiChatMessage(role = it.role, content = it.content)
        }
        val systemPrompts = mutableListOf(
            OpenAiChatMessage(
                role = "system",
                content = "当前日期是 $today。请直接输出给用户看的正式回答，不要输出内部推理过程、隐藏分析标记或占位内容。涉及新闻、版本、价格、政策、人物职位、时间敏感信息时，优先使用客户端提供的联网搜索结果；没有可靠搜索结果时必须说明无法确认最新信息。"
            )
        )

        if (webContext != null) {
            systemPrompts += OpenAiChatMessage(
                role = "system",
                content = "下面是客户端在 $today 自动联网搜索到的参考信息。联网搜索已经开启，你必须优先阅读并使用这些结果回答；采用时请在答案中说明来源链接。若参考信息不足或不相关，请直接说明无法从联网结果确认，不要编造，也不要用旧知识冒充最新信息。\n\n$webContext"
            )
        }

        if (retryInstruction != null) {
            systemPrompts += OpenAiChatMessage(
                role = "system",
                content = retryInstruction
            )
        }

        return systemPrompts + requestMessages
    }

    private fun retryInstruction(mode: RetryMode): String {
        return when (mode) {
            RetryMode.Regenerate -> "请重新回答上一条用户请求。不要复述原回答，不要提及这是重试；直接给出新的正式回答。"
            RetryMode.Detailed -> "请重新回答上一条用户请求，并显著增加细节、步骤、依据和必要的上下文。不要提及这是重试；直接给出更详细的正式回答。"
            RetryMode.Concise -> "请重新回答上一条用户请求，并压缩为更精简的版本，只保留结论、关键步骤和必要注意事项。不要提及这是重试；直接给出更精简的正式回答。"
        }
    }

    private suspend fun resolveWebContext(
        text: String,
        webSearchEnabled: Boolean,
        settings: AiSearchSettings
    ): String? {
        if (!webSearchEnabled) return null
        val query = buildSearchQuery(text)
        return aiChatRepository.searchWebContext(query, settings)
            ?: "客户端已尝试联网搜索，但没有获得可用结果。请在回答中明确说明无法从联网结果确认最新信息，不要使用旧知识冒充最新信息。"
    }

    private fun buildSearchQuery(text: String): String {
        val normalized = text.trim()
        val suffix = " ${todayText()} 2026 最新"
        return if (normalized.contains("2026") || normalized.contains("最新") || normalized.contains("今天") || normalized.contains("现在")) {
            normalized
        } else {
            normalized + suffix
        }
    }

    private fun newConversationModel(): AiChatConversation {
        return AiChatConversation(
            id = UUID.randomUUID().toString(),
            title = "新对话",
            messages = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun titleFor(conversation: AiChatConversation, text: String): String {
        if (conversation.messages.isNotEmpty() && conversation.title != "新对话") {
            return conversation.title
        }
        return text.take(24).ifBlank { "新对话" }
    }

    private fun upsertConversation(
        conversations: List<AiChatConversation>,
        conversation: AiChatConversation
    ): List<AiChatConversation> {
        val hasConversation = conversations.any { it.id == conversation.id }
        val next = if (hasConversation) {
            conversations.map { if (it.id == conversation.id) conversation else it }
        } else {
            listOf(conversation) + conversations
        }
        return next.sortedByDescending { it.updatedAt }
    }

    private fun AiChatConversation.withSyncedActiveBranch(): AiChatConversation {
        if (messages.none { it.branches.isNotEmpty() }) return this
        val syncedMessages = messages.mapIndexed { index, message ->
            if (message.role != "user" || message.branches.isEmpty()) {
                message
            } else {
                val activeIndex = message.activeBranchIndex.coerceIn(message.branches.indices)
                val syncedBranches = message.branches.mapIndexed { branchIndex, branch ->
                    if (branchIndex == activeIndex) {
                        branch.copy(
                            content = message.content,
                            followingMessages = messages.drop(index + 1)
                        )
                    } else {
                        branch
                    }
                }
                message.copy(
                    branches = syncedBranches,
                    activeBranchIndex = activeIndex
                )
            }
        }
        return copy(messages = syncedMessages)
    }

    private fun persist(conversations: List<AiChatConversation>) {
        aiChatStorage.set(conversations.map { it.withSyncedActiveBranch() }.sortedByDescending { it.updatedAt })
    }

    private fun todayText(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}
