package com.par9uet.jm.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Ephemeral detail text for active cache work; it is intentionally not persisted in Room. */
object DownloadProgressMessageStore {
    private val _messages = MutableStateFlow<Map<Int, String>>(emptyMap())
    val messages: StateFlow<Map<Int, String>> = _messages.asStateFlow()

    fun update(groupId: Int, message: String) {
        if (groupId <= 0) return
        _messages.update { it.toMutableMap().apply { put(groupId, message) } }
    }

    fun clear(groupId: Int) {
        if (groupId <= 0) return
        _messages.update { it.toMutableMap().apply { remove(groupId) } }
    }
}
