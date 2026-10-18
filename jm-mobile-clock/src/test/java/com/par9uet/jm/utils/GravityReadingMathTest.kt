package com.par9uet.jm.utils

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class GravityReadingMathTest {
    private fun tilted(degrees: Float): GravityVector {
        val radians = Math.toRadians(degrees.toDouble())
        return GravityVector(0f, sin(radians).toFloat(), cos(radians).toFloat())
    }

    @Test fun neutralAndDeadZoneHoldThePage() {
        assertEquals(0f, gravityReadingSpeed(gravityTiltDegrees(tilted(35f), tilted(35f)), 1f), 0f)
        assertEquals(0f, gravityReadingSpeed(3.9f, 1f), 0f)
    }
    @Test fun speedIsDirectionalMonotonicAndBounded() {
        assertTrue(gravityReadingSpeed(20f, 1f) > gravityReadingSpeed(10f, 1f))
        assertTrue(gravityReadingSpeed(10f, 2f) > gravityReadingSpeed(10f, 1f))
        assertEquals(-gravityReadingSpeed(20f, 1f), gravityReadingSpeed(-20f, 1f), .0001f)
        assertEquals(1f, gravityReadingSpeed(170f, 2.5f), 0f)
        assertEquals(0f, gravityReadingSpeed(Float.NaN, 1f), 0f)
    }
    @Test fun angleWrapDoesNotReverseAt180Degrees() {
        assertEquals(2f, gravityTiltDegrees(tilted(-179f), tilted(179f)), .01f)
        assertEquals(-2f, gravityTiltDegrees(tilted(179f), tilted(-179f)), .01f)
    }
    @Test fun calibrationRequiresThreeSecondsOfFreshStableSamples() {
        val tracker = GravityStabilityTracker()
        for (i in 0 until 60) assertNull(tracker.sample(tilted(15f), 1_000_000_000L + i * 50_000_000L))
        assertNotNull(tracker.sample(tilted(15f), 4_000_000_000L))
        assertEquals(3_000L, tracker.stableMillis)
    }
    @Test fun movementAndSensorGapsRestartCalibration() {
        val tracker = GravityStabilityTracker()
        for (i in 0..50) tracker.sample(tilted(0f), 1_000_000_000L + i * 50_000_000L)
        assertNull(tracker.sample(tilted(10f), 3_550_000_000L))
        assertEquals(0L, tracker.stableMillis)
        assertNull(tracker.sample(tilted(10f), 10_000_000_000L))
        assertEquals(0L, tracker.stableMillis)
    }
    @Test fun invalidAndSidewaysSensorsCannotCauseScrolling() {
        assertNull(GravityVector(Float.NaN, 0f, 0f).normalized())
        assertEquals(0f, gravityTiltDegrees(GravityVector(1f, 0f, 0f), tilted(0f)), 0f)
    }
}
