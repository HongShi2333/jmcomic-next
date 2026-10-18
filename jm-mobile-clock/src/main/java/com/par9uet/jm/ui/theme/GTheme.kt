package com.par9uet.jm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_DEFAULT
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_MONET
import com.par9uet.jm.store.LocalSettingManager
import org.koin.compose.getKoin

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColorScheme> {
    error("No extended color scheme provided")
}

object ExtendedTheme {
    val colors: ExtendedColorScheme
        @Composable
        get() = LocalExtendedColors.current
}

// 内置预设的四色（亮色/暗色），保持与 Color.kt 中的 light/dark 一致
private val PRESET_COLORS_DARK = mapOf(
    COLOR_PALETTE_PRESET_DEFAULT to longArrayOf(0xFFB8C7EF, 0xFFC2C5DD, 0xFFE4BAD8, 0xFFFFB4AB),
    "ocean" to longArrayOf(0xFF37C9CD, 0xFFB1CBCB, 0xFFB0C8E8, 0xFFFFB4AB),
    "sunset" to longArrayOf(0xFFFFB866, 0xFFE0C68F, 0xFFFFB3B5, 0xFFFFB4AB),
    "forest" to longArrayOf(0xFF7CDFA0, 0xFFB6CCBC, 0xFFA0D0D3, 0xFFFFB4AB),
    "lavender" to longArrayOf(0xFFD0BCFF, 0xFFCCC2DC, 0xFFEFB8C8, 0xFFFFB4AB),
)
private val PRESET_COLORS_LIGHT = mapOf(
    COLOR_PALETTE_PRESET_DEFAULT to longArrayOf(0xFF4F5F7F, 0xFF5A5D72, 0xFF75546F, 0xFFBA1A1A),
    "ocean" to longArrayOf(0xFF00696D, 0xFF4A6364, 0xFF48607E, 0xFFBA1A1A),
    "sunset" to longArrayOf(0xFF8C5000, 0xFF735C2D, 0xFF9C4146, 0xFFBA1A1A),
    "forest" to longArrayOf(0xFF2E6B3E, 0xFF4F6352, 0xFF38656A, 0xFFBA1A1A),
    "lavender" to longArrayOf(0xFF6750A4, 0xFF625B71, 0xFF7D5260, 0xFFBA1A1A),
)

private fun String.toColorOrNull(): Color? {
    return runCatching {
        val hex = this.removePrefix("#")
        val long = if (hex.length == 6) "FF$hex".toLong(16) else hex.toLong(16)
        Color(long.toInt())
    }.getOrNull()
}

@Composable
fun AppTheme(
    localSettingManager: LocalSettingManager = getKoin().get(),
    content: @Composable () -> Unit
) {
    val setting by localSettingManager.localSettingState.collectAsState()
    val theme = setting.theme
    val context = LocalContext.current
    val isDark = when (theme) {
        "auto" -> isSystemInDarkTheme()
        "dark" -> true
        else -> false
    }
    // 仅当用户选择"莫奈取色"预设时才使用动态色；其余预设始终应用调色板覆盖
    val supportDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamic = setting.colorPalettePreset == COLOR_PALETTE_PRESET_MONET && supportDynamic
    val baseScheme = when {
        useDynamic && isDark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        isDark -> darkScheme
        else -> lightScheme
    }
    val colorScheme = if (useDynamic) {
        baseScheme
    } else {
        applyPaletteOverride(baseScheme, setting.colorPalettePreset, isDark) { slot ->
            when (slot) {
                0 -> setting.customColorPrimary
                1 -> setting.customColorSecondary
                2 -> setting.customColorTertiary
                else -> setting.customColorError
            }
        }
    }
    val extendedColorScheme = extendedColorSchemeFor(colorScheme, isDark)

    // 切换深浅色时同步刷新系统栏样式（状态栏/导航栏图标颜色）
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.DisposableEffect(isDark) {
            val window = (view.context as android.app.Activity).window
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
            onDispose {}
        }
    }

    val motionEnabled = com.par9uet.jm.ui.components.rememberWatchMotionEnabled()
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColorScheme,
        com.par9uet.jm.ui.components.LocalWatchMotionEnabled provides motionEnabled,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content
            )
        }
    }
}

private fun applyPaletteOverride(
    base: ColorScheme,
    presetId: String,
    isDark: Boolean,
    customOverride: (Int) -> String?,
): ColorScheme {
    val presetColors = (if (isDark) PRESET_COLORS_DARK else PRESET_COLORS_LIGHT)[presetId]
        ?: (if (isDark) PRESET_COLORS_DARK else PRESET_COLORS_LIGHT)[COLOR_PALETTE_PRESET_DEFAULT]!!
    val primary = customOverride(0)?.toColorOrNull() ?: Color(presetColors[0])
    val secondary = customOverride(1)?.toColorOrNull() ?: Color(presetColors[1])
    val tertiary = customOverride(2)?.toColorOrNull() ?: Color(presetColors[2])
    val error = customOverride(3)?.toColorOrNull() ?: Color(presetColors[3])
    // 浅色模式下容器色应叠加在白色上产生浅色调，深色模式叠加在黑色上产生深色调
    val containerBase = if (isDark) Color.Black else Color.White
    val onContainerColor = if (isDark) Color.White else Color.Black
    return base.copy(
        primary = primary,
        onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White,
        primaryContainer = primary.copy(alpha = 0.3f).compositeOver(containerBase),
        onPrimaryContainer = if (isDark) primary else onContainerColor,
        inversePrimary = primary,
        secondary = secondary,
        onSecondary = if (secondary.luminance() > 0.5f) Color.Black else Color.White,
        secondaryContainer = secondary.copy(alpha = 0.3f).compositeOver(containerBase),
        onSecondaryContainer = if (isDark) secondary else onContainerColor,
        tertiary = tertiary,
        onTertiary = if (tertiary.luminance() > 0.5f) Color.Black else Color.White,
        tertiaryContainer = tertiary.copy(alpha = 0.3f).compositeOver(containerBase),
        onTertiaryContainer = if (isDark) tertiary else onContainerColor,
        error = error,
        onError = if (error.luminance() > 0.5f) Color.Black else Color.White,
        errorContainer = error.copy(alpha = 0.3f).compositeOver(containerBase),
        onErrorContainer = if (isDark) error else onContainerColor,
    )
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

private fun Color.compositeOver(background: Color): Color {
    val fgAlpha = alpha
    if (fgAlpha >= 1f) return this
    val a = alpha + background.alpha * (1f - fgAlpha)
    if (a <= 0f) return Color.Transparent
    val r = (red * fgAlpha + background.red * background.alpha * (1f - fgAlpha)) / a
    val g = (green * fgAlpha + background.green * background.alpha * (1f - fgAlpha)) / a
    val b = (blue * fgAlpha + background.blue * background.alpha * (1f - fgAlpha)) / a
    return Color(r, g, b, a)
}
