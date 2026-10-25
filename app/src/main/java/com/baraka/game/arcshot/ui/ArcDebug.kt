package com.baraka.game.arcshot.ui

import com.baraka.game.arcshot.data.ArcRepository
import com.baraka.game.arcshot.model.ARC_LEVELS
import com.baraka.game.arcshot.systems.ArcGame
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection

/** Progress cheats, available from the level select. They edit the real save data. */
fun menuDebugSections(repo: ArcRepository, onChanged: () -> Unit, onJump: (Int) -> Unit): List<DebugSection> = listOf(
    DebugSection(
        "Progress",
        listOf(
            DebugItem.Action("Unlock all levels") { repo.unlockAll(); onChanged() },
            DebugItem.Action("Complete every level (3 stars)") { repo.completeAll(); onChanged() },
            DebugItem.Action("RESET ALL PROGRESS") { repo.resetProgress(); onChanged() }
        )
    ),
    DebugSection(
        "Jump to level",
        ARC_LEVELS.mapIndexed { i, level -> DebugItem.Action("${i + 1}. ${level.name}") { onJump(i) } }
    )
)

/** In-run cheats: gravity and wind are read by the physics step every frame, so changes apply immediately. */
fun runDebugSections(
    game: ArcGame,
    repo: ArcRepository,
    levelIndex: Int,
    slowMo: Boolean,
    onSlowMo: (Boolean) -> Unit,
    onChanged: () -> Unit,
    onJump: (Int) -> Unit
): List<DebugSection> = listOf(
    DebugSection(
        "Shot",
        listOf(
            DebugItem.Action("Win instantly") { game.winInstantly(); onChanged() },
            DebugItem.Toggle("Infinite shots (no best-shots tracking)", game.infiniteShots) { game.infiniteShots = it; onChanged() },
            DebugItem.Toggle("Slow motion", slowMo) { onSlowMo(it) },
            DebugItem.Info("Shots taken: ${game.shots}  -  par ${ARC_LEVELS[levelIndex].parShots}")
        )
    ),
    DebugSection(
        "Physics",
        listOf(
            DebugItem.Action("Low gravity") { game.gravity = 400f; onChanged() },
            DebugItem.Action("Normal gravity") { game.gravity = ARC_LEVELS[levelIndex].gravity; onChanged() },
            DebugItem.Action("High gravity") { game.gravity = 1500f; onChanged() },
            DebugItem.Action("No wind") { game.wind = 0f; onChanged() }
        )
    ),
    DebugSection(
        "Progress",
        listOf(
            DebugItem.Action("Unlock all levels") { repo.unlockAll() },
            DebugItem.Action("RESET ALL PROGRESS") { repo.resetProgress() }
        ) + ARC_LEVELS.mapIndexed { i, level -> DebugItem.Action("Go to ${i + 1}. ${level.name}") { onJump(i) } }
    )
)
