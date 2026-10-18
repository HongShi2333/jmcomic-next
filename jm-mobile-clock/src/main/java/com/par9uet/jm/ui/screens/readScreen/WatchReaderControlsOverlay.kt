package com.par9uet.jm.ui.screens.readScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.par9uet.jm.ui.components.*
import kotlin.math.exp
import kotlin.math.roundToInt

internal fun parseWatchReaderPage(text: String, pageCount: Int): Int? =
    text.trim().toIntOrNull()?.takeIf { it in 1..pageCount }?.minus(1)

/** Transparent, in-place controls. The existing image Canvas stays the same size and stays interactive. */
@Composable
internal fun WatchReaderControlsOverlay(
    currentIndex: Int, pageCount: Int, scale: Float, minimumScale: Float, imageReady: Boolean,
    chapterTitle: String, previousChapterEnabled: Boolean, nextChapterEnabled: Boolean, chapterListEnabled: Boolean,
    collectEnabled: Boolean, collected: Boolean, commentsEnabled: Boolean,
    onResume: () -> Unit, onPageSelected: (Int) -> Unit, onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit, onOpenChapters: () -> Unit,
    onScaleChange: (Float) -> Unit, onFitWidth: () -> Unit, onFitPage: () -> Unit,
    onToggleCollect: () -> Unit, onComments: () -> Unit, onSettings: () -> Unit, onExit: () -> Unit,
    onZoomStep: (Float) -> Unit = { onScaleChange((scale * it).coerceIn(minimumScale, 4f)) },
) {
    var zoomMode by rememberSaveable { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var pageText by rememberSaveable { mutableStateOf("") }
    var scrubPage by remember { mutableStateOf<Int?>(null) }
    val focus = remember { FocusRequester() }
    val active = LocalWatchListActive.current && LocalWindowInfo.current.isWindowFocused && panel == null
    val largeText = LocalDensity.current.fontScale > 1.3f
    var rotaryDistance by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) { if (active) runCatching { focus.requestFocus() } }
    LaunchedEffect(zoomMode) { rotaryDistance = 0f }
    BackHandler(enabled = panel == null) { onResume() }
    BoxWithConstraints(
        Modifier.fillMaxSize().testTag("reader-live-controls")
            .onRotaryScrollEvent {
                if (!active) false else {
                    if (zoomMode && imageReady) onScaleChange((scale * exp(it.verticalScrollPixels * .003f)).coerceIn(minimumScale, 4f))
                    else if (!zoomMode) {
                        rotaryDistance += it.verticalScrollPixels
                        if (kotlin.math.abs(rotaryDistance) >= 60f) {
                            onPageSelected((currentIndex + if (rotaryDistance > 0) 1 else -1).coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
                            rotaryDistance = 0f
                        }
                    }
                    true
                }
            }.focusRequester(focus).focusable(active),
    ) {
        // Narrow centered chrome avoids the round corners; no fullscreen scrim/Surface.
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = maxHeight * .065f).fillMaxWidth(.72f).widthIn(max = 176.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveReaderButton(onClick = { panel = "menu" }, description = "阅读选项与跳页",
                modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(scrubPage ?: currentIndex) + 1} / $pageCount", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text(if (imageReady) "${(scale * 100).roundToInt()}%" else "加载中",
                        style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            LiveReaderButton(onResume, "收起阅读控制", Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, null, Modifier.size(18.dp))
            }
        }
        Row(
            Modifier.align(Alignment.TopCenter).offset(y = minOf(readerControlsCenterY(maxWidth.value, maxHeight.value), maxHeight.value * .59f).dp - 24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
                LiveReaderButton(
                    onClick = { if (zoomMode) onZoomStep(1f / 1.25f) else onPageSelected(currentIndex - 1) },
                    description = if (zoomMode) "缩小漫画" else "上一页", modifier = Modifier.size(48.dp),
                    enabled = if (zoomMode) imageReady && scale > minimumScale + .001f else currentIndex > 0,
                ) { Icon(if (zoomMode) Icons.Rounded.Remove else Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp)) }
                LiveReaderButton(
                    onClick = { zoomMode = !zoomMode }, description = if (zoomMode) "切换到翻页" else "切换到缩放",
                    modifier = Modifier.size(48.dp),
                ) {
                    if (largeText) Icon(if (zoomMode) Icons.Rounded.ZoomIn else Icons.Rounded.Layers, null, Modifier.size(18.dp))
                    else Text(if (zoomMode) "缩放" else "翻页", style = MaterialTheme.typography.labelSmall)
                }
                LiveReaderButton(
                    onClick = { if (zoomMode) onZoomStep(1.25f) else onPageSelected(currentIndex + 1) },
                    description = if (zoomMode) "放大漫画" else "下一页", modifier = Modifier.size(48.dp),
                    enabled = if (zoomMode) imageReady && scale < 3.999f else currentIndex < pageCount - 1,
                ) { Icon(if (zoomMode) Icons.Rounded.Add else Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp)) }
        }
        ReaderProgressBar(
            page = scrubPage ?: currentIndex, pageCount = pageCount,
            onPreview = { scrubPage = it }, onCommit = onPageSelected,
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = maxHeight * .86f - 18.dp).fillMaxWidth(.56f),
        )
    }
    when (panel) {
        "page" -> {
            val page = parseWatchReaderPage(pageText, pageCount)
            WatchTextEditorDialog("跳转页码", pageText, { pageText = it.filter(Char::isDigit).take(6) },
                onDismiss = { panel = "menu" }, onConfirm = { page?.let(onPageSelected); panel = null },
                confirmLabel = "跳转", confirmEnabled = page != null, singleLine = true, keyboardType = KeyboardType.Number,
                hint = "请输入 1～$pageCount")
        }
        "menu" -> WatchPanelDialog("阅读选项", onDismiss = { panel = null }) {
            item { WatchActionPair("适宽", "全图", imageReady, imageReady,
                onFirst = { onFitWidth(); zoomMode = true; panel = null },
                onSecond = { onFitPage(); zoomMode = true; panel = null }) }
            item { WatchActionRow("跳到指定页", "当前 ${currentIndex + 1} / $pageCount") { pageText = (currentIndex + 1).toString(); panel = "page" } }
            if (chapterTitle.isNotBlank()) item { Text(chapterTitle, style = MaterialTheme.typography.bodySmall) }
            item { WatchActionPair("上一章", "下一章", previousChapterEnabled, nextChapterEnabled, onPreviousChapter, onNextChapter) }
            item { WatchActionRow("章节目录", enabled = chapterListEnabled, onClick = onOpenChapters) }
            item { WatchActionRow(if (collected) "取消收藏" else "收藏漫画", enabled = collectEnabled, onClick = onToggleCollect) }
            if (commentsEnabled) item { WatchActionRow("查看评论", onClick = onComments) }
            item { WatchActionRow("阅读设置", onClick = onSettings) }
            item { WatchActionRow("退出阅读", onClick = onExit) }
        }
    }
}

@Composable
private fun LiveReaderButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier.heightIn(min = 48.dp).semantics { contentDescription = description }
            .clickable(enabled = enabled, interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.heightIn(min = 28.dp).watchPressFeedback(source), shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .90f),
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .4f),
            border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant),
        ) { Box(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), contentAlignment = Alignment.Center) { content() } }
    }
}
