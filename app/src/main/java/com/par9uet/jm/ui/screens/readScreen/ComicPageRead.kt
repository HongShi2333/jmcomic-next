package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.par9uet.jm.ui.components.ComicPicImage
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val PAGE_TURN_ANIMATION_MS = 180

internal enum class ReaderPageDirection {
    LeftToRight,
    RightToLeft,
    Vertical,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComicPageRead(
    initialIndex: Int,
    targetIndex: Int,
    direction: ReaderPageDirection,
    zoomState: ReaderZoomState,
    comicReadViewModel: ComicReadViewModel = koinViewModel(),
    doubleTapZoomEnabled: Boolean = true,
    onIndexChange: (Int) -> Unit,
) {
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val list = comicPicState.data ?: emptyList()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceAtLeast(0)) { list.size }

    LaunchedEffect(targetIndex, list.size) {
        if (list.isEmpty()) return@LaunchedEffect
        val target = targetIndex.coerceIn(0, list.lastIndex)
        if (target != pagerState.currentPage) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .filter { it }
            .collect { comicReadViewModel.hideToolBar() }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onIndexChange(page)
                comicReadViewModel.decodeIndex(page, context)
            }
    }

    fun turnPage(delta: Int) {
        if (list.isEmpty()) return
        val target = (pagerState.currentPage + delta).coerceIn(0, list.lastIndex)
        if (target == pagerState.currentPage) return
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = target,
                animationSpec = tween(durationMillis = PAGE_TURN_ANIMATION_MS),
            )
        }
    }

    val pagerModifier = Modifier
        .fillMaxSize()
        .readerTapZones(
            rightToLeft = direction == ReaderPageDirection.RightToLeft,
            onPrevious = { turnPage(-1) },
            onNext = { turnPage(1) },
            onMenu = { comicReadViewModel.triggerToolBar() },
            onDoubleTapCenter = if (doubleTapZoomEnabled) {
                { centroid -> zoomState.toggleZoom(centroid) }
            } else {
                null
            },
        )

    val pageContent: @Composable (Int) -> Unit = { page ->
        val item = list.getOrNull(page)
        if (item != null) {
            // 只允许当前页参与缩放，否则相邻页会跟着共享的缩放状态一起放大并露出第二张图。
            val isCurrentPage = page == pagerState.currentPage
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isCurrentPage) {
                            Modifier.readerZoomable(zoomState)
                        } else {
                            Modifier
                        }
                    )
            ) {
                ComicPicImage(
                    comicPicImageState = item,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }

    if (direction == ReaderPageDirection.Vertical) {
        VerticalPager(
            modifier = pagerModifier,
            state = pagerState,
            userScrollEnabled = !zoomState.isZoomed,
            beyondViewportPageCount = 1,
        ) { page ->
            pageContent(page)
        }
    } else {
        HorizontalPager(
            modifier = pagerModifier,
            state = pagerState,
            reverseLayout = direction == ReaderPageDirection.RightToLeft,
            userScrollEnabled = !zoomState.isZoomed,
            beyondViewportPageCount = 1,
        ) { page ->
            pageContent(page)
        }
    }
}
