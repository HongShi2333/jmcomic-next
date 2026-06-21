package com.par9uet.jm.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import com.par9uet.jm.ui.components.CommonScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val GITHUB_RELEASE_API =
    "https://api.github.com/repos/HongShi2333/jmcomic-next/releases/latest"
private const val GITHUB_RELEASE_URL =
    "https://github.com/HongShi2333/jmcomic-next/releases"
private const val GITHUB_REPO_URL =
    "https://github.com/HongShi2333/jmcomic-next"

private data class GithubRelease(
    val version: String,
    val name: String,
    val url: String
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
                    SectionLine("应用名称：禁漫天堂")
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
fun CheckUpdateScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val appIcon = remember(context) { loadAppIconBitmap(context) }
    val appVersion = remember(context) { appVersionName(context) }
    val versionCode = remember(context) { appVersionCode(context) }
    val coroutineScope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    fun checkUpdate() {
        updateState = UpdateState.Checking
        coroutineScope.launch {
            updateState = fetchLatestRelease().fold(
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
        }
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
                        UpdateState.Idle -> {
                            SectionLine("可检查 GitHub Releases 是否有新版本。")
                        }

                        UpdateState.Checking -> {
                            CircularProgressIndicator()
                            SectionLine("正在检查更新...")
                        }

                        is UpdateState.Success -> {
                            if (state.hasUpdate) {
                                SectionLine("发现新版本：${state.release.version}")
                                SectionLine("Release 名称：${state.release.name.ifBlank { state.release.version }}")
                                SectionLine("Release 链接：${state.release.url}")
                                SectionLine("请复制 Release 链接后到浏览器下载。")
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(state.release.url))
                                    }
                                ) {
                                    Text("复制 Release 链接")
                                }
                            } else {
                                SectionLine("当前已是最新版本。")
                                SectionLine("最新版本：${state.release.version}")
                            }
                        }

                        is UpdateState.Error -> {
                            SectionLine("检查失败：${state.message}")
                        }
                    }

                    Button(onClick = { checkUpdate() }) {
                        Text("检查更新")
                    }
                }
            }
        }
    }
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
            val tagName = json.get("tag_name")?.asString.orEmpty()
            val name = json.get("name")?.asString.orEmpty()
            val url = json.get("html_url")?.asString.orEmpty()
            val version = normalizeVersion(tagName.ifBlank { name })
            if (version.isBlank()) {
                error("未读取到 Release 版本号")
            }
            GithubRelease(
                version = version,
                name = name,
                url = url.ifBlank { GITHUB_RELEASE_URL }
            )
        }
    }
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
