package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.pagingSource.ComicPageState
import com.par9uet.jm.ui.pagingSource.COMIC_DISPLAY_PAGE_SIZE
import com.par9uet.jm.ui.pagingSource.COMIC_PAGINATION_THRESHOLD
import com.par9uet.jm.ui.theme.LocalAppMotion

class ComicListPagination(
    val state: ComicPageState,
    val onPageChange: (Int) -> Unit,
) {
    val firstIndex: Int get() = (state.page - 1) * COMIC_DISPLAY_PAGE_SIZE
    val limit: Int get() = if (state.enabled) COMIC_DISPLAY_PAGE_SIZE else COMIC_PAGINATION_THRESHOLD
}

@Composable
fun rememberComicListPagination(itemCount: Int, resetKey: Any? = Unit): ComicListPagination {
    val requestedPage = rememberSaveable(resetKey) { mutableIntStateOf(1) }
    val enabled = itemCount >= COMIC_PAGINATION_THRESHOLD
    val pageCount = ((itemCount.toLong() + COMIC_DISPLAY_PAGE_SIZE - 1) / COMIC_DISPLAY_PAGE_SIZE).toInt().coerceAtLeast(1)
    val page = if (enabled) requestedPage.intValue.coerceIn(1, pageCount) else 1
    return ComicListPagination(
        state = ComicPageState(page, itemCount, enabled, page < pageCount && enabled, isLoading = false),
        onPageChange = { requestedPage.intValue = it.coerceIn(1, pageCount) },
    )
}

@Composable
fun ComicPaginationBar(
    state: ComicPageState,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalAppMotion.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onPageChange(state.page - 1) },
                enabled = !state.isLoading && state.page > 1,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Text("上一页", maxLines = 1)
            }
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AnimatedContent(
                    targetState = state.pageCount?.let { "${state.page} / $it" } ?: "第 ${state.page} 页",
                    transitionSpec = { fadeIn(tween(motion.exitMs)) togetherWith fadeOut(tween(motion.exitMs)) },
                    label = "page-number",
                ) { label ->
                    Text(label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                }
                Text(
                    text = if (state.isLoading) "加载中…" else "每页 40 本",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = { onPageChange(state.page + 1) },
                enabled = !state.isLoading && state.hasNext,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Text("下一页", maxLines = 1)
            }
        }
    }
}
