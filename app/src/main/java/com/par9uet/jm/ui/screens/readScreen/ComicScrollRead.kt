package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.ComicPicImage
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import com.par9uet.jm.utils.log
import org.koin.compose.getKoin
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun ComicScrollRead(
    lazyListState: LazyListState,
    pagerState: PagerState,
    targetIndex: Int,
    zoomState: ReaderZoomState,
    comicReadViewModel: ComicReadViewModel = koinViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
    onUpdateSliderValue: (value: Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val comicPicState by comicReadViewModel.comicPicState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val list = comicPicState.data ?: listOf()
    val context = LocalContext.current
    val latestOnUpdateSliderValue = rememberUpdatedState(onUpdateSliderValue)

    fun scrollToCurrentPage() {
        if (list.isEmpty()) return
        val target = comicReadViewModel.currentIndexState.intValue
            .coerceIn(0, list.lastIndex)
        coroutineScope.launch {
            lazyListState.scrollToItem(target)
            pagerState.scrollToPage(target)
            latestOnUpdateSliderValue.value(target.toFloat())
        }
    }

    LaunchedEffect(targetIndex, list.size) {
        if (list.isEmpty()) return@LaunchedEffect
        val target = targetIndex.coerceIn(0, list.lastIndex)
        if (lazyListState.firstVisibleItemIndex != target) {
            lazyListState.scrollToItem(target)
            pagerState.scrollToPage(target)
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
            snapshotFlow {
                lazyListState.layoutInfo.visibleItemsInfo
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
            snapshotFlow {
                // Read both values so Compose emits while the current item is moving
                // and when the next manga page becomes the first visible item.
                lazyListState.firstVisibleItemIndex to
                    lazyListState.firstVisibleItemScrollOffset
            }
                .map { (index, _) -> index }
                .distinctUntilChanged()
                .collect { visibleIndex ->
                    val pageIndex = visibleIndex.coerceIn(0, list.lastIndex)
                    log("visible page index currentIndexState = ${comicReadViewModel.currentIndexState.intValue} it = $pageIndex")
                    // The parent owns the observable page state used by ToolsBar.
                    latestOnUpdateSliderValue.value(pageIndex.toFloat())
                    comicReadViewModel.decodeIndex(pageIndex, context)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(localSetting.readTapMode) {
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
                                val screenWidth = size.width
                                val clickY = up.position.y
                                val clickX = up.position.x

                                when {
                                    localSetting.readTapMode == "side" && clickX < screenWidth / 3 -> {
                                        comicReadViewModel.prev(context)
                                        scrollToCurrentPage()
                                    }

                                    localSetting.readTapMode == "side" && clickX > screenWidth * 2 / 3 -> {
                                        comicReadViewModel.next(context)
                                        scrollToCurrentPage()
                                    }

                                    localSetting.readTapMode != "side" && clickY < screenHeight / 3 -> {
                                        comicReadViewModel.prev(context)
                                        scrollToCurrentPage()
                                    }

                                    localSetting.readTapMode != "side" && clickY > screenHeight * 2 / 3 -> {
                                        comicReadViewModel.next(context)
                                        scrollToCurrentPage()
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
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxSize()
                .readerZoomable(zoomState, enableVerticalPan = false)
        ) {
            itemsIndexed(list, key = { _, item ->
                "${item.comicId}_${item.originSrc}"
            }) { page, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            when (val state = item.imageResultState) {
                                is ImageResultState.Success -> state.decodeImageAspectRatio
                                else -> 9f / 16
                            }
                        )
                ) {
                    ComicPicImage(
                        comicPicImageState = item,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
