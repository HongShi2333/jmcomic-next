package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.ui.geometry.Offset
import com.par9uet.jm.data.models.LocalSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderInteractionTest {
    @Test
    fun brightnessIsOptInAndUsesSystemModeDefaults() {
        val settings = LocalSetting()
        assertFalse(settings.readerBrightnessEnabled)
        assertEquals(100, settings.readerLightBrightnessPercent)
        assertEquals(30, settings.readerDarkBrightnessPercent)
        assertEquals(0f, readerDimmingAlpha(false, true, 100, 30), 0.001f)
        assertEquals(0f, readerDimmingAlpha(true, false, 100, 30), 0.001f)
        assertEquals(0.7f, readerDimmingAlpha(true, true, 100, 30), 0.001f)
    }

    @Test
    fun configuredPercentAndInvalidValuesStayReadable() {
        assertEquals(0.4f, readerDimmingAlpha(true, false, 60, 15), 0.001f)
        assertEquals(0.85f, readerDimmingAlpha(true, true, 60, 15), 0.001f)
        assertEquals(0.99f, readerDimmingAlpha(true, true, 60, -10), 0.001f)
        assertEquals(0f, readerDimmingAlpha(true, true, 60, 200), 0.001f)
    }

    @Test
    fun zoomedPanFollowsViewportDistanceWithoutDamping() {
        val state = ReaderZoomState()
        state.updateViewportSize(1000, 1600)
        state.applyZoom(4f, Offset.Zero, true)
        state.applyZoom(1f, Offset(120f, 80f), true)
        assertEquals(Offset(120f, 80f), state.offset)
    }

    @Test
    fun pinchKeepsFocusStationary() {
        val state = ReaderZoomState()
        state.updateViewportSize(1000, 1600)
        state.applyZoom(2f, Offset.Zero, true, Offset(750f, 1000f))
        assertEquals(Offset(-250f, -200f), state.offset)
        // Focus starts 250px right of center; scale*250 + translation remains 250.
        assertEquals(250f, 250f * state.scale + state.offset.x, 0.001f)
    }

    @Test
    fun scrollModeScalesFingerDeltaAndKeepsListDirectionConsistent() {
        val state = ReaderZoomState()
        state.updateViewportSize(1000, 1600)
        state.applyZoom(2f, Offset.Zero, false)
        val delta = state.applyZoom(1f, Offset(60f, -100f), false)
        assertEquals(50f, delta, 0.001f)
        assertEquals(Offset(60f, 0f), state.offset)
        assertEquals(-50f, zoomAdjustedReaderScrollDelta(100f, 2f), 0.001f)
    }

    @Test
    fun scrollFlingUsesSameDirectionAndZoomMultiplierAsDrag() {
        assertEquals(1000f / 3f, zoomAdjustedReaderScrollVelocity(-1000f, 3f), 0.001f)
        assertEquals(-1000f, zoomAdjustedReaderScrollVelocity(1000f, 1f), 0.001f)
        assertEquals(0f, zoomAdjustedReaderScrollVelocity(Float.NaN, 2f), 0.001f)
    }

    @Test
    fun pinchReleaseCannotCreateHorizontalFling() {
        assertEquals(
            Offset.Zero,
            sanitizeReaderReleaseVelocity(
                velocity = Offset(-12000f, 600f),
                hadTransformGesture = true,
                enableVerticalPan = true,
            ),
        )
    }

    @Test
    fun continuousReaderSuppressesSidewaysJitterAndCapsVelocity() {
        assertEquals(
            Offset(0f, -2400f),
            sanitizeReaderReleaseVelocity(
                velocity = Offset(-3000f, -2400f),
                hadTransformGesture = false,
                enableVerticalPan = false,
            ),
        )
        assertEquals(
            Offset(-5000f, 0f),
            sanitizeReaderReleaseVelocity(
                velocity = Offset(-12000f, 0f),
                hadTransformGesture = false,
                enableVerticalPan = false,
            ),
        )
    }

    @Test
    fun zoomOutResetsAndPanStopsAtImageEdges() {
        val state = ReaderZoomState()
        state.updateViewportSize(1000, 1600)
        state.applyZoom(2f, Offset(5000f, -5000f), true)
        assertEquals(Offset(500f, -800f), state.offset)
        state.applyZoom(0.1f, Offset.Zero, true)
        assertEquals(1f, state.scale, 0.001f)
        assertEquals(Offset.Zero, state.offset)
        assertFalse(state.isZoomed)
    }

    @Test
    fun finalPointerUpCannotIntroduceNanTransform() {
        val state = ReaderZoomState()
        state.updateViewportSize(1000, 1600)
        state.applyZoom(2f, Offset.Zero, true)
        state.applyZoom(1f, Offset.Zero, true, Offset.Unspecified)
        assertEquals(2f, state.scale, 0.001f)
        assertEquals(Offset.Zero, state.offset)
    }
}
