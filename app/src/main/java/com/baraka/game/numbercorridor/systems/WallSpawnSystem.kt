package com.baraka.game.numbercorridor.systems

import com.baraka.game.numbercorridor.model.MathOp
import com.baraka.game.numbercorridor.model.WallKind
import com.baraka.game.numbercorridor.model.WallRow
import com.baraka.game.numbercorridor.model.WallSegment
import kotlin.random.Random

/**
 * Builds a fresh [WallRow] at the horizon (progress 0). Each lane always gets the same kind
 * of tile — see [WallRow]'s KDoc — so this only decides whether a lane has a tile this row
 * and how tough/valuable it is. Difficulty is driven purely by [elapsedSeconds]: enemy hp
 * climbs and lanes fill more often the longer a run goes, mirroring how knightsurvivor's
 * EnemySpawnSystem scales off survival time.
 */
object WallSpawnSystem {

    fun spawnRow(id: Long, elapsedSeconds: Float, random: Random = Random.Default): WallRow {
        val fillChance = (0.55f + elapsedSeconds / 260f).coerceAtMost(0.9f)

        val segments = MutableList<WallSegment?>(WallRow.LANE_COUNT) { null }
        if (random.nextFloat() < fillChance) segments[WallRow.SQUAD_LANE] = squadSegment(random)
        if (random.nextFloat() < fillChance) segments[WallRow.ENEMY_LANE] = enemySegment(elapsedSeconds, random)
        if (random.nextFloat() < fillChance) segments[WallRow.WEAPON_LANE] = weaponCrateSegment(random)

        return WallRow(id = id, progress = 0f, segments = segments)
    }

    private fun squadSegment(random: Random): WallSegment {
        val op = listOf(MathOp.ADD, MathOp.SUBTRACT, MathOp.MULTIPLY, MathOp.DIVIDE).random(random)
        val value = if (op == MathOp.MULTIPLY || op == MathOp.DIVIDE) 2f else 1f
        val hp = random.nextInt(8, 18)
        return WallSegment(WallKind.SquadShift(op, value), hp, hp)
    }

    /** hp scales with time; how many little enemy icons render for it is derived from hp in render/CorridorRenderer.kt. */
    private fun enemySegment(elapsedSeconds: Float, random: Random): WallSegment {
        val base = 15 + (elapsedSeconds * 1.1f).toInt()
        val hp = random.nextInt(base, base + 15)
        val enemyCount = when {
            hp < 30 -> 1
            hp < 55 -> 2
            else -> 3
        }
        return WallSegment(WallKind.Obstacle(enemyCount), hp, hp)
    }

    private fun weaponCrateSegment(random: Random): WallSegment {
        // Mostly upgrades, occasionally a downgrade crate to keep the lane worth watching rather than a free win.
        val delta = if (random.nextFloat() < 0.8f) 1 else -1
        val hp = random.nextInt(8, 18)
        return WallSegment(WallKind.WeaponCrate(delta), hp, hp)
    }
}
