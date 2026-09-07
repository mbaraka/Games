package com.baraka.game.knightsurvivor.model

import kotlin.math.sqrt

/**
 * Minimal 2D vector used by every model/systems/balance file in this game.
 *
 * Deliberately NOT androidx.compose.ui.geometry.Offset: keeping the whole
 * gameplay-logic core (model/systems/balance) free of Android/Compose imports
 * means it stays plain-Kotlin and could be unit-tested with no Robolectric or
 * instrumentation, even though this build skips tests for now. Only the
 * render/ and ui/ layers convert Vec2 <-> Offset, at the Canvas boundary.
 */
data class Vec2(val x: Float, val y: Float) {

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vec2 = if (scalar != 0f) Vec2(x / scalar, y / scalar) else ZERO

    fun length(): Float = sqrt(x * x + y * y)

    fun distanceTo(other: Vec2): Float = (this - other).length()

    /** Returns a unit vector in the same direction, or ZERO if this vector has no length. */
    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0.0001f) Vec2(x / len, y / len) else ZERO
    }

    /** Clamps this point so it stays within [0, width] x [0, height], inset by [margin] on all sides. */
    fun clampToArena(width: Float, height: Float, margin: Float): Vec2 = Vec2(
        x = x.coerceIn(margin, (width - margin).coerceAtLeast(margin)),
        y = y.coerceIn(margin, (height - margin).coerceAtLeast(margin))
    )

    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}
