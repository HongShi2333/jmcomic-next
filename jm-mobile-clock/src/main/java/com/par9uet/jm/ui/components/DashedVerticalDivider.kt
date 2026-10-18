package com.par9uet.jm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DashedVerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    dashLength: Dp = 5.dp,
    gapLength: Dp = 3.dp,
    thickness: Dp = 1.dp
) {
    val density = LocalDensity.current
    val dashLengthPx = with(density) { dashLength.toPx() }
    val gapLengthPx = with(density) { gapLength.toPx() }
    val thicknessPx = with(density) { thickness.toPx() }

    Canvas(
        modifier = modifier
            .width(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(size.width, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = thicknessPx,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashLengthPx, gapLengthPx),
                phase = 0f
            )
        )
    }
}
