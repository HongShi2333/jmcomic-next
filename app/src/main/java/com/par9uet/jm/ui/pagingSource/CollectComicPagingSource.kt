package com.par9uet.jm.ui.pagingSource

import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.TagFilterLogic
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
    private val selectedRoles: Set<String> = emptySet(),
    private val selectedAuthors: Set<String> = emptySet(),
    private val selectedTypes: Set<String> = emptySet(),
    private val folderId: Int = 0,
    private val tagLogic: TagFilterLogic = TagFilterLogic.AND,
    private val preloadedFirstPage: UserCollectComicListResponse? = null,
    private val warmPage: (suspend (Int) -> NetWorkResult<UserCollectComicListResponse>)? = null,
) : ComicRemotePagingSource() {
    override val supportsNetworkPreload: Boolean
        get() = selectedTags.isEmpty() && selectedRoles.isEmpty()

    override val hasLocalFilters: Boolean
        get() = blockedTagList.isNotEmpty() || searchText.isNotBlank() ||
            selectedTags.isNotEmpty() || selectedRoles.isNotEmpty() ||
            selectedAuthors.isNotEmpty() || selectedTypes.isNotEmpty()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        // 作者与类型来自收藏摘要；标签和角色则需要详情数据才能保证筛选结果完整。
        val requiresDetailMetadata = selectedTags.isNotEmpty() || selectedRoles.isNotEmpty()
        val response = if (warmPage != null && currentPage in 1..3 && !requiresDetailMetadata) {
            when (val result = warmPage.invoke(currentPage)) {
                is NetWorkResult.Success -> result.data
                is NetWorkResult.Error -> return LoadResult.Error(Exception(result.message))
            }
        } else if (currentPage == 1 && preloadedFirstPage != null && !requiresDetailMetadata) {
            preloadedFirstPage
        } else {
            when (val result = userRepository.getCollectComicList(
                page = currentPage,
                order = order,
                folderId = folderId,
                enrichTags = requiresDetailMetadata,
            )) {
                is NetWorkResult.Error -> return LoadResult.Error(Exception(result.message))
                is NetWorkResult.Success -> result.data
            }
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

        val list = response.toComicList()
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
        return remotePage(
            page = currentPage,
            data = list,
            remoteCount = response.list.size,
            total = response.total.takeIf { it >= 0 },
        )
    }
}
