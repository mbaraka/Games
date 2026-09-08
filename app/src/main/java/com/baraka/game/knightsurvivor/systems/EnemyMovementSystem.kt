package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.Vec2

/**
 * Steers every enemy each tick. Every current [com.baraka.game.knightsurvivor.model.EnemyKind]
 * shares the same "walk straight at the player" behavior, modulated by stun/slow debuffs
 * and Mimic's disguise — a future kind needing a genuinely different AI (e.g. a ranged
 * enemy that keeps its distance) adds one `when(enemy.kind)` branch here, without touching
 * spawning, collision, or rendering.
 */
object EnemyMovementSystem {

    fun step(enemies: List<EnemyInstance>, playerPosition: Vec2, nowMs: Long, dtSeconds: Float): List<EnemyInstance> =
        enemies.map { enemy -> stepOne(enemy, playerPosition, nowMs, dtSeconds) }

    private fun stepOne(enemy: EnemyInstance, playerPosition: Vec2, nowMs: Long, dtSeconds: Float): EnemyInstance {
        if (enemy.disguisedAsPickup) {
            val hasWokenUp = playerPosition.distanceTo(enemy.position) <= MIMIC_TRIGGER_RADIUS_PX
            return if (hasWokenUp) enemy.copy(disguisedAsPickup = false) else enemy
        }

        if (nowMs < enemy.stunnedUntilMs) return enemy

        val effectiveSpeed = if (nowMs < enemy.slowUntilMs) enemy.speed * enemy.slowMultiplier else enemy.speed
        val direction = (playerPosition - enemy.position).normalized()
        return enemy.copy(position = enemy.position + direction * effectiveSpeed * dtSeconds)
    }

    private const val MIMIC_TRIGGER_RADIUS_PX = 70f
}
