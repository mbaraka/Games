package com.baraka.game.knightsurvivor.model

/**
 * A weapon the player currently owns: which [WeaponKind] archetype, how upgraded it is,
 * and its live cooldown/orbit-angle state.
 *
 * Every weapon (regardless of [WeaponKind.targeting]) shares one simple activation model:
 * [cooldownRemainingMs] ticks down every frame; when it reaches zero the weapon "fires"
 * (see systems/WeaponFiringSystem.kt for what that means per targeting kind) and resets to
 * `kind.cooldownMsAtLevel(level)`. [orbitAngleRadians] is only used by ORBIT weapons and
 * rotates continuously regardless of cooldown, purely for a smooth visual spin.
 */
data class WeaponInstance(
    val kind: WeaponKind,
    val level: Int = 1,
    val cooldownRemainingMs: Long = 0L,
    val orbitAngleRadians: Float = 0f
)
