package com.par9uet.jm.clock.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.par9uet.jm.clock.R
import com.par9uet.jm.clock.data.LoadState
import com.par9uet.jm.clock.data.WatchChapterPages
import com.par9uet.jm.clock.data.WatchPreferences
import com.par9uet.jm.clock.data.WatchRepository
import com.par9uet.jm.clock.ui.LocalWatchUiScale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    chapterId: Int,
    repository: WatchRepository,
    preferences: WatchPreferences,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val view = LocalView.current
    DisposableEffect(Unit) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }

    var reloadKey by remember { mutableIntStateOf(0) }
    var pagesState by remember(chapterId) {
        mutableStateOf<LoadState<WatchChapterPages>>(LoadState.Loading)
    }
    LaunchedEffect(chapterId, reloadKey) {
        pagesState = LoadState.Loading
        val result = repository.chapterPages(chapterId)
        pagesState = result.fold(
            onSuccess = { LoadState.Content(it) },
            onFailure = { LoadState.Error(it.message ?: "章节加载失败") },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        when (val current = pagesState) {
            LoadState.Idle, LoadState.Loading -> ReaderCenteredMessage {
                CircularProgressIndicator(color = Color.White)
                Text("正在读取章节…", color = Color.White)
            }
            is LoadState.Error -> ReaderCenteredMessage {
                Text(current.message, color = Color.White)
                Button(onClick = { reloadKey += 1 }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("重试")
                }
                FilledTonalButton(onClick = onBack) { Text("返回") }
            }
            is LoadState.Content -> ReaderPage(
                pages = current.value,
                repository = repository,
                preferences = preferences,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun ReaderPage(
    pages: WatchChapterPages,
    repository: WatchRepository,
    preferences: WatchPreferences,
    onBack: () -> Unit,
) {
    val savedPageIndex = remember(pages.chapterId, pages.urls.size) {
        preferences.readerPosition(pages.chapterId).coerceIn(0, pages.urls.lastIndex)
    }
    var pageIndex by remember(pages.chapterId) { mutableIntStateOf(savedPageIndex) }
    var pageReloadKey by remember(pages.chapterId) { mutableIntStateOf(0) }
    var pageBitmap by remember(pages.chapterId) { mutableStateOf<Bitmap?>(null) }
    var pageLoading by remember(pages.chapterId) { mutableStateOf(true) }
    var pageError by remember(pages.chapterId) { mutableStateOf<String?>(null) }
    var zoom by remember(pages.chapterId, pageIndex) { mutableFloatStateOf(1f) }
    var showControls by rememberSaveable(pages.chapterId) { mutableStateOf(true) }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val uiScale = LocalWatchUiScale.current
    val readerHorizontalInset = dimensionResource(R.dimen.watch_content_horizontal_padding)

    fun previousPage() {
        if (pageIndex > 0) pageIndex -= 1
    }

    fun nextPage() {
        if (pageIndex < pages.urls.lastIndex) pageIndex += 1
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onRotaryScrollEvent { event ->
                scope.launch {
                    val atTop = verticalScroll.value <= 1
                    val atBottom = verticalScroll.value >= verticalScroll.maxValue - 1
                    when {
                        event.verticalScrollPixels > 0 && atBottom && pageIndex < pages.urls.lastIndex -> nextPage()
                        event.verticalScrollPixels < 0 && atTop && pageIndex > 0 -> previousPage()
                        else -> verticalScroll.scrollBy(event.verticalScrollPixels)
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        val compactControls = maxWidth * uiScale < 200.dp
        val readerTopInset = if (showControls) (64f / uiScale).dp else 0.dp
        val readerBottomInset = if (!showControls) {
            0.dp
        } else if (compactControls) {
            (120f / uiScale).dp
        } else {
            (64f / uiScale).dp
        }
        val screenWidthPx = with(density) { maxWidth.roundToPx() }
        val targetWidthPx = (screenWidthPx * 2).coerceAtLeast(480)
        val imageWidth = maxWidth * zoom

        androidx.compose.runtime.LaunchedEffect(pages.chapterId, pageIndex) {
            preferences.saveReaderPosition(pages.chapterId, pageIndex)
        }

        LaunchedEffect(pageIndex, pageReloadKey, pages.urls) {
            pageBitmap = null
            pageLoading = true
            pageError = null
            zoom = 1f
            verticalScroll.scrollTo(0)
            horizontalScroll.scrollTo(0)
            val result = repository.loadPage(
                chapterId = pages.chapterId,
                pageUrl = pages.urls[pageIndex],
                scrambleId = pages.scrambleId,
                targetWidthPx = targetWidthPx,
            )
            val loaded = result.getOrNull()
            try {
                currentCoroutineContext().ensureActive()
                pageBitmap = loaded
                pageError = result.exceptionOrNull()?.message
                pageLoading = false
            } catch (error: Throwable) {
                loaded?.takeUnless { it.isRecycled }?.recycle()
                throw error
            }
        }

        DisposableEffect(pageBitmap) {
            val ownedBitmap = pageBitmap
            onDispose {
                ownedBitmap?.takeUnless { it.isRecycled }?.recycle()
            }
        }

        val bitmap = pageBitmap
        if (bitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "第 ${pageIndex + 1} 页",
                        modifier = Modifier
                            .width(imageWidth)
                            .clickable { showControls = !showControls }
                            .padding(top = readerTopInset, bottom = readerBottomInset),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }

        if (pageLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }

        if (pageError != null) {
            ReaderCenteredMessage {
                Text(pageError ?: "页面加载失败", color = Color.White)
                Button(onClick = { pageReloadKey += 1 }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("重试")
                }
            }
        }

        if (showControls) {
            ReaderTopControls(
                pageIndex = pageIndex,
                pageCount = pages.urls.size,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            ReaderBottomControls(
                canPrevious = pageIndex > 0,
                canNext = pageIndex < pages.urls.lastIndex,
                zoom = zoom,
                compact = compactControls,
                horizontalInset = readerHorizontalInset,
                onPrevious = ::previousPage,
                onNext = ::nextPage,
                onZoom = {
                    zoom = when {
                        zoom < 1.25f -> 1.5f
                        zoom < 1.75f -> 2f
                        else -> 1f
                    }
                    scope.launch {
                        verticalScroll.scrollTo(0)
                        horizontalScroll.scrollTo(0)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

@Composable
private fun ReaderTopControls(
    pageIndex: Int,
    pageCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minimumTouchTarget = LocalMinimumInteractiveComponentSize.current
    Surface(
        modifier = modifier.padding(top = 2.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Black.copy(alpha = 0.78f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledIconButton(onClick = onBack, modifier = Modifier.size(minimumTouchTarget)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("${pageIndex + 1} / $pageCount", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReaderBottomControls(
    canPrevious: Boolean,
    canNext: Boolean,
    zoom: Float,
    compact: Boolean,
    horizontalInset: Dp,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minimumTouchTarget = LocalMinimumInteractiveComponentSize.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = maxOf(horizontalInset, if (compact) 4.dp else 12.dp),
                end = maxOf(horizontalInset, if (compact) 4.dp else 12.dp),
                bottom = 4.dp,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Black.copy(alpha = 0.82f),
        contentColor = Color.White,
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledIconButton(onClick = onPrevious, enabled = canPrevious, modifier = Modifier.size(minimumTouchTarget)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一页")
                    }
                    FilledIconButton(onClick = onNext, enabled = canNext, modifier = Modifier.size(minimumTouchTarget)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一页")
                    }
                }
                ReaderZoomButton(zoom = zoom, onClick = onZoom)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(onClick = onPrevious, enabled = canPrevious, modifier = Modifier.size(minimumTouchTarget)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上一页")
                }
                ReaderZoomButton(zoom = zoom, onClick = onZoom)
                FilledIconButton(onClick = onNext, enabled = canNext, modifier = Modifier.size(minimumTouchTarget)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下一页")
                }
            }
        }
    }
}

@Composable
private fun ReaderZoomButton(
    zoom: Float,
    onClick: () -> Unit,
) {
    val minimumTouchTarget = LocalMinimumInteractiveComponentSize.current
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(minimumTouchTarget),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
    ) {
        Text("缩放 ${zoom}x")
    }
}

@Composable
private fun ReaderCenteredMessage(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}
