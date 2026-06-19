package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.database.dao.DownloadComicDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadFilter(
    val status: String,
)

data class DownloadEditState(
    val editing: Boolean = false,
    val selectedIds: Set<Int> = emptySet()
)

class DownloadViewModel(
    private val downloadComicDao: DownloadComicDao
) : ViewModel() {
    private val _downloadFilterState = MutableStateFlow(DownloadFilter("downloading"))
    val downloadFilterState = _downloadFilterState.asStateFlow()

    private val _editState = MutableStateFlow(DownloadEditState())
    val editState = _editState.asStateFlow()

    val completeList = downloadComicDao.observeCompleteList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeList = downloadComicDao.observeActiveList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val errorList = downloadComicDao.observeErrorList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateDownloadStatusFilter(status: String) {
        _downloadFilterState.update { it.copy(status = status) }
        clearSelection()
    }

    fun enterEdit(id: Int) {
        _editState.update {
            it.copy(editing = true, selectedIds = it.selectedIds + id)
        }
    }

    fun toggleSelected(id: Int) {
        _editState.update {
            val selected = if (id in it.selectedIds) {
                it.selectedIds - id
            } else {
                it.selectedIds + id
            }
            it.copy(editing = selected.isNotEmpty(), selectedIds = selected)
        }
    }

    fun clearSelection() {
        _editState.update { DownloadEditState() }
    }

    fun deleteSelected() {
        val ids = _editState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            downloadComicDao.deleteByIds(ids)
            clearSelection()
        }
    }

    fun deleteOne(id: Int) {
        viewModelScope.launch {
            downloadComicDao.deleteByIds(listOf(id))
            _editState.update {
                val selected = it.selectedIds - id
                it.copy(editing = selected.isNotEmpty(), selectedIds = selected)
            }
        }
    }

    fun pauseSelected() {
        updateSelectedStatus("paused")
    }

    fun startSelected() {
        updateSelectedStatus("pending")
    }

    private fun updateSelectedStatus(status: String) {
        val ids = _editState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            downloadComicDao.updateStatusByIds(ids, status)
            clearSelection()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadPager = _downloadFilterState.flatMapLatest { filter ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 6,
                initialLoadSize = 20
            ),
        ) {
            when (filter.status) {
                "complete" -> downloadComicDao.getCompleteList()
                "error" -> downloadComicDao.getErrorList()
                else -> downloadComicDao.getActiveList()
            }
        }.flow
    }.cachedIn(viewModelScope)
}
