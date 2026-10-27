package com.baraka.game.arcshot.systems

import com.baraka.game.arcshot.model.ARC_LEVELS
import com.baraka.game.arcshot.model.ArcLevel
import com.baraka.game.arcshot.model.GROUND_Y
import com.baraka.game.arcshot.model.LAUNCHER_X
import kotlin.math.hypot

const val MIN_POWER = 150f
const val MAX_POWER = 1200f
const val BALL_RADIUS = 8f
private const val TRAIL_CAPACITY = 180

enum class ShotResult { NONE, FLYING, HIT, MISS }

/**
 * Pure physics state for one Arc Shot level: ball position/velocity, gravity + wind, collision
 * against the level's obstacles and the target, and a fixed-size trail buffer for the renderer.
 * No input or drawing lives here.
 */
class ArcGame(val level: ArcLevel) {
    var gravity = level.gravity
    var wind = level.wind
    var ballX = LAUNCHER_X
    var ballY = GROUND_Y - BALL_RADIUS
    var vx = 0f
    var vy = 0f
    var result = ShotResult.NONE
        private set
    var shots = 0
        private set
    var won = false
        private set
    var infiniteShots = false

    private val trailX = FloatArray(TRAIL_CAPACITY)
    private val trailY = FloatArray(TRAIL_CAPACITY)
    var trailCount = 0
        private set
    private var trailHead = 0

    fun launch(aimVx: Float, aimVy: Float) {
        if (result == ShotResult.FLYING || won) return
        ballX = LAUNCHER_X
        ballY = GROUND_Y - BALL_RADIUS
        vx = aimVx
        vy = aimVy
        result = ShotResult.FLYING
        shots++
        trailCount = 0
        trailHead = 0
    }

    /** Advances the flight by [dt] seconds. Returns true while the ball is still airborne. */
    fun step(dt: Float): Boolean {
        if (result != ShotResult.FLYING) return false
        vx += wind * dt
        vy += gravity * dt
        ballX += vx * dt
        ballY += vy * dt

        trailX[trailHead] = ballX
        trailY[trailHead] = ballY
        trailHead = (trailHead + 1) % TRAIL_CAPACITY
        if (trailCount < TRAIL_CAPACITY) trailCount++

        if (hypot(ballX - level.targetX, ballY - level.targetY) <= level.targetRadius + BALL_RADIUS) {
            result = ShotResult.HIT
            won = true
            return false
        }
        for (r in level.obstacles) {
            if (ballX >= r.x - BALL_RADIUS && ballX <= r.x + r.w + BALL_RADIUS &&
                ballY >= r.y - BALL_RADIUS && ballY <= r.y + r.h + BALL_RADIUS
            ) {
                result = ShotResult.MISS
                return false
            }
        }
        if (ballY > GROUND_Y + BALL_RADIUS || ballX < -80f || ballX > 1080f) {
            result = ShotResult.MISS
            return false
        }
        return true
    }

    fun trailPoint(i: Int): Pair<Float, Float> {
        val idx = (trailHead - trailCount + i + TRAIL_CAPACITY) % TRAIL_CAPACITY
        return trailX[idx] to trailY[idx]
    }

    fun readyForNextShot() {
        if (result == ShotResult.MISS) result = ShotResult.NONE
    }

    /** Debug cheat: teleports the ball straight onto the target. */
    fun winInstantly() {
        ballX = level.targetX
        ballY = level.targetY
        result = ShotResult.HIT
        won = true
    }
}

fun starsFor(level: ArcLevel, shots: Int): Int = when {
    shots <= level.parShots - 1 || shots <= 1 -> 3
    shots <= level.parShots -> 2
    else -> 1
}.coerceIn(1, 3)

fun levelByIndex(i: Int): ArcLevel = ARC_LEVELS[i]
