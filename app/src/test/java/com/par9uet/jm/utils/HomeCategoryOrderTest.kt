package com.par9uet.jm.utils

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.HomeComicSwiperItem
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCategoryOrderTest {
    @Test
    fun keepsUnknownCategoriesAfterSavedOrder() {
        val categories = listOf(
            item("a", "A"),
            item("b", "B"),
            item("c", "C"),
        )

        assertEquals(
            listOf("b", "a", "c"),
            orderHomeCategories(categories, listOf("b", "a")).map { it.id },
        )
    }

    private fun item(id: String, title: String) = HomeComicSwiperItem(
        id = id,
        title = title,
        list = listOf(Comic.create(1, title, emptyList())),
    )
}
