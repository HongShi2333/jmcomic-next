package com.par9uet.jm.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.par9uet.jm.data.models.HomeComicSwiperItem
import com.par9uet.jm.utils.orderHomeCategories
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun HomeCategorySortDialog(
    categories: List<HomeComicSwiperItem>,
    order: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftOrder by rememberSaveable { mutableStateOf(order) }
    val ordered = remember(categories, draftOrder) { orderHomeCategories(categories, draftOrder) }
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    var draggedId by remember { mutableStateOf<String?>(null) }
    var draggedTop by remember { mutableFloatStateOf(0f) }
    var draggedHeight by remember { mutableIntStateOf(0) }

    fun move(from: Int, to: Int) {
        val current = orderHomeCategories(categories, draftOrder)
        if (from !in current.indices || to !in current.indices || from == to) return
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        draftOrder = current.toMutableList().apply {
            add(to, removeAt(from))
        }.map { it.id }
        if (firstIndex in minOf(from, to)..maxOf(from, to)) {
            listState.requestScrollToItem(firstIndex, firstOffset)
        }
    }

    fun reorderDraggedItem() {
        val id = draggedId ?: return
        val layout = listState.layoutInfo
        val current = orderHomeCategories(categories, draftOrder)
        val from = current.indexOfFirst { it.id == id }
        if (from < 0) return
        val center = (draggedTop + draggedHeight / 2f).coerceIn(
            layout.viewportStartOffset.toFloat(),
            layout.viewportEndOffset.toFloat(),
        )
        val target = layout.visibleItemsInfo.minByOrNull {
            abs(it.offset + it.size / 2f - center)
        } ?: return
        move(from, target.index)
    }

    val currentReorder by rememberUpdatedState(::reorderDraggedItem)
    val edgeSize = with(LocalDensity.current) { 48.dp.toPx() }
    val scrollSpeed = with(LocalDensity.current) { 480.dp.toPx() }

    LaunchedEffect(draggedId) {
        if (draggedId == null) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (isActive) {
            val frame = withFrameNanos { it }
            val elapsed = ((frame - lastFrame) / 1_000_000_000f).coerceAtMost(0.05f)
            lastFrame = frame
            val layout = listState.layoutInfo
            val center = draggedTop + draggedHeight / 2f
            val edge = minOf(
                edgeSize,
                (layout.viewportEndOffset - layout.viewportStartOffset) / 3f,
            ).coerceAtLeast(1f)
            val direction = when {
                center < layout.viewportStartOffset + edge ->
                    ((center - layout.viewportStartOffset - edge) / edge).coerceAtLeast(-1f)
                center > layout.viewportEndOffset - edge ->
                    ((center - layout.viewportEndOffset + edge) / edge).coerceAtMost(1f)
                else -> 0f
            }
            if (direction != 0f) {
                listState.scrollBy(direction * scrollSpeed * elapsed)
            }
            currentReorder()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("首页分类排序") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("长按右侧三横条拖动排序，保存后记住排列。")
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ordered, key = { it.id }) { category ->
                        val dragging = draggedId == category.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(placementSpec = if (dragging) null else spring())
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (dragging) {
                                        val top = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == category.id }
                                            ?.offset ?: 0
                                        draggedTop - top
                                    } else 0f
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = if (dragging) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            shadowElevation = if (dragging) 8.dp else 0.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val title = category.title
                                Text(
                                    text = title,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .pointerInput(category.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == category.id }
                                                        ?.let { item ->
                                                            draggedTop = item.offset.toFloat()
                                                            draggedHeight = item.size
                                                            draggedId = category.id
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.LongPress,
                                                            )
                                                        }
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    draggedTop += amount.y
                                                    currentReorder()
                                                },
                                                onDragEnd = {
                                                    currentReorder()
                                                    draggedId = null
                                                },
                                                onDragCancel = { draggedId = null },
                                            )
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "长按拖动${title}排序",
                                        modifier = Modifier.size(
                                            with(LocalDensity.current) {
                                                LocalTextStyle.current.fontSize.toDp()
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draftOrder) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { draftOrder = emptyList() }) { Text("恢复默认") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
