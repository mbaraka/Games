package com.baraka.game.knightsurvivor.model

/**
 * A short-lived visual-only spark, used for weapon-hit feedback and enemy death bursts.
 * Carries no gameplay meaning whatsoever — purely decorative, and safe to drop/cap
 * without affecting anything else, unlike every other list in [GameState].
 */
data class Particle(
    val id: Long,
    val position: Vec2,
    val velocity: Vec2,
    val colorArgb: Long,
    val size: Float,
    val lifeMs: Long,
    val ageMs: Long = 0L
)
