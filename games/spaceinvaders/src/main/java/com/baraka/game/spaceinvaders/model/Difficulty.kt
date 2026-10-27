package com.baraka.game.spaceinvaders.model

/** A run-wide preset — same shape as breakout's and roadrush's Difficulty. All tuning lives here. */
enum class Difficulty(
    val displayName: String,
    val alienSpeedMultiplier: Float,
    val alienFireRateMultiplier: Float,
    val startingLives: Int
) {
    EASY("Easy", alienSpeedMultiplier = 0.8f, alienFireRateMultiplier = 0.7f, startingLives = 4),
    NORMAL("Normal", alienSpeedMultiplier = 1f, alienFireRateMultiplier = 1f, startingLives = 3),
    HARD("Hard", alienSpeedMultiplier = 1.25f, alienFireRateMultiplier = 1.4f, startingLives = 2)
}
