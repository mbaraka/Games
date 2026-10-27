package com.baraka.game.breakout.systems

import com.baraka.game.breakout.model.Ball
import com.baraka.game.breakout.model.BreakoutState
import com.baraka.game.breakout.model.BreakoutState.Companion.BALL_RADIUS
import com.baraka.game.breakout.model.BreakoutState.Companion.BASE_BALL_SPEED
import com.baraka.game.breakout.model.BreakoutState.Companion.MAX_BALL_SPEED
import com.baraka.game.breakout.model.BreakoutState.Companion.PADDLE_THICKNESS
import com.baraka.game.breakout.model.BreakoutState.Companion.PADDLE_Y
import com.baraka.game.breakout.model.BreakoutState.Companion.POWER_UP_DROP_CHANCE
import com.baraka.game.breakout.model.BreakoutState.Companion.POWER_UP_FALL_SPEED
import com.baraka.game.breakout.model.BreakoutState.Companion.POWER_UP_RADIUS
import com.baraka.game.breakout.model.BreakoutState.Companion.SPEED_GROWTH_PER_LEVEL
import com.baraka.game.breakout.model.BreakoutState.Companion.WIDE_PADDLE_DURATION_SECONDS
import com.baraka.game.breakout.model.Difficulty
import com.baraka.game.breakout.model.FallingPowerUp
import com.baraka.game.breakout.model.GamePhase
import com.baraka.game.breakout.model.PowerUpKind
import com.baraka.game.breakout.model.brickRect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Advances a [BreakoutState] by one frame: same "pure `tick()` over immutable state" shape
 * as roadrush's RoadGameLoop, just paddle-and-ball physics instead of lane dodging. Ball/
 * brick and ball/paddle collision both use the classic cheap-and-good-enough approximation
 * (compare which axis actually overlaps to decide which velocity component flips) rather
 * than true swept collision — at this ball speed and this brick size it reads correctly and
 * never needs real physics.
 */
object BreakoutGameLoop {

    private const val MAX_DT = 0.034f // clamp so a dropped frame can't tunnel the ball through a brick row

    fun newGame(difficulty: Difficulty, bestScore: Int): BreakoutState =
        startLevel(
            BreakoutState(difficulty = difficulty, lives = difficulty.startingLives, bestScoreAtStart = bestScore),
            level = 1
        )

    fun startLevel(state: BreakoutState, level: Int): BreakoutState {
        val ball = Ball(id = 0, x = state.paddleX, y = PADDLE_Y - BALL_RADIUS * 2f, vx = 0f, vy = 0f)
        return state.copy(
            level = level,
            bricks = BrickLayout.build(level),
            powerUps = emptyList(),
            balls = listOf(ball),
            widePaddleSecondsLeft = 0f,
            nextBallId = 1L,
            phase = GamePhase.AIMING
        )
    }

    fun setPaddleX(state: BreakoutState, x: Float): BreakoutState {
        val half = state.paddleHalfWidth
        val clamped = x.coerceIn(half, 1f - half)
        var next = state.copy(paddleX = clamped)
        if (state.phase == GamePhase.AIMING) {
            next = next.copy(balls = next.balls.map { it.copy(x = clamped) })
        }
        return next
    }

    fun launch(state: BreakoutState, rng: Random): BreakoutState {
        if (state.phase != GamePhase.AIMING) return state
        val speed = ballSpeed(state)
        val angleDeg = rng.nextInt(-32, 33)
        val rad = Math.toRadians(angleDeg.toDouble())
        val ball = state.balls.first().copy(
            vx = (speed * sin(rad)).toFloat(),
            vy = -(speed * cos(rad)).toFloat()
        )
        return state.copy(balls = listOf(ball), phase = GamePhase.RUNNING)
    }

    private fun ballSpeed(state: BreakoutState): Float =
        (BASE_BALL_SPEED + (state.level - 1) * SPEED_GROWTH_PER_LEVEL)
            .coerceAtMost(MAX_BALL_SPEED) * state.difficulty.ballSpeedMultiplier

    fun tick(state: BreakoutState, deltaSeconds: Float, rng: Random): BreakoutState {
        if (state.phase != GamePhase.RUNNING) return state
        val dt = deltaSeconds.coerceIn(0f, MAX_DT)

        var bricks = state.bricks
        var scoreGained = 0
        var bricksBroken = 0
        var wallBounce = false
        var paddleBounce = false
        var nextPowerUpId = state.nextPowerUpId
        val newPowerUps = mutableListOf<FallingPowerUp>()

        val survivingBalls = mutableListOf<Ball>()
        var ballLost = false

        for (ball in state.balls) {
            var x = ball.x + ball.vx * dt
            var y = ball.y + ball.vy * dt
            var vx = ball.vx
            var vy = ball.vy

            // Side walls.
            if (x - BALL_RADIUS < 0f) {
                x = BALL_RADIUS; vx = abs(vx); wallBounce = true
            } else if (x + BALL_RADIUS > 1f) {
                x = 1f - BALL_RADIUS; vx = -abs(vx); wallBounce = true
            }
            // Ceiling.
            if (y - BALL_RADIUS < 0f) {
                y = BALL_RADIUS; vy = abs(vy); wallBounce = true
            }

            // Paddle — swept against the ball's *previous* y so a fast ball can't skip through
            // the thin paddle band in one tick.
            val paddleTop = PADDLE_Y - PADDLE_THICKNESS / 2f
            if (vy > 0f && ball.y <= paddleTop + BALL_RADIUS && y + BALL_RADIUS >= paddleTop) {
                val half = state.paddleHalfWidth
                if (x in (state.paddleX - half - BALL_RADIUS)..(state.paddleX + half + BALL_RADIUS)) {
                    val offset = ((x - state.paddleX) / half).coerceIn(-1f, 1f)
                    val speed = sqrt(vx * vx + vy * vy).coerceAtLeast(ballSpeed(state) * 0.6f)
                    val angle = offset * Math.toRadians(55.0)
                    vx = (speed * sin(angle)).toFloat()
                    vy = -(speed * cos(angle)).toFloat()
                    y = paddleTop - BALL_RADIUS
                    paddleBounce = true
                }
            }

            // Bricks — at most one hit per ball per tick.
            if (y - BRICK_SCAN_MARGIN < BreakoutState.BRICK_TOP + BreakoutState.BRICK_AREA_HEIGHT) {
                val hitIndex = bricks.indexOfFirst { brick -> circleOverlapsRect(x, y, BALL_RADIUS, brickRect(brick.row, brick.col)) }
                if (hitIndex >= 0) {
                    val brick = bricks[hitIndex]
                    val rect = brickRect(brick.row, brick.col)
                    val hitVertically = x in rect.left..rect.right
                    if (hitVertically) vy = -vy else vx = -vx

                    val remainingHp = brick.hp - 1
                    bricks = if (remainingHp <= 0) {
                        bricksBroken += 1
                        scoreGained += BrickLayout.pointsForRow(brick.row)
                        if (rng.nextFloat() < POWER_UP_DROP_CHANCE) {
                            newPowerUps += FallingPowerUp(
                                id = nextPowerUpId++,
                                kind = PowerUpKind.entries.random(rng),
                                x = (rect.left + rect.right) / 2f,
                                y = (rect.top + rect.bottom) / 2f
                            )
                        }
                        bricks.toMutableList().also { it.removeAt(hitIndex) }
                    } else {
                        scoreGained += 2
                        bricks.toMutableList().also { it[hitIndex] = brick.copy(hp = remainingHp) }
                    }
                }
            }

            if (y - BALL_RADIUS > 1.05f) {
                ballLost = true // this ball fell past the paddle
            } else {
                survivingBalls += ball.copy(x = x, y = y, vx = vx, vy = vy)
            }
        }

        // Falling power-ups: advance, collect against the paddle, drop the ones that fall off-screen.
        var widePaddleSecondsLeft = (state.widePaddleSecondsLeft - dt).coerceAtLeast(0f)
        var lives = state.lives
        var collected = false
        val paddleTop = PADDLE_Y - PADDLE_THICKNESS / 2f
        val survivingPowerUps = mutableListOf<FallingPowerUp>()
        for (p in state.powerUps + newPowerUps) {
            val fallen = p.copy(y = p.y + POWER_UP_FALL_SPEED * dt)
            val half = state.paddleHalfWidth
            val caught = fallen.y + POWER_UP_RADIUS >= paddleTop &&
                fallen.y - POWER_UP_RADIUS <= PADDLE_Y + PADDLE_THICKNESS &&
                fallen.x in (state.paddleX - half)..(state.paddleX + half)
            when {
                caught -> {
                    collected = true
                    when (fallen.kind) {
                        PowerUpKind.WIDE_PADDLE -> widePaddleSecondsLeft = WIDE_PADDLE_DURATION_SECONDS
                        PowerUpKind.EXTRA_LIFE -> lives += 1
                        PowerUpKind.MULTI_BALL -> {
                            val template = survivingBalls.firstOrNull()
                            if (template != null) {
                                val speed = sqrt(template.vx * template.vx + template.vy * template.vy)
                                val baseAngle = Math.atan2(template.vx.toDouble(), -template.vy.toDouble())
                                listOf(-1, 1).forEach { side ->
                                    val angle = baseAngle + side * Math.toRadians(28.0)
                                    survivingBalls += template.copy(
                                        id = state.nextBallId + if (side < 0) 0 else 1,
                                        vx = (speed * sin(angle)).toFloat(),
                                        vy = -(speed * cos(angle)).toFloat()
                                    )
                                }
                            }
                        }
                    }
                }
                fallen.y - POWER_UP_RADIUS > 1.05f -> {} // fell off, drop it
                else -> survivingPowerUps += fallen
            }
        }

        val outOfBalls = survivingBalls.isEmpty()
        if (outOfBalls) lives = (lives - 1).coerceAtLeast(0)

        val nextPhase = when {
            outOfBalls && lives <= 0 -> GamePhase.GAME_OVER
            bricks.isEmpty() -> GamePhase.LEVEL_CLEARED
            else -> GamePhase.RUNNING
        }

        val resetBallForNextTry = if (outOfBalls && lives > 0) {
            listOf(Ball(id = 0, x = state.paddleX, y = PADDLE_Y - BALL_RADIUS * 2f, vx = 0f, vy = 0f))
        } else null

        return state.copy(
            balls = resetBallForNextTry ?: survivingBalls,
            bricks = bricks,
            powerUps = survivingPowerUps,
            widePaddleSecondsLeft = widePaddleSecondsLeft,
            lives = lives,
            score = state.score + scoreGained,
            nextPowerUpId = nextPowerUpId,
            nextBallId = state.nextBallId + 2,
            phase = if (outOfBalls && lives > 0) GamePhase.AIMING else nextPhase,
            bricksBrokenThisTick = bricksBroken,
            wallBounceThisTick = wallBounce,
            paddleBounceThisTick = paddleBounce,
            powerUpCollectedThisTick = collected,
            ballLostThisTick = ballLost
        )
    }

    private const val BRICK_SCAN_MARGIN = 0.03f

    private fun circleOverlapsRect(cx: Float, cy: Float, radius: Float, rect: com.baraka.game.breakout.model.LogicalRect): Boolean {
        val closestX = cx.coerceIn(rect.left, rect.right)
        val closestY = cy.coerceIn(rect.top, rect.bottom)
        val dx = cx - closestX
        val dy = cy - closestY
        return dx * dx + dy * dy <= radius * radius
    }
}
