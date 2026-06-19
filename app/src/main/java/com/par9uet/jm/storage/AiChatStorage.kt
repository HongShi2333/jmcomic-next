package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.AiChatConversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AiChatStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "aiChatConversations"
    }

    private val _state = MutableStateFlow<List<AiChatConversation>?>(null)
    val state = _state.asStateFlow()

    fun get(): List<AiChatConversation> {
        if (_state.value == null) {
            _state.update {
                runCatching {
                    secureStorage.get<List<AiChatConversation>>(
                        STORAGE_KEY,
                        object : TypeToken<List<AiChatConversation>>() {}.type
                    )
                }.getOrNull() ?: emptyList()
            }
        }
        return _state.value.orEmpty()
    }

    fun set(conversations: List<AiChatConversation>) {
        _state.update { conversations }
        secureStorage.set(STORAGE_KEY, conversations)
    }

    fun remove() {
        _state.update { emptyList() }
        secureStorage.remove(STORAGE_KEY)
    }

}
