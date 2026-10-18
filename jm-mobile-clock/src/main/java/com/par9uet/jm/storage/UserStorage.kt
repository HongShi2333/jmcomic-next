package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "user"
    }

    private var _state = MutableStateFlow<User?>(null)
    val state = _state.asStateFlow()

    fun set(user: User) {
        _state.update {
            user
        }
        secureStorage.set(STORAGE_KEY, this.state.value)
    }

    fun get(): User {
        if (_state.value == null) {
            _state.update {
                secureStorage.get(STORAGE_KEY, object : TypeToken<User>() {}.type) ?: User.create()
            }
        }
        return _state.value ?: User.create()
    }

    fun remove() {
        _state.update {
            User.create()
        }
        secureStorage.remove(STORAGE_KEY)
    }
}