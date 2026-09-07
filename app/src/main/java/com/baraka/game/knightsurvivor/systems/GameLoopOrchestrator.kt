package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.GamePhase
import com.baraka.game.knightsurvivor.model.GameState
import com.baraka.game.knightsurvivor.model.UpgradeOption
import kotlin.random.Random

/**
 * The thin composition root: the only file in this package that knows the call order
 * between systems. Every system above stays independently swappable — this file is
 * intentionally the sole place that couples them, mirroring how MainActivity's
 * `when(gameType)` block is the sole coupling point for the whole Game Hub.
 */
object GameLoopOrchestrator {

    /** Advances [state] by one fixed tick (see [GameTunables.TICK_MS]). No-op unless the run is actually RUNNING. */
    fun tick(state: GameState, random: Random = Random.Default): GameState {
        if (state.phase != GamePhase.RUNNING) return state

        val dtMs = GameTunables.TICK_MS
        val dtSeconds = dtMs / 1000f
        val newElapsedMs = state.elapsedMs + dtMs

        val movedPlayer = PlayerMovementSystem.step(state.player, state.joystickInput, state.arenaSize, dtSeconds)

        val firing = WeaponFiringSystem.step(movedPlayer, state.enemies, newElapsedMs, dtMs)
        val playerWithWeapons = movedPlayer.copy(weapons = firing.weapons)

        val movedEnemies = EnemyMovementSystem.step(state.enemies, playerWithWeapons.position, dtSeconds)
        val movedProjectiles =
            ProjectileMovementSystem.step(state.projectiles + firing.newProjectiles, state.arenaSize, dtSeconds)

        val collision = CollisionSystem.resolve(
            enemies = movedEnemies,
            projectiles = movedProjectiles,
            player = playerWithWeapons,
            externalDamageEvents = firing.directDamageEvents,
            nowMs = newElapsedMs
        )

        val spawnedEnemies = EnemySpawnSystem.step(
            currentEnemyCount = collision.enemies.size,
            arenaSize = state.arenaSize,
            previousElapsedMs = state.elapsedMs,
            newElapsedMs = newElapsedMs,
            random = random
        )

        val xpStep = XpAndLevelingSystem.step(collision.player, state.xpGems + collision.newXpGems, dtSeconds)

        val prunedActivations = (state.recentWeaponActivations + firing.newActivationEvents)
            .filter { event -> newElapsedMs - event.activatedAtMs <= GameTunables.WEAPON_EFFECT_VISIBLE_MS }

        val newParticles = collision.hitFeedback.flatMap { ParticleSystem.hitSpark(it.position, it.colorArgb, random) } +
            collision.deathFeedback.flatMap { ParticleSystem.deathBurst(it.position, it.colorArgb, random) }
        val updatedParticles = ParticleSystem.step(state.particles + newParticles, dtMs, dtSeconds)

        val nextState = state.copy(
            player = xpStep.player,
            enemies = collision.enemies + spawnedEnemies,
            projectiles = collision.projectiles,
            xpGems = xpStep.xpGems,
            particles = updatedParticles,
            recentWeaponActivations = prunedActivations,
            elapsedMs = newElapsedMs,
            killCount = state.killCount + collision.killsThisTick
        )

        return when {
            nextState.player.hp <= 0 ->
                nextState.copy(phase = GamePhase.GAME_OVER)

            nextState.elapsedMs >= GameTunables.VICTORY_SURVIVAL_TIME_MS ->
                nextState.copy(phase = GamePhase.VICTORY)

            xpStep.leveledUp ->
                nextState.copy(
                    phase = GamePhase.LEVEL_UP_PAUSE,
                    pendingUpgradeChoices = UpgradeChoiceSystem.rollChoices(nextState.player, random)
                )

            else -> nextState
        }
    }

    /** Applies the player's chosen upgrade and resumes the run. Called from ui/KnightSurvivorScreen.kt when the level-up overlay is dismissed. */
    fun applyUpgradeChoiceAndResume(state: GameState, choice: UpgradeOption): GameState {
        val updatedPlayer = UpgradeChoiceSystem.applyChoice(state.player, choice)
        return state.copy(
            player = updatedPlayer,
            phase = GamePhase.RUNNING,
            pendingUpgradeChoices = emptyList()
        )
    }
}
