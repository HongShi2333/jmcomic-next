package com.par9uet.jm.ui.screens

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PASSWORD
import com.par9uet.jm.data.models.APP_LOCK_TYPE_PATTERN
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.network.ComicCoverUrlResolver
import com.par9uet.jm.cache.ensureDownloadTreeHiddenFromGallery
import com.par9uet.jm.cache.setDownloadTreeUri
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.store.BACKUP_PROTECTION_BOTH
import com.par9uet.jm.store.BACKUP_PROTECTION_PASSWORD
import com.par9uet.jm.store.BACKUP_PROTECTION_PATTERN
import com.par9uet.jm.store.BackupContentOptions
import com.par9uet.jm.store.BackupFile
import com.par9uet.jm.store.BackupManager
import com.par9uet.jm.store.ComicCacheBackup
import com.par9uet.jm.store.ComicGroupBackup
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.storage.AiChatStorage
import com.par9uet.jm.storage.PersonaStorage
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.FallbackAsyncImage
import com.par9uet.jm.ui.components.SelectDialog
import com.par9uet.jm.ui.components.SelectOption
import com.par9uet.jm.ui.components.adaptiveDialogMaxHeight
import com.par9uet.jm.utils.authenticateWithBiometrics
import com.par9uet.jm.utils.canUseBiometricAuth
import com.par9uet.jm.utils.createBackupBiometricBinding
import com.par9uet.jm.utils.findFragmentActivity
import com.par9uet.jm.utils.verifyBackupBiometricBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class BackupStep {
    None, SelectContent, SelectProtection, SetPassword, SetPattern, OfferBiometric
}

private enum class RestoreStep {
    None, OfferBiometric, VerifyPassword, VerifyPattern, SelectContent, SelectComicCache
}

private val protectionOptionList = listOf(
    SelectOption("仅密码", BACKUP_PROTECTION_PASSWORD),
    SelectOption("仅图案", BACKUP_PROTECTION_PATTERN),
    SelectOption("密码 + 图案", BACKUP_PROTECTION_BOTH),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    localSettingManager: LocalSettingManager = getKoin().get(),
    aiChatStorage: AiChatStorage = getKoin().get(),
    personaStorage: PersonaStorage = getKoin().get(),
    downloadComicDao: DownloadComicDao = getKoin().get(),
    downloadManager: DownloadManager = getKoin().get(),
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    toastManager: ToastManager = getKoin().get(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricAvailable = remember(context) { canUseBiometricAuth(context) }
    val scope = rememberCoroutineScope()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
    val backupManager = remember { BackupManager() }

    // 备份流程状态
    var backupStep by remember { mutableStateOf(BackupStep.None) }
    var contentOptions by remember { mutableStateOf(BackupContentOptions()) }
    var pendingProtectionType by remember { mutableStateOf(BACKUP_PROTECTION_PASSWORD) }
    var pendingPassword by remember { mutableStateOf<String?>(null) }
    var pendingPattern by remember { mutableStateOf<String?>(null) }
    var pendingCreateDocument by remember { mutableStateOf(false) }
    var pendingBiometricBinding by remember { mutableStateOf<String?>(null) }
    // 缓存备份在内存中暂存，等待写入文件时一起打包
    var pendingComicCacheBackup by remember { mutableStateOf<ComicCacheBackup?>(null) }

    // 恢复流程状态
    var restoreBackup by remember { mutableStateOf<BackupFile?>(null) }
    var restoreStep by remember { mutableStateOf(RestoreStep.None) }
    // 恢复时用户选择的内容选项
    var restoreContentOptions by remember { mutableStateOf(BackupContentOptions()) }

    fun resetBackupState() {
        backupStep = BackupStep.None
        contentOptions = BackupContentOptions()
        pendingProtectionType = BACKUP_PROTECTION_PASSWORD
        pendingPassword = null
        pendingPattern = null
        pendingBiometricBinding = null
        pendingComicCacheBackup = null
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) {
            toastManager.showAsync("未选择保存位置")
            resetBackupState()
            return@rememberLauncherForActivityResult
        }
        val pwd = pendingPassword
        val pat = pendingPattern
        val prot = pendingProtectionType
        val opts = contentOptions
        val cacheBackup = pendingComicCacheBackup
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = backupManager.createBackup(
                        localSetting = if (opts.includeLocalSetting || opts.includeDownloadPath) localSetting else null,
                        aiChats = if (opts.includeAiChats) aiChatStorage.get() else null,
                        personas = if (opts.includePersonas) personaStorage.get() else null,
                        comicCache = if (opts.includeComicCache) cacheBackup else null,
                        options = opts,
                        protectionType = prot,
                        password = pwd,
                        pattern = pat,
                        biometricBinding = pendingBiometricBinding,
                    )
                    if (!backupManager.writeToUri(context, uri, json)) {
                        error("写入备份文件失败")
                    }
                }
            }.onSuccess {
                toastManager.showAsync("备份成功")
            }.onFailure {
                // 系统保存器会先创建空文件；写入失败时删除它，避免留下无法恢复的 0KB 备份。
                withContext(Dispatchers.IO) {
                    runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                }
                toastManager.showAsync("备份失败：${it.message ?: "未知错误"}")
            }
            resetBackupState()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            toastManager.showAsync("未选择备份文件")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = backupManager.readFromUri(context, uri)
                        ?: error("无法读取备份文件")
                    backupManager.parseBackup(json).getOrThrow()
                }
            }.onSuccess { backup ->
                restoreBackup = backup
                restoreStep = when {
                    biometricAvailable && activity != null && verifyBackupBiometricBinding(context, backup.meta.biometricBinding) ->
                        RestoreStep.OfferBiometric
                    backupManager.needsPassword(backup) -> RestoreStep.VerifyPassword
                    backupManager.needsPattern(backup) -> RestoreStep.VerifyPattern
                    else -> RestoreStep.SelectContent
                }
            }.onFailure {
                toastManager.showAsync("恢复失败：${it.message ?: "未知错误"}")
            }
        }
    }

    fun onPasswordVerified() {
        val backup = restoreBackup ?: return
        restoreStep = if (backupManager.needsPattern(backup)) {
            RestoreStep.VerifyPattern
        } else {
            RestoreStep.SelectContent
        }
    }

    fun onPatternVerified() {
        restoreStep = RestoreStep.SelectContent
    }

    fun proceedAfterBaseProtection() {
        backupStep = if (biometricAvailable && activity != null) {
            BackupStep.OfferBiometric
        } else {
            pendingCreateDocument = true
            BackupStep.None
        }
    }

    fun useRestoreFallback() {
        val backup = restoreBackup ?: return
        restoreStep = when {
            backupManager.needsPassword(backup) -> RestoreStep.VerifyPassword
            backupManager.needsPattern(backup) -> RestoreStep.VerifyPattern
            else -> RestoreStep.SelectContent
        }
    }

    fun applyRestore(backup: BackupFile, options: BackupContentOptions) {
        runCatching {
            val applied = mutableListOf<String>()
            if (options.includeLocalSetting || options.includeDownloadPath) {
                val setting = backupManager.extractLocalSetting(backup)
                if (setting != null) {
                    if (options.includeLocalSetting) {
                        localSettingManager.applyLocalSetting(setting, options.includeDownloadPath)
                    } else if (options.includeDownloadPath) {
                        localSettingManager.updateDownloadTreeUri(setting.downloadTreeUri)
                    }
                    if (options.includeDownloadPath) {
                        setDownloadTreeUri(context, setting.downloadTreeUri)
                        ensureDownloadTreeHiddenFromGallery(context, setting.downloadTreeUri)
                        applied += "\u7f13\u5b58\u8def\u5f84"
                    }
                    applied += "本地设置"
                }
            }
            if (options.includeAiChats) {
                val chats = backupManager.extractAiChats(backup)
                if (chats.isNotEmpty()) {
                    aiChatStorage.set(chats)
                    applied += "AI 聊天记录"
                }
            }
            if (options.includePersonas) {
                val personas = backupManager.extractPersonas(backup)
                if (personas.isNotEmpty()) {
                    personaStorage.set(personas)
                    applied += "人格面具"
                }
            }
            // 缓存目录在 applyComicCacheRestore 中单独处理，这里不处理
            if (applied.isEmpty()) "未找到可恢复的内容" else "已恢复：${applied.joinToString("、")}"
        }.onSuccess { msg ->
            toastManager.showAsync(msg)
        }.onFailure {
            toastManager.showAsync("恢复失败：${it.message ?: "未知错误"}")
        }
        restoreBackup = null
        restoreStep = RestoreStep.None
    }

    /**
     * 恢复缓存目录：将用户选中的漫画组重新加入下载队列。
     */
    fun applyComicCacheRestore(selectedGroups: List<ComicGroupBackup>) {
        if (selectedGroups.isEmpty()) {
            toastManager.showAsync("未选择需要恢复缓存的漫画")
            restoreBackup = null
            restoreStep = RestoreStep.None
            return
        }
        scope.launch {
            var totalChapters = 0
            selectedGroups.forEach { group ->
                val parentComic = Comic.create(
                    id = group.id,
                    name = group.name,
                    authorList = group.authors,
                )
                val chapters = group.chapters.sortedBy { it.sortOrder }
                    .map { ChapterBackup_to_ComicChapter(it) }
                if (chapters.size == 1 && chapters.first().name.isBlank()) {
                    // 单篇漫画：走 downloadComic
                    downloadManager.downloadComic(parentComic)
                } else {
                    downloadManager.downloadChapters(parentComic, chapters)
                }
                totalChapters += chapters.size
            }
            toastManager.showAsync("已创建 ${selectedGroups.size} 部漫画的缓存任务（共 $totalChapters 章）")
            restoreBackup = null
            restoreStep = RestoreStep.None
        }
    }

    fun cancelRestore() {
        restoreBackup = null
        restoreStep = RestoreStep.None
        toastManager.showAsync("已取消恢复")
    }

    LaunchedEffect(pendingCreateDocument) {
        if (pendingCreateDocument) {
            pendingCreateDocument = false
            createDocumentLauncher.launch(generateBackupFileName())
        }
    }

    CommonScaffold(title = "数据备份与恢复") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { InfoCard() }
            item {
                ActionCard(
                    icon = Icons.Rounded.CloudUpload,
                    title = "备份数据",
                    description = "选择备份内容并强制设置密码或图形；可额外启用本机指纹/面容快捷验证",
                    onClick = {
                        resetBackupState()
                        backupStep = BackupStep.SelectContent
                    }
                )
            }
            item {
                ActionCard(
                    icon = Icons.Rounded.CloudDownload,
                    title = "恢复数据",
                    description = "从备份文件恢复，可选择需要恢复的内容（不会覆盖当前设备的应用锁状态）",
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json"))
                    }
                )
            }
        }

        // ============== 备份流程 ==============

        // 步骤 1：选择备份内容
        if (backupStep == BackupStep.SelectContent) {
            BackupContentPickerDialog(
                options = contentOptions,
                onChange = { contentOptions = it },
                onConfirm = {
                    if (contentOptions.isEmpty) {
                        toastManager.showAsync("请至少选择一项备份内容")
                    } else {
                        // 如果选中了缓存目录，先异步读取 DAO 数据
                        if (contentOptions.includeComicCache) {
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        val all = downloadComicDao.getAll()
                                        backupManager.buildComicCacheBackup(all)
                                    }
                                }.onSuccess { cache ->
                                    if (cache.groups.isEmpty()) {
                                        toastManager.showAsync("当前没有缓存记录，已自动取消勾选缓存目录")
                                        contentOptions = contentOptions.copy(includeComicCache = false)
                                    } else {
                                        pendingComicCacheBackup = cache
                                    }
                                }.onFailure {
                                    toastManager.showAsync("读取缓存列表失败：${it.message ?: "未知错误"}")
                                    contentOptions = contentOptions.copy(includeComicCache = false)
                                }
                                backupStep = BackupStep.SelectProtection
                            }
                        } else {
                            backupStep = BackupStep.SelectProtection
                        }
                    }
                },
                onDismiss = { resetBackupState() }
            )
        }

        // 步骤 2：选择保护方式
        if (backupStep == BackupStep.SelectProtection) {
            SelectDialog(
                title = "选择保护方式",
                value = null,
                selectOptionList = protectionOptionList,
                onSelect = { type ->
                    pendingProtectionType = type
                    when (type) {
                        BACKUP_PROTECTION_PASSWORD -> {
                            backupStep = BackupStep.SetPassword
                        }
                        BACKUP_PROTECTION_PATTERN -> {
                            backupStep = BackupStep.SetPattern
                        }
                        BACKUP_PROTECTION_BOTH -> {
                            backupStep = BackupStep.SetPassword
                        }
                    }
                },
                onDismissRequest = { resetBackupState() }
            )
        }

        // 步骤 3a：设置密码
        if (backupStep == BackupStep.SetPassword) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PASSWORD,
                passwordLength = 4,
                onConfirm = { pwd ->
                    pendingPassword = pwd
                    if (pendingProtectionType == BACKUP_PROTECTION_BOTH) {
                        backupStep = BackupStep.SetPattern
                    } else {
                        proceedAfterBaseProtection()
                    }
                },
                onDismiss = { resetBackupState() }
            )
        }

        // 步骤 3b：设置图案
        if (backupStep == BackupStep.SetPattern) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PATTERN,
                onConfirm = { pattern ->
                    pendingPattern = pattern
                    proceedAfterBaseProtection()
                },
                onDismiss = { resetBackupState() }
            )
        }

        if (backupStep == BackupStep.OfferBiometric) {
            AlertDialog(
                icon = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
                title = { Text("启用指纹或面容快捷验证？") },
                text = { Text("密码或图形仍会强制保留，用于没有生物识别的手机或跨设备恢复。生物识别仅在当前设备安装中可用。") },
                confirmButton = {
                    TextButton(onClick = {
                        authenticateWithBiometrics(
                            activity = activity!!,
                            title = "确认启用生物识别",
                            subtitle = "以后可用指纹或面容快捷验证此备份",
                            onSuccess = {
                                pendingBiometricBinding = createBackupBiometricBinding(context)
                                pendingCreateDocument = true
                                backupStep = BackupStep.None
                            },
                            onError = toastManager::showAsync,
                        )
                    }) { Text("启用") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingCreateDocument = true
                        backupStep = BackupStep.None
                    }) { Text("跳过") }
                },
                onDismissRequest = { resetBackupState() },
            )
        }

        // ============== 恢复流程 ==============

        if (restoreStep == RestoreStep.OfferBiometric) {
            AlertDialog(
                icon = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
                title = { Text("使用指纹或面容验证") },
                text = { Text("也可以改用创建备份时设置的密码或图形。") },
                confirmButton = {
                    TextButton(onClick = {
                        authenticateWithBiometrics(
                            activity = activity!!,
                            title = "验证备份",
                            subtitle = "使用指纹或面容继续恢复",
                            onSuccess = { restoreStep = RestoreStep.SelectContent },
                            onError = toastManager::showAsync,
                        )
                    }) { Text("验证") }
                },
                dismissButton = {
                    TextButton(onClick = ::useRestoreFallback) { Text("使用密码或图形") }
                },
                onDismissRequest = { cancelRestore() },
            )
        }

        // 核验密码
        if (restoreStep == RestoreStep.VerifyPassword) {
            val backup = restoreBackup
            if (backup != null) {
                VerifyPasswordDialog(
                    passwordLength = 4,
                    onVerify = { pwd ->
                        if (backupManager.verifyPassword(backup, pwd)) {
                            onPasswordVerified()
                            true
                        } else {
                            false
                        }
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }

        // 核验图案
        if (restoreStep == RestoreStep.VerifyPattern) {
            val backup = restoreBackup
            if (backup != null) {
                VerifyPatternDialog(
                    onVerify = { pattern ->
                        if (backupManager.verifyPattern(backup, pattern)) {
                            onPatternVerified()
                            true
                        } else {
                            false
                        }
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }

        // 选择恢复内容
        if (restoreStep == RestoreStep.SelectContent) {
            val backup = restoreBackup
            if (backup != null) {
                RestoreContentPickerDialog(
                    backup = backup,
                    onConfirm = { options ->
                        restoreContentOptions = options
                        // 如果包含缓存目录，先进入漫画选择步骤
                        if (options.includeComicCache && backup.meta.includeComicCache) {
                            restoreStep = RestoreStep.SelectComicCache
                        } else {
                            applyRestore(backup, options)
                        }
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }

        // 选择要恢复缓存的漫画
        if (restoreStep == RestoreStep.SelectComicCache) {
            val backup = restoreBackup
            if (backup != null) {
                val cache = backupManager.extractComicCache(backup)
                ComicCacheRestoreDialog(
                    groups = cache.groups,
                    imgHost = remoteSetting.imgHost,
                    onConfirm = { selected ->
                        // 先应用其他选项（localSetting/aiChats/personas），再恢复缓存
                        val opts = restoreContentOptions
                        if (opts.includeLocalSetting || opts.includeDownloadPath || opts.includeAiChats || opts.includePersonas) {
                            applyRestore(backup, opts.copy(includeComicCache = false))
                        } else {
                            restoreBackup = null
                            restoreStep = RestoreStep.None
                        }
                        applyComicCacheRestore(selected)
                    },
                    onSkip = {
                        // 用户选择不恢复缓存，只恢复其他内容
                        applyRestore(backup, restoreContentOptions.copy(includeComicCache = false))
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupContentPickerDialog(
    options: BackupContentOptions,
    onChange: (BackupContentOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 440.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "选择备份内容",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ContentToggleRow(
                    icon = Icons.Rounded.Info,
                    title = "本地设置",
                    subtitle = "标签排除、配色方案、推荐方式、网格列数、阅读设置等",
                    checked = options.includeLocalSetting,
                    onCheckedChange = { onChange(options.copy(includeLocalSetting = it)) }
                )
                ContentToggleRow(
                    icon = Icons.Rounded.Folder,
                    title = "缓存路径",
                    subtitle = "单独备份默认或自定义缓存目录设置",
                    checked = options.includeDownloadPath,
                    onCheckedChange = { onChange(options.copy(includeDownloadPath = it)) }
                )
                ContentToggleRow(
                    icon = Icons.Rounded.Chat,
                    title = "AI 聊天记录",
                    subtitle = "全部 AI 对话历史",
                    checked = options.includeAiChats,
                    onCheckedChange = { onChange(options.copy(includeAiChats = it)) }
                )
                ContentToggleRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "人格面具",
                    subtitle = "全部自定义人格面具",
                    checked = options.includePersonas,
                    onCheckedChange = { onChange(options.copy(includePersonas = it)) }
                )
                ContentToggleRow(
                    icon = Icons.Rounded.Book,
                    title = "缓存目录",
                    subtitle = "只备份漫画编号与章节信息，不备份图片文件",
                    checked = options.includeComicCache,
                    onCheckedChange = { onChange(options.copy(includeComicCache = it)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(onClick = onConfirm) { Text("下一步", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreContentPickerDialog(
    backup: BackupFile,
    onConfirm: (BackupContentOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var localSettingOn by remember { mutableStateOf(backup.meta.includeLocalSetting) }
    var downloadPathOn by remember { mutableStateOf(backup.meta.includeDownloadPath) }
    var aiChatsOn by remember { mutableStateOf(backup.meta.includeAiChats) }
    var personasOn by remember { mutableStateOf(backup.meta.includePersonas) }
    var comicCacheOn by remember { mutableStateOf(backup.meta.includeComicCache) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 440.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "选择恢复内容",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (backup.meta.includeLocalSetting) {
                    ContentToggleRow(
                        icon = Icons.Rounded.Info,
                        title = "本地设置",
                        subtitle = "会覆盖当前本地设置",
                        checked = localSettingOn,
                        onCheckedChange = { localSettingOn = it }
                    )
                }
                if (backup.meta.includeDownloadPath) {
                    ContentToggleRow(
                        icon = Icons.Rounded.Folder,
                        title = "缓存路径",
                        subtitle = "恢复缓存目录设置",
                        checked = downloadPathOn,
                        onCheckedChange = { downloadPathOn = it }
                    )
                }
                if (backup.meta.includeAiChats) {
                    ContentToggleRow(
                        icon = Icons.Rounded.Chat,
                        title = "AI 聊天记录",
                        subtitle = "会覆盖当前全部 AI 对话",
                        checked = aiChatsOn,
                        onCheckedChange = { aiChatsOn = it }
                    )
                }
                if (backup.meta.includePersonas) {
                    ContentToggleRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "人格面具",
                        subtitle = "会覆盖当前全部人格面具",
                        checked = personasOn,
                        onCheckedChange = { personasOn = it }
                    )
                }
                if (backup.meta.includeComicCache) {
                    ContentToggleRow(
                        icon = Icons.Rounded.Book,
                        title = "缓存目录",
                        subtitle = "共 ${backup.meta.comicCacheCount} 部漫画，恢复时可选择具体内容",
                        checked = comicCacheOn,
                        onCheckedChange = { comicCacheOn = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(onClick = {
                        onConfirm(
                            BackupContentOptions(
                                includeLocalSetting = localSettingOn,
                                includeDownloadPath = downloadPathOn,
                                includeAiChats = aiChatsOn,
                                includePersonas = personasOn,
                                includeComicCache = comicCacheOn,
                            )
                        )
                    }) { Text("下一步", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ContentToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun generateBackupFileName(): String {
    val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINESE)
    return "jm-mobile-backup-${sdf.format(Date())}.json"
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = "备份内容说明",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "备份时可选择：本地设置、AI 聊天记录、人格面具、缓存目录。\n" +
                        "缓存目录只备份漫画编号与章节信息，不备份图片文件；恢复时可选择具体要重新缓存的漫画。\n" +
                        "每份新备份必须设置密码或图形作为跨设备恢复保底；指纹/面容仅是当前设备的可选快捷验证。备份不会保存应用锁凭据，恢复也不会覆盖应用锁状态。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifyPasswordDialog(
    passwordLength: Int,
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "请输入备份密码",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                PasswordLockInput(
                    title = "",
                    correctPassword = null,
                    onUnlock = {},
                    passwordLength = passwordLength,
                    onInputComplete = { pwd ->
                        if (!onVerify(pwd)) {
                            errorMessage = "密码错误，请重试"
                        } else {
                            errorMessage = null
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifyPatternDialog(
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "请绘制备份图案",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                PatternLockInput(
                    title = "",
                    correctPassword = null,
                    onUnlock = {},
                    onInputComplete = { pattern ->
                        if (!onVerify(pattern)) {
                            errorMessage = "图案错误，请重试"
                        } else {
                            errorMessage = null
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}

/**
 * 将备份中的章节信息转换回 ComicChapter，用于恢复下载任务。
 */
private fun ChapterBackup_to_ComicChapter(chapter: com.par9uet.jm.store.ChapterBackup): ComicChapter {
    return ComicChapter(
        id = chapter.id,
        name = chapter.name,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicCacheRestoreDialog(
    groups: List<ComicGroupBackup>,
    imgHost: String,
    onConfirm: (List<ComicGroupBackup>) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 默认全部勾选
    val selectedIds = remember { mutableStateOf(groups.map { it.id }.toSet()) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 520.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 标题
                Text(
                    text = "恢复缓存目录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "共 ${groups.size} 部漫画。勾选需要重新缓存的漫画，未勾选的不会恢复。恢复时会按编号重新创建缓存任务。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 全选/取消全选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选 ${selectedIds.value.size} / ${groups.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        val allIds = groups.map { it.id }.toSet()
                        selectedIds.value = if (selectedIds.value.size == allIds.size) emptySet() else allIds
                    }) {
                        Text(if (selectedIds.value.size == groups.size) "取消全选" else "全选")
                    }
                }

                // 漫画列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = adaptiveDialogMaxHeight(360.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        val checked = group.id in selectedIds.value
                        ComicRestoreRow(
                            group = group,
                            checked = checked,
                            imgHost = imgHost,
                            onToggle = {
                                selectedIds.value = if (checked) {
                                    selectedIds.value - group.id
                                } else {
                                    selectedIds.value + group.id
                                }
                            }
                        )
                    }
                }

                // 底部操作栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.size(4.dp))
                    TextButton(onClick = onSkip) { Text("跳过缓存恢复") }
                    Spacer(modifier = Modifier.size(4.dp))
                    TextButton(onClick = {
                        val selected = groups.filter { it.id in selectedIds.value }
                        onConfirm(selected)
                    }) { Text("恢复", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ComicRestoreRow(
    group: ComicGroupBackup,
    checked: Boolean,
    imgHost: String,
    onToggle: () -> Unit,
) {
    val coverUrls = ComicCoverUrlResolver.resolve(
        comicId = group.id,
        apiImage = "",
        configuredImageHost = imgHost,
    )
    val imageLoader: coil.ImageLoader = getKoin().get()
    val containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左侧缩略图
            Box(
                modifier = Modifier
                    .size(52.dp, 70.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrls.isNotEmpty()) {
                    FallbackAsyncImage(
                        coverUrls = coverUrls,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Book,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            // 右侧信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name.ifBlank { "未命名漫画" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "共 ${group.chapterCount} 章" +
                        if (group.authors.isNotEmpty()) " · ${group.authors.joinToString("、")}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 右侧勾选图标
            Icon(
                imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
