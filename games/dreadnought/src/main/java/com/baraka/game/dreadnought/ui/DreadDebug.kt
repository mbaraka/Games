package com.baraka.game.dreadnought.ui

import com.baraka.game.dreadnought.data.DreadSave
import com.baraka.game.dreadnought.model.Refit
import com.baraka.game.dreadnought.model.ShipClass
import com.baraka.game.dreadnought.model.Theatre
import com.baraka.game.dreadnought.systems.NavalGame
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection

/** Harbour cheats: money, upgrades, unlocks and a full reset. They only edit the save, like a real purchase would. */
fun harborDebug(save: DreadSave, onSave: (DreadSave) -> Unit): List<DebugSection> = listOf(
    DebugSection(
        "Economy", listOf(
            DebugItem.Info("Coins: ${save.coins}"),
            DebugItem.Action("+1,000 coins") { onSave(save.copy(coins = save.coins + 1_000)) },
            DebugItem.Action("+10,000 coins") { onSave(save.copy(coins = save.coins + 10_000)) }
        )
    ),
    DebugSection(
        "Progress", listOf(
            DebugItem.Info("Best wave ${save.bestWave}, best score ${save.bestScore}"),
            DebugItem.Action("Unlock every ship") { onSave(save.copy(shipsOwned = (1 shl ShipClass.BUILTIN_PLAYABLE_COUNT) - 1)) },
            DebugItem.Action("Max every refit") { onSave(save.copy(levels = Refit.entries.map { it.maxLevel })) },
            DebugItem.Action("Unlock all theatres") {
                onSave(save.copy(bestWave = maxOf(save.bestWave, Theatre.entries.maxOf { it.unlockWave })))
            },
            DebugItem.Action("Set best wave to 20") { onSave(save.copy(bestWave = maxOf(save.bestWave, 20))) },
            DebugItem.Action("Reset all progress") { onSave(DreadSave()) }
        )
    )
)

/** In-sortie cheats. They go through the game's own spawn, damage and wave code, so nothing can end up impossible. */
fun runDebug(
    game: NavalGame,
    god: Boolean,
    onGod: (Boolean) -> Unit,
    instantReload: Boolean,
    onInstantReload: (Boolean) -> Unit,
    timeScale: Float,
    onTimeScale: (Float) -> Unit
): List<DebugSection> = listOf(
    DebugSection(
        "Economy", listOf(
            DebugItem.Info("Coins this sortie: ${game.runCoins}"),
            DebugItem.Action("+500 coins this sortie") { game.runCoins += 500 },
            DebugItem.Action("+5,000 score") { game.score += 5_000 }
        )
    ),
    DebugSection(
        "Ship", listOf(
            DebugItem.Toggle("God mode", god) { game.god = it; onGod(it) },
            DebugItem.Toggle("Instant reload", instantReload) { game.instantReload = it; onInstantReload(it) },
            DebugItem.Action("Repair hull and put out fires") { game.debugHeal() },
            DebugItem.Toggle("Slow motion (0.4x)", timeScale < 1f) { onTimeScale(if (it) 0.4f else 1f) },
            DebugItem.Toggle("Fast forward (2x)", timeScale > 1f) { onTimeScale(if (it) 2f else 1f) }
        )
    ),
    DebugSection(
        "Waves", listOf(
            DebugItem.Info("Wave ${game.wave}, ${game.enemiesLeft} hostile"),
            DebugItem.Action("Sink every enemy") { game.debugKillAll() },
            DebugItem.Action("Skip to next wave") { game.debugSkipWaves(1) },
            DebugItem.Action("Skip 5 waves") { game.debugSkipWaves(5) },
            DebugItem.Action("Skip to next boss wave") { game.debugSkipWaves(5 - game.wave % 5) }
        )
    ),
    DebugSection(
        "Spawn ahead", ShipClass.ENEMIES.map { c -> DebugItem.Action(c.label) { game.debugSpawnAhead(c) } }
    )
)
