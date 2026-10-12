package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.Vec2

/**
 * Steers every enemy each tick. Every current [com.baraka.game.knightsurvivor.model.EnemyKind]
 * shares the same "walk straight at the nearest target" behavior, modulated by stun/slow
 * debuffs and Mimic's disguise — a future kind needing a genuinely different AI (e.g. a
 * ranged enemy that keeps its distance) adds one `when(enemy.kind)` branch here, without
 * touching spawning, collision, or rendering.
 *
 * [targets] is a list rather than a single position so a co-op partner's knight is just
 * another target to steer toward/wake near — solo play always passes a one-element list.
 */
object EnemyMovementSystem {

    fun step(enemies: List<EnemyInstance>, targets: List<Vec2>, nowMs: Long, dtSeconds: Float): List<EnemyInstance> =
        enemies.map { enemy -> stepOne(enemy, targets, nowMs, dtSeconds) }

    private fun stepOne(enemy: EnemyInstance, targets: List<Vec2>, nowMs: Long, dtSeconds: Float): EnemyInstance {
        val nearestTarget = nearestOf(targets, enemy.position) ?: return enemy

        if (enemy.disguisedAsPickup) {
            val hasWokenUp = nearestTarget.distanceTo(enemy.position) <= MIMIC_TRIGGER_RADIUS_PX
            return if (hasWokenUp) enemy.copy(disguisedAsPickup = false) else enemy
        }

        if (nowMs < enemy.stunnedUntilMs) return enemy

        val effectiveSpeed = if (nowMs < enemy.slowUntilMs) enemy.speed * enemy.slowMultiplier else enemy.speed
        val direction = (nearestTarget - enemy.position).normalized()
        return enemy.copy(position = enemy.position + direction * effectiveSpeed * dtSeconds)
    }

    private fun nearestOf(targets: List<Vec2>, from: Vec2): Vec2? = targets.minByOrNull { it.distanceTo(from) }

    private const val MIMIC_TRIGGER_RADIUS_PX = 70f
}
