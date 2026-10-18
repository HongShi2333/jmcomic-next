package com.par9uet.jm.data.models

enum class ComicRankPeriod(
    val routeValue: String,
    val label: String,
    val apiValue: String,
) {
    DAY("day", "日排行", "t"),
    WEEK("week", "周排行", "w"),
    MONTH("month", "月排行", "m");

    companion object {
        fun fromRouteValue(value: String?): ComicRankPeriod =
            entries.firstOrNull { it.routeValue == value } ?: WEEK
    }
}

enum class ComicRankCategory(
    val apiValue: String,
    val label: String,
    val supportsCategoryRanking: Boolean = true,
) {
    DOUJIN("doujin", "同人"),
    SINGLE("single", "单本"),
    SHORT("short", "短篇"),
    OTHER("another", "其他类"),
    KOREAN("hanman", "韩漫"),
    AMERICAN("meiman", "美漫"),
    COSPLAY("doujin_cosplay", "Cosplay", supportsCategoryRanking = false),
    IMAGE_3D("3D", "3D", supportsCategoryRanking = false);

    companion object {
        fun fromTagLabel(value: String): ComicRankCategory? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { category ->
                category.label.lowercase() == normalized ||
                    category.apiValue.lowercase() == normalized ||
                    when (category) {
                        DOUJIN -> normalized in setOf("同人", "doujin")
                        SINGLE -> normalized in setOf("单本", "單本", "single")
                        SHORT -> normalized in setOf("短篇", "short")
                        OTHER -> normalized in setOf("其他", "其他类", "其他類", "another")
                        KOREAN -> normalized in setOf("韩漫", "韓漫", "hanman")
                        AMERICAN -> normalized in setOf("美漫", "meiman")
                        COSPLAY -> normalized in setOf("cosplay", "doujin_cosplay")
                        IMAGE_3D -> normalized in setOf("3d", "image_3d")
                    }
            }
        }

        fun fromApiValue(value: String): ComicRankCategory? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

data class ComicRankingFilter(
    val tagName: String = "",
    val category: ComicRankCategory? = null,
    val period: ComicRankPeriod = ComicRankPeriod.WEEK,
)
