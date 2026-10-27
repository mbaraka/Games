package com.baraka.game.cratecrew.ui

import com.baraka.game.cratecrew.data.CrateRepository
import com.baraka.game.cratecrew.model.CRATE_LEVELS
import com.baraka.game.cratecrew.systems.CrateGame
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection

/** Progress cheats, available from the level select. They edit the real save data. */
fun menuDebugSections(repo: CrateRepository, onChanged: () -> Unit, onJump: (Int) -> Unit): List<DebugSection> = listOf(
    DebugSection(
        "Progress",
        listOf(
            DebugItem.Action("Unlock all levels") { repo.unlockAll(); onChanged() },
            DebugItem.Action("Complete every level") { repo.completeAll(); onChanged() },
            DebugItem.Action("RESET ALL PROGRESS") { repo.resetProgress(); onChanged() }
        )
    ),
    DebugSection(
        "Jump to level",
        CRATE_LEVELS.mapIndexed { i, level -> DebugItem.Action("${i + 1}. ${level.name}") { onJump(i) } }
    )
)

/** In-level cheats: solving instantly and undo are safe because they go through [CrateGame]'s own state. */
fun runDebugSections(
    game: CrateGame,
    repo: CrateRepository,
    levelIndex: Int,
    onChanged: () -> Unit,
    onJump: (Int) -> Unit
): List<DebugSection> = listOf(
    DebugSection(
        "Level",
        listOf(
            DebugItem.Action("Solve instantly") { game.solveInstantly(); onChanged() },
            DebugItem.Action("Undo last move") { game.undo(); onChanged() },
            DebugItem.Info("Moves ${game.moves}  -  pushes ${game.pushes}  -  par ${CRATE_LEVELS[levelIndex].par}")
        )
    ),
    DebugSection(
        "Progress",
        listOf(
            DebugItem.Action("Unlock all levels") { repo.unlockAll() },
            DebugItem.Action("RESET ALL PROGRESS") { repo.resetProgress() }
        ) + CRATE_LEVELS.mapIndexed { i, level -> DebugItem.Action("Go to ${i + 1}. ${level.name}") { onJump(i) } }
    )
)
