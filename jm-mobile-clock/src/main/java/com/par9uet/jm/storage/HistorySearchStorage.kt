package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HistorySearchStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "historySearch"
    }

    private var _state = MutableStateFlow<List<String>?>(null)
    val state = _state.asStateFlow()

    fun set(list: List<String>) {
        _state.update {
            list
        }
        secureStorage.set(STORAGE_KEY, this.state.value)
    }

    fun get(): List<String> {
        if (_state.value == null) {
            _state.update {
                secureStorage.get(STORAGE_KEY, object : TypeToken<List<String>>() {}.type)
                    ?: listOf()
            }
        }
        return _state.value ?: listOf()
    }

    fun remove() {
        _state.update {
            listOf()
        }
        secureStorage.remove(STORAGE_KEY)
    }
}