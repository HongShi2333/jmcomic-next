package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.ComicPicImage
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import kotlin.math.roundToInt

@Composable
fun ComicScrollRead(
    lazyListState: LazyListState, pagerState: PagerState, targetIndex: Int,
    zoomState: ReaderZoomState, comicReadViewModel: ComicReadViewModel = koinViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(), onUpdateSliderValue: (Float) -> Unit,
    controlsVisible: Boolean = false,
    onToggleControls: () -> Unit = comicReadViewModel::triggerToolBar,
) {
    val state by comicReadViewModel.comicPicState.collectAsState()
    val list = state.data.orEmpty()
    val context = LocalContext.current
    val latestUpdate by rememberUpdatedState(onUpdateSliderValue)
    val latestControls by rememberUpdatedState(onToggleControls)
    val animationScope = rememberCoroutineScope()
    val focusedPage = zoomState.focusedPageIndex
    var pendingRestore by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var focusGestureInProgress by remember { mutableStateOf(false) }

    // Do not dispose the list and then await its layout. Recompose it first, then restore its offset.
    LaunchedEffect(controlsVisible, zoomState.scale, focusedPage, focusGestureInProgress) {
        if (!controlsVisible && !focusGestureInProgress && !zoomState.isAdjusted && focusedPage in list.indices) {
            pendingRestore = focusedPage to (-zoomState.offset.y).roundToInt().coerceAtLeast(0)
            zoomState.endScrollFocus()
        }
    }
    LaunchedEffect(pendingRestore, focusedPage) {
        val restore = pendingRestore
        if (restore != null && focusedPage < 0) {
            lazyListState.scrollToItem(restore.first.coerceIn(0, list.lastIndex.coerceAtLeast(0)), restore.second)
            pendingRestore = null
        }
    }
    LaunchedEffect(targetIndex, list.size) {
        if (list.isNotEmpty() && zoomState.focusedPageIndex < 0 && pendingRestore == null) {
            lazyListState.scrollToItem(targetIndex.coerceIn(0, list.lastIndex))
        }
    }
    if (focusedPage in list.indices) {
        val item = list[focusedPage]
        LaunchedEffect(focusedPage, item.comicId) { comicReadViewModel.decodeIndex(focusedPage, context) }
        WatchReaderImage(item, zoomState, onTap = { latestControls() })
    } else {
        LaunchedEffect(lazyListState, list, pendingRestore) {
            if (pendingRestore != null) return@LaunchedEffect
            snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.map { it.index } }.collect { visible ->
                if (visible.isNotEmpty()) {
                    latestUpdate(visible.first().toFloat())
                    comicReadViewModel.decodeVisibleRange(visible.first(), visible.last(), context)
                }
            }
        }
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(list, key = { _, item -> "${item.comicId}_${item.index}" }) { page, item ->
                val image = item.imageResultState as? ImageResultState.Success
                Box(Modifier.fillMaxWidth().aspectRatio(image?.decodeImageAspectRatio ?: .7f).pointerInput(item, image) {
                    detectTapGestures(onTap = { latestControls() }, onDoubleTap = { point ->
                        if (image != null) {
                            focusGestureInProgress = true
                            latestUpdate(page.toFloat())
                            val top = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == page }?.offset?.toFloat() ?: 0f
                            zoomState.prepareScrollPage(page, image.decodeImageAspectRatio, top)
                            animationScope.launch {
                                try { zoomState.focusOnImagePointAnimated(point) }
                                finally { focusGestureInProgress = false }
                            }
                        }
                    })
                }) { ComicPicImage(comicPicImageState = item, modifier = Modifier.fillMaxSize()) }
            }
        }
    }
}
