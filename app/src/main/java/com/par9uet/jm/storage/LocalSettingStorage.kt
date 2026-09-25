package com.par9uet.jm.storage

import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PASSWORD
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PATTERN
import com.par9uet.jm.data.models.APP_LOCK_METHOD_BIOMETRIC
import com.par9uet.jm.data.models.APP_LOCK_RULE_ANY
import com.par9uet.jm.data.models.APP_LOCK_RULE_REQUIRED
import com.par9uet.jm.data.models.AiChatModel
import com.par9uet.jm.data.models.BlockedTagTemplate
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_DEFAULT
import com.par9uet.jm.data.models.COMIC_API_SOURCE_BUILTIN
import com.par9uet.jm.data.models.COMIC_API_SOURCE_MIXED
import com.par9uet.jm.data.models.COMIC_API_SOURCE_NETWORK
import com.par9uet.jm.data.models.COVER_CACHE_MAX_DURATION_HOURS
import com.par9uet.jm.data.models.DEFAULT_COVER_CACHE_DURATION_HOURS
import com.par9uet.jm.data.models.LauncherDisguise
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.utils.flattenBlockedTagTemplates
import com.par9uet.jm.utils.normalizeBlockedTagList
import com.par9uet.jm.utils.normalizeBlockedTagTemplates
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
                // 旧版本字段 appLockType 迁移到 appLockUnlockMode
                val legacyAppLockType = parseLegacyAppLockType(savedJson)
                val migratedUnlockMode = if (savedJson.hasField("appLockUnlockMode")) {
                    saved.appLockUnlockMode
                } else if (legacyAppLockType != null) {
                    legacyAppLockType
                } else {
                    APP_LOCK_TYPE_PASSWORD
                }
                val legacyBlockedTags = normalizeBlockedTagList(
                    runCatching { saved.blockedTagList }.getOrNull() ?: listOf()
                )
                val savedTemplates = normalizeBlockedTagTemplates(
                    runCatching { saved.blockedTagTemplateList }.getOrNull() ?: listOf()
                )
                val migratedTemplates = if (savedJson.hasField("blockedTagTemplateList")) {
                    savedTemplates
                } else if (legacyBlockedTags.isNotEmpty()) {
                    listOf(BlockedTagTemplate(name = "默认排除", tagList = legacyBlockedTags))
                } else {
                    listOf()
                }
                val savedPassword = if (savedJson.hasField("appLockPassword")) {
                    saved.appLockPassword ?: ""
                } else {
                    ""
                }
                val savedPasswordLength = if (savedJson.hasField("appLockPasswordLength")) {
                    saved.appLockPasswordLength.coerceIn(4, 8)
                } else {
                    4
                }
                val savedPattern = if (savedJson.hasField("appLockPattern")) {
                    saved.appLockPattern ?: ""
                } else {
                    ""
                }
                val normalizedDuressPassword = if (savedJson.hasField("appLockDuressPassword")) {
                    saved.appLockDuressPassword?.takeIf {
                        savedPassword.isNotBlank() &&
                            it.isNotBlank() &&
                            it != savedPassword &&
                            it.length == savedPasswordLength &&
                            it.all(Char::isDigit)
                    }.orEmpty()
                } else {
                    ""
                }
                val normalizedDuressPattern = if (savedJson.hasField("appLockDuressPattern")) {
                    saved.appLockDuressPattern?.takeIf {
                        savedPattern.isNotBlank() &&
                            it.isNotBlank() &&
                            it != savedPattern
                    }.orEmpty()
                } else {
                    ""
                }
                saved.copy(
                    comicApiSourceList = listOf(
                        COMIC_API_SOURCE_BUILTIN,
                        COMIC_API_SOURCE_NETWORK,
                        COMIC_API_SOURCE_MIXED
                    ),
                    comicApiSource = if (savedJson.hasField("comicApiSource")) {
                        listOf(COMIC_API_SOURCE_BUILTIN, COMIC_API_SOURCE_NETWORK, COMIC_API_SOURCE_MIXED)
                            .firstOrNull { it == saved.comicApiSource }
                            ?: COMIC_API_SOURCE_BUILTIN
                    } else {
                        COMIC_API_SOURCE_BUILTIN
                    },
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
                    blockedTagList = flattenBlockedTagTemplates(migratedTemplates),
                    blockedTagTemplateList = migratedTemplates,
                    appLockPassword = savedPassword,
                    appLockPasswordLength = savedPasswordLength,
                    appLockPattern = savedPattern,
                    appLockUnlockMode = migratedUnlockMode,
                    appLockFingerprintEnabled = if (savedJson.hasField("appLockFingerprintEnabled")) {
                        saved.appLockFingerprintEnabled
                    } else {
                        saved.appLockBiometricEnabled
                    },
                    appLockFaceEnabled = if (savedJson.hasField("appLockFaceEnabled")) {
                        saved.appLockFaceEnabled
                    } else false,
                    appLockUnlockRule = if (savedJson.hasField("appLockUnlockRule")) {
                        saved.appLockUnlockRule
                    } else if (migratedUnlockMode == "both") {
                        APP_LOCK_RULE_REQUIRED
                    } else APP_LOCK_RULE_ANY,
                    appLockRequiredMethods = if (savedJson.hasField("appLockRequiredMethods")) {
                        saved.appLockRequiredMethods.filter {
                            it in setOf(APP_LOCK_TYPE_PASSWORD, APP_LOCK_TYPE_PATTERN, APP_LOCK_METHOD_BIOMETRIC)
                        }
                    } else if (migratedUnlockMode == "both") {
                        listOf(APP_LOCK_TYPE_PASSWORD, APP_LOCK_TYPE_PATTERN)
                    } else emptyList(),
                    appLockDuressEnabled = savedJson.hasField("appLockDuressEnabled") &&
                        saved.appLockDuressEnabled &&
                        (normalizedDuressPassword.isNotBlank() || normalizedDuressPattern.isNotBlank()),
                    appLockDuressPassword = normalizedDuressPassword,
                    appLockDuressPattern = normalizedDuressPattern,
                    colorPalettePreset = if (savedJson.hasField("colorPalettePreset")) {
                        saved.colorPalettePreset
                    } else {
                        COLOR_PALETTE_PRESET_DEFAULT
                    },
                    customColorPrimary = if (savedJson.hasField("customColorPrimary")) {
                        saved.customColorPrimary
                    } else {
                        null
                    },
                    customColorSecondary = if (savedJson.hasField("customColorSecondary")) {
                        saved.customColorSecondary
                    } else {
                        null
                    },
                    customColorTertiary = if (savedJson.hasField("customColorTertiary")) {
                        saved.customColorTertiary
                    } else {
                        null
                    },
                    customColorError = if (savedJson.hasField("customColorError")) {
                        saved.customColorError
                    } else {
                        null
                    },
                    dohEnabled = if (savedJson.hasField("dohEnabled")) saved.dohEnabled else false,
                    dohAutoStart = if (savedJson.hasField("dohAutoStart")) saved.dohAutoStart else false,
                    dohServerId = if (savedJson.hasField("dohServerId")) {
                        saved.dohServerId.takeIf { it.isNotBlank() } ?: "tencent"
                    } else {
                        "tencent"
                    },
                    dohCustomServerName = if (savedJson.hasField("dohCustomServerName")) {
                        saved.dohCustomServerName.orEmpty()
                    } else {
                        ""
                    },
                    dohCustomServerUrl = if (savedJson.hasField("dohCustomServerUrl")) {
                        saved.dohCustomServerUrl.orEmpty()
                    } else {
                        ""
                    },
                    dohUseDeviceCertificates = if (savedJson.hasField("dohUseDeviceCertificates")) {
                        saved.dohUseDeviceCertificates
                    } else {
                        true
                    },
                    dohPreferIpv6 = if (savedJson.hasField("dohPreferIpv6")) saved.dohPreferIpv6 else false,
                    showComicIdAsSubtitle = if (savedJson.hasField("showComicIdAsSubtitle")) {
                        saved.showComicIdAsSubtitle
                    } else {
                        false
                    },
                    comicReadingMemoryEnabled = if (savedJson.hasField("comicReadingMemoryEnabled")) {
                        saved.comicReadingMemoryEnabled
                    } else {
                        true
                    },
                    chapterReadingMemoryEnabled = if (savedJson.hasField("chapterReadingMemoryEnabled")) {
                        saved.chapterReadingMemoryEnabled
                    } else {
                        true
                    },
                    volumeKeyPageTurningEnabled = if (savedJson.hasField("volumeKeyPageTurningEnabled")) {
                        saved.volumeKeyPageTurningEnabled
                    } else {
                        false
                    },
                    coverCacheDurationHours = if (savedJson.hasField("coverCacheDurationHours")) {
                        saved.coverCacheDurationHours.coerceIn(0, COVER_CACHE_MAX_DURATION_HOURS)
                    } else {
                        DEFAULT_COVER_CACHE_DURATION_HOURS
                    },
                    woodenFishCount = if (savedJson.hasField("woodenFishCount")) {
                        saved.woodenFishCount.coerceAtLeast(0L)
                    } else {
                        0L
                    },
                    woodenFishSoundEnabled = if (savedJson.hasField("woodenFishSoundEnabled")) {
                        saved.woodenFishSoundEnabled
                    } else {
                        true
                    },
                    woodenFishVibrationEnabled = if (savedJson.hasField("woodenFishVibrationEnabled")) {
                        saved.woodenFishVibrationEnabled
                    } else {
                        true
                    },
                    splashLoadingEnabled = if (savedJson.hasField("splashLoadingEnabled")) {
                        saved.splashLoadingEnabled
                    } else {
                        true
                    },
                    aiModel = if (savedJson.hasField("aiModel")) {
                        AiChatModel.fromId(saved.aiModel).id
                    } else {
                        AiChatModel.UnlimitedAi.id
                    },
                )
            }
        }
        return _state.value ?: LocalSetting()
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

/**
 * 解析旧版本存储中的 appLockType 字段（已废弃，迁移到 appLockUnlockMode）
 */
private fun parseLegacyAppLockType(json: String?): String? {
    if (json.isNullOrBlank()) return null
    return runCatching {
        val obj = JsonParser.parseString(json).asJsonObject
        if (!obj.has("appLockType")) return null
        val value = obj.get("appLockType").asString
        when (value) {
            APP_LOCK_TYPE_PATTERN -> APP_LOCK_TYPE_PATTERN
            else -> APP_LOCK_TYPE_PASSWORD
        }
    }.getOrNull()
}
