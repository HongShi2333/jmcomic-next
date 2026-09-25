package com.par9uet.jm.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.TagFilterLogic
import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import com.par9uet.jm.utils.filterBlockedTags
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CollectComicPagingSource(
    private val userRepository: UserRepository,
    private val order: CollectComicOrderFilter,
    private val blockedTagList: List<String> = listOf(),
    private val searchText: String = "",
    private val selectedTags: Set<String> = emptySet(),
    private val selectedRoles: Set<String> = emptySet(),
    private val selectedAuthors: Set<String> = emptySet(),
    private val selectedTypes: Set<String> = emptySet(),
    private val folderId: Int = 0,
    private val tagLogic: TagFilterLogic = TagFilterLogic.AND,
    private val preloadedFirstPage: UserCollectComicListResponse? = null,
) : PagingSource<Int, Comic>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        // 作者与类型来自收藏摘要；标签和角色则需要详情数据才能保证筛选结果完整。
        val requiresDetailMetadata = selectedTags.isNotEmpty() || selectedRoles.isNotEmpty()
        val pageResults = coroutineScope {
            (0 until PAGE_BATCH_SIZE).map { offset ->
                val page = currentPage + offset
                async {
                    if (page == 1 && preloadedFirstPage != null) {
                        NetWorkResult.Success(preloadedFirstPage)
                    } else {
                        userRepository.getCollectComicList(
                            page = page,
                            order = order,
                            folderId = folderId,
                            enrichTags = requiresDetailMetadata,
                        )
                    }
                }
            }.awaitAll()
        }

        val failedPage = pageResults.filterIsInstance<NetWorkResult.Error>().firstOrNull()
        if (failedPage != null) {
            return LoadResult.Error(Exception(failedPage.message))
        }

        val successfulPages = pageResults.filterIsInstance<NetWorkResult.Success<UserCollectComicListResponse>>()
        if (successfulPages.isEmpty()) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = if (currentPage == 1) null else currentPage - PAGE_BATCH_SIZE,
                nextKey = null,
            )
        }

        val query = searchText.trim()
        val lowerSelectedTags = selectedTags.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()
        val lowerSelectedRoles = selectedRoles.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()
        val lowerSelectedAuthors = selectedAuthors.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()
        val lowerSelectedTypes = selectedTypes.map { it.lowercase().trim() }.filter { it.isNotBlank() }.toSet()
        val selectedGroups = listOf(
            lowerSelectedTags to Comic::tagList,
            lowerSelectedRoles to Comic::roleList,
            lowerSelectedAuthors to Comic::authorList,
            lowerSelectedTypes to Comic::typeList,
        ).filter { it.first.isNotEmpty() }

        val list = successfulPages.flatMap { pageData ->
            pageData.data.toComicList()
                .filterBlockedTags(blockedTagList)
                .filter { comic ->
                    // 顶部搜索支持按漫画名、作者或标签匹配
                    query.isBlank() ||
                        comic.name.contains(query, ignoreCase = true) ||
                        comic.authorList.any { it.contains(query, ignoreCase = true) } ||
                        comic.tagList.any { it.contains(query, ignoreCase = true) } ||
                        comic.roleList.any { it.contains(query, ignoreCase = true) } ||
                        comic.typeList.any { it.contains(query, ignoreCase = true) }
                }
                .filter { comic ->
                    if (selectedGroups.isEmpty()) {
                        true
                    } else {
                        val selectedValues = selectedGroups.flatMap { (selected, valuesSelector) ->
                            val values = valuesSelector(comic).map { it.lowercase().trim() }.toSet()
                            selected.map { value -> value to values }
                        }
                        when (tagLogic) {
                            TagFilterLogic.AND -> selectedValues.all { (value, values) -> value in values }
                            TagFilterLogic.OR -> selectedValues.any { (value, values) -> value in values }
                            TagFilterLogic.NOT -> selectedValues.none { (value, values) -> value in values }
                        }
                    }
                }
        }

        // 服务端收藏接口固定按 20 条分页。合并请求后以实际返回条数判断末页，
        // 不依赖内置 API 中语义不稳定的 total/count 字段。
        val reachedEnd = successfulPages.any { it.data.list.size < REMOTE_PAGE_SIZE }
        return LoadResult.Page(
            data = list,
            prevKey = if (currentPage == 1) null else (currentPage - PAGE_BATCH_SIZE).coerceAtLeast(1),
            nextKey = if (reachedEnd) null else currentPage + successfulPages.size,
        )
    }

    private companion object {
        const val REMOTE_PAGE_SIZE = 20
        const val PAGE_BATCH_SIZE = 3
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
}
