package com.par9uet.jm.storage

import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.LauncherDisguise
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.utils.normalizeBlockedTagList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalSettingStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "localSetting"
    }

    private var _state = MutableStateFlow<LocalSetting?>(null)
    val state = _state.asStateFlow()

    fun set(localSetting: LocalSetting) {
        _state.update {
            localSetting
        }
        secureStorage.set(STORAGE_KEY, this.state.value)
    }

    fun get(): LocalSetting {
        if (_state.value == null) {
            _state.update {
                val savedJson = secureStorage.getString(STORAGE_KEY)
                val saved = secureStorage.get<LocalSetting>(
                    STORAGE_KEY,
                    object : TypeToken<LocalSetting>() {}.type
                ) ?: LocalSetting()
                saved.copy(
                    showComicCacheNotification = if (savedJson.hasField("showComicCacheNotification")) {
                        saved.showComicCacheNotification
                    } else {
                        true
                    },
                    showComicCacheNotificationName = if (savedJson.hasField("showComicCacheNotificationName")) {
                        saved.showComicCacheNotificationName
                    } else {
                        true
                    },
                    launcherDisguise = if (savedJson.hasField("launcherDisguise")) {
                        LauncherDisguise.fromId(saved.launcherDisguise).id
                    } else {
                        LauncherDisguise.Default.id
                    },
                    blockedTagList = normalizeBlockedTagList(
                        runCatching { saved.blockedTagList }.getOrNull() ?: listOf()
                    )
                )
            }
        }
        return _state.value!!
    }

    fun remove() {
        _state.update {
            LocalSetting()
        }
        secureStorage.remove(STORAGE_KEY)
    }
}

private fun String?.hasField(name: String): Boolean {
    return this?.contains("\"$name\"") == true
}
