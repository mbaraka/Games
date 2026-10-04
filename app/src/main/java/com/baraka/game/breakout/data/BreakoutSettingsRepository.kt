package com.baraka.game.breakout.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.breakout.model.Difficulty

/** Persists best score, sound preference and last-chosen difficulty — same SharedPreferences pattern as roadrush's RoadRushSettingsRepository. */
class BreakoutSettingsRepository private constructor(
    private val preferences: SharedPreferences
) {
    fun bestScore(): Int = preferences.getInt(KEY_BEST_SCORE, 0)

    fun setBestScore(score: Int) {
        preferences.edit().putInt(KEY_BEST_SCORE, score).apply()
    }

    fun isSoundEnabled(): Boolean = preferences.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun difficulty(): Difficulty {
        val name = preferences.getString(KEY_DIFFICULTY, null) ?: return Difficulty.NORMAL
        return runCatching { Difficulty.valueOf(name) }.getOrDefault(Difficulty.NORMAL)
    }

    fun setDifficulty(difficulty: Difficulty) {
        preferences.edit().putString(KEY_DIFFICULTY, difficulty.name).apply()
    }

    companion object {
        private const val PREF_FILE = "breakout_settings"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_DIFFICULTY = "difficulty"

        @Volatile
        private var instance: BreakoutSettingsRepository? = null

        fun getInstance(context: Context): BreakoutSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: BreakoutSettingsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
