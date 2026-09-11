package com.baraka.game.numbercorridor.model

/**
 * Fired by a squad avatar in [lane]. Starts at the player's line (progress 1f) and travels
 * toward the horizon (progress 0f) — the mirror image of how [WallRow.progress] advances,
 * so a hit is simply the moment a bullet's progress drops to or below a row's.
 */
data class Bullet(
    val lane: Int,
    val progress: Float
)
