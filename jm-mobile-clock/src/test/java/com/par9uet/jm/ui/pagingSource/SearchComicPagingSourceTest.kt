package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicRankCategory
import com.par9uet.jm.data.models.ComicRankPeriod
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.CollectComicResponse
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.ComicDetailRelatedListItemResponse
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.ComicPicListResponse
import com.par9uet.jm.retrofit.model.CommentComicResponse
import com.par9uet.jm.retrofit.model.CommentListResponse
import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.retrofit.model.LikeComicResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.WeekRecommendComicResponse
import com.par9uet.jm.retrofit.model.WeekResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchComicPagingSourceTest {
    @Test
    fun filtersMultipleExcludedTagsByDetail() = runBlocking {
        val repository = FakeComicRepository()
        val source = SearchComicPagingSource(
            comicRepository = repository,
            filter = SearchComicFilter(
                searchContent = "artist",
                excludedTags = listOf("a", "b")
            )
        )

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val page = result as PagingSource.LoadResult.Page<Int, Comic>
        assertEquals("artist -a -b", repository.lastSearchContent)
        assertEquals(listOf(2), page.data.map { it.id })
    }

    private class FakeComicRepository : ComicRepository {
        var lastSearchContent: String? = null

        override suspend fun getComicList(
            page: Int,
            order: ComicSearchOrderFilter,
            searchContent: String
        ): NetWorkResult<ComicListResponse> {
            lastSearchContent = searchContent
            return NetWorkResult.Success(
                ComicListResponse(
                    search_query = searchContent,
                    total = "2",
                    redirect_aid = null,
                    content = listOf(
                        contentItem(id = 1),
                        contentItem(id = 2)
                    )
                )
            )
        }

        override suspend fun getComicDetail(id: Int): NetWorkResult<ComicDetailResponse> {
            return NetWorkResult.Success(
                detail(
                    id = id,
                    tags = if (id == 1) listOf("a") else listOf("c")
                )
            )
        }

        override suspend fun getComicIdsByTag(tagName: String, maxPages: Int): Set<Int> {
            error("getComicIdsByTag should not be used for search exclusions")
        }

        override suspend fun likeComic(id: Int): NetWorkResult<LikeComicResponse> = unused()

        override suspend fun collectComic(id: Int): NetWorkResult<CollectComicResponse> = unused()

        override suspend fun unCollectComic(id: Int): NetWorkResult<CollectComicResponse> = unused()

        override suspend fun getHomeSwiperComicList(): NetWorkResult<List<HomeSwiperComicListItemResponse>> = unused()

        override suspend fun getComicPicList(id: Int, shunt: String): NetWorkResult<ComicPicListResponse> = unused()

        override suspend fun downloadImageBytes(comicId: Int, imageIndex: Int): ByteArray? = unused()

        override suspend fun getComicTagRankList(
            page: Int,
            tagName: String,
            period: ComicRankPeriod,
        ): NetWorkResult<ComicListResponse> = unused()

        override suspend fun getComicCategoryRankList(
            page: Int,
            category: ComicRankCategory,
            period: ComicRankPeriod,
        ): NetWorkResult<ComicListResponse> = unused()

        override suspend fun getWeekData(): NetWorkResult<WeekResponse> = unused()

        override suspend fun getWeekRecommendComicList(
            page: Int,
            categoryId: String,
            typeId: String
        ): NetWorkResult<WeekRecommendComicResponse> = unused()

        override suspend fun getCommentList(page: Int, comicId: Int): NetWorkResult<CommentListResponse> = unused()

        override suspend fun comment(
            content: String,
            comicId: Int,
            commentId: Int?
        ): NetWorkResult<CommentComicResponse> = unused()

        override suspend fun likeComment(commentId: Int): NetWorkResult<CommentComicResponse> = unused()

        override suspend fun createFavoriteFolder(name: String): NetWorkResult<Unit> = unused()

        override suspend fun deleteFavoriteFolder(folderId: String): NetWorkResult<Unit> = unused()

        override suspend fun renameFavoriteFolder(folderId: String, newName: String): NetWorkResult<Unit> = unused()

        override suspend fun moveComicToFolder(comicId: Int, folderId: String): NetWorkResult<Unit> = unused()

        private fun contentItem(id: Int): ComicListResponse.ContentListItem {
            val category = ComicListResponse.ContentListItem.Category(id = null, title = "category")
            return ComicListResponse.ContentListItem(
                id = id.toString(),
                author = "author",
                description = "",
                name = "comic $id",
                image = "",
                category = category,
                category_sub = category,
                liked = false,
                is_favorite = false,
                update_at = 0,
                tags = null
            )
        }

        private fun detail(id: Int, tags: List<String>): ComicDetailResponse {
            return ComicDetailResponse(
                id = id,
                name = "comic $id",
                description = "",
                author = listOf("author"),
                total_views = 0,
                likes = 0,
                comment_total = 0,
                tags = tags,
                actors = emptyList(),
                works = emptyList(),
                is_favorite = false,
                liked = false,
                related_list = emptyList<ComicDetailRelatedListItemResponse>(),
                series = emptyList(),
                series_id = "",
                price = "0",
                purchased = false
            )
        }

        private fun unused(): Nothing {
            throw UnsupportedOperationException("Unused fake repository method")
        }
    }
}
