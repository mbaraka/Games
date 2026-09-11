package com.baraka.game.roadrush.model

enum class GamePhase {
    RUNNING,
    GAME_OVER
}

/** One rival car on the road. [progress] is normalized depth: 0 = just spawned at the horizon, 1 = at the player's line. */
data class ObstacleCar(
    val id: Long,
    val lane: Int,
    val progress: Float,
    /** True once this car has either hit the player or been safely dodged, so it can't score/damage twice. */
    val resolved: Boolean = false,
    val colorSeed: Int = 0
)

/** The player's car: which lane it's in, and a brief post-crash invincibility/flash window. */
data class PlayerCarState(
    val lane: Int,
    val invincibleSeconds: Float = 0f
) {
    companion object {
        fun initial(): PlayerCarState = PlayerCarState(lane = (LANE_COUNT - 1) / 2)
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
    val lives: Int = STARTING_LIVES,
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
        const val STARTING_LIVES = 3
        const val BASE_SPEED = 0.5f
        const val MAX_SPEED = 1.6f
        const val SPAWN_INTERVAL_SECONDS = 1.1f
        const val MIN_SPAWN_INTERVAL_SECONDS = 0.55f
        /** Collision band around the player's line: a car in the player's lane inside this range hits on contact. */
        const val COLLISION_BAND_START = 0.86f
        const val COLLISION_BAND_END = 1.0f
        const val INVINCIBILITY_SECONDS = 1.2f
    }
}
