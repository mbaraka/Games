package com.baraka.game.clashdecks.data

import android.content.Context
import android.content.SharedPreferences

/** Persists the sound on/off preference — same SharedPreferences pattern as roadrush's RoadRushSettingsRepository / memorymatch's MemoryMatchSettingsRepository. */
class ClashDecksSettingsRepository private constructor(
    private val preferences: SharedPreferences
) {
    fun isSoundEnabled(): Boolean = preferences.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    /** What a two-phone co-op partner sees you as (DuelEngine.startDuel's playerName). Empty until the player picks one. */
    fun playerName(): String = preferences.getString(KEY_PLAYER_NAME, null).orEmpty()

    fun setPlayerName(name: String) {
        preferences.edit().putString(KEY_PLAYER_NAME, name).apply()
    }

    companion object {
        private const val PREF_FILE = "clash_decks_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_PLAYER_NAME = "player_name"

        @Volatile
        private var instance: ClashDecksSettingsRepository? = null

        fun getInstance(context: Context): ClashDecksSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: ClashDecksSettingsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
