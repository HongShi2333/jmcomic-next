package com.par9uet.jm.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import coil.ImageLoader
import coil.request.ImageRequest
import com.par9uet.jm.cache.applyComicCoverCache
import com.par9uet.jm.cache.comicCoverCacheKey
import com.par9uet.jm.data.models.COMIC_API_SOURCE_NETWORK
import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.SignInData
import com.par9uet.jm.data.models.TagFilterLogic
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.retrofit.model.LoginResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.SignInDataResponse
import com.par9uet.jm.retrofit.model.SignInResponse
import com.par9uet.jm.retrofit.model.UserCollectComicListResponse
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.models.CommonUIState
import com.par9uet.jm.ui.pagingSource.CollectComicPagingSource
import com.par9uet.jm.ui.pagingSource.HistoryComicPagingSource
import com.par9uet.jm.ui.pagingSource.HistoryCommentPagingSource
import com.par9uet.jm.utils.filterBlockedTags
import com.par9uet.jm.utils.log
import com.par9uet.jm.utils.logError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectComicLocalFilter(
    val searchText: String = "",
    val selectedTags: Set<String> = emptySet(),
    val selectedRoles: Set<String> = emptySet(),
    val selectedAuthors: Set<String> = emptySet(),
    val selectedTypes: Set<String> = emptySet(),
    val tagLogic: TagFilterLogic = TagFilterLogic.AND
)

data class CollectEditState(
    val editing: Boolean = false,
    val selectedComicIds: Set<Int> = emptySet()
)

data class HistoryEditState(
    val editing: Boolean = false,
    val selectedComicIds: Set<Int> = emptySet()
)

private data class CollectPagerKey(
    val order: CollectComicOrderFilter,
    val blockedTagList: List<String>,
    val filter: CollectComicLocalFilter,
    val folderId: Int,
    val preloadVersion: Int,
)

class UserViewModel(
    private val userManager: UserManager,
    private val userRepository: UserRepository,
    private val toastManager: ToastManager,
    private val localSettingManager: LocalSettingManager,
    private val comicRepository: ComicRepository,
    private val downloadManager: DownloadManager,
) : ViewModel() {
    private val _loginState = MutableStateFlow(CommonUIState(data = null))
    val loginState = _loginState.asStateFlow()
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = userRepository.login(username, password)) {
                is NetWorkResult.Error -> {
                    _loginState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<LoginResponse> -> {
                    userManager.updateUser(
                        data.data.toUser(
                            password = password
                        )
                    )
                }
            }
            _loginState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    fun logout() {
        favoritePreloadJob?.cancel()
        favoritePreloadJob = null
        favoritePreloadCompleted = false
        _preloadedFavoritePage.value = null
        _favoritePreloadVersion.update { it + 1 }
        viewModelScope.launch {
            userManager.clearUser()
        }
    }

    private val _collectComicOrder = MutableStateFlow(CollectComicOrderFilter.COLLECT_TIME)
    val collectComicOrder = _collectComicOrder.asStateFlow()
    private val _collectComicFilter = MutableStateFlow(CollectComicLocalFilter())
    val collectComicFilter = _collectComicFilter.asStateFlow()
    private val _collectTagCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val collectTagCounts = _collectTagCounts.asStateFlow()
    private val _collectRoleCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val collectRoleCounts = _collectRoleCounts.asStateFlow()
    private val _collectAuthorCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val collectAuthorCounts = _collectAuthorCounts.asStateFlow()
    private val _collectTypeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val collectTypeCounts = _collectTypeCounts.asStateFlow()
    private val _selectedFolderId = MutableStateFlow(0)
    val selectedFolderId = _selectedFolderId.asStateFlow()
    private val _folderList = MutableStateFlow<Map<String, String>>(emptyMap())
    val folderList = _folderList.asStateFlow()
    private val _collectEditState = MutableStateFlow(CollectEditState())
    val collectEditState = _collectEditState.asStateFlow()
    private var collectTagCountJob: Job? = null
    private var collectTagCountKey = ""
    private var favoritePreloadJob: Job? = null
    private var favoritePreloadCompleted = false
    private val _preloadedFavoritePage = MutableStateFlow<UserCollectComicListResponse?>(null)
    private val _favoritePreloadVersion = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val collectComicPager = combine(
        _collectComicOrder,
        localSettingManager.localSettingState,
        _collectComicFilter,
        _selectedFolderId,
        _favoritePreloadVersion,
    ) { order, localSetting, filter, folderId, preloadVersion ->
        CollectPagerKey(order, localSetting.blockedTagList, filter, folderId, preloadVersion)
    }.flatMapLatest { key ->
        Pager(
            // 收藏接口按 20 条分页，PagingSource 会把连续三页合并为一次加载，
            // 让首屏和滚动加载不再被 20 条一断卡住。
            config = PagingConfig(pageSize = 60, prefetchDistance = 12, initialLoadSize = 60),
            pagingSourceFactory = {
                CollectComicPagingSource(
                    userRepository,
                    key.order,
                    key.blockedTagList,
                    key.filter.searchText,
                    key.filter.selectedTags,
                    key.filter.selectedRoles,
                    key.filter.selectedAuthors,
                    key.filter.selectedTypes,
                    key.folderId,
                    key.filter.tagLogic,
                    _preloadedFavoritePage.value.takeIf {
                        key.order == CollectComicOrderFilter.COLLECT_TIME &&
                            key.folderId == 0 &&
                            key.filter.searchText.isBlank() &&
                            key.filter.selectedTags.isEmpty() &&
                            key.filter.selectedRoles.isEmpty() &&
                            key.filter.selectedAuthors.isEmpty() &&
                            key.filter.selectedTypes.isEmpty()
                    }
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    /**
     * 首页首屏完成后预热收藏页：先缓存列表，再并行预取封面，最后补齐详情标签。
     * 预热结果会让收藏页首屏直接复用列表数据，标签补齐后通过版本号触发一次刷新。
     */
    fun preloadFavoritesAfterHome(context: Context, imageLoader: ImageLoader) {
        if ((userManager.userState.value.data?.id ?: 0) <= 0 || favoritePreloadCompleted || favoritePreloadJob?.isActive == true) return
        favoritePreloadJob = viewModelScope.launch(Dispatchers.IO) {
            val order = CollectComicOrderFilter.COLLECT_TIME
            val folderId = 0
            when (val firstPage = userRepository.getCollectComicList(1, order, folderId)) {
                is NetWorkResult.Error -> {
                    favoritePreloadCompleted = false
                    logError("UserViewModel", "预加载收藏列表失败：${firstPage.message}")
                }
                is NetWorkResult.Success -> {
                    _folderList.value = firstPage.data.folder_list ?: emptyMap()
                    _preloadedFavoritePage.value = firstPage.data
                    _favoritePreloadVersion.update { it + 1 }

                    // 列表到达后先并行预取封面，避免进入收藏页时图片请求与列表请求竞争。
                    val coverCacheDuration = localSettingManager.localSettingState.value.coverCacheDurationHours
                    if (coverCacheDuration > 0) coroutineScope {
                        firstPage.data.list
                            .mapNotNull { item ->
                                val comicId = item.id.trim().toIntOrNull()
                                val imageUrl = item.image.trim()
                                if (comicId != null && imageUrl.isNotBlank()) {
                                    comicId to imageUrl
                                } else {
                                    null
                                }
                            }
                            .distinctBy { (comicId, _) -> comicId }
                            .map { (comicId, imageUrl) ->
                                async {
                                    runCatching {
                                        imageLoader.execute(
                                            ImageRequest.Builder(context)
                                                .data(imageUrl)
                                                .applyComicCoverCache(
                                                    comicCoverCacheKey(comicId, coverCacheDuration)
                                                )
                                                .build()
                                        )
                                    }
                                }
                            }
                            .awaitAll()
                    }

                    // 内置/混合 API 的详情标签放到封面预取之后，避免拖慢收藏列表首屏。
                    if (localSettingManager.localSettingState.value.comicApiSource != COMIC_API_SOURCE_NETWORK) {
                        when (val taggedPage = userRepository.getCollectComicList(1, order, folderId, enrichTags = true)) {
                            is NetWorkResult.Success -> {
                                _preloadedFavoritePage.value = taggedPage.data
                                _favoritePreloadVersion.update { it + 1 }
                                favoritePreloadCompleted = true
                                // 若筛选弹窗已先打开过，重新统计会用到这批详情标签。
                                invalidateCollectFilterStats()
                                refreshCollectTagCounts()
                            }
                            is NetWorkResult.Error -> {
                                favoritePreloadCompleted = false
                                logError("UserViewModel", "预加载收藏标签失败：${taggedPage.message}")
                            }
                        }
                    } else {
                        favoritePreloadCompleted = true
                    }
                }
            }
        }
    }

    fun changeCollectComicOrder(order: CollectComicOrderFilter) {
        _collectComicOrder.update {
            order
        }
        invalidateCollectFilterStats()
    }

    fun updateCollectSearchText(value: String) {
        _collectComicFilter.update { it.copy(searchText = value) }
    }

    fun updateCollectSelectedTags(tags: Set<String>) {
        _collectComicFilter.update { it.copy(selectedTags = tags) }
    }

    fun updateCollectSelectedRoles(roles: Set<String>) {
        _collectComicFilter.update { it.copy(selectedRoles = roles) }
    }

    fun updateCollectTagLogic(logic: TagFilterLogic) {
        _collectComicFilter.update { it.copy(tagLogic = logic) }
    }

    fun updateCollectSelectedAuthors(authors: Set<String>) {
        _collectComicFilter.update { it.copy(selectedAuthors = authors) }
    }

    fun updateCollectSelectedTypes(types: Set<String>) {
        _collectComicFilter.update { it.copy(selectedTypes = types) }
    }

    fun changeFolder(folderId: Int) {
        _selectedFolderId.update { folderId }
        invalidateCollectFilterStats()
    }

    fun enterCollectEdit(comicId: Int) {
        _collectEditState.update {
            it.copy(editing = true, selectedComicIds = it.selectedComicIds + comicId)
        }
    }

    fun toggleCollectSelected(comicId: Int) {
        _collectEditState.update {
            val selected = if (comicId in it.selectedComicIds) {
                it.selectedComicIds - comicId
            } else {
                it.selectedComicIds + comicId
            }
            it.copy(editing = selected.isNotEmpty(), selectedComicIds = selected)
        }
    }

    fun clearCollectSelection() {
        _collectEditState.update { CollectEditState() }
    }

    fun deleteCollectedComics(comics: List<Comic>) {
        if (comics.isEmpty()) return
        viewModelScope.launch {
            var success = 0
            var fail = 0
            comics.forEach { comic ->
                when (comicRepository.unCollectComic(comic.id)) {
                    is NetWorkResult.Error -> fail++
                    is NetWorkResult.Success -> success++
                }
            }
            toastManager.showAsync(
                if (fail == 0) "已取消收藏 $success 部漫画"
                else "成功 $success 部，失败 $fail 部"
            )
            clearCollectSelection()
        }
    }

    fun cacheCollectedComics(comics: List<Comic>) {
        if (comics.isEmpty()) return
        downloadManager.downloadComics(comics)
        clearCollectSelection()
    }

    fun moveCollectedToFolder(comics: List<Comic>, folderId: String) {
        if (comics.isEmpty()) return
        viewModelScope.launch {
            var success = 0
            var fail = 0
            comics.forEach { comic ->
                when (comicRepository.moveComicToFolder(comic.id, folderId)) {
                    is NetWorkResult.Error -> fail++
                    is NetWorkResult.Success -> success++
                }
            }
            toastManager.showAsync(
                if (fail == 0) "已移动 $success 部漫画"
                else "成功 $success 部，失败 $fail 部"
            )
            clearCollectSelection()
        }
    }

    fun refreshFolderList() {
        viewModelScope.launch {
            val order = _collectComicOrder.value
            when (val data = userRepository.getCollectComicList(1, order, 0)) {
                is NetWorkResult.Error -> {
                    // 文件夹列表为可选功能，错误时忽略
                }

                is NetWorkResult.Success -> {
                    _folderList.value = data.data.folder_list ?: emptyMap()
                }
            }
        }
    }

    /** Clears the startup preload so a user initiated refresh always hits fresh collection data. */
    fun refreshCollectContent() {
        favoritePreloadJob?.cancel()
        favoritePreloadJob = null
        favoritePreloadCompleted = false
        _preloadedFavoritePage.value = null
        _favoritePreloadVersion.update { it + 1 }
        invalidateCollectFilterStats()
        refreshFolderList()
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            when (val data = comicRepository.createFavoriteFolder(name)) {
                is NetWorkResult.Error -> toastManager.showAsync(data.message)
                is NetWorkResult.Success -> {
                    refreshFolderList()
                    toastManager.showAsync("创建成功")
                }
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            when (val data = comicRepository.deleteFavoriteFolder(folderId)) {
                is NetWorkResult.Error -> toastManager.showAsync(data.message)
                is NetWorkResult.Success -> {
                    _selectedFolderId.update { 0 }
                    refreshFolderList()
                    toastManager.showAsync("删除成功")
                }
            }
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            when (val data = comicRepository.renameFavoriteFolder(folderId, newName)) {
                is NetWorkResult.Error -> toastManager.showAsync(data.message)
                is NetWorkResult.Success -> {
                    refreshFolderList()
                    toastManager.showAsync("重命名成功")
                }
            }
        }
    }

    fun refreshCollectTagCounts() {
        collectTagCountJob?.cancel()
        val blockedTagList = localSettingManager.localSettingState.value.blockedTagList
        val order = _collectComicOrder.value
        val folderId = _selectedFolderId.value
        val shouldEnrichTags = localSettingManager.localSettingState.value.comicApiSource != COMIC_API_SOURCE_NETWORK
        val requestKey = listOf(
            order.value,
            folderId,
            localSettingManager.localSettingState.value.comicApiSource,
            blockedTagList.joinToString("\u0001"),
        ).joinToString("|")
        if (collectTagCountKey == requestKey) return
        collectTagCountKey = ""
        _collectTagCounts.value = emptyMap()
        _collectRoleCounts.value = emptyMap()
        _collectAuthorCounts.value = emptyMap()
        _collectTypeCounts.value = emptyMap()
        collectTagCountJob = viewModelScope.launch(Dispatchers.IO) {
            val tagCounts = mutableMapOf<String, Int>()
            val authorCounts = mutableMapOf<String, Int>()
            val roleCounts = mutableMapOf<String, Int>()
            val typeCounts = mutableMapOf<String, Int>()
            fun consume(page: UserCollectComicListResponse) {
                page.toComicList().filterBlockedTags(blockedTagList).forEach { comic ->
                    comic.tagList.forEach { tag -> tagCounts[tag] = (tagCounts[tag] ?: 0) + 1 }
                    comic.authorList.forEach { author -> authorCounts[author] = (authorCounts[author] ?: 0) + 1 }
                    comic.roleList.forEach { role -> roleCounts[role] = (roleCounts[role] ?: 0) + 1 }
                    comic.typeList.forEach { type -> typeCounts[type] = (typeCounts[type] ?: 0) + 1 }
                }
                _collectTagCounts.value = tagCounts.toSortedMap()
                _collectRoleCounts.value = roleCounts.toSortedMap()
                _collectAuthorCounts.value = authorCounts.toSortedMap()
                _collectTypeCounts.value = typeCounts.toSortedMap()
            }

            val firstPage = if (
                order == CollectComicOrderFilter.COLLECT_TIME &&
                folderId == 0
            ) {
                _preloadedFavoritePage.value?.takeIf { page ->
                    !shouldEnrichTags || page.list.any { item ->
                        item.tags.orEmpty().isNotEmpty() || item.hidden_tags.orEmpty().isNotEmpty()
                    }
                }
            } else {
                null
            }
            val firstResult = firstPage?.let { NetWorkResult.Success(it) }
                ?: userRepository.getCollectComicList(1, order, folderId, enrichTags = shouldEnrichTags)
            val firstData = when (firstResult) {
                is NetWorkResult.Error -> {
                    toastManager.showAsync(firstResult.message)
                    return@launch
                }
                is NetWorkResult.Success -> firstResult.data
            }
            consume(firstData)

            val pageSize = firstData.list.size
            val total = maxOf(firstData.total, firstData.count)
            val pageCount = if (pageSize == 0 || total <= pageSize) {
                1
            } else {
                ((total + pageSize - 1) / pageSize).coerceAtMost(100)
            }
            if (pageCount > 1) {
                val pageConcurrency = if (shouldEnrichTags) {
                    COLLECT_DETAIL_STATS_PAGE_CONCURRENCY
                } else {
                    COLLECT_STATS_PAGE_CONCURRENCY
                }
                coroutineScope {
                    (2..pageCount).chunked(pageConcurrency).forEach { pageNumbers ->
                        pageNumbers.map { page ->
                            async {
                                userRepository.getCollectComicList(page, order, folderId, enrichTags = shouldEnrichTags)
                            }
                        }.awaitAll().forEach { result ->
                            when (result) {
                                is NetWorkResult.Error -> logError("UserViewModel", "收藏筛选统计页失败：${result.message}")
                                is NetWorkResult.Success -> consume(result.data)
                            }
                        }
                    }
                }
            }
            collectTagCountKey = requestKey
        }.also { job ->
            job.invokeOnCompletion {
                if (collectTagCountJob === job && collectTagCountKey.isBlank()) {
                    collectTagCountJob = null
                }
            }
        }
    }

    private fun invalidateCollectFilterStats() {
        collectTagCountJob?.cancel()
        collectTagCountKey = ""
        _collectTagCounts.value = emptyMap()
        _collectRoleCounts.value = emptyMap()
        _collectAuthorCounts.value = emptyMap()
        _collectTypeCounts.value = emptyMap()
    }

    private companion object {
        const val COLLECT_STATS_PAGE_CONCURRENCY = 4
        const val COLLECT_DETAIL_STATS_PAGE_CONCURRENCY = 3
    }

    private val _historyRefreshVersion = MutableStateFlow(0)

    fun refreshHistoryComics() {
        _historyRefreshVersion.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyComicPager = combine(
        localSettingManager.localSettingState,
        _historyRefreshVersion
    ) { localSetting, _ -> localSetting }
        .flatMapLatest { localSetting ->
        Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 6, initialLoadSize = 20),
            pagingSourceFactory = {
                HistoryComicPagingSource(
                    userRepository,
                    localSetting.blockedTagList
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    private val _historyEditState = MutableStateFlow(HistoryEditState())
    val historyEditState = _historyEditState.asStateFlow()

    fun enterHistoryEdit(comicId: Int) {
        _historyEditState.update {
            it.copy(editing = true, selectedComicIds = it.selectedComicIds + comicId)
        }
    }

    fun toggleHistorySelected(comicId: Int) {
        _historyEditState.update {
            val selected = if (comicId in it.selectedComicIds) {
                it.selectedComicIds - comicId
            } else {
                it.selectedComicIds + comicId
            }
            it.copy(editing = selected.isNotEmpty(), selectedComicIds = selected)
        }
    }

    fun clearHistorySelection() {
        _historyEditState.update { HistoryEditState() }
    }

    fun deleteHistoryComics(comics: List<Comic>) {
        if (comics.isEmpty()) return
        log("UserViewModel", "deleteHistoryComics: 开始删除 ${comics.size} 条历史记录, ids=${comics.map { it.id }}")
        viewModelScope.launch {
            var success = 0
            var fail = 0
            val errors = mutableListOf<String>()
            comics.forEach { comic ->
                log("UserViewModel", "deleteHistoryComics: 正在删除 comic.id=${comic.id}")
                when (val result = userRepository.deleteHistoryComic(comic.id)) {
                    is NetWorkResult.Error -> {
                        logError(
                            "UserViewModel",
                            "deleteHistoryComics: 删除 comic.id=${comic.id} 失败: ${result.message}"
                        )
                        errors += result.message
                        fail++
                    }
                    is NetWorkResult.Success -> success++
                }
            }
            log("UserViewModel", "deleteHistoryComics: 完成, 成功=$success, 失败=$fail")
            val message = when {
                fail == 0 -> "已删除 $success 条历史记录"
                success == 0 -> errors.firstOrNull() ?: "删除失败"
                else -> "成功 $success 条，失败 $fail 条：${errors.firstOrNull().orEmpty()}"
            }
            toastManager.showAsync(message)
            if (success > 0) {
                _historyRefreshVersion.update { it + 1 }
            }
            clearHistorySelection()
        }
    }

    fun cacheHistoryComics(comics: List<Comic>) {
        if (comics.isEmpty()) return
        downloadManager.downloadComics(comics)
        clearHistorySelection()
    }

    val historyCommentPager = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 6, initialLoadSize = 20),
        pagingSourceFactory = {
            HistoryCommentPagingSource(
                userRepository,
                userManager.userState.value.data?.id ?: 0
            )
        }
    ).flow.cachedIn(viewModelScope)

    private val _signInDataState = MutableStateFlow(
        CommonUIState<SignInData>(
            isLoading = true
        )
    )
    val signDataState = _signInDataState.asStateFlow()
    fun getSignInData() {
        viewModelScope.launch {
            _signInDataState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = userRepository.getSignData(userManager.userState.value.data?.id ?: 0)) {
                is NetWorkResult.Error -> {
                    _signInDataState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<SignInDataResponse> -> {
                    _signInDataState.update {
                        it.copy(
                            data = data.data.toSignData()
                        )
                    }
                }
            }
            _signInDataState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    private val _signInState = MutableStateFlow(CommonUIState<String>())
    val signInState = _signInState.asStateFlow()
    fun signIn() {
        viewModelScope.launch {
            _signInState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = userRepository.signIn(
                userManager.userState.value.data?.id ?: 0,
                _signInDataState.value.data?.dailyId ?: 0
            )) {
                is NetWorkResult.Error -> {
                    _signInState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<SignInResponse> -> {
                    toastManager.showAsync(data.data.msg)
                    getSignInData()
                    _signInState.update {
                        it.copy(
                            data = data.data.msg
                        )
                    }
                }
            }
            _signInState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }
}
