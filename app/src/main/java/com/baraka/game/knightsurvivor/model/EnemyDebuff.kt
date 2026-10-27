package com.baraka.game.knightsurvivor.model

/**
 * A temporary per-enemy effect a weapon hit applies — stun (Frying Pan), slow (Egg Toss
 * splash), and/or the next time an ORBIT weapon may damage this enemy again (Orbit Shield).
 * Kept as one type rather than several separate maps so systems/GameLoopOrchestrator.kt has
 * a single place to merge "what landed on this enemy this tick" before folding it onto
 * [EnemyInstance].
 */
data class EnemyDebuff(
    val stunUntilMs: Long = 0L,
    val slowUntilMs: Long = 0L,
    val slowMultiplier: Float = 1f,
    val orbitHitUntilMs: Long = 0L
)
