package com.par9uet.jm.ui.screens.readScreen
import org.junit.Assert.*
import org.junit.Test
class ReaderImageGeometryTest {
    private val portrait = ReaderImageGeometry(240f, 240f, .5f)
    @Test fun fitWidthDoesNotShrinkLongPage() { assertEquals(480f, portrait.imageHeight(1f), 0f); assertEquals(ReaderImageTransform(), portrait.clamp(ReaderImageTransform())) }
    @Test fun longPageCanPanAtOneTimes() { assertEquals(-240f, portrait.transform(ReaderImageTransform(), 1f, 0f, -999f, 0f, 0f).top, 0f) }
    @Test fun horizontalPanIsClampedAtFitWidth() { assertEquals(0f, portrait.transform(ReaderImageTransform(), 1f, -999f, 0f, 0f, 0f).left, 0f) }
    @Test fun allImageEdgesReachableAtFourTimes() { val value=portrait.clamp(ReaderImageTransform(4f, -9999f, -9999f)); assertEquals(-720f,value.left,0f);assertEquals(-1680f,value.top,0f) }
    @Test fun zoomIsAnchoredToFinger() { val value=portrait.transform(ReaderImageTransform(),2f,0f,0f,80f,100f); assertEquals(-80f,value.left,0f);assertEquals(-100f,value.top,0f) }
    @Test fun landscapeCentersVertically() { assertEquals(60f,ReaderImageGeometry(240f,240f,2f).clamp(ReaderImageTransform()).top,0f) }
    @Test fun scaleLimitsCannotBecomeZeroOrHuge() { assertEquals(1f,portrait.clamp(ReaderImageTransform(-2f)).scale,0f);assertEquals(4f,portrait.clamp(ReaderImageTransform(100f)).scale,0f) }
    @Test fun invalidMeasurementsNeverCreateNaN() { assertEquals(ReaderImageTransform(),ReaderImageGeometry(Float.NaN,240f,.5f).clamp(ReaderImageTransform(2f))); val value=portrait.transform(ReaderImageTransform(),Float.NaN,Float.NaN,Float.NaN,Float.NaN,Float.NaN);assertTrue(value.left.isFinite());assertTrue(value.top.isFinite()) }
    @Test fun roundCornersCanMoveInsideVisibleCenter() {
        val round = ReaderImageGeometry(240f,240f,.5f,true)
        val topLeft=round.clamp(ReaderImageTransform(2f,999f,999f))
        assertEquals(60f,topLeft.left,0f);assertEquals(60f,topLeft.top,0f)
        val bottomRight=round.clamp(ReaderImageTransform(2f,-9999f,-9999f))
        assertEquals(-300f,bottomRight.left,0f);assertEquals(-780f,bottomRight.top,0f)
    }
    @Test fun restoringZoomReturnsToTopOfPortrait() { assertEquals(0f,portrait.clamp(ReaderImageTransform()).top,0f) }
    @Test fun roundFitPageIncludesEveryCornerOfPortraitAndLandscapeImages() {
        for (size in listOf(160f, 192f, 240f)) for (ratio in listOf(.15f, .5f, 1f, 2f, 5f)) {
            val geometry = ReaderImageGeometry(size, size, ratio, true)
            val fit = geometry.clamp(ReaderImageTransform(geometry.fitPageScale))
            val halfWidth = size * fit.scale / 2f
            val halfHeight = geometry.imageHeight(fit.scale) / 2f
            val r = size / 2f
            assertTrue("$size/$ratio", (halfWidth * halfWidth + halfHeight * halfHeight) / (r * r) <= 1.0001f)
            assertEquals((size - halfWidth * 2) / 2, fit.left, .001f)
            assertEquals((size - halfHeight * 2) / 2, fit.top, .001f)
        }
    }
    @Test fun zoomInThenOutRetainsTheSameImagePoint() {
        val geometry = ReaderImageGeometry(240f, 240f, .2f)
        val atMiddle = ReaderImageTransform(1f, 0f, -400f)
        val enlarged = geometry.transform(atMiddle, 2f, 0f, 0f, 120f, 120f)
        val restored = geometry.transform(enlarged, .5f, 0f, 0f, 120f, 120f)
        assertEquals(atMiddle, restored)
    }
    @Test fun scrollPreviewUsesCenterPageNotTopClippedRemnant() {
        val topRemnant = ReaderVisiblePage(2, -430, 480)
        val centerPage = ReaderVisiblePage(3, 50, 480)
        assertEquals(centerPage, readerPreviewPage(listOf(topRemnant, centerPage), 0, 240))
        assertNull(readerPreviewPage(emptyList(), 0, 240))
    }
    @Test fun allThreeLiveControlCirclesStayWithinRoundDials() {
        for (size in listOf(160f, 192f, 240f, 320f)) {
            val y = readerControlsCenterY(size, size)
            for (x in listOf(size / 2 - 52f, size / 2, size / 2 + 52f)) {
                val distance = kotlin.math.sqrt((x-size/2)*(x-size/2) + (y-size/2)*(y-size/2))
                assertTrue("$size:$x,$y", distance + 24f <= size / 2)
            }
        }
    }
}
