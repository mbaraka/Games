package com.baraka.game.knightsurvivor.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.knightsurvivor.model.Achievement

/**
 * Persists which [Achievement]s have ever been earned, across every run and app restart —
 * once unlocked, always unlocked. Same SharedPreferences pattern as the other repositories
 * in this package; lives here rather than model/systems for the same reason they do.
 */
class AchievementRepository private constructor(
    private val preferences: SharedPreferences
) {

    fun unlockedAchievements(): Set<Achievement> =
        preferences.getStringSet(KEY_UNLOCKED, emptySet())
            .orEmpty()
            .mapNotNull { name -> runCatching { Achievement.valueOf(name) }.getOrNull() }
            .toSet()

    /** Adds [achievements] to the unlocked set — a no-op for any already unlocked. */
    fun unlock(achievements: Set<Achievement>) {
        if (achievements.isEmpty()) return
        val updated = unlockedAchievements() + achievements
        preferences.edit().putStringSet(KEY_UNLOCKED, updated.map { it.name }.toSet()).apply()
    }

    companion object {
        private const val PREF_FILE = "knight_survivor_achievements"
        private const val KEY_UNLOCKED = "unlocked"

        @Volatile
        private var instance: AchievementRepository? = null

        fun getInstance(context: Context): AchievementRepository {
            return instance ?: synchronized(this) {
                instance ?: AchievementRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
