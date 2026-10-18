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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.data.models.ComicRankCategory
import com.par9uet.jm.data.models.ComicRankPeriod
import com.par9uet.jm.ui.components.Comic
import com.par9uet.jm.ui.components.ComicSkeleton
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.components.PullRefreshAndLoadMoreGrid
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
    val gridState = rememberLazyGridState()

    LaunchedEffect(targetType, targetValue, period) {
        comicViewModel.setComicRankingFilter(
            tagName = tagName,
            category = category,
            period = period,
        )
    }
    LaunchedEffect(targetType, targetValue, period) {
        gridState.scrollToItem(0)
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

    var periodOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    CommonScaffold(title = "热度排行", onNavigateBack = { mainNavController.popBackStack() }) {
        PullRefreshAndLoadMoreGrid(Modifier.fillMaxSize(), paginationEnabled = true, paginationKey = listOf(targetType, targetValue, period), gridState = gridState, lazyPagingItems = pagingItems,
            key = { it.id }, columns = adaptiveComicGridCells(), headerContent = {
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { periodOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("周期 · ${period.label}") }
                    TextButton(onClick = { mainNavController.navigate("comicSearchResult/${android.net.Uri.encode(title)}") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("普通搜索") }
                }
            }) { Comic(it) }
    }
    if (periodOpen) com.par9uet.jm.ui.components.SelectDialog("排行周期", period.routeValue,
        ComicRankPeriod.entries.map { com.par9uet.jm.ui.components.SelectOption(it.label, it.routeValue) },
        onSelect = { periodOpen = false; selectPeriod(ComicRankPeriod.fromRouteValue(it)) }, onDismissRequest = { periodOpen = false })
}

private const val CATEGORY_TARGET = "category"
