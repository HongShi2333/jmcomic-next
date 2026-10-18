package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.par9uet.jm.ui.components.WatchScaffold
import com.par9uet.jm.ui.components.WatchLazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.par9uet.jm.data.models.ComicPicImageState
import com.par9uet.jm.data.models.ImageResultState
import com.par9uet.jm.ui.components.ComicPicImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private class ReaderTapTracker { var time = 0L; var position = Offset.Zero; var job: Job? = null; var zoomJob: Job? = null }

/** A single image viewport: fit WIDTH, pan long pages vertically, zoom around the finger. */
@Composable
fun WatchReaderImage(item: ComicPicImageState, zoom: ReaderZoomState,
    onTap: (Offset) -> Unit, onPageSwipe: ((Int) -> Unit)? = null, modifier: Modifier = Modifier) {
    val image = item.imageResultState as? ImageResultState.Success
    if (image == null) {
        // A broken/slow image must not trap the user on a page with no controls.
        WatchScaffold(title = "第 ${item.index + 1} 页") {
            WatchLazyColumn(modifier.fillMaxSize()) {
                item { ComicPicImage(comicPicImageState = item, modifier = Modifier.fillMaxWidth().height(120.dp)) }
                item { TextButton(onClick = { onTap(Offset(Float.NaN, Float.NaN)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("阅读控制 / 翻页") } }
            }
        }
        return
    }
    ReaderImageViewport(image, "${item.comicId}:${item.index}", zoom, onTap, onPageSwipe, modifier)
}

/** Production canvas shared with offline previews and device gesture/overlay regressions. */
@Composable
internal fun ReaderImageViewport(
    image: ImageResultState.Success,
    imageKey: Any,
    zoom: ReaderZoomState,
    onTap: (Offset) -> Unit,
    onPageSwipe: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val latestTap by rememberUpdatedState(onTap)
    val latestSwipe by rememberUpdatedState(onPageSwipe)
    val scope = rememberCoroutineScope()
    val tracker = remember(imageKey) { ReaderTapTracker() }
    DisposableEffect(tracker) { onDispose { tracker.job?.cancel(); tracker.zoomJob?.cancel() } }
    SideEffect { zoom.updateImageAspectRatio(image.decodeImageAspectRatio) }
    Canvas(modifier.fillMaxSize().testTag("reader-image-viewport").background(Color.Black).clipToBounds()
        .onSizeChanged { zoom.updateViewportSize(it.width, it.height) }
        .pointerInput(imageKey) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var delta = Offset.Zero
                var kind = 0 // 1 horizontal page swipe; 2 vertical pan; 3 free image transform.
                var last = down
                do {
                    val event = awaitPointerEvent()
                    val pan = event.calculatePan()
                    val factor = event.calculateZoom()
                    delta += pan
                    val multi = event.changes.count { it.pressed } > 1
                    if (multi) { kind = 3; tracker.job?.cancel(); tracker.zoomJob?.cancel(); tracker.time = 0L }
                    else if (kind == 0 && delta.getDistance() > viewConfiguration.touchSlop) {
                        tracker.job?.cancel(); tracker.zoomJob?.cancel(); tracker.time = 0L
                        if (zoom.isZoomed) kind = 3
                        else if (abs(delta.x) > abs(delta.y) && latestSwipe != null) kind = 1
                        else if (abs(delta.y) >= abs(delta.x) && zoom.canPanVertically) kind = 2
                        else kind = 4
                    }
                    when (kind) {
                        2 -> zoom.pan(Offset(0f, pan.y))
                        3 -> zoom.applyZoom(factor, pan, event.calculateCentroid())
                    }
                    if (kind in 1..3) event.changes.forEach { if (it.position != it.previousPosition) it.consume() }
                    last = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                } while (event.changes.any { it.pressed })
                if (kind == 1 && abs(delta.x) >= size.width * .16f) latestSwipe?.invoke(if (delta.x < 0) 1 else -1)
                else if (kind == 0) {
                    val time = last.uptimeMillis
                    val position = last.position
                    if (tracker.time != 0L && time - tracker.time <= viewConfiguration.doubleTapTimeoutMillis && (position - tracker.position).getDistance() < viewConfiguration.touchSlop * 2) {
                        tracker.job?.cancel(); tracker.zoomJob?.cancel(); tracker.time = 0L; tracker.zoomJob = scope.launch { zoom.toggleZoomAnimated(position) }
                    } else {
                        tracker.job?.cancel()
                        tracker.time = time; tracker.position = position
                        tracker.job = scope.launch { delay(viewConfiguration.doubleTapTimeoutMillis); tracker.time = 0; latestTap(position) }
                    }
                }
            }
        }) {
        if (zoom.renderedWidth > 0 && zoom.renderedHeight > 0) drawImage(image.decodeImageBitmap,
            dstOffset = IntOffset(zoom.offset.x.roundToInt(), zoom.offset.y.roundToInt()),
            dstSize = IntSize(zoom.renderedWidth.roundToInt().coerceAtLeast(1), zoom.renderedHeight.roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.Medium)
    }
}
