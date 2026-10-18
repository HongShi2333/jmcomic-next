package com.par9uet.jm.ui.screens.readScreen

import org.junit.Assert.*
import org.junit.Test

class ReaderZoomStateTest {
    @Test fun scrollPreviewBindsAspectBeforeScalingAndRetainsFocusAtOneTimes() {
        val zoom = ReaderZoomState()
        zoom.updateViewportSize(240, 240)
        zoom.prepareScrollPage(7, .2f, -400f)
        assertEquals(7, zoom.focusedPageIndex)
        assertEquals(1200f, zoom.renderedHeight, .001f)
        zoom.setScale(2f)
        assertEquals(-920f, zoom.offset.y, .001f)
        zoom.setScale(1f)
        assertEquals(-400f, zoom.offset.y, .001f)
        assertEquals(7, zoom.focusedPageIndex)
    }
    @Test fun wholePageAndFitWidthDoNotChangeTheImageOrViewportWidth() {
        val zoom = ReaderZoomState()
        zoom.updateViewportSize(192, 192)
        zoom.updateRoundScreen(true)
        zoom.prepareScrollPage(4, .5f)
        zoom.fitPage()
        assertTrue(zoom.isAdjusted)
        assertEquals(zoom.minimumScale, zoom.scale, 0f)
        zoom.reset()
        assertEquals(1f, zoom.scale, 0f)
        assertEquals(192f, zoom.renderedWidth, 0f)
        assertEquals(4, zoom.focusedPageIndex)
    }
    @Test fun badScaleInputDoesNotDestroyTheCurrentImageTransform() {
        val zoom = ReaderZoomState()
        zoom.updateViewportSize(240, 240)
        zoom.prepareScrollPage(0, .5f)
        zoom.setScale(2f)
        val before = zoom.offset
        zoom.setScale(Float.NaN)
        zoom.setScale(0f)
        assertEquals(2f, zoom.scale, 0f)
        assertEquals(before, zoom.offset)
    }
    @Test fun changedViewportScalesOffsetInImageCoordinates() {
        val zoom = ReaderZoomState()
        zoom.updateViewportSize(240, 240)
        zoom.prepareScrollPage(0, .2f, -400f)
        zoom.updateViewportSize(480, 480)
        assertEquals(-800f, zoom.offset.y, .001f)
        assertEquals(2400f, zoom.renderedHeight, .001f)
    }
}
