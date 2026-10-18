package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicRankingFilter
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.utils.filterBlockedTags

class ComicRankingPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: ComicRankingFilter,
    private val blockedTags: List<String>,
) : PagingSource<Int, Comic>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val page = params.key ?: 1
        val result = filter.category?.let { category ->
            comicRepository.getComicCategoryRankList(page, category, filter.period)
        } ?: comicRepository.getComicTagRankList(page, filter.tagName, filter.period)

        return when (result) {
            is NetWorkResult.Error -> LoadResult.Error(Exception(result.message))
            is NetWorkResult.Success<ComicListResponse> -> {
                val response = result.data
                val remoteItems = response.content
                val comics = response.toComicList().filterBlockedTags(blockedTags)
                val total = response.total.toLongOrNull()
                val hasMore = when {
                    remoteItems.isEmpty() -> false
                    total != null && total > 0L -> page.toLong() * REMOTE_PAGE_SIZE < total
                    else -> remoteItems.size >= REMOTE_PAGE_SIZE
                }
                LoadResult.Page(
                    data = comics,
                    prevKey = if (page == 1) null else page - 1,
                    // Determine continuation from the successful remote page, not
                    // the locally filtered list; a page may validly filter to zero.
                    nextKey = if (hasMore) page + 1 else null,
                )
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null

    private companion object {
        const val REMOTE_PAGE_SIZE = 20L
    }
}
