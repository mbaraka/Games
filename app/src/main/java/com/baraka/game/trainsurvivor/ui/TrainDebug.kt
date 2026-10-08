package com.baraka.game.trainsurvivor.ui

import com.baraka.game.trainsurvivor.model.AUTO_WEAPON
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.MAX_CAR_ARMOR
import com.baraka.game.trainsurvivor.model.MAX_POWER
import com.baraka.game.trainsurvivor.model.MAX_SLOTS
import com.baraka.game.trainsurvivor.model.Progression
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.TrainUpgrade
import com.baraka.game.trainsurvivor.model.WeaponType
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.TrainGame
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection
import kotlin.math.max

/** Developer cheats available from the Garage's settings. They edit the real save data. */
fun garageDebugSections(save: SaveData, onSave: (SaveData) -> Unit, onReset: () -> Unit): List<DebugSection> = listOf(
    DebugSection(
        "Economy",
        listOf(
            DebugItem.Action("+1,000 coins") { onSave(save.copy(coins = save.coins + 1_000)) },
            DebugItem.Action("+10,000 coins") { onSave(save.copy(coins = save.coins + 10_000)) },
            DebugItem.Action("+100,000 coins") { onSave(save.copy(coins = save.coins + 100_000)) },
            DebugItem.Action("Coins = 0") { onSave(save.copy(coins = 0)) }
        )
    ),
    DebugSection(
        "Progress",
        listOf(
            DebugItem.Action("Max all train upgrades") { onSave(save.copy(upgrades = TrainUpgrade.entries.associateWith { it.maxLevel })) },
            DebugItem.Action("Unlock every seat, weapon, max power and car armor") {
                onSave(
                    save.copy(
                        unlockedSlots = MAX_SLOTS,
                        slots = List(MAX_SLOTS) {
                            CrewSlot(owned = (1 shl WeaponType.entries.size) - 1, selected = AUTO_WEAPON, power = MAX_POWER, armor = MAX_CAR_ARMOR)
                        }
                    )
                )
            },
            DebugItem.Action("Complete today's goals") {
                val synced = Progression.sync(save)
                onSave(synced.copy(goalProgress = Progression.goalsForDay(synced.goalDay).map { it.target }))
            },
            DebugItem.Action("Best distance = 5,000 m") { onSave(save.copy(bestDistance = max(save.bestDistance, 5_000))) },
            DebugItem.Action("Unlock every danger level") { onSave(save.copy(dangerBest = List(5) { 99_999 })) },
            DebugItem.Action("Force +1 prestige") { onSave(save.copy(prestige = save.prestige + 1)) },
            DebugItem.Action("RESET ALL PROGRESS", onReset)
        )
    ),
    DebugSection("Notes", listOf(DebugItem.Info("These change your real save data. Reset clears coins, upgrades, goals and records but keeps settings.")))
)

/** Developer cheats available while a run is paused. They act on the live [game]. */
fun runDebugSections(
    game: TrainGame,
    godMode: Boolean,
    onGodMode: (Boolean) -> Unit,
    timeScale: Float,
    onTimeScale: (Float) -> Unit
): List<DebugSection> = listOf(
    DebugSection(
        "Player",
        listOf(
            DebugItem.Toggle("God mode (train can't be hurt)", godMode, onGodMode),
            DebugItem.Action("Heal train and wagons") { game.debugHeal() },
            DebugItem.Action("+1,000 run coins") { game.debugAddCoins(1_000) },
            DebugItem.Action("Reset ability cooldowns") { game.debugResetCooldowns() }
        )
    ),
    DebugSection(
        "Time",
        listOf(
            DebugItem.Action("Skip +30 s (harder zombies)") { game.debugSkipTime(30f) },
            DebugItem.Action("Skip +60 s") { game.debugSkipTime(60f) },
            DebugItem.Action("Skip +120 s") { game.debugSkipTime(120f) },
            DebugItem.Toggle("Slow motion (0.25x)", timeScale < 1f) { onTimeScale(if (it) 0.25f else 1f) },
            DebugItem.Toggle("Fast forward (2x)", timeScale > 1f) { onTimeScale(if (it) 2f else 1f) }
        )
    ),
    DebugSection(
        "Enemies",
        ZombieType.entries.filter { !it.boss }.map { type ->
            DebugItem.Action("Spawn 5 ${type.displayName}") { game.debugSpawn(type, 5) }
        } + listOf(
            DebugItem.Action("Spawn the next boss") { game.spawnBoss() },
        ) + com.baraka.game.trainsurvivor.model.EliteTrait.entries.map { trait ->
            DebugItem.Action("Spawn ${trait.displayName} elite") { game.debugSpawnElite(trait) }
        } + listOf(
            DebugItem.Action("Kill everything") { game.debugKillAll() }
        )
    ),
    DebugSection(
        "Events",
        listOf(
            DebugItem.Action("Open a route junction") { game.openJunction() },
            DebugItem.Action("Drop a supply crate") { game.debugCrate() },
            DebugItem.Action("Start a horde") { game.debugHorde() }
        )
    ),
    DebugSection("Notes", listOf(DebugItem.Info("Tip: turn on 'Show FPS counter' in Settings to watch performance while you use these.")))
)
