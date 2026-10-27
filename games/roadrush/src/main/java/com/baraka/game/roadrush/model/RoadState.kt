package com.baraka.game.roadrush.model

enum class GamePhase {
    RUNNING,
    GAME_OVER
}

/**
 * One rival car on the road. [progress] is normalized depth: 0 = just spawned at the
 * horizon, 1 = at the player's line. Rival cars still spawn into one of [PlayerCarState.LANE_COUNT]
 * fixed slots (see systems/ObstacleSpawnSystem.kt) so traffic stays readable and a spawn can
 * never wall off the whole road — only the player moves freely, via [PlayerCarState.x].
 */
data class ObstacleCar(
    val id: Long,
    val lane: Int,
    val progress: Float,
    /** True once this car has either hit the player or been safely dodged, so it can't score/damage twice. */
    val resolved: Boolean = false,
    val colorSeed: Int = 0
) {
    /** This car's horizontal position, as a fraction of the road's width (0 = left edge, 1 = right edge) — same space as [PlayerCarState.x]. */
    val x: Float get() = (lane + 0.5f) / PlayerCarState.LANE_COUNT
}

/**
 * The player's car: [x] is a continuous fraction of the road's width (0 = left edge, 1 =
 * right edge) rather than a lane index — see systems/RoadGameLoop.kt.tick's `steerDirection`
 * parameter for how it moves. Earlier versions snapped between 3 fixed lanes; player feedback
 * (F12 in docs/feedback-backlog.md) was that the lane lock felt restrictive compared to a
 * reference racer with free left/right movement, so only the *player* was loosened — rival
 * cars keep spawning in fixed lanes (see [ObstacleCar]'s KDoc) so traffic stays predictable.
 */
data class PlayerCarState(
    val x: Float,
    val invincibleSeconds: Float = 0f
) {
    companion object {
        fun initial(): PlayerCarState = PlayerCarState(x = 0.5f)

        /** Rival-car spawn slots and lane-marking visuals only — the player is no longer confined to these, see this class's KDoc. */
        const val LANE_COUNT = 3
    }
}

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/RoadGameLoop.kt's `tick()` one copy at a time — same "pure tick over immutable
 * state" shape as numbercorridor's GameState, just a lane-dodging racer instead of a shooter.
 */
data class RoadState(
    val player: PlayerCarState = PlayerCarState.initial(),
    val cars: List<ObstacleCar> = emptyList(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val lives: Int = difficulty.startingLives,
    val score: Int = 0,
    val elapsedSeconds: Float = 0f,
    /** How fast cars advance toward the player, in progress/second. Ramps up with [elapsedSeconds]. */
    val speed: Float = BASE_SPEED,
    /** Countdown to the next spawn. */
    val nextSpawnInSeconds: Float = 0.6f,
    val phase: GamePhase = GamePhase.RUNNING,
    val bestScoreAtStart: Int = 0,
    /** Monotonically increasing id source for spawned cars, threaded through state to keep tick() a pure function. */
    val nextCarId: Long = 0L,
    /** How many cars were successfully dodged on the tick that produced this state — a transient event flag, not persisted gameplay data, read once by ui/RoadRushScreen.kt to trigger a sound cue then discarded. */
    val carsDodgedThisTick: Int = 0
) {
    val isNewBest: Boolean get() = score > bestScoreAtStart

    companion object {
        const val BASE_SPEED = 0.5f
        const val MAX_SPEED = 1.6f
        const val SPAWN_INTERVAL_SECONDS = 1.1f
        const val MIN_SPAWN_INTERVAL_SECONDS = 0.55f
        /** Collision band around the player's line: a car overlapping the player's [PlayerCarState.x] inside this range hits on contact. */
        const val COLLISION_BAND_START = 0.86f
        const val COLLISION_BAND_END = 1.0f
        const val INVINCIBILITY_SECONDS = 1.2f
        /** Half-width of the player's car, in the same 0..1 road-fraction space as [PlayerCarState.x]. */
        const val PLAYER_HALF_WIDTH = 0.105f
        /** Half-width of a rival car, same space as [PLAYER_HALF_WIDTH]. */
        const val CAR_HALF_WIDTH = 0.095f
        /** How fast the player's car crosses the road while a steering button is held, in road-fractions/second. */
        const val STEER_SPEED = 1.1f
    }
}
