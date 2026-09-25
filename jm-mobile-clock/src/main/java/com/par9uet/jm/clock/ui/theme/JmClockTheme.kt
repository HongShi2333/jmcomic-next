package com.par9uet.jm.clock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.par9uet.jm.clock.data.WatchThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7AA8),
    onPrimary = Color(0xFF4B0828),
    primaryContainer = Color(0xFF68113B),
    onPrimaryContainer = Color(0xFFFFD8E5),
    secondary = Color(0xFFC9B9FF),
    onSecondary = Color(0xFF30215F),
    secondaryContainer = Color(0xFF463879),
    onSecondaryContainer = Color(0xFFE7DEFF),
    background = Color.Black,
    onBackground = Color(0xFFF7F1F4),
    surface = Color(0xFF0D0C0E),
    onSurface = Color(0xFFF7F1F4),
    surfaceVariant = Color(0xFF2A2529),
    onSurfaceVariant = Color(0xFFD6C2CA),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF9B2759),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E5),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF65558F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DDFF),
    onSecondaryContainer = Color(0xFF201047),
    background = Color(0xFFFFF8FA),
    onBackground = Color(0xFF211A1D),
    surface = Color(0xFFFFF8FA),
    onSurface = Color(0xFF211A1D),
    surfaceVariant = Color(0xFFF1DEE5),
    onSurfaceVariant = Color(0xFF514348),
)

private val WatchTypography = Typography(
    headlineSmall = TextStyle(fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    bodyMedium = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium),
)

private val WatchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun JmClockTheme(
    mode: WatchThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        WatchThemeMode.System -> isSystemInDarkTheme()
        WatchThemeMode.Dark -> true
        WatchThemeMode.Light -> false
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = WatchTypography,
        shapes = WatchShapes,
        content = content,
    )
}
