package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

internal val LocalWatchHeaderState = staticCompositionLocalOf<WatchCollapsingHeaderState?> { null }

/** Rotary scrollBy does not dispatch nested scroll, so share the same consumption explicitly. */
internal suspend fun scrollWatchContent(
    pixels: Float,
    header: WatchCollapsingHeaderState?,
    scrollBody: suspend (Float) -> Float,
) {
    val headerConsumed = if (pixels > 0f && header != null) -header.consume(-pixels) else 0f
    val remaining = pixels - headerConsumed
    val bodyConsumed = scrollBody(remaining)
    if (remaining < 0f) header?.consume(-(remaining - bodyConsumed))
}

/** Shares the scroll chain with lazy lists, grids and ordinary verticalScroll forms. */
internal class WatchCollapsingHeaderState : NestedScrollConnection {
    var height by mutableFloatStateOf(0f)
    var collapsed by mutableFloatStateOf(0f)

    fun consume(deltaY: Float): Float {
        val before = collapsed.coerceIn(0f, height)
        collapsed = (before - deltaY).coerceIn(0f, height)
        return before - collapsed
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

    // Reveal only when the body reaches its top; don't cover text whenever direction changes.
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero
}

@Composable
internal fun WatchCollapsingHeader(state: WatchCollapsingHeaderState, content: @Composable () -> Unit) {
    Layout(content = { Column { content() } }, modifier = Modifier.clipToBounds()) { children, constraints ->
        val child = children.single().measure(constraints.copy(minHeight = 0))
        state.height = child.height.toFloat()
        val hidden = state.collapsed.coerceIn(0f, child.height.toFloat()).roundToInt()
        layout(child.width, (child.height - hidden).coerceAtLeast(0)) {
            child.placeRelative(0, -hidden)
        }
    }
}
