package com.baraka.game.tunnelcrew.ui

import com.baraka.game.tunnelcrew.data.CrewRepository
import com.baraka.game.tunnelcrew.model.LEVELS
import com.baraka.game.tunnelcrew.systems.CrewSim
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection

/** Progress cheats, available from the level select. They edit the real save data. */
fun menuDebugSections(repo: CrewRepository, onChanged: () -> Unit, onJump: (Int) -> Unit): List<DebugSection> = listOf(
    DebugSection(
        "Progress",
        listOf(
            DebugItem.Action("Unlock all levels") { repo.unlockAll(); onChanged() },
            DebugItem.Action("Complete every level (full crew saved)") { repo.completeAll(); onChanged() },
            DebugItem.Action("RESET ALL PROGRESS") { repo.resetProgress(); onChanged() }
        )
    ),
    DebugSection(
        "Jump to level",
        LEVELS.mapIndexed { i, level -> DebugItem.Action("${i + 1}. ${level.name}") { onJump(i) } }
    )
)

/**
 * In-level cheats. [onChanged] makes the screen rebuild this list so toggle states stay in sync;
 * [slowMo] / [onSlowMo] drive the screen's time scale.
 */
fun runDebugSections(
    sim: CrewSim,
    repo: CrewRepository,
    onChanged: () -> Unit,
    slowMo: Boolean,
    onSlowMo: (Boolean) -> Unit,
    onJump: (Int) -> Unit
): List<DebugSection> = listOf(
    DebugSection(
        "Crew",
        listOf(
            DebugItem.Action("+5 of every skill") { sim.addSkills(5); onChanged() },
            DebugItem.Toggle("Infinite skills", sim.infiniteSkills) { sim.infiniteSkills = it; onChanged() },
            DebugItem.Toggle("God mode (nobody dies)", sim.god) { sim.god = it; onChanged() },
            DebugItem.Action("Release whole crew now") { sim.releaseAll() },
            DebugItem.Action("Nuke") { sim.nuke() }
        )
    ),
    DebugSection(
        "Time",
        listOf(
            DebugItem.Toggle("Freeze timer", sim.freezeTime) { sim.freezeTime = it; onChanged() },
            DebugItem.Action("+60 seconds") { sim.timeLeft += 60 * 30 },
            DebugItem.Toggle("Slow motion", slowMo) { onSlowMo(it) }
        )
    ),
    DebugSection(
        "Level",
        listOf(
            DebugItem.Action("Win now (everyone saved)") { sim.forceFinish(true) },
            DebugItem.Action("Fail now") { sim.forceFinish(false) },
            DebugItem.Info("Level ${sim.level.name}: need ${sim.level.needed} of ${sim.level.total}")
        )
    ),
    DebugSection(
        "Progress",
        listOf(
            DebugItem.Action("Unlock all levels") { repo.unlockAll() },
            DebugItem.Action("RESET ALL PROGRESS") { repo.resetProgress() }
        ) + LEVELS.mapIndexed { i, level -> DebugItem.Action("Go to ${i + 1}. ${level.name}") { onJump(i) } }
    )
)
