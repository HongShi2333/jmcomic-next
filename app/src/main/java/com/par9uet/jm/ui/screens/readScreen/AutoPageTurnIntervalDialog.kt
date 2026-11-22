package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.AUTO_PAGE_TURN_MAX_INTERVAL_MS
import com.par9uet.jm.data.models.AUTO_PAGE_TURN_MIN_INTERVAL_MS
import java.util.Locale
import kotlin.math.roundToInt

/** 设置页展示用的间隔文案：不足 1 秒按毫秒显示，其余按秒显示。 */
fun autoPageTurnIntervalLabel(intervalMs: Int): String {
    val clamped = intervalMs.coerceIn(AUTO_PAGE_TURN_MIN_INTERVAL_MS, AUTO_PAGE_TURN_MAX_INTERVAL_MS)
    return if (clamped < 1000) "$clamped 毫秒" else "${formatAutoPageTurnSeconds(clamped)} 秒"
}

private fun formatAutoPageTurnSeconds(intervalMs: Int): String {
    val seconds = intervalMs / 1000.0
    return if (seconds % 1.0 == 0.0) {
        seconds.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", seconds).trimEnd('0').trimEnd('.')
    }
}

private fun formatIntervalInput(intervalMs: Int, useSeconds: Boolean): String =
    if (useSeconds) formatAutoPageTurnSeconds(intervalMs) else intervalMs.toString()

private fun parseIntervalInput(raw: String, useSeconds: Boolean): Int? {
    val trimmed = raw.trim().replace(',', '.')
    if (trimmed.isEmpty()) return null
    val value = trimmed.toDoubleOrNull() ?: return null
    if (!value.isFinite()) return null
    return if (useSeconds) (value * 1000).roundToInt() else value.roundToInt()
}

/**
 * 自动翻页间隔面板：秒 / 毫秒两种录入单位，滑块与数字输入同步。
 * 阅读页侧栏长按与设置页入口共用这一份实现，底层始终以毫秒落库。
 */
@Composable
fun AutoPageTurnIntervalDialog(
    intervalMs: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialMs = intervalMs.coerceIn(AUTO_PAGE_TURN_MIN_INTERVAL_MS, AUTO_PAGE_TURN_MAX_INTERVAL_MS)
    var useSeconds by remember { mutableStateOf(initialMs >= 1000) }
    var draftMs by remember { mutableIntStateOf(initialMs) }
    var inputText by remember { mutableStateOf(formatIntervalInput(initialMs, initialMs >= 1000)) }

    fun applyInterval(nextMs: Int) {
        draftMs = nextMs.coerceIn(AUTO_PAGE_TURN_MIN_INTERVAL_MS, AUTO_PAGE_TURN_MAX_INTERVAL_MS)
        inputText = formatIntervalInput(draftMs, useSeconds)
    }

    fun switchUnit(toSeconds: Boolean) {
        if (toSeconds == useSeconds) return
        useSeconds = toSeconds
        inputText = formatIntervalInput(draftMs, toSeconds)
    }

    fun onInputChange(raw: String) {
        inputText = raw
        val parsed = parseIntervalInput(raw, useSeconds) ?: return
        draftMs = parsed.coerceIn(AUTO_PAGE_TURN_MIN_INTERVAL_MS, AUTO_PAGE_TURN_MAX_INTERVAL_MS)
    }

    val sliderRange = if (useSeconds) {
        (AUTO_PAGE_TURN_MIN_INTERVAL_MS / 1000f)..(AUTO_PAGE_TURN_MAX_INTERVAL_MS / 1000f)
    } else {
        AUTO_PAGE_TURN_MIN_INTERVAL_MS.toFloat()..AUTO_PAGE_TURN_MAX_INTERVAL_MS.toFloat()
    }
    val sliderValue = if (useSeconds) draftMs / 1000f else draftMs.toFloat()
    // 秒档按 100 ms 取整，毫秒档按 50 ms 取整，避免滑块产生 2987 这种读数
    val snapStepMs = if (useSeconds) 100 else 50

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动翻页间隔") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "当前间隔：${autoPageTurnIntervalLabel(draftMs)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InputChip(
                        selected = useSeconds,
                        onClick = { switchUnit(true) },
                        label = { Text("秒") }
                    )
                    InputChip(
                        selected = !useSeconds,
                        onClick = { switchUnit(false) },
                        label = { Text("毫秒") }
                    )
                }
                Slider(
                    value = sliderValue.coerceIn(sliderRange.start, sliderRange.endInclusive),
                    onValueChange = { raw ->
                        val rawMs = if (useSeconds) (raw * 1000f).roundToInt() else raw.roundToInt()
                        val snapped = ((rawMs + snapStepMs / 2) / snapStepMs) * snapStepMs
                        applyInterval(snapped)
                    },
                    valueRange = sliderRange,
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { onInputChange(it) },
                    label = { Text(if (useSeconds) "间隔（秒）" else "间隔（毫秒）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "范围 0.2 秒（200 毫秒）到 60 秒。间隔越短越依赖网络与图片解码速度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(draftMs.coerceIn(AUTO_PAGE_TURN_MIN_INTERVAL_MS, AUTO_PAGE_TURN_MAX_INTERVAL_MS))
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
