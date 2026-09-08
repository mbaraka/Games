package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.EnemyProjectile

/** Moves enemy-fired projectiles (Imp) and drops any that have drifted outside the arena — mirrors ProjectileMovementSystem for the player's own projectiles. */
object EnemyProjectileMovementSystem {

    fun step(projectiles: List<EnemyProjectile>, arenaSize: ArenaSize, dtSeconds: Float): List<EnemyProjectile> {
        val margin = GameTunables.PROJECTILE_DESPAWN_MARGIN
        return projectiles
            .map { it.copy(position = it.position + it.velocity * dtSeconds) }
            .filter { projectile ->
                projectile.position.x >= -margin && projectile.position.x <= arenaSize.width + margin &&
                    projectile.position.y >= -margin && projectile.position.y <= arenaSize.height + margin
            }
    }
}
