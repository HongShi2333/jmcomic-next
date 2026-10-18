package com.par9uet.jm.data.models

/** Metadata is local; choosing a category loads only its requested page. */
data class WatchHomeCategory(val id: String, val title: String, val pageable: Boolean = true)
val watchHomeCategories = listOf(
    WatchHomeCategory("builtin_latest", "最新上架"),
    WatchHomeCategory("builtin_week_hot", "本周热门"),
    WatchHomeCategory("builtin_month_hot", "本月热门"),
    WatchHomeCategory("builtin_most_liked", "最多喜欢"),
    WatchHomeCategory("builtin_random", "随机推荐", false),
    WatchHomeCategory("builtin_serialization", "连载系列"),
    WatchHomeCategory("builtin_doujin", "同人"),
    WatchHomeCategory("builtin_single", "单本"),
    WatchHomeCategory("builtin_short", "短篇"),
    WatchHomeCategory("builtin_korean", "韩漫"),
    WatchHomeCategory("builtin_american", "美漫"),
    WatchHomeCategory("builtin_cosplay", "Cosplay"),
    WatchHomeCategory("builtin_3d", "3D"),
    WatchHomeCategory("builtin_most_images", "图片最多"),
)
