package com.par9uet.jm.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.data.models.CollectComicOrderFilter
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.ComicSkeleton
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.FilterItem
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.viewModel.UserViewModel
import org.koin.compose.viewmodel.koinActivityViewModel


@Composable
private fun UserCollectComicSkeleton(
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
    ) {
        for (i in 0 until 18) {
            key(i) {
                ComicSkeleton(
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCollectComicScreen(
    userViewModel: UserViewModel = koinActivityViewModel(),
    useScaffold: Boolean = true,
) {
    val collectComicLazyPagingItems = userViewModel.collectComicPager.collectAsLazyPagingItems()
    val order by userViewModel.collectComicOrder.collectAsState()
    val collectComicFilter by userViewModel.collectComicFilter.collectAsState()
    val tagCountMap by userViewModel.collectTagCounts.collectAsState()
    var draftSelectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showTagFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userViewModel.refreshCollectTagCounts()
    }

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CollectComicOrderFilter.entries.forEach { item ->
                    key(item.label) {
                        FilterItem(
                            label = item.label,
                            onClick = {
                                userViewModel.changeCollectComicOrder(item)
                            },
                            active = item.value == order.value
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = collectComicFilter.searchText,
                    onValueChange = { userViewModel.updateCollectSearchText(it) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    label = { Text("搜索收藏夹") }
                )
                Button(
                    onClick = {
                        draftSelectedTags = collectComicFilter.selectedTags
                        showTagFilterDialog = true
                    }
                ) {
                    Icon(Icons.Rounded.FilterList, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("筛选")
                }
            }
            HorizontalDivider()
            if (collectComicLazyPagingItems.loadState.refresh is LoadState.Loading && collectComicLazyPagingItems.itemCount == 0) {
                UserCollectComicSkeleton(
                    modifier = Modifier.weight(1f)
                )
            } else {
                PullRefreshAndLoadMoreGrid(
                    modifier = Modifier.weight(1f),
                    lazyPagingItems = collectComicLazyPagingItems,
                    key = { it.id },
                    columns = adaptiveComicGridCells()
                ) {
                    Comic(it)
                }
            }
        }
    }

    if (useScaffold) {
        CommonScaffold(title = "我的收藏") {
            content()
        }
    } else {
        content()
    }

    if (showTagFilterDialog) {
        AlertDialog(
            onDismissRequest = { showTagFilterDialog = false },
            title = { Text("筛选收藏 tag") },
            text = {
                if (tagCountMap.isEmpty()) {
                    Text("当前已加载收藏中没有可筛选的 tag")
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tagCountMap.forEach { (tag, count) ->
                            FilterChip(
                                selected = tag in draftSelectedTags,
                                onClick = {
                                    draftSelectedTags = if (tag in draftSelectedTags) {
                                        draftSelectedTags - tag
                                    } else {
                                        draftSelectedTags + tag
                                    }
                                },
                                label = { Text("${tag}x$count") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    userViewModel.updateCollectSelectedTags(draftSelectedTags)
                    showTagFilterDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    draftSelectedTags = emptySet()
                    userViewModel.updateCollectSelectedTags(emptySet())
                    showTagFilterDialog = false
                }) {
                    Text("清空")
                }
            }
        )
    }
}
