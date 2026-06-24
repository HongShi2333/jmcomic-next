package com.par9uet.jm.ui.screens.downloadScreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.par9uet.jm.database.dao.ReadingProgressDao
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.database.model.ReadingProgress
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.DownloadComicDetailViewModel
import com.par9uet.jm.utils.CachedComicInfo
import com.par9uet.jm.utils.exportComicToPdf
import com.par9uet.jm.utils.formatBytes
import com.par9uet.jm.utils.getCachedComicInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadComicDetailScreen(
    id: Int,
    viewModel: DownloadComicDetailViewModel = koinViewModel(),
    imageLoader: ImageLoader = getKoin().get(),
    toastManager: ToastManager = getKoin().get(),
    readingProgressDao: ReadingProgressDao = getKoin().get()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainNavController = LocalMainNavController.current
    val comic by viewModel.comic.collectAsState()
    val scrollState = rememberScrollState()
    var cachedInfo by remember { mutableStateOf<CachedComicInfo?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<ReadingProgress?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val data = comic ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            toastManager.showAsync("未选择导出文件夹")
            return@rememberLauncherForActivityResult
        }
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        }
        exporting = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    exportComicToPdf(context, data, uri)
                }
            }
            exporting = false
            result
                .onSuccess { toastManager.showAsync("PDF 导出成功") }
                .onFailure { toastManager.showAsync(it.message ?: "PDF 导出失败") }
        }
    }

    LaunchedEffect(id) {
        viewModel.load(id)
    }
    LaunchedEffect(comic) {
        cachedInfo = comic?.let { withContext(Dispatchers.IO) { getCachedComicInfo(context, it) } }
        // Load reading progress
        comic?.let { downloadComic ->
            progress = withContext(Dispatchers.IO) {
                val parentId = if (downloadComic.parentId != 0) downloadComic.parentId else downloadComic.id
                readingProgressDao.getProgress(parentId)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val data = comic
            if (data != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (data.status == "complete") {
                        FilledTonalButton(
                            enabled = !exporting,
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            onClick = { exportLauncher.launch(null) }
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                            Text(if (exporting) "导出中" else "导出 PDF")
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Show reading progress if available
                    val progressText = progress?.let {
                        "上次读到第${it.pageIndex + 1}/${it.totalPages}页"
                    }

                    Button(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        onClick = {
                            mainNavController.navigate("localComicRead/${data.id}")
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (progressText != null) "继续阅读" else "阅读缓存")
                            if (progressText != null) {
                                Text(progressText, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val data = comic
        if (data == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            LocalCover(
                comic = data,
                imageLoader = imageLoader
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = data.parentName.ifBlank { data.name },
                    fontSize = 18.sp,
                    lineHeight = 1.5.em,
                    fontWeight = FontWeight.Bold,
                )
                downloadChapterLabel(data)?.let { chapterLabel ->
                    Text(
                        text = chapterLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    data.authorList.forEach {
                        key(it) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp,
                                lineHeight = 27.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CachedInfoItem(
                        modifier = Modifier.weight(0.5f),
                        icon = Icons.Default.DownloadDone,
                        label = "缓存状态",
                        value = statusLabel(data.status)
                    )
                    CachedInfoItem(
                        modifier = Modifier.weight(0.5f),
                        icon = Icons.Default.RemoveRedEye,
                        label = "本地阅读",
                        value = if (data.status == "complete") "可用" else "未完成"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CachedInfoItem(
                        modifier = Modifier.weight(0.5f),
                        icon = Icons.Default.Storage,
                        label = "图片数量",
                        value = "${cachedInfo?.imageCount ?: 0} 张"
                    )
                    CachedInfoItem(
                        modifier = Modifier.weight(0.5f),
                        icon = Icons.Default.FolderZip,
                        label = "占用空间",
                        value = formatBytes(cachedInfo?.totalBytes ?: 0L)
                    )
                }
                Text(
                    text = "缓存时间：${formatTime(data.createTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "封面：${data.coverPath.ifBlank { "无" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "缓存包：${data.zipPath.ifBlank { "无" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "导出 PDF 时会先通过系统文件夹选择器请求写入授权，然后把本地缓存图片按顺序写入 PDF。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocalCover(
    comic: DownloadComic,
    imageLoader: ImageLoader
) {
    if (comic.coverPath.isNotBlank()) {
        AsyncImage(
            model = File(comic.coverPath),
            imageLoader = imageLoader,
            contentDescription = "${comic.name}的封面",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        )
    }
}

@Composable
private fun CachedInfoItem(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun downloadChapterLabel(comic: DownloadComic): String? {
    val hasChapterMetadata = comic.parentId != comic.id ||
        comic.chapterCount > 1 ||
        comic.chapterName.isNotBlank()
    if (!hasChapterMetadata) return null

    val numberText = "第" + (comic.chapterIndex + 1) + "话"
    return if (comic.chapterName.isBlank()) {
        numberText
    } else {
        numberText + " · " + comic.chapterName
    }
}

private fun statusLabel(status: String): String {
    return when (status) {
        "complete" -> "已完成"
        "downloading" -> "缓存中"
        "pending" -> "等待中"
        "paused" -> "已暂停"
        "error" -> "出错"
        else -> status
    }
}

private fun formatTime(value: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value))
}
