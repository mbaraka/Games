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
    val hitFlashUntilMs: Long = 0L,
    /** While > nowMs, EnemyMovementSystem holds this enemy in place — see WeaponKind.stunChance (Frying Pan). */
    val stunnedUntilMs: Long = 0L,
    /** While > nowMs, EnemyMovementSystem applies [slowMultiplier] to this enemy's speed — see WeaponKind.splashSlowDurationMs (Egg Toss). */
    val slowUntilMs: Long = 0L,
    val slowMultiplier: Float = 1f,
    /** Disguised as a health pickup until the player wanders close (Mimic only) — see EnemyMovementSystem. Doesn't move or deal contact damage while true. */
    val disguisedAsPickup: Boolean = false,
    /** Ticks down independently of weapon cooldowns; an enemy with [EnemyKind.hasRangedAttack] fires at 0 (Imp) — see systems/EnemyRangedAttackSystem.kt. */
    val rangedAttackCooldownMs: Long = 0L
) {
    val radius: Float get() = kind.radius
    val isBoss: Boolean get() = kind.isBoss

    /** [EnemyKind.phasesInvulnerable] enemies (Ghost) cycle vulnerable/invulnerable on a fixed period, offset per-instance by [id] so a whole wave doesn't phase in lockstep. */
    fun isPhaseInvulnerableAt(nowMs: Long): Boolean {
        if (!kind.phasesInvulnerable) return false
        val cyclePosition = (nowMs + id * PHASE_OFFSET_MS) % PHASE_PERIOD_MS
        return cyclePosition >= PHASE_PERIOD_MS / 2
    }

    private companion object {
        const val PHASE_PERIOD_MS = 2000L
        const val PHASE_OFFSET_MS = 173L
    }
}
