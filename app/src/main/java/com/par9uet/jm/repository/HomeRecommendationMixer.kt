package com.par9uet.jm.repository

import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse

/**
 * Interleaves category sources so a general recommendation tab does not inherit
 * the site's current long-form Korean ranking bias.
 */
internal fun buildDiverseHomeCategory(
    categories: List<HomeSwiperComicListItemResponse>,
    maxItems: Int = 40,
): HomeSwiperComicListItemResponse? {
    val preferredIds = listOf(
        "builtin_doujin",
        "builtin_single",
        "builtin_short",
        "builtin_cosplay",
        "builtin_3d",
        "builtin_american",
        "builtin_korean",
        "builtin_random",
        "builtin_latest",
    )
    val buckets = preferredIds.mapNotNull { id ->
        categories.firstOrNull { it.id == id }?.content?.let { id to it }
    }
    if (buckets.isEmpty()) return null
    val cursors = buckets.associate { it.first to 0 }.toMutableMap()
    val selected = LinkedHashMap<String, HomeSwiperComicListItemResponse.ListItem>()
    var progress = true
    while (selected.size < maxItems && progress) {
        progress = false
        for ((id, items) in buckets) {
            val cursor = cursors.getValue(id)
            val item = items.getOrNull(cursor)
            if (item != null) {
                cursors[id] = cursor + 1
                if (selected.putIfAbsent(item.id, item) == null) progress = true
                if (selected.size >= maxItems) break
            }
        }
    }
    if (selected.isEmpty()) return null
    return HomeSwiperComicListItemResponse(
        id = "builtin_diverse",
        title = "精选推荐",
        slug = "builtin_diverse",
        type = "builtin",
        filter_val = "",
        content = selected.values.toList(),
    )
}
