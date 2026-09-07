package com.baraka.game.knightsurvivor.model

/**
 * A travelling attack fired by a NEAREST_ENEMY weapon (see [WeaponTargeting]). AoE/orbit
 * weapons never create one of these — they deal [DamageEvent]s directly.
 *
 * No arena is unbounded in this game, so "expire" simply means systems/
 * ProjectileMovementSystem.kt removes anything that drifts outside the arena bounds by a
 * margin; there's no separate lifetime timer to keep in sync with that.
 */
data class Projectile(
    val id: Long,
    val position: Vec2,
    val velocity: Vec2,
    val damage: Int,
    val sourceWeapon: WeaponKind,
    val pierceRemaining: Int = 0,
    val hitEnemyIds: Set<Long> = emptySet()
)
