package com.baraka.game.missilecommand.model

enum class GamePhase { RUNNING, WAVE_CLEARED, GAME_OVER }

data class City(val id: Int, val x: Float, val alive: Boolean = true)
data class Silo(val id: Int, val x: Float, val alive: Boolean = true)

enum class TargetKind { CITY, SILO, GROUND }

/** A falling enemy missile — [x]/[y] is its current position, [vx]/[vy] its fixed velocity toward ([targetX], [targetY]) set once at spawn. [targetKind]/[targetId] say what to damage on impact. [fast] ones are worth more and are harder to react to. */
data class EnemyMissile(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val targetX: Float,
    val targetY: Float,
    val targetKind: TargetKind,
    val targetId: Int,
    val fast: Boolean
)

/** A player-fired interceptor climbing toward ([targetX], [targetY]), where it detonates into an [Explosion] on arrival. */
data class Interceptor(val id: Long, val x: Float, val y: Float, val vx: Float, val vy: Float, val targetX: Float, val targetY: Float)

/** [ageSeconds] drives both the grow-then-shrink radius curve and the fade-out — see render/MissileCommandRenderer.kt. */
data class Explosion(val id: Long, val x: Float, val y: Float, val ageSeconds: Float = 0f)

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/MissileCommandLoop.kt's `tick()` one copy at a time — same "pure tick over
 * immutable state" shape as breakout's BreakoutState and spaceinvaders' SpaceInvadersState.
 * Field coordinates are normalized 0..1 independently on each axis (see BreakoutState's doc
 * comment for why).
 */
data class MissileCommandState(
    val cities: List<City> = emptyList(),
    val silos: List<Silo> = emptyList(),
    val enemyMissiles: List<EnemyMissile> = emptyList(),
    val interceptors: List<Interceptor> = emptyList(),
    val explosions: List<Explosion> = emptyList(),
    val ammo: Int = 0,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val score: Int = 0,
    val wave: Int = 1,
    val missilesToSpawnThisWave: Int = 0,
    val missilesSpawnedThisWave: Int = 0,
    val nextSpawnInSeconds: Float = 0f,
    val phase: GamePhase = GamePhase.RUNNING,
    val bestScoreAtStart: Int = 0,
    val nextMissileId: Long = 0L,
    val nextInterceptorId: Long = 0L,
    val nextExplosionId: Long = 0L,
    // Transient per-tick/per-action event flags — read once by ui/MissileCommandScreen.kt to
    // trigger a sound cue, then discarded. Not persisted gameplay data.
    val launchedThisTick: Boolean = false,
    val detonatedThisTick: Int = 0,
    val missilesDestroyedThisTick: Int = 0,
    val cityLostThisTick: Boolean = false,
    val siloLostThisTick: Boolean = false
) {
    val isNewBest: Boolean get() = score > bestScoreAtStart
    val citiesRemaining: Int get() = cities.count { it.alive }

    companion object {
        const val CITY_COUNT = 6
        const val SILO_COUNT = 3
        const val GROUND_Y = 0.90f
        const val CITY_HALF_WIDTH = 0.032f
        const val SILO_HALF_WIDTH = 0.028f

        const val BASE_ENEMY_SPEED = 0.085f // logical units/second
        const val FAST_ENEMY_SPEED_MULTIPLIER = 1.6f
        const val FAST_MISSILE_CHANCE = 0.22f

        const val INTERCEPTOR_SPEED = 0.95f
        const val ARRIVAL_EPSILON = 0.02f

        const val EXPLOSION_LIFETIME_SECONDS = 0.75f
        const val EXPLOSION_MAX_RADIUS = 0.055f

        const val BASE_AMMO_PER_WAVE = 12
        const val CITY_BONUS_SCORE = 50
        const val AMMO_BONUS_SCORE = 10
        const val ENEMY_KILL_SCORE = 25
        const val FAST_ENEMY_KILL_SCORE = 40
    }
}

/** Grows over the first 35% of the explosion's life, then shrinks back to nothing — a single curve shared by the game loop's collision check and the renderer's draw call. */
fun explosionRadius(explosion: Explosion): Float {
    val t = (explosion.ageSeconds / MissileCommandState.EXPLOSION_LIFETIME_SECONDS).coerceIn(0f, 1f)
    val shape = if (t < 0.35f) t / 0.35f else 1f - (t - 0.35f) / 0.65f
    return MissileCommandState.EXPLOSION_MAX_RADIUS * shape.coerceIn(0f, 1f)
}
