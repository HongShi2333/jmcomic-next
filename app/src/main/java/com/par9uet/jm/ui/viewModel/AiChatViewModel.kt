package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiChatMessage
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
    data class AiChatUiState(
        val conversations: List<AiChatConversation> = emptyList(),
        val activeConversationId: String = "",
        val input: String = "",
        val reasoningEnabled: Boolean = false,
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
                activeConversationId = normalized.first().id
            )
        }
    }

    fun changeInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun changeReasoningEnabled(value: Boolean) {
        _uiState.update { it.copy(reasoningEnabled = value) }
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
        )
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

        val requestMessages = buildRequestMessages(
            messages = active.messages + userMessage,
            reasoningEnabled = current.reasoningEnabled
        )

        sendJob = viewModelScope.launch {
            try {
                aiChatRepository.streamChat(messages = requestMessages) { delta ->
                    appendAssistantDelta(nextActive.id, assistantMessage.id, delta)
                }
                _uiState.update { it.copy(isSending = false) }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(isSending = false) }
            } catch (e: Exception) {
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
                )
            }.sortedByDescending { it.updatedAt }
            state.copy(conversations = conversations)
        }
    }

    private fun buildRequestMessages(
        messages: List<AiChatMessage>,
        reasoningEnabled: Boolean
    ): List<OpenAiChatMessage> {
        val requestMessages = messages.map {
            OpenAiChatMessage(role = it.role, content = it.content)
        }
        if (!reasoningEnabled) return requestMessages

        return listOf(
            OpenAiChatMessage(
                role = "system",
                content = "回答前先输出一个思考块，格式必须是：<think>你的思考内容</think>，然后再输出正式回答。注意：<think> 与 </think> 中间的思考内容绝对不能再次包含 <think> 或 </think>，也不能包含任何变体、示例、转义形式或代码片段形式的 think 标签。"
            )
        ) + requestMessages
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

    private fun persist(conversations: List<AiChatConversation>) {
        aiChatStorage.set(conversations.sortedByDescending { it.updatedAt })
    }
}
