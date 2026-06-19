package com.par9uet.jm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun AppTheme(
    localSettingManager: LocalSettingManager = getKoin().get(),
    content: @Composable () -> Unit
) {
    val localSettingState = localSettingManager.localSettingState.collectAsState()
    val theme by remember {
        derivedStateOf {
            localSettingState.value.theme
        }
    }
    val context = LocalContext.current
    val isDark = when (theme) {
        "auto" -> isSystemInDarkTheme()
        "dark" -> true
        else -> false
    }
    val colorScheme = when (theme) {
        "auto" -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
            isDark -> darkScheme
            else -> lightScheme
        }
        "light" -> lightScheme
        "dark" -> darkScheme
        else -> lightScheme
    }
    val extendedColorScheme = extendedColorSchemeFor(colorScheme, isDark)

    CompositionLocalProvider(LocalExtendedColors provides extendedColorScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
