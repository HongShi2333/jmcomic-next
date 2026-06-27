package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.max

@Stable
class ReaderZoomState {
    var scale by mutableStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    private var viewportWidth by mutableStateOf(0)
    private var viewportHeight by mutableStateOf(0)

    val isZoomed: Boolean
        get() = scale > 1.01f || offset != Offset.Zero

    fun updateViewportSize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        offset = clampOffset(offset, scale)
    }

    fun applyZoom(zoomChange: Float, panChange: Offset, enableVerticalPan: Boolean) {
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        offset = if (nextScale <= 1.01f) {
            Offset.Zero
        } else {
            val nextOffset = Offset(
                x = offset.x + panChange.x,
                y = if (enableVerticalPan) offset.y + panChange.y else 0f
            )
            clampOffset(nextOffset, nextScale)
        }
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    private fun clampOffset(value: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.01f || viewportWidth <= 0) {
            return Offset.Zero
        }
        val maxX = max(0f, viewportWidth * (targetScale - 1f) / 2f)
        val maxY = max(0f, viewportHeight * (targetScale - 1f) / 2f)
        return Offset(
            x = value.x.coerceIn(-maxX, maxX),
            y = value.y.coerceIn(-maxY, maxY)
        )
    }
}

@Composable
fun rememberReaderZoomState(): ReaderZoomState = remember { ReaderZoomState() }

@Composable
fun Modifier.readerZoomable(
    zoomState: ReaderZoomState,
    enableVerticalPan: Boolean = true
): Modifier {
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        zoomState.applyZoom(zoomChange, panChange, enableVerticalPan)
    }
    return this
        .onSizeChanged { zoomState.updateViewportSize(it.width, it.height) }
        .graphicsLayer(
            scaleX = zoomState.scale,
            scaleY = zoomState.scale,
            translationX = zoomState.offset.x,
            translationY = zoomState.offset.y
        )
        .transformable(state = transformableState)
}
