package com.par9uet.jm.ui.components
import org.junit.Assert.*
import org.junit.Test
class WatchLayoutGeometryTest {
    private val sizes = listOf(160f, 192f, 200f, 240f, 320f)
    @Test fun roundWidthsRemainStable() { sizes.forEach { assertEquals(14f, calculateWatchViewportInsets(it, it, true).horizontal, 0f) } }
    @Test fun squareUsesTenDp() { assertEquals(10f, calculateWatchViewportInsets(192f, 192f, false).horizontal, 0f) }
    @Test fun noVerticalInscribedRectangle() { sizes.forEach { assertEquals(0f, calculateWatchViewportInsets(it, it, true).vertical, 0f) } }
    @Test fun homeActionsAlwaysScrollWithContent() { sizes.forEach { size -> listOf(1f, 1.3f, 2f).forEach { font -> assertTrue(watchHomeUsesScrollableHeader(size, size, true, font)) } } }
    @Test fun topBarDoesNotAddASecondLargeInset() { sizes.forEach { assertEquals(4f, watchListStartPadding(it, true, true), 0f) } }
    @Test fun homeStartsBelowTheRoundRim() { sizes.forEach { val top = watchListStartPadding(it, true, false); assertTrue(top >= 8f); assertTrue(top <= 20f) } }
    @Test fun tinyAndInvalidWidthsAreSafe() { listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { assertEquals(0f, calculateWatchViewportInsets(it, it, true).horizontal, 0f) }; listOf(1f, 10f, 24f).forEach { assertTrue(it - 2 * calculateWatchViewportInsets(it, it, true).horizontal >= 0) } }
    @Test fun topBarIconCornersRemainVisibleOnRoundDials() {
        sizes.forEach { size ->
            val inset=watchTopBarInsets(size,size,true)
            val cx=inset.horizontal+24; val cy=inset.vertical+24
            for(x in listOf(cx-12,cx+12)) for(y in listOf(cy-12,cy+12)) {
                val dx=(x-size/2)/(size/2);val dy=(y-size/2)/(size/2)
                assertTrue("$size icon",dx*dx+dy*dy<=1f)
            }
        }
    }
    @Test fun pinCommitButtonsFitRoundDial() { sizes.forEach { size ->
        // Every corner of the two production 48dp buttons lies within the dial.
        for (x in listOf(size/2-52, size/2-4, size/2+4, size/2+52)) for (y in listOf(size*.70f-24, size*.70f+24)) {
            val dx=(x-size/2)/(size/2);val dy=(y-size/2)/(size/2)
            assertTrue("$size: $x,$y", dx*dx+dy*dy<=1f)
        }
    } }
    @Test fun listEdgeTransformDoesNotChangeMeasurements() {
        assertEquals(WatchEdgeTransform(), watchEdgeTransform(500f, 0f, 1_000f, false))
        assertEquals(WatchEdgeTransform(), watchEdgeTransform(500f, 0f, 1_000f, true))
        val nearEdge = watchEdgeTransform(950f, 0f, 1_000f, true)
        assertTrue(nearEdge.scale < 1f)
        assertTrue(nearEdge.scale >= .78f)
        assertTrue(nearEdge.alpha < 1f)
        assertTrue(nearEdge.alpha >= .68f)
    }
    @Test fun appsGridProtectsSmallRoundDialAndLargeText() {
        assertEquals(2, watchAppsColumns(192f, 1f))
        assertEquals(2, watchAppsColumns(240f, 1.5f))
        assertEquals(3, watchAppsColumns(240f, 1.2f))
    }
    @Test fun smallCapsuleIsCenteredAndFitsAboveRoundContent() {
        val width = watchCapsuleWidth(192f)
        assertEquals(119.04f, width, .01f)
        val y = watchCapsuleTop(192f, 192f, width, true)
        assertTrue(y >= 3f)
        assertTrue(y < 20f)
        assertEquals(4f, watchCapsuleTop(192f, 192f, width, false), 0f)
    }
    @Test fun floatingTargetAndDockStayInsideRoundAndSquareDials() {
        sizes.forEach { size ->
            val clamped = clampWatchFloatingPoint(WatchPoint(size * 3f, -size.toFloat()), size, size, 48f, true)
            val normalizedX = (clamped.x - size / 2f) / (size / 2f - 26f)
            val normalizedY = (clamped.y - size / 2f) / (size / 2f - 26f)
            assertTrue("$size clamped circle", normalizedX * normalizedX + normalizedY * normalizedY <= 1.0001f)

            val docked = watchFloatingDockPoint(WatchPoint(clamped.x, clamped.y), size, size, 48f, true)
            val dockX = (docked.x - size / 2f) / (size / 2f - 26f)
            val dockY = (docked.y - size / 2f) / (size / 2f - 26f)
            assertTrue("$size dock circle", dockX * dockX + dockY * dockY <= 1.0001f)
            assertTrue("$size dock side", kotlin.math.abs(dockX) > .80f)

            val square = clampWatchFloatingPoint(WatchPoint(-size.toFloat(), size * 3f), size, size, 48f, false)
            assertTrue("$size square x", square.x >= 26f && square.x <= size - 26f)
            assertTrue("$size square y", square.y >= 26f && square.y <= size - 26f)
        }
    }
    @Test fun flingProjectionIsBoundedForSmallDialClamp() {
        val projected = projectWatchFloatingPoint(WatchPoint(100f, 100f), WatchPoint(8_000f, -8_000f))
        assertEquals(300f, projected.x, 0f)
        assertEquals(-100f, projected.y, 0f)
    }
    @Test fun idleTimingAndOpacityStaySubtle() {
        assertEquals(5_000L, WATCH_FLOATING_IDLE_MILLIS)
        assertEquals(.42f, WATCH_FLOATING_IDLE_ALPHA, 0f)
    }
}
