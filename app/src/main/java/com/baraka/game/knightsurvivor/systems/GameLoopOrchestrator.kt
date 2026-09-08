package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.EnemyDebuff
import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.GamePhase
import com.baraka.game.knightsurvivor.model.GameState
import com.baraka.game.knightsurvivor.model.SoundCue
import com.baraka.game.knightsurvivor.model.UpgradeOption
import com.baraka.game.knightsurvivor.model.Vec2
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

        val movedPlayer = PlayerMovementSystem.step(state.player, state.joystickInput, state.arenaSize, newElapsedMs, dtSeconds)

        val firing = WeaponFiringSystem.step(movedPlayer, state.enemies, newElapsedMs, dtMs, random)
        val playerWithWeapons = movedPlayer.copy(weapons = firing.weapons)

        val movedEnemies = EnemyMovementSystem.step(state.enemies, playerWithWeapons.position, newElapsedMs, dtSeconds)
        val rangedAttack = EnemyRangedAttackSystem.step(movedEnemies, playerWithWeapons.position, dtMs)

        val movedProjectiles =
            ProjectileMovementSystem.step(state.projectiles + firing.newProjectiles, state.arenaSize, playerWithWeapons.position, dtSeconds)
        val movedEnemyProjectiles =
            EnemyProjectileMovementSystem.step(state.enemyProjectiles + rangedAttack.newProjectiles, state.arenaSize, dtSeconds)

        val collision = CollisionSystem.resolve(
            enemies = rangedAttack.enemies,
            projectiles = movedProjectiles,
            enemyProjectiles = movedEnemyProjectiles,
            player = playerWithWeapons,
            externalDamageEvents = firing.directDamageEvents,
            nowMs = newElapsedMs,
            random = random
        )

        val mergedDebuffs = mergeDebuffs(firing.enemyDebuffs, collision.enemyDebuffs)
        val enemiesWithEffects = collision.enemies.map { enemy -> applyPullAndDebuff(enemy, firing.enemyPulls[enemy.id], mergedDebuffs[enemy.id]) } +
            collision.newEnemies

        val spawnedEnemies = EnemySpawnSystem.step(
            currentEnemyCount = enemiesWithEffects.size,
            arenaSize = state.arenaSize,
            previousElapsedMs = state.elapsedMs,
            newElapsedMs = newElapsedMs,
            difficulty = state.difficulty,
            random = random
        )

        val xpStep = XpAndLevelingSystem.step(collision.player, state.xpGems + collision.newXpGems, dtSeconds)
        val healthStep = HealthPickupSystem.step(xpStep.player, state.healthPickups + collision.newHealthPickups, dtSeconds)

        val prunedActivations = (state.recentWeaponActivations + firing.newActivationEvents)
            .filter { event -> newElapsedMs - event.activatedAtMs <= GameTunables.WEAPON_EFFECT_VISIBLE_MS }

        val newParticles = collision.hitFeedback.flatMap { ParticleSystem.hitSpark(it.position, it.colorArgb, random) } +
            collision.deathFeedback.flatMap { ParticleSystem.deathBurst(it.position, it.colorArgb, random) }
        val updatedParticles = ParticleSystem.step(state.particles + newParticles, dtMs, dtSeconds)

        val soundCues = buildList {
            if (collision.hitFeedback.isNotEmpty()) add(SoundCue.ENEMY_HIT)
            if (collision.deathFeedback.isNotEmpty()) add(SoundCue.ENEMY_DEATH)
            if (collision.player.hp < playerWithWeapons.hp) add(SoundCue.PLAYER_HURT)
            if (healthStep.player.hp > xpStep.player.hp) add(SoundCue.HEALTH_PICKUP)
            if (xpStep.leveledUp) add(SoundCue.LEVEL_UP)
        }

        val nextState = state.copy(
            player = healthStep.player,
            enemies = enemiesWithEffects + spawnedEnemies,
            projectiles = collision.projectiles,
            enemyProjectiles = collision.enemyProjectiles,
            xpGems = xpStep.xpGems,
            healthPickups = healthStep.pickups,
            particles = updatedParticles,
            recentWeaponActivations = prunedActivations,
            pendingSoundCues = soundCues,
            pendingWeaponFireSounds = firing.firedWeaponKinds,
            elapsedMs = newElapsedMs,
            killCount = state.killCount + collision.killsThisTick,
            bossDefeatedThisRun = state.bossDefeatedThisRun || collision.deathFeedback.any { it.isBoss }
        )

        // If every weapon in the deck is maxed and every passive is at its max stacks, the
        // pool is legitimately empty — pausing for a choice that doesn't exist would
        // softlock the run (no cards to tap, nothing ever resumes it). Skip the pause
        // rather than get stuck; the level-up sound still played above.
        val upgradeChoices = if (xpStep.leveledUp) UpgradeChoiceSystem.rollChoices(nextState.player, state.weaponDeck, random) else emptyList()

        return when {
            nextState.player.hp <= 0 ->
                nextState.copy(phase = GamePhase.GAME_OVER, pendingSoundCues = soundCues + SoundCue.GAME_OVER)

            nextState.elapsedMs >= GameTunables.VICTORY_SURVIVAL_TIME_MS ->
                nextState.copy(phase = GamePhase.VICTORY, pendingSoundCues = soundCues + SoundCue.VICTORY)

            xpStep.leveledUp && upgradeChoices.isNotEmpty() ->
                nextState.copy(phase = GamePhase.LEVEL_UP_PAUSE, pendingUpgradeChoices = upgradeChoices)

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

    /** Combines this tick's weapon-inflicted debuffs (Frying Pan's stun) with collision-inflicted ones (Egg Toss's splash slow), keeping the stronger of each per enemy. */
    private fun mergeDebuffs(a: Map<Long, EnemyDebuff>, b: Map<Long, EnemyDebuff>): Map<Long, EnemyDebuff> {
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a
        val merged = a.toMutableMap()
        b.forEach { (id, debuff) ->
            val existing = merged[id]
            merged[id] = if (existing == null) {
                debuff
            } else {
                EnemyDebuff(
                    stunUntilMs = maxOf(existing.stunUntilMs, debuff.stunUntilMs),
                    slowUntilMs = maxOf(existing.slowUntilMs, debuff.slowUntilMs),
                    slowMultiplier = if (debuff.slowUntilMs >= existing.slowUntilMs) debuff.slowMultiplier else existing.slowMultiplier
                )
            }
        }
        return merged
    }

    private fun applyPullAndDebuff(enemy: EnemyInstance, pulledPosition: Vec2?, debuff: EnemyDebuff?): EnemyInstance {
        var updated = enemy
        if (pulledPosition != null) updated = updated.copy(position = pulledPosition)
        if (debuff != null) {
            if (debuff.stunUntilMs > updated.stunnedUntilMs) updated = updated.copy(stunnedUntilMs = debuff.stunUntilMs)
            if (debuff.slowUntilMs > updated.slowUntilMs) {
                updated = updated.copy(slowUntilMs = debuff.slowUntilMs, slowMultiplier = debuff.slowMultiplier)
            }
        }
        return updated
    }
}
