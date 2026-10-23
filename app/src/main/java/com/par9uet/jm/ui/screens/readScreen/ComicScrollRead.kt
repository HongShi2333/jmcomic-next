package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.par9uet.jm.data.models.ComicPicImageState
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.ui.components.ComicPicImage
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import com.par9uet.jm.utils.log
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ComicContinuousRead(
    horizontal: Boolean,
    initialIndex: Int,
    targetIndex: Int,
    zoomState: ReaderZoomState,
    comicReadViewModel: ComicReadViewModel = koinViewModel(),
    doubleTapZoomEnabled: Boolean = true,
    onIndexChange: (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val list = comicPicState.data ?: emptyList()
    val context = LocalContext.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceAtLeast(0)
    )

    LaunchedEffect(targetIndex, list.size) {
        if (list.isEmpty()) return@LaunchedEffect
        val target = targetIndex.coerceIn(0, list.lastIndex)
        if (listState.firstVisibleItemIndex != target) {
            listState.scrollToItem(target)
        }
    }

    LaunchedEffect(listState) {
        launch {
            snapshotFlow { listState.isScrollInProgress }
                .filter { it }
                .collect {
                    comicReadViewModel.hideToolBar()
                }
        }
        launch {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo
                    .takeIf { it.isNotEmpty() }
                    ?.let { it.first().index to it.last().index }
            }
                .filterNotNull()
                .distinctUntilChanged()
                .debounce(120)
                .collect { (first, last) ->
                    comicReadViewModel.decodeVisibleRange(first, last, context)
                }
        }
        launch {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { visibleIndex ->
                    if (list.isEmpty()) return@collect
                    val pageIndex = visibleIndex.coerceIn(0, list.lastIndex)
                    log("visible page index = $pageIndex")
                    onIndexChange(pageIndex)
                    comicReadViewModel.decodeIndex(pageIndex, context)
                }
        }
    }

    fun jumpPage(delta: Int) {
        if (list.isEmpty()) return
        val target = (listState.firstVisibleItemIndex + delta).coerceIn(0, list.lastIndex)
        if (target == listState.firstVisibleItemIndex) return
        coroutineScope.launch {
            listState.animateScrollToItem(target)
        }
    }

    val scrollModifier = if (horizontal) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxSize()
            .readerZoomable(zoomState, enableVerticalPan = false) {
                listState.dispatchRawDelta(it)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .readerTapZones(
                rightToLeft = false,
                onPrevious = { jumpPage(-1) },
                onNext = { jumpPage(1) },
                onMenu = { comicReadViewModel.triggerToolBar() },
                onDoubleTapCenter = if (!horizontal && doubleTapZoomEnabled) {
                    { centroid -> zoomState.toggleZoom(centroid) }
                } else {
                    null
                },
            )
    ) {
        if (horizontal) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(list, key = { _, item -> "${item.comicId}_${item.originSrc}" }) { _, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(item.aspectRatioOrFallback())
                    ) {
                        ComicPicImage(
                            comicPicImageState = item,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                userScrollEnabled = !zoomState.isZoomed,
                modifier = scrollModifier,
            ) {
                itemsIndexed(list, key = { _, item -> "${item.comicId}_${item.originSrc}" }) { _, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(item.aspectRatioOrFallback())
                    ) {
                        ComicPicImage(
                            comicPicImageState = item,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

private fun ComicPicImageState.aspectRatioOrFallback(): Float =
    when (val state = imageResultState) {
        is ImageResultState.Success -> state.decodeImageAspectRatio
        else -> 9f / 16f
    }
