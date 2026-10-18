package com.par9uet.jm.store

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.par9uet.jm.utils.GravityVector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class GravitySample(val vector: GravityVector, val timestampNanos: Long)

class GravityReadingManager(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val available get() = sensor != null
    private val _neutral = MutableStateFlow<GravityVector?>(null)
    val neutral = _neutral.asStateFlow()
    private val _calibrating = MutableStateFlow(false)
    val calibrating = _calibrating.asStateFlow()
    @Volatile var pauseUntilMillis = 0L
        private set

    fun setCalibrating(value: Boolean) { _calibrating.value = value; pause() }
    fun calibrate(vector: GravityVector) { _neutral.value = vector.normalized(); pause(1_000L) }
    fun pause(durationMillis: Long = 1_200L) { pauseUntilMillis = SystemClock.elapsedRealtime() + durationMillis }

    // Registration is scoped to the consumer (visible reader or calibration dialog).
    val samples = callbackFlow {
        val target = sensor
        if (target == null || manager == null) { close(); return@callbackFlow }
        var filtered: GravityVector? = null
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent) {
                val raw = GravityVector(event.values[0], event.values[1], event.values[2])
                val old = filtered
                val value = if (target.type == Sensor.TYPE_ACCELEROMETER && old != null) {
                    GravityVector(old.x * .85f + raw.x * .15f, old.y * .85f + raw.y * .15f, old.z * .85f + raw.z * .15f)
                } else raw
                filtered = value
                value.normalized()?.let { trySend(GravitySample(it, event.timestamp)) }
            }
        }
        val registered = manager.registerListener(listener, target, 50_000)
        if (!registered) close()
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()
}
