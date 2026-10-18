package com.par9uet.jm.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

val LocalWatchListActive = androidx.compose.runtime.staticCompositionLocalOf { true }
private val LocalWatchHasTopBar = androidx.compose.runtime.staticCompositionLocalOf { true }

/**
 * Shared watch layout primitives.
 *
 * Wear OS can expose either a round or a square window. The system clips a
 * round display, but content still needs an inner safe area so buttons and
 * text do not end up in the curved corners.
 */
@Composable
fun watchIsRound(): Boolean = LocalConfiguration.current.isScreenRound

@Composable
fun watchSafePadding(): Dp {
    val configuration = LocalConfiguration.current
    return calculateWatchViewportInsets(
        configuration.screenWidthDp.toFloat(),
        configuration.screenHeightDp.toFloat(),
        configuration.isScreenRound,
    ).horizontal.dp
}

@Composable
fun WatchScaffold(
    title: String,
    titleContent: @Composable (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    showTopBar: Boolean = true,
    scrollHeader: Boolean = true,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val headerState = remember(title, showTopBar) { WatchCollapsingHeaderState() }
    Surface(modifier = Modifier.fillMaxSize().clip(if (watchIsRound()) CircleShape else RoundedCornerShape(0.dp)),
        color = MaterialTheme.colorScheme.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().then(
                if (showTopBar && scrollHeader) Modifier.nestedScroll(headerState) else Modifier,
            ),
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (showTopBar) {
                    if (scrollHeader) WatchCollapsingHeader(headerState) {
                        WatchTopBar(title, titleContent, onNavigateBack, actions = actions)
                    } else WatchTopBar(title, titleContent, onNavigateBack, actions = actions)
                }
            },
            bottomBar = bottomBar,
        ) { innerPadding ->
            androidx.compose.runtime.CompositionLocalProvider(
                LocalWatchHasTopBar provides showTopBar,
                LocalWatchHeaderState provides if (showTopBar && scrollHeader) headerState else null,
            ) {
                Box(Modifier.fillMaxSize().padding(innerPadding).clipToBounds()) { content(innerPadding) }
            }
        }
    }
}

@Composable
fun WatchTopBar(
    title: String,
    titleContent: @Composable (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    horizontalPadding: Dp = 0.dp,
    embedded: Boolean = false,
    actions: @Composable (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.toFloat()
    val capsuleWidth = watchCapsuleWidth(width).dp
    val top = if (embedded) 0.dp else watchCapsuleTop(
        width, configuration.screenHeightDp.toFloat(), capsuleWidth.value, configuration.isScreenRound,
    ).dp
    val density = LocalDensity.current
    val visualHeight = with(density) { MaterialTheme.typography.labelMedium.lineHeight.toDp() } + 10.dp
    val source = remember { MutableInteractionSource() }
    // Only the small capsule paints a background. The 48dp hit area is transparent.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = if (onNavigateBack != null) (top - (48.dp - visualHeight) / 2).coerceAtLeast(0.dp) else top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.heightIn(min = if (onNavigateBack != null) 48.dp else visualHeight),
            contentAlignment = Alignment.Center,
        ) {
            val content: @Composable () -> Unit = {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onNavigateBack != null) Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    if (titleContent != null) titleContent() else {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    }
                }
            }
            if (onNavigateBack != null) {
                Box(Modifier.widthIn(max = capsuleWidth).heightIn(min = 48.dp)
                    .semantics { contentDescription = "返回上一页"; role = Role.Button }
                    .clickable(
                        interactionSource = source, indication = null, role = Role.Button,
                        onClick = onNavigateBack,
                    )) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).widthIn(max = capsuleWidth)
                            .watchPressFeedback(source),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = content,
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.widthIn(max = capsuleWidth), shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh, content = content,
                )
            }
        }
        // Custom toolbar actions remain usable; they don't squeeze a title between two rim icons.
        if (actions != null) Row(
            Modifier.fillMaxWidth().padding(horizontal = watchSafePadding()),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) { actions() }
    }
}

@Composable
fun WatchSearchButton(onClick: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showLabel = maxWidth >= (48f + 26f * LocalConfiguration.current.fontScale).dp
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Row(Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, contentDescription = if (showLabel) null else "搜索", modifier = Modifier.size(20.dp))
                if (showLabel) Text("搜索", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

@Composable
fun WatchMenuButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Rounded.Menu, contentDescription = "打开页面、分类和工具菜单")
    }
}

@Composable
fun WatchCompactHeader(
    title: String,
    onNavigateBack: () -> Unit,
) {
    WatchTopBar(title = title, onNavigateBack = onNavigateBack)
}

@Composable
fun WatchLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    rotaryEnabled: Boolean = true,
    edgeStartPadding: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(6.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    reverseLayout: Boolean = false,
    userScrollEnabled: Boolean = true,
    edgeScalingEnabled: Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val scaling = edgeScalingEnabled && LocalWatchMotionEnabled.current
    val header = LocalWatchHeaderState.current
    val keyboardVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val canFocus = rotaryEnabled && LocalWatchListActive.current && !keyboardVisible && !androidx.compose.ui.platform.LocalInspectionMode.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(canFocus) {
        if (canFocus) runCatching { focusRequester.requestFocus() }
    }
    LazyColumn(
        state = state,
        modifier = modifier
            .onRotaryScrollEvent { event ->
                if (canFocus && userScrollEnabled) {
                    scope.launch { scrollWatchContent(event.verticalScrollPixels, header) { state.scrollBy(it) } }
                    true
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .focusable(enabled = canFocus),
        contentPadding = PaddingValues(
            start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr).coerceAtMost(4.dp),
            end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr).coerceAtMost(4.dp),
            top = contentPadding.calculateTopPadding() + watchListStartPadding(configuration.screenHeightDp.toFloat(), configuration.isScreenRound && edgeStartPadding, LocalWatchHasTopBar.current).dp,
            bottom = contentPadding.calculateBottomPadding() + if (configuration.isScreenRound) (configuration.screenHeightDp * .32f).dp else 16.dp,
        ),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        content = {
            content(WatchListScope(this, state, scaling, 0))
        },
    )
}

@Composable
fun WatchSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                content = content,
            )
        }
    }
}

@Composable
fun WatchEmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

fun watchContentPadding(horizontal: Dp = 4.dp): PaddingValues =
    PaddingValues(horizontal = horizontal, vertical = 6.dp)

/** Decorates each row, rather than reducing the whole viewport. Keys/types stay unchanged. */
private class WatchListScope(
    private val delegate: LazyListScope,
    private val state: LazyListState,
    private val scaling: Boolean,
    private var nextIndex: Int,
) : LazyListScope by delegate {
    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        val index = nextIndex++
        val listState = state
        val edgeScaling = scaling
        delegate.item(key, contentType) { WatchDialItem(listState, index, edgeScaling) { content() } }
    }
    override fun items(count: Int, key: ((Int) -> Any)?, contentType: (Int) -> Any?, itemContent: @Composable LazyItemScope.(Int) -> Unit) {
        val start = nextIndex
        nextIndex += count
        val listState = state
        val edgeScaling = scaling
        delegate.items(count, key, contentType) { index ->
            WatchDialItem(listState, start + index, edgeScaling) { itemContent(index) }
        }
    }
    override fun stickyHeader(key: Any?, contentType: Any?, content: @Composable LazyItemScope.(Int) -> Unit) {
        // Filters must scroll out of the way on a watch, not occupy the viewport permanently.
        item(key, contentType) { content(0) }
    }
}

@Composable
fun WatchDialItem(
    state: LazyListState? = null,
    index: Int = 0,
    scaling: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = watchSafePadding()).graphicsLayer {
        val layout = state?.layoutInfo
        val item = if (scaling) layout?.visibleItemsInfo?.firstOrNull { it.index == index } else null
        val transform = if (layout != null && item != null) watchEdgeTransform(
            item.offset + item.size / 2f, layout.viewportStartOffset.toFloat(),
            layout.viewportEndOffset.toFloat(), true,
        ) else WatchEdgeTransform()
        scaleX = transform.scale; scaleY = transform.scale; alpha = transform.alpha
    }) { content() }
}

/** Two equal circular actions, not a long search pill pushed into the upper dial rim. */
@Composable
fun WatchHomeActions(onSearch: () -> Unit, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onSearch, modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Search, contentDescription = "搜索漫画") }
        }
        Surface(onClick = onMenu, modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Menu, contentDescription = "分类、页面与工具") }
        }
    }
}
