package com.baraka.game.minesweeper.data

import android.content.Context
import android.content.SharedPreferences

/** Every player-facing option of Minesweeper. Applied immediately and persisted by [MineSettingsRepository]. */
data class MineSettings(
    val vibration: Boolean = true,
    val showFps: Boolean = false,
    val longPressFlags: Boolean = true
)

class MineSettingsRepository private constructor(private val prefs: SharedPreferences) {
    fun load() = MineSettings(
        vibration = prefs.getBoolean(KEY_VIBRATION, true),
        showFps = prefs.getBoolean(KEY_FPS, false),
        longPressFlags = prefs.getBoolean(KEY_LONG, true)
    )

    fun save(s: MineSettings) {
        prefs.edit().putBoolean(KEY_VIBRATION, s.vibration).putBoolean(KEY_FPS, s.showFps).putBoolean(KEY_LONG, s.longPressFlags).apply()
    }

    companion object {
        private const val FILE = "minesweeper_settings"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_FPS = "show_fps"
        private const val KEY_LONG = "long_press_flags"

        @Volatile private var instance: MineSettingsRepository? = null

        fun getInstance(context: Context): MineSettingsRepository = instance ?: synchronized(this) {
            instance ?: MineSettingsRepository(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)).also { instance = it }
        }
    }
}
