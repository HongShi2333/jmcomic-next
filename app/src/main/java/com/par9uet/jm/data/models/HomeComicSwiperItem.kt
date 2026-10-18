package com.par9uet.jm.data.models

data class HomeComicSwiperItem(
    val id: String,
    val title: String,
    val list: List<Comic>,
    val pageSource: HomeCategoryPageSource? = null,
)

enum class HomeCategoryPageKind { BUILTIN, CATEGORY, PROMOTE }

data class HomeCategoryPageSource(
    val kind: HomeCategoryPageKind,
    val value: String,
    val initialPage: Int,
)

data class HomeComicPage(val comics: List<Comic>, val nextPage: Int?)
