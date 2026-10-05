package com.baraka.game.asteroids.model

/** A run-wide preset — same shape as the other Atari-style games' Difficulty. All tuning lives here. */
enum class Difficulty(
    val displayName: String,
    val asteroidSpeedMultiplier: Float,
    val startingLives: Int
) {
    EASY("Easy", asteroidSpeedMultiplier = 0.78f, startingLives = 4),
    NORMAL("Normal", asteroidSpeedMultiplier = 1f, startingLives = 3),
    HARD("Hard", asteroidSpeedMultiplier = 1.3f, startingLives = 2)
}
