package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.par9uet.jm.ui.components.LocalWatchMotionEnabled
import com.par9uet.jm.ui.viewModel.ComicReadViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ComicPageRead(
    lazyListState: LazyListState, pagerState: PagerState, targetIndex: Int,
    zoomState: ReaderZoomState, comicReadViewModel: ComicReadViewModel = koinViewModel(),
    tapOnly: Boolean = false, onUpdateSliderValue: (Float) -> Unit,
    controlsVisible: Boolean = false,
    onToggleControls: () -> Unit = comicReadViewModel::triggerToolBar,
) {
    val state by comicReadViewModel.comicPicState.collectAsState()
    var index by comicReadViewModel.currentIndexState
    val list = state.data.orEmpty()
    val context = LocalContext.current
    val item = list.getOrNull(index) ?: return
    val motion = LocalWatchMotionEnabled.current
    val transition = remember { Animatable(0f) }
    var previousIndex by remember { mutableIntStateOf(index) }
    var direction by remember { mutableIntStateOf(0) }
    LaunchedEffect(index) {
        val old = previousIndex
        previousIndex = index
        if (old != index && motion) {
            direction = if (index > old) 1 else -1
            transition.snapTo(1f)
            transition.animateTo(0f, tween(160))
        } else transition.snapTo(0f)
    }
    fun select(delta: Int) {
        val next = (index + delta).coerceIn(0, list.lastIndex)
        if (next == index) return
        zoomState.reset()
        index = next
        onUpdateSliderValue(next.toFloat())
        comicReadViewModel.decodeIndex(next, context)
    }
    LaunchedEffect(item) { comicReadViewModel.decodeIndex(index, context) }
    WatchReaderImage(
        item, zoomState,
        onTap = { point ->
            val width = context.resources.displayMetrics.widthPixels
            if (controlsVisible) onToggleControls()
            else if (tapOnly && !zoomState.isZoomed && point.x < width / 3f) select(-1)
            else if (tapOnly && !zoomState.isZoomed && point.x > width * 2 / 3f) select(1)
            else onToggleControls()
        },
        onPageSwipe = if (tapOnly) null else ::select,
        modifier = Modifier.graphicsLayer {
            translationX = direction * size.width * .08f * transition.value
            alpha = 1f - .15f * transition.value
        },
    )
}
