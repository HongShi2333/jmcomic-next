package com.par9uet.jm.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: String
) {
    val formatted: String get() = "[$timestamp][$level][$tag] $message"
}

object LogBuffer {
    private const val MAX_ENTRIES = 500
    private val entries = mutableListOf<LogEntry>()
    // 使用 SimpleDateFormat 替代 java.time，确保 Android 6 兼容性
    private val dateFormatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun append(tag: String, message: String, level: String = "D") {
        val time = dateFormatter.format(Date())
        entries.add(LogEntry(time, tag, message, level))
        if (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
    }

    @Synchronized
    fun appendError(tag: String, message: String) {
        append(tag, message, "E")
    }

    @Synchronized
    fun getLogs(): List<LogEntry> {
        return entries.toList()
    }

    @Synchronized
    fun getLogText(): String {
        return entries.joinToString("\n") { it.formatted }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
