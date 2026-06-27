package com.par9uet.jm

import com.par9uet.jm.data.models.LauncherDisguise
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherDisguiseTest {
    @Test
    fun fromId_returnsDefaultForUnknown() {
        assertEquals(LauncherDisguise.DEFAULT, LauncherDisguise.fromId("unknown"))
    }

    @Test
    fun fromId_resolvesPresets() {
        assertEquals(LauncherDisguise.INNOCUOUS_LAUNCHER, LauncherDisguise.fromId("launcher"))
        assertEquals(LauncherDisguise.INNOCUOUS_ROUND, LauncherDisguise.fromId("launcher_round"))
    }
}