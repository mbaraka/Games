package com.baraka.game.roadrush.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.roadrush.model.CarDesign
import com.baraka.game.roadrush.model.Difficulty

/** Persists the chosen car design and best score across runs — same SharedPreferences pattern as numbercorridor's CorridorSettingsRepository. */
class RoadRushSettingsRepository private constructor(
    private val preferences: SharedPreferences
) {
    fun carDesign(): CarDesign {
        val name = preferences.getString(KEY_CAR_DESIGN, null) ?: return CarDesign.CRIMSON
        return runCatching { CarDesign.valueOf(name) }.getOrDefault(CarDesign.CRIMSON)
    }

    fun setCarDesign(design: CarDesign) {
        preferences.edit().putString(KEY_CAR_DESIGN, design.name).apply()
    }

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
        private const val PREF_FILE = "road_rush_settings"
        private const val KEY_CAR_DESIGN = "car_design"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_DIFFICULTY = "difficulty"

        @Volatile
        private var instance: RoadRushSettingsRepository? = null

        fun getInstance(context: Context): RoadRushSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: RoadRushSettingsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
