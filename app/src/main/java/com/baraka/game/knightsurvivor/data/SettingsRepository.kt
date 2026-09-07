package com.baraka.game.knightsurvivor.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.knightsurvivor.model.Difficulty

/**
 * Persists the player's sound and difficulty preferences across app restarts, same
 * SharedPreferences pattern as [SurvivorStatsRepository]. Lives outside model/systems for
 * the same reason: those layers stay Android-free.
 *
 * Difficulty is read once when a new run starts (see ui/KnightSurvivorScreen.kt) and then
 * fixed for that run's [com.baraka.game.knightsurvivor.model.GameState.difficulty] — a
 * change here takes effect on the *next* run, not retroactively. Sound is checked live on
 * every play, so muting takes effect immediately.
 */
class SettingsRepository private constructor(
    private val preferences: SharedPreferences
) {

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
        private const val PREF_FILE = "knight_survivor_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_DIFFICULTY = "difficulty"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
