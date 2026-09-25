package com.par9uet.jm.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.COMIC_API_SOURCE_BUILTIN
import com.par9uet.jm.data.models.COMIC_API_SOURCE_MIXED
import com.par9uet.jm.data.models.COMIC_API_SOURCE_NETWORK
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_FULL
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_OFF
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_PARTIAL
import com.par9uet.jm.data.models.LauncherDisguise
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.network.DohManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.SelectDialog
import com.par9uet.jm.ui.components.SelectOption
import org.koin.compose.getKoin
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.lifecycle.Observer
import com.par9uet.jm.worker.CACHE_MIGRATION_PROGRESS
import com.par9uet.jm.worker.CACHE_MIGRATION_STAGE
import com.par9uet.jm.worker.CACHE_MIGRATION_TARGET_URI
import com.par9uet.jm.worker.CACHE_MIGRATION_WORK_NAME
import com.par9uet.jm.cache.ensureDownloadTreeHiddenFromGallery
import com.par9uet.jm.worker.CacheMigrationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private sealed class SettingType {
    object ComicApiSource : SettingType()
    object Api : SettingType()
    object Theme : SettingType()
    object LauncherDisguise : SettingType()
    object Shunt : SettingType()
    object PrefetchCount : SettingType()
    object ReadMode : SettingType()
    object ReadTapMode : SettingType()
    object NotificationManagement : SettingType()
    object RecommendSource : SettingType()
    object AllGridColumns : SettingType()
    object ReadDecodeConcurrency : SettingType()
    object CacheIntegrityCheck : SettingType()
}

private const val NOTIFICATION_ON_WITH_NAME = "on_with_name"
private const val NOTIFICATION_ON_WITHOUT_NAME = "on_without_name"
private const val NOTIFICATION_OFF = "off"

private val themeTextMap = mapOf(
    "auto" to "\u8ddf\u968f\u7cfb\u7edf",
    "light" to "\u65e5\u95f4\u6a21\u5f0f",
    "dark" to "\u591c\u95f4\u6a21\u5f0f",
)

private val comicApiSourceTextMap = mapOf(
    COMIC_API_SOURCE_BUILTIN to "\u5185\u7f6e API",
    COMIC_API_SOURCE_NETWORK to "\u7f51\u7edc API",
    COMIC_API_SOURCE_MIXED to "\u6df7\u5408 API",
)

private fun gridColumnsText(columns: Int): String =
    if (columns == 0) "\u81ea\u9002\u5e94" else "$columns \u5217"

@Composable
fun LocalSettingScreen(
    localSettingManager: LocalSettingManager = getKoin().get(),
    dohManager: DohManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val context = LocalContext.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val dohStatus by dohManager.status.collectAsState()
    var settingType by remember { mutableStateOf<SettingType>(SettingType.Api) }
    var isOpenSettingSelectDialog by remember { mutableStateOf(false) }
    var showHomeExcludedTagsDialog by remember { mutableStateOf(false) }
    var showCachePathDialog by remember { mutableStateOf(false) }
    val cachePathScope = rememberCoroutineScope()
    val workManager = remember(context) { WorkManager.getInstance(context) }
    var migrationWorkList by remember { mutableStateOf<List<WorkInfo>>(emptyList()) }
    DisposableEffect(workManager) {
        val liveData = workManager.getWorkInfosForUniqueWorkLiveData(CACHE_MIGRATION_WORK_NAME)
        val observer = Observer<List<WorkInfo>> { migrationWorkList = it.orEmpty() }
        liveData.observeForever(observer)
        onDispose { liveData.removeObserver(observer) }
    }
    val migrationWork = migrationWorkList.firstOrNull { !it.state.isFinished } ?: migrationWorkList.lastOrNull()
    val migrationActive = migrationWork?.state in setOf(
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.RUNNING,
        WorkInfo.State.BLOCKED,
    )
    var hiddenMigrationWorkId by remember { mutableStateOf<java.util.UUID?>(null) }

    fun startCacheMigration(targetUri: String) {
        val request = OneTimeWorkRequestBuilder<CacheMigrationWorker>()
            .setInputData(workDataOf(CACHE_MIGRATION_TARGET_URI to targetUri))
            .build()
        hiddenMigrationWorkId = null
        workManager.enqueueUniqueWork(CACHE_MIGRATION_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    val cacheFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            val treeUri = uri.toString()
            cachePathScope.launch(Dispatchers.IO) {
                ensureDownloadTreeHiddenFromGallery(context, treeUri)
            }
            startCacheMigration(treeUri)
        }
    }

    fun openSetting(type: SettingType) {
        settingType = type
        isOpenSettingSelectDialog = true
    }

    // 应用锁状态文本
    val appLockStatusText by remember(localSetting) {
        derivedStateOf {
            if (!localSetting.appLockEnabled) {
                "\u672a\u542f\u7528"
            } else {
                val methods = buildList {
                    if (localSetting.appLockPassword.isNotEmpty()) add("\u5bc6\u7801")
                    if (localSetting.appLockPattern.isNotEmpty()) add("\u56fe\u6848")
                }
                if (methods.isEmpty()) {
                    "\u5df2\u542f\u7528"
                } else {
                    "\u5df2\u542f\u7528 - ${methods.joinToString("+")}"
                }
            }
        }
    }

    CommonScaffold(title = "\u8bbe\u7f6e") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "界面与交互") {
                    SettingsRow(Icons.Rounded.DarkMode, "\u4e3b\u9898", themeTextMap[localSetting.theme].orEmpty()) {
                        openSetting(SettingType.Theme)
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = "\u8c03\u8272\u677f",
                        value = when (localSetting.colorPalettePreset) {
                            "custom" -> "\u81ea\u5b9a\u4e49"
                            "monet" -> "\u83ab\u5948\u53d6\u8272"
                            else -> "\u9884\u8bbe\u65b9\u6848"
                        }
                    ) {
                        mainNavController.navigate("colorPalette")
                    }
                    SettingsRow(Icons.Rounded.Image, "\u56fe\u6807\u4f2a\u88c5", LauncherDisguise.fromId(localSetting.launcherDisguise).label) {
                        openSetting(SettingType.LauncherDisguise)
                    }
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Psychology,
                        title = "\u663e\u793a AI",
                        value = localSetting.showAiEntry,
                        onCheckedChange = localSettingManager::updateShowAiEntry
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.ContentPaste,
                        title = "\u526a\u5207\u677f\u81ea\u52a8\u68c0\u6d4b",
                        value = localSetting.clipboardAutoDetectEnabled,
                        onCheckedChange = localSettingManager::updateClipboardAutoDetectEnabled
                    )
                    SettingsRow(
                        Icons.Rounded.GridView,
                        "\u7f51\u683c\u5217\u6570",
                        "\u9996\u9875 ${gridColumnsText(localSetting.homeGridColumns)} \u00b7 \u6536\u85cf ${gridColumnsText(localSetting.collectGridColumns)} \u00b7 \u7f13\u5b58 ${gridColumnsText(localSetting.downloadGridColumns)} \u00b7 \u5386\u53f2 ${gridColumnsText(localSetting.historyGridColumns)} \u00b7 \u641c\u7d22 ${gridColumnsText(localSetting.searchGridColumns)}"
                    ) {
                        openSetting(SettingType.AllGridColumns)
                    }
                }
            }
            item {
                SettingsSection(title = "安全与账户") {
                    SettingsRow(
                        icon = Icons.Rounded.Lock,
                        title = "\u5e94\u7528\u9501",
                        value = appLockStatusText
                    ) {
                        mainNavController.navigate("appLockSetting")
                    }
                    SettingsRow(Icons.Rounded.CloudSync, "数据备份", "备份与恢复应用设置") {
                        mainNavController.navigate("backupRestore")
                    }
                }
            }
            item {
                SettingsSection(title = "网络与数据") {
                    SettingsRow(
                        Icons.Rounded.Dns,
                        "DoH",
                        if (dohStatus.active) "已启用 · ${dohStatus.serverName}" else "未启用",
                    ) {
                        mainNavController.navigate("dohSetting")
                    }
                    SettingsRow(
                        Icons.Rounded.Api,
                        "\u6570\u636e\u6e90",
                        comicApiSourceTextMap[localSetting.comicApiSource].orEmpty()
                    ) {
                        openSetting(SettingType.ComicApiSource)
                    }
                    if (localSetting.comicApiSource == COMIC_API_SOURCE_NETWORK || localSetting.comicApiSource == COMIC_API_SOURCE_MIXED) {
                        SettingsRow(Icons.Rounded.Api, "API", localSetting.api) {
                            openSetting(SettingType.Api)
                        }
                        SettingsRow(Icons.Rounded.Image, "\u56fe\u7247\u7ebf\u8def", "\u7ebf\u8def ${localSetting.shunt}") {
                            openSetting(SettingType.Shunt)
                        }
                    }
                    if (localSetting.comicApiSource == COMIC_API_SOURCE_BUILTIN || localSetting.comicApiSource == COMIC_API_SOURCE_MIXED) {
                        SettingsSwitchRow(
                            icon = Icons.Rounded.Recommend,
                            title = "\u504f\u597d\u63a8\u8350",
                            value = localSetting.preferenceRecommendEnabled,
                            onCheckedChange = { localSettingManager.updatePreferenceRecommendEnabled(it) }
                        )
                        if (localSetting.preferenceRecommendEnabled) {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                text = "\u5f00\u542f\u540e\u5c06\u8bf7\u6c42\u7f51\u7edc API \u83b7\u53d6\u57fa\u4e8e\u767b\u5f55\u8d26\u53f7\u7684\u4e2a\u6027\u5316\u63a8\u8350\uff0c\u53ef\u80fd\u4e0d\u7a33\u5b9a",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsRow(
                                icon = Icons.Rounded.Source,
                                title = "\u63a8\u8350\u6e90",
                                value = if (localSetting.recommendSource == "builtin") "\u5185\u7f6e API \u63a8\u8350" else "\u7f51\u7edc API \u63a8\u8350"
                            ) {
                                openSetting(SettingType.RecommendSource)
                            }
                        }
                        SettingsRow(
                            icon = Icons.Rounded.Block,
                            title = "\u9996\u9875\u6807\u7b7e\u6392\u9664",
                            value = if (localSetting.homeExcludedTags.isEmpty()) "\u672a\u8bbe\u7f6e" else "${localSetting.homeExcludedTags.size} \u4e2a\u6807\u7b7e"
                        ) {
                            showHomeExcludedTagsDialog = true
                        }
                    }
                }
            }
            item {
                SettingsSection(title = "阅读与缓存") {
                    SettingsRow(Icons.Rounded.Tune, "\u56fe\u7247\u9884\u52a0\u8f7d", prefetchText(localSetting.prefetchCount)) {
                        openSetting(SettingType.PrefetchCount)
                    }
                    SettingsRow(Icons.AutoMirrored.Rounded.MenuBook, "\u9605\u8bfb\u6a21\u5f0f", readModeText(localSetting.readMode)) {
                        openSetting(SettingType.ReadMode)
                    }
                    SettingsRow(
                        Icons.Rounded.Tune,
                        "\u70b9\u51fb\u7ffb\u56fe",
                        if (localSetting.readTapMode == "side") "\u5de6\u53f3\u4e24\u4fa7" else "\u9ed8\u8ba4\u533a\u57df"
                    ) {
                        openSetting(SettingType.ReadTapMode)
                    }
                    SettingsRow(
                        Icons.Rounded.Memory,
                        "缓存检查",
                        cacheIntegrityCheckText(localSetting.cacheIntegrityCheckMode),
                    ) {
                        openSetting(SettingType.CacheIntegrityCheck)
                    }
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Memory,
                        title = "\u56fe\u7247\u5185\u5b58\u4f18\u5316",
                        value = localSetting.readMemoryOptEnabled,
                        onCheckedChange = { localSettingManager.updateReadMemoryOptEnabled(it) }
                    )
                    if (localSetting.readMemoryOptEnabled) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            text = "\u5f00\u542f\u540e\u9650\u5236\u5e76\u53d1\u89e3\u7801\u6570\u5e76\u964d\u4f4e\u91c7\u6837\u7387\uff0c\u7f13\u89e3\u4f4e\u7aef\u8bbe\u5907 OOM\uff1b\u63a8\u8350\u503c 2",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingsRow(
                            icon = Icons.Rounded.Memory,
                            title = "\u5e76\u53d1\u89e3\u7801\u6570",
                            value = "\u63a8\u8350 ${localSetting.readDecodeConcurrency}"
                        ) {
                            openSetting(SettingType.ReadDecodeConcurrency)
                        }
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Download,
                        title = "缓存路径",
                        value = if (localSetting.downloadTreeUri.isBlank()) "默认路径" else "自定义路径"
                    ) {
                        if (migrationActive) hiddenMigrationWorkId = null else showCachePathDialog = true
                    }
                    SettingsRow(Icons.Rounded.CleaningServices, "缓存清理", "清理图片、漫画等缓存文件") {
                        mainNavController.navigate("cacheCleanup")
                    }
                }
            }
            item {
                SettingsSection(title = "通知与后台") {
                    SettingsRow(Icons.Rounded.Notifications, "\u901a\u77e5\u7ba1\u7406", notificationText(localSetting)) {
                        openSetting(SettingType.NotificationManagement)
                    }
                    SettingsSwitchRow(
                        icon = Icons.Rounded.CloudSync,
                        title = "缓存迁移进度通知",
                        value = localSetting.showCacheMigrationNotification,
                        onCheckedChange = localSettingManager::updateShowCacheMigrationNotification,
                    )
                }
            }
            item {
                SettingsSection(title = "通用") {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.EventAvailable,
                        title = "\u81ea\u52a8\u7b7e\u5230",
                        value = localSetting.autoSignInEnabled,
                        onCheckedChange = { localSettingManager.updateAutoSignInEnabled(it) }
                    )
                    SettingsRow(Icons.Rounded.BugReport, "\u67e5\u770b\u65e5\u5fd7", "\u8c03\u8bd5\u548c\u9519\u8bef\u4fe1\u606f") {
                        mainNavController.navigate("logViewer")
                    }
                    SettingsRow(Icons.Rounded.Psychology, "\u4eba\u683c\u9762\u5177", "\u81ea\u5b9a\u4e49 AI \u540d\u5b57\u3001\u804c\u4e1a\u3001\u7b80\u4ecb\u7b49") {
                        mainNavController.navigate("personaManager")
                    }
                    SettingsRow(Icons.Rounded.SystemUpdate, "\u68c0\u67e5\u66f4\u65b0", "\u67e5\u770b GitHub Release \u6700\u65b0\u7248\u672c") {
                        mainNavController.navigate("checkUpdate")
                    }
                    SettingsRow(Icons.Rounded.Info, "\u5173\u4e8e", "\u5e94\u7528\u7248\u672c\u548c\u4ed3\u5e93") {
                        mainNavController.navigate("about")
                    }
                }
            }
        }

        if (isOpenSettingSelectDialog) {
            SettingSelectDialogContent(
                settingType = settingType,
                localSetting = localSetting,
                localSettingManager = localSettingManager,
                onDismiss = { isOpenSettingSelectDialog = false }
            )
        }
        if (showHomeExcludedTagsDialog) {
            HomeExcludedTagsDialog(
                tags = localSetting.homeExcludedTags,
                onConfirm = { tags ->
                    localSettingManager.updateHomeExcludedTags(tags)
                    showHomeExcludedTagsDialog = false
                },
                onDismiss = { showHomeExcludedTagsDialog = false }
            )
        }
        if (showCachePathDialog) {
            AlertDialog(
                onDismissRequest = { showCachePathDialog = false },
                title = { Text("\u7f13\u5b58\u8def\u5f84") },
                text = { Text("选择新位置后会迁移已有漫画缓存。应用会在自定义目录写入 .nomedia，避免缓存图片进入系统相册。迁移完成前继续使用原路径，可切换到后台并通过通知查看进度。") },
                confirmButton = {
                    TextButton(onClick = {
                        showCachePathDialog = false
                        cacheFolderLauncher.launch(null)
                    }) { Text("\u81ea\u5b9a\u4e49\u8def\u5f84") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCachePathDialog = false
                        startCacheMigration("")
                    }) { Text("\u9ed8\u8ba4\u8def\u5f84") }
                }
            )
        }
        if (migrationActive && migrationWork?.id != hiddenMigrationWorkId) {
            val progress = migrationWork?.progress?.getInt(CACHE_MIGRATION_PROGRESS, 0) ?: 0
            val stage = migrationWork?.progress?.getString(CACHE_MIGRATION_STAGE) ?: "正在准备缓存迁移"
            AlertDialog(
                onDismissRequest = { hiddenMigrationWorkId = migrationWork?.id },
                title = { Text("正在迁移缓存") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stage)
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "$progress% · 迁移完成前请勿移除原目录授权",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { hiddenMigrationWorkId = migrationWork?.id }) {
                        Text("后台运行")
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingSelectDialogContent(
    settingType: SettingType,
    localSetting: LocalSetting,
    localSettingManager: LocalSettingManager,
    onDismiss: () -> Unit
) {
    var pendingUnsupportedSource by remember { mutableStateOf<String?>(null) }
    // 网格列数使用滑块设置，不走选项列表
    if (settingType is SettingType.AllGridColumns) {
        AllGridColumnSliderDialog(
            homeColumns = localSetting.homeGridColumns,
            collectColumns = localSetting.collectGridColumns,
            downloadColumns = localSetting.downloadGridColumns,
            historyColumns = localSetting.historyGridColumns,
            searchColumns = localSetting.searchGridColumns,
            onConfirm = { home, collect, download, history, search ->
                localSettingManager.updateHomeGridColumns(home)
                localSettingManager.updateCollectGridColumns(collect)
                localSettingManager.updateDownloadGridColumns(download)
                localSettingManager.updateHistoryGridColumns(history)
                localSettingManager.updateSearchGridColumns(search)
                onDismiss()
            },
            onDismiss = onDismiss
        )
        return
    }
    val apiSelectOptionList by remember(localSetting.apiList) {
        derivedStateOf { localSetting.apiList.map { SelectOption(it.removePrefix("https://"), it) } }
    }
    val recommendSourceOptionList = remember {
        listOf(
            SelectOption("\u5185\u7f6e API \u63a8\u8350", "builtin"),
            SelectOption("\u7f51\u7edc API \u63a8\u8350", "network")
        )
    }
    val comicApiSourceOptionList by remember(localSetting.comicApiSourceList) {
        derivedStateOf {
            localSetting.comicApiSourceList.map {
                SelectOption(comicApiSourceTextMap[it].orEmpty(), it)
            }
        }
    }
    val themeSelectOptionList by remember(localSetting.themeList) {
        derivedStateOf { localSetting.themeList.map { SelectOption(themeTextMap[it].orEmpty(), it) } }
    }
    val launcherDisguiseOptionList by remember {
        derivedStateOf { LauncherDisguise.entries.map { SelectOption(it.label, it.id) } }
    }
    val shuntOptionList by remember(localSetting.shuntList) {
        derivedStateOf { localSetting.shuntList.map { SelectOption("\u7ebf\u8def $it", it) } }
    }
    val prefetchCountOptionList by remember {
        derivedStateOf {
            listOf(
                SelectOption("\u5173\u95ed", "0"),
                SelectOption("\u4e00\u5f20", "1"),
                SelectOption("\u4e24\u5f20", "2"),
                SelectOption("\u4e09\u5f20", "3"),
                SelectOption("\u56db\u5f20", "4"),
                SelectOption("\u4e94\u5f20", "5"),
                SelectOption("\u516d\u5f20", "6")
            )
        }
    }
    val readModeOptionList by remember {
        derivedStateOf {
            listOf(
                SelectOption("\u6eda\u52a8", "scroll"),
                SelectOption("\u7ffb\u9875", "page"),
                SelectOption("\u70b9\u51fb", "tap")
            )
        }
    }
    val readTapModeOptionList by remember {
        derivedStateOf {
            listOf(
                SelectOption("\u9ed8\u8ba4\u533a\u57df", "default"),
                SelectOption("\u5de6\u53f3\u4e24\u4fa7", "side")
            )
        }
    }
    val notificationOptionList by remember {
        derivedStateOf {
            listOf(
                SelectOption("\u5f00\u542f\u5e76\u663e\u793a\u6f2b\u753b\u540d", NOTIFICATION_ON_WITH_NAME),
                SelectOption("\u5f00\u542f\u4f46\u4e0d\u663e\u793a\u6f2b\u753b\u540d", NOTIFICATION_ON_WITHOUT_NAME),
                SelectOption("\u5173\u95ed", NOTIFICATION_OFF)
            )
        }
    }
    val readDecodeConcurrencyOptionList by remember {
        derivedStateOf {
            listOf(
                SelectOption("1", "1"),
                SelectOption("2\uff08\u63a8\u8350\uff09", "2"),
                SelectOption("3", "3"),
                SelectOption("4", "4")
            )
        }
    }
    val cacheIntegrityCheckOptionList by remember {
        derivedStateOf {
            listOf(
                SelectOption("关闭（默认）", CACHE_INTEGRITY_CHECK_OFF),
                SelectOption("部分检查（配置、封面）", CACHE_INTEGRITY_CHECK_PARTIAL),
                SelectOption("完全检查（配置、封面、全部图片）", CACHE_INTEGRITY_CHECK_FULL),
            )
        }
    }
    SelectDialog(
        title = settingTitle(settingType),
        value = settingValue(settingType, localSetting),
        selectOptionList = when (settingType) {
            is SettingType.ComicApiSource -> comicApiSourceOptionList
            is SettingType.Api -> apiSelectOptionList
            is SettingType.Theme -> themeSelectOptionList
            is SettingType.LauncherDisguise -> launcherDisguiseOptionList
            is SettingType.Shunt -> shuntOptionList
            is SettingType.PrefetchCount -> prefetchCountOptionList
            is SettingType.ReadMode -> readModeOptionList
            is SettingType.ReadTapMode -> readTapModeOptionList
            is SettingType.NotificationManagement -> notificationOptionList
            is SettingType.RecommendSource -> recommendSourceOptionList
            is SettingType.ReadDecodeConcurrency -> readDecodeConcurrencyOptionList
            is SettingType.CacheIntegrityCheck -> cacheIntegrityCheckOptionList
        },
        onSelect = {
            var shouldDismiss = true
            when (settingType) {
                is SettingType.ComicApiSource -> {
                    if (it == COMIC_API_SOURCE_NETWORK || it == COMIC_API_SOURCE_MIXED) {
                        pendingUnsupportedSource = it
                        shouldDismiss = false
                    } else {
                        localSettingManager.updateComicApiSource(it)
                    }
                }
                is SettingType.Api -> localSettingManager.updateApi(it)
                is SettingType.Theme -> localSettingManager.updateTheme(it)
                is SettingType.LauncherDisguise -> localSettingManager.updateLauncherDisguise(it)
                is SettingType.Shunt -> localSettingManager.updateShunt(it)
                is SettingType.PrefetchCount -> localSettingManager.updatePrefetchCount(it)
                is SettingType.ReadMode -> localSettingManager.updateReadMode(it)
                is SettingType.ReadTapMode -> localSettingManager.updateReadTapMode(it)
                is SettingType.NotificationManagement -> {
                    localSettingManager.updateNotificationSettings(
                        show = it != NOTIFICATION_OFF,
                        showName = it == NOTIFICATION_ON_WITH_NAME
                    )
                }
                is SettingType.RecommendSource -> localSettingManager.updateRecommendSource(it)
                is SettingType.ReadDecodeConcurrency -> localSettingManager.updateReadDecodeConcurrency(it.toIntOrNull() ?: 2)
                is SettingType.CacheIntegrityCheck -> localSettingManager.updateCacheIntegrityCheckMode(it)
            }
            if (shouldDismiss) onDismiss()
        },
        onDismissRequest = onDismiss
    )
    pendingUnsupportedSource?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingUnsupportedSource = null },
            title = { Text("\u7f51\u7edc API \u8b66\u544a") },
            text = {
                Text(
                    "\u7f51\u7edc API \u76ee\u524d\u5df2\u9000\u51fa\u652f\u6301\uff0c\u53ef\u80fd\u5bfc\u81f4\u8f6f\u4ef6\u4e0d\u7a33\u5b9a\u3001\u529f\u80fd\u65e0\u6cd5\u4f7f\u7528\u3002",
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    localSettingManager.updateComicApiSource(source)
                    pendingUnsupportedSource = null
                    onDismiss()
                }) { Text("\u786e\u5b9a") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnsupportedSource = null }) { Text("\u53d6\u6d88") }
            }
        )
    }
}

@Composable
private fun AllGridColumnSliderDialog(
    homeColumns: Int,
    collectColumns: Int,
    downloadColumns: Int,
    historyColumns: Int,
    searchColumns: Int,
    onConfirm: (Int, Int, Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var home by remember { mutableStateOf(homeColumns.toFloat()) }
    var collect by remember { mutableStateOf(collectColumns.toFloat()) }
    var download by remember { mutableStateOf(downloadColumns.toFloat()) }
    var history by remember { mutableStateOf(historyColumns.toFloat()) }
    var search by remember { mutableStateOf(searchColumns.toFloat()) }

    @Composable
    fun SliderRow(
        icon: ImageVector,
        label: String,
        value: Float,
        onChange: (Float) -> Unit,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = if (value <= 0f) "自适应" else "${value.toInt()} 列",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = 0f..6f,
                steps = 5,
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("网格列数") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "拖动滑块设置各页面每行显示的漫画数量，0 = 自适应",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SliderRow(Icons.Rounded.Home, "首页", home) { home = it }
                SliderRow(Icons.Rounded.Bookmarks, "收藏夹", collect) { collect = it }
                SliderRow(Icons.Rounded.Download, "缓存", download) { download = it }
                SliderRow(Icons.Rounded.History, "历史记录", history) { history = it }
                SliderRow(Icons.Rounded.Search, "搜索", search) { search = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(home.toInt(), collect.toInt(), download.toInt(), history.toInt(), search.toInt()) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeExcludedTagsDialog(
    tags: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var currentTags by remember { mutableStateOf(tags) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("首页标签排除") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "添加标签后，首页推荐将不再显示包含这些标签的漫画",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("输入标签名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val trimmed = text.trim()
                                if (trimmed.isNotEmpty() && trimmed !in currentTags) {
                                    currentTags = currentTags + trimmed
                                    text = ""
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "添加")
                        }
                    }
                )
                if (currentTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentTags.forEach { tag ->
                            InputChip(
                                label = { Text(tag) },
                                selected = false,
                                onClick = {},
                                trailingIcon = {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "删除",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                currentTags = currentTags - tag
                                            }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentTags) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    SettingsBaseRow(
        icon = icon,
        title = title,
        value = value,
        onClick = onClick,
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    value: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsBaseRow(
        icon = icon,
        title = title,
        value = if (value) "\u5f00\u542f" else "\u5173\u95ed",
        onClick = { onCheckedChange(!value) },
        trailingContent = {
            Switch(
                checked = value,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun SettingsBaseRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .drawBehind {
                val dividerY = size.height - 1.dp.toPx()
                drawLine(
                    color = dividerColor,
                    start = Offset(72.dp.toPx(), dividerY),
                    end = Offset(size.width, dividerY),
                    strokeWidth = 1.dp.toPx()
                )
            },
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = icon,
                        contentDescription = null
                    )
                }
            }
        },
        headlineContent = { Text(text = title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )
}

private fun prefetchText(value: Int): String {
    return when (value) {
        0 -> "\u5173\u95ed"
        1 -> "\u4e00\u5f20"
        2 -> "\u4e24\u5f20"
        3 -> "\u4e09\u5f20"
        else -> "$value \u5f20"
    }
}

private fun readModeText(value: String): String {
    return when (value) {
        "scroll" -> "\u6eda\u52a8"
        "page" -> "\u7ffb\u9875"
        "tap" -> "\u70b9\u51fb"
        else -> "\u6eda\u52a8"
    }
}

private fun cacheIntegrityCheckText(value: String): String = when (value) {
    CACHE_INTEGRITY_CHECK_FULL -> "完全检查"
    CACHE_INTEGRITY_CHECK_PARTIAL -> "部分检查"
    else -> "关闭"
}

private fun notificationText(localSetting: LocalSetting): String {
    return when {
        !localSetting.showComicCacheNotification -> "\u5173\u95ed"
        localSetting.showComicCacheNotificationName -> "\u5f00\u542f\u5e76\u663e\u793a\u6f2b\u753b\u540d"
        else -> "\u5f00\u542f\u4f46\u4e0d\u663e\u793a\u6f2b\u753b\u540d"
    }
}

private fun settingTitle(type: SettingType): String {
    return when (type) {
        is SettingType.ComicApiSource -> "\u6570\u636e\u6e90"
        is SettingType.Api -> "API"
        is SettingType.Theme -> "\u4e3b\u9898"
        is SettingType.LauncherDisguise -> "\u56fe\u6807\u4f2a\u88c5"
        is SettingType.Shunt -> "\u56fe\u7247\u7ebf\u8def"
        is SettingType.PrefetchCount -> "\u56fe\u7247\u9884\u52a0\u8f7d"
        is SettingType.ReadMode -> "\u9605\u8bfb\u6a21\u5f0f"
        is SettingType.ReadTapMode -> "\u70b9\u51fb\u7ffb\u56fe"
        is SettingType.NotificationManagement -> "\u901a\u77e5\u7ba1\u7406"
        is SettingType.RecommendSource -> "\u63a8\u8350\u6e90"
        is SettingType.AllGridColumns -> "\u7f51\u683c\u5217\u6570"
        is SettingType.ReadDecodeConcurrency -> "\u5e76\u53d1\u89e3\u7801\u6570"
        is SettingType.CacheIntegrityCheck -> "缓存检查"
    }
}

private fun settingValue(type: SettingType, localSetting: LocalSetting): String {
    return when (type) {
        is SettingType.ComicApiSource -> localSetting.comicApiSource
        is SettingType.Api -> localSetting.api
        is SettingType.Theme -> localSetting.theme
        is SettingType.LauncherDisguise -> LauncherDisguise.fromId(localSetting.launcherDisguise).id
        is SettingType.Shunt -> localSetting.shunt
        is SettingType.PrefetchCount -> "${localSetting.prefetchCount}"
        is SettingType.ReadMode -> localSetting.readMode
        is SettingType.ReadTapMode -> localSetting.readTapMode
        is SettingType.NotificationManagement -> when {
            !localSetting.showComicCacheNotification -> NOTIFICATION_OFF
            localSetting.showComicCacheNotificationName -> NOTIFICATION_ON_WITH_NAME
            else -> NOTIFICATION_ON_WITHOUT_NAME
        }
        is SettingType.RecommendSource -> localSetting.recommendSource
        is SettingType.AllGridColumns -> ""
        is SettingType.ReadDecodeConcurrency -> "${localSetting.readDecodeConcurrency}"
        is SettingType.CacheIntegrityCheck -> localSetting.cacheIntegrityCheckMode
    }
}
