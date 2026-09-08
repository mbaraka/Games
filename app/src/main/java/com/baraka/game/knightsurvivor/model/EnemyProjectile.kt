package com.baraka.game.knightsurvivor.model

/**
 * A travelling attack fired BY an enemy AT the player (see [EnemyKind.hasRangedAttack]) —
 * the mirror image of [Projectile], which always travels from the player outward. Kept as
 * a separate type rather than overloading [Projectile] since the two are never resolved
 * against the same targets (this one only ever hits the player, in
 * systems/CollisionSystem.kt) and carry no [WeaponKind] to render with.
 */
data class EnemyProjectile(
    val id: Long,
    val position: Vec2,
    val velocity: Vec2,
    val damage: Int,
    val colorArgb: Long
)
