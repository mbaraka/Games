package com.baraka.game.snake.model

/** A run-wide preset — same shape as the other Atari-style games' Difficulty. All tuning lives here. */
enum class Difficulty(
    val displayName: String,
    val startIntervalSeconds: Float,
    val minIntervalSeconds: Float,
    val speedupPerFood: Float
) {
    EASY("Easy", startIntervalSeconds = 0.17f, minIntervalSeconds = 0.10f, speedupPerFood = 0.0020f),
    NORMAL("Normal", startIntervalSeconds = 0.13f, minIntervalSeconds = 0.075f, speedupPerFood = 0.0028f),
    HARD("Hard", startIntervalSeconds = 0.105f, minIntervalSeconds = 0.06f, speedupPerFood = 0.0035f)
}
