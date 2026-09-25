package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.par9uet.jm.clock.data.LoadState
import com.par9uet.jm.clock.data.WatchComic
import com.par9uet.jm.clock.data.WatchAccount
import com.par9uet.jm.clock.data.WatchRepository
import com.par9uet.jm.clock.ui.components.WatchComicCard
import com.par9uet.jm.clock.ui.components.WatchError
import com.par9uet.jm.clock.ui.components.WatchHeaderButton
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchActionRow
import com.par9uet.jm.clock.ui.components.WatchLoading
import com.par9uet.jm.clock.ui.components.WatchScreen
import androidx.compose.material3.Icon

@Composable
fun HomeScreen(
    repository: WatchRepository,
    account: WatchAccount?,
    onSearch: () -> Unit,
    onAccount: () -> Unit,
    onComic: (Int) -> Unit,
) {
    var reloadKey by rememberSaveable { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<LoadState<List<WatchComic>>>(LoadState.Loading) }

    LaunchedEffect(reloadKey) {
        state = LoadState.Loading
        val result = repository.latest()
        state = result.fold(
            onSuccess = { LoadState.Content(it) },
            onFailure = { LoadState.Error(it.message ?: "首页加载失败") },
        )
    }

    WatchScreen(
        title = "JM 手表",
        actions = {
            WatchHeaderButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
            WatchHeaderButton(onClick = onAccount) {
                Icon(Icons.Default.Person, contentDescription = "我的")
            }
        },
    ) {
        item(key = "account") {
            WatchActionRow(
                title = account?.username ?: "登录账号",
                subtitle = account?.let { "收藏 ${it.favoriteCount} 部 · 点此管理账号" }
                    ?: "登录后同步收藏、管理详情页收藏状态",
                icon = Icons.Default.Person,
                onClick = onAccount,
            )
        }
        item(key = "online_tip") {
            WatchInfoCard(
                title = "在线发现",
                body = "与主项目共享在线发现和阅读能力；图片只在当前页面内存中保留，离开即释放。",
            )
        }
        when (val current = state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") { WatchLoading("正在获取最新作品…") }
            is LoadState.Error -> item(key = "error") {
                WatchError(current.message, onRetry = { reloadKey += 1 })
            }
            is LoadState.Content -> items(current.value, key = WatchComic::id) { comic ->
                WatchComicCard(
                    comic = comic,
                    repository = repository,
                    onClick = { onComic(comic.id) },
                )
            }
        }
    }
}
