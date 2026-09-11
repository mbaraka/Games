package com.baraka.game.roadrush.systems

import com.baraka.game.roadrush.model.ObstacleCar
import com.baraka.game.roadrush.model.PlayerCarState
import kotlin.random.Random

/**
 * Builds new [ObstacleCar]s at the horizon (progress 0). Always leaves at least one lane free
 * so a spawn can never be an unavoidable wall, and fills more lanes at once the longer the
 * run goes — mirroring how numbercorridor's WallSpawnSystem scales fill chance off elapsed time.
 */
object ObstacleSpawnSystem {

    fun spawn(nextId: Long, elapsedSeconds: Float, random: Random = Random.Default): List<ObstacleCar> {
        val laneCount = PlayerCarState.LANE_COUNT
        val maxFilled = (1 + (elapsedSeconds / 20f).toInt()).coerceIn(1, laneCount - 1)
        val filledCount = (1..maxFilled).random(random)

        val lanes = (0 until laneCount).shuffled(random).take(filledCount)
        var id = nextId
        return lanes.map { lane ->
            ObstacleCar(id = id++, lane = lane, progress = 0f, colorSeed = random.nextInt(4))
        }
    }
}
