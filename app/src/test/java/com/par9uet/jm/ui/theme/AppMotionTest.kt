package com.par9uet.jm.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMotionTest {
    @Test
    fun systemDisabledAnimationsRemainOffOnAllSupportedVersions() {
        listOf(23, 24, 35, 36).forEach {
            val policy = appMotionPolicy(it, 0f)
            assertFalse(policy.enabled)
            assertEquals(0, policy.enterMs)
            assertEquals(0, policy.exitMs)
        }
    }

    @Test
    fun androidSixUsesShorterTransitions() {
        val legacy = appMotionPolicy(23, 1f)
        val modern = appMotionPolicy(35, 1f)
        assertTrue(legacy.enabled)
        assertTrue(legacy.enterMs < modern.enterMs)
        assertTrue(legacy.exitMs < legacy.enterMs)
        assertTrue(modern.enterMs < 300)
    }
}
