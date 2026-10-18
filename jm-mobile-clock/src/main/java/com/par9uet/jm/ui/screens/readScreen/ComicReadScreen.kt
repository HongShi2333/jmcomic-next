package com.par9uet.jm.ui.screens.readScreen

import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.ui.components.WatchLazyColumn

import androidx.compose.ui.focus.focusRequester

import androidx.compose.ui.input.rotary.onRotaryScrollEvent

import androidx.compose.foundation.gestures.scrollBy

import androidx.compose.foundation.focusable

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import com.par9uet.jm.ui.components.WatchAlertDialog as AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.components.adaptiveDialogMaxHeight
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicReadScreen(
    comicId: Int,
    localOnly: Boolean = false,
    comicReadViewModel: ComicReadViewModel = koinViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
    readHistoryManager: ReadHistoryManager = getKoin().get(),
    userManager: UserManager = getKoin().get(),
    onControlsVisibilityChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val mainNavController = LocalMainNavController.current
    val isShowToolbar by comicReadViewModel.isShowToolBar
    val size = comicReadViewModel.size
    var currentIndexState by comicReadViewModel.currentIndexState
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val comicDetailState by comicReadViewModel.comicDetailState.collectAsState()
    val localChapterList by comicReadViewModel.localChapterList.collectAsState()
    val readHistory by readHistoryManager.readHistoryState.collectAsState()
    val comic = comicDetailState.data
    val loading = comicPicState.isLoading
    val initialReaderIndex = if (size > 0) currentIndexState.coerceIn(0, size - 1) else 0
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialReaderIndex)
    val pagerState = rememberPagerState(initialPage = initialReaderIndex) { size }
    val zoomState = rememberReaderZoomState()
    val isRoundScreen = androidx.compose.ui.platform.LocalConfiguration.current.isScreenRound
    androidx.compose.runtime.SideEffect { zoomState.updateRoundScreen(isRoundScreen) }
    var targetIndex by remember { mutableIntStateOf(initialReaderIndex) }
    var activeDialog by remember { mutableStateOf<ReadPanelDialog?>(null) }
    var loadedComicId by comicReadViewModel.loadedComicId
    val readHistoryComicId by comicReadViewModel.readHistoryComicId
    val readChapterIds = remember(
        readHistory,
        readHistoryComicId,
        localSetting.chapterReadingMemoryEnabled,
    ) {
        if (localSetting.chapterReadingMemoryEnabled && readHistoryComicId > 0) {
            readHistoryManager.readChapterIds(readHistoryComicId, readHistory)
        } else {
            emptySet()
        }
    }
    val readableChapters = if (localOnly) {
        localChapterList
    } else {
        comic?.comicChapterList.orEmpty()
    }
    val chapterIndex = remember(readableChapters, comicId) {
        readableChapters.indexOfFirst { it.id == comicId }
    }
    val previousChapter = remember(readableChapters, chapterIndex) {
        readableChapters.getOrNull(chapterIndex - 1)
    }
    val nextChapter = remember(readableChapters, chapterIndex) {
        readableChapters.getOrNull(chapterIndex + 1)
    }

    fun navigateToChapter(chapter: ComicChapter?) {
        if (chapter == null) return
        val targetRoute = if (localOnly) {
            "localComicRead/${chapter.id}"
        } else {
            "comicRead/${chapter.id}"
        }
        val currentRoute = if (localOnly) "localComicRead/$comicId" else "comicRead/$comicId"
        mainNavController.navigate(targetRoute) {
            popUpTo(currentRoute) {
                inclusive = true
            }
        }
    }

    fun updateIndexFromReader(value: Float) {
        val target = value.toInt().coerceIn(0, maxOf(0, size - 1))
        if (target != currentIndexState) {
            zoomState.reset()
        }
        // Keep the toolbar and reading-memory state driven by one observable source.
        currentIndexState = target
    }

    fun jumpToIndex(index: Int) {
        if (size <= 0) return

        val target = index.coerceIn(0, size - 1)
        if (target == currentIndexState) return
        zoomState.reset()
        zoomState.endScrollFocus()
        currentIndexState = target
        targetIndex = target
        comicReadViewModel.decodeIndex(target, context)
        if (localSetting.readMode == "scroll" && isShowToolbar) {
            val image = comicPicState.data?.getOrNull(target)?.imageResultState as? com.par9uet.jm.data.models.ImageResultState.Success
            zoomState.prepareScrollPage(target, image?.decodeImageAspectRatio ?: 1f)
        }
        comicReadViewModel.showToolBar()
    }

    fun prepareImagePreview() {
        if (localSetting.readMode != "scroll" || zoomState.focusedPageIndex >= 0 || size <= 0) return
        val info = lazyListState.layoutInfo
        val visible = readerPreviewPage(info.visibleItemsInfo.map { ReaderVisiblePage(it.index, it.offset, it.size) },
            info.viewportStartOffset, info.viewportEndOffset)
        val selected = (visible?.index ?: currentIndexState).coerceIn(0, size - 1)
        val image = comicPicState.data?.getOrNull(selected)?.imageResultState as? com.par9uet.jm.data.models.ImageResultState.Success
        currentIndexState = selected
        zoomState.prepareScrollPage(selected, image?.decodeImageAspectRatio ?: 1f, visible?.top?.toFloat() ?: 0f)
        comicReadViewModel.decodeIndex(selected, context)
    }
    fun toggleReaderControls() {
        if (isShowToolbar) comicReadViewModel.hideToolBar()
        else { prepareImagePreview(); comicReadViewModel.showToolBar() }
    }
    LaunchedEffect(isShowToolbar, localSetting.readMode) {
        if (isShowToolbar) prepareImagePreview()
    }

    fun loadChapter() {
        val readingMemoryEnabled = localSetting.comicReadingMemoryEnabled
        val onSuccess: (Int) -> Unit = { savedIndex ->
            if (loadedComicId != comicId || !readingMemoryEnabled) {
                // Read the ViewModel state directly: composition can still hold
                // the old zero page count in the frame where data arrives.
                val restoredIndex = savedIndex.coerceIn(0, maxOf(0, comicReadViewModel.size - 1))
                currentIndexState = restoredIndex
                targetIndex = restoredIndex
                loadedComicId = comicId
            } else {
                targetIndex = currentIndexState.coerceAtLeast(0)
            }
            zoomState.reset()
            zoomState.endScrollFocus()
            comicReadViewModel.decodeIndex(targetIndex, context)
        }
        if (localOnly) {
            comicReadViewModel.clearComicDetail()
            comicReadViewModel.getLocalComicPicList(
                comicId = comicId,
                context = context,
                restoreReadingMemory = readingMemoryEnabled,
                onSuccess = onSuccess,
            )
        } else {
            comicReadViewModel.loadRemoteComicChapter(
                comicId = comicId,
                shunt = localSettingManager.localSettingState.value.shunt,
                restoreReadingMemory = readingMemoryEnabled,
                onSuccess = onSuccess,
            )
        }
    }

    LaunchedEffect(comicId) { loadChapter() }
    val zoomScope = androidx.compose.runtime.rememberCoroutineScope()
    val motionEnabled = com.par9uet.jm.ui.components.LocalWatchMotionEnabled.current
    val gravityManager: com.par9uet.jm.store.GravityReadingManager = getKoin().get()
    BackHandler(enabled = zoomState.isAdjusted && !isShowToolbar && activeDialog == null) { zoomScope.launch { zoomState.resetAnimated() } }

    // 退出阅读时保存当前页数进度
    val latestPageIndex by rememberUpdatedState(currentIndexState)
    val latestPageCount by rememberUpdatedState(size)
    val latestHistoryComicId by rememberUpdatedState(readHistoryComicId)
    val latestReadingMemoryEnabled by rememberUpdatedState(localSetting.comicReadingMemoryEnabled)
    DisposableEffect(comicId) {
        onDispose {
            if (latestReadingMemoryEnabled && latestPageCount > 0 && latestHistoryComicId > 0) {
                readHistoryManager.saveReadProgress(
                    latestHistoryComicId,
                    comicId,
                    latestPageIndex,
                    latestPageCount
                )
            }
        }
    }

    // 数据加载完成后，滚动到恢复的页码
    LaunchedEffect(size, loadedComicId) {
        if (size > 0 && loadedComicId == comicId && targetIndex in 0 until size) {
            if (localSetting.readMode == "scroll") {
                lazyListState.scrollToItem(targetIndex)
            } else {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    val view = LocalView.current
    val controller = remember(view) {
        val window = (context as? Activity)?.window
        if (window == null) {
            null
        } else {
            WindowInsetsControllerCompat(window, view).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
    LaunchedEffect(controller) { controller?.hide(WindowInsetsCompat.Type.statusBars()) }
    DisposableEffect(Unit) {
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val rotaryRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val rotaryScope = androidx.compose.runtime.rememberCoroutineScope()
    var rotaryDistance by remember { mutableStateOf(0f) }
    val readerReady = !loading && size > 0
    val needsTip = (localSetting.showComicPageReadTip && localSetting.readMode == "page") ||
        (localSetting.showComicScrollReadTip && localSetting.readMode == "scroll")
    val controlsCallback by rememberUpdatedState(onControlsVisibilityChanged)
    DisposableEffect(isShowToolbar, activeDialog, needsTip) {
        controlsCallback(isShowToolbar || activeDialog != null || needsTip)
        onDispose { controlsCallback(false) }
    }
    val currentImageReady = comicPicState.data?.getOrNull(currentIndexState)?.imageResultState is com.par9uet.jm.data.models.ImageResultState.Success
    GravityReaderEffect(
        enabled = localSetting.gravityPageTurningEnabled && readerReady && currentImageReady &&
            com.par9uet.jm.ui.components.LocalWatchListActive.current && !isShowToolbar &&
            activeDialog == null && !needsTip && !zoomState.isAdjusted && zoomState.focusedPageIndex < 0,
        sensitivity = localSetting.gravityPageSensitivity,
        scrollMode = localSetting.readMode == "scroll",
        listState = lazyListState,
        manager = gravityManager,
        onTurnPage = { delta ->
            if (delta != 0 && size > 0) {
                val next = (currentIndexState + delta).coerceIn(0, size - 1)
                if (next != currentIndexState) {
                    zoomState.reset()
                    currentIndexState = next
                    targetIndex = next
                    comicReadViewModel.decodeIndex(next, context)
                }
            }
        },
    )
    LaunchedEffect(isShowToolbar, activeDialog, readerReady) { if (readerReady && !isShowToolbar && activeDialog == null) runCatching { rotaryRequester.requestFocus() } }
    Box(modifier = Modifier.fillMaxSize().pointerInput(gravityManager) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed || it.previousPressed }) gravityManager.pause()
            }
        }
    }.onSizeChanged { zoomState.updateViewportSize(it.width, it.height) }.onRotaryScrollEvent { event ->
        gravityManager.pause()
        if (isShowToolbar || activeDialog != null || size <= 0) false else {
            if (zoomState.isZoomed || localSetting.readMode != "scroll" && zoomState.canPanVertically) {
                zoomState.pan(Offset(0f, -event.verticalScrollPixels))
            } else if (localSetting.readMode == "scroll") rotaryScope.launch { lazyListState.scrollBy(event.verticalScrollPixels) }
            else {
                rotaryDistance += event.verticalScrollPixels
                if (kotlin.math.abs(rotaryDistance) >= 60f) {
                    jumpToIndex(currentIndexState + if (rotaryDistance > 0) 1 else -1)
                    comicReadViewModel.hideToolBar(); rotaryDistance = 0f
                }
            }
            true
        }
    }.focusRequester(rotaryRequester).focusable(readerReady && !isShowToolbar && activeDialog == null)) {
        if (loading || size <= 0) {
            WatchScaffold(title = "漫画阅读", onNavigateBack = { mainNavController.popBackStack() }) {
                WatchLazyColumn(Modifier.fillMaxSize()) {
                    item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (loading) CircularProgressIndicator(Modifier.size(24.dp))
                        Text(if (loading) "正在获取章节…" else comicPicState.errorMsg.orEmpty().ifBlank { "暂无可阅读图片" },
                            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } }
                    item { TextButton(onClick = ::loadChapter, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (loading) "重新请求" else "重试本章") } }
                    item { TextButton(onClick = { mainNavController.popBackStack() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("返回详情") } }
                }
            }
        } else {
            if (localSetting.readMode == "scroll") {
                ComicScrollRead(
                    lazyListState = lazyListState,
                    pagerState = pagerState,
                    targetIndex = targetIndex,
                    zoomState = zoomState,
                    comicReadViewModel = comicReadViewModel,
                    controlsVisible = isShowToolbar || activeDialog != null,
                    onToggleControls = ::toggleReaderControls,
                    onUpdateSliderValue = { updateIndexFromReader(it) }
                )
            } else {
                ComicPageRead(
                    lazyListState = lazyListState,
                    pagerState = pagerState,
                    targetIndex = targetIndex,
                    zoomState = zoomState,
                    comicReadViewModel = comicReadViewModel,
                    tapOnly = localSetting.readMode == "tap",
                    controlsVisible = isShowToolbar || activeDialog != null,
                    onToggleControls = ::toggleReaderControls,
                    onUpdateSliderValue = { updateIndexFromReader(it) }
                )
            }
            fun closeTip() {
                if (localSetting.readMode == "scroll") localSettingManager.closeShowComicScrollReadTip()
                else localSettingManager.closeShowComicPageReadTip()
                comicReadViewModel.hideToolBar()
            }
            if (needsTip) AlertDialog(onDismissRequest = ::closeTip, title = { Text("阅读操作") }, text = {
                Text(
                    (if (localSetting.readMode == "scroll") "上下滑动阅读。" else if (localSetting.readMode == "tap") "轻点左右两侧翻页。" else "左右滑动翻页。") +
                        "轻点画面显示工具，下方中键切换翻页或缩放，调整时漫画始终可见。点顶部页码可跳页、切换适宽或全图；双击图片也可放大，拖动查看细节。"
                )
            }, confirmButton = { TextButton(onClick = ::closeTip, modifier = Modifier.fillMaxWidth()) { Text("开始阅读") } })
            else if (isShowToolbar && activeDialog == null) {
                WatchReaderControlsOverlay(
                    currentIndex = currentIndexState, pageCount = size, scale = zoomState.scale,
                    minimumScale = zoomState.minimumScale, imageReady = currentImageReady,
                    chapterTitle = readableChapters.getOrNull(chapterIndex)?.name.orEmpty(),
                    previousChapterEnabled = previousChapter != null, nextChapterEnabled = nextChapter != null,
                    chapterListEnabled = readableChapters.isNotEmpty(),
                    collectEnabled = !localOnly && comic != null, collected = comic?.isCollect == true,
                    commentsEnabled = !localOnly,
                    onResume = comicReadViewModel::hideToolBar,
                    onPageSelected = { jumpToIndex(it) },
                    onPreviousChapter = { comicReadViewModel.hideToolBar(); navigateToChapter(previousChapter) },
                    onNextChapter = { comicReadViewModel.hideToolBar(); navigateToChapter(nextChapter) },
                    onOpenChapters = { activeDialog = ReadPanelDialog.Chapter },
                    onScaleChange = { prepareImagePreview(); zoomState.setScale(it) },
                    onZoomStep = { factor ->
                        prepareImagePreview()
                        if (motionEnabled) zoomScope.launch { zoomState.zoomByAnimated(factor) }
                        else zoomState.setScale(zoomState.scale * factor)
                    },
                    onFitWidth = {
                        prepareImagePreview()
                        if (motionEnabled) zoomScope.launch { zoomState.resetAnimated() } else zoomState.reset()
                    },
                    onFitPage = { prepareImagePreview(); zoomState.fitPage() },
                    onToggleCollect = {
                        if (!isLogin) { comicReadViewModel.hideToolBar(); mainNavController.navigate("login") }
                        else comic?.let { if (it.isCollect) comicReadViewModel.unCollect(it.id) else comicReadViewModel.collect(it.id) }
                    },
                    onComments = { comicReadViewModel.hideToolBar(); mainNavController.navigate(if (isLogin) "comment/$comicId" else "login") },
                    onSettings = { comicReadViewModel.hideToolBar(); mainNavController.navigate("appLocalSetting") },
                    onExit = { comicReadViewModel.hideToolBar(); mainNavController.popBackStack() },
                )
            }

        }
    }

    when (activeDialog) {
        ReadPanelDialog.Chapter -> {
            if (readableChapters.isNotEmpty()) {
                ChapterPickerDialog(
                    title = "跳转章节",
                    chapters = readableChapters,
                    currentChapterId = comicId,
                    readChapterIds = readChapterIds,
                    onDismiss = { activeDialog = null },
                    onSelect = { chapter ->
                        activeDialog = null
                        comicReadViewModel.hideToolBar()
                        navigateToChapter(chapter)
                    }
                )
            }
        }

        null -> Unit
    }
}

private enum class ReadPanelDialog {
    Chapter
}

@Composable
private fun ChapterPickerDialog(
    title: String,
    chapters: List<ComicChapter>,
    currentChapterId: Int?,
    readChapterIds: Set<Int>,
    onDismiss: () -> Unit,
    onSelect: (ComicChapter) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentIndex = remember(chapters, currentChapterId) {
        chapters.indexOfFirst { it.id == currentChapterId }
    }
    LaunchedEffect(currentIndex, chapters.size) {
        if (currentIndex >= 0) {
            listState.scrollToItem(currentIndex)
        }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        com.par9uet.jm.ui.components.WatchScaffold(title, onNavigateBack = onDismiss) {
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                if (chapters.isEmpty()) item { Text("暂无章节") }
                itemsIndexed(chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                    val selected = chapter.id == currentChapterId
                    Surface(onClick = { onSelect(chapter) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.large,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.padding(8.dp)) {
                            Text(chapter.name.ifBlank { "第 ${index + 1} 话" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (selected || chapter.id in readChapterIds) Text(if (selected) "当前阅读" else "已读", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
