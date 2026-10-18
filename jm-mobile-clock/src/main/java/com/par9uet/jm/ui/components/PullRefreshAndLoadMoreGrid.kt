package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import com.par9uet.jm.ui.components.WatchLazyVerticalGrid as LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch

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
    headerContent: @Composable (() -> Unit)? = null,
    refreshAction: (() -> Unit)? = null,
    paginationEnabled: Boolean = false,
    totalItemsHint: Int = 0,
    paginationKey: Any? = null,
    itemContent: @Composable ((item: T) -> Unit),
) {
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    val count = lazyPagingItems.itemCount
    val append = lazyPagingItems.loadState.append
    val ended = append is LoadState.NotLoading && append.endOfPaginationReached
    var page by rememberSaveable(paginationKey) { mutableIntStateOf(0) }
    var latched by rememberSaveable(paginationKey) { mutableStateOf(false) }
    var pendingPage by remember(paginationKey) { mutableIntStateOf(-1) }
    val paginated = paginationEnabled && (latched || count > WATCH_PAGINATION_THRESHOLD || totalItemsHint > WATCH_PAGINATION_THRESHOLD)
    val window = watchPageWindow(count, page, paginated)
    val scope = rememberCoroutineScope()
    LaunchedEffect(isRefreshing, paginationKey) {
        if (isRefreshing) { page = 0; pendingPage = -1; latched = false; gridState.scrollToItem(0) }
    }
    LaunchedEffect(paginated) {
        if (paginated && !latched) {
            latched = true
            page = (gridState.firstVisibleItemIndex / WATCH_COMICS_PER_PAGE).coerceAtMost(window.pageCount - 1)
            pendingPage = page // Only complete the current forty-item window.
        }
    }
    LaunchedEffect(paginated, count, ended, isRefreshing) {
        if (paginated && !isRefreshing && !ended && count > 0 && pendingPage < 0 &&
            count < (window.page + 1) * WATCH_COMICS_PER_PAGE) {
            pendingPage = window.page
        }
    }
    LaunchedEffect(pendingPage, count, append, isRefreshing) {
        if (pendingPage < 0 || isRefreshing) return@LaunchedEffect
        val desiredEnd = (pendingPage + 1) * WATCH_COMICS_PER_PAGE
        if (count >= desiredEnd || ended) {
            page = watchPageWindow(count, pendingPage, true).page
            pendingPage = -1
            gridState.scrollToItem(0)
        } else if (append is LoadState.NotLoading && count > 0) {
            // Explicit demand is the only append trigger once paginated.
            lazyPagingItems[count - 1]
        }
    }
    val changePage: (Int) -> Unit = { requested ->
        if ((requested + 1) * WATCH_COMICS_PER_PAGE <= count || ended) {
            pendingPage = -1
            page = watchPageWindow(count, requested, true).page
            scope.launch { gridState.scrollToItem(0) }
        } else pendingPage = requested
    }
    val pager: @Composable () -> Unit = {
        WatchPagination(window, hasMore = !ended, loading = pendingPage >= 0 && append !is LoadState.Error,
            onPrevious = { changePage(window.page - 1) }, onNext = { changePage(window.page + 1) })
    }
    val gridContent: @Composable () -> Unit = {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            columns = columns,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding
        ) {
            if (headerContent != null) item { headerContent() }
            if (paginated && count > 0) item(key = "pagination-top") { pager() }
            if (lazyPagingItems.itemCount == 0) item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (val refresh = lazyPagingItems.loadState.refresh) {
                        is LoadState.Loading -> {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text("加载中", style = MaterialTheme.typography.bodySmall)
                        }
                        is LoadState.Error -> {
                            Text(refresh.error.message ?: "加载失败", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { lazyPagingItems.retry() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("重试") }
                        }
                        is LoadState.NotLoading -> {
                            Text("暂无内容", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { refreshAction?.invoke() ?: lazyPagingItems.refresh() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("刷新") }
                        }
                    }
                }
            }
            items(
                window.end - window.start,
                key = { index -> lazyPagingItems.peek(window.start + index)?.let(key) ?: "placeholder-${window.start + index}" },
            ) { localIndex ->
                val index = window.start + localIndex
                // peek prevents the last visible item from prefetching another page.
                val item = if (paginated) lazyPagingItems.peek(index) else lazyPagingItems[index]
                if (item != null && itemVisible(item)) {
                    itemContent(item)
                }
            }
            if (paginated && count > 0) item(key = "pagination-bottom") { pager() }
            when (val appendState = lazyPagingItems.loadState.append) {
                is LoadState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(8.dp),
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
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("\u52a0\u8f7d\u5931\u8d25", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { lazyPagingItems.retry() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("\u91cd\u8bd5")
                            }
                        }
                    }
                }

                is LoadState.NotLoading -> {
                    if (appendState.endOfPaginationReached && !paginated) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
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
                refreshAction?.invoke() ?: lazyPagingItems.refresh()
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
