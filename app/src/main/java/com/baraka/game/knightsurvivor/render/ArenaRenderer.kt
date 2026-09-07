package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.model.ArenaSize

private val ARENA_FLOOR_COLOR = Color(0xFF1B1F27)
private val GRID_LINE_COLOR = Color.White.copy(alpha = 0.05f)
private const val GRID_SIZE_PX = 48f

/** Draws the arena floor and a faint reference grid. Pure presentation — carries no gameplay data beyond the arena's size. */
fun DrawScope.drawArena(arenaSize: ArenaSize) {
    drawRect(color = ARENA_FLOOR_COLOR, topLeft = Offset.Zero, size = Size(arenaSize.width, arenaSize.height))

    var x = 0f
    while (x <= arenaSize.width) {
        drawLine(GRID_LINE_COLOR, Offset(x, 0f), Offset(x, arenaSize.height), strokeWidth = 1f)
        x += GRID_SIZE_PX
    }
    var y = 0f
    while (y <= arenaSize.height) {
        drawLine(GRID_LINE_COLOR, Offset(0f, y), Offset(arenaSize.width, y), strokeWidth = 1f)
        y += GRID_SIZE_PX
    }
}
