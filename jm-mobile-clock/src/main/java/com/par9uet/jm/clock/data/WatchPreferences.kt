package com.par9uet.jm.clock.data

import android.content.Context

class WatchPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): WatchUiPreferences = WatchUiPreferences(
        themeMode = WatchThemeMode.fromStorage(preferences.getString(KEY_THEME, null)),
        uiScale = preferences.getFloat(KEY_UI_SCALE, 1f).coerceIn(MIN_SCALE, MAX_SCALE),
        appLockEnabled = preferences.getBoolean(KEY_APP_LOCK_ENABLED, false),
        appLockPinLength = preferences.getInt(KEY_APP_LOCK_PIN_LENGTH, 4)
            .takeIf { it in APP_LOCK_PIN_LENGTHS }
            ?: 4,
        clipboardAutoDetectEnabled = preferences.getBoolean(KEY_CLIPBOARD_AUTO_DETECT_ENABLED, false),
    )

    fun save(value: WatchUiPreferences) {
        preferences.edit()
            .putString(KEY_THEME, value.themeMode.storageValue)
            .putFloat(KEY_UI_SCALE, value.uiScale.coerceIn(MIN_SCALE, MAX_SCALE))
            .putBoolean(KEY_APP_LOCK_ENABLED, value.appLockEnabled)
            .putInt(
                KEY_APP_LOCK_PIN_LENGTH,
                value.appLockPinLength.takeIf { it in APP_LOCK_PIN_LENGTHS } ?: 4,
            )
            .putBoolean(KEY_CLIPBOARD_AUTO_DETECT_ENABLED, value.clipboardAutoDetectEnabled)
            .apply()
    }

    fun readerPosition(chapterId: Int): Int =
        preferences.getInt("$KEY_READER_POSITION_PREFIX$chapterId", 0).coerceAtLeast(0)

    fun saveReaderPosition(chapterId: Int, pageIndex: Int) {
        if (chapterId <= 0) return
        preferences.edit()
            .putInt("$KEY_READER_POSITION_PREFIX$chapterId", pageIndex.coerceAtLeast(0))
            .apply()
    }

    private companion object {
        const val FILE_NAME = "jm_mobile_clock_preferences"
        const val KEY_THEME = "theme"
        const val KEY_UI_SCALE = "ui_scale"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_PIN_LENGTH = "app_lock_pin_length"
        const val KEY_CLIPBOARD_AUTO_DETECT_ENABLED = "clipboard_auto_detect_enabled"
        const val KEY_READER_POSITION_PREFIX = "reader_position_"
        const val MIN_SCALE = 0.85f
        const val MAX_SCALE = 1.15f
        val APP_LOCK_PIN_LENGTHS = setOf(4, 6)
    }
}
