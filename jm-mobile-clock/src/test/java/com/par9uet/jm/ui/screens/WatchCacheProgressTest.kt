package com.par9uet.jm.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchCacheProgressTest {
    @Test fun writingMetadataDoesNotPrematurelyShowComplete() {
        assertEquals(99, cacheProgressPercent(1f, false))
        assertEquals(100, cacheProgressPercent(1f, true))
        assertEquals(25, cacheProgressPercent(.25f, false))
    }

    @Test fun invalidProgressAndUnknownSpeedStayReadable() {
        assertEquals(0, cacheProgressPercent(Float.NaN, false))
        assertEquals(0, cacheProgressPercent(-1f, false))
        assertEquals("正在获取图片", cacheSaveSpeed(Float.NaN))
        assertEquals("正在获取图片", cacheSaveSpeed(0f))
        assertEquals("保存 1.0 KB/s", cacheSaveSpeed(1024f))
        assertEquals("保存 1.0 MB/s", cacheSaveSpeed(1024f * 1024f))
    }
}
