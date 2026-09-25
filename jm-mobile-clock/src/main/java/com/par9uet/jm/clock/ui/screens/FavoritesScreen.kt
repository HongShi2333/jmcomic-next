package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.par9uet.jm.clock.data.LoadState
import com.par9uet.jm.clock.data.WatchComic
import com.par9uet.jm.clock.data.WatchFavoritePage
import com.par9uet.jm.clock.data.WatchRepository
import com.par9uet.jm.clock.ui.components.WatchComicCard
import com.par9uet.jm.clock.ui.components.WatchError
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchLoading
import com.par9uet.jm.clock.ui.components.WatchScreen
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    repository: WatchRepository,
    onBack: () -> Unit,
    onComic: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var reloadKey by rememberSaveable { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<LoadState<WatchFavoritePage>>(LoadState.Loading) }
    var nextPage by rememberSaveable { mutableIntStateOf(2) }
    var loadingMore by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(reloadKey) {
        state = LoadState.Loading
        nextPage = 2
        val result = repository.favorites(page = 1)
        state = result.fold(
            onSuccess = { LoadState.Content(it) },
            onFailure = { LoadState.Error(it.message ?: "收藏加载失败") },
        )
    }

    WatchScreen(title = "我的收藏", onBack = onBack) {
        when (val current = state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") { WatchLoading("正在同步收藏…") }
            is LoadState.Error -> item(key = "error") {
                WatchError(current.message, onRetry = { reloadKey += 1 })
            }
            is LoadState.Content -> {
                val page = current.value
                item(key = "summary") {
                    WatchInfoCard(
                        title = "线上收藏",
                        body = if (page.total > 0) {
                            "已显示 ${page.comics.size} / ${page.total} 部。列表直接从账号同步，不写入图片缓存。"
                        } else {
                            "当前账号还没有收藏作品。"
                        },
                    )
                }
                if (page.comics.isEmpty()) {
                    item(key = "empty") { WatchInfoCard("暂无收藏", "在漫画详情页点按心形按钮即可加入收藏。") }
                } else {
                    items(page.comics, key = WatchComic::id) { comic ->
                        WatchComicCard(
                            comic = comic,
                            repository = repository,
                            onClick = { onComic(comic.id) },
                        )
                    }
                }
                if (page.hasMore) {
                    item(key = "load_more") {
                        Button(
                            onClick = {
                                if (loadingMore) return@Button
                                scope.launch {
                                    loadingMore = true
                                    val result = repository.favorites(nextPage)
                                    result.onSuccess { extra ->
                                        state = LoadState.Content(
                                            page.copy(
                                                comics = (page.comics + extra.comics).distinctBy(WatchComic::id),
                                                page = extra.page,
                                                total = extra.total,
                                            ),
                                        )
                                        nextPage += 1
                                    }
                                    result.exceptionOrNull()?.let {
                                        state = LoadState.Error(it.message ?: "加载更多收藏失败")
                                    }
                                    loadingMore = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loadingMore,
                        ) {
                            Text(if (loadingMore) "加载中…" else "加载更多")
                        }
                    }
                }
            }
        }
    }
}
