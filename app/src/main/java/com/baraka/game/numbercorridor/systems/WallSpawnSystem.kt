package com.baraka.game.numbercorridor.systems

import com.baraka.game.numbercorridor.model.BuffTarget
import com.baraka.game.numbercorridor.model.MathOp
import com.baraka.game.numbercorridor.model.WallKind
import com.baraka.game.numbercorridor.model.WallRow
import com.baraka.game.numbercorridor.model.WallSegment
import kotlin.random.Random

/**
 * Builds a fresh [WallRow] at the horizon (progress 0). Difficulty is driven purely by
 * [elapsedSeconds]: more lanes get filled and obstacle hp climbs the longer a run goes,
 * mirroring how knightsurvivor's EnemySpawnSystem scales off survival time.
 */
object WallSpawnSystem {

    fun spawnRow(id: Long, elapsedSeconds: Float, random: Random = Random.Default): WallRow {
        val fillChance = (0.5f + elapsedSeconds / 240f).coerceAtMost(0.85f)
        val obstacleHpRange = obstacleHpRange(elapsedSeconds)

        val segments = List(WallRow.LANE_COUNT) { lane ->
            if (random.nextFloat() > fillChance) return@List null
            if (random.nextFloat() < 0.45f) {
                val hp = random.nextInt(obstacleHpRange.first, obstacleHpRange.last + 1)
                WallSegment(WallKind.Obstacle, hp, hp)
            } else {
                buffSegment(random)
            }
        }

        return WallRow(id = id, progress = 0f, segments = segments)
    }

    private fun obstacleHpRange(elapsedSeconds: Float): IntRange {
        val base = 15 + (elapsedSeconds * 0.9f).toInt()
        return base..(base + 12)
    }

    private fun buffSegment(random: Random): WallSegment {
        val target = BuffTarget.entries[random.nextInt(BuffTarget.entries.size)]
        val (op, value) = randomOpAndValue(target, random)
        val hp = random.nextInt(8, 18)
        return WallSegment(WallKind.Buff(target, op, value), hp, hp)
    }

    private fun randomOpAndValue(target: BuffTarget, random: Random): Pair<MathOp, Float> {
        return when (target) {
            BuffTarget.SQUAD -> {
                // Squad size is a small integer, so keep the math friendly: +/-1 or x/÷2.
                val op = listOf(MathOp.ADD, MathOp.SUBTRACT, MathOp.MULTIPLY, MathOp.DIVIDE).random(random)
                val value = if (op == MathOp.MULTIPLY || op == MathOp.DIVIDE) 2f else 1f
                op to value
            }
            BuffTarget.FIRE_RATE -> {
                val op = listOf(MathOp.ADD, MathOp.SUBTRACT, MathOp.MULTIPLY, MathOp.DIVIDE).random(random)
                val value = when (op) {
                    MathOp.MULTIPLY, MathOp.DIVIDE -> 2f
                    else -> (1..2).random(random).toFloat()
                }
                op to value
            }
            BuffTarget.DAMAGE -> {
                val op = listOf(MathOp.ADD, MathOp.SUBTRACT, MathOp.MULTIPLY, MathOp.DIVIDE).random(random)
                val value = when (op) {
                    MathOp.MULTIPLY, MathOp.DIVIDE -> 2f
                    else -> (2..6).random(random).toFloat()
                }
                op to value
            }
        }
    }
}
