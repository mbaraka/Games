package com.baraka.game.asteroids.systems

import com.baraka.game.asteroids.model.AsteroidsState
import com.baraka.game.asteroids.model.AsteroidsState.Companion.BULLET_LIFETIME_SECONDS
import com.baraka.game.asteroids.model.AsteroidsState.Companion.BULLET_SPEED
import com.baraka.game.asteroids.model.AsteroidsState.Companion.DRAG_PER_SECOND
import com.baraka.game.asteroids.model.AsteroidsState.Companion.FIRE_COOLDOWN_SECONDS
import com.baraka.game.asteroids.model.AsteroidsState.Companion.INVINCIBLE_SECONDS
import com.baraka.game.asteroids.model.AsteroidsState.Companion.MAX_SHIP_SPEED
import com.baraka.game.asteroids.model.AsteroidsState.Companion.ROTATE_SPEED_RAD
import com.baraka.game.asteroids.model.AsteroidsState.Companion.SHIP_RADIUS
import com.baraka.game.asteroids.model.AsteroidsState.Companion.THRUST_ACCEL
import com.baraka.game.asteroids.model.Bullet
import com.baraka.game.asteroids.model.Difficulty
import com.baraka.game.asteroids.model.GamePhase
import com.baraka.game.asteroids.model.Ship
import com.baraka.game.asteroids.model.wrapped
import com.baraka.game.asteroids.model.wrappedDelta
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Advances an [AsteroidsState] by one frame: same "pure `tick()` over immutable state" shape
 * as the other Atari-style games here, except this arena is continuous physics on a wrapping
 * torus rather than a lane/grid/formation. [rotatingLeft]/[rotatingRight]/[thrusting] are
 * live button-held flags passed in fresh every call (not stored in state — they're input,
 * not game state); [fire] is the one discrete action.
 */
object AsteroidsLoop {

    private const val MAX_DT = 0.034f

    fun newGame(difficulty: Difficulty, bestScore: Int, rng: Random): AsteroidsState {
        val base = AsteroidsState(difficulty = difficulty, lives = difficulty.startingLives, bestScoreAtStart = bestScore)
        return beginWave(base, wave = 1, rng)
    }

    fun startWave(state: AsteroidsState, wave: Int, rng: Random): AsteroidsState = beginWave(state, wave, rng)

    private fun beginWave(state: AsteroidsState, wave: Int, rng: Random): AsteroidsState {
        val (asteroids, nextId) = AsteroidField.startingWave(wave, state.nextAsteroidId, state.difficulty.asteroidSpeedMultiplier, rng)
        return state.copy(
            wave = wave,
            ship = Ship(invincibleSeconds = INVINCIBLE_SECONDS),
            asteroids = asteroids,
            bullets = emptyList(),
            nextAsteroidId = nextId,
            fireCooldownSeconds = 0f,
            phase = GamePhase.RUNNING
        )
    }

    fun fire(state: AsteroidsState): AsteroidsState {
        if (state.phase != GamePhase.RUNNING || state.fireCooldownSeconds > 0f) return state
        val ship = state.ship
        val bullet = Bullet(
            id = state.nextBulletId,
            x = ship.x, y = ship.y,
            vx = cos(ship.angleRad) * BULLET_SPEED, vy = sin(ship.angleRad) * BULLET_SPEED,
            lifeSecondsLeft = BULLET_LIFETIME_SECONDS
        )
        return state.copy(
            bullets = state.bullets + bullet,
            nextBulletId = state.nextBulletId + 1,
            fireCooldownSeconds = FIRE_COOLDOWN_SECONDS,
            firedThisTick = true
        )
    }

    fun tick(
        state: AsteroidsState,
        deltaSeconds: Float,
        rotatingLeft: Boolean,
        rotatingRight: Boolean,
        thrusting: Boolean,
        rng: Random
    ): AsteroidsState {
        if (state.phase != GamePhase.RUNNING) return state
        val dt = deltaSeconds.coerceIn(0f, MAX_DT)

        // --- Ship ---
        var ship = state.ship
        var angle = ship.angleRad
        if (rotatingLeft) angle -= ROTATE_SPEED_RAD * dt
        if (rotatingRight) angle += ROTATE_SPEED_RAD * dt

        var vx = ship.vx
        var vy = ship.vy
        if (thrusting) {
            vx += cos(angle) * THRUST_ACCEL * dt
            vy += sin(angle) * THRUST_ACCEL * dt
            val speed = hypot(vx, vy)
            if (speed > MAX_SHIP_SPEED) {
                vx = vx / speed * MAX_SHIP_SPEED
                vy = vy / speed * MAX_SHIP_SPEED
            }
        }
        val drag = (1f - DRAG_PER_SECOND * dt).coerceAtLeast(0f)
        vx *= drag
        vy *= drag

        ship = ship.copy(
            x = (ship.x + vx * dt).wrapped(),
            y = (ship.y + vy * dt).wrapped(),
            angleRad = angle,
            vx = vx, vy = vy,
            invincibleSeconds = (ship.invincibleSeconds - dt).coerceAtLeast(0f)
        )
        val fireCooldown = (state.fireCooldownSeconds - dt).coerceAtLeast(0f)

        // --- Asteroids drift (pure movement; splitting happens on bullet impact below) ---
        var asteroids = state.asteroids.map {
            it.copy(
                x = (it.x + it.vx * dt).wrapped(),
                y = (it.y + it.vy * dt).wrapped(),
                rotation = it.rotation + it.rotationSpeed * dt
            )
        }

        // --- Bullets: move, age out, and resolve against asteroids ---
        var score = state.score
        var destroyed = 0
        var nextAsteroidId = state.nextAsteroidId
        val survivingBullets = mutableListOf<Bullet>()
        for (bullet in state.bullets) {
            val life = bullet.lifeSecondsLeft - dt
            if (life <= 0f) continue
            val nx = (bullet.x + bullet.vx * dt).wrapped()
            val ny = (bullet.y + bullet.vy * dt).wrapped()

            val hitIndex = asteroids.indexOfFirst { a ->
                val dx = wrappedDelta(nx, a.x)
                val dy = wrappedDelta(ny, a.y)
                hypot(dx, dy) <= a.size.radius
            }
            if (hitIndex >= 0) {
                val hit = asteroids[hitIndex]
                score += hit.size.points
                destroyed += 1
                val (children, newNextId) = AsteroidField.split(hit, nextAsteroidId, state.difficulty.asteroidSpeedMultiplier, rng)
                nextAsteroidId = newNextId
                asteroids = asteroids.toMutableList().also { it.removeAt(hitIndex) } + children
                // bullet consumed, not added to survivors
            } else {
                survivingBullets += bullet.copy(x = nx, y = ny, lifeSecondsLeft = life)
            }
        }

        // --- Ship vs asteroids ---
        var lives = state.lives
        var shipDestroyed = false
        if (ship.invincibleSeconds <= 0f) {
            val collided = asteroids.any { a ->
                val dx = wrappedDelta(ship.x, a.x)
                val dy = wrappedDelta(ship.y, a.y)
                hypot(dx, dy) <= a.size.radius + SHIP_RADIUS
            }
            if (collided) {
                lives = (lives - 1).coerceAtLeast(0)
                shipDestroyed = true
                ship = Ship(invincibleSeconds = INVINCIBLE_SECONDS)
            }
        }

        val nextPhase = when {
            lives <= 0 -> GamePhase.GAME_OVER
            asteroids.isEmpty() -> GamePhase.WAVE_CLEARED
            else -> GamePhase.RUNNING
        }

        return state.copy(
            ship = ship,
            asteroids = asteroids,
            bullets = survivingBullets,
            fireCooldownSeconds = fireCooldown,
            lives = lives,
            score = score,
            nextAsteroidId = nextAsteroidId,
            phase = nextPhase,
            firedThisTick = false,
            asteroidsDestroyedThisTick = destroyed,
            shipDestroyedThisTick = shipDestroyed
        )
    }
}
