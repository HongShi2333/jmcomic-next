package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import com.par9uet.jm.utils.filterBlockedTags

class CollectComicPagingSource(
    private val userRepository: UserRepository,
    private val order: CollectComicOrderFilter,
    private val blockedTagList: List<String> = listOf(),
    private val searchText: String = "",
    private val selectedTags: Set<String> = emptySet(),
) : PagingSource<Int, Comic>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        return when (val data =
            userRepository.getCollectComicList(currentPage, order)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<UserCollectComicListResponse> -> {
                val query = searchText.trim()
                val list = data.data.toComicList()
                    .filterBlockedTags(blockedTagList)
                    .filter { comic ->
                        query.isBlank() ||
                            comic.name.contains(query, ignoreCase = true) ||
                            comic.authorList.any { it.contains(query, ignoreCase = true) }
                    }
                    .filter { comic ->
                        selectedTags.isEmpty() || selectedTags.all { it in comic.tagList }
                    }
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
