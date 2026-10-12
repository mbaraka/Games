package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.Projectile
import com.baraka.game.knightsurvivor.model.Vec2

/**
 * Moves projectiles and drops any that have drifted outside the arena. Never applies
 * damage — see CollisionSystem.
 *
 * Most projectiles just travel in a straight line until they leave the arena. A
 * [com.baraka.game.knightsurvivor.model.WeaponKind.returnsToPlayer] projectile (Boomerang)
 * is the one exception: past [BOOMERANG_RETURN_RANGE_PX] it turns around and homes back
 * toward whichever player fired it ([Projectile.ownerIsPartner]), so it reads as "flew out
 * and came back" rather than just despawning. [partnerPosition] is null in solo play.
 */
object ProjectileMovementSystem {

    fun step(projectiles: List<Projectile>, arenaSize: ArenaSize, hostPosition: Vec2, partnerPosition: Vec2?, dtSeconds: Float): List<Projectile> {
        val margin = GameTunables.PROJECTILE_DESPAWN_MARGIN
        return projectiles
            .map { projectile -> advance(projectile, homeOf(projectile, hostPosition, partnerPosition), dtSeconds) }
            .filter { projectile ->
                val homePosition = homeOf(projectile, hostPosition, partnerPosition)
                val withinBounds = projectile.position.x >= -margin && projectile.position.x <= arenaSize.width + margin &&
                    projectile.position.y >= -margin && projectile.position.y <= arenaSize.height + margin
                val reachedHome = projectile.returning && projectile.position.distanceTo(homePosition) <= BOOMERANG_CATCH_RADIUS_PX
                withinBounds && !reachedHome
            }
    }

    private fun homeOf(projectile: Projectile, hostPosition: Vec2, partnerPosition: Vec2?): Vec2 =
        if (projectile.ownerIsPartner) partnerPosition ?: hostPosition else hostPosition

    private fun advance(projectile: Projectile, homePosition: Vec2, dtSeconds: Float): Projectile {
        val stepDistance = projectile.velocity.length() * dtSeconds
        val movedPosition = projectile.position + projectile.velocity * dtSeconds

        if (!projectile.sourceWeapon.returnsToPlayer) {
            return projectile.copy(position = movedPosition)
        }

        val traveled = projectile.distanceTraveledPx + stepDistance
        if (!projectile.returning && traveled >= BOOMERANG_RETURN_RANGE_PX) {
            // Turn around: home toward the owner's current position and let it hit the
            // same enemies again on the way back.
            val homingVelocity = (homePosition - movedPosition).normalized() * projectile.velocity.length()
            return projectile.copy(position = movedPosition, velocity = homingVelocity, returning = true, hitEnemyIds = emptySet())
        }

        if (projectile.returning) {
            val homingVelocity = (homePosition - movedPosition).normalized() * projectile.velocity.length()
            return projectile.copy(position = movedPosition, velocity = homingVelocity, distanceTraveledPx = traveled)
        }

        return projectile.copy(position = movedPosition, distanceTraveledPx = traveled)
    }

    private const val BOOMERANG_RETURN_RANGE_PX = 220f
    private const val BOOMERANG_CATCH_RADIUS_PX = 24f
}
