package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.utils.filterBlockedTags
import com.par9uet.jm.utils.normalizeSearchExcludedTags
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class SearchComicFilter(
    val order: ComicSearchOrderFilter = ComicSearchOrderFilter.NEWEST,
    val searchContent: String = "",
    val excludedTags: List<String> = emptyList(),
)

class SearchComicPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: SearchComicFilter,
    private val onFindSingleComicId: (id: Int?) -> Unit = {}
) : PagingSource<Int, Comic>() {
    companion object {
        private const val DETAIL_FILTER_BATCH_SIZE = 6
    }

    private val detailBlockedCache = mutableMapOf<Int, Boolean>()

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
                    if (isComicBlockedByDetail(redirectId, excludedTags.toTagSet())) {
                        onFindSingleComicId(null)
                    } else {
                        onFindSingleComicId(redirectId)
                    }
                    LoadResult.Page(
                        data = listOf(),
                        prevKey = null,
                        nextKey = null
                    )
                } else {
                    onFindSingleComicId(null)
                    val list = filterExcludedComics(data.data.toComicList(), excludedTags)
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

    private suspend fun filterExcludedComics(
        candidates: List<Comic>,
        excludedTags: List<String>
    ): List<Comic> {
        if (candidates.isEmpty() || excludedTags.isEmpty()) return candidates

        val list = candidates.filterBlockedTags(excludedTags)
        if (excludedTags.size <= 1 || list.isEmpty()) return list

        return filterByDetailTags(list, excludedTags.toTagSet())
    }

    private suspend fun filterByDetailTags(
        candidates: List<Comic>,
        excludedTagSet: Set<String>
    ): List<Comic> {
        val result = mutableListOf<Comic>()
        candidates.chunked(DETAIL_FILTER_BATCH_SIZE).forEach { chunk ->
            val checkedChunk = coroutineScope {
                chunk.map { comic ->
                    async {
                        comic to isComicBlockedByDetail(comic.id, excludedTagSet)
                    }
                }.awaitAll()
            }
            result += checkedChunk
                .filterNot { (_, isBlocked) -> isBlocked }
                .map { (comic, _) -> comic }
        }
        return result
    }

    private suspend fun isComicBlockedByDetail(
        comicId: Int,
        excludedTagSet: Set<String>
    ): Boolean {
        if (excludedTagSet.isEmpty()) return false
        detailBlockedCache[comicId]?.let { return it }

        val isBlocked = when (val detail = comicRepository.getComicDetail(comicId)) {
            is NetWorkResult.Success<ComicDetailResponse> -> detail.data.containsAnyExcludedTag(excludedTagSet)
            is NetWorkResult.Error -> false
        }
        detailBlockedCache[comicId] = isBlocked
        return isBlocked
    }

    private fun buildSearchQuery(searchContent: String, excludedTags: List<String>): String {
        val baseQuery = searchContent.trim().replace(Regex("\\s+"), " ")
        val excludedQuery = excludedTags.joinToString(" ") { tag -> "-$tag" }
        return listOf(baseQuery, excludedQuery)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun List<String>.toTagSet(): Set<String> {
        return normalizeSearchExcludedTags(this).map { it.toTagKey() }.toSet()
    }

    private fun ComicDetailResponse.containsAnyExcludedTag(excludedTagSet: Set<String>): Boolean {
        return (tags + hidden_tags.orEmpty() + actors + works)
            .map { it.toTagKey() }
            .any { it in excludedTagSet }
    }

    private fun String.toTagKey(): String {
        return trim().lowercase()
    }
}
