package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.data.models.ComicRankCategory
import com.par9uet.jm.data.models.ComicRankPeriod
import com.par9uet.jm.data.models.ComicRankingFilter
import com.par9uet.jm.data.models.HomeComicSwiperItem
import com.par9uet.jm.data.models.WeekData
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.WeekResponse
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.models.CommonUIState
import com.par9uet.jm.ui.pagingSource.SearchComicFilter
import com.par9uet.jm.ui.pagingSource.SearchComicPagingSource
import com.par9uet.jm.ui.pagingSource.ComicRankingPagingSource
import com.par9uet.jm.ui.pagingSource.WeekComicPagingSource
import com.par9uet.jm.ui.pagingSource.WeekFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.par9uet.jm.utils.watchReadRequest
import com.par9uet.jm.data.models.watchHomeCategories

class ComicViewModel(
    private val comicRepository: ComicRepository,
    private val localSettingManager: LocalSettingManager,
) : ViewModel() {
    companion object {
        /**
         * Search is deliberately not cached in viewModelScope on the watch.
         * Keeping previous PagingData generations alive makes each tag search
         * retain its loaded page window, which is especially expensive when
         * several result screens are visited on a low-memory Wear OS device.
         */
        private const val SEARCH_PAGE_SIZE = 20
        private const val SEARCH_MAX_CACHED_ITEMS = 80
    }

    data class HomeComicUIState(
        val isLoading: Boolean = true,
        val isError: Boolean = false,
        val list: List<HomeComicSwiperItem> = emptyList(),
        val errorMsg: String? = null,
        val selectedCategoryId: String? = null,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = false,
        val moreError: String? = null,
    )
    private val _homeComicState = MutableStateFlow(HomeComicUIState())
    val homeComicState = _homeComicState.asStateFlow()
    private var catalogJob: Job? = null
    private var homeJob: Job? = null
    private var preferenceJob: Job? = null
    private var homeSource: String? = null
    private var preferenceEnabled: Boolean? = null
    private val categoryPages = mutableMapOf<String, Int>()
    private val categoryHasMore = mutableMapOf<String, Boolean>()

    fun getHomeComic() {
        catalogJob?.cancel(); homeJob?.cancel(); preferenceJob?.cancel()
        val setting = localSettingManager.localSettingState.value
        val sourceChanged = homeSource != setting.comicApiSource || preferenceEnabled != setting.preferenceRecommendEnabled
        homeSource = setting.comicApiSource
        preferenceEnabled = setting.preferenceRecommendEnabled
        if (sourceChanged) {
            categoryPages.clear(); categoryHasMore.clear()
            _homeComicState.value = HomeComicUIState()
        }
        catalogJob = viewModelScope.launch {
            _homeComicState.update { it.copy(isLoading = true, isError = false, errorMsg = null, isLoadingMore = false) }
            when (val result = watchReadRequest("首页分类") { comicRepository.getWatchHomeCatalog() }) {
                is NetWorkResult.Error -> _homeComicState.update { it.copy(isLoading = false, isError = true, errorMsg = result.message) }
                is NetWorkResult.Success -> {
                    // Keep categories selectable even before their first network response.
                    val incoming = result.data.map { it.toHomeComicSwiperItem() }
                    val previous = _homeComicState.value
                    val merged = if (sourceChanged) incoming else incoming.map { fresh ->
                        if (fresh.id.startsWith("builtin_") && fresh.list.isEmpty()) previous.list.firstOrNull { it.id == fresh.id } ?: fresh else fresh
                    }
                    val selected = previous.selectedCategoryId?.takeIf { id -> merged.any { it.id == id } } ?: merged.firstOrNull()?.id
                    _homeComicState.value = previous.copy(list = merged, selectedCategoryId = selected, isLoading = false, isError = false, errorMsg = null)
                    if (selected != null) selectHomeCategory(selected, force = true)
                    if (setting.preferenceRecommendEnabled) {
                        preferenceJob = viewModelScope.launch {
                            when (val prefs = watchReadRequest("偏好推荐") { comicRepository.getWatchPreferenceCategories() }) {
                                is NetWorkResult.Success -> _homeComicState.update { state ->
                                    state.copy(list = state.list.filterNot { it.id.startsWith("pref_") } + prefs.data.map { it.toHomeComicSwiperItem() })
                                }
                                is NetWorkResult.Error -> Unit // Built-in categories still work; refresh retries recommendations.
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectHomeCategory(id: String, force: Boolean = false, append: Boolean = false) {
        val state = _homeComicState.value
        val category = state.list.firstOrNull { it.id == id } ?: return
        if (append && (state.isLoading || state.isLoadingMore || categoryHasMore[id] != true)) return
        if (!force && state.selectedCategoryId == id && homeJob?.isActive == true) return
        homeJob?.cancel()
        _homeComicState.update { it.copy(selectedCategoryId = id, isLoadingMore = false, isError = false, errorMsg = null,
            canLoadMore = categoryHasMore[id] == true, moreError = null) }
        if (!id.startsWith("builtin_") || (!force && !append && categoryPages.containsKey(id))) {
            _homeComicState.update { it.copy(isLoading = false) }; return
        }
        val page = if (append) (categoryPages[id] ?: 1) + 1 else 1
        _homeComicState.update { it.copy(isLoading = !append, isLoadingMore = append) }
        homeJob = viewModelScope.launch {
            when (val result = watchReadRequest(category.title) { comicRepository.getWatchHomeCategory(id, page) }) {
                is NetWorkResult.Error -> _homeComicState.update { it.copy(isLoading = false, isLoadingMore = false,
                    isError = !append, errorMsg = if (append) null else result.message, moreError = if (append) result.message else null) }
                is NetWorkResult.Success -> {
                    val fresh = result.data.toHomeComicSwiperItem()
                    val current = _homeComicState.value.list.firstOrNull { it.id == id } ?: fresh
                    val combined = (if (append) current.list + fresh.list else fresh.list).distinctBy { it.id }
                    categoryPages[id] = page
                    categoryHasMore[id] = watchHomeCategories.firstOrNull { it.id == id }?.pageable == true &&
                        fresh.list.size >= 20 && (!append || combined.size > current.list.size)
                    _homeComicState.update { it.copy(list = it.list.map { old -> if (old.id == id) fresh.copy(list = combined) else old },
                        isLoading = false, isLoadingMore = false, isError = false, errorMsg = null, moreError = null,
                        canLoadMore = categoryHasMore[id] == true) }
                }
            }
        }
    }

    fun loadMoreHomeComic() { _homeComicState.value.selectedCategoryId?.let { selectHomeCategory(it, append = true) } }

    private val _searchComicFilterState = MutableStateFlow(SearchComicFilter())
    val searchComicFilterState = _searchComicFilterState.asStateFlow()
    private val _searchComicIdState = MutableStateFlow<Int?>(null)
    val searchComicIdState = _searchComicIdState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchComicPager = combine(
        _searchComicFilterState,
        localSettingManager.localSettingState
    ) { filter, localSetting -> filter to localSetting.blockedTagList }
        .flatMapLatest { (filter, blockedTagList) ->
        Pager(
            config = PagingConfig(
                pageSize = SEARCH_PAGE_SIZE,
                prefetchDistance = 1,
                initialLoadSize = SEARCH_PAGE_SIZE,
                maxSize = SEARCH_MAX_CACHED_ITEMS,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                SearchComicPagingSource(
                    comicRepository,
                    filter.copy(excludedTags = (filter.excludedTags + blockedTagList).distinct()),
                ) { id ->
                    _searchComicIdState.update {
                        id
                    }
                }
            }
        ).flow
    }

    fun changeSearchComicOrderFilter(order: ComicSearchOrderFilter) {
        _searchComicIdState.update { null }
        _searchComicFilterState.update {
            it.copy(
                order = order
            )
        }
    }

    fun changeSearchComicContent(searchContent: String) {
        _searchComicIdState.update { null }
        _searchComicFilterState.update {
            it.copy(
                searchContent = searchContent
            )
        }
    }

    fun changeSearchComicContent(
        searchContent: String,
        excludedTags: List<String>,
        mainTag: Int = 0,
    ) {
        _searchComicIdState.update { null }
        _searchComicFilterState.update {
            it.copy(
                searchContent = searchContent,
                excludedTags = excludedTags,
                mainTag = mainTag,
            )
        }
    }

    fun consumeSearchComicId() {
        _searchComicIdState.update { null }
    }

    private val _comicRankingFilterState = MutableStateFlow<ComicRankingFilter?>(null)
    val comicRankingFilterState = _comicRankingFilterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val comicRankingPager = combine(
        _comicRankingFilterState.filterNotNull(),
        localSettingManager.localSettingState,
    ) { filter, localSetting ->
        filter to localSetting.blockedTagList.distinct()
    }.flatMapLatest { (filter, blockedTags) ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 1,
                initialLoadSize = 20,
            ),
            pagingSourceFactory = {
                ComicRankingPagingSource(
                    comicRepository = comicRepository,
                    filter = filter,
                    blockedTags = blockedTags,
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun setComicRankingFilter(
        tagName: String,
        category: ComicRankCategory?,
        period: ComicRankPeriod,
    ) {
        _comicRankingFilterState.value = ComicRankingFilter(
            tagName = tagName,
            category = category,
            period = period,
        )
    }

    private val _weekDataState = MutableStateFlow(CommonUIState<WeekData>())
    val weekDataState = _weekDataState.asStateFlow()
    fun getWeekData() {
        viewModelScope.launch {
            _weekDataState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.getWeekData()) {
                is NetWorkResult.Error -> {
                    _weekDataState.update {
                        it.copy(isError = true, errorMsg = data.message)
                    }
                }

                is NetWorkResult.Success<WeekResponse> -> {
                    val d = data.data.toWeekData()
                    _weekDataState.update {
                        it.copy(data = d)
                    }
                    if (d.categoryList.isNotEmpty()) {
                        _weekFilterState.update {
                            it.copy(categoryId = d.categoryList[0].first)
                        }
                    }
                    if (d.typeList.isNotEmpty()) {
                        _weekFilterState.update {
                            it.copy(typeId = d.typeList[0].first)
                        }
                    }
                }
            }
            _weekDataState.update {
                it.copy(isLoading = false)
            }
        }
    }

    private val _weekFilterState = MutableStateFlow(WeekFilter())
    val weekFilterState = _weekFilterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val weekComicPager = combine(
        _weekFilterState,
        localSettingManager.localSettingState
    ) { filter, localSetting ->
        filter to (localSetting.blockedTagList + localSetting.homeExcludedTags).distinct()
    }
        .flatMapLatest { (filter, blockedTagList) ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 1,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                WeekComicPagingSource(
                    comicRepository,
                    filter,
                    blockedTagList
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun changeWeekCategoryFilter(categoryId: String?) {
        _weekFilterState.update {
            it.copy(
                categoryId = categoryId
            )
        }
    }

    fun changeWeekTypeFilter(typeId: String?) {
        _weekFilterState.update {
            it.copy(
                typeId = typeId
            )
        }
    }
}
