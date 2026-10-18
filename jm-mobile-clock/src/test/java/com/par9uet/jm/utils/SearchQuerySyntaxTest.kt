package com.par9uet.jm.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQuerySyntaxTest {
    @Test
    fun parsesMultipleExcludedTags() {
        val syntax = parseSearchSyntax("artist +tag -a -b")

        assertEquals(listOf("artist", "tag"), syntax.includes)
        assertEquals(listOf("a", "b"), syntax.excludes)
    }

    @Test
    fun extractsEditableSearchContentWithoutExcludedTags() {
        val content = searchContentWithoutExcludedTags("artist +tag -a -b")

        assertEquals("artist +tag", content)
    }

    @Test
    fun serializesAndDeserializesHiddenExcludedTags() {
        val serialized = serializeExcludedTags(listOf("a", "-b", "A"))

        assertEquals(listOf("a", "b"), deserializeExcludedTags(serialized))
    }
}
