package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.AiPersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * AI 人格面具存储：基于 [SecureStorage] 加密持久化多个人格。
 *
 * 同时维护一个「当前激活人格 ID」，用于全局默认人格切换。
 * 对话级别的人格绑定存储在 [AiChatConversation.personaId] 中，与此处互相独立。
 */
class PersonaStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "aiPersonas"
        private const val ACTIVE_ID_KEY = "aiPersonaActiveId"
    }

    private val _state = MutableStateFlow<List<AiPersona>?>(null)
    val state = _state.asStateFlow()
    private val _activeIdState = MutableStateFlow<String?>(null)
    val activeIdState = _activeIdState.asStateFlow()

    fun get(): List<AiPersona> {
        if (_state.value == null) {
            _state.update {
                runCatching {
                    secureStorage.get<List<AiPersona>>(
                        STORAGE_KEY,
                        object : TypeToken<List<AiPersona>>() {}.type
                    )
                }.getOrNull()?.sortedByDescending { it.updatedAt } ?: emptyList()
            }
        }
        return _state.value.orEmpty()
    }

    fun getById(id: String): AiPersona? {
        return get().firstOrNull { it.id == id }
    }

    fun getActiveId(): String? {
        if (_activeIdState.value == null) {
            _activeIdState.update {
                runCatching { secureStorage.getString(ACTIVE_ID_KEY) }.getOrNull()
            }
        }
        return _activeIdState.value
    }

    fun setActiveId(id: String?) {
        _activeIdState.update { id }
        if (id.isNullOrBlank()) {
            secureStorage.remove(ACTIVE_ID_KEY)
        } else {
            secureStorage.set(ACTIVE_ID_KEY, id)
        }
    }

    fun getActive(): AiPersona? {
        val id = getActiveId() ?: return null
        return getById(id)
    }

    /**
     * 新增或更新人格。空人格将被忽略（不会写入）。
     */
    fun upsert(persona: AiPersona): AiPersona {
        val current = get().toMutableList()
        val normalized = persona.touch()
        val index = current.indexOfFirst { it.id == normalized.id }
        if (index >= 0) {
            current[index] = normalized
        } else {
            current.add(normalized)
        }
        val sorted = current.sortedByDescending { it.updatedAt }
        persist(sorted)
        return normalized
    }

    /**
     * 新建一个空白人格并返回，但不立即持久化；调用 [upsert] 后才写入。
     * 便于在 UI 端先编辑再保存。
     */
    fun createDraft(): AiPersona {
        return AiPersona(id = UUID.randomUUID().toString())
    }

    fun delete(id: String) {
        val current = get().toMutableList()
        current.removeAll { it.id == id }
        persist(current.sortedByDescending { it.updatedAt })
        if (getActiveId() == id) {
            setActiveId(null)
        }
    }

    fun set(personas: List<AiPersona>) {
        val sorted = personas.sortedByDescending { it.updatedAt }
        persist(sorted)
    }

    fun remove() {
        _state.update { emptyList() }
        _activeIdState.update { null }
        secureStorage.remove(STORAGE_KEY)
        secureStorage.remove(ACTIVE_ID_KEY)
    }

    private fun persist(personas: List<AiPersona>) {
        _state.update { personas }
        secureStorage.set(STORAGE_KEY, personas)
    }
}
