package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import com.par9uet.jm.data.models.AiPersona
import com.par9uet.jm.storage.PersonaStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * AI 人格面具 ViewModel：用于人格管理界面（CRUD + 激活切换）。
 */
class PersonaViewModel(
    private val personaStorage: PersonaStorage
) : ViewModel() {

    data class PersonaUiState(
        val personas: List<AiPersona> = emptyList(),
        val activePersonaId: String? = null
    ) {
        val activePersona: AiPersona?
            get() = personas.firstOrNull { it.id == activePersonaId }
    }

    private val _uiState = MutableStateFlow(PersonaUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                personas = personaStorage.get(),
                activePersonaId = personaStorage.getActiveId()
            )
        }
    }

    fun createDraft(): AiPersona = personaStorage.createDraft()

    /**
     * 新建或更新人格。空人格会被忽略。
     * @return 已保存的人格；若被忽略则返回 null。
     */
    fun save(persona: AiPersona): AiPersona? {
        if (persona.isEmpty) return null
        val saved = personaStorage.upsert(persona)
        refresh()
        return saved
    }

    fun delete(id: String) {
        personaStorage.delete(id)
        refresh()
    }

    fun setActive(id: String?) {
        personaStorage.setActiveId(id)
        _uiState.update { it.copy(activePersonaId = id) }
    }

    /**
     * 导入人格列表（用于备份恢复）。
     */
    fun importAll(personas: List<AiPersona>) {
        personaStorage.set(personas)
        refresh()
    }

    /**
     * 导出全部人格（用于备份）。
     */
    fun exportAll(): List<AiPersona> = personaStorage.get()
}
