package com.baraka.game.breakout.model

/** A run-wide preset — same shape as roadrush's Difficulty. All tuning lives here. */
enum class Difficulty(val displayName: String, val ballSpeedMultiplier: Float, val startingLives: Int) {
    EASY("Easy", ballSpeedMultiplier = 0.82f, startingLives = 4),
    NORMAL("Normal", ballSpeedMultiplier = 1f, startingLives = 3),
    HARD("Hard", ballSpeedMultiplier = 1.22f, startingLives = 2)
}
