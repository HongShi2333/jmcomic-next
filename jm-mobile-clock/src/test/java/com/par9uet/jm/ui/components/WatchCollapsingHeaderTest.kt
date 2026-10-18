package com.par9uet.jm.ui.components

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchCollapsingHeaderTest {
    @Test fun rotaryScrollCollapsesHeaderBeforePassingRemainderToBody() = runBlocking {
        val header = WatchCollapsingHeaderState().apply { height = 48f }
        var body = 0f
        scrollWatchContent(80f, header) { body += it; it }
        assertEquals(48f, header.collapsed, 0f)
        assertEquals(32f, body, 0f)
    }

    @Test fun rotaryOnlyRevealsHeaderAfterBodyReturnsToTop() = runBlocking {
        val header = WatchCollapsingHeaderState().apply { height = 48f; collapsed = 48f }
        scrollWatchContent(-20f, header) { it }
        assertEquals(48f, header.collapsed, 0f)
        scrollWatchContent(-20f, header) { -8f }
        assertEquals(36f, header.collapsed, 0f)
        scrollWatchContent(-100f, header) { 0f }
        assertEquals(0f, header.collapsed, 0f)
    }
}
