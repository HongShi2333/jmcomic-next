package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.repository.ComicTagFilter
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.utils.normalizeSearchExcludedTags

data class SearchComicFilter(
    val order: ComicSearchOrderFilter = ComicSearchOrderFilter.NEWEST,
    val searchContent: String = "",
    val excludedTags: List<String> = emptyList(),
)

class SearchComicPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: SearchComicFilter,
    private val tagFilter: ComicTagFilter = ComicTagFilter(comicRepository::getComicDetail),
    private val tagSource: String = "default",
    private val onFindSingleComicId: (id: Int?) -> Unit = {}
) : PagingSource<Int, Comic>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        val excludedTags = normalizeSearchExcludedTags(filter.excludedTags)
        val searchQuery = buildSearchQuery(filter.searchContent, excludedTags)
        return when (val data =
            comicRepository.getComicList(currentPage, filter.order, searchQuery)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<ComicListResponse> -> {
                if (data.data.redirect_aid != null) {
                    val redirectId = data.data.redirect_aid.toInt()
                    val checked = tagFilter.filter(
                        listOf(Comic.create(redirectId, "", emptyList())), excludedTags, tagSource,
                    )
                    if (checked.unresolvedCount > 0) {
                        onFindSingleComicId(null)
                        return LoadResult.Error(Exception("漫画标签检查失败，请重试"))
                    }
                    onFindSingleComicId(redirectId.takeIf { checked.comics.isNotEmpty() })
                    LoadResult.Page(
                        data = listOf(),
                        prevKey = null,
                        nextKey = null
                    )
                } else {
                    onFindSingleComicId(null)
                    val checked = tagFilter.filter(data.data.toComicList(), excludedTags, tagSource)
                    if (checked.comics.isEmpty() && checked.unresolvedCount > 0) {
                        return LoadResult.Error(Exception("漫画标签检查失败，请重试"))
                    }
                    val list = checked.comics
                    val total = data.data.total.toInt()
                    val isLastPage = currentPage >= (total + params.loadSize - 1) / params.loadSize
                    LoadResult.Page(
                        data = list,
                        prevKey = if (currentPage == 1) null else currentPage - 1,
                        nextKey = if (isLastPage) null else currentPage + 1
                    )
                }
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null

    private fun buildSearchQuery(searchContent: String, excludedTags: List<String>): String {
        val baseQuery = searchContent.trim().replace(Regex("\\s+"), " ")
        val excludedQuery = excludedTags.joinToString(" ") { tag -> "-$tag" }
        return listOf(baseQuery, excludedQuery)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

}
