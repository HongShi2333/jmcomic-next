package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.ComicPicImage
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import org.koin.androidx.compose.koinViewModel

private const val CLICK_PAGE_TURN_ANIMATION_MS = 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicPageRead(
    lazyListState: LazyListState,
    pagerState: PagerState,
    targetIndex: Int,
    zoomState: ReaderZoomState,
    comicReadViewModel: ComicReadViewModel = koinViewModel(),
    tapOnly: Boolean = false,
    onUpdateSliderValue: (value: Float) -> Unit
) {
    var currentIndexState by comicReadViewModel.currentIndexState
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val list = comicPicState.data ?: listOf()
    val context = LocalContext.current
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var clickTargetIndex by remember { mutableIntStateOf(-1) }

    // 点击翻页：等待 animateScrollToPage 动画完成后再更新状态
    LaunchedEffect(clickTargetIndex) {
        if (clickTargetIndex < 0) return@LaunchedEffect
        if (list.isEmpty()) {
            clickTargetIndex = -1
            return@LaunchedEffect
        }
        val target = clickTargetIndex.coerceIn(0, maxOf(0, list.lastIndex))
        if (target == pagerState.currentPage) {
            clickTargetIndex = -1
            return@LaunchedEffect
        }
        isProgrammaticScroll = true
        pagerState.animateScrollToPage(
            page = target,
            animationSpec = tween(durationMillis = CLICK_PAGE_TURN_ANIMATION_MS)
        )
        currentIndexState = target
        onUpdateSliderValue(target.toFloat())
        comicReadViewModel.decodeIndex(target, context)
        isProgrammaticScroll = false
        clickTargetIndex = -1
    }

    LaunchedEffect(targetIndex, list.size) {
        if (list.isEmpty()) return@LaunchedEffect
        val target = targetIndex.coerceIn(0, list.lastIndex)
        if (target == pagerState.currentPage) return@LaunchedEffect
        isProgrammaticScroll = true
        pagerState.animateScrollToPage(target)
        isProgrammaticScroll = false
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .filter { it }
            .collect {
                comicReadViewModel.hideToolBar()
            }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .filterNot { isProgrammaticScroll }
            .collect { newPage ->
                if (newPage != currentIndexState) {
                    currentIndexState = newPage
                    onUpdateSliderValue(newPage.toFloat())
                    comicReadViewModel.decodeIndex(newPage, context)
                }
            }
    }

    val readerModifier = Modifier
        .fillMaxSize()
        .readerZoomable(zoomState)

    val clickModifier = if (tapOnly) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Final
                    )
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    if (up != null && !up.isConsumed) {
                        val distance = (up.position - down.position).getDistance()
                        if (distance < 10.dp.toPx()) {
                            val screenWidth = size.width
                            val clickX = up.position.x

                            when {
                                list.isEmpty() -> {
                                    comicReadViewModel.triggerToolBar()
                                }

                                clickX < screenWidth / 3 -> {
                                    val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    clickTargetIndex = target
                                }

                                clickX > screenWidth * 2 / 3 -> {
                                    val target = (pagerState.currentPage + 1).coerceAtMost(maxOf(0, list.lastIndex))
                                    clickTargetIndex = target
                                }

                                else -> {
                                    comicReadViewModel.triggerToolBar()
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(
                        requireUnconsumed = true,
                        pass = PointerEventPass.Final
                    )
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    if (up != null && !up.isConsumed) {
                        val distance = (up.position - down.position).getDistance()
                        if (distance < 10.dp.toPx()) {
                            val screenWidth = size.width
                            val clickX = up.position.x

                            when {
                                list.isEmpty() -> {
                                    comicReadViewModel.triggerToolBar()
                                }

                                clickX < screenWidth / 3 -> {
                                    val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    clickTargetIndex = target
                                }

                                clickX > screenWidth * 2 / 3 -> {
                                    val target = (pagerState.currentPage + 1).coerceAtMost(maxOf(0, list.lastIndex))
                                    clickTargetIndex = target
                                }

                                else -> {
                                    comicReadViewModel.triggerToolBar()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    HorizontalPager(
        modifier = readerModifier.then(clickModifier),
        state = pagerState,
        userScrollEnabled = !tapOnly && !zoomState.isZoomed
    ) { page ->
        val item = list.getOrNull(page) ?: return@HorizontalPager
        ComicPicImage(
            comicPicImageState = item,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
