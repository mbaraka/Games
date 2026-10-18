package com.baraka.game.minesweeper.systems

import com.baraka.game.minesweeper.model.COOP_LIVES
import com.baraka.game.minesweeper.model.Cell
import com.baraka.game.minesweeper.model.MINE_PENALTY
import com.baraka.game.minesweeper.model.MineDifficulty
import com.baraka.game.minesweeper.model.MineMode
import com.baraka.game.minesweeper.model.MineSnap
import com.baraka.game.minesweeper.model.MineStatus
import com.baraka.game.minesweeper.model.SOLO_LIVES
import kotlin.random.Random

/**
 * The rules of the shared board. Only the host phone owns one of these; the guest just draws the [MineSnap]s it is sent
 * and asks the host to reveal/flag. Players are 0 (host) and 1 (guest). Mines are placed on the first reveal so the first
 * tap (and its neighbours) is always safe, which makes the opening a flood fill for both players to start from.
 */
class MineGame(val mode: MineMode, val difficulty: MineDifficulty, private val rng: Random = Random(System.nanoTime())) {
    val w = difficulty.w
    val h = difficulty.h
    private val n = w * h
    private val mine = BooleanArray(n)
    private val adj = ByteArray(n)
    private val state = ByteArray(n)
    private val owner = ByteArray(n)
    private val stack = IntArray(n)
    private val safeTotal = n - difficulty.mines

    private var generated = false
    private var revealedSafe = 0
    private val startLives = if (mode == MineMode.SOLO) SOLO_LIVES else COOP_LIVES
    var lives = startLives
        private set
    val score = IntArray(2)
    var status = MineStatus.PLAYING
        private set
    var boomSeq = 0
        private set
    var boomBy = 0
        private set

    /** Uncovers [idx] for [player]. Returns true if anything changed. Flagged and already open cells are ignored. */
    fun reveal(idx: Int, player: Int): Boolean {
        if (status != MineStatus.PLAYING || idx !in 0 until n) return false
        if (state[idx].toInt() != Cell.HIDDEN) return false
        if (!generated) generate(idx)
        if (mine[idx]) {
            state[idx] = Cell.EXPLODED.toByte()
            owner[idx] = player.toByte()
            boomSeq++
            boomBy = player
            when (mode) {
                MineMode.COOP, MineMode.SOLO -> if (--lives <= 0) status = MineStatus.LOST
                MineMode.VERSUS -> score[player] -= MINE_PENALTY
            }
            return true
        }
        flood(idx, player)
        if (revealedSafe >= safeTotal) status = MineStatus.WON
        return true
    }

    /** Toggles a flag on a covered cell. Either player may remove the other's flag. */
    fun flag(idx: Int, player: Int): Boolean {
        if (status != MineStatus.PLAYING || idx !in 0 until n) return false
        when (state[idx].toInt()) {
            Cell.HIDDEN -> { state[idx] = Cell.FLAGGED.toByte(); owner[idx] = player.toByte() }
            Cell.FLAGGED -> state[idx] = Cell.HIDDEN.toByte()
            else -> return false
        }
        return true
    }

    /** Debug: uncovers up to [count] random safe cells. Goes through [reveal] so scores and the win check stay right. */
    fun revealSafe(count: Int, player: Int) {
        if (!generated) reveal(rng.nextInt(n), player)
        var left = count
        var guard = n * 4
        while (left > 0 && status == MineStatus.PLAYING && guard-- > 0) {
            val i = rng.nextInt(n)
            if (!mine[i] && state[i].toInt() != Cell.REVEALED) {
                state[i] = Cell.HIDDEN.toByte() // lift a flag first so reveal() accepts it
                reveal(i, player)
                left--
            }
        }
    }

    /** Debug: gives the shared lives back and resumes a co-op game lost on the last mine. */
    fun restoreLives() {
        lives = startLives
        if (status == MineStatus.LOST) status = MineStatus.PLAYING
    }

    fun snapshot(xray: Boolean = false): MineSnap {
        val over = status != MineStatus.PLAYING
        val cells = ByteArray(n)
        for (i in 0 until n) {
            val s = state[i].toInt()
            val showMine = mine[i] && (over || xray || s == Cell.EXPLODED)
            val showAdj = s == Cell.REVEALED || over || xray
            cells[i] = Cell.pack(s, owner[i].toInt(), showMine, if (showAdj) adj[i].toInt() else 0)
        }
        return MineSnap(mode.ordinal, w, h, difficulty.mines, lives, score[0], score[1], status, boomSeq, boomBy, generated, cells)
    }

    private fun flood(start: Int, player: Int) {
        var top = 0
        stack[top++] = start
        state[start] = Cell.REVEALED.toByte()
        while (top > 0) {
            val i = stack[--top]
            owner[i] = player.toByte()
            revealedSafe++
            score[player]++
            if (adj[i].toInt() != 0) continue
            val x = i % w
            val y = i / w
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                val j = ny * w + nx
                if (state[j].toInt() == Cell.HIDDEN && !mine[j]) {
                    state[j] = Cell.REVEALED.toByte()
                    stack[top++] = j
                }
            }
        }
    }

    private fun generate(safe: Int) {
        val sx = safe % w
        val sy = safe / w
        var placed = 0
        while (placed < difficulty.mines) {
            val i = rng.nextInt(n)
            if (mine[i]) continue
            val dx = i % w - sx
            val dy = i / w - sy
            if (dx in -1..1 && dy in -1..1) continue
            mine[i] = true
            placed++
        }
        for (i in 0 until n) {
            if (mine[i]) continue
            var c = 0
            val x = i % w
            val y = i / w
            for (dy in -1..1) for (dx in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if ((dx != 0 || dy != 0) && nx in 0 until w && ny in 0 until h && mine[ny * w + nx]) c++
            }
            adj[i] = c.toByte()
        }
        generated = true
    }
}
