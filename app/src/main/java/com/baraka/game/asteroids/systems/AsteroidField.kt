package com.baraka.game.asteroids.systems

import com.baraka.game.asteroids.model.Asteroid
import com.baraka.game.asteroids.model.AsteroidSize
import com.baraka.game.asteroids.model.AsteroidsState.Companion.STARTING_LARGE_ASTEROIDS
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Spawns a wave's starting rocks, and splits one rock that just got shot into its two smaller children. */
object AsteroidField {

    private const val MIN_SPEED = 0.05f
    private const val MAX_SPEED = 0.09f
    private const val SPEED_PER_SIZE_STEP = 1.45f // each size down is this much faster — small rocks are the frantic ones
    private const val SAFE_SPAWN_RADIUS = 0.22f // keep starting rocks away from the ship's center spawn

    fun startingWave(wave: Int, nextId: Long, speedMultiplier: Float, rng: Random): Pair<List<Asteroid>, Long> {
        val count = (STARTING_LARGE_ASTEROIDS + (wave - 1)).coerceAtMost(8)
        var id = nextId
        val asteroids = (0 until count).map {
            val angle = rng.nextFloat() * (2 * Math.PI).toFloat()
            val dist = SAFE_SPAWN_RADIUS + rng.nextFloat() * 0.25f
            val x = (0.5f + cos(angle) * dist).mod(1f)
            val y = (0.5f + sin(angle) * dist).mod(1f)
            spawn(id++, x, y, AsteroidSize.LARGE, speedMultiplier, rng)
        }
        return asteroids to id
    }

    /** Two smaller children at the parent's position, inheriting a spread of its velocity. */
    fun split(parent: Asteroid, nextId: Long, speedMultiplier: Float, rng: Random): Pair<List<Asteroid>, Long> {
        val childSize = when (parent.size) {
            AsteroidSize.LARGE -> AsteroidSize.MEDIUM
            AsteroidSize.MEDIUM -> AsteroidSize.SMALL
            AsteroidSize.SMALL -> return emptyList<Asteroid>() to nextId
        }
        var id = nextId
        val children = (0 until 2).map {
            val spreadAngle = rng.nextFloat() * (2 * Math.PI).toFloat()
            val speed = randomSpeed(childSize, speedMultiplier, rng)
            Asteroid(
                id = id++,
                x = parent.x, y = parent.y,
                vx = cos(spreadAngle) * speed, vy = sin(spreadAngle) * speed,
                rotation = rng.nextFloat() * (2 * Math.PI).toFloat(),
                rotationSpeed = (rng.nextFloat() - 0.5f) * 2.5f,
                size = childSize,
                shapeSeed = rng.nextInt()
            )
        }
        return children to id
    }

    private fun spawn(id: Long, x: Float, y: Float, size: AsteroidSize, speedMultiplier: Float, rng: Random): Asteroid {
        val angle = rng.nextFloat() * (2 * Math.PI).toFloat()
        val speed = randomSpeed(size, speedMultiplier, rng)
        return Asteroid(
            id = id, x = x, y = y,
            vx = cos(angle) * speed, vy = sin(angle) * speed,
            rotation = rng.nextFloat() * (2 * Math.PI).toFloat(),
            rotationSpeed = (rng.nextFloat() - 0.5f) * 2f,
            size = size,
            shapeSeed = rng.nextInt()
        )
    }

    private fun randomSpeed(size: AsteroidSize, speedMultiplier: Float, rng: Random): Float {
        val sizeStep = when (size) { AsteroidSize.LARGE -> 0; AsteroidSize.MEDIUM -> 1; AsteroidSize.SMALL -> 2 }
        val base = MIN_SPEED + rng.nextFloat() * (MAX_SPEED - MIN_SPEED)
        return base * Math.pow(SPEED_PER_SIZE_STEP.toDouble(), sizeStep.toDouble()).toFloat() * speedMultiplier
    }
}
