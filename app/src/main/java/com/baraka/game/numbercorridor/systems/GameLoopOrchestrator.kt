package com.baraka.game.numbercorridor.systems

import com.baraka.game.numbercorridor.model.Bullet
import com.baraka.game.numbercorridor.model.GamePhase
import com.baraka.game.numbercorridor.model.GameState
import com.baraka.game.numbercorridor.model.GameState.Companion.BASE_ROW_SPEED
import com.baraka.game.numbercorridor.model.GameState.Companion.BULLET_SPEED
import com.baraka.game.numbercorridor.model.GameState.Companion.MAX_ROW_SPEED
import com.baraka.game.numbercorridor.model.GameState.Companion.ROW_SPAWN_INTERVAL_SECONDS
import com.baraka.game.numbercorridor.model.PlayerState
import com.baraka.game.numbercorridor.model.WallRow

/**
 * Advances a [GameState] by one frame: same "pure `tick()` over immutable state" shape as
 * knightsurvivor's orchestrator of the same name, just driving a lane runner instead of an
 * arena. Auto-fire and row spawning both live here since, unlike the knight, this player
 * never manually aims — the only input is which lanes the squad stands in.
 */
object GameLoopOrchestrator {

    fun tick(state: GameState, deltaSeconds: Float): GameState {
        if (state.phase != GamePhase.RUNNING) return state
        val dt = deltaSeconds.coerceIn(0f, 0.05f) // clamp so a dropped frame can't skip a whole row past the player

        val elapsed = state.elapsedSeconds + dt
        val rowSpeed = (BASE_ROW_SPEED + elapsed * 0.0015f).coerceAtMost(MAX_ROW_SPEED)

        val advancedRows = state.rows.map { it.copy(progress = it.progress + rowSpeed * dt) }

        var nextRowId = state.nextRowId
        var nextRowIn = state.nextRowInSeconds - dt
        val spawnInterval = (ROW_SPAWN_INTERVAL_SECONDS - elapsed * 0.004f).coerceAtLeast(0.7f)
        val rowsWithSpawn = if (nextRowIn <= 0f) {
            nextRowIn = spawnInterval
            advancedRows + WallSpawnSystem.spawnRow(nextRowId++, elapsed)
        } else {
            advancedRows
        }

        val advancedBullets = state.bullets
            .map { it.copy(progress = it.progress - BULLET_SPEED * dt) }
            .filter { it.progress > 0f }

        val (playerAfterFiring, newBullets) = fireWeapons(state.player, dt)
        val bulletsWithNew = advancedBullets + newBullets

        val resolved = CollisionSystem.resolve(rowsWithSpawn, bulletsWithNew, playerAfterFiring)
        val newHealth = (resolved.player.health - resolved.damageTaken).coerceAtLeast(0)
        val finalPlayer = resolved.player.copy(health = newHealth)

        return state.copy(
            player = finalPlayer,
            rows = resolved.rows,
            bullets = resolved.bullets,
            score = state.score + resolved.scoreGained,
            elapsedSeconds = elapsed,
            rowSpeed = rowSpeed,
            nextRowInSeconds = nextRowIn,
            nextRowId = nextRowId,
            phase = if (newHealth <= 0) GamePhase.GAME_OVER else GamePhase.RUNNING
        )
    }

    /** Moves the squad's lane window, clamped so every occupied lane stays on the [WallRow.LANE_COUNT]-wide corridor. */
    fun moveSquad(state: GameState, targetLaneStart: Int): GameState {
        val maxStart = WallRow.LANE_COUNT - state.player.squadCount
        val clamped = targetLaneStart.coerceIn(0, maxStart)
        return state.copy(player = state.player.copy(laneStart = clamped))
    }

    private fun fireWeapons(player: PlayerState, dt: Float): Pair<PlayerState, List<Bullet>> {
        val interval = (PlayerState.BASE_FIRE_INTERVAL_SECONDS / player.fireRateMultiplier).coerceAtLeast(0.05f)
        val newCooldowns = mutableMapOf<Int, Float>()
        val newBullets = mutableListOf<Bullet>()

        for (lane in player.occupiedLanes) {
            val remaining = (player.fireCooldowns[lane] ?: 0f) - dt
            if (remaining <= 0f) {
                newBullets += Bullet(lane = lane, progress = 1f)
                newCooldowns[lane] = interval
            } else {
                newCooldowns[lane] = remaining
            }
        }

        return player.copy(fireCooldowns = newCooldowns) to newBullets
    }
}
