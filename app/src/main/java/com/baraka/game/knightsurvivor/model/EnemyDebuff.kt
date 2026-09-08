package com.baraka.game.knightsurvivor.model

/**
 * A temporary status effect a weapon hit applies to one enemy — stun (Frying Pan) and/or
 * slow (Egg Toss splash). Kept as one type rather than two separate maps so
 * systems/GameLoopOrchestrator.kt has a single place to merge "what debuffs landed this
 * tick" before folding them onto [EnemyInstance].
 */
data class EnemyDebuff(
    val stunUntilMs: Long = 0L,
    val slowUntilMs: Long = 0L,
    val slowMultiplier: Float = 1f
)
