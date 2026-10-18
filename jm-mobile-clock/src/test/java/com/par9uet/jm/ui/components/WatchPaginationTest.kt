package com.par9uet.jm.ui.components

import org.junit.Assert.*
import org.junit.Test

class WatchPaginationTest {
    @Test fun shortListsStayOnOnePage() {
        assertEquals(WatchPageWindow(0, 1, 0, 50), watchPageWindow(50, 4, false))
    }
    @Test fun boundary51Gives40And11() {
        assertEquals(WatchPageWindow(0, 2, 0, 40), watchPageWindow(51, 0, true))
        assertEquals(WatchPageWindow(1, 2, 40, 51), watchPageWindow(51, 1, true))
    }
    @Test fun pageWindowsNeverSkipOrRepeatEntries() {
        for (count in listOf(51, 80, 81, 121, 601)) {
            val countPages = watchPageWindow(count, 0, true).pageCount
            val visited = (0 until countPages).flatMap { page ->
                val window = watchPageWindow(count, page, true)
                assertTrue(window.end - window.start <= 40)
                (window.start until window.end).toList()
            }
            assertEquals((0 until count).toList(), visited)
        }
    }
    @Test fun deletedItemsAndEmptyListsClampTheCurrentPage() {
        assertEquals(WatchPageWindow(0, 1, 0, 0), watchPageWindow(0, 8, true))
        assertEquals(WatchPageWindow(1, 2, 40, 51), watchPageWindow(51, 8, true))
    }
}
