package com.par9uet.jm.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.data.models.ComicRankCategory
import com.par9uet.jm.data.models.ComicRankPeriod
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.ComicPaginationBar
import com.par9uet.jm.ui.components.ComicPagePreloadEffect
import com.par9uet.jm.ui.components.ComicSkeleton
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.components.rememberComicPageGridState
import com.par9uet.jm.ui.components.adaptiveComicGridCells
import com.par9uet.jm.ui.viewModel.ComicViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComicRankingScreen(
    targetType: String,
    targetValue: String,
    periodValue: String,
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    localSettingManager: com.par9uet.jm.store.LocalSettingManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val category = if (targetType == CATEGORY_TARGET) {
        ComicRankCategory.fromApiValue(targetValue)
    } else {
        null
    }
    val tagName = if (category == null) targetValue else ""
    val title = category?.label ?: targetValue
    val period = ComicRankPeriod.fromRouteValue(periodValue)
    val pagingItems = comicViewModel.comicRankingPager.collectAsLazyPagingItems()
    val pageState by comicViewModel.rankingPagination.state.collectAsState()
    ComicPagePreloadEffect(
        controller = comicViewModel.rankingPagination,
        ready = pagingItems.itemCount > 0 && pagingItems.loadState.refresh is LoadState.NotLoading,
    )
    val gridState = rememberComicPageGridState(pageState.page, Triple(targetType, targetValue, period))

    LaunchedEffect(targetType, targetValue, period) {
        comicViewModel.setComicRankingFilter(
            tagName = tagName,
            category = category,
            period = period,
        )
    }

    fun selectPeriod(nextPeriod: ComicRankPeriod) {
        if (nextPeriod == period) return
        comicViewModel.setComicRankingFilter(
            tagName = tagName,
            category = category,
            period = nextPeriod,
        )
        val currentDestinationId = mainNavController.currentDestination?.id
        mainNavController.navigate(
            "comicRanking/$targetType/${android.net.Uri.encode(targetValue)}?period=${nextPeriod.routeValue}"
        ) {
            if (currentDestinationId != null) {
                popUpTo(currentDestinationId) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    val isLoading = pagingItems.loadState.refresh is LoadState.Loading
    val refreshError = pagingItems.loadState.refresh as? LoadState.Error

    CommonScaffold(
        title = "$title 排行",
        onNavigateBack = { mainNavController.popBackStack() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (category != null) "JM 分类 · $title" else "JM 标签 · $title",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "只显示当前${if (category != null) "分类" else "标签"}范围内的阅读热度排行",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            mainNavController.navigate(
                                "comicSearchResult/${android.net.Uri.encode(title)}"
                            )
                        }
                    ) {
                        Text("普通搜索")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComicRankPeriod.entries.forEach { option ->
                    FilterChip(
                        selected = period == option,
                        onClick = { selectPeriod(option) },
                        label = { Text(option.label) },
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (isLoading && pagingItems.itemCount == 0) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(10.dp),
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
                ) {
                    repeat(12) {
                        ComicSkeleton(modifier = Modifier.weight(1f))
                    }
                }
            } else if (refreshError != null && pagingItems.itemCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = refreshError.error.message ?: "排行加载失败",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { pagingItems.retry() }) {
                            Text("重试")
                        }
                    }
                }
            } else {
                PullRefreshAndLoadMoreGrid(
                    modifier = Modifier.weight(1f),
                    gridState = gridState,
                    lazyPagingItems = pagingItems,
                    key = { it.id },
                    columns = adaptiveComicGridCells(localSetting.searchGridColumns),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    pageState = pageState,
                    onPageChange = comicViewModel.rankingPagination::selectPage,
                    onRefresh = comicViewModel.rankingPagination::refresh,
                ) {
                    Comic(it)
                }
            }
            if (pageState.enabled && pagingItems.itemCount == 0 && (isLoading || refreshError != null)) {
                ComicPaginationBar(
                    state = pageState,
                    onPageChange = comicViewModel.rankingPagination::selectPage,
                )
            }
        }
    }
}

private const val CATEGORY_TARGET = "category"
