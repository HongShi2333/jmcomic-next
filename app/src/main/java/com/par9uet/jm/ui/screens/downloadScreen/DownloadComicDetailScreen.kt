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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.ImageLoader
import coil.compose.AsyncImage
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.DownloadProgressMessageStore
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.ChapterMultiSelectDialog
import com.par9uet.jm.ui.components.ChapterSingleSelectDialog
import com.par9uet.jm.ui.components.ComicContentTag
import com.par9uet.jm.ui.components.ComicIdChip
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.DownloadComicDetailViewModel
import com.par9uet.jm.utils.CachedComicInfo
import com.par9uet.jm.utils.exportComicToPdf
import com.par9uet.jm.utils.exportComicsToMergedPdf
import com.par9uet.jm.utils.exportComicsToSeparatePdf
import com.par9uet.jm.utils.formatBytes
import com.par9uet.jm.utils.getCachedComicInfo
import com.par9uet.jm.cache.isDocumentCachePath
import com.par9uet.jm.cache.cachePathExists
import com.par9uet.jm.cache.cachePathHasContent
import com.par9uet.jm.cache.listComicImagePaths
import com.par9uet.jm.cache.CacheIntegrityResult
import com.par9uet.jm.cache.checkComicCacheIntegrity
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_OFF
import com.par9uet.jm.data.models.CACHE_INTEGRITY_CHECK_FULL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    downloadManager: DownloadManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainNavController = LocalMainNavController.current
    val detailState by viewModel.detailState.collectAsState()
    val progressMessages by DownloadProgressMessageStore.messages.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val scrollState = rememberScrollState()
    var cachedInfo by remember { mutableStateOf<CachedComicInfo?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var selectedExportChapterIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var activeDialog by remember { mutableStateOf<DownloadDetailDialog?>(null) }
    var exportMode by remember { mutableStateOf(PdfExportMode.Merge) }
    var integrityResult by remember { mutableStateOf(CacheIntegrityResult()) }
    var forcedIntegrityResult by remember { mutableStateOf<CacheIntegrityResult?>(null) }
    var forceIntegrityCheckToken by remember { mutableStateOf(0) }
    var forceIntegrityChecking by remember { mutableStateOf(false) }
    var dismissedIntegritySignature by remember { mutableStateOf<String?>(null) }
    var integrityRefreshToken by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 文件管理器删除缓存不会触发 Room 状态变化。重新回到应用时强制重扫，
    // 这样删除单页、全部图片或整个章节目录都会立即出现完整性提示。
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, localSetting.cacheIntegrityCheckMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                integrityRefreshToken++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val selectedItems = detailState.completeItems.filter { it.id in selectedExportChapterIds }
        if (uri == null) {
            toastManager.showAsync("未选择导出文件夹")
            return@rememberLauncherForActivityResult
        }
        if (selectedItems.isEmpty()) {
            toastManager.showAsync("未选择可导出的缓存章节")
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
                    when (exportMode) {
                        PdfExportMode.Merge -> {
                            if (selectedItems.size > 1) {
                                exportComicsToMergedPdf(context, selectedItems, uri)
                            } else {
                                exportComicToPdf(context, selectedItems.first(), uri)
                            }
                        }

                        PdfExportMode.SplitByChapter -> {
                            exportComicsToSeparatePdf(context, selectedItems, uri)
                        }
                    }
                }
            }
            exporting = false
            result
                .onSuccess {
                    toastManager.showAsync(
                        if (exportMode == PdfExportMode.SplitByChapter) {
                            "已导出 ${selectedItems.size} 个章节 PDF"
                        } else {
                            "PDF 导出成功"
                        }
                    )
                }
                .onFailure { toastManager.showAsync(it.message ?: "PDF 导出失败") }
        }
    }

    LaunchedEffect(id) {
        viewModel.load(id)
    }

    LaunchedEffect(
        detailState.completeItems,
        detailState.cachePath,
        localSetting.cacheIntegrityCheckMode,
        integrityRefreshToken,
    ) {
        cachedInfo = if (detailState.completeItems.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val infos = detailState.completeItems.map { comic ->
                    runCatching { getCachedComicInfo(context, comic) }
                        .getOrDefault(CachedComicInfo(0, 0L, null, null))
                }
                val cacheRoot = detailState.cachePath.takeIf { it.isNotBlank() && !isDocumentCachePath(it) }?.let(::File)
                val rootBytes = cacheRoot?.takeIf { it.isDirectory }?.let(::directorySize)
                CachedComicInfo(
                    imageCount = infos.sumOf { it.imageCount },
                    totalBytes = rootBytes ?: infos.sumOf { it.totalBytes },
                    imageDir = infos.mapNotNull { it.imageDir }.firstOrNull(),
                    zipFile = infos.mapNotNull { it.zipFile }.firstOrNull()
                )
            }
        }
        // Clear the previous result before a new scan. If the screen is
        // recomposed while the scan is running, the old error must not flash.
        forcedIntegrityResult = null
        integrityResult = CacheIntegrityResult()
        integrityResult = try {
            withContext(Dispatchers.IO) {
                // A worker marks the chapter complete before the final config
                // write. Do not inspect during that short window, otherwise a
                // healthy download gets a permanent false corruption dialog.
                if (localSetting.cacheIntegrityCheckMode == CACHE_INTEGRITY_CHECK_OFF ||
                    detailState.isDownloading || detailState.hasError
                ) {
                    CacheIntegrityResult()
                } else {
                    delay(300)
                    suspend fun scan() = checkComicCacheIntegrity(
                        context = context,
                        chapters = detailState.completeItems,
                        mode = localSetting.cacheIntegrityCheckMode,
                    )
                    val first = scan()
                    if (first.isHealthy) {
                        first
                    } else {
                        // A just-finished worker may still be writing the
                        // manifest. Require two identical results so a normal
                        // comic never shows a one-frame corruption dialog.
                        delay(250)
                        val second = scan()
                        if (second == first) second else CacheIntegrityResult()
                    }
                }
            }
        } catch (error: CancellationException) {
            // Compose cancels LaunchedEffect during navigation/recomposition.
            // Cancellation is not a cache error and must never become a dialog.
            throw error
        } catch (error: Exception) {
            // Non-cancellation failures are real scan failures and remain
            // actionable; only Compose cancellation is ignored above.
            val ids = detailState.completeItems.mapTo(mutableSetOf()) { it.id }
            CacheIntegrityResult(
                brokenChapterIds = ids,
                chapterIds = ids,
                reason = "缓存完整性检查失败：${error.message ?: "无法读取缓存目录"}",
            )
        }
    }

    // 手动检查始终使用“完全检查”，不受设置中的关闭/部分检查影响。
    LaunchedEffect(forceIntegrityCheckToken) {
        if (forceIntegrityCheckToken == 0) return@LaunchedEffect
        forceIntegrityChecking = true
        forcedIntegrityResult = try {
            withContext(Dispatchers.IO) {
                checkComicCacheIntegrity(
                    context = context,
                    chapters = detailState.completeItems,
                    mode = CACHE_INTEGRITY_CHECK_FULL,
                )
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            val ids = detailState.completeItems.mapTo(mutableSetOf()) { it.id }
            CacheIntegrityResult(
                brokenChapterIds = ids,
                chapterIds = ids,
                reason = "缓存完整性检查失败：${error.message ?: "无法读取缓存目录"}",
            )
        }
        forceIntegrityChecking = false
    }

    val displayedIntegrityResult = forcedIntegrityResult ?: integrityResult
    val integritySignature = "${displayedIntegrityResult.brokenChapterIds.sorted().joinToString(",")}:${displayedIntegrityResult.missingCover}:${displayedIntegrityResult.missingConfig}:${displayedIntegrityResult.reason}"
    if (!displayedIntegrityResult.isHealthy && dismissedIntegritySignature != integritySignature) {
        AlertDialog(
            onDismissRequest = { dismissedIntegritySignature = integritySignature },
            title = { Text("漫画内容损坏") },
            text = {
                Text(
                    displayedIntegrityResult.reason.ifBlank {
                        "漫画内容损坏，需重新下载。"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dismissedIntegritySignature = integritySignature
                    downloadManager.repairCachedItems(displayedIntegrityResult)
                }) { Text("重新下载") }
            },
            dismissButton = {
                TextButton(onClick = { dismissedIntegritySignature = integritySignature }) { Text("暂不") }
            },
        )
    }

    when (activeDialog) {
        DownloadDetailDialog.ReadChapter -> {
            ChapterSingleSelectDialog(
                title = "选择缓存章节",
                chapters = detailState.readableChapters,
                currentChapterId = null,
                onDismiss = { activeDialog = null },
                onSelect = { chapter ->
                    activeDialog = null
                    mainNavController.navigate("localComicRead/${chapter.id}")
                }
            )
        }

        DownloadDetailDialog.ExportChapter -> {
            ChapterMultiSelectDialog(
                title = "选择导出章节",
                chapters = detailState.readableChapters,
                selectedChapterIds = selectedExportChapterIds,
                onSelectedChange = { selectedExportChapterIds = it },
                onDismiss = { activeDialog = null },
                confirmText = "合并导出",
                onConfirm = {
                    exportMode = PdfExportMode.Merge
                    activeDialog = null
                    exportLauncher.launch(null)
                },
                secondaryConfirmText = "分章导出",
                onSecondaryConfirm = {
                    exportMode = PdfExportMode.SplitByChapter
                    activeDialog = null
                    exportLauncher.launch(null)
                }
            )
        }

        null -> Unit
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (detailState.found && detailState.canRead) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    DownloadDetailBottomActions(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        isMultiChapter = detailState.isMultiChapter,
                        exporting = exporting,
                        onExport = {
                            selectedExportChapterIds = detailState.completeItems.map { it.id }.toSet()
                            activeDialog = DownloadDetailDialog.ExportChapter
                        },
                        onSelectChapter = {
                            activeDialog = DownloadDetailDialog.ReadChapter
                        },
                        onRead = {
                            mainNavController.navigate("localComicRead/${detailState.completeItems.first().id}")
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (!detailState.found) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (!detailState.loading) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "未找到缓存详情",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LocalCachedHeroCard(detailState = detailState, imageLoader = imageLoader)
            LocalCachedTagCard(tags = detailState.tagList)
            LocalCachedChapterDirectoryCard(
                chapters = detailState.readableChapters,
                onOpenAll = { activeDialog = DownloadDetailDialog.ReadChapter },
                onRead = { chapter ->
                    if (chapter.isAvailable) {
                        mainNavController.navigate("localComicRead/${chapter.id}")
                    }
                },
            )
            LocalCacheSummaryCard(
                detailState = detailState,
                cachedInfo = cachedInfo,
                forceIntegrityChecking = forceIntegrityChecking,
                onCheckIntegrity = {
                    forcedIntegrityResult = null
                    forceIntegrityCheckToken++
                },
            )
            if (detailState.isDownloading) {
                val progressGroupId = detailState.allItems.firstOrNull()
                    ?.let { item -> item.groupId.takeIf { it != 0 } ?: item.id }
                val progressMessage = progressMessages[progressGroupId].orEmpty()
                val animatedProgress by animateFloatAsState(
                    targetValue = detailState.groupProgress,
                    label = "downloadProgress"
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CachedInfoItem(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Speed,
                                label = "下载速度",
                                value = formatSpeed(detailState.downloadSpeed),
                            )
                            Text(
                                text = "${(detailState.groupProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (progressMessage.isNotBlank()) {
                            Text(
                                text = progressMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            if (detailState.hasError) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { downloadManager.retryGroup(viewModel.groupId.value) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text("重试下载")
                }
            }
            Text(
                text = "阅读和导出只显示已缓存完成的章节；重复选择已缓存章节时不会重复下载。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class DownloadDetailDialog {
    ReadChapter,
    ExportChapter
}

private enum class PdfExportMode {
    Merge,
    SplitByChapter
}

@Composable
private fun LocalCachedHeroCard(
    detailState: com.par9uet.jm.ui.viewModel.DownloadComicDetailState,
    imageLoader: ImageLoader,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.width(142.dp)) {
                LocalCover(
                    title = detailState.title,
                    coverPath = detailState.coverPath,
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (detailState.remoteCoverComicId > 0) {
                    ComicIdChip(
                        comicId = detailState.remoteCoverComicId,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = detailState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (detailState.authorList.isNotEmpty()) {
                    Text(
                        text = detailState.authorList.joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (detailState.canRead) "已缓存，可直接阅读" else "正在准备缓存内容",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LocalCachedTagCard(tags: List<String>) {
    if (tags.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "内容信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "标签",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.distinct().forEach { tag ->
                    key(tag) { ComicContentTag(tag) }
                }
            }
        }
    }
}

@Composable
private fun LocalCachedChapterDirectoryCard(
    chapters: List<com.par9uet.jm.data.models.ComicChapter>,
    onOpenAll: () -> Unit,
    onRead: (com.par9uet.jm.data.models.ComicChapter) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "章节目录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (chapters.isEmpty()) "暂无缓存章节" else "${chapters.size} 个章节",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (chapters.size > 1) {
                    TextButton(onClick = onOpenAll) { Text("选择章节") }
                }
            }
            if (chapters.isEmpty()) {
                Text(
                    text = "完成下载后，章节会显示在这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                chapters.forEachIndexed { index, chapter ->
                    val chapterColor = if (chapter.isAvailable) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = chapter.isAvailable) { onRead(chapter) },
                        shape = MaterialTheme.shapes.medium,
                        color = chapterColor,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(30.dp),
                            )
                            Text(
                                text = chapter.name.ifBlank { "第 ${index + 1} 章" },
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!chapter.isAvailable) {
                                Text(
                                    text = "未完成",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalCacheSummaryCard(
    detailState: com.par9uet.jm.ui.viewModel.DownloadComicDetailState,
    cachedInfo: CachedComicInfo?,
    forceIntegrityChecking: Boolean,
    onCheckIntegrity: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "缓存信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CachedInfoItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DownloadDone,
                    label = "状态",
                    value = detailState.statusSummary,
                )
                CachedInfoItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.RemoveRedEye,
                    label = "本地阅读",
                    value = if (detailState.canRead) "可用" else "未完成",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CachedInfoItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "缓存章节",
                    value = "${detailState.completeChapterCount} / ${detailState.totalChapterCount}",
                )
                CachedInfoItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Storage,
                    label = "图片数量",
                    value = "${cachedInfo?.imageCount ?: 0} 张",
                )
            }
            CachedInfoItem(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.FolderZip,
                label = "占用空间",
                value = formatBytes(cachedInfo?.totalBytes ?: 0L),
            )
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !forceIntegrityChecking,
                onClick = onCheckIntegrity,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(if (forceIntegrityChecking) "检查中…" else "检查缓存完整性")
            }
            Text(
                text = "缓存时间：${formatTime(detailState.createTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "缓存路径：${detailState.cachePath.ifBlank { detailState.zipPath.ifBlank { "无" } }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DownloadDetailBottomActions(
    modifier: Modifier,
    isMultiChapter: Boolean,
    exporting: Boolean,
    onExport: () -> Unit,
    onSelectChapter: () -> Unit,
    onRead: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            enabled = !exporting,
            contentPadding = PaddingValues(horizontal = 8.dp),
            onClick = onExport
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(if (exporting) "导出中" else if (isMultiChapter) "导出" else "导出 PDF")
        }
        if (isMultiChapter) {
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                onClick = onSelectChapter
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text("章节")
            }
        }
        Button(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            onClick = onRead
        ) {
            Text(if (isMultiChapter) "阅读" else "阅读缓存")
        }
    }
}

@Composable
private fun LocalCover(
    title: String,
    coverPath: String,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val context = LocalContext.current
    val localCoverModel: Any? = when {
        coverPath.isNotBlank() && cachePathExists(context, coverPath) &&
            cachePathHasContent(context, coverPath) -> {
            if (isDocumentCachePath(coverPath)) Uri.parse(coverPath) else File(coverPath)
        }
        else -> null
    }

    if (localCoverModel != null) {
        AsyncImage(
            model = localCoverModel,
            imageLoader = imageLoader,
            contentDescription = "${title}的封面",
            contentScale = ContentScale.Crop,
            modifier = modifier.aspectRatio(0.75f),
        )
    } else {
        Box(
            modifier = modifier.aspectRatio(0.75f),
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

private fun formatTime(value: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value))
}

private fun directorySize(dir: File): Long {
    return dir.walkBottomUp()
        .filter { it.isFile }
        .sumOf { it.length() }
}

private fun formatSpeed(bytesPerSec: Float): String {
    return when {
        bytesPerSec >= 1_048_576 -> String.format("%.1f MB/s", bytesPerSec / 1_048_576)
        bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024)
        else -> "${bytesPerSec.toInt()} B/s"
    }
}
