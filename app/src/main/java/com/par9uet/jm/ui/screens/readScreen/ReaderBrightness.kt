package com.par9uet.jm.ui.screens.readScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal fun readerBrightnessPercent(
    enabled: Boolean, systemDark: Boolean, light: Int, dark: Int, overridePercent: Int = 0,
): Int = when {
    overridePercent > 0 -> overridePercent.coerceIn(1, 100)
    enabled -> (if (systemDark) dark else light).coerceIn(1, 100)
    else -> 100
}

internal fun readerDimmingAlpha(
    enabled: Boolean, systemDark: Boolean, light: Int, dark: Int, overridePercent: Int = 0,
): Float = 1f - readerBrightnessPercent(enabled, systemDark, light, dark, overridePercent) / 100f

@Composable
fun ReaderBrightnessControls(light: Int, dark: Int, onChange: (Int, Int) -> Unit) {
    var lightValue by remember(light) { mutableFloatStateOf(light.toFloat()) }
    var darkValue by remember(dark) { mutableFloatStateOf(dark.toFloat()) }
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "随手机系统的浅色／深色模式切换。100% 为原始画面亮度，可在系统最低亮度下进一步调暗漫画，不影响工具栏和相册保存。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("浅色模式")
            Text("${lightValue.roundToInt()}%")
        }
        Slider(
            value = lightValue,
            onValueChange = { lightValue = it },
            onValueChangeFinished = { onChange(lightValue.roundToInt(), darkValue.roundToInt()) },
            valueRange = 1f..100f,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("深色模式")
            Text("${darkValue.roundToInt()}%")
        }
        Slider(
            value = darkValue,
            onValueChange = { darkValue = it },
            onValueChangeFinished = { onChange(lightValue.roundToInt(), darkValue.roundToInt()) },
            valueRange = 1f..100f,
        )
    }
}
