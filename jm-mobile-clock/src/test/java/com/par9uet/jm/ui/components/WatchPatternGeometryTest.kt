package com.par9uet.jm.ui.components

import kotlin.math.hypot
import org.junit.Assert.*
import org.junit.Test

class WatchPatternGeometryTest {
    @Test fun expandedGridHitRegionsFitTheRoundDial() {
        for (size in listOf(160f, 192f, 240f, 320f)) {
            val side = size * .76f
            val left = (size - side) / 2f
            val top = size * .55f - side / 2f
            for (point in watchPatternCenters(side, side)) {
                val x = left + point.x
                val y = top + point.y
                assertTrue("$size: $point", hypot(x - size / 2, y - size / 2) + side * .06f <= size / 2f)
                assertTrue("Top control stays separate", y - side * .06f > 34f)
            }
        }
    }
    @Test fun widerSpacingLeavesGapsThatDoNotSelectAdjacentDots() {
        val side = 192f * .76f
        val points = watchPatternCenters(side, side)
        assertTrue(points[1].x - points[0].x > 45f)
        for (i in points.indices) assertEquals(i, watchPatternHit(points[i].x, points[i].y, side, side))
        assertNull(watchPatternHit((points[0].x + points[1].x) / 2, points[0].y, side, side))
        assertNull(watchPatternHit(points[0].x + side * .07f, points[0].y, side, side))
    }
    @Test fun unchangedIndicesKeepPreviouslySavedPatternsCompatible() {
        val points = watchPatternCenters(180f, 180f)
        val oldSequence = listOf(0, 1, 2, 4, 6)
        val entered = oldSequence.map { watchPatternHit(points[it].x, points[it].y, 180f, 180f) }.joinToString("")
        assertEquals("01246", entered)
    }
}
