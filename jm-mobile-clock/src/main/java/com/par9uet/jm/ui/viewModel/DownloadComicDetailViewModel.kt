package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.utils.DownloadSpeedTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DownloadComicDetailState(
    val loading: Boolean = true,
    val found: Boolean = false,
    val title: String = "",
    val authorList: List<String> = emptyList(),
    val tagList: List<String> = emptyList(),
    val coverPath: String = "",
    val remoteCoverComicId: Int = 0,
    val createTime: Long = 0L,
    val zipPath: String = "",
    val cachePath: String = "",
    val allItems: List<DownloadComic> = emptyList(),
    val completeItems: List<DownloadComic> = emptyList(),
    val readableChapters: List<ComicChapter> = emptyList(),
    val statusSummary: String = "暂无缓存",
    val downloadSpeed: Float = 0f,
) {
    val totalChapterCount: Int get() = allItems.size.coerceAtLeast(completeItems.size)
    val completeChapterCount: Int get() = completeItems.size
    val canRead: Boolean get() = completeItems.isNotEmpty()
    val isMultiChapter: Boolean get() = readableChapters.size > 1
    val groupProgress: Float get() = allItems.takeIf { it.isNotEmpty() }
        ?.map { it.progress.coerceIn(0f, 1f) }
        ?.average()?.toFloat() ?: 0f
    val hasError: Boolean get() = allItems.any { it.status == "error" }
    val isDownloading: Boolean get() = allItems.any { it.status == "downloading" || it.status == "pending" }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadComicDetailViewModel(
    private val downloadComicDao: DownloadComicDao
) : ViewModel() {

    private val _groupId = MutableStateFlow(0)
    val groupId: StateFlow<Int> = _groupId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allItemsFlow = _groupId.flatMapLatest { gid ->
        if (gid == 0) kotlinx.coroutines.flow.flowOf(emptyList())
        else downloadComicDao.observeByGroupId(gid)
    }

    private val completeItemsFlow = _groupId.flatMapLatest { gid ->
        if (gid == 0) kotlinx.coroutines.flow.flowOf(emptyList())
        else downloadComicDao.observeCompleteByGroupId(gid)
    }

    private val speedFlow = DownloadSpeedTracker.speedByGroup

    val detailState: StateFlow<DownloadComicDetailState> =
        combine(allItemsFlow, completeItemsFlow, speedFlow) { allItems, completeItems, speeds ->
            if (allItems.isEmpty() && completeItems.isEmpty()) {
                DownloadComicDetailState(loading = false)
            } else {
                val detailItems = (allItems + completeItems).distinctBy { it.id }.sortedBy { it.createTime }
                if (detailItems.isEmpty()) {
                    DownloadComicDetailState(loading = false)
                } else {
                    buildDetailState(
                        groupId = _groupId.value,
                        allItems = allItems,
                        completeItems = completeItems,
                        detailItems = detailItems,
                        downloadSpeed = speeds[_groupId.value] ?: 0f
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DownloadComicDetailState(loading = true)
        )

    fun load(id: Int) {
        viewModelScope.launch {
            val currentItem = downloadComicDao.getById(id)
            val gid = currentItem?.groupId?.takeIf { it != 0 } ?: id
            _groupId.value = gid
        }
    }

    private fun buildDetailState(
        groupId: Int,
        allItems: List<DownloadComic>,
        completeItems: List<DownloadComic>,
        detailItems: List<DownloadComic>,
        downloadSpeed: Float
    ): DownloadComicDetailState {
        val titleItem = detailItems.firstOrNull { it.groupName.isNotBlank() }
            ?: detailItems.first()
        val authorItem = detailItems.firstOrNull { it.authorList.isNotEmpty() } ?: titleItem
        val tagItem = detailItems.firstOrNull { it.tagList.isNotEmpty() } ?: titleItem
        val coverItem = detailItems.firstOrNull { it.coverPath.isNotBlank() }
        val zipItem = completeItems.firstOrNull { it.zipPath.isNotBlank() }
            ?: detailItems.firstOrNull { it.zipPath.isNotBlank() }
        val completeSorted = completeItems.sortedBy { it.createTime }
        val allSorted = allItems.sortedBy { it.createTime }

        return DownloadComicDetailState(
            loading = false,
            found = true,
            title = titleItem.groupName.ifBlank { titleItem.name },
            authorList = authorItem.authorList,
            tagList = tagItem.tagList,
            coverPath = resolveCoverPath(coverItem?.coverPath, zipItem?.zipPath),
            remoteCoverComicId = groupId,
            createTime = detailItems.maxOf { it.createTime },
            zipPath = zipItem?.zipPath.orEmpty(),
            cachePath = resolveCachePath(coverItem?.coverPath, zipItem?.zipPath),
            allItems = allSorted,
            completeItems = completeSorted,
            readableChapters = allSorted.mapIndexed { index, item ->
                ComicChapter(
                    id = item.id,
                    name = item.chapterName.ifBlank {
                        if (allSorted.size > 1) "第 ${index + 1} 章" else item.name
                    },
                    isAvailable = item.status == "complete",
                )
            },
            statusSummary = buildStatusSummary(allItems, completeItems),
            downloadSpeed = downloadSpeed
        )
    }
}

private fun resolveCachePath(coverPath: String?, zipPath: String?): String {
    val chapterPath = zipPath.orEmpty()
    if (chapterPath.isNotBlank()) {
        if (chapterPath.startsWith("content://")) {
            // SAF paths are opaque document URIs; never pass them through File,
            // which turns them into a bogus path under the default cache root.
            return chapterPath
        }
        val file = File(chapterPath)
        if (file.isDirectory) {
            return file.parentFile?.absolutePath ?: file.absolutePath
        }
        return file.absolutePath
    }
    val cover = coverPath.orEmpty()
    if (cover.isNotBlank()) {
        if (cover.startsWith("content://")) return cover
        return File(cover).parentFile?.absolutePath.orEmpty()
    }
    return ""
}

private fun resolveCoverPath(coverPath: String?, zipPath: String?): String {
    val cover = coverPath.orEmpty()
    // Preserve persisted SAF URIs. The local image composable validates the path
    // without ever falling back to a network request for cached comics.
    if (cover.isNotBlank()) {
        return cover
    }
    val chapterPath = zipPath.orEmpty()
    if (chapterPath.isNotBlank()) {
        val file = File(chapterPath)
        val rootDir = when {
            file.isDirectory -> file.parentFile
            file.isFile -> file.parentFile
            else -> null
        }
        val rootCover = rootDir?.let { File(it, "cover.webp") }
        if (rootCover?.exists() == true) {
            return rootCover.absolutePath
        }
    }
    return cover
}

private fun buildStatusSummary(
    allItems: List<DownloadComic>,
    completeItems: List<DownloadComic>
): String {
    val pendingCount = allItems.count { it.status == "pending" }
    val downloadingCount = allItems.count { it.status == "downloading" }
    val pausedCount = allItems.count { it.status == "paused" }
    val errorCount = allItems.count { it.status == "error" }
    return when {
        allItems.isEmpty() -> "暂无缓存"
        allItems.size == completeItems.size -> "全部完成"
        downloadingCount > 0 -> "缓存中 $downloadingCount 章"
        pendingCount > 0 -> "等待中 $pendingCount 章"
        pausedCount > 0 -> "已暂停 $pausedCount 章"
        errorCount > 0 -> "失败 $errorCount 章"
        else -> "已缓存 ${completeItems.size} 章"
    }
}
