package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.par9uet.jm.data.models.AUTO_PAGE_TURN_MAX_INTERVAL_MS
import com.par9uet.jm.data.models.AUTO_PAGE_TURN_MIN_INTERVAL_MS
import kotlinx.coroutines.delay

/** 倒计时刷新步长：40 ms 足够让进度环连续，也不会每帧都触发。 */
private const val AUTO_PAGE_TURN_TICK_MS = 40L

/**
 * 左侧控件组的「自动翻页」项。
 *
 * @param running 是否处于运行态（图标在播放/暂停之间切换，并只在运行时绘制剩余进度环）
 * @param progress 本次倒计时的剩余比例，1f 为刚开始，0f 为即将翻页。以 lambda 传入，
 * 读取发生在绘制阶段，避免每次倒计时刷新都重组整个阅读页
 * @param onClick 单击启停
 * @param onLongClick 长按打开间隔设置
 */
@Composable
fun ReadAutoPageTurnAction(
    running: Boolean,
    progress: () -> Float,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val ringColor = MaterialTheme.colorScheme.primary
    // 用 rememberUpdatedState 持有回调：重组不会打断进行中的长按
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                detectTapGestures(
                    onTap = { if (enabled) currentOnClick() },
                    onLongPress = { if (enabled) currentOnLongClick() }
                )
            }
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (running) {
                Canvas(modifier = Modifier.size(34.dp)) {
                    // 在绘制阶段读取倒计时，重组与布局都不会被 40 ms 的刷新波及
                    val remaining = progress().coerceIn(0f, 1f)
                    val strokeWidth = 2.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = ringColor.copy(alpha = 0.24f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * remaining,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Icon(
                imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (running) "暂停自动翻页" else "自动翻页",
                modifier = Modifier.size(22.dp),
                tint = if (running) ringColor else contentColor
            )
        }
        Text(
            text = "自动翻页",
            color = contentColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 自动翻页计时器。
 *
 * [pageIndex] 与 [pageCount] 都是 key：任何翻页（自动或手动）都会重启倒计时，
 * 因此"每次翻页后都有完整的停留时间"由 Compose 的重启语义保证，不需要显式重置。
 *
 * @param isBusy 手势或缩放进行中的判定；倒计时到点仍在手势中时丢弃这次触发，等手势结束后重新计时
 * @param onAdvance 顺延一页，返回 false 表示已经是最后一页
 * @param onFinish 无法继续翻页时回调一次（调用方负责退出运行态并提示）
 */
@Composable
fun AutoPageTurnEffect(
    running: Boolean,
    intervalMs: Int,
    pageIndex: Int,
    pageCount: Int,
    isBusy: () -> Boolean,
    onAdvance: () -> Boolean,
    onFinish: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(running, intervalMs, pageIndex, pageCount, lifecycleOwner) {
        if (!running || pageCount <= 0) {
            onProgress(1f)
            return@LaunchedEffect
        }
        val interval = intervalMs
            .coerceIn(AUTO_PAGE_TURN_MIN_INTERVAL_MS, AUTO_PAGE_TURN_MAX_INTERVAL_MS)
            .toLong()
        // 熄屏或切到后台时这段计时被取消（不会在后台偷偷翻页、解码），
        // 回到前台重新进入 RESUMED 后按完整间隔重新计时，运行态保持不变。
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                var elapsed = 0L
                onProgress(1f)
                while (elapsed < interval) {
                    delay(AUTO_PAGE_TURN_TICK_MS)
                    elapsed += AUTO_PAGE_TURN_TICK_MS
                    onProgress((1f - elapsed.toFloat() / interval.toFloat()).coerceIn(0f, 1f))
                }
                if (!isBusy()) break
                // 到点时用户正在拖动或缩放：不抢手势，等手抬起后按完整间隔重新计时
                while (isBusy()) {
                    delay(AUTO_PAGE_TURN_TICK_MS)
                }
            }
            onProgress(1f)
            if (!onAdvance()) {
                onFinish()
            }
        }
    }
}
