package com.par9uet.jm.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicSearchRequestTest {
    @Test
    fun categoryRankPeriodsUseDistinctCombinedOrders() {
        val orders = listOf(ComicSearchOrderFilter.DAY_RANK, ComicSearchOrderFilter.WEEK_RANK, ComicSearchOrderFilter.MONTH_RANK)
        assertEquals(
            listOf("mv_t", "mv_w", "mv_m"),
            orders.map { ComicSearchRequest.from("同人", 3, it).categoryOrder },
        )
        orders.forEach { order ->
            assertEquals(ComicRankCategory.DOUJIN, ComicSearchRequest.from("同人 -excluded", 3, order).category)
            assertFalse(ComicSearchRequest.from("同人", 3, order).usesWebTimeRanking)
            assertTrue(ComicSearchRequest.from("同人 -excluded", 3, order).usesWebTimeRanking)
            assertEquals("-excluded", ComicSearchRequest.from("同人 -excluded", 3, order).webSearchQuery)
        }
    }

    @Test
    fun customTagKeepsSearchAndDistinctTimeOptions() {
        val requests = listOf(ComicSearchOrderFilter.DAY_RANK, ComicSearchOrderFilter.WEEK_RANK, ComicSearchOrderFilter.MONTH_RANK)
            .map { ComicSearchRequest.from("custom-tag", 3, it) }
        requests.forEach { assertNull(it.category) }
        requests.forEach { assertTrue(it.usesWebTimeRanking) }
        assertEquals(listOf("mv", "mv", "mv"), requests.map { it.order })
        assertEquals(listOf("t", "w", "m"), requests.map { it.time })
    }

    @Test
    fun nonPeriodSortsExplicitlyResetToAllTime() {
        listOf(ComicSearchOrderFilter.NEWEST, ComicSearchOrderFilter.MOST_VIEW_COUNT, ComicSearchOrderFilter.MOST_LIKE_COUNT, ComicSearchOrderFilter.MOST_PIC_COUNT)
            .forEach { order ->
                val request = ComicSearchRequest.from("同人", 3, order)
                assertEquals("a", request.time)
                assertEquals(order.value, request.categoryOrder)
                assertFalse(request.usesWebTimeRanking)
            }
    }

    @Test
    fun normalSearchForCategorySupportsRankingButMultiWordSearchStaysSearch() {
        assertNull(ComicSearchRequest.from("Cosplay", 0, ComicSearchOrderFilter.WEEK_RANK).category)
        assertTrue(ComicSearchRequest.from("Cosplay", 0, ComicSearchOrderFilter.WEEK_RANK).usesWebTimeRanking)
        assertNull(ComicSearchRequest.from("3D", 3, ComicSearchOrderFilter.MONTH_RANK).category)
        assertTrue(ComicSearchRequest.from("3D", 3, ComicSearchOrderFilter.MONTH_RANK).usesWebTimeRanking)
        assertNull(ComicSearchRequest.from("同人 +作者", 0, ComicSearchOrderFilter.WEEK_RANK).category)
        assertNull(ComicSearchRequest.from("同人", 1, ComicSearchOrderFilter.WEEK_RANK).category)
    }

    @Test
    fun traditionalCategoryLabelsHaveTheSameRequestMapping() {
        assertEquals(ComicRankCategory.SINGLE, ComicSearchRequest.from("單本", 3, ComicSearchOrderFilter.DAY_RANK).category)
        assertEquals(ComicRankCategory.KOREAN, ComicSearchRequest.from("韓漫", 3, ComicSearchOrderFilter.MONTH_RANK).category)
        assertEquals(ComicRankCategory.OTHER, ComicSearchRequest.from("其他類", 3, ComicSearchOrderFilter.WEEK_RANK).category)
    }
}
