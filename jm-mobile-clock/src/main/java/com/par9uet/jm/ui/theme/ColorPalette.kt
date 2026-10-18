package com.par9uet.jm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// 自定义方案的 key
const val COLOR_PALETTE_KEY_CUSTOM = "custom"

/**
 * 调色板方案定义
 * @param key 方案唯一标识
 * @param name 方案显示名称
 * @param lightScheme 浅色模式 ColorScheme
 * @param darkScheme 深色模式 ColorScheme
 * @param showcasePrimary 用于卡片展示的 primary 色块
 * @param showcaseSecondary 用于卡片展示的 secondary 色块
 * @param showcaseTertiary 用于卡片展示的 tertiary 色块
 */
@Immutable
data class ColorPalette(
    val key: String,
    val name: String,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val showcasePrimary: Color,
    val showcaseSecondary: Color,
    val showcaseTertiary: Color,
)

// 构建浅色 ColorScheme：方案特定的核心颜色 + 复用默认中性色
private fun buildLightScheme(
    primary: Color, onPrimary: Color,
    primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color,
    secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color,
    tertiaryContainer: Color, onTertiaryContainer: Color,
    inversePrimary: Color,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    surfaceTint = primary,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    surfaceBright = surfaceBrightLight,
    surfaceDim = surfaceDimLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
)

// 构建深色 ColorScheme：方案特定的核心颜色 + 复用默认中性色
private fun buildDarkScheme(
    primary: Color, onPrimary: Color,
    primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color,
    secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color,
    tertiaryContainer: Color, onTertiaryContainer: Color,
    inversePrimary: Color,
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    surfaceTint = primary,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    surfaceBright = surfaceBrightDark,
    surfaceDim = surfaceDimDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
)

// 默认方案（复用现有蓝灰配色）
private val defaultPalette = ColorPalette(
    key = "default",
    name = "默认蓝",
    lightScheme = lightScheme,
    darkScheme = darkScheme,
    showcasePrimary = primaryLight,
    showcaseSecondary = secondaryLight,
    showcaseTertiary = tertiaryLight,
)

// 蓝色海洋
private val oceanPalette = ColorPalette(
    key = "ocean",
    name = "蓝色海洋",
    lightScheme = buildLightScheme(
        primary = Color(0xFF00696D), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF6FF6FE), onPrimaryContainer = Color(0xFF002021),
        secondary = Color(0xFF4A6363), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E7), onSecondaryContainer = Color(0xFF051F1F),
        tertiary = Color(0xFF4B607C), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD3E4FF), onTertiaryContainer = Color(0xFF041C35),
        inversePrimary = Color(0xFF4CD9E1),
    ),
    darkScheme = buildDarkScheme(
        primary = Color(0xFF4CD9E1), onPrimary = Color(0xFF003739),
        primaryContainer = Color(0xFF004F53), onPrimaryContainer = Color(0xFF6FF6FE),
        secondary = Color(0xFFB0CCCB), onSecondary = Color(0xFF1B3534),
        secondaryContainer = Color(0xFF324B4B), onSecondaryContainer = Color(0xFFCCE8E7),
        tertiary = Color(0xFFB4C8E8), onTertiary = Color(0xFF1B304A),
        tertiaryContainer = Color(0xFF334662), onTertiaryContainer = Color(0xFFD3E4FF),
        inversePrimary = Color(0xFF00696D),
    ),
    showcasePrimary = Color(0xFF00696D),
    showcaseSecondary = Color(0xFF4A6363),
    showcaseTertiary = Color(0xFF4B607C),
)

// 绿色森林
private val forestPalette = ColorPalette(
    key = "forest",
    name = "绿色森林",
    lightScheme = buildLightScheme(
        primary = Color(0xFF386A20), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB8F397), onPrimaryContainer = Color(0xFF042100),
        secondary = Color(0xFF55624C), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E7CB), onSecondaryContainer = Color(0xFF131F0E),
        tertiary = Color(0xFF386666), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBCECEC), onTertiaryContainer = Color(0xFF002020),
        inversePrimary = Color(0xFF9DD67E),
    ),
    darkScheme = buildDarkScheme(
        primary = Color(0xFF9DD67E), onPrimary = Color(0xFF0E3900),
        primaryContainer = Color(0xFF1F5108), onPrimaryContainer = Color(0xFFB8F397),
        secondary = Color(0xFFBCCBB0), onSecondary = Color(0xFF273420),
        secondaryContainer = Color(0xFF3D4A35), onSecondaryContainer = Color(0xFFD8E7CB),
        tertiary = Color(0xFFA0D0D0), onTertiary = Color(0xFF003737),
        tertiaryContainer = Color(0xFF1E4E4E), onTertiaryContainer = Color(0xFFBCECEC),
        inversePrimary = Color(0xFF386A20),
    ),
    showcasePrimary = Color(0xFF386A20),
    showcaseSecondary = Color(0xFF55624C),
    showcaseTertiary = Color(0xFF386666),
)

// 紫色梦境
private val purplePalette = ColorPalette(
    key = "purple",
    name = "紫色梦境",
    lightScheme = buildLightScheme(
        primary = Color(0xFF8B4789), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD7F2), onPrimaryContainer = Color(0xFF360034),
        secondary = Color(0xFF6E5869), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF8DAEE), onSecondaryContainer = Color(0xFF271623),
        tertiary = Color(0xFF825349), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDBCF), onTertiaryContainer = Color(0xFF32110A),
        inversePrimary = Color(0xFFF7ACF4),
    ),
    darkScheme = buildDarkScheme(
        primary = Color(0xFFF7ACF4), onPrimary = Color(0xFF521050),
        primaryContainer = Color(0xFF6F296F), onPrimaryContainer = Color(0xFFFFD7F2),
        secondary = Color(0xFFDBBBD5), onSecondary = Color(0xFF3E2A3A),
        secondaryContainer = Color(0xFF553F51), onSecondaryContainer = Color(0xFFF8DAEE),
        tertiary = Color(0xFFF5B8A7), onTertiary = Color(0xFF4C1910),
        tertiaryContainer = Color(0xFF66332A), onTertiaryContainer = Color(0xFFFFDBCF),
        inversePrimary = Color(0xFF8B4789),
    ),
    showcasePrimary = Color(0xFF8B4789),
    showcaseSecondary = Color(0xFF6E5869),
    showcaseTertiary = Color(0xFF825349),
)

// 橙色日落
private val sunsetPalette = ColorPalette(
    key = "sunset",
    name = "橙色日落",
    lightScheme = buildLightScheme(
        primary = Color(0xFF8D4E00), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCC2), onPrimaryContainer = Color(0xFF2E1500),
        secondary = Color(0xFF755846), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDCC2), onSecondaryContainer = Color(0xFF2A1707),
        tertiary = Color(0xFF5C6236), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE1E8AF), onTertiaryContainer = Color(0xFF1A1E00),
        inversePrimary = Color(0xFFFFB687),
    ),
    darkScheme = buildDarkScheme(
        primary = Color(0xFFFFB687), onPrimary = Color(0xFF4E2600),
        primaryContainer = Color(0xFF6E3900), onPrimaryContainer = Color(0xFFFFDCC2),
        secondary = Color(0xFFE4BFA8), onSecondary = Color(0xFF432B1B),
        secondaryContainer = Color(0xFF5B4030), onSecondaryContainer = Color(0xFFFFDCC2),
        tertiary = Color(0xFFC5CC94), onTertiary = Color(0xFF2F340A),
        tertiaryContainer = Color(0xFF454B1E), onTertiaryContainer = Color(0xFFE1E8AF),
        inversePrimary = Color(0xFF8D4E00),
    ),
    showcasePrimary = Color(0xFF8D4E00),
    showcaseSecondary = Color(0xFF755846),
    showcaseTertiary = Color(0xFF5C6236),
)

// 粉色樱花
private val sakuraPalette = ColorPalette(
    key = "sakura",
    name = "粉色樱花",
    lightScheme = buildLightScheme(
        primary = Color(0xFFA23F7E), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD8E7), onPrimaryContainer = Color(0xFF3E001F),
        secondary = Color(0xFF745660), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD8E7), onSecondaryContainer = Color(0xFF2A151B),
        tertiary = Color(0xFF7D5735), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDCC2), onTertiaryContainer = Color(0xFF2E1500),
        inversePrimary = Color(0xFFFFAFD7),
    ),
    darkScheme = buildDarkScheme(
        primary = Color(0xFFFFAFD7), onPrimary = Color(0xFF630048),
        primaryContainer = Color(0xFF872568), onPrimaryContainer = Color(0xFFFFD8E7),
        secondary = Color(0xFFE3BDC7), onSecondary = Color(0xFF42272E),
        secondaryContainer = Color(0xFF5A3D44), onSecondaryContainer = Color(0xFFFFD8E7),
        tertiary = Color(0xFFEFBC8F), onTertiary = Color(0xFF48210D),
        tertiaryContainer = Color(0xFF623621), onTertiaryContainer = Color(0xFFFFDCC2),
        inversePrimary = Color(0xFFA23F7E),
    ),
    showcasePrimary = Color(0xFFA23F7E),
    showcaseSecondary = Color(0xFF745660),
    showcaseTertiary = Color(0xFF7D5735),
)

// 青色湖泊
private val tealPalette = ColorPalette(
    key = "teal",
    name = "青色湖泊",
    lightScheme = buildLightScheme(
        primary = Color(0xFF006A6A), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF6FF7F7), onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF4A6363), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E7), onSecondaryContainer = Color(0xFF051F1F),
        tertiary = Color(0xFF486079), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD1E4FF), onTertiaryContainer = Color(0xFF001D35),
        inversePrimary = Color(0xFF4CDADA),
    ),
    darkScheme = buildDarkScheme(
        primary = Color(0xFF4CDADA), onPrimary = Color(0xFF003737),
        primaryContainer = Color(0xFF004F4F), onPrimaryContainer = Color(0xFF6FF7F7),
        secondary = Color(0xFFB0CCCB), onSecondary = Color(0xFF1B3534),
        secondaryContainer = Color(0xFF324B4B), onSecondaryContainer = Color(0xFFCCE8E7),
        tertiary = Color(0xFFB2C8E8), onTertiary = Color(0xFF1B304A),
        tertiaryContainer = Color(0xFF2F4762), onTertiaryContainer = Color(0xFFD1E4FF),
        inversePrimary = Color(0xFF006A6A),
    ),
    showcasePrimary = Color(0xFF006A6A),
    showcaseSecondary = Color(0xFF4A6363),
    showcaseTertiary = Color(0xFF486079),
)

// 所有内置调色板方案
val builtInColorPalettes: List<ColorPalette> = listOf(
    defaultPalette,
    oceanPalette,
    forestPalette,
    purplePalette,
    sunsetPalette,
    sakuraPalette,
    tealPalette,
)

// 根据 key 获取调色板方案，找不到时返回默认方案
fun getColorPalette(key: String?): ColorPalette {
    if (key.isNullOrBlank()) return defaultPalette
    return builtInColorPalettes.firstOrNull { it.key == key } ?: defaultPalette
}

// ============= 自定义配色方案生成 =============

// 将 Compose Color 转为 HSV 数组 [hue, saturation, value]
private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsv
    )
    return hsv
}

// 从 HSV 值创建 Compose Color
private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
        )
    )
}

// 根据明度选择对比色（黑或白），确保可读性
private fun contrastColor(v: Float): Color =
    if (v > 0.55f) Color(0xFF000000) else Color(0xFFFFFFFF)

// 基于用户选择的四种主辅颜色生成浅色 ColorScheme
fun generateCustomLightScheme(
    primary: Color,
    secondary: Color? = null,
    tertiary: Color? = null,
    error: Color? = null
): ColorScheme {
    val pHex = primary.toHsv()
    val pH = pHex[0]; val pS = pHex[1]; val pV = pHex[2]
    val onPrimary = contrastColor(pV)
    val primaryContainer = hsvToColor(pH, (pS * 0.3f).coerceAtMost(0.3f), 0.92f)
    val onPrimaryContainer = hsvToColor(pH, pS.coerceAtMost(0.5f), 0.15f)
    // 使用用户提供的 secondary 或自动生成
    val sec = secondary ?: run {
        val secH = (pH + 30) % 360
        val secV = pV.coerceIn(0.3f, 0.7f)
        hsvToColor(secH, (pS * 0.5f).coerceAtMost(0.4f), secV)
    }
    val secHex = sec.toHsv()
    val onSecondary = contrastColor(secHex[2])
    val secondaryContainer = hsvToColor(secHex[0], (secHex[1] * 0.3f).coerceAtMost(0.2f), 0.92f)
    val onSecondaryContainer = hsvToColor(secHex[0], (secHex[1] * 0.5f).coerceAtMost(0.4f), 0.15f)
    // 使用用户提供的 tertiary 或自动生成
    val ter = tertiary ?: run {
        val terH = (pH + 60) % 360
        val terV = (pV * 0.85f).coerceIn(0.3f, 0.65f)
        hsvToColor(terH, (pS * 0.6f).coerceAtMost(0.5f), terV)
    }
    val terHex = ter.toHsv()
    val onTertiary = contrastColor(terHex[2])
    val tertiaryContainer = hsvToColor(terHex[0], (terHex[1] * 0.3f).coerceAtMost(0.25f), 0.92f)
    val onTertiaryContainer = hsvToColor(terHex[0], (terHex[1] * 0.5f).coerceAtMost(0.5f), 0.15f)
    val inversePrimary = hsvToColor(pH, pS, (pV * 0.75f))
    val lightScheme = buildLightScheme(
        primary, onPrimary, primaryContainer, onPrimaryContainer,
        sec, onSecondary, secondaryContainer, onSecondaryContainer,
        ter, onTertiary, tertiaryContainer, onTertiaryContainer,
        inversePrimary
    )
    // 如果用户提供了 error 颜色，覆盖默认的 error
    return if (error != null) {
        lightScheme.copy(
            error = error,
            onError = contrastColor(error.toHsv()[2])
        )
    } else {
        lightScheme
    }
}

// 基于用户选择的四种主辅颜色生成深色 ColorScheme
fun generateCustomDarkScheme(
    primary: Color,
    secondary: Color? = null,
    tertiary: Color? = null,
    error: Color? = null
): ColorScheme {
    val pHex = primary.toHsv()
    val pH = pHex[0]; val pS = pHex[1]; val pV = pHex[2]
    // 深色方案中 primary 使用高明度版本
    val darkPrimary = hsvToColor(pH, pS.coerceAtMost(0.7f), (pV * 0.5f + 0.45f).coerceAtMost(0.9f))
    val onPrimary = hsvToColor(pH, pS.coerceAtMost(0.6f), 0.18f)
    val primaryContainer = hsvToColor(pH, pS.coerceAtMost(0.7f), 0.3f)
    val onPrimaryContainer = hsvToColor(pH, (pS * 0.3f).coerceAtMost(0.3f), 0.92f)
    // 使用用户提供的 secondary 或自动生成
    val sec = secondary ?: run {
        val secH = (pH + 30) % 360
        hsvToColor(secH, (pS * 0.4f).coerceAtMost(0.35f), 0.8f)
    }
    val secHex = sec.toHsv()
    val onSecondary = hsvToColor(secHex[0], secHex[1].coerceAtMost(0.5f), 0.2f)
    val secondaryContainer = hsvToColor(secHex[0], secHex[1].coerceAtMost(0.5f), 0.3f)
    val onSecondaryContainer = hsvToColor(secHex[0], (secHex[1] * 0.3f).coerceAtMost(0.2f), 0.9f)
    // 使用用户提供的 tertiary 或自动生成
    val ter = tertiary ?: run {
        val terH = (pH + 60) % 360
        hsvToColor(terH, (pS * 0.5f).coerceAtMost(0.45f), 0.78f)
    }
    val terHex = ter.toHsv()
    val onTertiary = hsvToColor(terHex[0], terHex[1].coerceAtMost(0.5f), 0.2f)
    val tertiaryContainer = hsvToColor(terHex[0], terHex[1].coerceAtMost(0.5f), 0.3f)
    val onTertiaryContainer = hsvToColor(terHex[0], (terHex[1] * 0.3f).coerceAtMost(0.25f), 0.9f)
    val inversePrimary = hsvToColor(pH, pS, pV.coerceIn(0.3f, 0.6f))
    val darkScheme = buildDarkScheme(
        darkPrimary, onPrimary, primaryContainer, onPrimaryContainer,
        sec, onSecondary, secondaryContainer, onSecondaryContainer,
        ter, onTertiary, tertiaryContainer, onTertiaryContainer,
        inversePrimary
    )
    // 如果用户提供了 error 颜色，覆盖默认的 error
    return if (error != null) {
        val errHex = error.toHsv()
        darkScheme.copy(
            error = error,
            onError = contrastColor(errHex[2])
        )
    } else {
        darkScheme
    }
}

// 解析 ARGB hex 字符串为 Color（支持 "FF00696D" 或 "#FF00696D" 格式）
fun parseCustomColor(hex: String): Color? {
    if (hex.isBlank()) return null
    val cleaned = hex.removePrefix("#")
    return runCatching {
        Color(android.graphics.Color.parseColor("#$cleaned"))
    }.getOrNull()
}

// 将 Compose Color 转为 ARGB hex 字符串（如 "#FF00696D"）
fun colorToHex(color: Color): String {
    return String.format(java.util.Locale.US, "#%08X", color.toArgb())
}
