package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal fun readerPageAtFraction(fraction: Float, pageCount: Int): Int =
    if (pageCount <= 1 || !fraction.isFinite()) 0
    else (fraction.coerceIn(0f, 1f) * (pageCount - 1)).roundToInt()

/** Small visual track, generous input height. Scrubbing previews; lifting commits one request. */
@Composable
internal fun ReaderProgressBar(
    page: Int,
    pageCount: Int,
    onPreview: (Int?) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safePage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    var dragging by remember { mutableStateOf(false) }
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onCommit)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .9f)
    val accent = MaterialTheme.colorScheme.primary
    val halo = MaterialTheme.colorScheme.primaryContainer
    DisposableEffect(Unit) { onDispose { preview(null) } }
    Canvas(modifier.height(36.dp).testTag("reader-progress").semantics {
        contentDescription = "阅读进度"
        stateDescription = "第 ${safePage + 1} / $pageCount 页"
        progressBarRangeInfo = ProgressBarRangeInfo(safePage.toFloat(), 0f..(pageCount - 1).coerceAtLeast(1).toFloat())
        if (pageCount > 1) setProgress { value ->
            commit(value.roundToInt().coerceIn(0, pageCount - 1)); true
        } else disabled()
    }.pointerInput(pageCount) {
        if (pageCount <= 1) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            val inset = 8.dp.toPx()
            fun pageFor(x: Float) = readerPageAtFraction((x - inset) / (size.width - inset * 2).coerceAtLeast(1f), pageCount)
            var candidate = pageFor(down.position.x)
            var released = false
            dragging = true
            preview(candidate)
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.isConsumed) break
                    candidate = pageFor(change.position.x)
                    preview(candidate)
                    change.consume()
                    if (!change.pressed) { released = true; break }
                }
            } finally {
                dragging = false
                preview(null)
            }
            if (released) commit(candidate)
        }
    }) {
        val start = Offset(8.dp.toPx(), size.height / 2)
        val end = Offset(size.width - 8.dp.toPx(), start.y)
        val fraction = if (pageCount > 1) safePage.toFloat() / (pageCount - 1) else 0f
        val thumb = Offset(start.x + (end.x - start.x) * fraction, start.y)
        val trackHeight = 4.dp.toPx()
        val indicatorHeight = 4.dp.toPx()
        drawRoundRect(
            color = track,
            topLeft = Offset(start.x, start.y - trackHeight / 2),
            size = androidx.compose.ui.geometry.Size(end.x - start.x, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2),
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(start.x, start.y - indicatorHeight / 2),
            size = androidx.compose.ui.geometry.Size((thumb.x - start.x).coerceAtLeast(indicatorHeight), indicatorHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(indicatorHeight / 2),
        )
        drawCircle(
            color = halo,
            radius = (if (dragging) 8.dp else 6.dp).toPx(),
            center = thumb,
        )
        drawCircle(
            color = accent,
            radius = (if (dragging) 5.dp else 4.dp).toPx(),
            center = thumb,
        )
    }
}
