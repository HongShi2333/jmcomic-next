package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiSearchSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AiChatStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "aiChatConversations"
        private const val SEARCH_SETTINGS_KEY = "aiSearchSettings"
    }

    private val _state = MutableStateFlow<List<AiChatConversation>?>(null)
    val state = _state.asStateFlow()
    private val _searchSettingsState = MutableStateFlow<AiSearchSettings?>(null)
    val searchSettingsState = _searchSettingsState.asStateFlow()

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

    fun getSearchSettings(): AiSearchSettings {
        if (_searchSettingsState.value == null) {
            _searchSettingsState.update {
                runCatching {
                    secureStorage.get<AiSearchSettings>(
                        SEARCH_SETTINGS_KEY,
                        object : TypeToken<AiSearchSettings>() {}.type
                    )
                }.getOrNull()?.normalized() ?: AiSearchSettings()
            }
        }
        return _searchSettingsState.value ?: AiSearchSettings()
    }

    fun setSearchSettings(settings: AiSearchSettings) {
        val normalized = settings.normalized()
        _searchSettingsState.update { normalized }
        secureStorage.set(SEARCH_SETTINGS_KEY, normalized)
    }

    fun remove() {
        _state.update { emptyList() }
        secureStorage.remove(STORAGE_KEY)
    }

}
