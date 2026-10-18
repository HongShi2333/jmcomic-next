package com.par9uet.jm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Real sp sizes: system font scaling stays enabled. Do not shrink a phone UI with graphicsLayer.
private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)
val AppTypography = Typography(
    displayLarge = style(24, 28), displayMedium = style(22, 26), displaySmall = style(20, 24),
    headlineLarge = style(20, 24), headlineMedium = style(18, 22), headlineSmall = style(16, 20, FontWeight.SemiBold),
    titleLarge = style(16, 20, FontWeight.SemiBold), titleMedium = style(14, 18, FontWeight.SemiBold), titleSmall = style(13, 17, FontWeight.SemiBold),
    bodyLarge = style(14, 19), bodyMedium = style(13, 18), bodySmall = style(12, 17),
    labelLarge = style(13, 17, FontWeight.Medium), labelMedium = style(12, 16), labelSmall = style(11, 15),
)
