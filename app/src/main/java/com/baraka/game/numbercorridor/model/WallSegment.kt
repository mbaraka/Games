package com.baraka.game.numbercorridor.model

/**
 * What a wall segment does once its hp reaches zero. Which kind can appear is fixed by
 * lane — see [WallRow]'s KDoc — so the player always knows what a lane does just by its
 * position, not by reading every tile.
 */
sealed class WallKind {
    /** Middle lane only. One or more enemies blocking the lane — just blocks/costs a heart if it survives to the player's line, no stat change. */
    data class Obstacle(val enemyCount: Int) : WallKind()

    /** Left lane only. Applies [op] with [value] to squad size once destroyed — the math lesson. */
    data class SquadShift(val op: MathOp, val value: Float) : WallKind()

    /** Right lane only. Shifts the equipped weapon's tier by [delta] (usually +1, occasionally -1) once destroyed. */
    data class WeaponCrate(val delta: Int) : WallKind()
}

/**
 * One shootable tile inside a [WallRow], occupying a single lane. `null` in a row's
 * [WallRow.segments] array means that lane is clear for that row.
 */
data class WallSegment(
    val kind: WallKind,
    val hp: Int,
    val maxHp: Int
) {
    /** Short label rendered on the tile. */
    fun label(): String = when (val k = kind) {
        is WallKind.Obstacle -> hp.toString()
        is WallKind.SquadShift -> "${k.op.symbol}${formatValue(k.value)}"
        is WallKind.WeaponCrate -> if (k.delta >= 0) "+${k.delta}" else "${k.delta}"
    }

    private fun formatValue(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
}

/**
 * A row of [LANE_COUNT] wall segments scrolling toward the player, one lane each with a
 * fixed job: lane 0 (left) only ever carries [WallKind.SquadShift], lane 1 (middle) only
 * [WallKind.Obstacle], lane 2 (right) only [WallKind.WeaponCrate] — see
 * systems/WallSpawnSystem.kt. [progress] is a normalized depth: 0f is the far horizon where
 * rows spawn, 1f is the player's own line.
 */
data class WallRow(
    val id: Long,
    val progress: Float,
    val segments: List<WallSegment?>
) {
    companion object {
        const val LANE_COUNT = 3
        const val SQUAD_LANE = 0
        const val ENEMY_LANE = 1
        const val WEAPON_LANE = 2
    }
}
