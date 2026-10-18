package com.par9uet.jm.cache

import com.par9uet.jm.data.models.Comic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicCoverPreloaderTest {
    @Test
    fun disabledCacheDoesNotPreloadAnyCovers() {
        val comics = listOf(Comic.create(1, "Comic", emptyList()))
        assertTrue(comicCoverPreloadCandidates(comics, 0).isEmpty())
        assertTrue(comicCoverPreloadCandidates(comics, -1).isEmpty())
    }

    @Test
    fun coverPreloadIsCappedAtOneDisplayPage() {
        val comics = (1..200).map { Comic.create(it, "Comic $it", emptyList()) }
        assertEquals((1..40).toList(), comicCoverPreloadCandidates(comics, 72).map { it.id })
    }

    @Test
    fun invalidAndDuplicateIdsAreSkippedBeforeApplyingLimit() {
        val ids = listOf(-1, 0, 1, 1, 2) + (3..80).toList()
        val comics = ids.map { Comic.create(it, "Comic $it", emptyList()) }
        assertEquals((1..40).toList(), comicCoverPreloadCandidates(comics, 24).map { it.id })
    }
}
