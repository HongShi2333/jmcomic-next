package com.par9uet.jm.ui.screens.readScreen

import kotlin.math.sqrt

internal data class ReaderImageTransform(val scale: Float = 1f, val left: Float = 0f, val top: Float = 0f)

/** The bounds belong to the displayed IMAGE, not to the surrounding pager/list. */
internal data class ReaderImageGeometry(val width: Float, val height: Float, val aspectRatio: Float, val round: Boolean = false) {
    val valid get() = width.isFinite() && height.isFinite() && aspectRatio.isFinite() && width > 0 && height > 0 && aspectRatio > 0
    fun imageHeight(scale: Float) = if (valid) width / aspectRatio * scale else 0f
    // A round display must contain all four image corners, not just fit its bounding square.
    val fitPageScale: Float get() {
        if (!valid) return 1f
        val ratio = (width / aspectRatio / height).toDouble()
        return (if (round) 1.0 / sqrt(1.0 + ratio * ratio) else minOf(1.0, 1.0 / ratio))
            .toFloat().coerceIn(.001f, 1f)
    }
    fun clamp(value: ReaderImageTransform): ReaderImageTransform {
        if (!valid) return ReaderImageTransform()
        val scale = if (value.scale.isFinite() && value.scale > 0f) value.scale.coerceIn(fitPageScale, 4f) else 1f
        val w = width * scale; val h = imageHeight(scale)
        fun bound(position: Float, frame: Float, content: Float): Float {
            val edgeRoom = if (round && scale > 1.01f) frame * .25f else 0f
            if (content <= frame) {
                val center = (frame - content) / 2
                return if (edgeRoom > 0) (if (position.isFinite()) position else center).coerceIn(center - edgeRoom, center + edgeRoom) else center
            }
            return (if (position.isFinite()) position else 0f).coerceIn(frame - content - edgeRoom, edgeRoom)
        }
        return ReaderImageTransform(scale, bound(value.left, width, w), bound(value.top, height, h))
    }
    fun transform(current: ReaderImageTransform, zoom: Float, panX: Float, panY: Float, anchorX: Float, anchorY: Float): ReaderImageTransform {
        val old = clamp(current)
        val next = if (zoom.isFinite() && zoom > 0) (old.scale * zoom).coerceIn(fitPageScale, 4f) else old.scale
        val ratio = next / old.scale
        val x = if (anchorX.isFinite()) anchorX else width / 2
        val y = if (anchorY.isFinite()) anchorY else height / 2
        return clamp(ReaderImageTransform(next,
            x - (x - old.left) * ratio + if (panX.isFinite()) panX else 0f,
            y - (y - old.top) * ratio + if (panY.isFinite()) panY else 0f))
    }
}

internal data class ReaderVisiblePage(val index: Int, val top: Int, val height: Int)

/** Focus the page occupying the middle of the dial, rather than a clipped remnant at its top. */
internal fun readerPreviewPage(pages: List<ReaderVisiblePage>, viewportStart: Int, viewportEnd: Int): ReaderVisiblePage? {
    val center = (viewportStart + viewportEnd) / 2f
    return pages.firstOrNull { center >= it.top && center < it.top + it.height }
        ?: pages.maxByOrNull { (minOf(it.top + it.height, viewportEnd) - maxOf(it.top, viewportStart)).coerceAtLeast(0) }
}

/** Three 48dp round hit targets, 4dp apart, tucked below the image center when space permits. */
internal fun readerControlsCenterY(width: Float, height: Float): Float {
    val rx = (width / 2f - 26f).coerceAtLeast(1f)
    val ry = (height / 2f - 26f).coerceAtLeast(1f)
    val side = (52f / rx).coerceIn(0f, 1f)
    val available = (ry * sqrt(1f - side * side) - 2f).coerceAtLeast(0f)
    return height / 2f + minOf(height * .22f, available)
}
