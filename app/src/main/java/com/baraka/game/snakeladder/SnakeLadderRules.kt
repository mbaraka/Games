package com.baraka.game.snakeladder

/** Classic-style 10×10 board (1–100), zigzag rows, bottom row 1–10 left to right. */
object SnakeLadderRules {
    const val BOARD_SIZE = 100
    const val GRID = 10

    /** Ladder: land on key (bottom), climb to value (top). */
    val ladders: Map<Int, Int> = mapOf(
        1 to 38,
        4 to 14,
        9 to 31,
        21 to 42,
        28 to 84,
        36 to 44,
        51 to 67,
        71 to 91,
        80 to 100
    )

    /** Top of ladder → square of its foot (for UI: where a climb ends). */
    val ladderTopToFoot: Map<Int, Int> by lazy {
        ladders.entries.associate { (foot, top) -> top to foot }
    }

    /** Snake tail → head (for UI: where a slide ends). */
    val snakeTailToHead: Map<Int, Int> by lazy {
        snakes.entries.associate { (head, tail) -> tail to head }
    }

    /** Snake: land on key (head), slide to value (tail). */
    val snakes: Map<Int, Int> = mapOf(
        98 to 13,
        95 to 75,
        92 to 88,
        89 to 68,
        74 to 53,
        64 to 60,
        62 to 19,
        49 to 11,
        48 to 10,
        16 to 6
    )

    /**
     * Top row is row 0, bottom row is 9.
     * Odd row index → numbers increase left to right; even → right to left.
     */
    fun cellAtDisplayGrid(rowFromTop: Int, col: Int): Int {
        require(rowFromTop in 0 until GRID && col in 0 until GRID)
        val base = (GRID - 1 - rowFromTop) * GRID
        return if (rowFromTop % 2 == 1) {
            base + col + 1
        } else {
            base + (GRID - col)
        }
    }

    fun displayGridRowCol(cell: Int): Pair<Int, Int> {
        require(cell in 1..BOARD_SIZE)
        val rowFromTop = GRID - 1 - (cell - 1) / GRID
        val col = if (rowFromTop % 2 == 1) {
            (cell - 1) % GRID
        } else {
            GRID - 1 - (cell - 1) % GRID
        }
        return rowFromTop to col
    }

    fun applySnakesAndLadders(position: Int): Int {
        var p = position
        var hops = 0
        while (hops < 24) {
            hops++
            val ladderDest = ladders[p]
            if (ladderDest != null) {
                p = ladderDest
                continue
            }
            val snakeDest = snakes[p]
            if (snakeDest != null) {
                p = snakeDest
                continue
            }
            break
        }
        return p
    }

    /**
     * [current] 0 = off board; first roll moves to [dice] (1–6) on the track.
     * Overshoot past 100 → no move. Returns new position after slide/climb, or null if no move.
     */
    fun moveAfterRoll(current: Int, dice: Int): Int? {
        require(dice in 1..6)
        if (current == BOARD_SIZE) return null
        val target = when {
            current == 0 -> dice
            else -> current + dice
        }
        if (target > BOARD_SIZE) return null
        return applySnakesAndLadders(target)
    }
}
