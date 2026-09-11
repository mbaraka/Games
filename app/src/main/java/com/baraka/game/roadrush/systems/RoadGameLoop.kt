package com.baraka.game.roadrush.systems

import com.baraka.game.roadrush.model.GamePhase
import com.baraka.game.roadrush.model.PlayerCarState
import com.baraka.game.roadrush.model.RoadState
import com.baraka.game.roadrush.model.RoadState.Companion.BASE_SPEED
import com.baraka.game.roadrush.model.RoadState.Companion.COLLISION_BAND_END
import com.baraka.game.roadrush.model.RoadState.Companion.COLLISION_BAND_START
import com.baraka.game.roadrush.model.RoadState.Companion.INVINCIBILITY_SECONDS
import com.baraka.game.roadrush.model.RoadState.Companion.MAX_SPEED
import com.baraka.game.roadrush.model.RoadState.Companion.MIN_SPAWN_INTERVAL_SECONDS
import com.baraka.game.roadrush.model.RoadState.Companion.SPAWN_INTERVAL_SECONDS

/**
 * Advances a [RoadState] by one frame: same "pure `tick()` over immutable state" shape as
 * numbercorridor's GameLoopOrchestrator, just dodging traffic instead of shooting walls. The
 * only input is which lane the player wants to be in, applied instantly via [moveLeft]/[moveRight]
 * (arcade-style lane snap, not a smooth slide) — see ui/RoadRushScreen.kt for the tap buttons.
 */
object RoadGameLoop {

    fun tick(state: RoadState, deltaSeconds: Float): RoadState {
        if (state.phase != GamePhase.RUNNING) return state
        val dt = deltaSeconds.coerceIn(0f, 0.05f) // clamp so a dropped frame can't skip a whole car past the player

        val elapsed = state.elapsedSeconds + dt
        val speed = (BASE_SPEED + elapsed * 0.01f).coerceAtMost(MAX_SPEED)

        val advancedCars = state.cars.map { it.copy(progress = it.progress + speed * dt) }

        var nextCarId = state.nextCarId
        var nextSpawnIn = state.nextSpawnInSeconds - dt
        val spawnInterval = (SPAWN_INTERVAL_SECONDS - elapsed * 0.006f).coerceAtLeast(MIN_SPAWN_INTERVAL_SECONDS)
        val carsWithSpawn = if (nextSpawnIn <= 0f) {
            nextSpawnIn = spawnInterval
            val spawned = ObstacleSpawnSystem.spawn(nextCarId, elapsed)
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
            when {
                inCollisionBand && car.lane == state.player.lane && invincibleLeft <= 0f -> {
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
            player = state.player.copy(invincibleSeconds = newInvincible),
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

    fun moveLeft(state: RoadState): RoadState = moveTo(state, state.player.lane - 1)

    fun moveRight(state: RoadState): RoadState = moveTo(state, state.player.lane + 1)

    private fun moveTo(state: RoadState, targetLane: Int): RoadState {
        if (state.phase != GamePhase.RUNNING) return state
        val clamped = targetLane.coerceIn(0, PlayerCarState.LANE_COUNT - 1)
        return state.copy(player = state.player.copy(lane = clamped))
    }
}
