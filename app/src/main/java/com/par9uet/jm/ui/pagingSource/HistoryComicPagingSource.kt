package com.par9uet.jm.ui.pagingSource

import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.UserHistoryComicListResponse
import com.par9uet.jm.utils.filterBlockedTags

class HistoryComicPagingSource(
    private val userRepository: UserRepository,
    private val blockedTagList: List<String> = listOf(),
) : ComicRemotePagingSource() {
    override val hasLocalFilters: Boolean get() = blockedTagList.isNotEmpty()
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        return when (val data =
            userRepository.getHistoryComicList(currentPage)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<UserHistoryComicListResponse> -> {
                val list = data.data.toComicList().filterBlockedTags(blockedTagList)
                remotePage(
                    page = currentPage,
                    data = list,
                    remoteCount = data.data.list.size,
                    total = data.data.total.takeIf { it >= 0 },
                )
            }
        }
    }

}
