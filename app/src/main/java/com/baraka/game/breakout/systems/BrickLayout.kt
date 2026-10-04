package com.baraka.game.breakout.systems

import com.baraka.game.breakout.model.Brick
import com.baraka.game.breakout.model.BreakoutState.Companion.COLS
import com.baraka.game.breakout.model.BreakoutState.Companion.ROWS

/** Builds each level's starting brick grid. The only thing that changes between levels is how many of the top rows need 2 hits instead of 1. */
object BrickLayout {

    fun build(level: Int): List<Brick> {
        val toughRows = (level - 1).coerceIn(0, ROWS - 1)
        val bricks = mutableListOf<Brick>()
        var id = 0
        for (row in 0 until ROWS) {
            val hp = if (row < toughRows) 2 else 1
            for (col in 0 until COLS) {
                bricks += Brick(id = id++, row = row, col = col, hp = hp, colorSeed = row)
            }
        }
        return bricks
    }

    /** Top rows are worth more — rewards clearing a path upward rather than just poking the bottom row. */
    fun pointsForRow(row: Int): Int = (ROWS - row) * 10
}
