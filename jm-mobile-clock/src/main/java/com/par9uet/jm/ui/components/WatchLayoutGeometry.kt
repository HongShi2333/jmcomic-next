package com.par9uet.jm.ui.components

import kotlin.math.sqrt

internal data class WatchViewportInsets(val horizontal: Float, val vertical: Float)

/** Stable width: never remeasure text or change a button's width as it scrolls. */
internal fun calculateWatchViewportInsets(widthDp: Float, heightDp: Float, isRound: Boolean): WatchViewportInsets {
    val horizontal = if (widthDp.isFinite() && widthDp > 0f) minOf(if (isRound) 14f else 10f, widthDp / 2f) else 0f
    return WatchViewportInsets(horizontal, 0f)
}

internal fun watchListStartPadding(height: Float, isRound: Boolean, hasTopBar: Boolean): Float =
    if (!hasTopBar && isRound && height.isFinite() && height > 0f) (height * .08f).coerceIn(8f, 20f) else 4f

/** Search/menu are always part of the home list, including the larger watch sizes. */
internal fun watchHomeUsesScrollableHeader(widthDp: Float, heightDp: Float, isRound: Boolean, fontScale: Float): Boolean = true

/** The top row is a fixed touch band, not an item whose width changes on scroll. */
internal fun watchTopBarInsets(widthDp: Float, heightDp: Float, isRound: Boolean): WatchViewportInsets {
    if (!isRound) return WatchViewportInsets(8f, 0f)
    if (!widthDp.isFinite() || !heightDp.isFinite() || widthDp <= 0 || heightDp <= 0) return WatchViewportInsets(0f, 0f)
    val horizontal = (widthDp * .10f).coerceIn(10f, 22f)
    val radiusX = widthDp / 2f
    val radiusY = heightDp / 2f
    val iconCornerX = horizontal + 12f
    val normalizedX = ((radiusX - iconCornerX) / radiusX).coerceIn(-1f, 1f)
    val vertical = (radiusY - 12f - radiusY * sqrt(1f - normalizedX * normalizedX)).coerceAtLeast(0f)
    return WatchViewportInsets(horizontal, vertical + 1f)
}
