package com.baraka.game.knightsurvivor.model

/**
 * A data-driven silhouette descriptor — pure data, no drawing logic (see
 * render/VectorIconRenderer.kt for the dispatcher that actually draws one of these).
 *
 * [EnemyKind] and [WeaponKind] each carry one of these as a plain property, like they
 * already carry a color, so adding a new kind with a new look touches only that enum
 * entry — zero renderer files change, not even a `when` branch.
 *
 * Kept independent of any other Knight-Survivor model type on purpose: this file (and its
 * renderer) could be lifted into a shared package later if another game (e.g.
 * Battleship's ship pieces) wants real silhouettes instead of flat-colored cells.
 */
sealed class VectorIcon {
    data class Polygon(val sides: Int) : VectorIcon()
    data class Diamond(val aspectRatio: Float = 1f) : VectorIcon()
    data class Star(val points: Int, val innerRadiusRatio: Float = 0.5f) : VectorIcon()

    /** A heater-shield silhouette — the Knight's own body, and any weapon that should visually read as "a shield." */
    data object Shield : VectorIcon()

    /** A bladed weapon: pointed blade, crossguard, grip, and pommel — not just an elongated diamond. [lengthRatio] (relative to radius) differentiates a short dagger from a longer sword. */
    data class Blade(val lengthRatio: Float = 2.2f) : VectorIcon()

    /** A handle with a crescent axe-head — distinct from [Blade] so a thrown axe doesn't just look like another dagger. */
    data class Axe(val lengthRatio: Float = 1.6f) : VectorIcon()

    /** A heart — health pickups, so they read as "heal" instead of another gem. */
    data object Heart : VectorIcon()

    /** A classic bath-toy rubber duck — round body, round head, small beak. Purely a fun/cute weapon. */
    data object Duck : VectorIcon()

    /**
     * A simple creature face: a round head plus optional features. New monsters can
     * usually be described by mixing these flags rather than needing a whole new
     * hand-drawn shape — pointy ears alone already reads as goblin-ish, add tusks for an
     * orc, hollow sockets for a skull, a crown on top of that for a boss.
     */
    data class Face(
        val earStyle: EarStyle = EarStyle.NONE,
        val eyeStyle: EyeStyle = EyeStyle.DOT,
        val hasTusks: Boolean = false,
        val hasCrown: Boolean = false
    ) : VectorIcon()

    enum class EarStyle { NONE, POINTY, ROUND }
    enum class EyeStyle { DOT, HOLLOW_SOCKET }
}
