package com.par9uet.jm.ui.components

import kotlin.math.abs
import kotlin.math.sqrt

internal const val WATCH_FLOATING_IDLE_MILLIS = 5_000L
internal const val WATCH_FLOATING_IDLE_ALPHA = .42f

internal data class WatchPoint(val x: Float, val y: Float)
internal data class WatchEdgeTransform(val scale: Float = 1f, val alpha: Float = 1f)

/** Draw-only scaling: list measurements, line wrapping and scroll distances never change. */
internal fun watchEdgeTransform(
    itemCenter: Float,
    viewportStart: Float,
    viewportEnd: Float,
    enabled: Boolean,
): WatchEdgeTransform {
    val height = viewportEnd - viewportStart
    if (!enabled || !height.isFinite() || height <= 0f || !itemCenter.isFinite()) {
        return WatchEdgeTransform()
    }
    val distance = abs(itemCenter - (viewportStart + viewportEnd) / 2f) / (height / 2f)
    val edge = ((distance - .48f) / .52f).coerceIn(0f, 1f)
    val progress = edge * edge * (3f - 2f * edge)
    return WatchEdgeTransform(scale = 1f - .22f * progress, alpha = 1f - .32f * progress)
}

/** A compact title, not the full-width phone toolbar with icons in the round corners. */
internal fun watchCapsuleWidth(width: Float): Float =
    if (width.isFinite() && width > 0f) (width * .62f).coerceAtMost(144f) else 0f

internal fun watchCapsuleTop(width: Float, height: Float, capsuleWidth: Float, isRound: Boolean): Float {
    if (!isRound || width <= 0f || height <= 0f || !width.isFinite() || !height.isFinite()) return 4f
    // The top straight edge stops before the capsule's 13dp rounded ends.
    val topHalfWidth = (capsuleWidth / 2f - 10f).coerceAtLeast(0f)
    val x = (topHalfWidth / (width / 2f)).coerceIn(0f, .95f)
    return height / 2f * (1f - sqrt(1f - x * x)) + 3f
}

internal fun watchAppsColumns(width: Float, fontScale: Float): Int =
    if (width < 200f || fontScale > 1.3f) 2 else 3

/** Keep the entire circular 48dp touch target within a round/elliptical display. */
internal fun clampWatchFloatingPoint(
    point: WatchPoint,
    width: Float,
    height: Float,
    diameter: Float,
    isRound: Boolean,
    inset: Float = 2f,
): WatchPoint {
    if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return WatchPoint(0f, 0f)
    val cx = width / 2f
    val cy = height / 2f
    val rx = (cx - diameter / 2f - inset).coerceAtLeast(0f)
    val ry = (cy - diameter / 2f - inset).coerceAtLeast(0f)
    val px = point.x.takeIf { it.isFinite() } ?: cx
    val py = point.y.takeIf { it.isFinite() } ?: cy
    if (rx == 0f || ry == 0f) return WatchPoint(cx, cy)
    if (!isRound) return WatchPoint(px.coerceIn(cx - rx, cx + rx), py.coerceIn(cy - ry, cy + ry))
    val dx = (px - cx) / rx
    val dy = (py - cy) / ry
    val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    return WatchPoint(cx + dx / distance * rx, cy + dy / distance * ry)
}

/** Snap to the curved side rim, not the rectangular window edge or the top title. */
internal fun watchFloatingDockPoint(
    point: WatchPoint,
    width: Float,
    height: Float,
    diameter: Float,
    isRound: Boolean,
): WatchPoint {
    val safe = clampWatchFloatingPoint(point, width, height, diameter, isRound)
    val cx = width / 2f
    val cy = height / 2f
    val rx = (cx - diameter / 2f - 2f).coerceAtLeast(0f)
    val ry = (cy - diameter / 2f - 2f).coerceAtLeast(0f)
    if (rx == 0f || ry == 0f) return safe
    val y = safe.y.coerceIn(cy - ry * .48f, cy + ry * .48f)
    val normalizedY = (y - cy) / ry
    val reach = if (isRound) rx * sqrt((1f - normalizedY * normalizedY).coerceAtLeast(0f)) else rx
    return WatchPoint(cx + if (safe.x < cx) -reach else reach, y)
}

/** Short momentum projection on a small dial. High velocities cannot throw the ball offscreen. */
internal fun projectWatchFloatingPoint(point: WatchPoint, velocity: WatchPoint): WatchPoint =
    WatchPoint(
        point.x + velocity.x.coerceIn(-2_000f, 2_000f) * .10f,
        point.y + velocity.y.coerceIn(-2_000f, 2_000f) * .10f,
    )
