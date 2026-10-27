package com.baraka.game.memorymatch

import android.content.Context
import android.content.SharedPreferences

/** Persists the sound on/off preference — same SharedPreferences pattern as roadrush's RoadRushSettingsRepository. */
class MemoryMatchSettingsRepository private constructor(
    private val preferences: SharedPreferences
) {
    fun isSoundEnabled(): Boolean = preferences.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREF_FILE = "memory_match_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"

        @Volatile
        private var instance: MemoryMatchSettingsRepository? = null

        fun getInstance(context: Context): MemoryMatchSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: MemoryMatchSettingsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
