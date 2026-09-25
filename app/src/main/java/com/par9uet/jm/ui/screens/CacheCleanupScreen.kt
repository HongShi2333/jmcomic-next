package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.cache.getComicCoverCacheDir
import com.par9uet.jm.cache.getDownloadDir
import com.par9uet.jm.cache.getLegacyDownloadDir
import com.par9uet.jm.cache.cachePathSize
import com.par9uet.jm.cache.deleteCachePath
import com.par9uet.jm.cache.isDocumentCachePath
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.utils.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import org.koin.compose.getKoin

private data class CacheItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val sizeBytes: Long,
    val dir: File?,
    val documentPaths: List<String> = emptyList(),
)

@Composable
@OptIn(ExperimentalCoilApi::class)
fun CacheCleanupScreen(
    downloadComicDao: DownloadComicDao = getKoin().get(),
    imageLoader: ImageLoader = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var loading by remember { mutableStateOf(true) }
    var cleaning by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var cleanResult by remember { mutableStateOf<String?>(null) }
    val checkedMap = remember { mutableStateMapOf<String, Boolean>() }

    var cacheItems by remember { mutableStateOf<List<CacheItem>>(emptyList()) }
    var downloadRecordIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        withContext(Dispatchers.IO) {
            val items = mutableListOf<CacheItem>()

            val comicCoverCacheDir = getComicCoverCacheDir(context)
            val comicCoverCacheSize = dirSize(comicCoverCacheDir)
            items.add(
                CacheItem(
                    id = "comic_covers",
                    icon = Icons.Default.Cached,
                    title = "漫画封面缓存",
                    description = "封面与图片加载缓存，清理后图片需重新下载",
                    sizeBytes = comicCoverCacheSize,
                    dir = comicCoverCacheDir
                )
            )

            val downloadDir = getDownloadDir(context)
            val downloadRecords = downloadComicDao.getAll()
            downloadRecordIds = downloadRecords.map { it.id }
            val documentPaths = downloadRecords
                .flatMap { listOf(it.zipPath, it.coverPath) }
                .filter(::isDocumentCachePath)
                .distinct()
            val downloadSize = dirSize(downloadDir) + documentPaths.sumOf { cachePathSize(context, it) }
            items.add(
                CacheItem(
                    id = "download",
                    icon = Icons.Default.Folder,
                    title = "漫画缓存",
                    description = "已下载的漫画图片，清理后需重新下载",
                    sizeBytes = downloadSize,
                    dir = downloadDir,
                    documentPaths = documentPaths,
                )
            )

            val picDecodeDir = File(context.cacheDir, "pic_decode")
            val picDecodeSize = dirSize(picDecodeDir)
            items.add(
                CacheItem(
                    id = "pic_decode",
                    icon = Icons.Default.BrokenImage,
                    title = "解码缓存",
                    description = "图片解密临时文件，可安全清理",
                    sizeBytes = picDecodeSize,
                    dir = picDecodeDir
                )
            )

            val pdfDir = File(context.cacheDir, "pdf_export")
            val pdfSize = dirSize(pdfDir)
            items.add(
                CacheItem(
                    id = "pdf",
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF 导出缓存",
                    description = "PDF 导出临时文件，可安全清理",
                    sizeBytes = pdfSize,
                    dir = pdfDir
                )
            )

            val totalAppCache = context.cacheDir
            val totalSize = dirSize(totalAppCache) +
                dirSize(getDownloadDir(context)) +
                dirSize(getLegacyDownloadDir(context)) +
                documentPaths.sumOf { cachePathSize(context, it) }
            items.add(
                CacheItem(
                    id = "total",
                    icon = Icons.Default.DeleteSweep,
                    title = "全部应用缓存",
                    description = "包含以上所有缓存和其他临时文件",
                    sizeBytes = totalSize,
                    dir = totalAppCache,
                    documentPaths = documentPaths,
                )
            )

            cacheItems = items
            loading = false
        }
    }

    val totalSelected = cacheItems.filter { checkedMap[it.id] == true }.sumOf { it.sizeBytes }

    if (showConfirmDialog) {
        val selectedItems = cacheItems.filter { checkedMap[it.id] == true }
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认清理") },
            text = {
                Text(
                    "将清理 ${selectedItems.size} 项缓存，" +
                        "共 ${formatBytes(totalSelected)}。\n\n" +
                        selectedItems.joinToString("\n") { "• ${it.title}" }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        cleaning = true
                        scope.launch {
                            var freedBytes = 0L
                            withContext(Dispatchers.IO) {
                                val selectedIds = selectedItems.mapTo(mutableSetOf()) { it.id }
                                val clearAll = "total" in selectedIds
                                val clearImageCache = clearAll || "comic_covers" in selectedIds
                                if (clearImageCache) {
                                    val imageCacheDir = getComicCoverCacheDir(context)
                                    freedBytes += dirSize(imageCacheDir)
                                    runCatching { imageLoader.memoryCache?.clear() }
                                    runCatching { imageLoader.diskCache?.clear() }
                                    // Coil keeps this cache instance alive; never delete its root directory directly.
                                    getComicCoverCacheDir(context)
                                }

                                if (clearAll) {
                                    context.cacheDir.listFiles().orEmpty()
                                        .filterNot { it.name in setOf("common", "comic_covers") }
                                        .forEach { child ->
                                            freedBytes += dirSize(child)
                                            child.deleteRecursively()
                                        }
                                    listOf(getDownloadDir(context), getLegacyDownloadDir(context))
                                        .distinctBy { it.absolutePath }
                                        .forEach { dir ->
                                            freedBytes += dirSize(dir)
                                            dir.deleteRecursively()
                                        }
                                } else {
                                    selectedItems.filter { it.id !in setOf("comic_covers", "total") }.forEach { item ->
                                        item.dir?.let { dir ->
                                            freedBytes += dirSize(dir)
                                            dir.deleteRecursively()
                                        }
                                    }
                                }

                                if (clearAll || "download" in selectedIds) {
                                    selectedItems.flatMap { it.documentPaths }.distinct().forEach { path ->
                                        freedBytes += cachePathSize(context, path)
                                        deleteCachePath(context, path)
                                    }
                                }
                                selectedIds.forEach { checkedMap[it] = false }
                                if ((clearAll || "download" in selectedIds) && downloadRecordIds.isNotEmpty()) {
                                    downloadComicDao.deleteByIds(downloadRecordIds)
                                }
                            }
                            cleaning = false
                            cleanResult = "已清理 ${formatBytes(freedBytes)}"
                            loading = true
                            reloadKey++
                        }
                    }
                ) { Text("清理", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    CommonScaffold(title = "缓存清理") {
        if (loading) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cleanResult?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                cacheItems.forEach { item ->
                    val checked = checkedMap[item.id] == true
                    val isTotal = item.id == "total"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTotal)
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            else
                                MaterialTheme.colorScheme.surfaceContainer
                        ),
                        onClick = { checkedMap[item.id] = !checked }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { checkedMap[item.id] = it }
                            )
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatBytes(item.sizeBytes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (totalSelected > 0) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !cleaning
                    ) {
                        if (cleaning) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text("清理选中项 (${formatBytes(totalSelected)})")
                    }
                }
            }
        }
    }
}

private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.walkBottomUp()
        .filter { it.isFile }
        .sumOf { it.length() }
}
