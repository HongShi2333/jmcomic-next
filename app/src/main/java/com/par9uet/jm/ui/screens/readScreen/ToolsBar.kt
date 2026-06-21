package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ToolsBar(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    pageCount: Int,
    previousChapterEnabled: Boolean,
    nextChapterEnabled: Boolean,
    showResetZoom: Boolean = false,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPageSelected: (index: Int) -> Unit,
    onResetZoom: () -> Unit = {},
) {
    val lastIndex = (pageCount - 1).coerceAtLeast(0)
    val safeIndex = currentIndex.coerceIn(0, lastIndex)
    val currentPage = if (pageCount <= 0) 0 else safeIndex + 1
    val progress = if (pageCount <= 0) 0f else currentPage.toFloat() / pageCount
    val progressText = if (pageCount <= 0) "0%" else "${(progress * 100).roundToInt()}%"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .widthIn(max = 560.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(
                    enabled = previousChapterEnabled,
                    onClick = onPreviousChapter
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "上一章"
                    )
                }
                Text(
                    text = "$currentPage / $pageCount",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalIconButton(
                        enabled = nextChapterEnabled,
                        onClick = onNextChapter
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "下一章"
                        )
                    }
                }
            }
            if (showResetZoom) {
                TextButton(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    onClick = onResetZoom
                ) {
                    Text(
                        text = "还原页面",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            PageProgressBar(
                currentIndex = safeIndex,
                pageCount = pageCount,
                onPageSelected = onPageSelected
            )
        }
    }
}

@Composable
private fun PageProgressBar(
    currentIndex: Int,
    pageCount: Int,
    onPageSelected: (index: Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun selectFromX(x: Float) {
        if (pageCount <= 1 || size.width <= 0) return

        val fraction = (x / size.width).coerceIn(0f, 1f)
        val target = (fraction * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
        onPageSelected(target)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { size = it }
            .pointerInput(pageCount, size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    selectFromX(down.position.x)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                selectFromX(change.position.x)
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val trackHeight = 8.dp.toPx()
            val thumbRadius = 12.dp.toPx()
            val centerY = size.height / 2f
            val radius = trackHeight / 2f
            val activeFraction = if (pageCount <= 1) {
                0f
            } else {
                currentIndex.toFloat() / (pageCount - 1).toFloat()
            }.coerceIn(0f, 1f)
            val activeWidth = this.size.width * activeFraction
            val activeColor = if (pageCount > 1) primary else disabledColor

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(this.size.width, trackHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(activeWidth, trackHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawCircle(
                color = activeColor,
                radius = thumbRadius,
                center = Offset(activeWidth, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.82f),
                radius = 4.dp.toPx(),
                center = Offset(activeWidth, centerY)
            )
        }
    }
}
