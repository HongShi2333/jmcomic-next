package com.par9uet.jm.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.ui.components.*
import com.par9uet.jm.ui.viewModel.DownloadViewModel
import com.par9uet.jm.ui.viewModel.DownloadComicDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel
import java.io.File
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.par9uet.jm.store.DownloadProgressMessageStore
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.utils.DownloadSpeedTracker
import java.util.Locale

internal fun cacheProgressPercent(progress: Float, complete: Boolean): Int =
    if (complete) 100 else ((if (progress.isFinite()) progress else 0f).coerceIn(0f, 1f) * 100).toInt().coerceAtMost(99)

/** Speed is compressed page bytes saved per second, not network socket throughput. */
internal fun cacheSaveSpeed(bytesPerSecond: Float): String =
    if (!bytesPerSecond.isFinite() || bytesPerSecond <= 0f) "正在获取图片"
    else if (bytesPerSecond >= 1024f * 1024f) String.format(Locale.ROOT, "保存 %.1f MB/s", bytesPerSecond / (1024f * 1024f))
    else String.format(Locale.ROOT, "保存 %.1f KB/s", bytesPerSecond / 1024f)

@Composable
internal fun WatchCacheProgress(progress: Float, status: String, stage: String, speed: Float) {
    val percent = cacheProgressPercent(progress, status == "complete")
    val animated by animateFloatAsState(
        percent / 100f,
        animationSpec = tween(if (LocalWatchMotionEnabled.current) 180 else 0),
        label = "cache-progress",
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${cacheStatusLabel(status)} · $percent%", style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
        if (stage.isNotBlank() && status in setOf("pending", "downloading", "error")) {
            Text(stage, style = MaterialTheme.typography.labelSmall,
                color = if (status == "error") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status == "downloading") Text(cacheSaveSpeed(speed), style = MaterialTheme.typography.labelSmall)
    }
}

internal fun cacheStatusLabel(status: String) = when (status) {
    "complete" -> "已缓存"
    "paused" -> "已暂停"
    "downloading" -> "缓存中"
    "error" -> "缓存失败"
    else -> "排队中 / 准备启动"
}

@Composable
internal fun WatchCacheScreen(
    viewModel: DownloadViewModel = koinActivityViewModel(),
    manager: DownloadManager = getKoin().get(),
) {
    val nav = LocalMainNavController.current
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val speeds by DownloadSpeedTracker.speedByGroup.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("all") }
    var chooseFilter by remember { mutableStateOf(false) }
    val visible = groups.filter { when (filter) { "complete" -> it.status == "complete"; "active" -> it.status in setOf("pending", "downloading", "paused"); "error" -> it.status == "error"; else -> true } }
    val pages = rememberWatchListPages(visible.size, key = filter)
    val state = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(Unit) { manager.requeuePendingDownloads() }
    LaunchedEffect(pages.window.page, filter) { state.scrollToItem(0) }
    WatchScaffold("漫画缓存", onNavigateBack = { nav.popBackStack() }) {
        WatchLazyColumn(Modifier.fillMaxSize(), state = state, rotaryEnabled = !chooseFilter) {
            item { WatchActionRow("查看 · ${visible.size} 部", when(filter) { "complete" -> "已完成"; "active" -> "进行中与暂停"; "error" -> "失败"; else -> "全部缓存" }) { chooseFilter = true } }
            if (visible.isEmpty()) item { Text("暂无缓存。从漫画详情页选择章节，保存后即可离线阅读。", style = MaterialTheme.typography.bodySmall) }
            if (pages.enabled) item { WatchPagination(pages.window, onPrevious = pages.previous, onNext = pages.next) }
            items(visible.subList(pages.window.start, pages.window.end), key = { it.id }) { group ->
                Surface(onClick = { nav.navigate("downloadComicDetail/${group.id}") }, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LocalCacheCover(group.coverPath, Modifier.size(42.dp, 56.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(group.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("${group.chapterCount} 话", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    // Keep the status below the cover/title so it has the full dial width.
                    WatchCacheProgress(group.progress, group.status, group.progressMessage, speeds[group.id] ?: 0f)
                    }
                }
            }
            if (pages.enabled) item { WatchPagination(pages.window, onPrevious = pages.previous, onNext = pages.next) }
        }
    }
    if (chooseFilter) SelectDialog("缓存状态", filter,
        listOf(SelectOption("全部", "all"), SelectOption("已完成", "complete"), SelectOption("进行中与暂停", "active"), SelectOption("失败", "error")),
        onSelect = { filter = it; chooseFilter = false }, onDismissRequest = { chooseFilter = false })
}

@Composable
internal fun WatchCacheDetailScreen(
    id: Int,
    viewModel: DownloadComicDetailViewModel = koinViewModel(),
    manager: DownloadManager = getKoin().get(),
    historyManager: ReadHistoryManager = getKoin().get(),
    toastManager: ToastManager = getKoin().get(),
) {
    val nav = LocalMainNavController.current
    val detail by viewModel.detailState.collectAsStateWithLifecycle()
    val history by historyManager.readHistoryState.collectAsStateWithLifecycle()
    var actions by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val groupId by viewModel.groupId.collectAsStateWithLifecycle()
    val progressMessages by DownloadProgressMessageStore.messages.collectAsStateWithLifecycle()
    LaunchedEffect(id) { viewModel.load(id) }
    val ids = detail.allItems.map { it.id }
    val lastRead = history[groupId]?.lastChapterId
    val readable = detail.completeItems.firstOrNull { it.id == lastRead } ?: detail.completeItems.firstOrNull()
    WatchScaffold("缓存详情", onNavigateBack = { nav.popBackStack() }) {
        WatchLazyColumn(Modifier.fillMaxSize(), rotaryEnabled = !actions && !deleting) {
            if (detail.loading) item { CircularProgressIndicator(Modifier.size(24.dp)) }
            else if (!detail.found) item { Text("缓存不存在或已删除", style = MaterialTheme.typography.bodySmall) }
            else {
                item {
                    Text(detail.title, style = MaterialTheme.typography.titleSmall)
                    Text("${detail.completeChapterCount} / ${detail.totalChapterCount} 话已缓存", style = MaterialTheme.typography.labelMedium)
                    WatchCacheProgress(detail.groupProgress,
                        when {
                            detail.allItems.any { it.status == "downloading" } -> "downloading"
                            detail.allItems.any { it.status == "pending" } -> "pending"
                            detail.hasError -> "error"
                            detail.allItems.any { it.status == "paused" } -> "paused"
                            else -> "complete"
                        }, progressMessages[groupId].orEmpty(), detail.downloadSpeed)
                }
                item { WatchActionPair("离线阅读", "管理", firstEnabled = readable != null,
                    onFirst = { readable?.let { nav.navigate("localComicRead/${it.id}") } }, onSecond = { actions = true }) }
                items(detail.allItems, key = { it.id }) { chapter ->
                    WatchActionRow(chapter.chapterName.ifBlank { chapter.name },
                        "${cacheStatusLabel(chapter.status)} · ${cacheProgressPercent(chapter.progress, chapter.status == "complete")}%",
                        enabled = chapter.status == "complete") { nav.navigate("localComicRead/${chapter.id}") }
                }
            }
        }
    }
    if (actions) WatchPanelDialog("管理缓存", onDismiss = { actions = false }) {
        if (detail.isDownloading) item { WatchActionRow("暂停缓存") { manager.pauseDownloads(ids); actions = false } }
        if (detail.allItems.any { it.status == "paused" || it.status == "error" }) item {
            WatchActionRow("继续 / 重试未完成章节") {
                manager.resumeDownloads(detail.allItems.filter { it.status in setOf("paused", "error") }.map { it.id }); actions = false
            }
        }
        item { WatchActionRow("检查缺失图片并重下", enabled = detail.canRead && !detail.isDownloading) {
            manager.checkAndRepairGroup(viewModel.groupId.value); actions = false
        } }
        item { WatchActionRow("删除本地缓存", "${detail.totalChapterCount} 话") { actions = false; deleting = true } }
    }
    if (deleting) WatchPanelDialog("删除缓存？", onDismiss = { if (!busy) deleting = false }) {
        item { Text("将删除此漫画的本地图片与任务，收藏和阅读记录会保留。", style = MaterialTheme.typography.bodySmall) }
        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item { WatchActionPair("取消", if (busy) "删除中" else "删除", !busy, !busy,
            onFirst = { deleting = false }, onSecond = {
                busy = true
                scope.launch {
                    try {
                        val deleted = manager.deleteCachedItems(ids)
                        when {
                            deleted == ids.size -> {
                                deleting = false
                                nav.popBackStack()
                            }
                            deleted < 0 -> {
                                deleting = false
                                toastManager.showAsync("删除任务已停止，部分文件可能仍在清理")
                            }
                            else -> {
                                deleting = false
                                toastManager.showAsync("已删除 $deleted/${ids.size} 项，剩余文件可稍后重试")
                            }
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (failure: Exception) { error = failure.message ?: "删除失败" }
                    finally {
                        busy = false
                        deleting = false
                    }
                }
            }) }
    }
}

@Composable
internal fun WatchCacheChaptersDialog(comic: Comic, onDismiss: () -> Unit, onQueued: () -> Unit, manager: DownloadManager = getKoin().get()) {
    val chapters = remember(comic.id, comic.comicChapterList) { watchDetailChapters(comic) }
    var selected by rememberSaveable { mutableStateOf(listOf(chapters.first().id)) }
    WatchPanelDialog("缓存章节", onDismiss) {
        item {
            Button(onClick = { manager.downloadChapters(comic, chapters.filter { it.id in selected }); onQueued() },
                enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("缓存 ${selected.size} 话") }
        }
        item { Text("保存到本机，完成后无需网络。", style = MaterialTheme.typography.labelSmall) }
        if (chapters.size > 1) item { WatchActionPair("全选", "清空",
            onFirst = { selected = chapters.map { it.id } }, onSecond = { selected = emptyList() }) }
        items(chapters, key = { it.id }) { chapter ->
            WatchActionRow(chapter.name.ifBlank { "JM${chapter.id}" }, selected = chapter.id in selected) {
                selected = if (chapter.id in selected) selected - chapter.id else selected + chapter.id
            }
        }
    }
}

@Composable
private fun LocalCacheCover(path: String, modifier: Modifier) {
    Surface(modifier.clip(MaterialTheme.shapes.medium), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        if (path.isNotBlank()) AsyncImage(
            model = if (path.startsWith("content://")) Uri.parse(path) else File(path),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        ) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("缓存", style = MaterialTheme.typography.labelSmall) }
    }
}
