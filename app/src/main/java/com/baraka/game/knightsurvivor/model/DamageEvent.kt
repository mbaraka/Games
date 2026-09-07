package com.baraka.game.knightsurvivor.model

/**
 * One instance of damage to be applied to a specific enemy.
 *
 * Produced either by systems/CollisionSystem.kt itself (resolving a projectile hitting an
 * enemy) or handed to it by systems/WeaponFiringSystem.kt (for AREA_AROUND_PLAYER/ORBIT
 * weapons, which have no travelling projectile to collide). Funnelling both sources
 * through CollisionSystem keeps "damage is applied / enemies die / XP drops" logic in
 * exactly one place, regardless of which weapon shape caused it.
 */
data class DamageEvent(
    val targetEnemyId: Long,
    val amount: Int,
    val sourceWeapon: WeaponKind
)
