package com.par9uet.jm.ui.screens.readScreen

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.par9uet.jm.data.models.READER_BACKGROUND_LIST
import com.par9uet.jm.data.models.READER_BACKGROUND_THEME
import com.par9uet.jm.data.models.READER_BACKGROUND_WHITE
import com.par9uet.jm.data.models.READ_MODE_HORIZONTAL
import com.par9uet.jm.data.models.READ_MODE_LIST
import com.par9uet.jm.data.models.READ_MODE_LTR
import com.par9uet.jm.data.models.READ_MODE_RTL
import com.par9uet.jm.data.models.READ_MODE_VERTICAL
import com.par9uet.jm.data.models.READ_MODE_WEBTOON
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal val ReaderBarColor = Color.Black.copy(alpha = 0.82f)
internal val ReaderBarContentColor = Color.White

internal fun readerModeLabel(mode: String): String = when (mode) {
    READ_MODE_LTR -> "从左到右"
    READ_MODE_RTL -> "从右到左"
    READ_MODE_VERTICAL -> "竖向翻页"
    READ_MODE_HORIZONTAL -> "连续横向"
    else -> "连续竖向"
}

private fun readerModeDescription(mode: String): String = when (mode) {
    READ_MODE_LTR -> "左右滑动翻页，适合日漫以外作品"
    READ_MODE_RTL -> "日漫习惯，从右向左翻页"
    READ_MODE_VERTICAL -> "上下滑动整页翻页"
    READ_MODE_HORIZONTAL -> "横向连续滚动，不自动分页"
    else -> "Webtoon 式纵向连续滚动"
}

internal fun readerBackgroundLabel(background: String): String = when (background) {
    READER_BACKGROUND_WHITE -> "白色"
    READER_BACKGROUND_THEME -> "跟随主题"
    else -> "黑色"
}

@Composable
internal fun ReaderTopBar(
    title: String,
    subtitle: String,
    isCollected: Boolean,
    canCollect: Boolean,
    canComment: Boolean,
    canDownload: Boolean,
    onBack: () -> Unit,
    onToggleCollect: () -> Unit,
    onComment: () -> Unit,
    onDownload: () -> Unit,
    onChapterList: () -> Unit,
) {
    Surface(color = ReaderBarColor, contentColor = ReaderBarContentColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderBarIcon(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "退出阅读",
                onClick = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = ReaderBarContentColor.copy(alpha = 0.7f),
                    )
                }
            }
            ReaderBarIcon(
                icon = if (isCollected) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "收藏",
                enabled = canCollect,
                onClick = onToggleCollect,
            )
            if (canComment) {
                ReaderBarIcon(
                    icon = Icons.AutoMirrored.Outlined.Message,
                    contentDescription = "评论",
                    onClick = onComment,
                )
            }
            if (canDownload) {
                ReaderBarIcon(
                    icon = Icons.Default.Download,
                    contentDescription = "缓存",
                    onClick = onDownload,
                )
            }
            ReaderBarIcon(
                icon = Icons.Default.Menu,
                contentDescription = "章节列表",
                onClick = onChapterList,
            )
        }
    }
}

@Composable
private fun ReaderBarIcon(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(42.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = if (enabled) ReaderBarContentColor else ReaderBarContentColor.copy(alpha = 0.35f),
        )
    }
}

@Composable
internal fun ReaderBottomBar(
    currentIndex: Int,
    pageCount: Int,
    readingMode: String,
    previousChapterEnabled: Boolean,
    nextChapterEnabled: Boolean,
    zoomed: Boolean,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onOpenChapters: () -> Unit,
    onOpenReadingMode: () -> Unit,
    onOpenBrightness: () -> Unit,
    onOpenSettings: () -> Unit,
    onResetZoom: () -> Unit,
) {
    val lastIndex = (pageCount - 1).coerceAtLeast(0)
    val safeIndex = currentIndex.coerceIn(0, lastIndex)
    var sliderValue by remember(safeIndex, pageCount) {
        mutableFloatStateOf(safeIndex.toFloat())
    }
    val sliderPage = sliderValue.roundToInt().coerceIn(0, lastIndex)
    val sliderLabel = if (pageCount <= 0) "0 / 0" else "${sliderPage + 1} / $pageCount"

    Surface(color = ReaderBarColor, contentColor = ReaderBarContentColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReaderBarIcon(
                    icon = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "上一章",
                    enabled = previousChapterEnabled,
                    onClick = onPreviousChapter,
                )
                Slider(
                    modifier = Modifier.weight(1f),
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onPageSelected(sliderValue.roundToInt()) },
                    valueRange = 0f..lastIndex.toFloat().coerceAtLeast(1f),
                    enabled = pageCount > 1,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                    ),
                )
                Text(
                    text = sliderLabel,
                    modifier = Modifier.width(64.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                ReaderBarIcon(
                    icon = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "下一章",
                    enabled = nextChapterEnabled,
                    onClick = onNextChapter,
                )
            }
            if (zoomed) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onResetZoom) {
                        Text("还原页面", color = ReaderBarContentColor, fontSize = 12.sp)
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            ) {
                ReaderQuickAction(
                    icon = Icons.Default.Menu,
                    label = "章节",
                    onClick = onOpenChapters,
                )
                ReaderQuickAction(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = readerModeLabel(readingMode),
                    onClick = onOpenReadingMode,
                )
                ReaderQuickAction(
                    icon = Icons.Rounded.Brightness6,
                    label = "亮度",
                    onClick = onOpenBrightness,
                )
                ReaderQuickAction(
                    icon = Icons.Rounded.Tune,
                    label = "设置",
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun RowScope.ReaderQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(21.dp),
            tint = ReaderBarContentColor,
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
            color = ReaderBarContentColor.copy(alpha = 0.85f),
        )
    }
}

@Composable
internal fun ReaderPageIndicator(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    pageCount: Int,
) {
    if (pageCount <= 0) return
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = ReaderBarContentColor,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = "${currentIndex.coerceIn(0, pageCount - 1) + 1} / $pageCount",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 12.sp,
        )
    }
}

@Composable
internal fun ReaderTapZoneHints(
    modifier: Modifier = Modifier,
    rightToLeft: Boolean,
) {
    Row(modifier = modifier.fillMaxSize()) {
        ReaderTapZoneHint(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            icon = Icons.Default.ChevronLeft,
            label = if (rightToLeft) "下一页" else "上一页",
        )
        ReaderTapZoneHint(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            icon = Icons.Default.Menu,
            label = "菜单",
        )
        ReaderTapZoneHint(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            icon = Icons.Default.ChevronRight,
            label = if (rightToLeft) "上一页" else "下一页",
        )
    }
}

@Composable
private fun ReaderTapZoneHint(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
) {
    Box(
        modifier = modifier.background(Color.White.copy(alpha = 0.03f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(26.dp),
                tint = Color.White.copy(alpha = 0.55f),
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSettingsSheet(
    readMode: String,
    readerBackground: String,
    brightnessPercent: Int,
    brightnessOverridden: Boolean,
    showTapZones: Boolean,
    doubleTapZoomEnabled: Boolean,
    showPageNumber: Boolean,
    volumeKeyPageTurningEnabled: Boolean,
    onReadModeChange: (String) -> Unit,
    onBackgroundChange: (String) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onRestoreBrightness: () -> Unit,
    onShowTapZonesChange: (Boolean) -> Unit,
    onDoubleTapZoomChange: (Boolean) -> Unit,
    onShowPageNumberChange: (Boolean) -> Unit,
    onVolumeKeyPageTurningChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = "阅读设置",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            ReaderSettingsSection(title = "阅读模式") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(READ_MODE_LIST) { mode ->
                        FilterChip(
                            selected = readMode == mode,
                            onClick = { onReadModeChange(mode) },
                            label = { Text(readerModeLabel(mode)) },
                        )
                    }
                }
                Text(
                    text = readerModeDescription(readMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReaderSettingsSection(title = "背景颜色") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(READER_BACKGROUND_LIST) { background ->
                        FilterChip(
                            selected = readerBackground == background,
                            onClick = { onBackgroundChange(background) },
                            label = { Text(readerBackgroundLabel(background)) },
                        )
                    }
                }
            }
            ReaderSettingsSection(title = "亮度") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Brightness6,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        value = brightnessPercent.toFloat(),
                        onValueChange = { onBrightnessChange(it.roundToInt()) },
                        valueRange = 1f..100f,
                    )
                    Text(
                        text = "$brightnessPercent%",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (brightnessOverridden) "本次阅读亮度" else "跟随阅读设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        enabled = brightnessOverridden,
                        onClick = onRestoreBrightness,
                    ) {
                        Text("恢复设置")
                    }
                }
            }
            ReaderSettingsSection(title = "交互") {
                ReaderSwitchRow(
                    title = "显示点按区域",
                    checked = showTapZones,
                    onCheckedChange = onShowTapZonesChange,
                )
                ReaderSwitchRow(
                    title = "双击中央缩放",
                    checked = doubleTapZoomEnabled,
                    onCheckedChange = onDoubleTapZoomChange,
                )
                ReaderSwitchRow(
                    title = "工具栏隐藏时显示页码",
                    checked = showPageNumber,
                    onCheckedChange = onShowPageNumberChange,
                )
                ReaderSwitchRow(
                    title = "音量键翻页",
                    checked = volumeKeyPageTurningEnabled,
                    onCheckedChange = onVolumeKeyPageTurningChange,
                )
            }
        }
    }
}

@Composable
private fun ReaderSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun ReaderSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

internal enum class ReaderTapZone { Previous, Menu, Next }

/**
 * Mihon 风格点按区域：左侧上一页、右侧下一页、中央切换菜单。
 * 中央区域在开启双击缩放时会等待双击超时后再切换菜单。
 */
@Composable
internal fun Modifier.readerTapZones(
    rightToLeft: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMenu: () -> Unit,
    onDoubleTapCenter: ((Offset) -> Unit)? = null,
): Modifier {
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnMenu by rememberUpdatedState(onMenu)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTapCenter)
    val scope = rememberCoroutineScope()
    var pendingMenuJob by remember { mutableStateOf<Job?>(null) }
    var lastCenterTapAt by remember { mutableLongStateOf(0L) }

    return this.pointerInput(rightToLeft, currentOnDoubleTap != null) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Final,
                )
                val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                if (up == null || up.isConsumed) continue
                if ((up.position - down.position).getDistance() >= 10.dp.toPx()) continue

                val width = size.width.toFloat()
                val zone = when {
                    up.position.x < width / 3f ->
                        if (rightToLeft) ReaderTapZone.Next else ReaderTapZone.Previous
                    up.position.x > width * 2f / 3f ->
                        if (rightToLeft) ReaderTapZone.Previous else ReaderTapZone.Next
                    else -> ReaderTapZone.Menu
                }
                if (zone == ReaderTapZone.Menu && currentOnDoubleTap != null) {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastCenterTapAt <= viewConfiguration.doubleTapTimeoutMillis) {
                        lastCenterTapAt = 0L
                        pendingMenuJob?.cancel()
                        pendingMenuJob = null
                        currentOnDoubleTap?.invoke(up.position)
                    } else {
                        lastCenterTapAt = now
                        pendingMenuJob?.cancel()
                        pendingMenuJob = scope.launch {
                            delay(viewConfiguration.doubleTapTimeoutMillis)
                            currentOnMenu()
                        }
                    }
                } else {
                    when (zone) {
                        ReaderTapZone.Previous -> currentOnPrevious()
                        ReaderTapZone.Next -> currentOnNext()
                        ReaderTapZone.Menu -> currentOnMenu()
                    }
                }
            }
        }
    }
}
