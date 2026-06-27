package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.data.models.SignInData
import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.retrofit.model.LoginResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.retrofit.model.SignInDataResponse
import com.par9uet.jm.retrofit.model.SignInResponse
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.UserManager
import com.par9uet.jm.ui.models.CommonUIState
import com.par9uet.jm.ui.pagingSource.CollectComicPagingSource
import com.par9uet.jm.ui.pagingSource.HistoryComicPagingSource
import com.par9uet.jm.ui.pagingSource.HistoryCommentPagingSource
import com.par9uet.jm.utils.filterBlockedTags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectComicLocalFilter(
    val searchText: String = "",
    val selectedTags: Set<String> = emptySet()
)

class UserViewModel(
    private val userManager: UserManager,
    private val userRepository: UserRepository,
    private val toastManager: ToastManager,
    private val localSettingManager: LocalSettingManager,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val collectComicPager = combine(
        _collectComicOrder,
        localSettingManager.localSettingState,
        _collectComicFilter
    ) { order, localSetting, filter ->
        Triple(order, localSetting.blockedTagList, filter)
    }.flatMapLatest { (order, blockedTagList, filter) ->
        Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 6, initialLoadSize = 20),
            pagingSourceFactory = {
                CollectComicPagingSource(
                    userRepository,
                    order,
                    blockedTagList,
                    filter.searchText,
                    filter.selectedTags
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun changeCollectComicOrder(order: CollectComicOrderFilter) {
        _collectComicOrder.update {
            order
        }
        refreshCollectTagCounts()
    }

    fun updateCollectSearchText(value: String) {
        _collectComicFilter.update { it.copy(searchText = value) }
    }

    fun updateCollectSelectedTags(tags: Set<String>) {
        _collectComicFilter.update { it.copy(selectedTags = tags) }
    }

    fun refreshCollectTagCounts() {
        viewModelScope.launch {
            val blockedTagList = localSettingManager.localSettingState.value.blockedTagList
            val order = _collectComicOrder.value
            val counts = mutableMapOf<String, Int>()
            var page = 1
            var loaded = 0
            var total = Int.MAX_VALUE
            while (loaded < total && page <= 100) {
                when (val data = userRepository.getCollectComicList(page, order)) {
                    is NetWorkResult.Error -> {
                        toastManager.showAsync(data.message)
                        return@launch
                    }

                    is NetWorkResult.Success -> {
                        val comics = data.data.toComicList().filterBlockedTags(blockedTagList)
                        comics.flatMap { it.tagList }.forEach { tag ->
                            counts[tag] = (counts[tag] ?: 0) + 1
                        }
                        total = data.data.total
                        loaded += data.data.list.size
                        if (data.data.list.isEmpty()) break
                        page += 1
                    }
                }
            }
            _collectTagCounts.value = counts.toSortedMap()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyComicPager = localSettingManager.localSettingState.flatMapLatest { localSetting ->
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

    val historyCommentPager = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 6, initialLoadSize = 20),
        pagingSourceFactory = {
            HistoryCommentPagingSource(
                userRepository,
                userManager.userState.value.data!!.id
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
            when (val data = userRepository.getSignData(userManager.userState.value.data!!.id)) {
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
                userManager.userState.value.data!!.id,
                _signInDataState.value.data!!.dailyId
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
