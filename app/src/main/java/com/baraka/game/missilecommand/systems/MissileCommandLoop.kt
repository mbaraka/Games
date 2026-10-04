package com.baraka.game.missilecommand.systems

import com.baraka.game.missilecommand.model.Difficulty
import com.baraka.game.missilecommand.model.EnemyMissile
import com.baraka.game.missilecommand.model.Explosion
import com.baraka.game.missilecommand.model.GamePhase
import com.baraka.game.missilecommand.model.Interceptor
import com.baraka.game.missilecommand.model.MissileCommandState
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.AMMO_BONUS_SCORE
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.ARRIVAL_EPSILON
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.BASE_AMMO_PER_WAVE
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.BASE_ENEMY_SPEED
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.CITY_BONUS_SCORE
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.ENEMY_KILL_SCORE
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.FAST_ENEMY_KILL_SCORE
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.FAST_ENEMY_SPEED_MULTIPLIER
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.FAST_MISSILE_CHANCE
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.GROUND_Y
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.INTERCEPTOR_SPEED
import com.baraka.game.missilecommand.model.TargetKind
import com.baraka.game.missilecommand.model.explosionRadius
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Advances a [MissileCommandState] by one frame: same "pure `tick()` over immutable state"
 * shape as breakout's BreakoutGameLoop and spaceinvaders' AlienWaveLoop. [fire] is the one
 * player action (tap anywhere to launch an interceptor from the nearest living silo);
 * everything else — spawning, flight, detonation, damage — happens inside [tick].
 */
object MissileCommandLoop {

    private const val MAX_DT = 0.034f

    fun newGame(difficulty: Difficulty, bestScore: Int): MissileCommandState {
        val base = MissileCommandState(
            difficulty = difficulty,
            cities = WaveBuilder.freshCities(),
            silos = WaveBuilder.freshSilos(),
            bestScoreAtStart = bestScore
        )
        return beginWave(base, wave = 1)
    }

    fun startWave(state: MissileCommandState, wave: Int): MissileCommandState = beginWave(state, wave)

    private fun beginWave(state: MissileCommandState, wave: Int): MissileCommandState = state.copy(
        wave = wave,
        enemyMissiles = emptyList(),
        interceptors = emptyList(),
        explosions = emptyList(),
        ammo = (BASE_AMMO_PER_WAVE + wave + state.difficulty.startingAmmoBonus).coerceAtLeast(4),
        missilesToSpawnThisWave = WaveBuilder.missileCountForWave(wave),
        missilesSpawnedThisWave = 0,
        nextSpawnInSeconds = 0.5f,
        phase = GamePhase.RUNNING
    )

    /** Launches an interceptor from the silo nearest [targetX] toward ([targetX], [targetY]) — a no-op if out of ammo, no silo stands, or the run isn't live. */
    fun fire(state: MissileCommandState, targetX: Float, targetY: Float): MissileCommandState {
        if (state.phase != GamePhase.RUNNING || state.ammo <= 0) return state
        val silo = state.silos.filter { it.alive }.minByOrNull { kotlin.math.abs(it.x - targetX) } ?: return state

        val dx = targetX - silo.x
        val dy = targetY - GROUND_Y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
        val interceptor = Interceptor(
            id = state.nextInterceptorId,
            x = silo.x, y = GROUND_Y,
            vx = dx / dist * INTERCEPTOR_SPEED,
            vy = dy / dist * INTERCEPTOR_SPEED,
            targetX = targetX, targetY = targetY
        )
        return state.copy(
            interceptors = state.interceptors + interceptor,
            ammo = state.ammo - 1,
            nextInterceptorId = state.nextInterceptorId + 1,
            launchedThisTick = true
        )
    }

    fun tick(state: MissileCommandState, deltaSeconds: Float, rng: Random): MissileCommandState {
        if (state.phase != GamePhase.RUNNING) return state
        val dt = deltaSeconds.coerceIn(0f, MAX_DT)

        var score = state.score
        var cities = state.cities
        var silos = state.silos
        var cityLost = false
        var siloLost = false
        var killed = 0

        // --- Spawn ---
        var nextSpawnIn = state.nextSpawnInSeconds - dt
        var spawned = state.missilesSpawnedThisWave
        var enemyMissiles = state.enemyMissiles
        var nextMissileId = state.nextMissileId
        if (spawned < state.missilesToSpawnThisWave && nextSpawnIn <= 0f) {
            val interval = (1.5f - state.wave * 0.035f).coerceAtLeast(0.3f) / state.difficulty.spawnRateMultiplier
            nextSpawnIn = interval
            spawned += 1

            val targets = cities.filter { it.alive }.map { TargetKind.CITY to it.id to it.x } +
                silos.filter { it.alive }.map { TargetKind.SILO to it.id to it.x }
            if (targets.isNotEmpty()) {
                val (kindAndId, targetX) = targets.random(rng)
                val (kind, targetId) = kindAndId
                val fast = state.wave >= 3 && rng.nextFloat() < FAST_MISSILE_CHANCE
                val speed = BASE_ENEMY_SPEED * state.difficulty.missileSpeedMultiplier *
                    (1f + (state.wave - 1) * 0.05f) * (if (fast) FAST_ENEMY_SPEED_MULTIPLIER else 1f)
                val startX = rng.nextFloat()
                val dx = targetX - startX
                val dy = GROUND_Y
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
                enemyMissiles = enemyMissiles + EnemyMissile(
                    id = nextMissileId++,
                    x = startX, y = 0f,
                    vx = dx / dist * speed, vy = dy / dist * speed,
                    targetX = targetX, targetY = GROUND_Y,
                    targetKind = kind, targetId = targetId,
                    fast = fast
                )
            }
        }

        // --- Move + resolve enemy missiles (against this tick's *existing* explosions first) ---
        var newExplosions = mutableListOf<Explosion>()
        var nextExplosionId = state.nextExplosionId
        val survivingMissiles = mutableListOf<EnemyMissile>()
        for (missile in enemyMissiles) {
            val nx = missile.x + missile.vx * dt
            val ny = missile.y + missile.vy * dt
            val caughtInBlast = state.explosions.any { explosion ->
                val r = explosionRadius(explosion)
                r > 0.001f && distance(nx, ny, explosion.x, explosion.y) <= r
            }
            if (caughtInBlast) {
                killed += 1
                score += if (missile.fast) FAST_ENEMY_KILL_SCORE else ENEMY_KILL_SCORE
                continue
            }
            val arrived = distance(nx, ny, missile.targetX, missile.targetY) <= ARRIVAL_EPSILON || ny >= GROUND_Y
            if (arrived) {
                when (missile.targetKind) {
                    TargetKind.CITY -> {
                        if (cities.any { it.id == missile.targetId && it.alive }) {
                            cities = cities.map { if (it.id == missile.targetId) it.copy(alive = false) else it }
                            cityLost = true
                        }
                    }
                    TargetKind.SILO -> {
                        if (silos.any { it.id == missile.targetId && it.alive }) {
                            silos = silos.map { if (it.id == missile.targetId) it.copy(alive = false) else it }
                            siloLost = true
                        }
                    }
                    TargetKind.GROUND -> {}
                }
                newExplosions += Explosion(id = nextExplosionId++, x = missile.targetX, y = missile.targetY)
            } else {
                survivingMissiles += missile.copy(x = nx, y = ny)
            }
        }

        // --- Move + resolve interceptors ---
        var detonated = 0
        val survivingInterceptors = mutableListOf<Interceptor>()
        for (interceptor in state.interceptors) {
            val nx = interceptor.x + interceptor.vx * dt
            val ny = interceptor.y + interceptor.vy * dt
            val arrived = distance(nx, ny, interceptor.targetX, interceptor.targetY) <= ARRIVAL_EPSILON
            if (arrived) {
                newExplosions += Explosion(id = nextExplosionId++, x = interceptor.targetX, y = interceptor.targetY)
                detonated += 1
            } else {
                survivingInterceptors += interceptor.copy(x = nx, y = ny)
            }
        }

        // --- Age existing explosions, then add this tick's new ones (unaged) ---
        val agedExplosions = state.explosions
            .map { it.copy(ageSeconds = it.ageSeconds + dt) }
            .filter { it.ageSeconds < MissileCommandState.EXPLOSION_LIFETIME_SECONDS }

        val waveFullySpawned = spawned >= state.missilesToSpawnThisWave
        val nextPhase = when {
            cities.none { it.alive } -> GamePhase.GAME_OVER
            waveFullySpawned && survivingMissiles.isEmpty() && survivingInterceptors.isEmpty() -> GamePhase.WAVE_CLEARED
            else -> GamePhase.RUNNING
        }
        val waveClearBonus = if (nextPhase == GamePhase.WAVE_CLEARED) {
            cities.count { it.alive } * CITY_BONUS_SCORE + state.ammo * AMMO_BONUS_SCORE
        } else 0

        return state.copy(
            cities = cities,
            silos = silos,
            enemyMissiles = survivingMissiles,
            interceptors = survivingInterceptors,
            explosions = agedExplosions + newExplosions,
            score = score + waveClearBonus,
            missilesSpawnedThisWave = spawned,
            nextSpawnInSeconds = nextSpawnIn,
            nextMissileId = nextMissileId,
            nextExplosionId = nextExplosionId,
            phase = nextPhase,
            launchedThisTick = false,
            detonatedThisTick = detonated,
            missilesDestroyedThisTick = killed,
            cityLostThisTick = cityLost,
            siloLostThisTick = siloLost
        )
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
