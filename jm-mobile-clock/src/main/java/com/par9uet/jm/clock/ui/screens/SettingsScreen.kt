package com.par9uet.jm.clock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.par9uet.jm.clock.data.WatchThemeMode
import com.par9uet.jm.clock.data.WatchUiPreferences
import com.par9uet.jm.clock.ui.components.WatchInfoCard
import com.par9uet.jm.clock.ui.components.WatchActionRow
import com.par9uet.jm.clock.ui.components.WatchScreen

@Composable
fun SettingsScreen(
    preferences: WatchUiPreferences,
    onPreferencesChange: (WatchUiPreferences) -> Unit,
    onAppLockSettings: () -> Unit,
    onBack: () -> Unit,
) {
    WatchScreen(title = "手表设置", onBack = onBack) {
        item(key = "theme_title") {
            Text("主题", style = MaterialTheme.typography.titleSmall)
        }
        WatchThemeMode.entries.forEach { mode ->
            item(key = "theme_${mode.storageValue}") {
                WatchChoiceRow(
                    title = mode.label,
                    selected = preferences.themeMode == mode,
                    onClick = { onPreferencesChange(preferences.copy(themeMode = mode)) },
                )
            }
        }

        item(key = "scale_title") {
            Text("界面缩放", style = MaterialTheme.typography.titleSmall)
        }
        SCALE_OPTIONS.forEach { option ->
            item(key = "scale_$option") {
                WatchChoiceRow(
                    title = when (option) {
                        0.85f -> "紧凑 85%"
                        1.15f -> "放大 115%"
                        else -> "标准 100%"
                    },
                    selected = kotlin.math.abs(preferences.uiScale - option) < 0.01f,
                    onClick = { onPreferencesChange(preferences.copy(uiScale = option)) },
                )
            }
        }

        item(key = "app_lock") {
            WatchActionRow(
                title = "应用锁",
                subtitle = if (preferences.appLockEnabled) "已启用，离开应用后需要 PIN" else "使用 PIN 保护手表端内容",
                icon = Icons.Default.Lock,
                onClick = onAppLockSettings,
            )
        }

        item(key = "clipboard") {
            WatchToggleSettingRow(
                title = "剪切板自动检测",
                subtitle = "回到前台时识别 JM 编号并请求跳转",
                checked = preferences.clipboardAutoDetectEnabled,
                onCheckedChange = { enabled ->
                    onPreferencesChange(preferences.copy(clipboardAutoDetectEnabled = enabled))
                },
            )
        }

        item(key = "reader_storage") {
            WatchInfoCard(
                title = "无缓存阅读",
                body = "账号、应用锁和阅读进度会保留；封面、漫画页和下载文件不会缓存到本机。",
            )
        }
    }
}

@Composable
private fun WatchChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val minimumTouchTarget = LocalMinimumInteractiveComponentSize.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minimumTouchTarget)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "已选择")
            }
        }
    }
}

@Composable
private fun WatchToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val minimumTouchTarget = LocalMinimumInteractiveComponentSize.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minimumTouchTarget)
            .clickable { onCheckedChange(!checked) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private val SCALE_OPTIONS = listOf(0.85f, 1f, 1.15f)
