package com.par9uet.jm.repository

import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecommendationMixerTest {
    @Test
    fun interleavesSourcesAndDeduplicatesWithoutChangingSourceLists() {
        val categories = listOf(
            category("builtin_korean", 1..20),
            category("builtin_doujin", 21..40),
            category("builtin_single", 41..60),
            category("builtin_cosplay", 61..80),
        )
        val mixed = buildDiverseHomeCategory(categories)!!
        assertEquals(40, mixed.content.size)
        assertTrue(mixed.content.take(8).map { it.id }.toSet().size > 1)
        assertNotEquals((1..20).map(Int::toString), mixed.content.take(20).map { it.id })
        assertEquals(20, categories.first().content.size)
    }

    @Test
    fun emptySourcesDoNotCreateAnEmptyTab() {
        assertEquals(null, buildDiverseHomeCategory(emptyList()))
        assertEquals(null, buildDiverseHomeCategory(listOf(category("builtin_korean", emptyList()))))
    }

    private fun category(id: String, ids: IntRange): HomeSwiperComicListItemResponse =
        category(id, ids.map(Int::toString))

    private fun category(id: String, ids: List<String>) =
        HomeSwiperComicListItemResponse(
            id, id, id, "builtin", "",
            ids.map { value ->
                HomeSwiperComicListItemResponse.ListItem(
                    id = value,
                    author = "",
                    description = null,
                    name = value,
                    image = "",
                    category = HomeSwiperComicListItemResponse.ListItem.Category(null, null),
                    category_sub = HomeSwiperComicListItemResponse.ListItem.Category(null, null),
                    liked = false,
                    is_favorite = false,
                    update_at = 0,
                )
            },
        )
}
