package com.par9uet.jm.ui.screens.downloadScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.DownloadComicGroup
import com.par9uet.jm.ui.viewModel.DownloadViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun DownloadScreen(
    downloadViewModel: DownloadViewModel = koinActivityViewModel()
) {
    val mainNavController = LocalMainNavController.current
    val completeGroups by downloadViewModel.completeGroups.collectAsState()
    val activeGroups by downloadViewModel.activeGroups.collectAsState()
    val errorGroups by downloadViewModel.errorGroups.collectAsState()
    val editState by downloadViewModel.editState.collectAsState()
    var completeExpanded by rememberSaveable { mutableStateOf(true) }
    var activeExpanded by rememberSaveable { mutableStateOf(false) }
    var errorExpanded by rememberSaveable { mutableStateOf(false) }

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
                        count = completeGroups.size,
                        expanded = completeExpanded,
                        onClick = { completeExpanded = !completeExpanded }
                    )
                }
                item {
                    AnimatedVisibility(visible = completeExpanded) {
                        CompletedGrid(
                            groups = completeGroups,
                            editing = editState.editing,
                            selectedIds = editState.selectedIds,
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
                item {
                    DownloadSectionHeader(
                        title = "正在缓存",
                        count = activeGroups.size,
                        expanded = activeExpanded,
                        onClick = { activeExpanded = !activeExpanded }
                    )
                }
                if (activeExpanded) {
                    items(activeGroups, key = { it.id }) { group ->
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
                item {
                    DownloadSectionHeader(
                        title = "缓存失败",
                        count = errorGroups.size,
                        expanded = errorExpanded,
                        onClick = { errorExpanded = !errorExpanded }
                    )
                }
                if (errorExpanded) {
                    items(errorGroups, key = { it.id }) { group ->
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

@Composable
private fun DownloadSectionHeader(
    title: String,
    count: Int,
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
                text = "$title ($count)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
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
    onClick: (DownloadComicGroup) -> Unit,
    onLongClick: (DownloadComicGroup) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.chunked(3).forEach { row ->
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
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
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
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "退出编辑")
            }
            Text(
                modifier = Modifier.weight(1f),
                text = "已选择 $selectedCount 项"
            )
            IconButton(onClick = onPause) {
                Icon(Icons.Rounded.Pause, contentDescription = "暂停")
            }
            IconButton(onClick = onStart) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "继续")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "删除")
            }
        }
    }
}
