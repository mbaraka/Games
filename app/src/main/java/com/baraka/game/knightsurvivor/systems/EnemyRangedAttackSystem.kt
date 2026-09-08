package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.EnemyProjectile
import com.baraka.game.knightsurvivor.model.EntityIdGenerator
import com.baraka.game.knightsurvivor.model.Vec2

/** Everything one tick of enemy ranged-attack activity produced, for GameLoopOrchestrator to fold into the next GameState. */
data class EnemyRangedAttackResult(
    val enemies: List<EnemyInstance>,
    val newProjectiles: List<EnemyProjectile>
)

/**
 * Mirrors systems/WeaponFiringSystem.kt but for enemies attacking the player instead of
 * the other way around — currently just the Imp ([com.baraka.game.knightsurvivor.model.EnemyKind.hasRangedAttack]).
 * Ticks each ranged enemy's own attack cooldown ([EnemyInstance.rangedAttackCooldownMs])
 * and fires an [EnemyProjectile] at the player when it's ready and the player is within
 * [com.baraka.game.knightsurvivor.model.EnemyKind.rangedAttackRangePx].
 */
object EnemyRangedAttackSystem {

    fun step(enemies: List<EnemyInstance>, playerPosition: Vec2, dtMs: Long): EnemyRangedAttackResult {
        val updatedEnemies = mutableListOf<EnemyInstance>()
        val newProjectiles = mutableListOf<EnemyProjectile>()

        for (enemy in enemies) {
            if (!enemy.kind.hasRangedAttack || enemy.disguisedAsPickup) {
                updatedEnemies += enemy
                continue
            }

            val remainingCooldownMs = enemy.rangedAttackCooldownMs - dtMs
            if (remainingCooldownMs > 0L) {
                updatedEnemies += enemy.copy(rangedAttackCooldownMs = remainingCooldownMs)
                continue
            }

            val distanceToPlayer = enemy.position.distanceTo(playerPosition)
            if (distanceToPlayer > enemy.kind.rangedAttackRangePx) {
                // Ready to fire but out of range — stay at zero so it fires the instant it's back in range.
                updatedEnemies += enemy.copy(rangedAttackCooldownMs = 0L)
                continue
            }

            val direction = (playerPosition - enemy.position).normalized()
            newProjectiles += EnemyProjectile(
                id = EntityIdGenerator.next(),
                position = enemy.position,
                velocity = direction * enemy.kind.rangedProjectileSpeed,
                damage = enemy.kind.rangedAttackDamage,
                colorArgb = enemy.kind.colorArgb
            )
            updatedEnemies += enemy.copy(rangedAttackCooldownMs = enemy.kind.rangedAttackCooldownMs)
        }

        return EnemyRangedAttackResult(updatedEnemies, newProjectiles)
    }
}
