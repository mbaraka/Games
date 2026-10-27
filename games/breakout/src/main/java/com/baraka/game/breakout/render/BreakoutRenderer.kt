package com.baraka.game.breakout.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.breakout.model.Ball
import com.baraka.game.breakout.model.BreakoutState
import com.baraka.game.breakout.model.FallingPowerUp
import com.baraka.game.breakout.model.PowerUpKind
import com.baraka.game.breakout.model.brickRect
import com.baraka.game.ui.icons.VectorIcon
import com.baraka.game.ui.icons.drawVectorIcon

private val BALL_COLOR = Color(0xFFECEFF1)
private val PADDLE_COLOR = Color(0xFF4FC3F7)
private val PADDLE_GLOW = Color(0xFF81D4FA)

/** Row 0 (top, worth the most) to row 5 (bottom) — a warm-to-cool gradient so the field reads at a glance. */
private val ROW_COLORS = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
    Color(0xFF7CB342), Color(0xFF26A69A), Color(0xFF42A5F5)
)

private fun powerUpColor(kind: PowerUpKind): Color = when (kind) {
    PowerUpKind.WIDE_PADDLE -> Color(0xFF4FC3F7)
    PowerUpKind.MULTI_BALL -> Color(0xFFFFD54F)
    PowerUpKind.EXTRA_LIFE -> Color(0xFFEF5350)
}

private fun powerUpIcon(kind: PowerUpKind): VectorIcon = when (kind) {
    PowerUpKind.WIDE_PADDLE -> VectorIcon.Diamond(aspectRatio = 0.5f)
    PowerUpKind.MULTI_BALL -> VectorIcon.Polygon(sides = 3)
    PowerUpKind.EXTRA_LIFE -> VectorIcon.Heart
}

/** Draws the whole field: bricks, paddle, ball(s), and any falling power-ups. Pure render — reads [BreakoutState], changes nothing. */
@Composable
fun BreakoutRenderer(state: BreakoutState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawBricks(state)
        state.powerUps.forEach { drawPowerUp(it) }
        drawPaddle(state)
        state.balls.forEach { drawBall(it) }
    }
}

private fun DrawScope.px(xLogical: Float): Float = xLogical * size.width
private fun DrawScope.py(yLogical: Float): Float = yLogical * size.height

private fun DrawScope.drawBricks(state: BreakoutState) {
    state.bricks.forEach { brick ->
        val rect = brickRect(brick.row, brick.col)
        val base = ROW_COLORS[brick.row % ROW_COLORS.size]
        val color = if (brick.hp > 1) base else base.copy(alpha = 0.82f)
        drawRoundRect(
            color = color,
            topLeft = Offset(px(rect.left), py(rect.top)),
            size = Size(px(rect.right) - px(rect.left), py(rect.bottom) - py(rect.top)),
            cornerRadius = CornerRadius(px(0.006f))
        )
        if (brick.hp > 1) {
            // A thin bright inset outline reads as "reinforced, needs another hit".
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = Offset(px(rect.left) + 2f, py(rect.top) + 2f),
                size = Size(px(rect.right) - px(rect.left) - 4f, py(rect.bottom) - py(rect.top) - 4f),
                cornerRadius = CornerRadius(px(0.004f)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
    }
}

private fun DrawScope.drawPaddle(state: BreakoutState) {
    val half = state.paddleHalfWidth
    val top = BreakoutState.PADDLE_Y - BreakoutState.PADDLE_THICKNESS / 2f
    val left = px(state.paddleX - half)
    val width = px(state.paddleX + half) - left
    val height = py(BreakoutState.PADDLE_THICKNESS)
    drawRoundRect(
        color = PADDLE_COLOR,
        topLeft = Offset(left, py(top)),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f)
    )
    drawRoundRect(
        color = PADDLE_GLOW.copy(alpha = 0.6f),
        topLeft = Offset(left + width * 0.08f, py(top) + height * 0.15f),
        size = Size(width * 0.84f, height * 0.3f),
        cornerRadius = CornerRadius(height / 3f)
    )
}

private fun DrawScope.drawBall(ball: Ball) {
    val radius = px(BreakoutState.BALL_RADIUS)
    val center = Offset(px(ball.x), py(ball.y))
    drawCircle(Color.Black.copy(alpha = 0.25f), radius = radius * 1.15f, center = center + Offset(0f, radius * 0.3f))
    drawCircle(BALL_COLOR, radius = radius, center = center)
    drawCircle(Color.White.copy(alpha = 0.8f), radius = radius * 0.35f, center = center - Offset(radius * 0.3f, radius * 0.3f))
}

private fun DrawScope.drawPowerUp(powerUp: FallingPowerUp) {
    val radius = px(BreakoutState.POWER_UP_RADIUS)
    val center = Offset(px(powerUp.x), py(powerUp.y))
    val color = powerUpColor(powerUp.kind)
    drawCircle(Color.Black.copy(alpha = 0.3f), radius = radius * 1.3f, center = center)
    drawCircle(color.copy(alpha = 0.25f), radius = radius * 1.6f, center = center)
    drawVectorIcon(icon = powerUpIcon(powerUp.kind), center = center, radius = radius * 0.85f, color = color)
}
