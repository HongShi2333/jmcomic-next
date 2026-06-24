package com.par9uet.jm.ui.screens.downloadScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.database.dao.ChapterProgressDao
import com.par9uet.jm.database.dao.ReadingProgressDao
import com.par9uet.jm.database.model.ChapterProgress
import com.par9uet.jm.database.model.ReadingProgress
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.DownloadComicGroup
import com.par9uet.jm.ui.viewModel.DownloadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun DownloadScreen(
    downloadViewModel: DownloadViewModel = koinActivityViewModel(),
    readingProgressDao: ReadingProgressDao = getKoin().get(),
    chapterProgressDao: ChapterProgressDao = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val coroutineScope = rememberCoroutineScope()
    val completeGroups by downloadViewModel.completeGroups.collectAsState()
    val activeGroups by downloadViewModel.activeGroups.collectAsState()
    val errorGroups by downloadViewModel.errorGroups.collectAsState()
    val editState by downloadViewModel.editState.collectAsState()
    var completeExpanded by rememberSaveable { mutableStateOf(true) }
    var activeExpanded by rememberSaveable { mutableStateOf(false) }
    var errorExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedGroupKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var progressMap by remember { mutableStateOf<Map<Int, ReadingProgress>>(emptyMap()) }
    var chapterProgressMap by remember { mutableStateOf<Map<Int, ChapterProgress>>(emptyMap()) }

    // Load reading progress for all complete groups
    LaunchedEffect(completeGroups) {
        coroutineScope.launch {
            val parentIds = completeGroups.map { group ->
                val primary = group.primary
                if (primary.parentId != 0) primary.parentId else primary.id
            }.distinct()

            val progresses = withContext(Dispatchers.IO) {
                parentIds.mapNotNull { parentId ->
                    readingProgressDao.getProgress(parentId)?.let { parentId to it }
                }.toMap()
            }
            progressMap = progresses

            // Load chapter progress for all chapters
            val allChapterIds = completeGroups.flatMap { it.ids }
            val chapterProgresses = withContext(Dispatchers.IO) {
                allChapterIds.mapNotNull { chapterId ->
                    chapterProgressDao.getProgress(chapterId)?.let { chapterId to it }
                }.toMap()
            }
            chapterProgressMap = chapterProgresses
        }
    }

    fun toggleGroup(key: String) {
        expandedGroupKeys = if (key in expandedGroupKeys) {
            expandedGroupKeys - key
        } else {
            expandedGroupKeys + key
        }
    }

    CommonScaffold(title = "下载") {
        Column {
            if (editState.editing) {
                DownloadEditBar(
                    selectedCount = editState.selectedIds.size,
                    onClose = downloadViewModel::clearSelection,
                    onDelete = downloadViewModel::deleteSelected,
                    onPause = downloadViewModel::pauseSelected,
                    onStart = downloadViewModel::startSelected
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DownloadSectionHeader(
                        title = "缓存完成",
                        countText = sectionCountText(completeGroups),
                        expanded = completeExpanded,
                        onClick = { completeExpanded = !completeExpanded }
                    )
                }
                if (completeExpanded) {
                    items(completeGroups, key = { "complete-${it.key}" }) { group ->
                        val groupKey = "complete-${group.key}"
                        val primary = group.primary
                        val parentId = if (primary.parentId != 0) primary.parentId else primary.id
                        val progress = progressMap[parentId]

                        DownloadGroupRowItem(
                            modifier = Modifier.fillMaxWidth(),
                            group = group,
                            expanded = groupKey in expandedGroupKeys,
                            editing = editState.editing,
                            selected = group.ids.all { it in editState.selectedIds },
                            readingProgress = progress,
                            chapterProgressMap = chapterProgressMap,
                            onClick = {
                                if (editState.editing) {
                                    downloadViewModel.toggleSelected(group.ids)
                                } else {
                                    // Click card: navigate to last read chapter
                                    val targetChapterId = progress?.chapterId ?: primary.id
                                    mainNavController.navigate("localComicRead/$targetChapterId")
                                }
                            },
                            onLongClick = { downloadViewModel.enterEdit(group.ids) },
                            onExpandClick = { toggleGroup(groupKey) },
                            onChapterClick = { chapter ->
                                mainNavController.navigate("downloadComicDetail/${chapter.id}")
                            }
                        )
                    }
                }
                item {
                    DownloadSectionHeader(
                        title = "正在缓存",
                        countText = sectionCountText(activeGroups),
                        expanded = activeExpanded,
                        onClick = { activeExpanded = !activeExpanded }
                    )
                }
                if (activeExpanded) {
                    items(activeGroups, key = { "active-${it.key}" }) { group ->
                        val groupKey = "active-${group.key}"
                        DownloadGroupRowItem(
                            modifier = Modifier.fillMaxWidth(),
                            group = group,
                            expanded = groupKey in expandedGroupKeys,
                            editing = editState.editing,
                            selected = group.ids.all { it in editState.selectedIds },
                            onClick = {
                                if (editState.editing) {
                                    downloadViewModel.toggleSelected(group.ids)
                                } else if (group.chapterSize > 1) {
                                    toggleGroup(groupKey)
                                }
                            },
                            onLongClick = { downloadViewModel.enterEdit(group.ids) },
                            onExpandClick = { toggleGroup(groupKey) },
                            onCancel = { downloadViewModel.deleteOne(group.ids) }
                        )
                    }
                }
                item {
                    DownloadSectionHeader(
                        title = "发生错误",
                        countText = sectionCountText(errorGroups),
                        expanded = errorExpanded,
                        onClick = { errorExpanded = !errorExpanded }
                    )
                }
                if (errorExpanded) {
                    items(errorGroups, key = { "error-${it.key}" }) { group ->
                        val groupKey = "error-${group.key}"
                        DownloadGroupRowItem(
                            modifier = Modifier.fillMaxWidth(),
                            group = group,
                            expanded = groupKey in expandedGroupKeys,
                            editing = editState.editing,
                            selected = group.ids.all { it in editState.selectedIds },
                            onClick = {
                                if (editState.editing) {
                                    downloadViewModel.toggleSelected(group.ids)
                                } else if (group.chapterSize > 1) {
                                    toggleGroup(groupKey)
                                }
                            },
                            onLongClick = { downloadViewModel.enterEdit(group.ids) },
                            onExpandClick = { toggleGroup(groupKey) },
                            onCancel = { downloadViewModel.deleteOne(group.ids) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadSectionHeader(
    title: String,
    countText: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "$title ($countText)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DownloadEditBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "退出编辑")
            }
            Text(
                modifier = Modifier.weight(1f),
                text = "已选择 $selectedCount 话"
            )
            IconButton(onClick = onPause) {
                Icon(Icons.Rounded.Pause, contentDescription = "暂停")
            }
            IconButton(onClick = onStart) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "开始")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "删除")
            }
        }
    }
}

private fun sectionCountText(groups: List<DownloadComicGroup>): String {
    val chapterCount = groups.sumOf { it.chapterSize }
    return if (chapterCount == groups.size) {
        "${groups.size} 本"
    } else {
        "${groups.size} 本 / $chapterCount 话"
    }
}
