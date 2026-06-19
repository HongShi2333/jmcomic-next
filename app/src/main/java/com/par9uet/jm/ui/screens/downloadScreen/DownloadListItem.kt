package com.par9uet.jm.ui.screens.downloadScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.par9uet.jm.database.model.DownloadComic
import com.par9uet.jm.utils.shimmer
import org.koin.compose.getKoin
import java.io.File

@Composable
private fun ComicCoverImage(
    comic: DownloadComic,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = getKoin().get()
) {
    if (comic.coverPath.isNotBlank()) {
        AsyncImage(
            model = File(comic.coverPath),
            imageLoader = imageLoader,
            contentDescription = "${comic.name}的封面",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.shimmer())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadCoverGridItem(
    modifier: Modifier = Modifier,
    comic: DownloadComic,
    editing: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ComicCoverImage(
                    comic = comic,
                    modifier = Modifier
                        .aspectRatio(3f / 4f)
                        .fillMaxWidth()
                )
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = comic.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp),
                    text = comic.authorList.joinToString(",").ifBlank { "暂无作者" },
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (editing) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadRowItem(
    modifier: Modifier = Modifier,
    comic: DownloadComic,
    editing: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
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
                comic = comic,
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
                    text = comic.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = comic.authorList.joinToString(",").ifBlank { "暂无作者" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DownloadStateBlock(
                modifier = Modifier
                    .width(82.dp)
                    .fillMaxHeight(),
                comic = comic
            )
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "取消",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DownloadStateBlock(
    modifier: Modifier,
    comic: DownloadComic
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        when (comic.status) {
            "downloading" -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = comic.progress.coerceIn(0f, 1f),
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "progressAnimation"
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

            "error" -> {
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

            "paused" -> {
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
        if (comic.status == "downloading") {
            LinearProgressIndicator(
                progress = { comic.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
