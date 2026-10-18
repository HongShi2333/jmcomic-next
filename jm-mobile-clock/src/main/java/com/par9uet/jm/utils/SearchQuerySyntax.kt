package com.par9uet.jm.utils

data class SearchQuerySyntax(
    val includes: List<String>,
    val excludes: List<String>,
)

private const val EXCLUDED_TAG_SEPARATOR = "\u001F"

fun parseSearchSyntax(value: String): SearchQuerySyntax {
    val includes = mutableListOf<String>()
    val excludes = mutableListOf<String>()
    val token = StringBuilder()
    var mode = SearchTokenMode.Include

    fun flush() {
        val text = token.toString().trim()
        if (text.isNotBlank()) {
            if (mode == SearchTokenMode.Exclude) {
                excludes += text
            } else {
                includes += text
            }
        }
        token.clear()
        mode = SearchTokenMode.Include
    }

    value.forEach { char ->
        when {
            char == '+' || char.isWhitespace() -> flush()
            char == '-' -> {
                flush()
                mode = SearchTokenMode.Exclude
            }
            else -> token.append(char)
        }
    }
    flush()

    return SearchQuerySyntax(
        includes = normalizeBlockedTagList(includes),
        excludes = normalizeBlockedTagList(excludes),
    )
}

fun searchContentWithoutExcludedTags(value: String): String {
    val includes = mutableListOf<String>()
    val token = StringBuilder()
    var mode = SearchTokenMode.Include
    var hasPlusPrefix = false

    fun flush() {
        val text = token.toString().trim()
        if (text.isNotBlank() && mode == SearchTokenMode.Include) {
            includes += if (hasPlusPrefix) "+$text" else text
        }
        token.clear()
        mode = SearchTokenMode.Include
        hasPlusPrefix = false
    }

    value.forEach { char ->
        when {
            char.isWhitespace() -> flush()
            char == '+' -> {
                flush()
                mode = SearchTokenMode.Include
                hasPlusPrefix = true
            }
            char == '-' -> {
                flush()
                mode = SearchTokenMode.Exclude
                hasPlusPrefix = false
            }
            else -> token.append(char)
        }
    }
    flush()

    return includes.joinToString(" ")
}

fun normalizeSearchExcludedTags(tags: List<String>): List<String> {
    return normalizeBlockedTagList(tags.map(::normalizeSearchTagOperand))
}

fun serializeExcludedTags(tags: List<String>): String {
    return normalizeSearchExcludedTags(tags).joinToString(EXCLUDED_TAG_SEPARATOR)
}

fun deserializeExcludedTags(value: String?): List<String> {
    if (value.isNullOrBlank()) return emptyList()
    return normalizeSearchExcludedTags(value.split(EXCLUDED_TAG_SEPARATOR))
}

private fun normalizeSearchTagOperand(value: String): String {
    return value.trim()
        .trimStart('+', '-')
        .trim()
}

private enum class SearchTokenMode {
    Include,
    Exclude
}
