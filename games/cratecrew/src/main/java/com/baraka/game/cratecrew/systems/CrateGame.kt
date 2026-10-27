package com.baraka.game.cratecrew.systems

import com.baraka.game.cratecrew.model.CrateLevel

/**
 * Pure Sokoban state for one level: grid of walls/targets, crate positions and the player,
 * plus an undo stack so every push can be stepped back. No rendering or input lives here.
 */
class CrateGame(level: CrateLevel) {
    val width: Int
    val height: Int
    private val wall: BooleanArray
    private val target: BooleanArray
    val crates: MutableSet<Int> = mutableSetOf()
    var playerPos: Int = 0
        private set
    var moves: Int = 0
        private set
    var pushes: Int = 0
        private set

    private data class Move(val from: Int, val to: Int, val crateFrom: Int, val crateTo: Int)
    private val history = ArrayDeque<Move>()

    val targetCount: Int

    init {
        val rows = level.layout
        height = rows.size
        width = rows.maxOf { it.length }
        wall = BooleanArray(width * height)
        target = BooleanArray(width * height)
        var player = -1
        for (y in 0 until height) {
            val row = rows[y]
            for (x in 0 until width) {
                val ch = if (x < row.length) row[x] else ' '
                val i = idx(x, y)
                when (ch) {
                    '#' -> wall[i] = true
                    'T' -> target[i] = true
                    'C' -> crates.add(i)
                    'B' -> { crates.add(i); target[i] = true }
                    'P' -> player = i
                    'S' -> { player = i; target[i] = true }
                    ' ' -> wall[i] = true
                }
            }
        }
        playerPos = player
        targetCount = target.count { it }
    }

    private fun idx(x: Int, y: Int) = y * width + x
    private fun xOf(i: Int) = i % width
    private fun yOf(i: Int) = i / width
    private fun inBounds(x: Int, y: Int) = x in 0 until width && y in 0 until height
    fun isTarget(i: Int) = target[i]
    fun isWall(i: Int) = wall[i]

    val isSolved: Boolean get() = crates.isNotEmpty() && crates.all { target[it] }

    /** Attempts one step in (dx, dy) (one of the 4 cardinal directions). Pushes a crate if one is ahead and the cell past it is free. */
    fun move(dx: Int, dy: Int): Boolean {
        val px = xOf(playerPos)
        val py = yOf(playerPos)
        val nx = px + dx
        val ny = py + dy
        if (!inBounds(nx, ny)) return false
        val next = idx(nx, ny)
        if (wall[next]) return false
        if (next in crates) {
            val bx = nx + dx
            val by = ny + dy
            if (!inBounds(bx, by)) return false
            val beyond = idx(bx, by)
            if (wall[beyond] || beyond in crates) return false
            crates.remove(next)
            crates.add(beyond)
            history.addLast(Move(playerPos, next, next, beyond))
            playerPos = next
            moves++
            pushes++
        } else {
            history.addLast(Move(playerPos, next, -1, -1))
            playerPos = next
            moves++
        }
        return true
    }

    fun undo(): Boolean {
        val last = history.removeLastOrNull() ?: return false
        playerPos = last.from
        if (last.crateFrom >= 0) {
            crates.remove(last.crateTo)
            crates.add(last.crateFrom)
            pushes--
        }
        moves--
        return true
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    /** Debug cheat: snaps every crate onto a free target, solving the level instantly. */
    fun solveInstantly() {
        val targets = (0 until width * height).filter { target[it] }.toMutableList()
        val newCrates = mutableSetOf<Int>()
        for (t in targets) if (newCrates.size < crates.size) newCrates.add(t)
        crates.clear()
        crates.addAll(newCrates)
    }
}
