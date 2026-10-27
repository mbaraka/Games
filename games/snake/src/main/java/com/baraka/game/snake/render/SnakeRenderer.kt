package com.baraka.game.snake.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.snake.model.Direction
import com.baraka.game.snake.model.SnakeState
import com.baraka.game.snake.model.SnakeState.Companion.GRID_HEIGHT
import com.baraka.game.snake.model.SnakeState.Companion.GRID_WIDTH

private val BOARD_COLOR_A = Color(0xFF14181F)
private val BOARD_COLOR_B = Color(0xFF171C24)
private val SNAKE_BODY_COLOR = Color(0xFF66BB6A)
private val SNAKE_HEAD_COLOR = Color(0xFF81C784)
private val FOOD_COLOR = Color(0xFFEF5350)

@Composable
fun SnakeRenderer(state: SnakeState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = minOf(size.width / GRID_WIDTH, size.height / GRID_HEIGHT)
        val boardWidth = cellSize * GRID_WIDTH
        val boardHeight = cellSize * GRID_HEIGHT
        val offsetX = (size.width - boardWidth) / 2f
        val offsetY = (size.height - boardHeight) / 2f

        drawCheckerboard(offsetX, offsetY, cellSize)
        drawFood(state, offsetX, offsetY, cellSize)
        drawSnake(state, offsetX, offsetY, cellSize)
    }
}

private fun DrawScope.drawCheckerboard(offsetX: Float, offsetY: Float, cellSize: Float) {
    for (gx in 0 until GRID_WIDTH) {
        for (gy in 0 until GRID_HEIGHT) {
            val color = if ((gx + gy) % 2 == 0) BOARD_COLOR_A else BOARD_COLOR_B
            drawRect(
                color = color,
                topLeft = Offset(offsetX + gx * cellSize, offsetY + gy * cellSize),
                size = Size(cellSize, cellSize)
            )
        }
    }
}

private fun DrawScope.drawFood(state: SnakeState, offsetX: Float, offsetY: Float, cellSize: Float) {
    val cx = offsetX + (state.food.x + 0.5f) * cellSize
    val cy = offsetY + (state.food.y + 0.5f) * cellSize
    drawCircle(FOOD_COLOR, radius = cellSize * 0.38f, center = Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.5f), radius = cellSize * 0.12f, center = Offset(cx - cellSize * 0.12f, cy - cellSize * 0.12f))
    // A tiny stem so it reads as a fruit, not a plain dot.
    drawRect(
        color = Color(0xFF2E7D32),
        topLeft = Offset(cx - cellSize * 0.03f, cy - cellSize * 0.46f),
        size = Size(cellSize * 0.06f, cellSize * 0.14f)
    )
}

private fun DrawScope.drawSnake(state: SnakeState, offsetX: Float, offsetY: Float, cellSize: Float) {
    state.snake.forEachIndexed { index, cell ->
        val isHead = index == 0
        val inset = cellSize * (if (isHead) 0.05f else 0.09f)
        val topLeft = Offset(offsetX + cell.x * cellSize + inset, offsetY + cell.y * cellSize + inset)
        val segmentSize = cellSize - inset * 2f
        drawRoundRect(
            color = if (isHead) SNAKE_HEAD_COLOR else SNAKE_BODY_COLOR,
            topLeft = topLeft,
            size = Size(segmentSize, segmentSize),
            cornerRadius = CornerRadius(segmentSize * 0.3f)
        )
        if (isHead) drawEyes(state.direction, topLeft, segmentSize)
    }
}

private fun DrawScope.drawEyes(direction: Direction, topLeft: Offset, segmentSize: Float) {
    val eyeRadius = segmentSize * 0.09f
    val forward = Offset(direction.dx.toFloat(), direction.dy.toFloat())
    val side = Offset(-forward.y, forward.x) // perpendicular, to place the two eyes apart
    val center = Offset(topLeft.x + segmentSize / 2f, topLeft.y + segmentSize / 2f) + forward * segmentSize * 0.18f
    val eyeOffset = side * segmentSize * 0.22f
    drawCircle(Color.Black, radius = eyeRadius, center = center + eyeOffset)
    drawCircle(Color.Black, radius = eyeRadius, center = center - eyeOffset)
}
