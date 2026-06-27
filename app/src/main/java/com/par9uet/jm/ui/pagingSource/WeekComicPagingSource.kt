package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.WeekRecommendComicResponse
import com.par9uet.jm.utils.filterBlockedTags

data class WeekFilter(
    val categoryId: String? = null,
    val typeId: String? = null,
)

class WeekComicPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: WeekFilter,
    private val blockedTagList: List<String> = listOf(),
) : PagingSource<Int, Comic>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        if (filter.categoryId == null || filter.typeId == null) {
            return LoadResult.Page(
                data = listOf(),
                prevKey = null,
                nextKey = null
            )
        }
        return when (val data = comicRepository.getWeekRecommendComicList(
            currentPage,
            filter.categoryId,
            filter.typeId
        )) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<WeekRecommendComicResponse> -> {
                val list = data.data.toComicList().filterBlockedTags(blockedTagList)
                val total = data.data.total
                val isLastPage = currentPage >= (total + params.loadSize - 1) / params.loadSize
                LoadResult.Page(
                    data = list,
                    prevKey = if (currentPage == 1) null else currentPage - 1,
                    nextKey = if (isLastPage) null else currentPage + 1
                )
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
}
