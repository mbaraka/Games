package com.baraka.game.numbercorridor.model

/** What a wall segment does once its hp reaches zero. */
sealed class WallKind {
    /** Just blocks the lane. Destroying it grants score but changes no stat. */
    data object Obstacle : WallKind()

    /** Applies [op] with [value] to [target] once destroyed — the math lesson. */
    data class Buff(val target: BuffTarget, val op: MathOp, val value: Float) : WallKind()
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
    val isObstacle: Boolean get() = kind is WallKind.Obstacle

    /** Short label rendered on the tile: the remaining hp for obstacles, the math op for buffs. */
    fun label(): String = when (val k = kind) {
        is WallKind.Obstacle -> hp.toString()
        is WallKind.Buff -> "${k.op.symbol}${formatValue(k.value)}"
    }

    private fun formatValue(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
}

/**
 * A row of up to [LANE_COUNT] wall segments scrolling toward the player. [progress] is a
 * normalized depth: 0f is the far horizon where rows spawn, 1f is the player's own line —
 * see systems/GameLoopOrchestrator.kt for how this advances and render/CorridorRenderer.kt
 * for how it's turned into screen coordinates.
 */
data class WallRow(
    val id: Long,
    val progress: Float,
    val segments: List<WallSegment?>
) {
    companion object {
        const val LANE_COUNT = 5
    }
}
