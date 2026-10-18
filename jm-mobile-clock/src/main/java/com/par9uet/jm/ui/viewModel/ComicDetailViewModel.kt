package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.data.models.COMIC_API_SOURCE_BUILTIN
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.repository.ComicRepository
import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.retrofit.model.CollectComicResponse
import com.par9uet.jm.retrofit.model.ComicDetailResponse
import com.par9uet.jm.retrofit.model.CommentComicResponse
import com.par9uet.jm.retrofit.model.LikeComicResponse
import com.par9uet.jm.retrofit.model.NetWorkResult
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.RemoteSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.models.CommonUIState
import com.par9uet.jm.ui.pagingSource.ComicCommentPagingSource
import com.par9uet.jm.utils.log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComicDetailViewModel(
    private val comicRepository: ComicRepository,
    private val toastManager: ToastManager,
    private val downloadComicDao: DownloadComicDao,
    private val remoteSettingManager: RemoteSettingManager,
    private val userRepository: UserRepository,
    private val localSettingManager: LocalSettingManager,
) : ViewModel() {
    private val _comicDetailState = MutableStateFlow<CommonUIState<Comic>>(
        CommonUIState(
            isLoading = true,
        )
    )
    val comicDetailState = _comicDetailState.asStateFlow()

    fun getComicDetail(id: Int) {
        viewModelScope.launch {
            _comicDetailState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = "",
                )
            }
            when (val data = comicRepository.getComicDetail(id)) {
                is NetWorkResult.Error -> {
                    _comicDetailState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<ComicDetailResponse> -> {
                    _comicDetailState.update {
                        it.copy(
                            data = data.data.toComic()
                        )
                    }
                }
            }
            _comicDetailState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    private val _likeComicState = MutableStateFlow(CommonUIState(data = null))
    val likeComicState = _likeComicState.asStateFlow()
    fun likeComic(id: Int) {
        viewModelScope.launch {
            _likeComicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.likeComic(id)) {
                is NetWorkResult.Error -> {
                    _likeComicState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<LikeComicResponse> -> {
                    toastManager.showAsync("喜欢成功")
                    _comicDetailState.update { state ->
                        val currentData = state.data
                        if (currentData != null) {
                            state.copy(
                                data = currentData.copy(
                                    isLike = true,
                                    likeCount = currentData.likeCount + 1
                                )
                            )
                        } else {
                            state
                        }
                    }
                }
            }
            _likeComicState.update {
                it.copy(
                    isLoading = false,
                )
            }
        }
    }

    private val _collectComicState = MutableStateFlow(CommonUIState(data = null))
    val collectComicState = _collectComicState.asStateFlow()
    fun collect(id: Int) {
        viewModelScope.launch {
            _collectComicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.collectComic(id)) {
                is NetWorkResult.Error -> {
                    _collectComicState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<CollectComicResponse> -> {
                    toastManager.showAsync("收藏成功")
                    _comicDetailState.update { state ->
                        val currentData = state.data
                        if (currentData != null) {
                            state.copy(data = currentData.copy(isCollect = true))
                        } else {
                            state
                        }
                    }
                }
            }
            _collectComicState.update {
                it.copy(
                    isLoading = false,
                )
            }
        }
    }

    fun unCollect(id: Int) {
        viewModelScope.launch {
            _collectComicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.unCollectComic(id)) {
                is NetWorkResult.Error -> {
                    _collectComicState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<CollectComicResponse> -> {
                    toastManager.showAsync("取消收藏成功")
                    _comicDetailState.update { state ->
                        val currentData = state.data
                        if (currentData != null) {
                            state.copy(data = currentData.copy(isCollect = false))
                        } else {
                            state
                        }
                    }
                }
            }
            _collectComicState.update {
                it.copy(
                    isLoading = false,
                )
            }
        }
    }

    // 收藏夹选择相关
    private val _folderList = MutableStateFlow<Map<String, String>>(emptyMap())
    val folderList = _folderList.asStateFlow()

    private val _showFolderPicker = MutableStateFlow(false)
    val showFolderPicker = _showFolderPicker.asStateFlow()

    fun shouldShowFolderPicker(): Boolean {
        return localSettingManager.localSettingState.value.comicApiSource == COMIC_API_SOURCE_BUILTIN
    }

    fun refreshFolderList() {
        viewModelScope.launch {
            val order = com.par9uet.jm.data.models.CollectComicOrderFilter.COLLECT_TIME
            when (val data = userRepository.getCollectComicList(1, order, 0)) {
                is NetWorkResult.Success -> _folderList.value = data.data.folder_list ?: emptyMap()
                else -> {}
            }
        }
    }

    fun showFolderPicker() {
        _showFolderPicker.value = true
    }

    fun hideFolderPicker() {
        _showFolderPicker.value = false
    }

    fun collectWithFolder(comicId: Int, folderId: String) {
        viewModelScope.launch {
            _showFolderPicker.value = false
            _collectComicState.update { it.copy(isLoading = true, isError = false, errorMsg = "") }
            // 先收藏到默认夹
            when (val data = comicRepository.collectComic(comicId)) {
                is NetWorkResult.Error -> {
                    _collectComicState.update { it.copy(isError = true, errorMsg = data.message) }
                }
                is NetWorkResult.Success<CollectComicResponse> -> {
                    // 如果选择了非默认夹，再移动到目标夹
                    if (folderId != "0") {
                        when (val moveResult = comicRepository.moveComicToFolder(comicId, folderId)) {
                            is NetWorkResult.Error -> {
                                toastManager.showAsync("已收藏但移动到收藏夹失败：${moveResult.message}")
                            }
                            is NetWorkResult.Success<Unit> -> {
                                val folderName = _folderList.value[folderId] ?: "收藏夹"
                                toastManager.showAsync("已收藏到 $folderName")
                            }
                        }
                    } else {
                        toastManager.showAsync("收藏成功")
                    }
                    _comicDetailState.update { state ->
                        val currentData = state.data
                        if (currentData != null) {
                            state.copy(data = currentData.copy(isCollect = true))
                        } else {
                            state
                        }
                    }
                }
            }
            _collectComicState.update { it.copy(isLoading = false) }
        }
    }

    fun reset(id: Int?) {
        if (id != null && id == _comicDetailState.value.data?.id) {
            return
        }
        _comicDetailState.update {
            CommonUIState(
                isLoading = true,
            )
        }
    }

    private val _commentComicIdState = MutableStateFlow(0)
    val commentComicIdState = _commentComicIdState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentPager = _commentComicIdState.flatMapLatest { comicId ->
        Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 6, initialLoadSize = 20),
            pagingSourceFactory = {
                ComicCommentPagingSource(
                    comicRepository,
                    comicId
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun changeCommentComicId(comicId: Int) {
        _commentComicIdState.update {
            comicId
        }
    }

    private val _commentComicState = MutableStateFlow(CommonUIState(data = null))
    val commentComicState = _commentComicState.asStateFlow()
    fun comment(
        content: String,
        comicId: Int,
        commentId: Int? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _commentComicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.comment(content, comicId, commentId)) {
                is NetWorkResult.Error -> {
                    _commentComicState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                    toastManager.showAsync(data.message)
                }

                is NetWorkResult.Success<CommentComicResponse> -> {
                    log("commentArg $content, $comicId, $commentId")
                    val status = data.data.status.trim()
                    val isSuccess = status.isBlank()
                        || status.equals("ok", ignoreCase = true)
                        || status.equals("success", ignoreCase = true)
                    if (isSuccess) {
                        toastManager.showAsync(data.data.msg.ifBlank { "发送成功" })
                        onSuccess?.invoke()
                    } else {
                        val message = data.data.msg.ifBlank { "发送评论失败" }
                        _commentComicState.update {
                            it.copy(isError = true, errorMsg = message)
                        }
                        toastManager.showAsync(message)
                    }
                }
            }
            _commentComicState.update {
                it.copy(
                    isLoading = false,
                )
            }
        }
    }

    // 评论点赞：记录已点赞的评论ID，避免重复点赞
    private val _likedCommentIds = MutableStateFlow<Set<Int>>(emptySet())
    val likedCommentIds = _likedCommentIds.asStateFlow()

    fun likeComment(commentId: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (val data = comicRepository.likeComment(commentId)) {
                is NetWorkResult.Error -> {
                    toastManager.showAsync("点赞失败：${data.message}")
                    onResult(false)
                }

                is NetWorkResult.Success<CommentComicResponse> -> {
                    _likedCommentIds.update { it + commentId }
                    onResult(true)
                }
            }
        }
    }
}
