package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.ComicCoverImage
import com.par9uet.jm.ui.components.WatchLazyColumn
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import com.par9uet.jm.ui.viewModel.ComicViewModel
import com.par9uet.jm.utils.filterBlockedTags
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun WatchHomeScreen(
    selectedIndex: Int = 0,
    active: Boolean = true,
    header: @Composable (() -> Unit)? = null,
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val homeState by comicViewModel.homeComicState.collectAsState()
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val selectedPage = homeState.list.getOrNull(
        selectedIndex.coerceIn(0, homeState.list.lastIndex.coerceAtLeast(0)),
    )
    val blockedTags = remember(localSetting.blockedTagList, localSetting.homeExcludedTags) {
        (localSetting.blockedTagList + localSetting.homeExcludedTags).distinct()
    }
    val comics = remember(selectedPage, blockedTags) {
        selectedPage?.list.orEmpty().filterBlockedTags(blockedTags).distinctBy { it.id }
    }

    LaunchedEffect(localSetting.comicApiSource, localSetting.preferenceRecommendEnabled) {
        comicViewModel.getHomeComic()
    }

    LaunchedEffect(selectedPage?.id, active) { if (active) selectedPage?.id?.let(comicViewModel::selectHomeCategory) }

    WatchHomeList(
        comics = comics,
        selectedTitle = selectedPage?.title ?: "发现漫画",
        isLoading = homeState.isLoading,
        errorMessage = homeState.errorMsg?.takeIf { homeState.isError },
        emptyMessage = if (blockedTags.isEmpty()) "暂无漫画" else "当前结果被标签过滤",
        showComicId = localSetting.showComicIdAsSubtitle,
        active = active,
        header = header,
        onRefresh = comicViewModel::getHomeComic,
        canLoadMore = homeState.canLoadMore,
        isLoadingMore = homeState.isLoadingMore,
        moreError = homeState.moreError,
        onLoadMore = comicViewModel::loadMoreHomeComic,
        onComicClick = { comic ->
            comicDetailViewModel.reset(comic.id)
            mainNavController.navigate("comicDetail/${comic.id}")
        },
    )
}

/** Same layout in the application and previews; no phone header or cover grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchHomeList(
    comics: List<Comic>,
    selectedTitle: String,
    isLoading: Boolean,
    errorMessage: String? = null,
    emptyMessage: String = "暂无漫画",
    showComicId: Boolean = false,
    active: Boolean = true,
    header: @Composable (() -> Unit)? = null,
    onRefresh: () -> Unit,
    onComicClick: (Comic) -> Unit,
    canLoadMore: Boolean = false,
    isLoadingMore: Boolean = false,
    moreError: String? = null,
    onLoadMore: () -> Unit = {},
    coverContent: @Composable (Comic, Modifier) -> Unit = { comic, modifier ->
        ComicCoverImage(comic = comic, modifier = modifier)
    },
) {
    val listState = rememberLazyListState()
    val pages = com.par9uet.jm.ui.components.rememberWatchListPages(
        comics.size, key = selectedTitle, hasMore = canLoadMore, loading = isLoadingMore, onLoadMore = onLoadMore,
    )
    val window = pages.window
    LaunchedEffect(window.page) { listState.scrollToItem(0) }
    LaunchedEffect(selectedTitle) { listState.scrollToItem(0) }

    PullToRefreshBox(
        isRefreshing = isLoading && comics.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        WatchLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            rotaryEnabled = active,
            edgeScalingEnabled = false,
        ) {
            if (header != null) {
                item(key = "actions") { header() }
            }
            item(key = "category") {
                Text(
                    text = selectedTitle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (pages.enabled) item(key = "pagination-top") {
                com.par9uet.jm.ui.components.WatchPagination(window, canLoadMore, pages.waiting, pages.previous, pages.next)
            }
            if (isLoading && comics.isEmpty()) {
                item(key = "loading") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("正在加载…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                if (!errorMessage.isNullOrBlank() || comics.isEmpty()) {
                    item(key = "status") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage?.takeIf { it.isNotBlank() } ?: emptyMessage,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(
                                onClick = onRefresh,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text("重新加载") }
                        }
                    }
                }
                items(comics.subList(window.start, window.end), key = { it.id }) { comic ->
                    WatchComicRow(comic, showComicId, { onComicClick(comic) }, coverContent)
                }
                if (pages.enabled) item(key = "pagination-bottom") {
                    com.par9uet.jm.ui.components.WatchPagination(window, canLoadMore, pages.waiting, pages.previous, pages.next)
                }
                if ((!pages.enabled && (canLoadMore || isLoadingMore)) || moreError != null) item(key = "more") {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        moreError?.let { Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
                        TextButton(onClick = onLoadMore, enabled = !isLoadingMore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (isLoadingMore) "加载中…" else if (moreError != null) "重试下一页" else "加载更多")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchComicRow(
    comic: Comic,
    showComicId: Boolean,
    onClick: () -> Unit,
    coverContent: @Composable (Comic, Modifier) -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            coverContent(comic, Modifier.width(54.dp).height(72.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = comic.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (showComicId) {
                        "JM${comic.id}"
                    } else {
                        comic.authorList.joinToString("、").ifBlank { "JM${comic.id}" }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
