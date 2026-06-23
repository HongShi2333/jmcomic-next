package com.par9uet.jm.ui.screens.downloadScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.ui.viewModel.DownloadComicGroup
import com.par9uet.jm.utils.shimmer
import org.koin.compose.getKoin
import java.io.File

// Removed MAX_EXPANDED_CHAPTER_ROWS limit to show all chapters

@Composable
private fun ComicCoverImage(
    comic: DownloadComic,
    contentDescriptionName: String,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = getKoin().get()
) {
    if (comic.coverPath.isNotBlank()) {
        AsyncImage(
            model = File(comic.coverPath),
            imageLoader = imageLoader,
            contentDescription = "${contentDescriptionName}的封面",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.shimmer())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadGroupRowItem(
    modifier: Modifier = Modifier,
    group: DownloadComicGroup,
    expanded: Boolean,
    editing: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onExpandClick: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onChapterClick: ((DownloadComic) -> Unit)? = null
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editing) {
                    Checkbox(checked = selected, onCheckedChange = null)
                }
                ComicCoverImage(
                    comic = group.coverComic,
                    contentDescriptionName = group.displayName,
                    modifier = Modifier
                        .width(64.dp)
                        .aspectRatio(3f / 4f)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = group.displayName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = group.authorList.joinToString(",").ifBlank { "暂无作者" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = groupSummary(group),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DownloadGroupStateBlock(
                    modifier = Modifier
                        .width(82.dp)
                        .fillMaxHeight(),
                    group = group
                )
                if (group.chapterSize > 1) {
                    IconButton(onClick = onExpandClick) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "折叠章节" else "展开章节",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onCancel != null) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "移除任务",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (expanded && group.chapterSize > 1) {
                HorizontalDivider()
                group.sortedChapters.forEach { chapter ->
                    DownloadChapterRow(
                        chapter = chapter,
                        onClick = onChapterClick?.let { click ->
                            { click(chapter) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadChapterRow(
    chapter: DownloadComic,
    onClick: (() -> Unit)?
) {
    val rowModifier = if (onClick == null) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapterTitle(chapter),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (chapter.status == "downloading") {
                LinearProgressIndicator(
                    progress = { chapter.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
        }
        Text(
            text = chapterStatusText(chapter),
            style = MaterialTheme.typography.labelMedium,
            color = if (chapter.status == "error") {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun DownloadGroupStateBlock(
    modifier: Modifier,
    group: DownloadComicGroup
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        when {
            group.downloadingCount > 0 -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = group.progress.coerceIn(0f, 1f),
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "groupProgressAnimation"
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            group.pendingCount > 0 -> {
                Icon(
                    imageVector = Icons.Rounded.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "等待中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            group.pausedCount > 0 -> {
                Icon(
                    imageVector = Icons.Rounded.PauseCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "已暂停",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            group.errorCount > 0 -> {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "出错",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            group.completeCount == group.chapterSize -> {
                Icon(
                    imageVector = Icons.Rounded.CheckCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "已完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.Rounded.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "等待中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (group.downloadingCount > 0) {
            LinearProgressIndicator(
                progress = { group.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun groupSummary(group: DownloadComicGroup): String {
    val chapterText = if (group.totalChapters > group.chapterSize) {
        "${group.chapterSize}/${group.totalChapters} 话"
    } else {
        "${group.chapterSize} 话"
    }
    val statusParts = buildList {
        if (group.completeCount > 0) add("完成 ${group.completeCount}")
        if (group.downloadingCount > 0) add("下载中 ${group.downloadingCount}")
        if (group.pendingCount > 0) add("等待 ${group.pendingCount}")
        if (group.pausedCount > 0) add("暂停 ${group.pausedCount}")
        if (group.errorCount > 0) add("出错 ${group.errorCount}")
    }
    return (listOf(chapterText) + statusParts).joinToString(" · ")
}

private fun chapterTitle(chapter: DownloadComic): String {
    val parsedTitle = chapter.name.chapterTitleFromName()
    val numberText = when {
        chapter.chapterCount > 1 || chapter.chapterIndex > 0 -> "第${chapter.chapterIndex + 1}话"
        parsedTitle != null -> parsedTitle
        else -> "单话"
    }
    return if (chapter.chapterName.isNotBlank() && chapter.chapterName != numberText) {
        "$numberText · ${chapter.chapterName}"
    } else {
        numberText
    }
}

private fun String.chapterTitleFromName(): String? {
    return Regex("第\\s*\\d+\\s*[话話回集]")
        .find(this)
        ?.value
        ?.replace(Regex("\\s+"), "")
}

private fun chapterStatusText(chapter: DownloadComic): String {
    return when (chapter.status) {
        "complete" -> "已完成"
        "downloading" -> "${(chapter.progress.coerceIn(0f, 1f) * 100).toInt()}%"
        "pending" -> "等待中"
        "paused" -> "已暂停"
        "error" -> "出错"
        else -> chapter.status
    }
}
