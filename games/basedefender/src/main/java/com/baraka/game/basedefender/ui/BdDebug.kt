package com.baraka.game.basedefender.ui

import com.baraka.game.basedefender.data.BdRepository
import com.baraka.game.basedefender.model.EnemyType
import com.baraka.game.basedefender.model.PowerUpType
import com.baraka.game.basedefender.model.Tuning
import com.baraka.game.basedefender.systems.BaseDefenderEngine
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection

/**
 * Base Defender's cheats. Every list is derived from the content enums (enemy types, power-ups, waves), so new
 * content shows up here by itself. All actions go through the engine's real code paths.
 * [refresh] asks the UI to rebuild this list after a toggle changes.
 */
fun baseDefenderDebugSections(engine: BaseDefenderEngine, repo: BdRepository, refresh: () -> Unit): List<DebugSection> = listOf(
    DebugSection(
        "Economy",
        listOf(
            DebugItem.Action("+$500") { engine.debugAddMoney(500) },
            DebugItem.Action("+$5000") { engine.debugAddMoney(5000) },
            DebugItem.Action("Max all upgrades") { engine.debugMaxUpgrades() },
            DebugItem.Action("Reset best score") { repo.resetProgress() }
        )
    ),
    DebugSection(
        "Run",
        listOf(
            DebugItem.Toggle("God mode (no life loss)", engine.godMode) { engine.godMode = it; refresh() },
            DebugItem.Action("Full lives") { engine.debugHeal() },
            DebugItem.Action("Kill all enemies") { engine.debugKillAll() },
            DebugItem.Action("Skip to next wave") { engine.debugSkipWave() },
            DebugItem.Toggle("Slow motion x0.4", engine.timeScale < 1f) { engine.timeScale = if (it) 0.4f else 1f; refresh() },
            DebugItem.Toggle("Fast forward x2.5", engine.timeScale > 1f) { engine.timeScale = if (it) 2.5f else 1f; refresh() }
        )
    ),
    DebugSection(
        "Jump to wave",
        (listOf(1, 3, 5, 10, 15, Tuning.TOTAL_WAVES)).map { w -> DebugItem.Action("Wave $w") { engine.debugJumpToWave(w) } }
    ),
    DebugSection(
        "Spawn enemy",
        EnemyType.entries.map { t -> DebugItem.Action(t.label) { engine.debugSpawn(t) } }
    ),
    DebugSection(
        "Spawn power-up",
        PowerUpType.entries.map { p -> DebugItem.Action(p.label) { engine.debugSpawnPickup(p) } }
    ),
    DebugSection(
        "Info",
        listOf(DebugItem.Info("Wave ${engine.wave}, ${engine.enemies.size} enemies, ${engine.bullets.size} bullets, ${engine.turrets.size} turrets"))
    )
)
