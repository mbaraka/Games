package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.Projectile

/** Moves projectiles and drops any that have drifted outside the arena. Never applies damage — see CollisionSystem. */
object ProjectileMovementSystem {

    fun step(projectiles: List<Projectile>, arenaSize: ArenaSize, dtSeconds: Float): List<Projectile> {
        val margin = GameTunables.PROJECTILE_DESPAWN_MARGIN
        return projectiles
            .map { it.copy(position = it.position + it.velocity * dtSeconds) }
            .filter { projectile ->
                projectile.position.x >= -margin && projectile.position.x <= arenaSize.width + margin &&
                    projectile.position.y >= -margin && projectile.position.y <= arenaSize.height + margin
            }
    }
}
