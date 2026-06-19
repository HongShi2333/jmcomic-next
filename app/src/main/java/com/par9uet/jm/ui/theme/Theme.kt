package com.par9uet.jm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val contentTag: ColorFamily,
    val roleTag: ColorFamily,
    val workTag: ColorFamily,
)

val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

val extendedLight = ExtendedColorScheme(
    contentTag = ColorFamily(
        contentTagLight,
        onContentTagLight,
        contentTagContainerLight,
        onContentTagContainerLight,
    ),
    roleTag = ColorFamily(
        roleTagLight,
        onRoleTagLight,
        roleTagContainerLight,
        onRoleTagContainerLight,
    ),
    workTag = ColorFamily(
        workTagLight,
        onWorkTagLight,
        workTagContainerLight,
        onWorkTagContainerLight,
    ),
)

val extendedDark = ExtendedColorScheme(
    contentTag = ColorFamily(
        contentTagDark,
        onContentTagDark,
        contentTagContainerDark,
        onContentTagContainerDark,
    ),
    roleTag = ColorFamily(
        roleTagDark,
        onRoleTagDark,
        roleTagContainerDark,
        onRoleTagContainerDark,
    ),
    workTag = ColorFamily(
        workTagDark,
        onWorkTagDark,
        workTagContainerDark,
        onWorkTagContainerDark,
    ),
)

fun extendedColorSchemeFor(colorScheme: ColorScheme, isDark: Boolean): ExtendedColorScheme {
    val fallback = if (isDark) extendedDark else extendedLight
    return ExtendedColorScheme(
        contentTag = fallback.contentTag.copy(
            color = colorScheme.primary,
            colorContainer = colorScheme.primaryContainer,
            onColorContainer = colorScheme.onPrimaryContainer,
        ),
        roleTag = fallback.roleTag.copy(
            color = colorScheme.tertiary,
            colorContainer = colorScheme.tertiaryContainer,
            onColorContainer = colorScheme.onTertiaryContainer,
        ),
        workTag = fallback.workTag.copy(
            color = colorScheme.secondary,
            colorContainer = colorScheme.secondaryContainer,
            onColorContainer = colorScheme.onSecondaryContainer,
        ),
    )
}

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)
