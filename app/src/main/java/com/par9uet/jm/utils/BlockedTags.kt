package com.par9uet.jm.utils

import com.par9uet.jm.data.models.Comic

fun normalizeBlockedTag(value: String): String {
    return value.trim()
}

fun normalizeBlockedTagList(tags: List<String>): List<String> {
    return tags.map(::normalizeBlockedTag)
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
}

fun Comic.isBlockedByTags(blockedTags: List<String>): Boolean {
    val normalizedBlockedTags = normalizeBlockedTagList(blockedTags)
    if (normalizedBlockedTags.isEmpty()) return false
    val blockedSet = normalizedBlockedTags.map { it.lowercase() }.toSet()
    return (tagList + roleList + workList)
        .map { it.trim().lowercase() }
        .any { it in blockedSet }
}

fun List<Comic>.filterBlockedTags(blockedTags: List<String>): List<Comic> {
    if (blockedTags.isEmpty()) return this
    return filterNot { it.isBlockedByTags(blockedTags) }
}
