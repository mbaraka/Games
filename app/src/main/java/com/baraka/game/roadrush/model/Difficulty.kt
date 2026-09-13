package com.baraka.game.roadrush.model

/**
 * A run-wide preset picked before a run starts, same role as knightsurvivor's Difficulty —
 * all tuning lives here so a number change doesn't need to touch systems/RoadGameLoop.kt.
 * Aimed at letting a wide range of kids each find a level they can actually play: Easy trades
 * speed and traffic density for extra lives, Hard is the original tuning.
 */
enum class Difficulty(
    val displayName: String,
    val description: String,
    val startingLives: Int,
    /** Multiplies both the base and max road speed, and how fast speed ramps up over a run. */
    val speedMultiplier: Float,
    /** >1 = slower spawns (easier), <1 = faster (harder). */
    val spawnIntervalMultiplier: Float,
    /** Caps how many of the road's lanes a single spawn can fill at once — always leaves at least one free lane beyond this. */
    val maxLanesFilledAtOnce: Int
) {
    EASY(
        displayName = "Easy",
        description = "Slow traffic, plenty of gaps, extra life. Good for younger kids.",
        startingLives = 4,
        speedMultiplier = 0.65f,
        spawnIntervalMultiplier = 1.6f,
        maxLanesFilledAtOnce = 1
    ),
    NORMAL(
        displayName = "Normal",
        description = "A fair, steadily-ramping challenge.",
        startingLives = 3,
        speedMultiplier = 1f,
        spawnIntervalMultiplier = 1f,
        maxLanesFilledAtOnce = 2
    ),
    HARD(
        displayName = "Hard",
        description = "Fast traffic from the start, less room to breathe.",
        startingLives = 2,
        speedMultiplier = 1.3f,
        spawnIntervalMultiplier = 0.75f,
        maxLanesFilledAtOnce = 2
    )
}
