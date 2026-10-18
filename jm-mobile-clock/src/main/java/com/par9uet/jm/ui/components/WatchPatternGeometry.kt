package com.par9uet.jm.ui.components

import kotlin.math.hypot

internal data class WatchPatternPoint(val x: Float, val y: Float)

internal fun watchPatternCenters(width: Float, height: Float): List<WatchPatternPoint> =
    listOf(.17f, .50f, .83f).flatMap { y ->
        listOf(.17f, .50f, .83f).map { x -> WatchPatternPoint(width * x, height * y) }
    }

/** Do not connect a nearby point just because a finger travels between two dots. */
internal fun watchPatternHit(x: Float, y: Float, width: Float, height: Float): Int? {
    if (width <= 0 || height <= 0 || !x.isFinite() || !y.isFinite()) return null
    val radius = minOf(width, height) * .06f
    return watchPatternCenters(width, height).indexOfFirst { hypot(x - it.x, y - it.y) <= radius }.takeIf { it >= 0 }
}
