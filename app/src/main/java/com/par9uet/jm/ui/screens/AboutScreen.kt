package com.par9uet.jm.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.store.AppUpdateDownloadManager
import com.par9uet.jm.store.AppUpdateDownloadRequest
import com.par9uet.jm.store.AppUpdateDownloadStatus
import com.par9uet.jm.store.formatBytes
import com.par9uet.jm.ui.components.CommonScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.compose.getKoin
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SimpleSection("应用信息") {
                    appIcon?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "应用图标",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    SectionLine("应用名称：JMcomic")
                    SectionLine("版本名称：$appVersion")
                    SectionLine("版本代码：$versionCode")
                }
            }
            item {
                SimpleSection("项目仓库") {
                    SectionLine("仓库地址：$GITHUB_REPO_URL")
                    OutlinedButton(onClick = { uriHandler.openUri(GITHUB_REPO_URL) }) {
                        Text("打开 GitHub 仓库")
                    }
                }
            }
        }
    }
}

@Composable
fun CheckUpdateScreen(
    updateDownloadManager: AppUpdateDownloadManager = getKoin().get()
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
            val nextState = fetchLatestRelease().fold(
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

    CommonScaffold(title = "检查更新") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SimpleSection("当前版本") {
                    appIcon?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "应用图标",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    SectionLine("版本名称：$appVersion")
                    SectionLine("版本代码：$versionCode")
                }
            }
            item {
                SimpleSection("更新检查") {
                    when (val state = updateState) {
                        UpdateState.Idle -> SectionLine("打开页面后会自动检查 GitHub Releases。")
                        UpdateState.Checking -> {
                            CircularProgressIndicator()
                            SectionLine("正在检查更新...")
                        }
                        is UpdateState.Success -> {
                            if (state.hasUpdate) {
                                SectionLine("发现新版本：${state.release.version}")
                                SectionLine("Release：${state.release.name.ifBlank { state.release.version }}")
                                SectionLine("APK：${state.release.fileName.ifBlank { "未找到" }}")
                                OutlinedButton(onClick = { visibleRelease = state.release }) {
                                    Text("查看更新内容")
                                }
                            } else {
                                SectionLine("当前已经是最新版本。")
                                SectionLine("最新版本：${state.release.version}")
                            }
                        }
                        is UpdateState.Error -> SectionLine("检查失败：${state.message}")
                    }

                    Button(onClick = { checkUpdate() }) {
                        Text("重新检查")
                    }
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
            statusText = when (downloadState.status) {
                AppUpdateDownloadStatus.Downloading -> "下载中"
                AppUpdateDownloadStatus.Paused -> "已暂停"
                AppUpdateDownloadStatus.Completed -> "下载完成"
                AppUpdateDownloadStatus.Canceled -> "已取消"
                AppUpdateDownloadStatus.Error -> "下载失败：${downloadState.errorMessage}"
                AppUpdateDownloadStatus.Idle -> "等待下载"
            },
            fileName = downloadState.fileName,
            progress = downloadState.progress,
            speed = downloadState.speedBytesPerSecond,
            downloaded = downloadState.downloadedBytes,
            total = downloadState.totalBytes,
            isPaused = downloadState.status == AppUpdateDownloadStatus.Paused,
            isDone = downloadState.status == AppUpdateDownloadStatus.Completed ||
                downloadState.status == AppUpdateDownloadStatus.Error ||
                downloadState.status == AppUpdateDownloadStatus.Canceled
        )
    }
}

@Composable
private fun ReleaseDialog(
    release: GithubRelease,
    onCopyDownloadUrl: () -> Unit,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本") },
        text = {
            Column {
                Text(
                    text = release.name.ifBlank { release.version },
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = release.body.ifBlank { "此 Release 未填写更新内容。" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = release.downloadUrl.isNotBlank(),
                onClick = onDownload
            ) {
                Text("下载更新")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCopyDownloadUrl) {
                    Text("复制下载链接")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
}

@Composable
private fun UpdateDownloadDialog(
    onDismiss: () -> Unit,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onBackground: () -> Unit,
    statusText: String,
    fileName: String,
    progress: Float,
    speed: Long,
    downloaded: Long,
    total: Long,
    isPaused: Boolean,
    isDone: Boolean
) {
    AlertDialog(
        onDismissRequest = {
            if (isDone) onDismiss()
        },
        title = { Text("下载更新") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLine(fileName.ifBlank { "更新包" })
                SectionLine(statusText)
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                SectionLine(
                    "${(progress * 100).roundToInt()}% · " +
                        "${formatBytes(downloaded)} / ${if (total > 0) formatBytes(total) else "未知大小"}"
                )
                SectionLine("速度：${formatBytes(speed)}/s")
            }
        },
        confirmButton = {
            Button(
                enabled = !isDone,
                onClick = onPauseResume
            ) {
                Text(if (isPaused) "继续" else "暂停")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) {
                    Text(if (isDone) "关闭" else "取消")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    enabled = !isDone,
                    onClick = onBackground
                ) {
                    Text("后台下载")
                }
            }
        }
    )
}

@Composable
private fun SimpleSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
private fun SectionLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private suspend fun fetchLatestRelease(): Result<GithubRelease> = withContext(Dispatchers.IO) {
    runCatching {
        val request = Request.Builder()
            .url(GITHUB_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "jmcomic-next-android")
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("GitHub 返回 ${response.code}")
            }
            val body = response.body.string()
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
