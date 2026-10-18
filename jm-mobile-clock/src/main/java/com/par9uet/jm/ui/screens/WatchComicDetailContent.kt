package com.par9uet.jm.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.data.models.ComicChapter
import com.par9uet.jm.ui.components.*

internal data class WatchComicInfoGroup(val title: String, val values: List<String>, val contentTags: Boolean = false)

internal fun watchComicInfoGroups(comic: Comic): List<WatchComicInfoGroup> = listOf(
    WatchComicInfoGroup("作者", comic.authorList),
    WatchComicInfoGroup("标签", comic.tagList, contentTags = true),
    WatchComicInfoGroup("角色", comic.roleList),
    WatchComicInfoGroup("作品", comic.workList),
    WatchComicInfoGroup("分类", comic.typeList),
).map { it.copy(values = it.values.map(String::trim).filter(String::isNotBlank).distinct()) }

internal fun watchDetailChapters(comic: Comic): List<ComicChapter> =
    comic.comicChapterList.ifEmpty { listOf(ComicChapter(comic.id, comic.name)) }

/** Same production content for the watch and previews: no phone-sized buttons before metadata. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun WatchComicDetailContent(
    comic: Comic,
    lastReadChapter: Int?,
    readChapterIds: Set<Int>,
    onRead: (Int) -> Unit,
    onChapters: () -> Unit,
    onSearch: (String) -> Unit,
    onContentTag: (String) -> Unit,
    onCopy: (String) -> Unit,
    onLike: () -> Unit,
    onCollect: () -> Unit,
    onComments: () -> Unit,
    onRelated: () -> Unit,
    likeBusy: Boolean = false,
    collectBusy: Boolean = false,
    error: String? = null,
    state: LazyListState = rememberLazyListState(),
    cover: @Composable (Modifier) -> Unit,
    onCache: (() -> Unit)? = null,
) {
    val chapters = watchDetailChapters(comic)
    WatchLazyColumn(
        modifier = Modifier.fillMaxSize().testTag("watch-comic-detail"),
        state = state,
        edgeScalingEnabled = false,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "hero") {
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        cover(Modifier.width(54.dp).height(72.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("JM${comic.id}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.combinedClickable(onClick = { onCopy("JM${comic.id}") }, onLongClick = { onCopy("JM${comic.id}") }))
                            Text("${chapters.size} 话", style = MaterialTheme.typography.labelSmall)
                            if (comic.isCollect) Text("已收藏", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (comic.isLike) Text("已喜欢", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (comic.price > 0 || comic.isBuy) Text(
                                if (comic.isBuy) "已购买" else "价格 ${comic.price}", style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    // Full title and description are never truncated on the detail page.
                    Text(comic.name.ifBlank { "JM${comic.id}" }, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { onCopy(comic.name) }))
                }
            }
        }
        item(key = "read") {
            WatchActionPair(if (lastReadChapter != null) "继续阅读" else "开始阅读", "章节",
                onFirst = { onRead(lastReadChapter ?: chapters.first().id) }, onSecond = onChapters)
        }
        if (onCache != null) item(key = "cache") { WatchActionRow("缓存到本机", "选择章节 · 离线阅读", onClick = onCache) }
        item(key = "counts") {
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("喜欢" to comic.likeCount, "浏览" to comic.readCount, "评论" to comic.commentCount).forEach { (label, count) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(count.toString(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        items(watchComicInfoGroups(comic).filter { it.title != "分类" || it.values.isNotEmpty() }, key = { "group-${it.title}" }) { group ->
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(group.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (group.values.isEmpty()) Text("暂无${group.title}信息", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        group.values.forEach { value ->
                            Surface(
                                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.heightIn(min = 40.dp).combinedClickable(
                                    onClick = { if (group.contentTags) onContentTag(value) else onSearch(value) },
                                    onLongClick = { onCopy(value) },
                                ),
                            ) {
                                Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(value, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "description") {
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("简介", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(comic.description.ifBlank { "暂无简介" }, style = MaterialTheme.typography.bodySmall)
                    if (comic.seriesId.isNotBlank()) Text("系列 ${comic.seriesId}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item(key = "actions") {
            WatchActionPair(if (comic.isLike) "已喜欢" else "喜欢", if (comic.isCollect) "取消收藏" else "收藏",
                firstEnabled = !comic.isLike && !likeBusy, secondEnabled = !collectBusy, onFirst = onLike, onSecond = onCollect)
        }
        if (!error.isNullOrBlank()) item(key = "action-error") { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        item(key = "links") { WatchActionPair("评论 ${comic.commentCount}", "相关推荐", onFirst = onComments, onSecond = onRelated) }
        item(key = "directory") { Text("章节目录 · ${chapters.size} 话", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        items(chapters.take(4), key = { "chapter-${it.id}" }) { chapter ->
            WatchActionRow(chapter.name.ifBlank { "JM${chapter.id}" },
                when { chapter.id == lastReadChapter -> "上次阅读 · 继续"; chapter.id in readChapterIds -> "已读"; else -> "点击阅读" },
                onClick = { onRead(chapter.id) })
        }
        if (chapters.size > 4) item(key = "all-chapters") { WatchActionRow("全部 ${chapters.size} 话", onClick = onChapters) }
        item(key = "read-again") { Button(onClick = { onRead(lastReadChapter ?: chapters.first().id) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (lastReadChapter == null) "开始阅读" else "继续阅读") } }
    }
}
