package com.baraka.game.spaceinvaders.model

enum class GamePhase { RUNNING, WAVE_CLEARED, GAME_OVER }

data class Alien(val id: Int, val row: Int, val col: Int, val alive: Boolean = true)

/** One shot in flight. [fromPlayer] decides direction and who it can hurt. */
data class Bullet(val id: Long, val x: Float, val y: Float, val fromPlayer: Boolean)

/** A bunker: a simple rect barrier with [hp] remaining — fades out as it's chipped away. */
data class Shield(val id: Int, val x: Float, val hp: Int)

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/AlienWaveLoop.kt's `tick()` one copy at a time — same "pure tick over immutable
 * state" shape as breakout's BreakoutState. Field coordinates are normalized 0..1
 * independently on each axis (see BreakoutState's doc comment for why).
 */
data class SpaceInvadersState(
    val playerX: Float = 0.5f,
    val playerInvincibleSeconds: Float = 0f,
    val aliens: List<Alien> = emptyList(),
    val alienDirection: Int = 1, // +1 right, -1 left
    val alienFormationOffsetX: Float = 0f,
    val alienDescent: Float = 0f,
    val alienFormationSpeed: Float = BASE_ALIEN_SPEED,
    val shields: List<Shield> = emptyList(),
    val bullets: List<Bullet> = emptyList(),
    val playerFireCooldown: Float = 0f,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val lives: Int = difficulty.startingLives,
    val score: Int = 0,
    val wave: Int = 1,
    val phase: GamePhase = GamePhase.RUNNING,
    val bestScoreAtStart: Int = 0,
    val nextBulletId: Long = 0L,
    // Transient per-tick event flags — read once by ui/SpaceInvadersScreen.kt to trigger a
    // sound cue, then discarded on the next tick. Not persisted gameplay data.
    val alienKilledThisTick: Int = 0,
    val playerShotThisTick: Boolean = false,
    val playerHitThisTick: Boolean = false,
    val shieldHitThisTick: Boolean = false,
    val alienMarchStepThisTick: Boolean = false
) {
    val isNewBest: Boolean get() = score > bestScoreAtStart
    val aliveAlienCount: Int get() = aliens.count { it.alive }

    companion object {
        const val ROWS = 5
        const val COLS = 8

        const val ALIEN_AREA_TOP = 0.08f
        const val ALIEN_ROW_SPACING = 0.052f
        const val ALIEN_COL_SPACING = 1f / (COLS + 1.4f)
        const val ALIEN_HALF_SIZE = 0.028f
        const val ALIEN_START_LEFT = ALIEN_COL_SPACING * 0.7f

        const val BASE_ALIEN_SPEED = 0.09f // logical units/second, horizontal
        const val MAX_ALIEN_SPEED = 0.62f
        const val DESCENT_STEP = 0.028f

        const val PLAYER_Y = 0.90f
        const val PLAYER_HALF_WIDTH = 0.045f
        const val PLAYER_INVINCIBLE_SECONDS = 1.1f
        const val INVASION_Y = PLAYER_Y - 0.08f // an alien reaching this row ends the game instantly

        const val BULLET_HALF_WIDTH = 0.006f
        const val PLAYER_BULLET_SPEED = 1.15f
        const val BASE_ALIEN_BULLET_SPEED = 0.42f
        const val PLAYER_FIRE_COOLDOWN_SECONDS = 0.45f
        const val BASE_ALIEN_FIRE_CHANCE_PER_SECOND = 0.35f // scaled by wave + difficulty, split across columns

        const val SHIELD_COUNT = 4
        const val SHIELD_Y = 0.78f
        const val SHIELD_HALF_WIDTH = 0.052f
        const val SHIELD_HALF_HEIGHT = 0.022f
        const val SHIELD_MAX_HP = 5
    }
}

fun alienX(offsetX: Float, col: Int): Float =
    SpaceInvadersState.ALIEN_START_LEFT + offsetX + col * SpaceInvadersState.ALIEN_COL_SPACING

fun alienY(row: Int, descent: Float): Float =
    SpaceInvadersState.ALIEN_AREA_TOP + descent + row * SpaceInvadersState.ALIEN_ROW_SPACING
