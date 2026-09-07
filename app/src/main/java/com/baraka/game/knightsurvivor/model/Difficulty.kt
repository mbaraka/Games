package com.baraka.game.knightsurvivor.model

/**
 * A run-wide difficulty preset. Applied as a flat multiplier on top of
 * [com.baraka.game.knightsurvivor.balance.GameTunables]'s own time-based scaling curves —
 * those curves stay the single source of truth for "how hard does a NORMAL run get",
 * and difficulty only stretches or compresses that curve, rather than duplicating it.
 */
enum class Difficulty(
    val displayName: String,
    val enemyPowerMultiplier: Float,
    val enemySpeedMultiplier: Float,
    /** Multiplies the base spawn interval — above 1 means enemies arrive more slowly (easier), below 1 means more often (harder). */
    val spawnIntervalMultiplier: Float
) {
    EASY(displayName = "Easy", enemyPowerMultiplier = 0.75f, enemySpeedMultiplier = 0.9f, spawnIntervalMultiplier = 1.25f),
    NORMAL(displayName = "Normal", enemyPowerMultiplier = 1f, enemySpeedMultiplier = 1f, spawnIntervalMultiplier = 1f),
    HARD(displayName = "Hard", enemyPowerMultiplier = 1.35f, enemySpeedMultiplier = 1.15f, spawnIntervalMultiplier = 0.8f)
}
