package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.HomeComicSwiperItem
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.data.models.WeekData
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.repository.ComicTagFilter
import com.par9uet.jm.repository.ComicTagFilterResult
import com.par9uet.jm.retrofit.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.WeekResponse
import com.par9uet.jm.ui.models.CommonUIState
import com.par9uet.jm.ui.pagingSource.SearchComicFilter
import com.par9uet.jm.ui.pagingSource.SearchComicPagingSource
import com.par9uet.jm.ui.pagingSource.WeekComicPagingSource
import com.par9uet.jm.ui.pagingSource.WeekFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComicViewModel(
    private val comicRepository: ComicRepository,
    private val localSettingState: StateFlow<LocalSetting>,
    private val comicTagFilter: ComicTagFilter,
) : ViewModel() {
    data class HomeFilterRequest(
        val category: HomeComicSwiperItem,
        val excludedTags: List<String>,
        val source: String,
    )

    data class HomeFilteredState(
        val request: HomeFilterRequest? = null,
        val isLoading: Boolean = false,
        val result: ComicTagFilterResult? = null,
        val errorMsg: String? = null,
        val checkedCount: Int = 0,
        val totalCount: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        val loadMoreError: String? = null,
        val autoFillPaused: Boolean = false,
    )

    private var homeFilterJob: Job? = null
    private val _homeFilteredState = MutableStateFlow(HomeFilteredState())
    val homeFilteredState = _homeFilteredState.asStateFlow()

    private data class HomeCategoryPages(
        val seed: HomeComicSwiperItem,
        val source: String,
        val comics: List<Comic>,
        val nextPage: Int?,
    )
    private val homePages = mutableMapOf<String, HomeCategoryPages>()

    private fun homePagesFor(request: HomeFilterRequest): HomeCategoryPages {
        val saved = homePages[request.category.id]
        if (saved?.seed == request.category && saved.source == request.source) return saved
        return HomeCategoryPages(
            request.category, request.source, request.category.list.distinctBy { it.id }, request.category.pageSource?.initialPage,
        ).also {
            homePages[request.category.id] = it
        }
    }

    fun filterHomeCategory(request: HomeFilterRequest, forceRefresh: Boolean = false) {
        val current = _homeFilteredState.value
        if (!forceRefresh && current.request == request &&
            (homeFilterJob?.isActive == true || !current.isLoading)) return
        homeFilterJob?.cancel()
        val pages = homePagesFor(request)
        _homeFilteredState.value = HomeFilteredState(request, isLoading = true, hasMore = pages.nextPage != null)
        homeFilterJob = viewModelScope.launch {
            try {
                checkHomeCandidates(request, pages.comics, append = false)
                if ((_homeFilteredState.value.result?.comics?.size ?: 0) < 20) {
                    appendHomePages(request, targetCount = 20)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _homeFilteredState.update { it.copy(isLoading = false, errorMsg = "标签检查失败，请重试") }
            }
        }
    }

    private suspend fun checkHomeCandidates(request: HomeFilterRequest, candidates: List<Comic>, append: Boolean) {
        val previous = _homeFilteredState.value
        val base = if (append) previous.result ?: ComicTagFilterResult(emptyList()) else ComicTagFilterResult(emptyList())
        val checked = if (append) previous.checkedCount else 0
        val total = checked + candidates.size
        fun merge(result: ComicTagFilterResult) = ComicTagFilterResult(
            (base.comics + result.comics).distinctBy { it.id }, base.unresolvedCount + result.unresolvedCount,
        )
        _homeFilteredState.update { it.copy(isLoading = true, totalCount = total) }
        val result = comicTagFilter.filter(candidates, request.excludedTags, request.source) { progress ->
            withContext(Dispatchers.Main.immediate) {
                currentCoroutineContext().ensureActive()
                _homeFilteredState.update {
                    it.copy(
                        result = merge(progress.result),
                        checkedCount = checked + progress.checkedCount,
                        totalCount = total,
                    )
                }
            }
        }
        currentCoroutineContext().ensureActive()
        _homeFilteredState.update { it.copy(isLoading = false, result = merge(result), checkedCount = total) }
    }

    fun loadMoreHomeCategory(request: HomeFilterRequest) {
        val state = _homeFilteredState.value
        if (homeFilterJob?.isActive == true || state.request != request || !state.hasMore) return
        homeFilterJob = viewModelScope.launch {
            appendHomePages(request, (state.result?.comics?.size ?: 0) + 20)
        }
    }

    private suspend fun appendHomePages(request: HomeFilterRequest, targetCount: Int) {
        val source = request.category.pageSource ?: return
        _homeFilteredState.update { it.copy(isLoadingMore = true, loadMoreError = null, autoFillPaused = false) }
        try {
            var duplicatePages = 0
            for (attempt in 1..3) {
                val previous = homePagesFor(request)
                val page = previous.nextPage ?: break
                val response = comicRepository.getHomeCategoryPage(source, page)
                currentCoroutineContext().ensureActive()
                when (response) {
                    is NetWorkResult.Error -> {
                        _homeFilteredState.update { it.copy(loadMoreError = response.message) }
                        break
                    }
                    is NetWorkResult.Success -> {
                        val known = previous.comics.mapTo(mutableSetOf()) { it.id }
                        val fresh = response.data.comics.distinctBy { it.id }.filterNot { it.id in known }
                        val next = response.data.nextPage
                        homePages[request.category.id] = previous.copy(comics = previous.comics + fresh, nextPage = next)
                        _homeFilteredState.update { it.copy(hasMore = next != null) }
                        if (fresh.isNotEmpty()) {
                            duplicatePages = 0
                            checkHomeCandidates(request, fresh, append = true)
                        } else if (next != null && ++duplicatePages >= 2) {
                            _homeFilteredState.update { it.copy(loadMoreError = "后续页面暂未返回新漫画，请稍后重试") }
                            break
                        }
                        if ((_homeFilteredState.value.result?.comics?.size ?: 0) >= targetCount) break
                    }
                }
            }
            _homeFilteredState.update {
                it.copy(autoFillPaused = it.hasMore && (it.result?.comics?.size ?: 0) < targetCount)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _homeFilteredState.update { it.copy(isLoading = false, loadMoreError = "后续漫画加载失败，请重试") }
        } finally {
            currentCoroutineContext().ensureActive()
            _homeFilteredState.update { it.copy(isLoadingMore = false) }
        }
    }

    data class HomeComicUIState(
        val isLoading: Boolean = true,
        val isError: Boolean = false,
        val list: List<HomeComicSwiperItem> = listOf(),
        val errorMsg: String? = null,
        val revision: Long = 0,
    )

    private val _homeComicState = MutableStateFlow(HomeComicUIState())
    val homeComicState = _homeComicState.asStateFlow()
    private var homeLoadJob: Job? = null
    private var homeSource: Triple<String, String, Boolean>? = null

    fun getHomeComic(forceRefresh: Boolean = true) {
        val setting = localSettingState.value
        val source = Triple(setting.comicApiSource, setting.api, setting.preferenceRecommendEnabled)
        if (!forceRefresh && homeSource == source &&
            (homeLoadJob?.isActive == true || !_homeComicState.value.isLoading)) return
        homeLoadJob?.cancel()
        if (homeSource != source) {
            homeFilterJob?.cancel()
            homePages.clear()
            _homeFilteredState.value = HomeFilteredState()
            _homeComicState.value = HomeComicUIState()
        } else {
            _homeComicState.update { it.copy(isLoading = true, isError = false, errorMsg = null) }
        }
        homeSource = source
        homeLoadJob = viewModelScope.launch {
            try {
                val data = comicRepository.getHomeSwiperComicList()
                currentCoroutineContext().ensureActive()
                when (data) {
                    is NetWorkResult.Error -> {
                        _homeComicState.update {
                            it.copy(isLoading = false, isError = true, errorMsg = data.message)
                        }
                    }
                    is NetWorkResult.Success<List<HomeSwiperComicListItemResponse>> -> {
                        homeFilterJob?.cancel()
                        homePages.clear()
                        _homeFilteredState.value = HomeFilteredState()
                        _homeComicState.update {
                            it.copy(isLoading = false, revision = it.revision + 1, list = data.data.map { item -> item.toHomeComicSwiperItem() })
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _homeComicState.update { it.copy(isLoading = false, isError = true, errorMsg = "首页加载失败，请下拉重试") }
            }
        }
    }

    private val _searchComicFilterState = MutableStateFlow(SearchComicFilter())
    val searchComicFilterState = _searchComicFilterState.asStateFlow()
    private val _searchComicIdState = MutableStateFlow<Int?>(null)
    val searchComicIdState = _searchComicIdState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchComicPager = combine(
        _searchComicFilterState,
        localSettingState
    ) { filter, localSetting ->
        Triple(filter, localSetting.blockedTagList, "${localSetting.comicApiSource}|${localSetting.api}")
    }
        .flatMapLatest { (filter, blockedTagList, tagSource) ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 6,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                SearchComicPagingSource(
                    comicRepository,
                    filter.copy(excludedTags = (filter.excludedTags + blockedTagList).distinct()),
                    tagFilter = comicTagFilter,
                    tagSource = tagSource,
                ) { id ->
                    _searchComicIdState.update {
                        id
                    }
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

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

    fun changeSearchComicContent(searchContent: String, excludedTags: List<String>) {
        _searchComicIdState.update { null }
        _searchComicFilterState.update {
            it.copy(
                searchContent = searchContent,
                excludedTags = excludedTags
            )
        }
    }

    fun consumeSearchComicId() {
        _searchComicIdState.update { null }
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
        localSettingState
    ) { filter, localSetting ->
        Triple(
            filter,
            (localSetting.blockedTagList + localSetting.homeExcludedTags).distinct(),
            "${localSetting.comicApiSource}|${localSetting.api}",
        )
    }
        .flatMapLatest { (filter, blockedTagList, tagSource) ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 6,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                WeekComicPagingSource(
                    comicRepository,
                    filter,
                    blockedTagList,
                    comicTagFilter,
                    tagSource,
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
