package com.baraka.game.numbercorridor.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.numbercorridor.model.AvatarDesign

/** Persists the chosen avatar and best score across runs — same SharedPreferences pattern as knightsurvivor's SettingsRepository. */
class CorridorSettingsRepository private constructor(
    private val preferences: SharedPreferences
) {
    fun avatarDesign(): AvatarDesign {
        val name = preferences.getString(KEY_AVATAR_DESIGN, null) ?: return AvatarDesign.RANGER
        return runCatching { AvatarDesign.valueOf(name) }.getOrDefault(AvatarDesign.RANGER)
    }

    fun setAvatarDesign(design: AvatarDesign) {
        preferences.edit().putString(KEY_AVATAR_DESIGN, design.name).apply()
    }

    fun bestScore(): Int = preferences.getInt(KEY_BEST_SCORE, 0)

    fun setBestScore(score: Int) {
        preferences.edit().putInt(KEY_BEST_SCORE, score).apply()
    }

    companion object {
        private const val PREF_FILE = "number_corridor_settings"
        private const val KEY_AVATAR_DESIGN = "avatar_design"
        private const val KEY_BEST_SCORE = "best_score"

        @Volatile
        private var instance: CorridorSettingsRepository? = null

        fun getInstance(context: Context): CorridorSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: CorridorSettingsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
