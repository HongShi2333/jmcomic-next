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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.pagingSource.ComicPageState
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.par9uet.jm.ui.theme.revealComicPage

/** Persist scroll across navigation; reset only when the query or display page changes. */
@Composable
fun rememberComicPageGridState(page: Int, queryKey: Any = Unit): LazyGridState {
    val state = rememberSaveable(queryKey, saver = LazyGridState.Saver) { LazyGridState() }
    val previousPage = rememberSaveable(queryKey) { mutableIntStateOf(page) }
    LaunchedEffect(page) {
        if (previousPage.intValue != page) {
            state.scrollToItem(0)
            previousPage.intValue = page
        }
    }
    return state
}

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
    pageState: ComicPageState? = null,
    onPageChange: (Int) -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    itemContent: @Composable ((item: T) -> Unit),
) {
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    val previousPage = remember(gridState) { mutableIntStateOf(pageState?.page ?: 1) }
    LaunchedEffect(pageState?.page, pageState?.isLoading) {
        val page = pageState?.page ?: return@LaunchedEffect
        if (page != previousPage.intValue && !pageState.isLoading) {
            gridState.scrollToItem(0)
            previousPage.intValue = page
        }
    }
    val itemsContent: @Composable () -> Unit = {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            columns = columns,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding
        ) {
            if (lazyPagingItems.itemCount == 0 && !isRefreshing) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val error = lazyPagingItems.loadState.refresh as? LoadState.Error
                        Text(
                            text = error?.error?.message ?: "当前条件没有漫画",
                            color = if (error == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        )
                        if (error != null) {
                            Button(onClick = { lazyPagingItems.retry() }) { Text("重试") }
                        }
                    }
                }
            }
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
                    if (appendState.endOfPaginationReached && pageState == null && lazyPagingItems.itemCount > 0) {
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
    val gridContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxSize()) {
            val firstKey = if (lazyPagingItems.itemCount > 0) lazyPagingItems.peek(0)?.let(key) else null
            Box(modifier = Modifier.weight(1f).revealComicPage(
                pageState?.page to firstKey, ready = firstKey != null && !isRefreshing,
            )) { itemsContent() }
            if (pageState?.enabled == true) {
                ComicPaginationBar(state = pageState, onPageChange = onPageChange)
            }
        }
    }
    if (enablePullRefresh) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (onRefresh != null) onRefresh() else lazyPagingItems.refresh()
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
