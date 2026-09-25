package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.clock.data.LoadState
import com.par9uet.jm.clock.data.WatchComicDetail
import com.par9uet.jm.clock.data.WatchRepository
import com.par9uet.jm.clock.ui.components.WatchCover
import com.par9uet.jm.clock.ui.components.WatchError
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchHeaderButton
import com.par9uet.jm.clock.ui.components.WatchLoading
import com.par9uet.jm.clock.ui.components.WatchScreen
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    comicId: Int,
    repository: WatchRepository,
    isLoggedIn: Boolean,
    onLoginRequired: () -> Unit,
    onBack: () -> Unit,
    onRead: (Int) -> Unit,
) {
    var reloadKey by rememberSaveable { mutableIntStateOf(0) }
    var state by remember(comicId) {
        mutableStateOf<LoadState<WatchComicDetail>>(LoadState.Loading)
    }
    val scope = rememberCoroutineScope()
    var favoriteBusy by remember(comicId) { mutableStateOf(false) }
    var favoriteError by remember(comicId) { mutableStateOf<String?>(null) }

    LaunchedEffect(comicId, reloadKey) {
        state = LoadState.Loading
        val result = repository.detail(comicId)
        state = result.fold(
            onSuccess = { LoadState.Content(it) },
            onFailure = { LoadState.Error(it.message ?: "详情加载失败") },
        )
    }

    WatchScreen(
        title = "漫画详情",
        onBack = onBack,
        actions = {
            val isFavorite = (state as? LoadState.Content)?.value?.isFavorite == true
            WatchHeaderButton(
                onClick = {
                    if (!isLoggedIn) {
                        onLoginRequired()
                        return@WatchHeaderButton
                    }
                    val detail = (state as? LoadState.Content)?.value ?: return@WatchHeaderButton
                    if (favoriteBusy) return@WatchHeaderButton
                    scope.launch {
                        favoriteBusy = true
                        favoriteError = null
                        repository.toggleFavorite(detail.id).fold(
                            onSuccess = {
                                state = LoadState.Content(detail.copy(isFavorite = !detail.isFavorite))
                            },
                            onFailure = { failure ->
                                favoriteError = failure.message ?: "收藏操作失败"
                            },
                        )
                        favoriteBusy = false
                    }
                },
                enabled = !favoriteBusy,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                )
            }
        },
    ) {
        when (val current = state) {
            LoadState.Idle, LoadState.Loading -> item(key = "loading") { WatchLoading("正在获取详情…") }
            is LoadState.Error -> item(key = "error") {
                WatchError(current.message, onRetry = { reloadKey += 1 })
            }
            is LoadState.Content -> {
                val detail = current.value
                favoriteError?.let { message ->
                    item(key = "favorite_error") { WatchError(message) }
                }
                item(key = "cover") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        WatchCover(
                            comic = detail.asComic(),
                            repository = repository,
                            modifier = Modifier
                                .width(82.dp)
                                .height(110.dp),
                        )
                    }
                }
                item(key = "title") {
                    Text(
                        text = detail.title,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (detail.authors.isNotEmpty()) {
                    item(key = "authors") {
                        Text(
                            text = detail.authors.joinToString("、"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (detail.tags.isNotEmpty()) {
                    item(key = "tags") {
                        WatchInfoCard(
                            title = "标签",
                            body = detail.tags.take(8).joinToString(" · "),
                        )
                    }
                }
                if (detail.description.isNotBlank()) {
                    item(key = "description") {
                        WatchInfoCard(title = "简介", body = detail.description)
                    }
                }
                item(key = "chapter_title") {
                    Text("在线章节", style = MaterialTheme.typography.titleSmall)
                }
                if (detail.chapters.isEmpty()) {
                    item(key = "no_chapter") {
                        WatchError("当前作品没有可读取章节")
                    }
                } else {
                    detail.chapters.forEachIndexed { index, chapter ->
                        item(key = "chapter_${chapter.id}") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRead(chapter.id) },
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null)
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        text = "第 ${index + 1} 章 · 仅在线",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
