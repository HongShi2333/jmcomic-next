package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.ComicListResponse
import com.par9uet.jm.retrofit.model.NetWorkResult

data class SearchComicFilter(
    val order: ComicSearchOrderFilter = ComicSearchOrderFilter.NEWEST,
    val searchContent: String = "",
)

data class ParsedSearchQuery(
    val includes: List<String>,
    val excludes: List<String>,
) {
    val primaryQuery: String = includes.firstOrNull().orEmpty()
    val secondaryIncludes: List<String> = includes.drop(1)
    val usesTagFilter: Boolean = secondaryIncludes.isNotEmpty() || excludes.isNotEmpty()
}

class SearchComicPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: SearchComicFilter,
    private val onFindSingleComicId: (id: Int?) -> Unit = {}
) : PagingSource<Int, Comic>() {
    companion object {
        private const val TAG_FILTER_SCAN_PAGES = 4
    }

    private val tagIdCache = mutableMapOf<String, Set<Int>>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        val parsedQuery = parseSearchQuery(filter.searchContent)
        val primaryQuery = parsedQuery.primaryQuery.ifBlank { filter.searchContent }
        return when (val data =
            comicRepository.getComicList(currentPage, filter.order, primaryQuery)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<ComicListResponse> -> {
                if (data.data.redirect_aid != null) {
                    onFindSingleComicId(data.data.redirect_aid.toInt())
                    LoadResult.Page(
                        data = listOf(),
                        prevKey = null,
                        nextKey = null
                    )
                } else {
                    onFindSingleComicId(null)
                    val list = applyTagFilter(data.data.toComicList(), parsedQuery)
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

    private suspend fun applyTagFilter(
        candidates: List<Comic>,
        query: ParsedSearchQuery
    ): List<Comic> {
        if (!query.usesTagFilter || candidates.isEmpty()) return candidates

        val includeIdSets = query.secondaryIncludes.map { term ->
            fetchIdsForTerm(term)
        }
        val excludeIds = query.excludes.flatMapTo(mutableSetOf()) { term ->
            fetchIdsForTerm(term)
        }

        return candidates.filter { comic ->
            includeIdSets.all { ids -> comic.id in ids } && comic.id !in excludeIds
        }
    }

    private suspend fun fetchIdsForTerm(term: String): Set<Int> {
        if (term.isBlank()) return emptySet()
        tagIdCache[term]?.let { return it }

        val ids = mutableSetOf<Int>()
        for (page in 1..TAG_FILTER_SCAN_PAGES) {
            when (val data = comicRepository.getComicList(page, filter.order, term)) {
                is NetWorkResult.Error -> {
                    tagIdCache[term] = ids
                    return ids
                }
                is NetWorkResult.Success<ComicListResponse> -> {
                    data.data.redirect_aid?.toIntOrNull()?.let {
                        ids += it
                        tagIdCache[term] = ids
                        return ids
                    }
                    val comics = data.data.toComicList()
                    if (comics.isEmpty()) {
                        tagIdCache[term] = ids
                        return ids
                    }
                    ids += comics.map { it.id }
                }
            }
        }
        tagIdCache[term] = ids
        return ids
    }
}

fun parseSearchQuery(value: String): ParsedSearchQuery {
    val includes = mutableListOf<String>()
    val excludes = mutableListOf<String>()
    val token = StringBuilder()
    var mode = SearchTokenMode.Include

    fun flush() {
        val text = token.toString().trim()
        if (text.isNotBlank()) {
            if (mode == SearchTokenMode.Exclude) {
                excludes += text
            } else {
                includes += text
            }
        }
        token.clear()
        mode = SearchTokenMode.Include
    }

    value.forEach { char ->
        when {
            char == '+' || char.isWhitespace() -> flush()
            char == '-' -> {
                flush()
                mode = SearchTokenMode.Exclude
            }
            else -> token.append(char)
        }
    }
    flush()

    val fallback = value.trim().takeIf { it.isNotBlank() && includes.isEmpty() && excludes.isEmpty() }
    return ParsedSearchQuery(
        includes = includes.ifEmpty { fallback?.let { listOf(it) } ?: emptyList() },
        excludes = excludes
    )
}

private enum class SearchTokenMode {
    Include,
    Exclude
}
