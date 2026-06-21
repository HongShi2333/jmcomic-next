package com.par9uet.jm.ui.screens.readScreen

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicPageRead(
    lazyListState: LazyListState,
    pagerState: PagerState,
    targetIndex: Int,
    zoomState: ReaderZoomState,
    comicReadViewModel: ComicReadViewModel = koinViewModel(),
    onUpdateSliderValue: (value: Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentIndexState by comicReadViewModel.currentIndexState
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val list = comicPicState.data ?: listOf()
    val context = LocalContext.current
    var programmaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(targetIndex, list.size) {
        if (list.isEmpty()) return@LaunchedEffect
        val target = targetIndex.coerceIn(0, list.lastIndex)
        if (pagerState.currentPage != target) {
            programmaticScroll = true
            pagerState.scrollToPage(target)
            lazyListState.scrollToItem(target)
            programmaticScroll = false
        }
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
            .collect {
                if (programmaticScroll) return@collect
                if (currentIndexState != it) {
                    currentIndexState = it
                    onUpdateSliderValue(it.toFloat())
                    comicReadViewModel.decodeIndex(currentIndexState, context)
                }
            }
    }

    HorizontalPager(
        modifier = Modifier
            .fillMaxSize()
            .readerZoomable(zoomState)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // 1. 在 Initial 阶段观察按下，不消耗事件，确保 Pager 能收到
                        val down =
                            awaitFirstDown(
                                requireUnconsumed = true,
                                pass = PointerEventPass.Final
                            )
                        // 2. 等待抬起
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                        // 3. 判定逻辑：只有在没被消费（说明不是滑动）且距离很短时触发
                        if (up != null && !up.isConsumed) {
                            val distance = (up.position - down.position).getDistance()
                            if (distance < 10.dp.toPx()) {
                                // --- 获取点击位置 ---
                                val screenWidth = size.width
                                val clickX = up.position.x

                                when {
                                    clickX < screenWidth / 3 -> {
                                        comicReadViewModel.prev(context)
                                        coroutineScope.launch {
                                            lazyListState.scrollToItem(currentIndexState)
                                            pagerState.scrollToPage(currentIndexState)
                                            onUpdateSliderValue(currentIndexState.toFloat())
                                        }
                                    }

                                    clickX > screenWidth * 2 / 3 -> {
                                        comicReadViewModel.next(context)
                                        coroutineScope.launch {
                                            lazyListState.scrollToItem(currentIndexState)
                                            pagerState.scrollToPage(currentIndexState)
                                            onUpdateSliderValue(currentIndexState.toFloat())
                                        }
                                    }

                                    else -> {
                                        comicReadViewModel.triggerToolBar()
                                    }
                                }
                            }
                        }
                    }
                }
            },
        state = pagerState,
        userScrollEnabled = !zoomState.isZoomed
    ) { page ->
        val item = list[page]
        ComicPicImage(
            comicPicImageState = item,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
