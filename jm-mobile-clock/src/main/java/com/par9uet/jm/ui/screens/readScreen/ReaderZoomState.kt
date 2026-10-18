package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

@Stable
class ReaderZoomState {
    private var transform by mutableStateOf(ReaderImageTransform())
    private var width by mutableFloatStateOf(0f)
    private var height by mutableFloatStateOf(0f)
    private var aspectRatio by mutableFloatStateOf(1f)
    private var round by mutableStateOf(false)
    private val animationProgress = Animatable(0f)
    private var transformRevision = 0
    var focusedPageIndex by mutableIntStateOf(-1)
        private set
    private val geometry get() = ReaderImageGeometry(width, height, aspectRatio, round)
    fun updateRoundScreen(value: Boolean) { if (round != value) { round = value; transform = geometry.clamp(transform) } }
    val scale get() = transform.scale
    val offset get() = Offset(transform.left, transform.top)
    val renderedWidth get() = width * scale
    val renderedHeight get() = geometry.imageHeight(scale)
    val isZoomed get() = scale > 1.01f
    val isAdjusted get() = kotlin.math.abs(scale - 1f) > .01f
    val minimumScale get() = geometry.fitPageScale
    val canPanVertically get() = renderedHeight > height + 1f
    fun updateViewportSize(w: Int, h: Int) {
        if (width == w.toFloat() && height == h.toFloat()) return
        val oldWidth = width
        val ratio = if (oldWidth > 0) w / oldWidth else 1f
        transformRevision++
        width = w.toFloat(); height = h.toFloat()
        transform = geometry.clamp(transform.copy(left = transform.left * ratio, top = transform.top * ratio))
    }
    fun updateImageAspectRatio(value: Float) {
        if (value.isFinite() && value > 0 && value != aspectRatio) {
            transformRevision++
            aspectRatio = value; transform = geometry.clamp(transform)
        }
    }
    fun applyZoom(zoom: Float, pan: Offset = Offset.Zero, anchor: Offset = Offset(width / 2, height / 2)) {
        transformRevision++
        transform = geometry.transform(transform, zoom, pan.x, pan.y, anchor.x, anchor.y)
    }
    fun pan(delta: Offset) = applyZoom(1f, delta)
    fun zoomIn() = applyZoom(1.5f)
    fun zoomOut() = applyZoom(1f / 1.5f)
    fun toggleZoom(anchor: Offset) { if (isAdjusted) reset() else applyZoom(2f, anchor = anchor) }
    fun prepareScrollPage(index: Int, imageAspectRatio: Float, imageTop: Float = 0f) {
        transformRevision++
        focusedPageIndex = index
        aspectRatio = imageAspectRatio.takeIf { it.isFinite() && it > 0 } ?: 1f
        transform = geometry.clamp(ReaderImageTransform(top = imageTop))
    }
    fun endScrollFocus() { focusedPageIndex = -1 }
    fun setScale(value: Float) {
        if (value.isFinite() && value > 0) applyZoom(value / scale)
    }
    fun fitPage() {
        transformRevision++
        transform = geometry.clamp(ReaderImageTransform(geometry.fitPageScale))
    }
    fun focusOnImagePoint(point: Offset) {
        transformRevision++
        transform = geometry.clamp(ReaderImageTransform(2f, width / 2 - point.x * 2, height / 2 - point.y * 2))
    }
    fun reset() { transformRevision++; transform = geometry.clamp(ReaderImageTransform()) }

    /**
     * Animate the complete image transform instead of snapping scale and offset independently.
     * This keeps the image anchored under the finger while the watch renders each frame.
     */
    private suspend fun animateTo(target: ReaderImageTransform, durationMillis: Int = 180) {
        val revision = ++transformRevision
        val from = geometry.clamp(transform)
        val destination = geometry.clamp(target)
        if (from == destination) {
            transform = destination
            return
        }
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        ) {
            val t = value
            if (transformRevision == revision) transform = geometry.clamp(
                ReaderImageTransform(
                    scale = mix(from.scale, destination.scale, t),
                    left = mix(from.left, destination.left, t),
                    top = mix(from.top, destination.top, t),
                )
            )
        }
        if (transformRevision == revision) transform = destination
    }

    suspend fun toggleZoomAnimated(anchor: Offset) {
        val destination = if (isAdjusted) {
            ReaderImageTransform()
        } else {
            geometry.transform(transform, 2f, 0f, 0f, anchor.x, anchor.y)
        }
        animateTo(destination)
    }

    suspend fun focusOnImagePointAnimated(point: Offset) {
        animateTo(ReaderImageTransform(2f, width / 2 - point.x * 2, height / 2 - point.y * 2))
    }

    suspend fun zoomByAnimated(factor: Float) {
        animateTo(geometry.transform(transform, factor, 0f, 0f, width / 2, height / 2))
    }

    suspend fun resetAnimated() {
        animateTo(ReaderImageTransform())
    }

    private fun mix(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction
}

@Composable
fun rememberReaderZoomState(): ReaderZoomState = remember { ReaderZoomState() }
