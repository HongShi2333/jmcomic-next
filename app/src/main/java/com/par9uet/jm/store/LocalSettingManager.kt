package com.par9uet.jm.store

import com.par9uet.jm.data.models.BlockedTagTemplate
import com.par9uet.jm.data.models.AiChatModel
import com.par9uet.jm.data.models.COVER_CACHE_MAX_DURATION_HOURS
import com.par9uet.jm.data.models.LauncherDisguise
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.storage.LocalSettingStorage
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.utils.LauncherDisguiseApplier
import com.par9uet.jm.utils.flattenBlockedTagTemplates
import com.par9uet.jm.utils.log
import com.par9uet.jm.utils.normalizeBlockedTag
import com.par9uet.jm.utils.normalizeBlockedTagList
import com.par9uet.jm.utils.normalizeBlockedTagTemplates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalSettingManager(
    private val localSettingStorage: LocalSettingStorage,
    private val launcherDisguiseApplier: LauncherDisguiseApplier,
) : AppInitTask {
    @Volatile private var initialized = false
    private val _localSettingState = MutableStateFlow(LocalSetting())
    val localSettingState = _localSettingState.asStateFlow()

    fun updateComicApiSource(comicApiSource: String) =
        updateSetting { it.copy(comicApiSource = comicApiSource) }

    fun updateDownloadTreeUri(uri: String) =
        updateSetting { it.copy(downloadTreeUri = uri) }

    fun updatePreferenceRecommendEnabled(enabled: Boolean) =
        updateSetting { it.copy(preferenceRecommendEnabled = enabled) }

    fun updateOnboardingCompleted(completed: Boolean) =
        updateSetting { it.copy(onboardingCompleted = completed) }

    fun updateClipboardAutoDetectEnabled(enabled: Boolean) =
        updateSetting { it.copy(clipboardAutoDetectEnabled = enabled) }

    fun updateAutoSignInEnabled(enabled: Boolean) =
        updateSetting { it.copy(autoSignInEnabled = enabled) }

    fun updateRecommendSource(source: String) =
        updateSetting { it.copy(recommendSource = source) }

    fun updateApi(api: String) = updateSetting { it.copy(api = api) }

    fun updateDohEnabled(enabled: Boolean) =
        updateSetting { it.copy(dohEnabled = enabled) }

    fun updateDohAutoStart(enabled: Boolean) =
        updateSetting { it.copy(dohAutoStart = enabled) }

    fun updateDohServer(id: String) =
        updateSetting { it.copy(dohServerId = id) }

    fun updateDohCustomServer(name: String, url: String) =
        updateSetting {
            it.copy(
                dohServerId = "custom",
                dohCustomServerName = name.trim(),
                dohCustomServerUrl = url.trim(),
            )
        }

    fun updateDohUseDeviceCertificates(enabled: Boolean) =
        updateSetting { it.copy(dohUseDeviceCertificates = enabled) }

    fun updateDohPreferIpv6(enabled: Boolean) =
        updateSetting { it.copy(dohPreferIpv6 = enabled) }

    fun updateTheme(theme: String) = updateSetting { it.copy(theme = theme) }

    fun updateShunt(shunt: String) = updateSetting { it.copy(shunt = shunt) }

    fun updatePrefetchCount(prefetchCount: String) =
        updateSetting { it.copy(prefetchCount = prefetchCount.toInt()) }

    fun updateReadMode(readMode: String) = updateSetting { it.copy(readMode = readMode) }

    fun closeShowComicScrollReadTip() =
        updateSetting { it.copy(showComicScrollReadTip = false) }

    fun closeShowComicPageReadTip() =
        updateSetting { it.copy(showComicPageReadTip = false) }

    fun updateReadTapMode(readTapMode: String) =
        updateSetting { it.copy(readTapMode = readTapMode) }

    fun updateShowComicIdAsSubtitle(enabled: Boolean) =
        updateSetting { it.copy(showComicIdAsSubtitle = enabled) }

    fun updateComicReadingMemoryEnabled(enabled: Boolean) =
        updateSetting { it.copy(comicReadingMemoryEnabled = enabled) }

    fun updateChapterReadingMemoryEnabled(enabled: Boolean) =
        updateSetting { it.copy(chapterReadingMemoryEnabled = enabled) }

    fun updateVolumeKeyPageTurningEnabled(enabled: Boolean) =
        updateSetting { it.copy(volumeKeyPageTurningEnabled = enabled) }

    fun updateCoverCacheDurationHours(hours: Int) =
        updateSetting { it.copy(coverCacheDurationHours = hours.coerceIn(0, COVER_CACHE_MAX_DURATION_HOURS)) }

    fun updateLauncherDisguise(launcherDisguise: String) {
        val disguise = LauncherDisguise.fromId(launcherDisguise)
        updateSetting { it.copy(launcherDisguise = disguise.id) }
        launcherDisguiseApplier.apply(disguise)
    }

    fun updateWoodenFishCount(count: Long) =
        updateSetting { it.copy(woodenFishCount = count.coerceAtLeast(0L)) }

    fun updateWoodenFishSoundEnabled(enabled: Boolean) =
        updateSetting { it.copy(woodenFishSoundEnabled = enabled) }

    fun updateWoodenFishVibrationEnabled(enabled: Boolean) =
        updateSetting { it.copy(woodenFishVibrationEnabled = enabled) }

    fun updateSplashLoadingEnabled(enabled: Boolean) =
        updateSetting { it.copy(splashLoadingEnabled = enabled) }

    fun updateAiModel(model: String) =
        updateSetting { it.copy(aiModel = AiChatModel.fromId(model).id) }

    fun updateNotificationSettings(show: Boolean, showName: Boolean) =
        updateSetting {
            it.copy(
                showComicCacheNotification = show,
                showComicCacheNotificationName = show && showName
            )
        }

    fun updateShowComicCacheNotification(show: Boolean) =
        updateSetting { it.copy(showComicCacheNotification = show) }

    fun updateShowComicCacheNotificationName(show: Boolean) =
        updateSetting { it.copy(showComicCacheNotificationName = show) }

    fun updateShowCacheMigrationNotification(show: Boolean) =
        updateSetting { it.copy(showCacheMigrationNotification = show) }

    fun updateShowAiEntry(show: Boolean) =
        updateSetting { it.copy(showAiEntry = show) }

    fun addBlockedTag(tag: String) {
        val normalizedTag = normalizeBlockedTag(tag)
        if (normalizedTag.isBlank()) return
        updateSetting {
            it.copy(
                blockedTagList = normalizeBlockedTagList(it.blockedTagList + normalizedTag)
            )
        }
    }

    fun replaceBlockedTags(tags: List<String>) =
        updateSetting { it.copy(blockedTagList = normalizeBlockedTagList(tags)) }

    fun removeBlockedTag(tag: String) {
        val normalizedTag = normalizeBlockedTag(tag)
        updateSetting {
            it.copy(
                blockedTagList = it.blockedTagList.filterNot { item ->
                    item.equals(normalizedTag, ignoreCase = true)
                }
            )
        }
    }

    fun saveBlockedTagTemplate(index: Int?, name: String, tags: List<String>) {
        val normalizedTags = normalizeBlockedTagList(tags)
        if (normalizedTags.isEmpty()) return
        val template = BlockedTagTemplate(
            name = name.trim().ifBlank { "排除模板" },
            tagList = normalizedTags
        )
        updateSetting { setting ->
            val mutable = setting.blockedTagTemplateList.toMutableList()
            if (index != null && index in mutable.indices) {
                mutable[index] = template
            } else {
                mutable += template
            }
            setting.withBlockedTagTemplates(mutable)
        }
    }

    fun removeBlockedTagTemplate(index: Int) =
        updateSetting { setting ->
            if (index !in setting.blockedTagTemplateList.indices) {
                setting
            } else {
                setting.withBlockedTagTemplates(
                    setting.blockedTagTemplateList.filterIndexed { i, _ -> i != index }
                )
            }
        }

    fun replaceBlockedTagTemplates(templates: List<BlockedTagTemplate>) =
        updateSetting { it.withBlockedTagTemplates(templates) }

    fun updateAppLockEnabled(enabled: Boolean) =
        updateSetting { it.copy(appLockEnabled = enabled) }

    fun updateAppLockPassword(pwd: String) =
        updateSetting {
            val nextDuressPassword = it.appLockDuressPassword.takeIf { duress ->
                pwd.isNotBlank() &&
                    duress != pwd &&
                    duress.length == pwd.length &&
                    duress.all(Char::isDigit)
            }.orEmpty()
            it.copy(
                appLockPassword = pwd,
                appLockDuressPassword = nextDuressPassword,
                appLockDuressEnabled = if (nextDuressPassword.isBlank() && it.appLockDuressPattern.isBlank()) {
                    false
                } else it.appLockDuressEnabled,
            )
        }

    fun updateAppLockPasswordLength(len: Int) =
        updateSetting {
            val normalizedLength = len.coerceIn(4, 8)
            val nextDuressPassword = it.appLockDuressPassword.takeIf { password ->
                password.length == normalizedLength && password.all(Char::isDigit)
            }.orEmpty()
            it.copy(
                appLockPasswordLength = normalizedLength,
                appLockDuressPassword = nextDuressPassword,
                appLockDuressEnabled = if (nextDuressPassword.isBlank() && it.appLockDuressPattern.isBlank()) {
                    false
                } else it.appLockDuressEnabled,
            )
        }

    fun updateAppLockPattern(pattern: String) =
        updateSetting {
            val nextDuressPattern = it.appLockDuressPattern.takeIf { duress ->
                pattern.isNotBlank() && duress != pattern
            }.orEmpty()
            it.copy(
                appLockPattern = pattern,
                appLockDuressPattern = nextDuressPattern,
                appLockDuressEnabled = if (nextDuressPattern.isBlank() && it.appLockDuressPassword.isBlank()) {
                    false
                } else it.appLockDuressEnabled,
            )
        }

    fun updateAppLockUnlockMode(mode: String) =
        updateSetting { it.copy(appLockUnlockMode = mode) }

    fun updateAppLockBiometricEnabled(enabled: Boolean) =
        updateSetting { it.copy(appLockBiometricEnabled = enabled) }

    fun updateAppLockFingerprintEnabled(enabled: Boolean) =
        updateSetting { it.copy(appLockFingerprintEnabled = enabled, appLockBiometricEnabled = enabled || it.appLockFaceEnabled) }

    fun updateAppLockFaceEnabled(enabled: Boolean) =
        updateSetting { it.copy(appLockFaceEnabled = enabled, appLockBiometricEnabled = enabled || it.appLockFingerprintEnabled) }

    fun updateAppLockUnlockRule(rule: String) =
        updateSetting { it.copy(appLockUnlockRule = rule) }

    fun updateAppLockRequiredMethods(methods: List<String>) =
        updateSetting { it.copy(appLockRequiredMethods = methods.distinct()) }

    fun updateAppLockDuressEnabled(enabled: Boolean) =
        updateSetting {
            it.copy(
                appLockDuressEnabled = enabled &&
                    (it.appLockDuressPassword.isNotBlank() || it.appLockDuressPattern.isNotBlank())
            )
        }

    fun updateAppLockDuressPassword(password: String) =
        updateSetting {
            val valid = password.isNotBlank() &&
                password != it.appLockPassword &&
                password.length == it.appLockPasswordLength &&
                password.all(Char::isDigit)
            val nextPassword = if (valid) password else ""
            it.copy(
                appLockDuressPassword = nextPassword,
                appLockDuressEnabled = if (nextPassword.isBlank() && it.appLockDuressPattern.isBlank()) {
                    false
                } else it.appLockDuressEnabled,
            )
        }

    fun updateAppLockDuressPattern(pattern: String) =
        updateSetting {
            val valid = pattern.isNotBlank() && pattern != it.appLockPattern
            val nextPattern = if (valid) pattern else ""
            it.copy(
                appLockDuressPattern = nextPattern,
                appLockDuressEnabled = if (nextPattern.isBlank() && it.appLockDuressPassword.isBlank()) {
                    false
                } else it.appLockDuressEnabled,
            )
        }

    fun updateColorPalettePreset(preset: String) =
        updateSetting { it.copy(colorPalettePreset = preset) }

    fun updateCustomColor(primary: String?, secondary: String?, tertiary: String?, error: String?) =
        updateSetting {
            it.copy(
                customColorPrimary = primary,
                customColorSecondary = secondary,
                customColorTertiary = tertiary,
                customColorError = error,
            )
        }

    fun dismissNsfwWarning() =
        updateSetting { it.copy(nsfwWarningDismissed = true) }

    fun updateHomeGridColumns(columns: Int) =
        updateSetting { it.copy(homeGridColumns = columns.coerceIn(0, 6)) }

    fun updateCollectGridColumns(columns: Int) =
        updateSetting { it.copy(collectGridColumns = columns.coerceIn(0, 6)) }

    fun updateDownloadGridColumns(columns: Int) =
        updateSetting { it.copy(downloadGridColumns = columns.coerceIn(0, 6)) }

    fun updateHistoryGridColumns(columns: Int) =
        updateSetting { it.copy(historyGridColumns = columns.coerceIn(0, 6)) }

    fun updateSearchGridColumns(columns: Int) =
        updateSetting { it.copy(searchGridColumns = columns.coerceIn(0, 6)) }

    fun updateHomeExcludedTags(tags: List<String>) =
        updateSetting { it.copy(homeExcludedTags = tags) }

    fun updateReadMemoryOptEnabled(enabled: Boolean) =
        updateSetting { it.copy(readMemoryOptEnabled = enabled) }

    fun updateReadDecodeConcurrency(concurrency: Int) =
        updateSetting { it.copy(readDecodeConcurrency = concurrency.coerceIn(1, 4)) }

    fun updateCacheIntegrityCheckMode(mode: String) =
        updateSetting {
            it.copy(
                cacheIntegrityCheckMode = mode.takeIf { value ->
                    value in setOf("off", "partial", "full")
                } ?: "off"
            )
        }

    /**
     * 应用从备份恢复的 [LocalSetting]。
     *
     * 备份中已剥离应用锁与胁迫凭据明文，因此恢复时保留当前设备的应用锁
     * 相关字段，避免恢复后应用锁状态异常。
     * 若恢复导致 launcherDisguise 变化，会重新应用伪装图标。
     */
    fun applyLocalSetting(setting: LocalSetting, includeDownloadPath: Boolean = true) {
        val previousLauncherDisguise = _localSettingState.value.launcherDisguise
        updateSetting { current ->
            setting.copy(
                appLockEnabled = current.appLockEnabled,
                appLockPassword = current.appLockPassword,
                appLockPasswordLength = current.appLockPasswordLength,
                appLockPattern = current.appLockPattern,
                appLockUnlockMode = current.appLockUnlockMode,
                appLockBiometricEnabled = current.appLockBiometricEnabled,
                appLockFingerprintEnabled = current.appLockFingerprintEnabled,
                appLockFaceEnabled = current.appLockFaceEnabled,
                appLockUnlockRule = current.appLockUnlockRule,
                appLockRequiredMethods = current.appLockRequiredMethods,
                appLockDuressEnabled = current.appLockDuressEnabled,
                appLockDuressPassword = current.appLockDuressPassword,
                appLockDuressPattern = current.appLockDuressPattern,
                downloadTreeUri = if (includeDownloadPath) setting.downloadTreeUri else current.downloadTreeUri,
            )
        }
        val newLauncherDisguise = _localSettingState.value.launcherDisguise
        if (newLauncherDisguise != previousLauncherDisguise) {
            launcherDisguiseApplier.apply(LauncherDisguise.fromId(newLauncherDisguise))
        }
    }

    private fun updateSetting(update: (LocalSetting) -> LocalSetting) {
        _localSettingState.update(update)
        localSettingStorage.set(_localSettingState.value)
    }

    private fun LocalSetting.withBlockedTagTemplates(templates: List<BlockedTagTemplate>): LocalSetting {
        val normalizedTemplates = normalizeBlockedTagTemplates(templates)
        return copy(
            blockedTagTemplateList = normalizedTemplates,
            blockedTagList = flattenBlockedTagTemplates(normalizedTemplates)
        )
    }

    private var appTaskInfo = AppTaskInfo(
        taskName = "load local app settings",
        // All network interceptors read this state, so load it before remote settings.
        sort = 0,
    )

    override suspend fun init() {
        if (initialized) return
        log("local app settings init start")
        _localSettingState.update {
            localSettingStorage.get()
        }
        launcherDisguiseApplier.apply(LauncherDisguise.fromId(_localSettingState.value.launcherDisguise))
        initialized = true
        log("local app settings init finished")
    }

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}
