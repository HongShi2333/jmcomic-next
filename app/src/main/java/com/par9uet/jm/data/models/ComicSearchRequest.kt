package com.par9uet.jm.data.models

import com.par9uet.jm.utils.searchContentWithoutExcludedTags
import com.par9uet.jm.utils.parseSearchSyntax

/**
 * Category ranking uses categories/filter with o=mv_t/mv_w/mv_m.
 * The mobile search endpoint ignores t (verified against live day/week/month results).
 * Timed text/tag queries must use the website instead of presenting all-time results.
 * Never infer a category from one token of a multi-keyword query.
 */
data class ComicSearchRequest(
    val category: ComicRankCategory?,
    val order: String,
    val time: String,
    val categoryOrder: String,
    val usesWebTimeRanking: Boolean,
    val webSearchQuery: String,
) {
    companion object {
        fun from(query: String, mainTag: Int, order: ComicSearchOrderFilter): ComicSearchRequest {
            val category = if (mainTag == 0 || mainTag == 3) {
                ComicRankCategory.fromTagLabel(searchContentWithoutExcludedTags(query).trim())
                    ?.takeIf { it.supportsCategoryRanking }
            } else {
                null
            }
            val time = order.period?.apiValue ?: "a"
            val exclusions = parseSearchSyntax(query).excludes
            return ComicSearchRequest(
                category = category,
                order = order.value,
                time = time,
                categoryOrder = if (time == "a") order.value else "${order.value}_$time",
                usesWebTimeRanking = order.period != null && (category == null || exclusions.isNotEmpty()),
                // A category is part of the website path. Leave only exclusions in its query.
                webSearchQuery = if (category == null) query else exclusions.joinToString(" ") { "-$it" },
            )
        }
    }
}
