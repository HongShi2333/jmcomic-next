package com.par9uet.jm.ui.screens.downloadScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.DownloadComicGroup
import com.par9uet.jm.ui.viewModel.DownloadViewModel
import com.par9uet.jm.store.LocalSettingManager
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun DownloadScreen(
    downloadViewModel: DownloadViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val completeGroups by downloadViewModel.completeGroups.collectAsState()
    val activeGroups by downloadViewModel.activeGroups.collectAsState()
    val errorGroups by downloadViewModel.errorGroups.collectAsState()
    val editState by downloadViewModel.editState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    var completeExpanded by rememberSaveable { mutableStateOf(true) }
    var activeExpanded by rememberSaveable { mutableStateOf(true) }
    var errorExpanded by rememberSaveable { mutableStateOf(true) }

    CommonScaffold(title = "下载") {
        Column {
            if (editState.editing) {
                // 仅当选中项中存在"正在缓存"分组时才显示暂停/继续按钮
                val activeItemIds = remember(activeGroups) {
                    activeGroups.flatMap { it.itemIds }.toSet()
                }
                val showPauseResume = remember(editState.selectedIds, activeItemIds) {
                    editState.selectedIds.any { it in activeItemIds }
                }
                DownloadEditBar(
                    selectedCount = editState.selectedIds.size,
                    showPauseResume = showPauseResume,
                    onClose = downloadViewModel::clearSelection,
                    onDelete = downloadViewModel::deleteSelected,
                    onPause = downloadViewModel::pauseSelected,
                    onStart = downloadViewModel::startSelected,
                    onRedownload = downloadViewModel::redownloadSelected
                )
            }
            val activeCount = activeGroups.size
            val errorCount = errorGroups.size
            val completeCount = completeGroups.size
            val totalCount = activeCount + errorCount + completeCount

            if (totalCount == 0) {
                DownloadEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (activeCount > 0) {
                        item {
                            DownloadSectionHeader(
                                title = "正在缓存",
                                count = activeCount,
                                expanded = activeExpanded,
                                onClick = { activeExpanded = !activeExpanded },
                                accentColor = MaterialTheme.colorScheme.primary,
                                icon = Icons.Rounded.Schedule
                            )
                        }
                        if (activeExpanded) {
                            items(activeGroups, key = { "active_${it.id}" }) { group ->
                                DownloadRowItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    group = group,
                                    editing = editState.editing,
                                    selected = editState.selectedIds.containsAll(group.itemIds),
                                    onClick = {
                                        if (editState.editing) {
                                            downloadViewModel.toggleSelected(group.itemIds)
                                        } else {
                                            mainNavController.navigate("downloadComicDetail/${group.id}")
                                        }
                                    },
                                    onLongClick = { downloadViewModel.enterEdit(group.itemIds) },
                                    onCancel = { downloadViewModel.deleteMany(group.itemIds) }
                                )
                            }
                        }
                    }

                    if (completeCount > 0) {
                        item {
                            DownloadSectionHeader(
                                title = "缓存完成",
                                count = completeCount,
                                expanded = completeExpanded,
                                onClick = { completeExpanded = !completeExpanded },
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                icon = Icons.Rounded.DownloadDone
                            )
                        }
                        if (completeExpanded) {
                            item {
                                CompletedGrid(
                                    groups = completeGroups,
                                    editing = editState.editing,
                                    selectedIds = editState.selectedIds,
                                    gridColumns = localSetting.downloadGridColumns,
                                    onClick = { group ->
                                        if (editState.editing) {
                                            downloadViewModel.toggleSelected(group.itemIds)
                                        } else {
                                            mainNavController.navigate("downloadComicDetail/${group.id}")
                                        }
                                    },
                                    onLongClick = { group ->
                                        downloadViewModel.enterEdit(group.itemIds)
                                    }
                                )
                            }
                        }
                    }

                    if (errorCount > 0) {
                        item {
                            DownloadSectionHeader(
                                title = "缓存失败",
                                count = errorCount,
                                expanded = errorExpanded,
                                onClick = { errorExpanded = !errorExpanded },
                                accentColor = MaterialTheme.colorScheme.error,
                                icon = Icons.Rounded.ErrorOutline
                            )
                        }
                        if (errorExpanded) {
                            items(errorGroups, key = { "error_${it.id}" }) { group ->
                                DownloadRowItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    group = group,
                                    editing = editState.editing,
                                    selected = editState.selectedIds.containsAll(group.itemIds),
                                    onClick = {
                                        if (editState.editing) {
                                            downloadViewModel.toggleSelected(group.itemIds)
                                        } else {
                                            mainNavController.navigate("downloadComicDetail/${group.id}")
                                        }
                                    },
                                    onLongClick = { downloadViewModel.enterEdit(group.itemIds) },
                                    onCancel = { downloadViewModel.deleteMany(group.itemIds) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.DownloadDone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "暂无缓存任务",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "从漫画详情页点击缓存即可在此查看",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun DownloadSectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            BadgedBox(
                badge = {
                    if (count > 0) {
                        Badge(containerColor = accentColor.copy(alpha = 0.2f)) {
                            Text(
                                text = count.toString(),
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {}
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletedGrid(
    groups: List<DownloadComicGroup>,
    editing: Boolean,
    selectedIds: Set<Int>,
    gridColumns: Int,
    onClick: (DownloadComicGroup) -> Unit,
    onLongClick: (DownloadComicGroup) -> Unit
) {
    val configuration = LocalConfiguration.current
    val columns = if (gridColumns > 0) {
        gridColumns
    } else {
        when {
            configuration.screenWidthDp >= 600 -> 4
            configuration.screenWidthDp >= 400 -> 3
            else -> 2
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { group ->
                    DownloadCoverGridItem(
                        modifier = Modifier.weight(1f),
                        group = group,
                        editing = editing,
                        selected = selectedIds.containsAll(group.itemIds),
                        onClick = { onClick(group) },
                        onLongClick = { onLongClick(group) }
                    )
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DownloadEditBar(
    selectedCount: Int,
    showPauseResume: Boolean,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onStart: () -> Unit,
    onRedownload: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "退出编辑")
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = "已选择 $selectedCount 项",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (showPauseResume) {
                    TextButton(onClick = onStart) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("继续")
                    }
                    TextButton(onClick = onPause) {
                        Icon(
                            Icons.Rounded.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("暂停")
                    }
                }
                TextButton(onClick = onRedownload) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("重下")
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
