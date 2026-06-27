package com.par9uet.jm.ui.screens.readScreen

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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Download
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
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ReadHistoryManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.screens.LocalMainNavController
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
    downloadManager: DownloadManager = getKoin().get(),
    userManager: UserManager = getKoin().get()
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
    var targetIndex by remember { mutableIntStateOf(initialReaderIndex) }
    var activeDialog by remember { mutableStateOf<ReadPanelDialog?>(null) }
    var selectedCacheChapterIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var loadedComicId by comicReadViewModel.loadedComicId
    val readHistoryComicId by comicReadViewModel.readHistoryComicId
    val readChapterIds = remember(readHistory, readHistoryComicId) {
        if (readHistoryComicId > 0) {
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
            currentIndexState = target
        }
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

    LaunchedEffect(comicId) {
        val onSuccess = {
            if (loadedComicId != comicId) {
                currentIndexState = 0
                targetIndex = 0
                loadedComicId = comicId
            } else {
                targetIndex = currentIndexState.coerceAtLeast(0)
            }
            zoomState.reset()
            comicReadViewModel.decodeIndex(targetIndex, context)
        }
        if (localOnly) {
            comicReadViewModel.clearComicDetail()
            comicReadViewModel.getLocalComicPicList(comicId, context, onSuccess)
        } else {
            comicReadViewModel.getComicDetail(comicId)
            comicReadViewModel.getComicPicList(
                comicId,
                localSettingManager.localSettingState.value.shunt,
                onSuccess
            )
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
            controller?.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (size <= 0) {
            Text(
                text = comicPicState.errorMsg.orEmpty().ifBlank { "暂无可阅读图片" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            if (localSetting.readMode == "scroll") {
                ComicScrollRead(
                    lazyListState = lazyListState,
                    pagerState = pagerState,
                    targetIndex = targetIndex,
                    zoomState = zoomState,
                    onUpdateSliderValue = { updateIndexFromReader(it) }
                )
            } else {
                ComicPageRead(
                    lazyListState = lazyListState,
                    pagerState = pagerState,
                    targetIndex = targetIndex,
                    zoomState = zoomState,
                    tapOnly = localSetting.readMode == "tap",
                    onUpdateSliderValue = { updateIndexFromReader(it) }
                )
            }
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp),
                visible = isShowToolbar,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeOut()
            ) {
                ReadSideBar(
                    comic = comic,
                    localOnly = localOnly,
                    chapterEnabled = readableChapters.isNotEmpty(),
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
                    onCache = {
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
                    onComment = {
                        if (!isLogin) {
                            mainNavController.navigate("login")
                        } else {
                            mainNavController.navigate("comment/$comicId")
                        }
                    },
                    onChapterJump = {
                        if (readableChapters.isNotEmpty()) {
                            activeDialog = ReadPanelDialog.Chapter
                        }
                    }
                )
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = isShowToolbar,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut()
            ) {
                ToolsBar(
                    currentIndex = currentIndexState,
                    pageCount = size,
                    previousChapterEnabled = previousChapter != null,
                    nextChapterEnabled = nextChapter != null,
                    showResetZoom = zoomState.isZoomed,
                    onPreviousChapter = { navigateToChapter(previousChapter) },
                    onNextChapter = { navigateToChapter(nextChapter) },
                    onPageSelected = { jumpToIndex(it) },
                    onResetZoom = { zoomState.reset() }
                )
            }
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp),
                visible = isShowToolbar,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 250)
                ) + fadeOut()
            ) {
                ReaderExitButton(
                    onClick = { mainNavController.popBackStack() }
                )
            }
            if (localSetting.showComicPageReadTip && localSetting.readMode == "page" || localSetting.showComicScrollReadTip && localSetting.readMode == "scroll") {
                Tip(readMode = localSetting.readMode)
                TipCloseButton(
                    modifier = Modifier.align(
                        if (localSetting.readMode == "scroll") Alignment.CenterEnd else Alignment.BottomCenter
                    ).let {
                        if (localSetting.readMode == "scroll") {
                            it.padding(end = 40.dp)
                        } else {
                            it.padding(bottom = 40.dp)
                        }
                    },
                    onClick = {
                        if (localSetting.readMode == "scroll") {
                            localSettingManager.closeShowComicScrollReadTip()
                        } else {
                            localSettingManager.closeShowComicPageReadTip()
                        }
                    }
                )
            }
        }
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
                    title = "跳转章节",
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
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
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
private fun ReadSideBar(
    comic: Comic?,
    localOnly: Boolean,
    chapterEnabled: Boolean,
    onToggleCollect: () -> Unit,
    onCache: () -> Unit,
    onComment: () -> Unit,
    onChapterJump: () -> Unit,
) {
    Surface(
        modifier = Modifier.shadow(14.dp, RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 10.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .width(82.dp)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReadSideBarAction(
                icon = if (comic?.isCollect == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = "收藏",
                enabled = !localOnly && comic != null,
                onClick = onToggleCollect
            )
            ReadSideBarAction(
                icon = Icons.Default.Download,
                label = "缓存",
                enabled = !localOnly && comic != null,
                onClick = onCache
            )
            if (!localOnly) {
                ReadSideBarAction(
                    icon = Icons.AutoMirrored.Outlined.Message,
                    label = "评论",
                    enabled = comic != null,
                    onClick = onComment
                )
            }
            ReadChapterSideBarAction(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "章节",
                enabled = chapterEnabled,
                onClick = onChapterJump
            )
        }
    }
}

@Composable
private fun ReaderExitButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "退出阅读"
            )
        }
    }
}

@Composable
private fun ReadSideBarAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = contentColor
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReadChapterSideBarAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        tonalElevation = if (enabled) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(23.dp),
                tint = contentColor
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            if (chapters.isEmpty()) {
                Text(text = "暂无可选章节")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
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

