package com.par9uet.jm.ui.screens.readScreen

import org.junit.Assert.*
import org.junit.Test

class ReaderProgressTest {
    @Test fun progressEndpointsReachTheFirstAndLastPages() {
        for (count in listOf(1, 11, 40, 201, 1200)) {
            assertEquals(0, readerPageAtFraction(0f, count))
            assertEquals(count - 1, readerPageAtFraction(1f, count))
        }
    }
    @Test fun progressUsesNearestPageAndNeverOverflows() {
        assertEquals(5, readerPageAtFraction(.5f, 11))
        assertEquals(0, readerPageAtFraction(-4f, 40))
        assertEquals(39, readerPageAtFraction(3f, 40))
        assertEquals(0, readerPageAtFraction(Float.NaN, 40))
        assertEquals(0, readerPageAtFraction(.5f, 0))
    }
    @Test fun scrubbingForwardNeverMovesBackwards() {
        val pages = (0..200).map { readerPageAtFraction(it / 200f, 137) }
        assertEquals(0, pages.first())
        assertEquals(136, pages.last())
        assertTrue(pages.zipWithNext().all { (a, b) -> b >= a })
    }
}
