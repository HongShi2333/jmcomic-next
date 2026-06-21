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

@Stable
class ReaderZoomState {
    var scale by mutableStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    val isZoomed: Boolean
        get() = scale > 1.01f || offset != Offset.Zero

    fun applyZoom(zoomChange: Float, panChange: Offset) {
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        offset = if (nextScale <= 1.01f) {
            Offset.Zero
        } else {
            offset + panChange
        }
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }
}

@Composable
fun rememberReaderZoomState(): ReaderZoomState = remember { ReaderZoomState() }

@Composable
fun Modifier.readerZoomable(zoomState: ReaderZoomState): Modifier {
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        zoomState.applyZoom(zoomChange, panChange)
    }
    return this
        .graphicsLayer(
            scaleX = zoomState.scale,
            scaleY = zoomState.scale,
            translationX = zoomState.offset.x,
            translationY = zoomState.offset.y
        )
        .transformable(state = transformableState)
}
