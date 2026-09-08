package com.baraka.game.knightsurvivor.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.knightsurvivor.model.Difficulty
import com.baraka.game.knightsurvivor.model.WeaponKind

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

    /** Which weapons the player has chosen as eligible to appear during a run. Defaults to every weapon so first-run behavior is unchanged. */
    fun weaponDeck(): Set<WeaponKind> {
        val saved = preferences.getStringSet(KEY_WEAPON_DECK, null) ?: return WeaponKind.entries.toSet()
        val deck = saved.mapNotNull { name -> runCatching { WeaponKind.valueOf(name) }.getOrNull() }.toSet()
        return deck.ifEmpty { WeaponKind.entries.toSet() }
    }

    fun setWeaponDeck(deck: Set<WeaponKind>) {
        preferences.edit().putStringSet(KEY_WEAPON_DECK, deck.map { it.name }.toSet()).apply()
    }

    companion object {
        private const val PREF_FILE = "knight_survivor_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_WEAPON_DECK = "weapon_deck"

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
