package com.par9uet.jm.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.data.models.ComicSearchOrderFilter
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.ComicPaginationBar
import com.par9uet.jm.ui.components.ComicPagePreloadEffect
import com.par9uet.jm.ui.components.ComicSkeleton
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.components.rememberComicPageGridState
import com.par9uet.jm.ui.components.ScrollToTopButton
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.ui.viewModel.ComicViewModel
import com.par9uet.jm.utils.serializeExcludedTags
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
private fun ComicSearchResultSkeleton(
    modifier: Modifier = Modifier,
    columns: GridCells,
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = columns,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    ) {
        items(12) {
            ComicSkeleton(modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicSearchResultScreen(
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val comicSearchLazyPagingItems = comicViewModel.searchComicPager.collectAsLazyPagingItems()
    val pageState by comicViewModel.searchPagination.state.collectAsState()
    val comicSearchFilterState by comicViewModel.searchComicFilterState.collectAsState()
    val searchComicIdState by comicViewModel.searchComicIdState.collectAsState()
    ComicPagePreloadEffect(
        controller = comicViewModel.searchPagination,
        ready = comicSearchLazyPagingItems.itemCount > 0 &&
            comicSearchLazyPagingItems.loadState.refresh is LoadState.NotLoading,
    )

    fun editRoute(): String {
        val encodedSearchContent = Uri.encode(comicSearchFilterState.searchContent)
        val encodedExcludedTags = Uri.encode(serializeExcludedTags(comicSearchFilterState.excludedTags))
        return "comicSearch?searchContent=$encodedSearchContent&excludedTags=$encodedExcludedTags&mainTag=${comicSearchFilterState.mainTag}"
    }

    fun navigateToSearchEditor() {
        val previousRoute = mainNavController.previousBackStackEntry?.destination?.route.orEmpty()
        val previousIsSearchEditor = previousRoute == "comicSearch" || previousRoute.startsWith("comicSearch?")
        if (previousIsSearchEditor && mainNavController.popBackStack()) return

        mainNavController.popBackStack()
        mainNavController.navigate(editRoute()) {
            launchSingleTop = true
        }
    }

    BackHandler {
        navigateToSearchEditor()
    }

    LaunchedEffect(searchComicIdState) {
        val comicId = searchComicIdState ?: return@LaunchedEffect
        comicDetailViewModel.reset(comicId)
        comicViewModel.consumeSearchComicId()
        mainNavController.navigate("comicDetail/$comicId") {
            launchSingleTop = true
        }
    }

    val refreshState = comicSearchLazyPagingItems.loadState.refresh
    val isLoading = refreshState is LoadState.Loading
    val refreshError = refreshState as? LoadState.Error
    val gridState = rememberComicPageGridState(pageState.page, comicSearchFilterState)
    val gridColumns = adaptiveComicGridCells(localSetting.searchGridColumns)

    CommonScaffold(
        title = "搜索结果",
        onNavigateBack = { navigateToSearchEditor() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchResultSummaryCard(
                query = comicSearchFilterState.searchContent,
                excludedTags = comicSearchFilterState.excludedTags,
                order = comicSearchFilterState.order,
                loadedCount = comicSearchLazyPagingItems.itemCount,
                onEdit = { navigateToSearchEditor() },
            )
            OrderFilterRow(
                currentOrder = comicSearchFilterState.order,
                onOrderChange = { comicViewModel.changeSearchComicOrderFilter(it) },
            )
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading && comicSearchLazyPagingItems.itemCount == 0 -> {
                        ComicSearchResultSkeleton(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp),
                            columns = gridColumns,
                        )
                    }

                    refreshError != null && comicSearchLazyPagingItems.itemCount == 0 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SearchResultMessageCard(
                                title = "搜索结果加载失败",
                                message = refreshError.error.message ?: "请检查网络后重试",
                                action = { comicSearchLazyPagingItems.retry() },
                            )
                        }
                    }

                    !isLoading && comicSearchLazyPagingItems.itemCount == 0 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SearchResultMessageCard(
                                title = "没有找到漫画",
                                message = "可以修改关键词、排序或排除标签后重试",
                                action = { navigateToSearchEditor() },
                                actionLabel = "修改搜索",
                            )
                        }
                    }

                    else -> {
                        PullRefreshAndLoadMoreGrid(
                            modifier = Modifier.fillMaxSize(),
                            gridState = gridState,
                            lazyPagingItems = comicSearchLazyPagingItems,
                            key = { it.id },
                            columns = gridColumns,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            pageState = pageState,
                            onPageChange = comicViewModel.searchPagination::selectPage,
                            onRefresh = comicViewModel.searchPagination::refresh,
                        ) {
                            Comic(it)
                        }
                        ScrollToTopButton(
                            gridState = gridState,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (pageState.enabled) 94.dp else 16.dp),
                        )
                    }
                }
            }
            // Keep navigation available when a non-first page fails to load.
            if (pageState.enabled && comicSearchLazyPagingItems.itemCount == 0) {
                ComicPaginationBar(
                    state = pageState,
                    onPageChange = comicViewModel.searchPagination::selectPage,
                )
            }
        }
    }
}

@Composable
private fun SearchResultSummaryCard(
    query: String,
    excludedTags: List<String>,
    order: ComicSearchOrderFilter,
    loadedCount: Int,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = query.ifBlank { "全部漫画" },
                        modifier = Modifier.clickable(onClick = onEdit),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "已加载 $loadedCount 部 · 当前排序：${order.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onEdit) {
                    Text("修改")
                }
            }
            if (excludedTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    excludedTags.distinct().take(8).forEach { tag ->
                        AssistChip(
                            onClick = onEdit,
                            label = {
                                Text(
                                    text = "-$tag",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultMessageCard(
    title: String,
    message: String,
    action: () -> Unit,
    actionLabel: String = "重试",
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = action) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun OrderFilterRow(
    currentOrder: ComicSearchOrderFilter,
    onOrderChange: (ComicSearchOrderFilter) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "排序",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ComicSearchOrderFilter.entries.forEach { item ->
                FilterChip(
                    selected = item == currentOrder,
                    onClick = { onOrderChange(item) },
                    label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}
