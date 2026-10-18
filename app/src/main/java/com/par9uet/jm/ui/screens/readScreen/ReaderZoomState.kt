package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlin.math.abs
import kotlin.math.max

private const val MAX_READER_FLING_VELOCITY = 5000f

internal fun zoomAdjustedReaderScrollDelta(fingerDeltaY: Float, scale: Float): Float {
    if (!fingerDeltaY.isFinite() || !scale.isFinite()) return 0f
    return -fingerDeltaY / scale.coerceIn(1f, 4f)
}

internal fun zoomAdjustedReaderScrollVelocity(fingerVelocityY: Float, scale: Float): Float {
    if (!fingerVelocityY.isFinite() || !scale.isFinite()) return 0f
    return -fingerVelocityY / scale.coerceIn(1f, 4f)
}

internal fun sanitizeReaderReleaseVelocity(
    velocity: Offset,
    hadTransformGesture: Boolean,
    enableVerticalPan: Boolean,
): Offset {
    if (hadTransformGesture) return Offset.Zero
    val x = velocity.x.takeIf(Float::isFinite)?.coerceIn(
        -MAX_READER_FLING_VELOCITY,
        MAX_READER_FLING_VELOCITY,
    ) ?: 0f
    val y = velocity.y.takeIf(Float::isFinite)?.coerceIn(
        -MAX_READER_FLING_VELOCITY,
        MAX_READER_FLING_VELOCITY,
    ) ?: 0f
    if (!enableVerticalPan && abs(y) >= abs(x) * 0.8f) {
        return Offset(0f, y)
    }
    return Offset(x, y)
}

@Stable
class ReaderZoomState {
    var scale by mutableStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    private var viewportWidth by mutableStateOf(0)
    private var viewportHeight by mutableStateOf(0)
    private var horizontalFling: Animatable<Float, AnimationVector1D>? = null
    private var verticalFling: Animatable<Float, AnimationVector1D>? = null
    private var flingJob: Job? = null
    private var flingGeneration = 0L

    val isZoomed: Boolean
        get() = scale > 1.01f || offset != Offset.Zero

    fun updateViewportSize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        offset = clampOffset(offset, scale)
    }

    fun applyZoom(
        zoomChange: Float,
        panChange: Offset,
        enableVerticalPan: Boolean,
        centroid: Offset = Offset(viewportWidth / 2f, viewportHeight / 2f),
    ): Float {
        if (!zoomChange.isFinite() || zoomChange <= 0f || !panChange.x.isFinite() || !panChange.y.isFinite() ||
            !centroid.x.isFinite() || !centroid.y.isFinite()
        ) return 0f
        val oldScale = scale
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        val ratio = nextScale / oldScale
        val focus = centroid - Offset(viewportWidth / 2f, viewportHeight / 2f)
        val nextOffset = (offset - focus) * ratio + focus + panChange
        scale = nextScale
        offset = if (nextScale <= 1.01f) {
            Offset.Zero
        } else {
            clampOffset(nextOffset.copy(y = if (enableVerticalPan) nextOffset.y else 0f), nextScale)
        }
        // Continuous reading is driven by LazyColumn, so map the finger's
        // movement into list scroll coordinates using the current zoom scale.
        return if (enableVerticalPan) 0f else zoomAdjustedReaderScrollDelta(panChange.y, nextScale)
    }

    suspend fun stopFling() {
        horizontalFling?.stop()
        verticalFling?.stop()
    }

    suspend fun fling(
        velocity: Offset,
        decay: DecayAnimationSpec<Float>,
        enableVerticalPan: Boolean,
        onVerticalScroll: (Float) -> Float,
    ) {
        if (!isZoomed || !velocity.x.isFinite() || !velocity.y.isFinite()) return
        flingJob?.cancel()
        flingJob = null
        val generation = ++flingGeneration
        val horizontal = Animatable(offset.x)
        val vertical = Animatable(if (enableVerticalPan) offset.y else 0f)
        val maxX = max(0f, viewportWidth * (scale - 1f) / 2f)
        val maxY = max(0f, viewportHeight * (scale - 1f) / 2f)
        val verticalVelocity = if (enableVerticalPan) {
            velocity.y
        } else {
            zoomAdjustedReaderScrollVelocity(velocity.y, scale)
        }
        horizontal.updateBounds(-maxX, maxX)
        if (enableVerticalPan) vertical.updateBounds(-maxY, maxY)
        horizontalFling = horizontal
        verticalFling = vertical
        flingJob = currentCoroutineContext()[Job]
        try {
            coroutineScope {
                val horizontalJob = launch {
                    horizontal.animateDecay(velocity.x, decay) {
                        if (flingGeneration == generation) {
                            offset = Offset(value, offset.y)
                        }
                    }
                }
                val verticalJob = launch {
                    if (enableVerticalPan) {
                        vertical.animateDecay(verticalVelocity, decay) {
                            if (flingGeneration == generation) {
                                offset = Offset(offset.x, value)
                            }
                        }
                    } else {
                        var previous = 0f
                        vertical.animateDecay(verticalVelocity, decay) {
                            val requested = value - previous
                            previous = value
                            if (flingGeneration == generation) {
                                onVerticalScroll(requested)
                            }
                        }
                    }
                }
                joinAll(horizontalJob, verticalJob)
            }
        } finally {
            if (horizontalFling === horizontal) horizontalFling = null
            if (verticalFling === vertical) verticalFling = null
            if (flingJob === currentCoroutineContext()[Job]) flingJob = null
        }
    }

    fun reset() {
        cancelFling()
        scale = 1f
        offset = Offset.Zero
    }

    fun cancelFling() {
        flingGeneration++
        flingJob?.cancel()
        flingJob = null
        horizontalFling = null
        verticalFling = null
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
    enableVerticalPan: Boolean = true,
    onVerticalScroll: (Float) -> Float = { 0f },
): Modifier {
    val latestVerticalScroll by rememberUpdatedState(onVerticalScroll)
    val decay = rememberSplineBasedDecay<Float>()
    val gestureScope = rememberCoroutineScope()
    return this
        .onSizeChanged { zoomState.updateViewportSize(it.width, it.height) }
        // Input must be outside the scaled layer, otherwise pan is divided by scale.
        .pointerInput(zoomState, enableVerticalPan) {
            awaitEachGesture {
                zoomState.cancelFling()
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var accepted = false
                var accumulatedZoom = 1f
                var accumulatedPan = Offset.Zero
                var velocityTracker = VelocityTracker()
                var hadTransformGesture = false
                var velocitySampleCount = 0
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.isConsumed }) break
                    val pointerCount = event.changes.count { it.pressed }
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid(useCurrent = true)
                    if (pointerCount > 1 || abs(zoom - 1f) > 0.001f) {
                        // A pinch changes the centroid when a finger is released.
                        // Never turn that discontinuity into a horizontal fling.
                        hadTransformGesture = true
                        velocityTracker = VelocityTracker()
                        velocitySampleCount = 0
                    } else if (pointerCount == 1 && !hadTransformGesture &&
                        centroid.x.isFinite() && centroid.y.isFinite()
                    ) {
                        val activeChange = event.changes.firstOrNull { it.pressed }
                        if (activeChange != null) {
                            velocityTracker.addPosition(activeChange.uptimeMillis, centroid)
                            velocitySampleCount++
                        }
                    }
                    accumulatedZoom *= zoom
                    accumulatedPan += pan
                    val canTransform = zoomState.isZoomed || event.changes.count { it.pressed } > 1
                    if (!accepted && canTransform) {
                        val zoomMotion = abs(1 - accumulatedZoom) * event.calculateCentroidSize(useCurrent = false)
                        accepted = zoomMotion > viewConfiguration.touchSlop ||
                            accumulatedPan.getDistance() > viewConfiguration.touchSlop
                    }
                    if (accepted && (zoom != 1f || pan != Offset.Zero)) {
                        val scroll = zoomState.applyZoom(zoom, pan, enableVerticalPan, event.calculateCentroid(useCurrent = false))
                        if (!enableVerticalPan && scroll != 0f) latestVerticalScroll(scroll)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (accepted) {
                    val rawVelocity = velocityTracker.calculateVelocity()
                    val velocity = if (velocitySampleCount >= 2) {
                        sanitizeReaderReleaseVelocity(
                            velocity = Offset(rawVelocity.x, rawVelocity.y),
                            hadTransformGesture = hadTransformGesture,
                            enableVerticalPan = enableVerticalPan,
                        )
                    } else {
                        Offset.Zero
                    }
                    if (velocity != Offset.Zero) {
                        gestureScope.launch {
                            zoomState.fling(
                                velocity = Offset(velocity.x, velocity.y),
                                decay = decay,
                                enableVerticalPan = enableVerticalPan,
                                onVerticalScroll = latestVerticalScroll,
                            )
                        }
                    }
                }
            }
        }
        .graphicsLayer {
            scaleX = zoomState.scale
            scaleY = zoomState.scale
            translationX = zoomState.offset.x
            translationY = zoomState.offset.y
        }
}
