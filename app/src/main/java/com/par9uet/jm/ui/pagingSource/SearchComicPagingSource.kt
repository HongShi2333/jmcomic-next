package com.par9uet.jm.ui.pagingSource

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.data.models.ComicSearchRequest
import com.par9uet.jm.network.TIMED_WEB_PAGE_SIZE
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.repository.ComicTagFilter
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.utils.filterBlockedTags
import com.par9uet.jm.utils.normalizeSearchExcludedTags

data class SearchComicFilter(
    val order: ComicSearchOrderFilter = ComicSearchOrderFilter.NEWEST,
    val searchContent: String = "",
    val excludedTags: List<String> = emptyList(),
    val mainTag: Int = 0,
    val apiSource: String = "",
)

class SearchComicPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: SearchComicFilter,
    private val onFindSingleComicId: (id: Int?) -> Unit = {},
    private val tagFilter: ComicTagFilter? = null,
    private val tagSource: String = "default",
) : ComicRemotePagingSource() {
    override val hasLocalFilters: Boolean get() = filter.excludedTags.isNotEmpty()
    override val remotePageSize: Int
        get() = if (ComicSearchRequest.from(
            buildSearchQuery(filter.searchContent, normalizeSearchExcludedTags(filter.excludedTags)),
            filter.mainTag, filter.order,
        ).usesWebTimeRanking) TIMED_WEB_PAGE_SIZE else 80
    override val supportsNetworkPreload: Boolean
        get() = normalizeSearchExcludedTags(filter.excludedTags).size <= 1
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        val excludedTags = normalizeSearchExcludedTags(filter.excludedTags)
        val searchQuery = buildSearchQuery(filter.searchContent, excludedTags)
        return when (val data =
            comicRepository.getComicListWithMainTag(
                page = currentPage,
                order = filter.order,
                searchContent = searchQuery,
                mainTag = filter.mainTag,
            )) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<ComicListResponse> -> {
                if (data.data.redirect_aid != null) {
                    val redirectId = data.data.redirect_aid.toInt()
                    val allowed = if (excludedTags.isEmpty()) {
                        true
                    } else {
                        (tagFilter ?: ComicTagFilter(comicRepository::getComicDetail))
                            .filter(
                                candidates = listOf(Comic.create(redirectId, "", emptyList())),
                                excludedTags = excludedTags,
                                source = tagSource,
                            )
                            .comics
                            .isNotEmpty()
                    }
                    if (currentPage == 1) {
                        onFindSingleComicId(redirectId.takeIf { allowed })
                    }
                    LoadResult.Page(
                        data = listOf(),
                        prevKey = null,
                        nextKey = null
                    )
                } else {
                    if (currentPage == 1) onFindSingleComicId(null)
                    val list = filterExcludedComics(data.data.toComicList(), excludedTags)
                    remotePage(
                        page = currentPage,
                        data = list,
                        remoteCount = data.data.content.size,
                        total = data.data.total.toIntOrNull(),
                    )
                }
            }
        }
    }

    private suspend fun filterExcludedComics(
        candidates: List<Comic>,
        excludedTags: List<String>
    ): List<Comic> {
        if (candidates.isEmpty() || excludedTags.isEmpty()) return candidates

        val list = candidates.filterBlockedTags(excludedTags)
        if (excludedTags.size <= 1 || list.isEmpty()) return list

        return (tagFilter ?: ComicTagFilter(comicRepository::getComicDetail))
            .filter(list, excludedTags, tagSource)
            .comics
    }

    private fun buildSearchQuery(searchContent: String, excludedTags: List<String>): String {
        val baseQuery = searchContent.trim().replace(Regex("\\s+"), " ")
        val excludedQuery = excludedTags.joinToString(" ") { tag -> "-$tag" }
        return listOf(baseQuery, excludedQuery)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

}
