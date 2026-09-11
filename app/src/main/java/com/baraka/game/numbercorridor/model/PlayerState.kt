package com.baraka.game.numbercorridor.model

/**
 * The squad's stats and position. All three combat stats — [damage], [fireRateMultiplier]
 * and [squadCount] — are only ever changed by shooting down a [WallKind.Buff] wall, never
 * by leveling or shopping, which is the whole point of this game: math is the upgrade path.
 */
data class PlayerState(
    /** Leftmost lane the squad occupies; the squad spans [laneStart] .. laneStart + squadCount - 1. */
    val laneStart: Int,
    val squadCount: Int,
    val damage: Float,
    val fireRateMultiplier: Float,
    val health: Int,
    val maxHealth: Int,
    /** Per-lane time (seconds) until that lane's avatar can fire again. Keyed by absolute lane index. */
    val fireCooldowns: Map<Int, Float> = emptyMap()
) {
    val occupiedLanes: IntRange get() = laneStart until (laneStart + squadCount)

    companion object {
        const val MAX_SQUAD = 4
        const val MIN_SQUAD = 1
        const val BASE_FIRE_INTERVAL_SECONDS = 0.5f

        fun initial(): PlayerState {
            val start = (WallRow.LANE_COUNT - 1) / 2
            return PlayerState(
                laneStart = start,
                squadCount = 1,
                damage = 10f,
                fireRateMultiplier = 1f,
                health = 5,
                maxHealth = 5
            )
        }
    }
}
