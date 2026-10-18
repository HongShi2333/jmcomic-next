package com.par9uet.jm.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun Modifier.watchVerticalScroll(state: ScrollState, enabled: Boolean = true, flingBehavior: FlingBehavior? = null, reverseScrolling: Boolean = false): Modifier {
    val requester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val header = LocalWatchHeaderState.current
    val canFocus = enabled && LocalWatchListActive.current && !androidx.compose.ui.platform.LocalInspectionMode.current && WindowInsets.ime.getBottom(LocalDensity.current) == 0
    LaunchedEffect(canFocus) {
        if (canFocus) {
            // A screen can be replaced while the first frame is attaching the
            // focus target. Never let that transient state crash app startup.
            runCatching { requester.requestFocus() }
        }
    }
    return this.onRotaryScrollEvent {
        if (canFocus) { scope.launch { scrollWatchContent(it.verticalScrollPixels, header) { delta -> state.scrollBy(delta) } }; true } else false
    }.focusRequester(requester).focusable(canFocus).verticalScroll(state, enabled, flingBehavior, reverseScrolling)
        .padding(bottom = if (watchIsRound()) 48.dp else 16.dp)
}

@Composable
fun WatchLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(6.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    reverseLayout: Boolean = false,
    userScrollEnabled: Boolean = true,
    content: WatchGridScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val requester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val header = LocalWatchHeaderState.current
    val canFocus = userScrollEnabled && LocalWatchListActive.current && !androidx.compose.ui.platform.LocalInspectionMode.current && WindowInsets.ime.getBottom(LocalDensity.current) == 0
    LaunchedEffect(canFocus) { if (canFocus) runCatching { requester.requestFocus() } }
    LazyVerticalGrid(columns = GridCells.Fixed(1), state = state,
        modifier = modifier.onRotaryScrollEvent {
            if (canFocus) { scope.launch { scrollWatchContent(it.verticalScrollPixels, header) { delta -> state.scrollBy(delta) } }; true } else false
        }.focusRequester(requester).focusable(canFocus),
        contentPadding = PaddingValues(
            start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr).coerceAtMost(4.dp),
            end = contentPadding.calculateRightPadding(LayoutDirection.Ltr).coerceAtMost(4.dp),
            top = contentPadding.calculateTopPadding().coerceAtMost(6.dp),
            bottom = contentPadding.calculateBottomPadding() + if (configuration.isScreenRound) (configuration.screenHeightDp * .32f).dp else 16.dp),
        verticalArrangement = verticalArrangement, horizontalArrangement = horizontalArrangement,
        reverseLayout = reverseLayout, userScrollEnabled = userScrollEnabled,
        content = { content(WatchGridScope(this)) })
}

/**
 * Decorate grid entries through composition, not inheritance of Foundation's sealed scope.
 * Keep the original grid state, item keys, content types, spans and item-scope receiver.
 */
class WatchGridScope internal constructor(private val delegate: LazyGridScope) {
    fun item(
        key: Any? = null,
        span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
        contentType: Any? = null,
        content: @Composable LazyGridItemScope.() -> Unit,
    ) {
        delegate.item(key, span, contentType) { WatchDialItem { content() } }
    }

    fun items(
        count: Int,
        key: ((Int) -> Any)? = null,
        span: (LazyGridItemSpanScope.(Int) -> GridItemSpan)? = null,
        contentType: (Int) -> Any? = { null },
        itemContent: @Composable LazyGridItemScope.(Int) -> Unit,
    ) {
        delegate.items(count, key, span, contentType) { index -> WatchDialItem { itemContent(index) } }
    }

    fun <T> items(
        items: List<T>,
        key: ((T) -> Any)? = null,
        span: (LazyGridItemSpanScope.(T) -> GridItemSpan)? = null,
        contentType: (T) -> Any? = { null },
        itemContent: @Composable LazyGridItemScope.(T) -> Unit,
    ) {
        items(count = items.size,
            key = key?.let { selector -> { index: Int -> selector(items[index]) } },
            span = span?.let { selector -> { index: Int -> selector(items[index]) } },
            contentType = { index -> contentType(items[index]) },
        ) { index -> itemContent(items[index]) }
    }

    fun <T> itemsIndexed(
        items: List<T>,
        key: ((Int, T) -> Any)? = null,
        span: (LazyGridItemSpanScope.(Int, T) -> GridItemSpan)? = null,
        contentType: (Int, T) -> Any? = { _, _ -> null },
        itemContent: @Composable LazyGridItemScope.(Int, T) -> Unit,
    ) {
        items(count = items.size,
            key = key?.let { selector -> { index: Int -> selector(index, items[index]) } },
            span = span?.let { selector -> { index: Int -> selector(index, items[index]) } },
            contentType = { index -> contentType(index, items[index]) },
        ) { index -> itemContent(index, items[index]) }
    }

    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyGridItemScope.(Int) -> Unit,
    ) {
        // On a watch, let filters scroll away instead of permanently covering the list.
        item(key = key, contentType = contentType) { content(0) }
    }
}
