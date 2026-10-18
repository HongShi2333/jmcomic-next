package com.par9uet.jm.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.heightIn

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.RestartAlt
import com.par9uet.jm.ui.components.WatchAlertDialog as AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_CUSTOM
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_DEFAULT
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_FOREST
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_LAVENDER
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_MONET
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_OCEAN
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_SUNSET
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.ui.components.CommonScaffold
import org.koin.compose.getKoin

// 预设方案：每组 4 个颜色对应 [primary, secondary, tertiary, error]，ARGB hex
private data class ColorPreset(
    val id: String,
    val name: String,
    val colors: List<Long>,
)

private val COLOR_PRESETS = listOf(
    ColorPreset(COLOR_PALETTE_PRESET_DEFAULT, "默认蓝", listOf(0xFF4F5F7F, 0xFF5A5D72, 0xFF75546F, 0xFFBA1A1A)),
    ColorPreset(COLOR_PALETTE_PRESET_OCEAN, "海洋青", listOf(0xFF00696D, 0xFF4A6364, 0xFF48607E, 0xFFBA1A1A)),
    ColorPreset(COLOR_PALETTE_PRESET_SUNSET, "日落橙", listOf(0xFF8C5000, 0xFF735C2D, 0xFF9C4146, 0xFFBA1A1A)),
    ColorPreset(COLOR_PALETTE_PRESET_FOREST, "森林绿", listOf(0xFF2E6B3E, 0xFF4F6352, 0xFF38656A, 0xFFBA1A1A)),
    ColorPreset(COLOR_PALETTE_PRESET_LAVENDER, "薰衣紫", listOf(0xFF6750A4, 0xFF625B71, 0xFF7D5260, 0xFFBA1A1A)),
)

private enum class ColorSlot(val label: String) {
    Primary("主色"),
    Secondary("辅助色"),
    Tertiary("第三色"),
    Error("错误色");

    companion object {
        fun fromIndex(index: Int): ColorSlot = entries.getOrElse(index) { Primary }
    }
}

@Composable
fun ColorPaletteScreen(
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val localSetting by localSettingManager.localSettingState.collectAsState()
    val context = LocalContext.current

    var editingSlot by remember { mutableStateOf<ColorSlot?>(null) }

    val currentPreset = COLOR_PRESETS.firstOrNull { it.id == localSetting.colorPalettePreset }
    // 当前生效的四色：莫奈取色时从动态色获取，其余从预设/自定义获取
    val effectiveColors = remember(localSetting) {
        if (localSetting.colorPalettePreset == COLOR_PALETTE_PRESET_MONET &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            val ds = dynamicLightColorScheme(context)
            listOf(ds.primary, ds.secondary, ds.tertiary, ds.error)
        } else {
            val presetColors = currentPreset?.colors ?: COLOR_PRESETS[0].colors
            listOf(
                localSetting.customColorPrimary?.toColorOrNull() ?: Color(presetColors[0]),
                localSetting.customColorSecondary?.toColorOrNull() ?: Color(presetColors[1]),
                localSetting.customColorTertiary?.toColorOrNull() ?: Color(presetColors[2]),
                localSetting.customColorError?.toColorOrNull() ?: Color(presetColors[3]),
            )
        }
    }
    val hasCustomOverride = localSetting.customColorPrimary != null ||
            localSetting.customColorSecondary != null ||
            localSetting.customColorTertiary != null ||
            localSetting.customColorError != null

    CommonScaffold(title = "调色板") {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Section(title = "当前配色") {
                    ColorPreviewRow(colors = effectiveColors)
                }
            }
            item {
                Section(title = "预设方案") {
                    PresetGrid(
                        selectedPreset = localSetting.colorPalettePreset,
                        hasCustomOverride = hasCustomOverride,
                        onSelect = { presetId ->
                            // 切换预设时清空自定义颜色覆盖
                            localSettingManager.updateCustomColor(null, null, null, null)
                            localSettingManager.updateColorPalettePreset(presetId)
                        }
                    )
                }
            }
            item {
                Section(title = "自定义颜色") {
                    ColorSlotRow(
                        label = ColorSlot.Primary.label,
                        color = effectiveColors[0],
                        onPick = { editingSlot = ColorSlot.Primary }
                    )
                    ColorSlotRow(
                        label = ColorSlot.Secondary.label,
                        color = effectiveColors[1],
                        onPick = { editingSlot = ColorSlot.Secondary }
                    )
                    ColorSlotRow(
                        label = ColorSlot.Tertiary.label,
                        color = effectiveColors[2],
                        onPick = { editingSlot = ColorSlot.Tertiary }
                    )
                    ColorSlotRow(
                        label = ColorSlot.Error.label,
                        color = effectiveColors[3],
                        onPick = { editingSlot = ColorSlot.Error }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            localSettingManager.updateCustomColor(null, null, null, null)
                            localSettingManager.updateColorPalettePreset(COLOR_PALETTE_PRESET_DEFAULT)
                        }) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("重置为默认")
                        }
                    }
                }
            }
        }
    }

    editingSlot?.let { slot ->
        ColorPickerDialog(
            slot = slot,
            initialColor = when (slot) {
                ColorSlot.Primary -> effectiveColors[0]
                ColorSlot.Secondary -> effectiveColors[1]
                ColorSlot.Tertiary -> effectiveColors[2]
                ColorSlot.Error -> effectiveColors[3]
            },
            onDismiss = { editingSlot = null },
            onConfirm = { newColor ->
                val hex = newColor.toArgbHex()
                val primary = if (slot == ColorSlot.Primary) hex else localSetting.customColorPrimary
                val secondary = if (slot == ColorSlot.Secondary) hex else localSetting.customColorSecondary
                val tertiary = if (slot == ColorSlot.Tertiary) hex else localSetting.customColorTertiary
                val error = if (slot == ColorSlot.Error) hex else localSetting.customColorError
                // 一旦自定义颜色，预设自动切到 custom
                if (localSetting.colorPalettePreset != COLOR_PALETTE_PRESET_CUSTOM) {
                    localSettingManager.updateColorPalettePreset(COLOR_PALETTE_PRESET_CUSTOM)
                }
                localSettingManager.updateCustomColor(primary, secondary, tertiary, error)
                editingSlot = null
            }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ColorPreviewRow(colors: List<Color>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun PresetGrid(
    selectedPreset: String,
    hasCustomOverride: Boolean,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 莫奈取色（仅 Android 12+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamicScheme = remember { dynamicLightColorScheme(context) }
            val monetPreset = ColorPreset(
                id = COLOR_PALETTE_PRESET_MONET,
                name = "莫奈取色",
                colors = listOf(
                    dynamicScheme.primary.value.toLong(),
                    dynamicScheme.secondary.value.toLong(),
                    dynamicScheme.tertiary.value.toLong(),
                    dynamicScheme.error.value.toLong(),
                )
            )
            PresetItem(
                preset = monetPreset,
                selected = !hasCustomOverride && monetPreset.id == selectedPreset,
                onClick = { onSelect(monetPreset.id) }
            )
        }
        COLOR_PRESETS.forEach { preset ->
            PresetItem(
                preset = preset,
                selected = !hasCustomOverride && preset.id == selectedPreset,
                onClick = { onSelect(preset.id) }
            )
        }
        if (hasCustomOverride) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(COLOR_PALETTE_PRESET_CUSTOM) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "自定义",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "已自定义颜色，点击预设恢复",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetItem(
    preset: ColorPreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${if (selected) "✓ " else ""}${preset.name}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                preset.colors.forEach { color -> Box(Modifier.size(16.dp).clip(CircleShape).background(Color(color))) }
            }
        }
    }
}

@Composable
private fun ColorSlotRow(
    label: String,
    color: Color,
    onPick: () -> Unit
) {
    Surface(onClick = onPick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(color))
            Text("编辑$label", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ColorPickerDialog(
    slot: ColorSlot,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var red by remember { mutableStateOf((initialColor.red * 255).toInt()) }
    var green by remember { mutableStateOf((initialColor.green * 255).toInt()) }
    var blue by remember { mutableStateOf((initialColor.blue * 255).toInt()) }
    val currentColor = Color(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择${slot.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                )
                ColorSliderRow(label = "红", value = red, color = Color(0xFFE53935)) { red = it }
                ColorSliderRow(label = "绿", value = green, color = Color(0xFF43A047)) { green = it }
                ColorSliderRow(label = "蓝", value = blue, color = Color(0xFF1E88E5)) { blue = it }
                Text(
                    text = "HEX: ${currentColor.toArgbHex()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ColorSliderRow(
    label: String,
    value: Int,
    color: Color,
    onChange: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label · $value", style = MaterialTheme.typography.bodyMedium)
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt().coerceIn(0, 255)) }, valueRange = 0f..255f,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
    }
}

private fun String.toColorOrNull(): Color? {
    return runCatching {
        val hex = this.removePrefix("#")
        val long = if (hex.length == 6) "FF$hex".toLong(16) else hex.toLong(16)
        Color(long.toInt())
    }.getOrNull()
}

private fun Color.toArgbHex(): String {
    val argb = (0xFF shl 24) or
            ((red * 255).toInt() shl 16) or
            ((green * 255).toInt() shl 8) or
            (blue * 255).toInt()
    return "#${argb.toUInt().toString(16).uppercase().padStart(8, '0')}"
}
