package com.par9uet.jm.data.models

enum class ComicSearchOrderFilter(
    val value: String,
    val label: String,
    val period: ComicRankPeriod? = null,
) {
    NEWEST("mr", "最新"),
    DAY_RANK("mv", "日排行", ComicRankPeriod.DAY),
    WEEK_RANK("mv", "周排行", ComicRankPeriod.WEEK),
    MONTH_RANK("mv", "月排行", ComicRankPeriod.MONTH),
    MOST_VIEW_COUNT("mv", "最多浏览"),
    MOST_LIKE_COUNT("tf", "最多喜欢"),
    MOST_PIC_COUNT("mp", "最多图片"),
}
