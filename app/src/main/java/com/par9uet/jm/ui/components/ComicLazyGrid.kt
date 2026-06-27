package com.par9uet.jm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.utils.filterBlockedTags
import org.koin.compose.getKoin

@Composable
fun ComicLazyGrid(
    list: List<Comic>,
    isRefreshing: Boolean,
    isMoreLoading: Boolean,
    hasMore: Boolean,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
    columns: GridCells = adaptiveComicGridCells(),
    verticalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(10.dp),
    horizontalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(10.dp),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    stickyHeaderContent: @Composable (() -> Unit)? = null,
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val visibleList = remember(list, localSetting.blockedTagList) {
        list.filterBlockedTags(localSetting.blockedTagList)
    }
    val shouldLoadMore =
        remember(
            gridState.layoutInfo.visibleItemsInfo,
            gridState.layoutInfo.totalItemsCount,
            isRefreshing,
            hasMore
        ) {
            derivedStateOf {
                val layoutInfo = gridState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem?.index == layoutInfo.totalItemsCount - 1 &&
                        !isRefreshing &&
                        hasMore
            }
        }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = columns,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding,
        ) {
            if (stickyHeaderContent !== null) {
                stickyHeader {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        stickyHeaderContent()
                    }
                }
            }
            items(
                items = visibleList,
                key = { it.id },
            ) { item ->
                Comic(item)
            }
            if (visibleList.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadMore(
                        isLoading = isMoreLoading,
                        hasMore = hasMore
                    )
                }
            }
        }
    }
    if (isRefreshing && list.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 禁止点击，让点击穿透
                .pointerInput(Unit) { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
