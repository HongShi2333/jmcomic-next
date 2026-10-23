package com.par9uet.jm.ui.screens.readScreen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.data.models.READER_BACKGROUND_THEME
import com.par9uet.jm.data.models.READER_BACKGROUND_WHITE
import com.par9uet.jm.data.models.READ_MODE_HORIZONTAL
import com.par9uet.jm.data.models.READ_MODE_RTL
import com.par9uet.jm.data.models.READ_MODE_VERTICAL
import com.par9uet.jm.data.models.READ_MODE_WEBTOON
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.components.adaptiveDialogMaxHeight
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import com.par9uet.jm.utils.VolumeKeyPageTurnDispatcher
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
    downloadManager: DownloadManager = getKoin().get(),
    userManager: UserManager = getKoin().get(),
) {
    val context = LocalContext.current
    val mainNavController = LocalMainNavController.current
    val isShowToolbar by comicReadViewModel.isShowToolBar
    val size = comicReadViewModel.size
    var currentIndexState by comicReadViewModel.currentIndexState
    val localSetting by localSettingManager.localSettingState.collectAsState()
    var brightnessOverride by rememberSaveable { mutableIntStateOf(0) }
    val systemDark = isSystemInDarkTheme()
    val brightnessPercent = readerBrightnessPercent(
        localSetting.readerBrightnessEnabled, systemDark,
        localSetting.readerLightBrightnessPercent, localSetting.readerDarkBrightnessPercent,
        brightnessOverride,
    )
    val dimmingAlpha = readerDimmingAlpha(
        localSetting.readerBrightnessEnabled,
        systemDark,
        localSetting.readerLightBrightnessPercent,
        localSetting.readerDarkBrightnessPercent,
        brightnessOverride,
    )
    val isLogin by userManager.isLoginState.collectAsState(false)
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val comicDetailState by comicReadViewModel.comicDetailState.collectAsState()
    val localChapterList by comicReadViewModel.localChapterList.collectAsState()
    val readHistory by readHistoryManager.readHistoryState.collectAsState()
    val comic = comicDetailState.data
    val loading = comicPicState.isLoading
    val readMode = localSetting.readMode
    val isContinuous = readMode == READ_MODE_WEBTOON || readMode == READ_MODE_HORIZONTAL
    val zoomState = rememberReaderZoomState()
    var targetIndex by remember { mutableIntStateOf(0) }
    var activeDialog by remember { mutableStateOf<ReadPanelDialog?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedCacheChapterIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var loadedComicId by comicReadViewModel.loadedComicId
    val readHistoryComicId by comicReadViewModel.readHistoryComicId

    val readerBackgroundColor = when (localSetting.readerBackground) {
        READER_BACKGROUND_WHITE -> Color.White
        READER_BACKGROUND_THEME -> MaterialTheme.colorScheme.background
        else -> Color.Black
    }
    val readerContentColor = when (localSetting.readerBackground) {
        READER_BACKGROUND_WHITE -> Color.Black
        READER_BACKGROUND_THEME -> MaterialTheme.colorScheme.onBackground
        else -> Color.White
    }

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
    val currentChapter = readableChapters.getOrNull(chapterIndex)
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

    fun updateIndexFromReader(index: Int) {
        val target = index.coerceIn(0, maxOf(0, size - 1))
        if (!isContinuous && target != currentIndexState) {
            zoomState.reset()
        }
        currentIndexState = target
    }

    fun jumpToIndex(index: Int) {
        if (size <= 0) return

        val target = index.coerceIn(0, size - 1)
        zoomState.reset()
        currentIndexState = target
        targetIndex = target
        comicReadViewModel.decodeIndex(target, context)
        comicReadViewModel.showToolBar()
    }

    val volumePageTurnHandler = rememberUpdatedState<(Boolean) -> Unit> { isVolumeUp ->
        val currentIndex = comicReadViewModel.currentIndexState.intValue
        jumpToIndex(if (isVolumeUp) currentIndex - 1 else currentIndex + 1)
    }
    DisposableEffect(comicId, localOnly, localSetting.volumeKeyPageTurningEnabled) {
        if (!localSetting.volumeKeyPageTurningEnabled) {
            onDispose { }
        } else {
            val unregister = VolumeKeyPageTurnDispatcher.register { isVolumeUp ->
                volumePageTurnHandler.value(isVolumeUp)
            }
            onDispose { unregister() }
        }
    }

    LaunchedEffect(comicId) {
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

    // 切换阅读模式时保持当前页，并还原缩放
    LaunchedEffect(readMode) {
        zoomState.reset()
        if (size > 0) {
            targetIndex = currentIndexState.coerceIn(0, size - 1)
        }
    }

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
    LaunchedEffect(isShowToolbar) {
        if (isShowToolbar) {
            // 阅读器顶/底栏始终是深色，系统栏图标需要保持浅色。
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    val darkSystemBarIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(Unit) {
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            controller?.isAppearanceLightStatusBars = darkSystemBarIcons
            controller?.isAppearanceLightNavigationBars = darkSystemBarIcons
        }
    }

    val initialIndex = if (size > 0) currentIndexState.coerceIn(0, size - 1) else 0
    val readerTitle = if (localOnly) {
        "本地阅读"
    } else {
        comic?.name.orEmpty().ifBlank { "阅读" }
    }
    val chapterLabel = currentChapter?.name.orEmpty().ifBlank {
        if (chapterIndex >= 0) "第 ${chapterIndex + 1} 话" else ""
    }
    val subtitle = if (localOnly) {
        chapterLabel.ifBlank { "本地缓存" }
    } else {
        chapterLabel
    }
    val doubleTapZoomEnabled = localSetting.readerDoubleTapZoomEnabled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBackgroundColor)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = readerContentColor.copy(alpha = 0.7f),
            )
        } else if (size <= 0) {
            Text(
                text = comicPicState.errorMsg.orEmpty().ifBlank { "暂无可阅读图片" },
                color = readerContentColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        if (dimmingAlpha > 0f) {
                            drawRect(Color.Black.copy(alpha = dimmingAlpha))
                        }
                    }
            ) {
                when (readMode) {
                    READ_MODE_WEBTOON -> ComicContinuousRead(
                        horizontal = false,
                        initialIndex = initialIndex,
                        targetIndex = targetIndex,
                        zoomState = zoomState,
                        comicReadViewModel = comicReadViewModel,
                        doubleTapZoomEnabled = doubleTapZoomEnabled,
                        onIndexChange = ::updateIndexFromReader,
                    )

                    READ_MODE_HORIZONTAL -> ComicContinuousRead(
                        horizontal = true,
                        initialIndex = initialIndex,
                        targetIndex = targetIndex,
                        zoomState = zoomState,
                        comicReadViewModel = comicReadViewModel,
                        doubleTapZoomEnabled = false,
                        onIndexChange = ::updateIndexFromReader,
                    )

                    READ_MODE_VERTICAL -> ComicPageRead(
                        initialIndex = initialIndex,
                        targetIndex = targetIndex,
                        direction = ReaderPageDirection.Vertical,
                        zoomState = zoomState,
                        comicReadViewModel = comicReadViewModel,
                        doubleTapZoomEnabled = doubleTapZoomEnabled,
                        onIndexChange = ::updateIndexFromReader,
                    )

                    READ_MODE_RTL -> ComicPageRead(
                        initialIndex = initialIndex,
                        targetIndex = targetIndex,
                        direction = ReaderPageDirection.RightToLeft,
                        zoomState = zoomState,
                        comicReadViewModel = comicReadViewModel,
                        doubleTapZoomEnabled = doubleTapZoomEnabled,
                        onIndexChange = ::updateIndexFromReader,
                    )

                    else -> ComicPageRead(
                        initialIndex = initialIndex,
                        targetIndex = targetIndex,
                        direction = ReaderPageDirection.LeftToRight,
                        zoomState = zoomState,
                        comicReadViewModel = comicReadViewModel,
                        doubleTapZoomEnabled = doubleTapZoomEnabled,
                        onIndexChange = ::updateIndexFromReader,
                    )
                }
            }
            if (isShowToolbar && localSetting.readerShowTapZones) {
                ReaderTapZoneHints(rightToLeft = readMode == READ_MODE_RTL)
            }
            if (!isShowToolbar && localSetting.readerShowPageNumber) {
                ReaderPageIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                    currentIndex = currentIndexState,
                    pageCount = size,
                )
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.TopCenter),
                visible = isShowToolbar,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> -fullHeight },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> -fullHeight },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeOut()
            ) {
                ReaderTopBar(
                    title = readerTitle,
                    subtitle = subtitle,
                    isCollected = comic?.isCollect == true,
                    canCollect = !localOnly && comic != null,
                    canComment = !localOnly,
                    canDownload = !localOnly && comic != null,
                    onBack = { mainNavController.popBackStack() },
                    onToggleCollect = {
                        if (!isLogin) {
                            mainNavController.navigate("login")
                        } else {
                            comic?.let { currentComic ->
                                if (currentComic.isCollect) {
                                    comicReadViewModel.unCollect(currentComic.id)
                                } else {
                                    comicReadViewModel.collect(currentComic.id)
                                }
                            }
                        }
                    },
                    onComment = {
                        if (!isLogin) {
                            mainNavController.navigate("login")
                        } else {
                            mainNavController.navigate("comment/$comicId")
                        }
                    },
                    onDownload = {
                        comic?.let { currentComic ->
                            if (currentComic.comicChapterList.isEmpty()) {
                                downloadManager.downloadComic(currentComic)
                            } else {
                                selectedCacheChapterIds =
                                    currentComic.comicChapterList.map { it.id }.toSet()
                                activeDialog = ReadPanelDialog.Cache
                            }
                        }
                    },
                    onChapterList = {
                        if (readableChapters.isNotEmpty()) {
                            activeDialog = ReadPanelDialog.Chapter
                        }
                    },
                )
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = isShowToolbar,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeOut()
            ) {
                ReaderBottomBar(
                    currentIndex = currentIndexState,
                    pageCount = size,
                    readingMode = readMode,
                    previousChapterEnabled = previousChapter != null,
                    nextChapterEnabled = nextChapter != null,
                    zoomed = zoomState.isZoomed,
                    onPreviousChapter = { navigateToChapter(previousChapter) },
                    onNextChapter = { navigateToChapter(nextChapter) },
                    onPageSelected = { jumpToIndex(it) },
                    onOpenChapters = {
                        if (readableChapters.isNotEmpty()) {
                            activeDialog = ReadPanelDialog.Chapter
                        }
                    },
                    onOpenReadingMode = { showSettings = true },
                    onOpenBrightness = { showSettings = true },
                    onOpenSettings = { showSettings = true },
                    onResetZoom = { zoomState.reset() },
                )
            }
        }
        if (loading || size <= 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = ReaderBarColor,
                contentColor = ReaderBarContentColor,
            ) {
                IconButton(onClick = { mainNavController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "退出阅读",
                    )
                }
            }
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            readMode = readMode,
            readerBackground = localSetting.readerBackground,
            brightnessPercent = brightnessPercent,
            brightnessOverridden = brightnessOverride > 0,
            showTapZones = localSetting.readerShowTapZones,
            doubleTapZoomEnabled = localSetting.readerDoubleTapZoomEnabled,
            showPageNumber = localSetting.readerShowPageNumber,
            volumeKeyPageTurningEnabled = localSetting.volumeKeyPageTurningEnabled,
            onReadModeChange = localSettingManager::updateReadMode,
            onBackgroundChange = localSettingManager::updateReaderBackground,
            onBrightnessChange = { brightnessOverride = it.coerceIn(1, 100) },
            onRestoreBrightness = { brightnessOverride = 0 },
            onShowTapZonesChange = localSettingManager::updateReaderShowTapZones,
            onDoubleTapZoomChange = localSettingManager::updateReaderDoubleTapZoomEnabled,
            onShowPageNumberChange = localSettingManager::updateReaderShowPageNumber,
            onVolumeKeyPageTurningChange = localSettingManager::updateVolumeKeyPageTurningEnabled,
            onDismiss = { showSettings = false },
        )
    }

    when (activeDialog) {
        ReadPanelDialog.Cache -> {
            val currentComic = comic
            if (currentComic != null) {
                ChapterCachePickerDialog(
                    chapters = currentComic.comicChapterList,
                    selectedChapterIds = selectedCacheChapterIds,
                    onSelectedChange = { selectedCacheChapterIds = it },
                    onDismiss = { activeDialog = null },
                    onConfirm = {
                        val selectedChapters = currentComic.comicChapterList
                            .filter { it.id in selectedCacheChapterIds }
                        downloadManager.downloadChapters(currentComic, selectedChapters)
                        activeDialog = null
                        selectedCacheChapterIds = emptySet()
                    }
                )
            }
        }

        ReadPanelDialog.Chapter -> {
            if (readableChapters.isNotEmpty()) {
                ChapterPickerDialog(
                    title = "章节列表",
                    chapters = readableChapters,
                    currentChapterId = comicId,
                    readChapterIds = readChapterIds,
                    onDismiss = { activeDialog = null },
                    onSelect = { chapter ->
                        activeDialog = null
                        navigateToChapter(chapter)
                    }
                )
            }
        }

        null -> Unit
    }
}

private enum class ReadPanelDialog {
    Cache,
    Chapter
}

@Composable
private fun ChapterCachePickerDialog(
    chapters: List<ComicChapter>,
    selectedChapterIds: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val allChapterIds = remember(chapters) { chapters.map { it.id }.toSet() }
    val allSelected = chapters.isNotEmpty() && selectedChapterIds.containsAll(allChapterIds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "选择缓存章节") },
        text = {
            if (chapters.isEmpty()) {
                Text(text = "暂无可选章节")
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectedChange(if (allSelected) emptySet() else allChapterIds)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked ->
                                onSelectedChange(if (checked) allChapterIds else emptySet())
                            }
                        )
                        Text(text = if (allSelected) "取消全选" else "全选")
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = adaptiveDialogMaxHeight())) {
                        itemsIndexed(chapters) { index, chapter ->
                            val selected = chapter.id in selectedChapterIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectedChange(
                                            if (selected) {
                                                selectedChapterIds - chapter.id
                                            } else {
                                                selectedChapterIds + chapter.id
                                            }
                                        )
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { checked ->
                                        onSelectedChange(
                                            if (checked) {
                                                selectedChapterIds + chapter.id
                                            } else {
                                                selectedChapterIds - chapter.id
                                            }
                                        )
                                    }
                                )
                                Text(
                                    text = chapter.name.ifBlank { "第 ${index + 1} 章" },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedChapterIds.isNotEmpty()
            ) {
                Text(text = "开始缓存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            if (chapters.isEmpty()) {
                Text(text = "暂无可选章节")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = adaptiveDialogMaxHeight())
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        val selected = chapter.id == currentChapterId
                        val read = chapter.id in readChapterIds
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(chapter) }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else if (read) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else if (read) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            tonalElevation = if (selected) 3.dp else 0.dp
                        ) {
                            Text(
                                text = chapter.name.ifBlank { "第 ${index + 1} 章" },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}
