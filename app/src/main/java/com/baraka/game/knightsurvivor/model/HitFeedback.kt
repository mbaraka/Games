package com.baraka.game.knightsurvivor.model

/**
 * "Something visually notable happened here" — emitted by systems/CollisionSystem.kt for
 * both a regular hit (an enemy took damage and survived) and a death (an enemy's HP
 * reached zero), and turned into an actual [Particle] burst by
 * systems/GameLoopOrchestrator.kt. [colorArgb] is the source enemy's own tint, so the
 * burst reads as "this creature" rather than a generic flash. [isBoss] additionally lets
 * GameLoopOrchestrator track a boss kill for systems/AchievementSystem.kt without a
 * separate event type.
 */
data class HitFeedback(val position: Vec2, val colorArgb: Long, val isBoss: Boolean = false)
