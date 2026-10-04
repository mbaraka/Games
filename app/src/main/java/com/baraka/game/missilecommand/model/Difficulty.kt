package com.baraka.game.missilecommand.model

/** A run-wide preset — same shape as breakout's and spaceinvaders' Difficulty. All tuning lives here. */
enum class Difficulty(
    val displayName: String,
    val missileSpeedMultiplier: Float,
    val spawnRateMultiplier: Float,
    val startingAmmoBonus: Int
) {
    EASY("Easy", missileSpeedMultiplier = 0.8f, spawnRateMultiplier = 0.75f, startingAmmoBonus = 6),
    NORMAL("Normal", missileSpeedMultiplier = 1f, spawnRateMultiplier = 1f, startingAmmoBonus = 0),
    HARD("Hard", missileSpeedMultiplier = 1.25f, spawnRateMultiplier = 1.35f, startingAmmoBonus = -3)
}
