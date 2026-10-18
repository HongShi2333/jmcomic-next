package com.par9uet.jm.utils

import com.par9uet.jm.data.models.HomeComicSwiperItem

fun orderHomeCategories(
    categories: List<HomeComicSwiperItem>,
    order: List<String>,
): List<HomeComicSwiperItem> {
    val positions = order.distinct().withIndex().associate { it.value to it.index }
    return categories.sortedBy { positions[it.id] ?: Int.MAX_VALUE }
}
