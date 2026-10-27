package com.baraka.game.snake.model

enum class GamePhase { RUNNING, GAME_OVER }

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    fun isOppositeOf(other: Direction): Boolean = dx == -other.dx && dy == -other.dy
}

data class Cell(val x: Int, val y: Int) {
    fun moved(direction: Direction): Cell = Cell(x + direction.dx, y + direction.dy)
}

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/SnakeGameLoop.kt's `tick()` one copy at a time — same "pure tick over immutable
 * state" shape as the other Atari-style games here, just grid-stepped instead of continuous:
 * the snake only actually advances once [elapsedSinceMoveSeconds] crosses
 * [moveIntervalSeconds], everything else about `tick()` is a no-op pass-through.
 *
 * [snake] is head-first. [pendingDirection] is the latest swipe, applied at the *next* grid
 * step rather than immediately — buffering it this way means a quick swipe during the current
 * step is never silently dropped, and a same-tick 180° reversal (which would self-collide
 * instantly) is rejected in `SnakeGameLoop.tick`, not here.
 */
data class SnakeState(
    val snake: List<Cell> = emptyList(),
    val direction: Direction = Direction.RIGHT,
    val pendingDirection: Direction? = null,
    val food: Cell = Cell(0, 0),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val score: Int = 0,
    val moveIntervalSeconds: Float = difficulty.startIntervalSeconds,
    val elapsedSinceMoveSeconds: Float = 0f,
    val phase: GamePhase = GamePhase.RUNNING,
    val bestScoreAtStart: Int = 0,
    // Transient per-step event flag — read once by ui/SnakeScreen.kt to trigger a sound cue, then discarded on the next step.
    val foodEatenThisStep: Boolean = false
) {
    val isNewBest: Boolean get() = score > bestScoreAtStart

    companion object {
        const val GRID_WIDTH = 14
        const val GRID_HEIGHT = 20
        const val STARTING_LENGTH = 3
        const val SCORE_PER_FOOD = 10
    }
}
