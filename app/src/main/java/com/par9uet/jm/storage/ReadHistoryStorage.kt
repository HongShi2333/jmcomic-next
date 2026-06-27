package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ComicReadHistory(
    val lastChapterId: Int = 0,
    val readChapterIds: List<Int> = emptyList()
)

class ReadHistoryStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "comicReadHistory"
    }

    private val _state = MutableStateFlow<Map<Int, ComicReadHistory>?>(null)
    val state = _state.asStateFlow()

    fun set(history: Map<Int, ComicReadHistory>) {
        _state.update { history }
        secureStorage.set(STORAGE_KEY, history)
    }

    fun get(): Map<Int, ComicReadHistory> {
        if (_state.value == null) {
            _state.update {
                secureStorage.get(
                    STORAGE_KEY,
                    object : TypeToken<Map<Int, ComicReadHistory>>() {}.type
                ) ?: emptyMap()
            }
        }
        return _state.value.orEmpty()
    }

    fun remove() {
        _state.update { emptyMap() }
        secureStorage.remove(STORAGE_KEY)
    }
}
