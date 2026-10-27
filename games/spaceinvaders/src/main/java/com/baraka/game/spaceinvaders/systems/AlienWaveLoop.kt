package com.baraka.game.spaceinvaders.systems

import com.baraka.game.spaceinvaders.model.Alien
import com.baraka.game.spaceinvaders.model.Bullet
import com.baraka.game.spaceinvaders.model.Difficulty
import com.baraka.game.spaceinvaders.model.GamePhase
import com.baraka.game.spaceinvaders.model.Shield
import com.baraka.game.spaceinvaders.model.SpaceInvadersState
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.ALIEN_HALF_SIZE
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.BASE_ALIEN_BULLET_SPEED
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.BASE_ALIEN_FIRE_CHANCE_PER_SECOND
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.BASE_ALIEN_SPEED
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.BULLET_HALF_WIDTH
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.DESCENT_STEP
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.INVASION_Y
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.MAX_ALIEN_SPEED
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.PLAYER_BULLET_SPEED
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.PLAYER_FIRE_COOLDOWN_SECONDS
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.PLAYER_HALF_WIDTH
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.PLAYER_INVINCIBLE_SECONDS
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.PLAYER_Y
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.SHIELD_HALF_HEIGHT
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.SHIELD_HALF_WIDTH
import com.baraka.game.spaceinvaders.model.alienX
import com.baraka.game.spaceinvaders.model.alienY
import kotlin.random.Random

/**
 * Advances a [SpaceInvadersState] by one frame: same "pure `tick()` over immutable state"
 * shape as breakout's BreakoutGameLoop, just a marching alien grid instead of a bouncing
 * ball. The formation itself has no per-alien velocity — it's one shared horizontal offset
 * plus a shared descent, exactly like the original: speed is derived each tick from how many
 * aliens are left, so the last few are always the tensest.
 */
object AlienWaveLoop {

    private const val MAX_DT = 0.034f
    private const val MAX_ALIEN_BULLETS = 3

    fun newGame(difficulty: Difficulty, bestScore: Int): SpaceInvadersState =
        startWave(
            SpaceInvadersState(difficulty = difficulty, lives = difficulty.startingLives, bestScoreAtStart = bestScore),
            wave = 1
        )

    fun startWave(state: SpaceInvadersState, wave: Int): SpaceInvadersState = state.copy(
        wave = wave,
        aliens = AlienFormation.build(),
        shields = AlienFormation.freshShields(),
        bullets = emptyList(),
        alienDirection = 1,
        alienFormationOffsetX = 0f,
        alienDescent = 0f,
        playerFireCooldown = 0f,
        phase = GamePhase.RUNNING
    )

    fun movePlayer(state: SpaceInvadersState, x: Float): SpaceInvadersState =
        state.copy(playerX = x.coerceIn(PLAYER_HALF_WIDTH, 1f - PLAYER_HALF_WIDTH))

    fun firePlayerBullet(state: SpaceInvadersState): SpaceInvadersState {
        if (state.phase != GamePhase.RUNNING || state.playerFireCooldown > 0f) return state
        val bullet = Bullet(id = state.nextBulletId, x = state.playerX, y = PLAYER_Y - 0.03f, fromPlayer = true)
        return state.copy(
            bullets = state.bullets + bullet,
            nextBulletId = state.nextBulletId + 1,
            playerFireCooldown = PLAYER_FIRE_COOLDOWN_SECONDS,
            playerShotThisTick = true
        )
    }

    private fun currentAlienSpeed(state: SpaceInvadersState): Float {
        val total = (SpaceInvadersState.ROWS * SpaceInvadersState.COLS).toFloat()
        val remainingFraction = (state.aliveAlienCount / total).coerceIn(0f, 1f)
        val waveFactor = 1f + (state.wave - 1) * 0.12f
        val base = BASE_ALIEN_SPEED + (1f - remainingFraction) * (MAX_ALIEN_SPEED - BASE_ALIEN_SPEED) * 0.8f
        return (base * waveFactor * state.difficulty.alienSpeedMultiplier).coerceAtMost(MAX_ALIEN_SPEED)
    }

    fun tick(state: SpaceInvadersState, deltaSeconds: Float, rng: Random): SpaceInvadersState {
        if (state.phase != GamePhase.RUNNING) return state
        val dt = deltaSeconds.coerceIn(0f, MAX_DT)

        val invincibleLeft = (state.playerInvincibleSeconds - dt).coerceAtLeast(0f)
        val fireCooldownLeft = (state.playerFireCooldown - dt).coerceAtLeast(0f)

        // --- Formation march ---
        val speed = currentAlienSpeed(state)
        var offsetX = state.alienFormationOffsetX + state.alienDirection * speed * dt
        var direction = state.alienDirection
        var descent = state.alienDescent
        var marchStepped = false

        val aliveCols = state.aliens.filter { it.alive }.map { it.col }
        if (aliveCols.isNotEmpty()) {
            val minCol = aliveCols.min()
            val maxCol = aliveCols.max()
            val leftEdge = SpaceInvadersState.ALIEN_START_LEFT + offsetX + minCol * SpaceInvadersState.ALIEN_COL_SPACING - ALIEN_HALF_SIZE
            val rightEdge = SpaceInvadersState.ALIEN_START_LEFT + offsetX + maxCol * SpaceInvadersState.ALIEN_COL_SPACING + ALIEN_HALF_SIZE
            if (leftEdge < 0f || rightEdge > 1f) {
                offsetX = state.alienFormationOffsetX // undo this tick's move, bounce next tick instead
                direction = -direction
                descent += DESCENT_STEP
                marchStepped = true
            }
        }

        // --- Invasion check: an alien reached the player's row ---
        val invaded = state.aliens.any { it.alive && alienY(it.row, descent) + ALIEN_HALF_SIZE >= INVASION_Y }
        if (invaded) {
            return state.copy(phase = GamePhase.GAME_OVER, alienDescent = descent, alienFormationOffsetX = offsetX)
        }

        // --- Alien fire ---
        var bullets = state.bullets.toMutableList()
        var nextBulletId = state.nextBulletId
        val alienBulletsInFlight = bullets.count { !it.fromPlayer }
        if (alienBulletsInFlight < MAX_ALIEN_BULLETS && aliveCols.isNotEmpty()) {
            val waveFactor = 1f + (state.wave - 1) * 0.1f
            val fireChance = BASE_ALIEN_FIRE_CHANCE_PER_SECOND * dt * state.difficulty.alienFireRateMultiplier * waveFactor
            if (rng.nextFloat() < fireChance) {
                val shooterCol = aliveCols.random(rng)
                val shooterRow = state.aliens.filter { it.alive && it.col == shooterCol }.maxOf { it.row }
                val sx = alienX(offsetX, shooterCol)
                val sy = alienY(shooterRow, descent) + ALIEN_HALF_SIZE
                bullets += Bullet(id = nextBulletId++, x = sx, y = sy, fromPlayer = false)
            }
        }

        // --- Move bullets ---
        bullets = bullets.mapNotNull { b ->
            val ny = b.y + (if (b.fromPlayer) -PLAYER_BULLET_SPEED else BASE_ALIEN_BULLET_SPEED) * dt
            if (ny < -0.03f || ny > 1.03f) null else b.copy(y = ny)
        }.toMutableList()

        // --- Collisions ---
        var aliens = state.aliens
        var shields = state.shields
        var score = state.score
        var alienKilled = 0
        var shieldHit = false
        var lives = state.lives
        var playerHit = false
        var invincible = invincibleLeft

        val survivors = mutableListOf<Bullet>()
        for (bullet in bullets) {
            var consumed = false

            if (bullet.fromPlayer) {
                val hitIndex = aliens.indexOfFirst { alien ->
                    alien.alive &&
                        kotlin.math.abs(alienX(offsetX, alien.col) - bullet.x) < ALIEN_HALF_SIZE + BULLET_HALF_WIDTH &&
                        kotlin.math.abs(alienY(alien.row, descent) - bullet.y) < ALIEN_HALF_SIZE
                }
                if (hitIndex >= 0) {
                    val alien = aliens[hitIndex]
                    score += AlienFormation.pointsForRow(alien.row)
                    alienKilled += 1
                    aliens = aliens.toMutableList().also { it[hitIndex] = alien.copy(alive = false) }
                    consumed = true
                }
            } else if (!consumed && invincible <= 0f &&
                kotlin.math.abs(bullet.x - state.playerX) < PLAYER_HALF_WIDTH &&
                kotlin.math.abs(bullet.y - PLAYER_Y) < 0.035f
            ) {
                lives = (lives - 1).coerceAtLeast(0)
                playerHit = true
                invincible = PLAYER_INVINCIBLE_SECONDS
                consumed = true
            }

            if (!consumed) {
                val shieldIndex = shields.indexOfFirst { shield ->
                    shield.hp > 0 &&
                        kotlin.math.abs(shield.x - bullet.x) < SHIELD_HALF_WIDTH + BULLET_HALF_WIDTH &&
                        kotlin.math.abs(SpaceInvadersState.SHIELD_Y - bullet.y) < SHIELD_HALF_HEIGHT
                }
                if (shieldIndex >= 0) {
                    val shield = shields[shieldIndex]
                    shields = shields.toMutableList().also { it[shieldIndex] = shield.copy(hp = shield.hp - 1) }
                    shieldHit = true
                    consumed = true
                }
            }

            if (!consumed) survivors += bullet
        }
        shields = shields.filter { it.hp > 0 }

        val nextPhase = when {
            lives <= 0 -> GamePhase.GAME_OVER
            aliens.none { it.alive } -> GamePhase.WAVE_CLEARED
            else -> GamePhase.RUNNING
        }

        return state.copy(
            aliens = aliens,
            alienDirection = direction,
            alienFormationOffsetX = offsetX,
            alienDescent = descent,
            alienFormationSpeed = speed,
            shields = shields,
            bullets = survivors,
            playerFireCooldown = fireCooldownLeft,
            playerInvincibleSeconds = invincible,
            lives = lives,
            score = score,
            nextBulletId = nextBulletId,
            phase = nextPhase,
            alienKilledThisTick = alienKilled,
            playerShotThisTick = false,
            playerHitThisTick = playerHit,
            shieldHitThisTick = shieldHit,
            alienMarchStepThisTick = marchStepped
        )
    }
}
