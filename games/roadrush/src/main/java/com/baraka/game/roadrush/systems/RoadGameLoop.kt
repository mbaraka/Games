package com.baraka.game.roadrush.systems

import com.baraka.game.roadrush.model.GamePhase
import com.baraka.game.roadrush.model.RoadState
import com.baraka.game.roadrush.model.RoadState.Companion.BASE_SPEED
import com.baraka.game.roadrush.model.RoadState.Companion.CAR_HALF_WIDTH
import com.baraka.game.roadrush.model.RoadState.Companion.COLLISION_BAND_END
import com.baraka.game.roadrush.model.RoadState.Companion.COLLISION_BAND_START
import com.baraka.game.roadrush.model.RoadState.Companion.INVINCIBILITY_SECONDS
import com.baraka.game.roadrush.model.RoadState.Companion.MAX_SPEED
import com.baraka.game.roadrush.model.RoadState.Companion.MIN_SPAWN_INTERVAL_SECONDS
import com.baraka.game.roadrush.model.RoadState.Companion.PLAYER_HALF_WIDTH
import com.baraka.game.roadrush.model.RoadState.Companion.SPAWN_INTERVAL_SECONDS
import com.baraka.game.roadrush.model.RoadState.Companion.STEER_SPEED
import kotlin.math.abs

/**
 * Advances a [RoadState] by one frame: same "pure `tick()` over immutable state" shape as
 * numbercorridor's GameLoopOrchestrator, just dodging traffic instead of shooting walls. The
 * player's horizontal position is continuous, not lane-locked (see model/RoadState.kt's
 * PlayerCarState KDoc) — [steerDirection] is -1 (holding left), 0, or 1 (holding right),
 * sampled by ui/RoadRushScreen.kt from the steering buttons' press state every tick, so the
 * car glides smoothly while a button is held rather than snapping on each tap.
 */
object RoadGameLoop {

    fun tick(state: RoadState, deltaSeconds: Float, steerDirection: Float = 0f): RoadState {
        if (state.phase != GamePhase.RUNNING) return state
        val dt = deltaSeconds.coerceIn(0f, 0.05f) // clamp so a dropped frame can't skip a whole car past the player

        val difficulty = state.difficulty
        val elapsed = state.elapsedSeconds + dt
        val speed = ((BASE_SPEED + elapsed * 0.01f) * difficulty.speedMultiplier).coerceAtMost(MAX_SPEED * difficulty.speedMultiplier)

        // Clamped to keep the car's own width on the asphalt rather than overhanging onto the grass at the very edge.
        val newX = (state.player.x + steerDirection.coerceIn(-1f, 1f) * STEER_SPEED * dt)
            .coerceIn(PLAYER_HALF_WIDTH, 1f - PLAYER_HALF_WIDTH)

        val advancedCars = state.cars.map { it.copy(progress = it.progress + speed * dt) }

        var nextCarId = state.nextCarId
        var nextSpawnIn = state.nextSpawnInSeconds - dt
        val spawnInterval = ((SPAWN_INTERVAL_SECONDS - elapsed * 0.006f) * difficulty.spawnIntervalMultiplier)
            .coerceAtLeast(MIN_SPAWN_INTERVAL_SECONDS)
        val carsWithSpawn = if (nextSpawnIn <= 0f) {
            nextSpawnIn = spawnInterval
            val spawned = ObstacleSpawnSystem.spawn(nextCarId, elapsed, difficulty)
            nextCarId += spawned.size
            advancedCars + spawned
        } else {
            advancedCars
        }

        val invincibleLeft = (state.player.invincibleSeconds - dt).coerceAtLeast(0f)

        var scoreGained = 0
        var livesLost = 0
        var dodgedCount = 0
        val resolvedCars = carsWithSpawn.map { car ->
            if (car.resolved) return@map car
            val inCollisionBand = car.progress in COLLISION_BAND_START..COLLISION_BAND_END
            val overlapsPlayer = abs(car.x - newX) < PLAYER_HALF_WIDTH + CAR_HALF_WIDTH
            when {
                inCollisionBand && overlapsPlayer && invincibleLeft <= 0f -> {
                    livesLost += 1
                    car.copy(resolved = true)
                }
                car.progress > COLLISION_BAND_END -> {
                    scoreGained += 5
                    dodgedCount += 1
                    car.copy(resolved = true)
                }
                else -> car
            }
        }
        val survivingCars = resolvedCars.filter { it.progress < 1.15f }

        val newLives = (state.lives - livesLost).coerceAtLeast(0)
        val newInvincible = if (livesLost > 0) INVINCIBILITY_SECONDS else invincibleLeft
        val distanceScore = (dt * speed * 40f).toInt()

        return state.copy(
            player = state.player.copy(x = newX, invincibleSeconds = newInvincible),
            cars = survivingCars,
            lives = newLives,
            score = state.score + scoreGained + distanceScore,
            elapsedSeconds = elapsed,
            speed = speed,
            nextSpawnInSeconds = nextSpawnIn,
            nextCarId = nextCarId,
            phase = if (newLives <= 0) GamePhase.GAME_OVER else GamePhase.RUNNING,
            carsDodgedThisTick = dodgedCount
        )
    }
}
