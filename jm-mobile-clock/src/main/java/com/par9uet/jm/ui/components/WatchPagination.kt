package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal const val WATCH_COMICS_PER_PAGE = 40
internal const val WATCH_PAGINATION_THRESHOLD = 50

internal data class WatchPageWindow(val page: Int, val pageCount: Int, val start: Int, val end: Int)

internal fun watchPageWindow(count: Int, requestedPage: Int, paginated: Boolean): WatchPageWindow {
    val safeCount = count.coerceAtLeast(0)
    val pages = if (paginated) ((safeCount + WATCH_COMICS_PER_PAGE - 1) / WATCH_COMICS_PER_PAGE).coerceAtLeast(1) else 1
    val page = requestedPage.coerceIn(0, pages - 1)
    val start = if (paginated) page * WATCH_COMICS_PER_PAGE else 0
    return WatchPageWindow(page, pages, start, if (paginated) minOf(start + WATCH_COMICS_PER_PAGE, safeCount) else safeCount)
}

internal class WatchListPages(
    val enabled: Boolean,
    val window: WatchPageWindow,
    val waiting: Boolean,
    val previous: () -> Unit,
    val next: () -> Unit,
)

@Composable
internal fun rememberWatchListPages(
    count: Int,
    key: Any? = null,
    hasMore: Boolean = false,
    loading: Boolean = false,
    onLoadMore: () -> Unit = {},
): WatchListPages {
    var page by rememberSaveable(key) { mutableIntStateOf(0) }
    var latched by rememberSaveable(key) { mutableStateOf(false) }
    var pending by remember(key) { mutableIntStateOf(-1) }
    var requestedAt by remember(key) { mutableIntStateOf(-1) }
    val enabled = latched || count > WATCH_PAGINATION_THRESHOLD
    val window = watchPageWindow(count, page, enabled)
    LaunchedEffect(enabled) { if (enabled) latched = true }
    LaunchedEffect(pending, count, loading, hasMore) {
        if (pending < 0 || loading) return@LaunchedEffect
        if (count >= (pending + 1) * WATCH_COMICS_PER_PAGE || !hasMore || requestedAt == count) {
            page = watchPageWindow(count, pending, true).page
            pending = -1
            requestedAt = -1
        } else {
            requestedAt = count
            onLoadMore()
        }
    }
    return WatchListPages(enabled, window, pending >= 0,
        previous = { page = (window.page - 1).coerceAtLeast(0) },
        next = {
            if ((window.page + 2) * WATCH_COMICS_PER_PAGE <= count || !hasMore) {
                page = watchPageWindow(count, window.page + 1, true).page
            } else { requestedAt = -1; pending = window.page + 1 }
        },
    )
}

/** Scrolls with the list; the dial bottom is left free of a phone-sized navigation bar. */
@Composable
internal fun WatchPagination(
    window: WatchPageWindow,
    hasMore: Boolean = false,
    loading: Boolean = false,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(Modifier.padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = window.page > 0 && !loading, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "上一页", modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${window.page + 1} / ${window.pageCount}${if (hasMore) "+" else ""}",
                    style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = "第${window.page + 1}页" })
                if (loading) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.dp)
                else Text("${window.end - window.start} 部", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onNext, enabled = !loading && (window.page + 1 < window.pageCount || hasMore),
                modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "下一页", modifier = Modifier.size(18.dp))
            }
        }
    }
}
