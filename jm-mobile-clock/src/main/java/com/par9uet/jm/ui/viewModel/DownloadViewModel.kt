package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.store.DownloadManager
import com.par9uet.jm.store.DownloadProgressMessageStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

data class DownloadComicGroup(
    val id: Int,
    val name: String,
    val authorList: List<String>,
    val coverPath: String,
    val itemIds: Set<Int>,
    val chapterCount: Int,
    val latestTime: Long,
    val status: String,
    val progress: Float,
    val progressMessage: String = "",
)

class DownloadViewModel(
    private val downloadComicDao: DownloadComicDao,
    private val downloadManager: DownloadManager
) : ViewModel() {
    private val _downloadFilterState = MutableStateFlow(DownloadFilter("downloading"))
    val downloadFilterState = _downloadFilterState.asStateFlow()

    private val _editState = MutableStateFlow(DownloadEditState())
    val editState = _editState.asStateFlow()
    val allGroups = downloadComicDao.observeAll().combine(DownloadProgressMessageStore.messages) { items, messages ->
        groupDownloads(items, messages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completeList = downloadComicDao.observeCompleteList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeList = downloadComicDao.observeActiveList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val errorList = downloadComicDao.observeErrorList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completeGroups = downloadComicDao.observeCompleteList()
        .combine(DownloadProgressMessageStore.messages) { items, messages ->
            groupDownloads(items, messages)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeGroups = combine(activeList, completeList, DownloadProgressMessageStore.messages) { activeItems, completeItems, messages ->
        groupActiveDownloads(activeItems, completeItems, messages)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val errorGroups = downloadComicDao.observeErrorList()
        .combine(DownloadProgressMessageStore.messages) { items, messages ->
            groupDownloads(items, messages)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateDownloadStatusFilter(status: String) {
        _downloadFilterState.update { it.copy(status = status) }
        clearSelection()
    }

    fun enterEdit(id: Int) {
        enterEdit(setOf(id))
    }

    fun enterEdit(ids: Set<Int>) {
        _editState.update {
            it.copy(editing = true, selectedIds = it.selectedIds + ids)
        }
    }

    fun toggleSelected(id: Int) {
        toggleSelected(setOf(id))
    }

    fun toggleSelected(ids: Set<Int>) {
        _editState.update {
            val allSelected = ids.all { id -> id in it.selectedIds }
            val selected = if (allSelected) {
                it.selectedIds - ids
            } else {
                it.selectedIds + ids
            }
            it.copy(editing = selected.isNotEmpty(), selectedIds = selected)
        }
    }

    fun setSelected(ids: Set<Int>) {
        _editState.update {
            it.copy(
                editing = ids.isNotEmpty(),
                selectedIds = ids
            )
        }
    }

    fun clearSelection() {
        _editState.update { DownloadEditState() }
    }

    fun deleteSelected() {
        val ids = _editState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            downloadManager.deleteCachedItems(ids)
            clearSelection()
        }
    }

    fun deleteOne(id: Int) {
        deleteMany(setOf(id))
    }

    fun deleteMany(ids: Set<Int>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            downloadManager.deleteCachedItems(ids)
            _editState.update {
                val selected = it.selectedIds - ids
                it.copy(editing = selected.isNotEmpty(), selectedIds = selected)
            }
        }
    }

    fun pauseSelected() {
        downloadManager.pauseDownloads(_editState.value.selectedIds)
        clearSelection()
    }

    fun startSelected() {
        val ids = _editState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        clearSelection()
        downloadManager.resumeDownloads(ids)
    }

    private fun updateSelectedStatus(status: String) {
        val ids = _editState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            downloadComicDao.updateStatusByIds(ids, status)
            clearSelection()
        }
    }

    fun redownloadSelected() {
        val ids = _editState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val groupIds = mutableSetOf<Int>()
            ids.forEach { id ->
                val item = downloadComicDao.getById(id)
                if (item != null) {
                    groupIds.add(if (item.groupId != 0) item.groupId else item.id)
                }
            }
            groupIds.forEach { groupId ->
                downloadManager.redownloadGroup(groupId)
            }
            clearSelection()
        }
    }

    fun redownloadOne(groupId: Int) {
        downloadManager.redownloadGroup(groupId)
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

internal fun groupDownloads(
    items: List<DownloadComic>,
    progressMessages: Map<Int, String> = emptyMap(),
): List<DownloadComicGroup> {
    return items
        .groupBy(::downloadGroupId)
        .values
        .map { groupItems ->
            val sortedItems = groupItems.sortedBy { it.createTime }
            val displayItem = sortedItems.firstOrNull { it.coverPath.isNotBlank() } ?: sortedItems.first()
            DownloadComicGroup(
                id = if (displayItem.groupId != 0) displayItem.groupId else displayItem.id,
                name = displayItem.groupName.ifBlank { displayItem.name },
                authorList = displayItem.authorList,
                coverPath = resolveGroupCoverPath(sortedItems, displayItem),
                itemIds = sortedItems.map { it.id }.toSet(),
                chapterCount = sortedItems.size,
                latestTime = sortedItems.maxOf { it.createTime },
                status = resolveGroupStatus(sortedItems),
                progress = sortedItems.map { it.progress.coerceIn(0f, 1f) }.average().toFloat(),
                progressMessage = progressMessages[downloadGroupId(displayItem)].orEmpty(),
            )
        }
        .sortedByDescending { it.latestTime }
}

private fun groupActiveDownloads(
    activeItems: List<DownloadComic>,
    completeItems: List<DownloadComic>,
    progressMessages: Map<Int, String> = emptyMap(),
): List<DownloadComicGroup> {
    val activeGroupIds = activeItems.map(::downloadGroupId).toSet()
    val relatedCompleteItems = completeItems.filter { item ->
        downloadGroupId(item) in activeGroupIds
    }
    return groupDownloads(activeItems + relatedCompleteItems, progressMessages)
}

private fun downloadGroupId(item: DownloadComic): Int {
    return if (item.groupId != 0) item.groupId else item.id
}

private fun resolveGroupCoverPath(items: List<DownloadComic>, displayItem: DownloadComic): String {
    for (item in items) {
        if (item.coverPath.isNotBlank()) return item.coverPath
    }
    return displayItem.coverPath
}

private fun resolveGroupStatus(items: List<DownloadComic>): String {
    return when {
        items.any { it.status == "downloading" } -> "downloading"
        items.any { it.status == "pending" } -> "pending"
        items.any { it.status == "paused" } -> "paused"
        items.any { it.status == "error" } -> "error"
        else -> items.firstOrNull()?.status ?: "pending"
    }
}
