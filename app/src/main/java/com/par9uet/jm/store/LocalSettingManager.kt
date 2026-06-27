package com.par9uet.jm.store

import com.par9uet.jm.data.models.LauncherDisguise
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.storage.LocalSettingStorage
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.utils.LauncherDisguiseApplier
import com.par9uet.jm.utils.normalizeBlockedTag
import com.par9uet.jm.utils.normalizeBlockedTagList
import com.par9uet.jm.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalSettingManager(
    private val localSettingStorage: LocalSettingStorage,
    private val launcherDisguiseApplier: LauncherDisguiseApplier,
) : AppInitTask {
    private val _localSettingState = MutableStateFlow(LocalSetting())
    val localSettingState = _localSettingState.asStateFlow()

    fun updateApi(api: String) {
        _localSettingState.update {
            it.copy(
                api = api
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updateTheme(theme: String) {
        _localSettingState.update {
            it.copy(
                theme = theme
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updateShunt(shunt: String) {
        _localSettingState.update {
            it.copy(
                shunt = shunt
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updatePrefetchCount(prefetchCount: String) {
        _localSettingState.update {
            it.copy(
                prefetchCount = prefetchCount.toInt()
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updateReadMode(readMode: String) {
        _localSettingState.update {
            it.copy(
                readMode = readMode
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun closeShowComicScrollReadTip() {
        _localSettingState.update {
            it.copy(
                showComicScrollReadTip = false
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun closeShowComicPageReadTip() {
        _localSettingState.update {
            it.copy(
                showComicPageReadTip = false
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updateReadTapMode(readTapMode: String) {
        _localSettingState.update {
            it.copy(
                readTapMode = readTapMode
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updateLauncherDisguise(launcherDisguise: String) {
        val disguise = LauncherDisguise.fromId(launcherDisguise)
        _localSettingState.update {
            it.copy(
                launcherDisguise = disguise.id
            )
        }
        localSettingStorage.set(_localSettingState.value)
        launcherDisguiseApplier.apply(disguise)
    }

    fun updateNotificationSettings(show: Boolean, showName: Boolean) {
        _localSettingState.update {
            it.copy(
                showComicCacheNotification = show,
                showComicCacheNotificationName = show && showName
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updateShowComicCacheNotification(show: Boolean) {
        _localSettingState.update {
            it.copy(
                showComicCacheNotification = show
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun updateShowComicCacheNotificationName(show: Boolean) {
        _localSettingState.update {
            it.copy(
                showComicCacheNotificationName = show
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun addBlockedTag(tag: String) {
        val normalizedTag = normalizeBlockedTag(tag)
        if (normalizedTag.isBlank()) return
        _localSettingState.update {
            it.copy(
                blockedTagList = normalizeBlockedTagList(it.blockedTagList + normalizedTag)
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun replaceBlockedTags(tags: List<String>) {
        _localSettingState.update {
            it.copy(
                blockedTagList = normalizeBlockedTagList(tags)
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    fun removeBlockedTag(tag: String) {
        val normalizedTag = normalizeBlockedTag(tag)
        _localSettingState.update {
            it.copy(
                blockedTagList = it.blockedTagList.filterNot { item ->
                    item.equals(normalizedTag, ignoreCase = true)
                }
            )
        }
        localSettingStorage.set(_localSettingState.value)
    }

    private var appTaskInfo = AppTaskInfo(
        taskName = "加载本地 APP 设置",
        sort = 3,
    )

    override suspend fun init() {
        log("本地应用设置开始初始化")
        log("加载本地应用设置")
        _localSettingState.update {
            localSettingStorage.get()
        }
        launcherDisguiseApplier.apply(LauncherDisguise.fromId(_localSettingState.value.launcherDisguise))
        log("已加载本地应用设置")
        log("本地应用设置初始化结束")
    }

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}
