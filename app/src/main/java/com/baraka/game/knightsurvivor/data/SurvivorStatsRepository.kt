package com.baraka.game.knightsurvivor.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists best-run stats across app restarts. Every other game in this app is
 * session-only; Knight Survivor is the first to remember progress, so this stays a small,
 * self-contained repository rather than pulling other games toward it.
 *
 * Lives outside model/systems on purpose: those layers stay Android-free, and reading
 * from/writing to SharedPreferences is a UI-layer side effect performed by
 * ui/KnightSurvivorScreen.kt (load once at run start, save once a run ends) — never from
 * inside systems/GameLoopOrchestrator.kt's pure `tick()`.
 */
class SurvivorStatsRepository private constructor(
    private val preferences: SharedPreferences
) {

    fun bestSurvivalTimeMs(): Long = preferences.getLong(KEY_BEST_SURVIVAL_TIME_MS, 0L)

    fun bestLevelReached(): Int = preferences.getInt(KEY_BEST_LEVEL, 1)

    fun totalKillsAllTime(): Long = preferences.getLong(KEY_TOTAL_KILLS, 0L)

    /** Called once a run concludes (game over or victory); folds this run's results into the persisted bests/totals. */
    fun recordRunResult(survivalTimeMs: Long, levelReached: Int, killsThisRun: Int) {
        preferences.edit()
            .putLong(KEY_BEST_SURVIVAL_TIME_MS, maxOf(bestSurvivalTimeMs(), survivalTimeMs))
            .putInt(KEY_BEST_LEVEL, maxOf(bestLevelReached(), levelReached))
            .putLong(KEY_TOTAL_KILLS, totalKillsAllTime() + killsThisRun)
            .apply()
    }

    companion object {
        private const val PREF_FILE = "knight_survivor_prefs"
        private const val KEY_BEST_SURVIVAL_TIME_MS = "best_survival_time_ms"
        private const val KEY_BEST_LEVEL = "best_level"
        private const val KEY_TOTAL_KILLS = "total_kills"

        @Volatile
        private var instance: SurvivorStatsRepository? = null

        fun getInstance(context: Context): SurvivorStatsRepository {
            return instance ?: synchronized(this) {
                instance ?: SurvivorStatsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
