package com.par9uet.jm.utils

import kotlin.math.*

data class GravityVector(val x: Float, val y: Float, val z: Float) {
    fun normalized(): GravityVector? {
        val length = sqrt(x * x + y * y + z * z)
        return if (length.isFinite() && length > .001f) GravityVector(x / length, y / length, z / length) else null
    }
}

fun gravityAngleDegrees(a: GravityVector, b: GravityVector): Float {
    val p = a.normalized() ?: return 180f
    val q = b.normalized() ?: return 180f
    return Math.toDegrees(acos((p.x * q.x + p.y * q.y + p.z * q.z).coerceIn(-1f, 1f)).toDouble()).toFloat()
}

/** Signed screen-forward tilt; wrap across +/-180 instead of suddenly turning backwards. */
fun gravityTiltDegrees(current: GravityVector, neutral: GravityVector, rotation: Int = 0): Float {
    fun forward(v: GravityVector): Float = when (rotation) {
        1 -> -v.x // ROTATION_90
        2 -> -v.y
        3 -> v.x
        else -> v.y
    }
    val a = current.normalized() ?: return 0f
    val b = neutral.normalized() ?: return 0f
    val ay = forward(a); val by = forward(b)
    if (hypot(ay, a.z) < .20f || hypot(by, b.z) < .20f) return 0f
    val degrees = Math.toDegrees((atan2(ay, a.z) - atan2(by, b.z)).toDouble()).toFloat()
    return (degrees + 540f) % 360f - 180f
}

/** Dead band holds the page still; speed increases continuously with tilt, never above one. */
fun gravityReadingSpeed(tiltDegrees: Float, sensitivity: Float): Float {
    if (!tiltDegrees.isFinite()) return 0f
    val gain = if (sensitivity.isFinite()) sensitivity.coerceIn(.5f, 2.5f) else 1f
    val amount = ((abs(tiltDegrees) * gain - 4f) / 26f).coerceIn(0f, 1f)
    return sign(tiltDegrees) * amount * amount
}

/** Uses event timestamps: a frozen/missing sensor stream can never finish calibration. */
class GravityStabilityTracker {
    private var anchor: GravityVector? = null
    private var startedNanos = 0L
    private var lastNanos = 0L
    var stableMillis = 0L
        private set

    fun sample(vector: GravityVector, timestampNanos: Long): GravityVector? {
        val normalized = vector.normalized() ?: return null
        val previous = anchor
        if (previous == null || timestampNanos <= lastNanos || timestampNanos - lastNanos > 400_000_000L ||
            gravityAngleDegrees(previous, normalized) > 2f) {
            anchor = normalized
            startedNanos = timestampNanos
        }
        lastNanos = timestampNanos
        stableMillis = ((timestampNanos - startedNanos) / 1_000_000L).coerceAtLeast(0L)
        return if (stableMillis >= 3_000L) normalized else null
    }
}
