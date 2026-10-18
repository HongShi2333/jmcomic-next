package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.heightIn
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import com.par9uet.jm.ui.components.watchVerticalScroll as verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Minimize
import com.par9uet.jm.ui.components.WatchBasicAlertDialog as BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.store.AppUpdateDownloadManager
import com.par9uet.jm.store.AppUpdateDownloadRequest
import com.par9uet.jm.store.AppUpdateDownloadStatus
import com.par9uet.jm.store.formatBytes
import com.par9uet.jm.network.DohManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.utils.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.compose.getKoin
import java.io.File
import kotlin.math.roundToInt

private const val GITHUB_RELEASE_API =
    "https://api.github.com/repos/HongShi2333/jmcomic-next/releases/latest"
private const val GITHUB_RELEASE_URL =
    "https://github.com/HongShi2333/jmcomic-next/releases"
private const val GITHUB_REPO_URL =
    "https://github.com/HongShi2333/jmcomic-next"

private data class GithubRelease(
    val version: String,
    val name: String,
    val url: String,
    val body: String,
    val downloadUrl: String,
    val fileName: String
)

private sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Success(val release: GithubRelease, val hasUpdate: Boolean) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val appIcon = remember(context) { loadAppIconBitmap(context) }
    val appVersion = remember(context) { appVersionName(context) }
    val versionCode = remember(context) { appVersionCode(context) }

    CommonScaffold(title = "关于") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { AppHeaderCard(appIcon, appVersion, versionCode) }
            item { RepoCard(onOpenRepo = { uriHandler.openUri(GITHUB_REPO_URL) }) }
            item { TechCard() }
        }
    }
}

@Composable
private fun AppHeaderCard(
    appIcon: android.graphics.Bitmap?,
    appVersion: String,
    versionCode: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            appIcon?.let {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "应用图标",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = "JMcomic",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoChip(text = "v$appVersion")
                InfoChip(text = "($versionCode)")
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun RepoCard(onOpenRepo: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "项目仓库",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            SelectionContainer {
                Text(
                    text = GITHUB_REPO_URL,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(
                onClick = onOpenRepo,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("打开仓库")
            }
        }
    }
}

@Composable
private fun TechCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "技术栈",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TechRow("Jetpack Compose", "声明式 UI 框架")
            TechRow("Material You 3", "动态主题设计")
            TechRow("Coil", "图片加载与缓存")
            TechRow("Koin", "依赖注入")
            TechRow("Room", "本地数据库")
            TechRow("JMComic-Api-Java", "内置 API 支持")
        }
    }
}

@Composable
private fun TechRow(name: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpdateScreen(
    updateDownloadManager: AppUpdateDownloadManager = getKoin().get(),
    dohManager: DohManager = getKoin().get(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val appIcon = remember(context) { loadAppIconBitmap(context) }
    val appVersion = remember(context) { appVersionName(context) }
    val versionCode = remember(context) { appVersionCode(context) }
    val coroutineScope = rememberCoroutineScope()
    val downloadState by updateDownloadManager.state.collectAsState()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var visibleRelease by remember { mutableStateOf<GithubRelease?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    fun checkUpdate() {
        updateState = UpdateState.Checking
        coroutineScope.launch {
            val nextState = fetchLatestRelease(dohManager).fold(
                onSuccess = {
                    UpdateState.Success(
                        release = it,
                        hasUpdate = compareVersion(it.version, appVersion) > 0
                    )
                },
                onFailure = {
                    UpdateState.Error(it.message ?: "检查更新失败")
                }
            )
            updateState = nextState
            if (nextState is UpdateState.Success && nextState.hasUpdate) {
                visibleRelease = nextState.release
            }
        }
    }

    LaunchedEffect(Unit) {
        checkUpdate()
    }

    // 检查下载是否已完成，用于展示安装按钮
    val apkReady = downloadState.status == AppUpdateDownloadStatus.Completed &&
        downloadState.savedPath.isNotEmpty() &&
        File(downloadState.savedPath).exists()

    CommonScaffold(title = "检查更新") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CurrentVersionCard(appIcon, appVersion, versionCode)
            }
            item {
                UpdateStatusCard(
                    updateState = updateState,
                    appVersion = appVersion,
                    onRetry = { checkUpdate() },
                    onViewRelease = { visibleRelease = (updateState as? UpdateState.Success)?.release }
                )
            }
            if (apkReady) {
                item {
                    InstallCard(
                        version = downloadState.version,
                        onInstall = { installApk(context, downloadState.savedPath) }
                    )
                }
            }
        }
    }

    visibleRelease?.let { release ->
        ReleaseDialog(
            release = release,
            onCopyDownloadUrl = {
                clipboardManager.setText(AnnotatedString(release.downloadUrl.ifBlank { release.url }))
            },
            onDismiss = { visibleRelease = null },
            onDownload = {
                updateDownloadManager.start(
                    AppUpdateDownloadRequest(
                        version = release.version,
                        fileName = release.fileName.ifBlank { "jm-mobile_v${release.version}_unknown.apk" },
                        downloadUrl = release.downloadUrl
                    )
                )
                visibleRelease = null
                showDownloadDialog = true
            }
        )
    }

    if (showDownloadDialog && !downloadState.background) {
        UpdateDownloadDialog(
            onDismiss = { showDownloadDialog = false },
            onPauseResume = {
                if (downloadState.status == AppUpdateDownloadStatus.Paused) {
                    updateDownloadManager.resume()
                } else {
                    updateDownloadManager.pause()
                }
            },
            onCancel = {
                updateDownloadManager.cancel()
                showDownloadDialog = false
            },
            onBackground = {
                updateDownloadManager.sendToBackground()
                showDownloadDialog = false
            },
            downloadState = downloadState
        )
    }
}

@Composable
private fun InstallCard(
    version: String,
    onInstall: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "更新包已就绪",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "v$version 已下载完成，点击下方按钮立即安装。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("安装更新")
            }
        }
    }
}

@Composable
private fun CurrentVersionCard(
    appIcon: android.graphics.Bitmap?,
    appVersion: String,
    versionCode: String,
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            appIcon?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp)) }
            Text("JMcomic · 手表版", style = MaterialTheme.typography.titleSmall)
            Text("v$appVersion", style = MaterialTheme.typography.bodyMedium)
            Text("版本代码 $versionCode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpdateStatusCard(
    updateState: UpdateState,
    appVersion: String,
    onRetry: () -> Unit,
    onViewRelease: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "更新检查",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            when (val state = updateState) {
                UpdateState.Idle -> {
                    StatusRow(
                        icon = Icons.Rounded.Info,
                        text = "打开页面后会自动检查 GitHub Releases。"
                    )
                }
                UpdateState.Checking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "正在检查更新...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is UpdateState.Success -> {
                    if (state.hasUpdate) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "发现新版本",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = state.release.version,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        if (state.release.name.isNotBlank()) {
                            Text(
                                text = state.release.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalButton(
                            onClick = onViewRelease,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("查看更新内容")
                        }
                    } else {
                        StatusRow(
                            icon = Icons.Rounded.Info,
                            text = "当前已经是最新版本。",
                            highlight = false
                        )
                        Text(
                            text = "最新版本：${state.release.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }
                is UpdateState.Error -> {
                    StatusRow(
                        icon = Icons.Rounded.Info,
                        text = "检查失败：${state.message}",
                        highlight = true
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("重新检查")
            }
        }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    highlight: Boolean = false,
) {
    Text(text, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
        color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseDialog(
    release: GithubRelease,
    onCopyDownloadUrl: () -> Unit,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    com.par9uet.jm.ui.components.WatchAlertDialog(onDismissRequest = onDismiss, title = { Text("更新 ${release.version}") }, text = {
        Column {
            if (release.name.isNotBlank()) Text(release.name, style = MaterialTheme.typography.bodySmall)
            // Plain full-width text is readable without a nested phone-sized Markdown WebView.
            Text(release.body.ifBlank { "此版本未填写更新说明。" }, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCopyDownloadUrl, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("复制下载链接") }
        }
    }, confirmButton = { Button(onClick = onDownload, enabled = release.downloadUrl.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("下载更新") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("返回") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateDownloadDialog(
    onDismiss: () -> Unit,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onBackground: () -> Unit,
    downloadState: com.par9uet.jm.store.AppUpdateDownloadState
) {
    val status = downloadState.status
    val isDone = status == AppUpdateDownloadStatus.Completed ||
        status == AppUpdateDownloadStatus.Error ||
        status == AppUpdateDownloadStatus.Canceled
    val isPaused = status == AppUpdateDownloadStatus.Paused

    com.par9uet.jm.ui.components.WatchAlertDialog(onDismissRequest = { if (isDone) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = isDone, dismissOnClickOutside = isDone), title = { Text("下载更新") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(downloadState.fileName.ifBlank { "更新包" }, maxLines = 2)
            Text(when (status) {
                AppUpdateDownloadStatus.Downloading -> "下载中"; AppUpdateDownloadStatus.Paused -> "已暂停"
                AppUpdateDownloadStatus.Completed -> "下载完成，正在安装"; AppUpdateDownloadStatus.Canceled -> "已取消"
                AppUpdateDownloadStatus.Error -> "下载失败：${downloadState.errorMessage}"; AppUpdateDownloadStatus.Idle -> "等待下载"
            })
            LinearProgressIndicator(progress = { downloadState.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("${(downloadState.progress * 100).roundToInt()}% · ${formatBytes(downloadState.downloadedBytes)}", style = MaterialTheme.typography.bodySmall)
            if (status == AppUpdateDownloadStatus.Downloading || isPaused) TextButton(onClick = onPauseResume, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (isPaused) "继续下载" else "暂停下载") }
            if (!isDone) TextButton(onClick = onBackground, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("后台下载") }
        }
    }, confirmButton = { TextButton(onClick = if (isDone) onDismiss else onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (isDone) "关闭" else "取消下载") } })
}

private fun installApk(context: Context, savedPath: String) {
    val file = File(savedPath)
    if (!file.exists()) {
        android.widget.Toast.makeText(
            context,
            "安装包文件不存在，请重新下载",
            android.widget.Toast.LENGTH_LONG
        ).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        com.par9uet.jm.utils.logError("AppUpdate", "打开系统安装器失败: ${error.stackTraceToString()}")
        android.widget.Toast.makeText(
            context,
            "打开安装器失败：${error.message ?: "未知错误"}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

private suspend fun fetchLatestRelease(dohManager: DohManager): Result<GithubRelease> = withContext(Dispatchers.IO) {
    runCatching {
        val request = Request.Builder()
            .url(GITHUB_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "jmcomic-next-android")
            .build()
        OkHttpClient.Builder()
            .dns(dohManager)
            .build()
            .newCall(request)
            .execute()
            .use { response ->
            if (!response.isSuccessful) {
                error("GitHub 返回 ${response.code}")
            }
            val body = response.body?.string() ?: error("GitHub 返回空响应")
            val json = JsonParser.parseString(body).asJsonObject
            val tagName = json.stringOrEmpty("tag_name")
            val name = json.stringOrEmpty("name")
            val url = json.stringOrEmpty("html_url")
            val version = normalizeVersion(tagName.ifBlank { name })
            if (version.isBlank()) {
                error("未读取到 Release 版本号")
            }
            val asset = selectApkAsset(json.getAsJsonArray("assets"), version)
            GithubRelease(
                version = version,
                name = name,
                url = url.ifBlank { "$GITHUB_RELEASE_URL/tag/$tagName" },
                body = json.stringOrEmpty("body"),
                downloadUrl = asset?.downloadUrl.orEmpty(),
                fileName = asset?.name.orEmpty()
            )
        }
    }
}

private data class ReleaseAsset(val name: String, val downloadUrl: String)

private fun selectApkAsset(assets: JsonArray?, version: String): ReleaseAsset? {
    if (assets == null) return null
    val apkAssets = assets.mapNotNull { item ->
        val obj = item.asJsonObject
        val name = obj.stringOrEmpty("name")
        val url = obj.stringOrEmpty("browser_download_url")
        if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
            ReleaseAsset(name, url)
        } else {
            null
        }
    }
    return apkAssets.firstOrNull {
        it.name.contains("jm-mobile_v$version", ignoreCase = true)
    } ?: apkAssets.firstOrNull()
}

private fun JsonObject.stringOrEmpty(key: String): String {
    return get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
}

private fun normalizeVersion(value: String): String {
    return value.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore(" ")
        .substringBefore("-")
}

private fun compareVersion(left: String, right: String): Int {
    val leftParts = normalizeVersion(left).split(".").map { it.toIntOrNull() ?: 0 }
    val rightParts = normalizeVersion(right).split(".").map { it.toIntOrNull() ?: 0 }
    val count = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until count) {
        val l = leftParts.getOrElse(index) { 0 }
        val r = rightParts.getOrElse(index) { 0 }
        if (l != r) return l.compareTo(r)
    }
    return 0
}

@Suppress("DEPRECATION")
private fun appVersionName(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "unknown" }
}

@Suppress("DEPRECATION")
private fun appVersionCode(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString()
    }.getOrNull().orEmpty().ifBlank { "unknown" }
}

@Suppress("DEPRECATION")
private fun loadAppIconBitmap(context: Context) = runCatching {
    val drawable = context.packageManager.getApplicationIcon(context.packageName)
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
    val bitmap = android.graphics.Bitmap.createBitmap(
        width,
        height,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    bitmap
}.getOrNull()
