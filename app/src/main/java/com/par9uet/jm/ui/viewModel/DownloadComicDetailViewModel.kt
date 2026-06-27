package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.DownloadComic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val statusSummary: String = "暂无缓存"
) {
    val totalChapterCount: Int get() = allItems.size.coerceAtLeast(completeItems.size)
    val completeChapterCount: Int get() = completeItems.size
    val canRead: Boolean get() = completeItems.isNotEmpty()
    val isMultiChapter: Boolean get() = readableChapters.size > 1
}

class DownloadComicDetailViewModel(
    private val downloadComicDao: DownloadComicDao
) : ViewModel() {
    private val _detailState = MutableStateFlow(DownloadComicDetailState())
    val detailState = _detailState.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _detailState.value = DownloadComicDetailState(loading = true)

            val currentItem = downloadComicDao.getById(id)
            val groupId = currentItem?.groupId?.takeIf { it != 0 } ?: id
            val allItems = downloadComicDao.getByGroupId(groupId)
            val completeItems = downloadComicDao.getCompleteByGroupId(groupId)
            val detailItems = (allItems + completeItems + listOfNotNull(currentItem))
                .distinctBy { it.id }
                .sortedBy { it.createTime }

            _detailState.value = if (detailItems.isEmpty()) {
                DownloadComicDetailState(loading = false)
            } else {
                buildDetailState(
                    requestedId = id,
                    groupId = groupId,
                    allItems = allItems.ifEmpty { detailItems },
                    completeItems = completeItems,
                    detailItems = detailItems
                )
            }
        }
    }

    private fun buildDetailState(
        requestedId: Int,
        groupId: Int,
        allItems: List<DownloadComic>,
        completeItems: List<DownloadComic>,
        detailItems: List<DownloadComic>
    ): DownloadComicDetailState {
        val titleItem = detailItems.firstOrNull { it.groupName.isNotBlank() }
            ?: detailItems.first()
        val authorItem = detailItems.firstOrNull { it.authorList.isNotEmpty() } ?: titleItem
        val tagItem = detailItems.firstOrNull { it.tagList.isNotEmpty() } ?: titleItem
        val coverItem = detailItems.firstOrNull { it.coverPath.isNotBlank() }
        val zipItem = completeItems.firstOrNull { it.zipPath.isNotBlank() }
            ?: detailItems.firstOrNull { it.zipPath.isNotBlank() }
        val remoteCoverComicId = titleItem.groupId.takeIf { it != 0 }
            ?: groupId.takeIf { it != 0 }
            ?: requestedId
        val completeSorted = completeItems.sortedBy { it.createTime }

        return DownloadComicDetailState(
            loading = false,
            found = true,
            title = titleItem.groupName.ifBlank { titleItem.name },
            authorList = authorItem.authorList,
            tagList = tagItem.tagList,
            coverPath = resolveCoverPath(coverItem?.coverPath, zipItem?.zipPath),
            remoteCoverComicId = remoteCoverComicId,
            createTime = detailItems.maxOf { it.createTime },
            zipPath = zipItem?.zipPath.orEmpty(),
            cachePath = resolveCachePath(coverItem?.coverPath, zipItem?.zipPath),
            allItems = allItems.sortedBy { it.createTime },
            completeItems = completeSorted,
            readableChapters = completeSorted.mapIndexed { index, item ->
                ComicChapter(
                    id = item.id,
                    name = item.chapterName.ifBlank {
                        if (completeSorted.size > 1) "第 ${index + 1} 章" else item.name
                    }
                )
            },
            statusSummary = buildStatusSummary(allItems, completeItems)
        )
    }
}

private fun resolveCachePath(coverPath: String?, zipPath: String?): String {
    val chapterPath = zipPath.orEmpty()
    if (chapterPath.isNotBlank()) {
        val file = File(chapterPath)
        if (file.isDirectory) {
            return file.parentFile?.absolutePath ?: file.absolutePath
        }
        return file.absolutePath
    }
    val cover = coverPath.orEmpty()
    if (cover.isNotBlank()) {
        return File(cover).parentFile?.absolutePath.orEmpty()
    }
    return ""
}

private fun resolveCoverPath(coverPath: String?, zipPath: String?): String {
    val cover = coverPath.orEmpty()
    if (cover.isNotBlank() && File(cover).exists()) {
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
