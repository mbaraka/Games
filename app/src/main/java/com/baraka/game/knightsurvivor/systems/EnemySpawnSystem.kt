package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.Difficulty
import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.EnemyKind
import com.baraka.game.knightsurvivor.model.EntityIdGenerator
import com.baraka.game.knightsurvivor.model.Vec2
import kotlin.random.Random

/**
 * Decides WHEN, WHERE, and HOW STRONG new enemies are, reading purely from
 * balance/GameTunables.kt's curves/weights/thresholds, stretched or compressed by the
 * run's [Difficulty]. Never decides enemy movement AI (see EnemyMovementSystem) or how
 * they're drawn (see render/EnemyRenderer.kt).
 */
object EnemySpawnSystem {

    /**
     * Regular spawns are gated by [GameTunables.spawnIntervalMsAt] (scaled by
     * [Difficulty.spawnIntervalMultiplier]); a boss is added every
     * [GameTunables.BOSS_SPAWN_INTERVAL_MS]. Pass the tick's *previous* and *new* elapsed
     * time so a threshold crossed within this one tick is detected exactly once.
     */
    fun step(
        currentEnemyCount: Int,
        arenaSize: ArenaSize,
        previousElapsedMs: Long,
        newElapsedMs: Long,
        difficulty: Difficulty,
        random: Random = Random.Default
    ): List<EnemyInstance> {
        if (currentEnemyCount >= GameTunables.MAX_LIVE_ENEMIES) return emptyList()

        val spawned = mutableListOf<EnemyInstance>()
        val elapsedSeconds = newElapsedMs / 1000f

        if (crossedInterval(previousElapsedMs, newElapsedMs, GameTunables.BOSS_SPAWN_INTERVAL_MS)) {
            EnemyKind.BOSSES.randomOrNull(random)?.let { bossKind ->
                spawned += spawnEnemy(bossKind, elapsedSeconds, arenaSize, difficulty, random)
            }
        }

        val regularIntervalMs = (GameTunables.spawnIntervalMsAt(elapsedSeconds) * difficulty.spawnIntervalMultiplier).toLong()
        if (crossedInterval(previousElapsedMs, newElapsedMs, regularIntervalMs)) {
            val kind = pickWeightedRegularKind(elapsedSeconds, random)
            spawned += spawnEnemy(kind, elapsedSeconds, arenaSize, difficulty, random)
        }

        return spawned
    }

    /** True if a boundary of [intervalMs] falls strictly between previous and new elapsed time. */
    private fun crossedInterval(previousElapsedMs: Long, newElapsedMs: Long, intervalMs: Long): Boolean {
        if (newElapsedMs <= 0L || intervalMs <= 0L) return false
        return (previousElapsedMs / intervalMs) != (newElapsedMs / intervalMs)
    }

    private fun pickWeightedRegularKind(elapsedSeconds: Float, random: Random): EnemyKind {
        val weighted = GameTunables.availableRegularEnemyWeights(elapsedSeconds)
        val totalWeight = weighted.sumOf { it.second.toDouble() }.toFloat()
        var roll = random.nextFloat() * totalWeight
        for ((kind, weight) in weighted) {
            roll -= weight
            if (roll <= 0f) return kind
        }
        return weighted.last().first
    }

    private fun spawnEnemy(kind: EnemyKind, elapsedSeconds: Float, arenaSize: ArenaSize, difficulty: Difficulty, random: Random): EnemyInstance {
        val powerMultiplier = GameTunables.enemyPowerMultiplierAt(elapsedSeconds) * difficulty.enemyPowerMultiplier
        val speedMultiplier = GameTunables.enemySpeedMultiplierAt(elapsedSeconds) * difficulty.enemySpeedMultiplier
        val hp = (kind.baseHp * powerMultiplier).toInt().coerceAtLeast(1)

        return EnemyInstance(
            id = EntityIdGenerator.next(),
            kind = kind,
            position = randomEdgePosition(arenaSize, random),
            hp = hp,
            maxHp = hp,
            speed = kind.baseSpeed * speedMultiplier,
            damage = (kind.baseDamage * powerMultiplier).toInt().coerceAtLeast(1),
            disguisedAsPickup = kind.disguisesAsPickup
        )
    }

    private fun randomEdgePosition(arenaSize: ArenaSize, random: Random): Vec2 {
        val margin = GameTunables.SPAWN_EDGE_MARGIN
        return when (random.nextInt(4)) {
            0 -> Vec2(random.nextFloat() * arenaSize.width, -margin) // top
            1 -> Vec2(random.nextFloat() * arenaSize.width, arenaSize.height + margin) // bottom
            2 -> Vec2(-margin, random.nextFloat() * arenaSize.height) // left
            else -> Vec2(arenaSize.width + margin, random.nextFloat() * arenaSize.height) // right
        }
    }
}
