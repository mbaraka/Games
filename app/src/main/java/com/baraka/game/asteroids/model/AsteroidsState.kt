package com.baraka.game.asteroids.model

enum class GamePhase { RUNNING, WAVE_CLEARED, GAME_OVER }

enum class AsteroidSize(val radius: Float, val points: Int) {
    LARGE(0.070f, 20),
    MEDIUM(0.045f, 50),
    SMALL(0.026f, 100)
}

data class Ship(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val angleRad: Float = -Math.PI.toFloat() / 2f, // pointing "up"
    val vx: Float = 0f,
    val vy: Float = 0f,
    val invincibleSeconds: Float = 0f
)

/** [shapeSeed] drives a fixed jagged outline (see render/AsteroidsRenderer.kt) so the same rock doesn't re-roll its silhouette every frame; [rotation] just animates for flavor and has no gameplay effect. */
data class Asteroid(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: AsteroidSize,
    val shapeSeed: Int
)

data class Bullet(val id: Long, val x: Float, val y: Float, val vx: Float, val vy: Float, val lifeSecondsLeft: Float)

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/AsteroidsLoop.kt's `tick()` one copy at a time — same "pure tick over immutable
 * state" shape as the other Atari-style games here. Unlike those, this one is a true
 * continuous-physics arena rather than a lane/grid/formation, and both axes wrap (a ship or
 * rock that exits one edge re-enters the opposite one) — see [wrapped].
 *
 * Coordinates are a single normalized 0..1 *square*, not independently-scaled axes like the
 * other games: a rotating ship/asteroid would read as stretched under non-uniform scaling,
 * so render/AsteroidsRenderer.kt fits this square into the canvas with one shared scale and
 * letterboxes the rest — same trick snake's board uses for square grid cells.
 */
data class AsteroidsState(
    val ship: Ship = Ship(),
    val asteroids: List<Asteroid> = emptyList(),
    val bullets: List<Bullet> = emptyList(),
    val fireCooldownSeconds: Float = 0f,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val lives: Int = difficulty.startingLives,
    val score: Int = 0,
    val wave: Int = 1,
    val phase: GamePhase = GamePhase.RUNNING,
    val bestScoreAtStart: Int = 0,
    val nextAsteroidId: Long = 0L,
    val nextBulletId: Long = 0L,
    // Transient per-tick/per-action event flags — read once by ui/AsteroidsScreen.kt to
    // trigger a sound cue, then discarded. Not persisted gameplay data.
    val firedThisTick: Boolean = false,
    val asteroidsDestroyedThisTick: Int = 0,
    val shipDestroyedThisTick: Boolean = false
) {
    val isNewBest: Boolean get() = score > bestScoreAtStart

    companion object {
        const val ROTATE_SPEED_RAD = 3.3f
        const val THRUST_ACCEL = 0.55f
        const val MAX_SHIP_SPEED = 0.42f
        const val DRAG_PER_SECOND = 0.6f
        const val SHIP_RADIUS = 0.022f
        const val INVINCIBLE_SECONDS = 1.6f

        const val BULLET_SPEED = 0.75f
        const val BULLET_LIFETIME_SECONDS = 0.9f
        const val FIRE_COOLDOWN_SECONDS = 0.28f

        const val STARTING_LARGE_ASTEROIDS = 3
    }
}

/** Wraps a logical coordinate back into 0..1 — the arena is a torus, not a bounded box. */
fun Float.wrapped(): Float = ((this % 1f) + 1f) % 1f

/** Shortest signed distance from this coordinate to [other] accounting for wraparound — used for collision checks so a rock half-offscreen on one edge still correctly hits a ship near the opposite edge. */
fun wrappedDelta(a: Float, b: Float): Float {
    var d = a - b
    if (d > 0.5f) d -= 1f
    if (d < -0.5f) d += 1f
    return d
}
