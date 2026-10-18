package com.par9uet.jm.ui.pagingSource

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicRankingFilter
import com.par9uet.jm.data.models.ComicRankCategory
import com.par9uet.jm.network.TIMED_WEB_PAGE_SIZE
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.utils.filterBlockedTags

class ComicRankingPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: ComicRankingFilter,
    private val blockedTags: List<String>,
) : ComicRemotePagingSource() {
    override val hasLocalFilters: Boolean get() = blockedTags.isNotEmpty()
    override val remotePageSize: Int
        get() = if ((filter.category ?: ComicRankCategory.fromTagLabel(filter.tagName))
                ?.supportsCategoryRanking == true) 80 else TIMED_WEB_PAGE_SIZE
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
                remotePage(
                    page = page,
                    data = comics,
                    remoteCount = remoteItems.size,
                    total = response.total.toIntOrNull(),
                )
            }
        }
    }

}
