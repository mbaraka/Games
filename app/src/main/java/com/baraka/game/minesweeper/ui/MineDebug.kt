package com.baraka.game.minesweeper.ui

import com.baraka.game.minesweeper.MineViewModel
import com.baraka.game.minesweeper.model.MineStatus
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection

/**
 * Developer tools for Minesweeper. The board lives on the host phone, so board cheats only exist there; every cheat goes
 * through the engine's normal reveal/restore paths, so scores, lives and the win check stay consistent.
 * There is no economy or saved progress in this game, so the sections are about the board itself.
 */
fun mineDebugSections(vm: MineViewModel): List<DebugSection> {
    val snap = vm.snap
    val info = DebugSection(
        "Board",
        listOf(
            DebugItem.Info(
                if (snap == null) "No board yet."
                else "${snap.w}x${snap.h}, ${snap.mines} mines, ${snap.flagCount()} flags, lives ${snap.lives}, " +
                    "score ${snap.scoreHost}:${snap.scoreGuest}, status ${snap.status}, role ${if (vm.isHost) "host" else "guest"}"
            )
        )
    )
    if (!vm.isHost) {
        return listOf(info, DebugSection("Host only", listOf(DebugItem.Info("Board cheats run on the host phone (the one that owns the rules)."))))
    }
    return listOf(
        info,
        DebugSection(
            "Cheats",
            listOf(
                DebugItem.Toggle("X-ray (show mines on this phone)", vm.xray) { vm.debugXray(it) },
                DebugItem.Action("Uncover 5 safe cells") { vm.debugRevealSafe(5) },
                DebugItem.Action("Uncover 25 safe cells") { vm.debugRevealSafe(25) },
                DebugItem.Action("Uncover ALL safe cells (win)") { vm.debugRevealSafe(10_000) },
                DebugItem.Action("Restore shared lives") { vm.debugRestoreLives() }
            )
        ),
        DebugSection(
            "Run",
            listOf(
                DebugItem.Action(if (snap?.status == MineStatus.PLAYING) "Restart board" else "New board") { vm.debugRestart() }
            )
        )
    )
}
