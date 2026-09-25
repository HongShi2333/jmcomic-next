package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey

@Composable
fun <T : Any> PullRefreshAndLoadMoreGrid(
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    lazyPagingItems: LazyPagingItems<T>,
    key: ((item: T) -> Any),
    columns: GridCells,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp, Alignment.Top),
    horizontalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(10.dp),
    contentPadding: PaddingValues = PaddingValues(10.dp),
    itemVisible: (item: T) -> Boolean = { true },
    enablePullRefresh: Boolean = true,
    itemContent: @Composable ((item: T) -> Unit),
) {
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    val gridContent: @Composable () -> Unit = {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            columns = columns,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding
        ) {
            items(
                lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { key(it) },
            ) { index ->
                val item = lazyPagingItems[index]
                if (item != null && itemVisible(item)) {
                    itemContent(item)
                }
            }
            when (val appendState = lazyPagingItems.loadState.append) {
                is LoadState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                is LoadState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("\u52a0\u8f7d\u5931\u8d25", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { lazyPagingItems.retry() }) {
                                Text("\u91cd\u8bd5")
                            }
                        }
                    }
                }

                is LoadState.NotLoading -> {
                    if (appendState.endOfPaginationReached) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "\u6ca1\u6709\u66f4\u591a\u6570\u636e\u4e86",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (enablePullRefresh) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                lazyPagingItems.refresh()
            },
            modifier = modifier
        ) {
            gridContent()
        }
    } else {
        Box(modifier = modifier) {
            gridContent()
        }
    }
}
