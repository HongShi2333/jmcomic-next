package com.par9uet.jm.repository

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicTagFilterTest {
    @Test
    fun cachesHiddenTagsAndReusesThemForLaterFiltering() = runBlocking {
        var fetchCount = 0
        val storage = MemoryTagCache()
        val filter = ComicTagFilter(
            fetchDetail = {
                fetchCount++
                NetWorkResult.Success(
                    ComicDetailResponse(
                        id = 1,
                        name = "测试漫画",
                        description = "",
                        author = listOf("作者"),
                        total_views = 0,
                        likes = 0,
                        comment_total = 0,
                        tags = listOf("可见标签"),
                        hidden_tags = listOf("隐藏标签"),
                        actors = listOf("角色"),
                        works = listOf("作品"),
                        is_favorite = false,
                        liked = false,
                        related_list = emptyList(),
                        series = emptyList(),
                        series_id = "",
                        price = "0",
                        purchased = false,
                    ),
                )
            },
            storage = storage,
        )
        val candidate = Comic.create(1, "测试漫画", listOf("作者"))

        val blocked = filter.filter(listOf(candidate), listOf("隐藏标签"), "test")
        assertTrue(blocked.comics.isEmpty())
        assertEquals(1, fetchCount)

        val allowed = filter.filter(listOf(candidate), listOf("不存在的标签"), "test")
        assertEquals(listOf(1), allowed.comics.map { it.id })
        assertEquals(listOf("可见标签", "隐藏标签"), allowed.comics.single().tagList)
        assertEquals(1, fetchCount)
    }

    private class MemoryTagCache : ComicTagCache {
        private var entries: Map<String, CachedComicTags> = emptyMap()

        override suspend fun read(): Map<String, CachedComicTags> = entries

        override suspend fun write(entries: Map<String, CachedComicTags>) {
            this.entries = entries
        }
    }
}
