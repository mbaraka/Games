package com.baraka.game.knightsurvivor.model

/**
 * XP dropped by a dead enemy. [magnetized] flips on once the player's pickup radius
 * reaches it (see systems/XpAndLevelingSystem.kt), after which it accelerates toward the
 * player instead of sitting still.
 */
data class XpGem(
    val id: Long,
    val position: Vec2,
    val value: Int,
    val magnetized: Boolean = false
)
