package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Cookie

class CookieStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "cookie"
    }

    private var _state = MutableStateFlow<List<Cookie>?>(null)
    val state = _state.asStateFlow()

    fun set(cookieStore: List<Cookie>) {
        _state.update {
            cookieStore
        }
        secureStorage.set(STORAGE_KEY, this.state.value)
    }

    fun get(): List<Cookie> {
        if (_state.value == null) {
            _state.update {
                secureStorage.get(STORAGE_KEY, object : TypeToken<List<Cookie>>() {}.type)
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