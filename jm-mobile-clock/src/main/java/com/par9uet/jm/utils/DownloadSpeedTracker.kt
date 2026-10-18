package com.par9uet.jm.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 下载速度跟踪器，按 groupId 跟踪下载速度。
 * Worker 在每页下载完成后更新字节数，UI 通过 StateFlow 观察实时速度。
 */
object DownloadSpeedTracker {
    private data class SpeedSample(
        val totalBytes: Long,
        val startTimeMs: Long,
    )

    private val _speedByGroup = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val speedByGroup: StateFlow<Map<Int, Float>> = _speedByGroup.asStateFlow()

    private val samplesByGroup = mutableMapOf<Int, SpeedSample>()

    /**
     * 开始跟踪一个下载任务的速度
     */
    fun startTracking(groupId: Int) {
        samplesByGroup[groupId] = SpeedSample(
            totalBytes = 0L,
            startTimeMs = System.currentTimeMillis()
        )
        _speedByGroup.value = _speedByGroup.value + (groupId to 0f)
    }

    /**
     * 增加已下载字节数，并更新速度（bytes/s）
     */
    fun addBytes(groupId: Int, bytes: Long) {
        val sample = samplesByGroup[groupId] ?: return
        val newTotal = sample.totalBytes + bytes
        val elapsedSec = (System.currentTimeMillis() - sample.startTimeMs) / 1000.0
        val speed = if (elapsedSec > 0) (newTotal / elapsedSec).toFloat() else 0f
        samplesByGroup[groupId] = sample.copy(totalBytes = newTotal)
        _speedByGroup.value = _speedByGroup.value + (groupId to speed)
    }

    /**
     * 停止跟踪，移除速度数据
     */
    fun stopTracking(groupId: Int) {
        samplesByGroup.remove(groupId)
        _speedByGroup.value = _speedByGroup.value - groupId
    }

    /**
     * 获取当前速度（bytes/s）
     */
    fun getSpeed(groupId: Int): Float {
        return _speedByGroup.value[groupId] ?: 0f
    }
}
