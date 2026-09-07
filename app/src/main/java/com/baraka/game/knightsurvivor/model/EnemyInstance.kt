package com.baraka.game.knightsurvivor.model

/**
 * A live enemy in the arena. [kind] carries the archetype's base stats/appearance;
 * everything here is just this particular instance's mutable-in-spirit (copy()'d each
 * tick) runtime state.
 */
data class EnemyInstance(
    val id: Long,
    val kind: EnemyKind,
    val position: Vec2,
    val hp: Int,
    val maxHp: Int,
    val speed: Float,
    val damage: Int,
    val hitFlashUntilMs: Long = 0L
) {
    val radius: Float get() = kind.radius
    val isBoss: Boolean get() = kind.isBoss
}
