package com.baraka.game.basedefender.data

import android.content.Context
import android.content.SharedPreferences

/** Immutable player settings. New fields need a default here and a line in [BdRepository] (old saves keep working). */
data class BdSettings(
    val vibration: Boolean = true,
    val screenShake: Boolean = true,
    val damageNumbers: Boolean = true,
    val autoAim: Boolean = true,
    val reducedEffects: Boolean = false,
    val showFps: Boolean = false
)

/** Settings plus the small amount of progress this game keeps, in the `base_defender` preferences file. */
class BdRepository private constructor(private val prefs: SharedPreferences) {
    fun settings(): BdSettings {
        val d = BdSettings()
        return BdSettings(
            vibration = prefs.getBoolean(K_VIBRATION, d.vibration),
            screenShake = prefs.getBoolean(K_SHAKE, d.screenShake),
            damageNumbers = prefs.getBoolean(K_NUMBERS, d.damageNumbers),
            autoAim = prefs.getBoolean(K_AUTO_AIM, d.autoAim),
            reducedEffects = prefs.getBoolean(K_REDUCED, d.reducedEffects),
            showFps = prefs.getBoolean(K_FPS, d.showFps)
        )
    }

    fun save(s: BdSettings) {
        prefs.edit()
            .putBoolean(K_VIBRATION, s.vibration)
            .putBoolean(K_SHAKE, s.screenShake)
            .putBoolean(K_NUMBERS, s.damageNumbers)
            .putBoolean(K_AUTO_AIM, s.autoAim)
            .putBoolean(K_REDUCED, s.reducedEffects)
            .putBoolean(K_FPS, s.showFps)
            .apply()
    }

    fun bestScore(): Int = prefs.getInt(K_BEST_SCORE, 0)
    fun bestWave(): Int = prefs.getInt(K_BEST_WAVE, 0)

    /** Stores the run's result if it beats the record. */
    fun recordRun(score: Int, wave: Int) {
        val e = prefs.edit()
        if (score > bestScore()) e.putInt(K_BEST_SCORE, score)
        if (wave > bestWave()) e.putInt(K_BEST_WAVE, wave)
        e.apply()
    }

    fun resetProgress() {
        prefs.edit().remove(K_BEST_SCORE).remove(K_BEST_WAVE).apply()
    }

    companion object {
        private const val FILE = "base_defender"
        private const val K_VIBRATION = "vibration"
        private const val K_SHAKE = "screen_shake"
        private const val K_NUMBERS = "damage_numbers"
        private const val K_AUTO_AIM = "auto_aim"
        private const val K_REDUCED = "reduced_effects"
        private const val K_FPS = "show_fps"
        private const val K_BEST_SCORE = "best_score"
        private const val K_BEST_WAVE = "best_wave"

        @Volatile
        private var instance: BdRepository? = null

        fun get(context: Context): BdRepository = instance ?: synchronized(this) {
            instance ?: BdRepository(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)).also { instance = it }
        }
    }
}
