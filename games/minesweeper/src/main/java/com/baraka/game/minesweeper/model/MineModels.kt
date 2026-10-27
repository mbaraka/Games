package com.baraka.game.minesweeper.model

/** How the two players relate: [COOP] shares lives and wins or loses together, [VERSUS] scores every cell you uncover. */
enum class MineMode(val label: String, val blurb: String, val multiplayer: Boolean = true) {
    COOP("Co-op", "Clear the board together. Both of you share 3 lives: every mine you trigger costs one."),
    VERSUS("Versus", "Race for cells. Each safe cell you uncover scores a point, each mine costs 5. Most points wins."),
    /** Classic single-player game on the same engine and screens: one mine ends it. Keep last so snapshot ordinals stay stable. */
    SOLO("Solo", "Classic Minesweeper. Uncover every safe cell; one mine ends the game.", multiplayer = false)
}

enum class MineDifficulty(val label: String, val w: Int, val h: Int, val mines: Int) {
    EASY("Easy", 8, 10, 10),
    MEDIUM("Medium", 10, 14, 26),
    HARD("Hard", 12, 16, 45)
}

/** Cell state as stored in the low two bits of a snapshot cell. */
object Cell {
    const val HIDDEN = 0
    const val REVEALED = 1
    const val FLAGGED = 2
    const val EXPLODED = 3

    // Packed layout: bits 0-1 state, bit 2 owner (0 host, 1 guest), bit 3 mine (only sent when known), bits 4-7 neighbour mines.
    fun state(c: Byte): Int = c.toInt() and 3
    fun owner(c: Byte): Int = (c.toInt() shr 2) and 1
    fun isMine(c: Byte): Boolean = (c.toInt() and 8) != 0
    fun adjacent(c: Byte): Int = (c.toInt() shr 4) and 15
    fun pack(state: Int, owner: Int, mine: Boolean, adj: Int): Byte =
        (state or (owner shl 2) or (if (mine) 8 else 0) or (adj shl 4)).toByte()
}

object MineStatus {
    const val PLAYING = 0
    const val WON = 1
    const val LOST = 2
}

const val COOP_LIVES = 3
const val SOLO_LIVES = 1
const val MINE_PENALTY = 5

/**
 * What the host tells the guest after every change, and what both phones render. It is the whole visible state of a
 * board (<= 200 bytes of cells), so a full snapshot per change is simpler and safer than streaming deltas.
 * [boomSeq] increments on every mine hit so each phone can vibrate once; [boomBy] is who triggered it (0 host, 1 guest).
 */
class MineSnap(
    val mode: Int,
    val w: Int,
    val h: Int,
    val mines: Int,
    val lives: Int,
    val scoreHost: Int,
    val scoreGuest: Int,
    val status: Int,
    val boomSeq: Int,
    val boomBy: Int,
    val started: Boolean,
    val cells: ByteArray
) {
    val modeEnum: MineMode get() = MineMode.entries[mode.coerceIn(0, MineMode.entries.size - 1)]

    fun flagCount(): Int {
        var n = 0
        for (i in cells.indices) if (Cell.state(cells[i]) == Cell.FLAGGED) n++
        return n
    }
}
