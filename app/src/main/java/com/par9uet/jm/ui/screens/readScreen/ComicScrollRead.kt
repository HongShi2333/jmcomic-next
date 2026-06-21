package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.ui.components.ComicPicImage
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import com.par9uet.jm.utils.log
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun ComicScrollRead(
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
        if (lazyListState.firstVisibleItemIndex != target) {
            programmaticScroll = true
            lazyListState.scrollToItem(target)
            pagerState.scrollToPage(target)
            programmaticScroll = false
        }
    }

    LaunchedEffect(lazyListState) {
        launch {
            snapshotFlow { lazyListState.isScrollInProgress }
                .filter { it }
                .collect {
                    comicReadViewModel.hideToolBar()
                }
        }
        launch {
            snapshotFlow { lazyListState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .debounce(1000)
                .collect {
                    if (programmaticScroll) return@collect
                    log("lazyListState.firstVisibleItemIndex currentIndexState = $currentIndexState it = $it")
                    if (currentIndexState != it) {
                        currentIndexState = it
                        onUpdateSliderValue(it.toFloat())
                        comicReadViewModel.decodeIndex(currentIndexState, context)
                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                                val screenHeight = size.height
                                val clickY = up.position.y

                                when {
                                    clickY < screenHeight / 3 -> {
                                        comicReadViewModel.prev(context)
                                        coroutineScope.launch {
                                            lazyListState.scrollToItem(currentIndexState)
                                            pagerState.scrollToPage(currentIndexState)
                                            onUpdateSliderValue(currentIndexState.toFloat())
                                        }
                                    }

                                    clickY > screenHeight * 2 / 3 -> {
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
            }
    ) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = !zoomState.isZoomed,
            modifier = Modifier
                .fillMaxSize()
                .readerZoomable(zoomState)
        ) {
            items(list, key = {
                "${it.comicId}_${it.originSrc}"
            }) {
                ComicPicImage(
                    comicPicImageState = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            when (val state = it.imageResultState) {
                                is ImageResultState.Success -> {
                                    state.decodeImageAspectRatio
                                }

                                else -> {
                                    9f / 16
                                }
                            }
                        )
                )
            }
        }
    }
}
