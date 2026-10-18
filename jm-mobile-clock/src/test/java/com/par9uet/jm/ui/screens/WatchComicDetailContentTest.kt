package com.par9uet.jm.ui.screens

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter
import org.junit.Assert.*
import org.junit.Test

class WatchComicDetailContentTest {
    @Test fun detailsKeepEveryGroupWithoutMixingAuthorsRolesWorksAndTags() {
        val comic = Comic.create(42, "完整标题", listOf("作者甲", "作者乙")).copy(
            tagList = listOf("全彩", "中文", "全彩", " "),
            roleList = listOf("角色甲"), workList = listOf("作品甲"), typeList = listOf("单本"),
        )
        val groups = watchComicInfoGroups(comic).associateBy { it.title }
        assertEquals(listOf("作者甲", "作者乙"), groups["作者"]!!.values)
        assertEquals(listOf("全彩", "中文"), groups["标签"]!!.values)
        assertEquals(listOf("角色甲"), groups["角色"]!!.values)
        assertEquals(listOf("作品甲"), groups["作品"]!!.values)
        assertEquals(listOf("单本"), groups["分类"]!!.values)
        assertTrue(groups["标签"]!!.contentTags)
        assertFalse(groups["作者"]!!.contentTags)
    }
    @Test fun singleChapterComicCanStillBeOpenedFromTheChapterDirectory() {
        val comic = Comic.create(42, "单篇标题", emptyList())
        assertEquals(listOf(ComicChapter(42, "单篇标题")), watchDetailChapters(comic))
    }
    @Test fun longSeriesKeepsAllChapterIdsAndNames() {
        val chapters = (1..105).map { ComicChapter(1000 + it, "第$it 话") }
        val comic = Comic.create(42, "系列标题", emptyList()).copy(comicChapterList = chapters)
        assertEquals(chapters, watchDetailChapters(comic))
    }
}
