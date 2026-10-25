package com.baraka.game.arcshot.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.baraka.game.arcshot.model.ArcLevel
import com.baraka.game.arcshot.model.GROUND_Y
import com.baraka.game.arcshot.model.LAUNCHER_X
import com.baraka.game.arcshot.systems.ArcGame
import com.baraka.game.arcshot.systems.BALL_RADIUS
import com.baraka.game.arcshot.systems.ShotResult
import com.baraka.game.arcshot.ui.ArcColors
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Draws one frame of an Arc Shot level: sky, ground, obstacles, bullseye target, cannon, ball
 * and trails. [scale]/[ox]/[oy] place the fixed-size puzzle world inside the real canvas (see
 * `ArcPlayScreen.fit()`); [canvasSize] is the untranslated canvas size so the sky/ground can
 * fill the whole screen even when it is wider than the world's own aspect ratio.
 */
fun DrawScope.drawArcScene(
    game: ArcGame,
    level: ArcLevel,
    scale: Float,
    ox: Float,
    oy: Float,
    canvasSize: Size,
    aimVx: Float,
    aimVy: Float,
    aiming: Boolean,
    previewPoints: List<Offset>
) {
    val groundLineY = (oy + GROUND_Y * scale).coerceIn(0f, canvasSize.height)

    drawRect(
        Brush.verticalGradient(listOf(ArcColors.SkyTop, Color(0xFF2F5B8F)), startY = 0f, endY = groundLineY),
        Offset.Zero, Size(canvasSize.width, groundLineY)
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0x33FFF3B0), Color(0x00FFF3B0)),
            center = Offset(canvasSize.width * 0.82f, groundLineY * 0.22f),
            radius = groundLineY * 0.9f
        ),
        radius = groundLineY * 0.9f,
        center = Offset(canvasSize.width * 0.82f, groundLineY * 0.22f)
    )
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF4C7A3E), ArcColors.Ground), startY = groundLineY, endY = canvasSize.height),
        Offset(0f, groundLineY), Size(canvasSize.width, canvasSize.height - groundLineY)
    )
    drawRect(Color(0xFF8FC774), Offset(0f, groundLineY), Size(canvasSize.width, 3f))

    for (r in level.obstacles) {
        val top = oy + r.y * scale
        val rectSize = Size(r.w * scale, r.h * scale)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF9A6B3E), ArcColors.Obstacle), startY = top, endY = top + rectSize.height),
            topLeft = Offset(ox + r.x * scale, top),
            size = rectSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        drawRoundRect(
            Color(0xFF4A3118),
            topLeft = Offset(ox + r.x * scale, top),
            size = rectSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
        )
    }

    drawBullseye(level, scale, ox, oy)
    drawCannon(aimVx, aimVy, aiming, scale, ox, oy)

    if (aiming) {
        for (p in previewPoints) drawCircle(ArcColors.Trail.copy(alpha = 0.55f), 4f, Offset(ox + p.x * scale, oy + p.y * scale))
    }
    if (game.result == ShotResult.FLYING) {
        for (i in 0 until game.trailCount) {
            val (tx, ty) = game.trailPoint(i)
            val alpha = (i + 1f) / game.trailCount * 0.55f
            drawCircle(ArcColors.Trail.copy(alpha = alpha), 3.5f, Offset(ox + tx * scale, oy + ty * scale))
        }
    }

    drawBall(game, scale, ox, oy, groundLineY)
}

private fun DrawScope.drawBullseye(level: ArcLevel, scale: Float, ox: Float, oy: Float) {
    val center = Offset(ox + level.targetX * scale, oy + level.targetY * scale)
    val r = level.targetRadius * scale
    drawCircle(Color(0x33000000), r * 1.08f, center + Offset(3f, 5f))
    drawCircle(ArcColors.TargetRing, r * 1.08f, center)
    drawCircle(ArcColors.Target, r * 0.74f, center)
    drawCircle(Color.White, r * 0.46f, center)
    drawCircle(ArcColors.Target, r * 0.2f, center)
}

private fun DrawScope.drawCannon(aimVx: Float, aimVy: Float, aiming: Boolean, scale: Float, ox: Float, oy: Float) {
    val baseX = ox + LAUNCHER_X * scale
    val baseY = oy + GROUND_Y * scale
    val angle = if (aiming && hypot(aimVx, aimVy) > 1f) atan2(aimVy, aimVx) else Math.toRadians(-40.0).toFloat()
    val degrees = Math.toDegrees(angle.toDouble()).toFloat()

    drawCircle(Color(0x33000000), 15f * scale * 0.08f + 14f, Offset(baseX + 2f, baseY + 3f))
    rotate(degrees = degrees, pivot = Offset(baseX, baseY - 6f)) {
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF6B7684), Color(0xFF384252))),
            topLeft = Offset(baseX - 9f, baseY - 6f - 42f),
            size = Size(18f, 42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )
    }
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF7D8896), Color(0xFF2B3340)), center = Offset(baseX - 6f, baseY - 10f), radius = 22f),
        radius = 20f,
        center = Offset(baseX, baseY - 6f)
    )
}

private fun DrawScope.drawBall(game: ArcGame, scale: Float, ox: Float, oy: Float, groundLineY: Float) {
    val cx = ox + game.ballX * scale
    val cy = oy + game.ballY * scale
    val r = BALL_RADIUS * scale
    if (cy < groundLineY - r * 0.4f) {
        val shadowY = groundLineY.coerceAtMost(oy + GROUND_Y * scale)
        drawOval(Color(0x2A000000), Offset(cx - r * 0.9f, shadowY - r * 0.35f), Size(r * 1.8f, r * 0.7f))
    }
    val base = if (game.result == ShotResult.HIT) ArcColors.Good else ArcColors.Ball
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), base), center = Offset(cx - r * 0.3f, cy - r * 0.3f), radius = r * 1.6f),
        radius = r,
        center = Offset(cx, cy)
    )
}
