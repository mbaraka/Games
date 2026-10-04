package com.baraka.game.breakout.model

enum class GamePhase {
    /** Ball rests on the paddle, following it, waiting for a tap to launch. */
    AIMING,
    RUNNING,
    LEVEL_CLEARED,
    GAME_OVER
}

enum class PowerUpKind { WIDE_PADDLE, MULTI_BALL, EXTRA_LIFE }

data class Brick(val id: Int, val row: Int, val col: Int, val hp: Int, val colorSeed: Int)

data class Ball(val id: Long, val x: Float, val y: Float, val vx: Float, val vy: Float)

data class FallingPowerUp(val id: Long, val kind: PowerUpKind, val x: Float, val y: Float)

/** Axis-aligned rect in the same normalized logical space as everything else here. */
data class LogicalRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/BreakoutGameLoop.kt's `tick()` one copy at a time — same "pure tick over immutable
 * state" shape as roadrush's RoadState. Field coordinates are normalized 0..1 independently
 * on each axis: an x-ish size (ball radius, paddle half-width) scales by the canvas's actual
 * *width* in render/BreakoutRenderer.kt, a y-ish size (row height, paddle thickness) by its
 * *height* — so physics never needs to know real pixel dimensions, same trick roadrush's
 * RoadState uses with normalized `progress`.
 */
data class BreakoutState(
    val paddleX: Float = 0.5f,
    val widePaddleSecondsLeft: Float = 0f,
    val balls: List<Ball> = listOf(Ball(id = 0, x = 0.5f, y = PADDLE_Y - BALL_RADIUS * 2f, vx = 0f, vy = 0f)),
    val bricks: List<Brick> = emptyList(),
    val powerUps: List<FallingPowerUp> = emptyList(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val lives: Int = difficulty.startingLives,
    val score: Int = 0,
    val level: Int = 1,
    val phase: GamePhase = GamePhase.AIMING,
    val bestScoreAtStart: Int = 0,
    val nextBallId: Long = 1L,
    val nextPowerUpId: Long = 0L,
    // Transient per-tick event flags — read once by ui/BreakoutScreen.kt to trigger a sound
    // cue, then discarded on the next tick. Not persisted gameplay data.
    val bricksBrokenThisTick: Int = 0,
    val wallBounceThisTick: Boolean = false,
    val paddleBounceThisTick: Boolean = false,
    val powerUpCollectedThisTick: Boolean = false,
    val ballLostThisTick: Boolean = false
) {
    val isNewBest: Boolean get() = score > bestScoreAtStart
    val paddleHalfWidth: Float get() = if (widePaddleSecondsLeft > 0f) WIDE_PADDLE_HALF_WIDTH else BASE_PADDLE_HALF_WIDTH

    companion object {
        const val ROWS = 6
        const val COLS = 8
        const val BRICK_TOP = 0.10f
        const val BRICK_AREA_HEIGHT = 0.34f
        const val BRICK_MARGIN = 0.010f

        const val PADDLE_Y = 0.92f
        const val PADDLE_THICKNESS = 0.016f
        const val BASE_PADDLE_HALF_WIDTH = 0.095f
        const val WIDE_PADDLE_HALF_WIDTH = 0.15f
        const val WIDE_PADDLE_DURATION_SECONDS = 8f

        const val BALL_RADIUS = 0.013f
        const val BASE_BALL_SPEED = 0.50f // logical units/second
        const val SPEED_GROWTH_PER_LEVEL = 0.035f
        const val MAX_BALL_SPEED = 0.95f

        const val POWER_UP_RADIUS = 0.022f
        const val POWER_UP_FALL_SPEED = 0.26f
        const val POWER_UP_DROP_CHANCE = 0.16f
    }
}

/** Where brick (row, col) sits in logical space, margin already applied — the one source of truth both the game loop and the renderer read from. */
fun brickRect(row: Int, col: Int): LogicalRect {
    val cellWidth = 1f / BreakoutState.COLS
    val cellHeight = BreakoutState.BRICK_AREA_HEIGHT / BreakoutState.ROWS
    val m = BreakoutState.BRICK_MARGIN
    val top = BreakoutState.BRICK_TOP + row * cellHeight
    return LogicalRect(
        left = col * cellWidth + m,
        top = top + m,
        right = (col + 1) * cellWidth - m,
        bottom = top + cellHeight - m
    )
}
