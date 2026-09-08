package com.baraka.game.knightsurvivor.model

/** A run-wide preset, layered on top of GameTunables' time-based curves. All tuning lives here — change a number, rebuild. */
enum class Difficulty(
    val displayName: String,
    val enemyPowerMultiplier: Float,
    val enemySpeedMultiplier: Float,
    val spawnIntervalMultiplier: Float, // >1 = slower spawns (easier), <1 = faster (harder)
    val playerMaxHpMultiplier: Float
) {
    EASY(displayName = "Easy", enemyPowerMultiplier = 0.5f, enemySpeedMultiplier = 0.8f, spawnIntervalMultiplier = 1.6f, playerMaxHpMultiplier = 1.3f),
    NORMAL(displayName = "Normal", enemyPowerMultiplier = 1f, enemySpeedMultiplier = 1f, spawnIntervalMultiplier = 1f, playerMaxHpMultiplier = 1f),
    HARD(displayName = "Hard", enemyPowerMultiplier = 1.35f, enemySpeedMultiplier = 1.15f, spawnIntervalMultiplier = 0.8f, playerMaxHpMultiplier = 1f)
}
