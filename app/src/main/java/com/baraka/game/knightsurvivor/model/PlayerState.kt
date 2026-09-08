package com.baraka.game.knightsurvivor.model

/**
 * The knight. [passiveStacks] and [weapons] are the whole "build" for this run; every
 * derived stat (effective max HP, move speed, pickup radius, cooldown multiplier) is
 * computed on demand from them below, so nothing can desync from what was actually
 * granted by upgrade choices.
 */
data class PlayerState(
    val position: Vec2,
    val radius: Float = PLAYER_RADIUS,
    val hp: Int,
    val baseMaxHp: Int,
    val xp: Int = 0,
    val level: Int = 1,
    val xpToNextLevel: Int,
    val invulnerableUntilMs: Long = 0L,
    /** While > nowMs, PlayerMovementSystem ignores joystick input — Golem's root-on-hit. */
    val rootedUntilMs: Long = 0L,
    val weapons: List<WeaponInstance> = emptyList(),
    val passiveStacks: Map<PassiveKind, Int> = emptyMap()
) {
    fun passiveStackCount(kind: PassiveKind): Int = passiveStacks[kind] ?: 0

    /** Max HP after [PassiveKind.MAX_HP] stacks are applied. */
    val effectiveMaxHp: Int
        get() = baseMaxHp + PassiveKind.MAX_HP.magnitudeAtStacks(passiveStackCount(PassiveKind.MAX_HP)).toInt()

    /** Move-speed multiplier from [PassiveKind.MOVE_SPEED] stacks (1.0 = no bonus). */
    val moveSpeedMultiplier: Float
        get() = PassiveKind.MOVE_SPEED.magnitudeAtStacks(passiveStackCount(PassiveKind.MOVE_SPEED))

    /** Pickup/magnet-radius multiplier from [PassiveKind.PICKUP_RADIUS] stacks (1.0 = no bonus). */
    val pickupRadiusMultiplier: Float
        get() = PassiveKind.PICKUP_RADIUS.magnitudeAtStacks(passiveStackCount(PassiveKind.PICKUP_RADIUS))

    /** Weapon-cooldown multiplier from [PassiveKind.COOLDOWN_REDUCTION] stacks (below 1.0 = faster attacks). */
    val cooldownMultiplier: Float
        get() = PassiveKind.COOLDOWN_REDUCTION.magnitudeAtStacks(passiveStackCount(PassiveKind.COOLDOWN_REDUCTION))

    fun isInvulnerableAt(nowMs: Long): Boolean = nowMs < invulnerableUntilMs

    fun isRootedAt(nowMs: Long): Boolean = nowMs < rootedUntilMs

    fun weaponOf(kind: WeaponKind): WeaponInstance? = weapons.find { it.kind == kind }

    companion object {
        const val PLAYER_RADIUS = 16f
    }
}
