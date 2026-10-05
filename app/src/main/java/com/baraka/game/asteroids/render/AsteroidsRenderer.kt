package com.baraka.game.asteroids.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.asteroids.model.Asteroid
import com.baraka.game.asteroids.model.AsteroidsState
import com.baraka.game.asteroids.model.Bullet
import com.baraka.game.asteroids.model.Ship
import kotlin.math.cos
import kotlin.math.sin

private val SHIP_COLOR = Color(0xFFECEFF1)
private val THRUST_COLOR = Color(0xFFFFB74D)
private val ASTEROID_COLOR = Color(0xFF9E9E9E)
private val BULLET_COLOR = Color(0xFFFFEE58)

@Composable
fun AsteroidsRenderer(state: AsteroidsState, thrusting: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = minOf(size.width, size.height)
        val offsetX = (size.width - scale) / 2f
        val offsetY = (size.height - scale) / 2f

        drawStarfield(state.wave, offsetX, offsetY, scale)
        state.asteroids.forEach { drawWrapped(it.x, it.y, it.size.radius, offsetX, offsetY, scale) { cx, cy -> drawAsteroid(it, cx, cy, scale) } }
        state.bullets.forEach { drawWrapped(it.x, it.y, 0.01f, offsetX, offsetY, scale) { cx, cy -> drawBullet(cx, cy, scale) } }
        val flashing = state.ship.invincibleSeconds > 0f && (state.ship.invincibleSeconds * 10).toInt() % 2 == 0
        drawWrapped(state.ship.x, state.ship.y, AsteroidsState.SHIP_RADIUS, offsetX, offsetY, scale) { cx, cy ->
            drawShip(state.ship, cx, cy, scale, thrusting, flashing)
        }
    }
}

/** Draws [draw] at the entity's primary screen position, plus any wrap-around duplicates needed when it's close enough to an edge to be peeking through the opposite one. */
private inline fun DrawScope.drawWrapped(
    x: Float, y: Float, radius: Float,
    offsetX: Float, offsetY: Float, scale: Float,
    draw: DrawScope.(cx: Float, cy: Float) -> Unit
) {
    val dxs = buildList {
        add(0f)
        if (x < radius) add(1f)
        if (x > 1f - radius) add(-1f)
    }
    val dys = buildList {
        add(0f)
        if (y < radius) add(1f)
        if (y > 1f - radius) add(-1f)
    }
    for (dx in dxs) for (dy in dys) {
        draw(offsetX + (x + dx) * scale, offsetY + (y + dy) * scale)
    }
}

private fun DrawScope.drawStarfield(wave: Int, offsetX: Float, offsetY: Float, scale: Float) {
    val rng = kotlin.random.Random(wave * 7919)
    repeat(50) {
        val x = rng.nextFloat()
        val y = rng.nextFloat()
        val r = scale * (0.0012f + rng.nextFloat() * 0.0016f)
        drawCircle(Color.White.copy(alpha = 0.2f + rng.nextFloat() * 0.3f), radius = r, center = Offset(offsetX + x * scale, offsetY + y * scale))
    }
}

private fun DrawScope.drawShip(ship: Ship, cx: Float, cy: Float, scale: Float, thrusting: Boolean, flashing: Boolean) {
    val r = AsteroidsState.SHIP_RADIUS * scale
    val angle = ship.angleRad
    fun point(distance: Float, offsetAngle: Float): Offset {
        val a = angle + offsetAngle
        return Offset(cx + cos(a) * distance, cy + sin(a) * distance)
    }
    val nose = point(r * 1.3f, 0f)
    val left = point(r * 0.9f, Math.PI.toFloat() * 0.78f)
    val right = point(r * 0.9f, -Math.PI.toFloat() * 0.78f)
    val tailCenter = point(r * 0.35f, Math.PI.toFloat())

    val color = if (flashing) Color.White else SHIP_COLOR
    val path = Path().apply {
        moveTo(nose.x, nose.y)
        lineTo(left.x, left.y)
        lineTo(tailCenter.x, tailCenter.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color, style = Stroke(width = (r * 0.18f).coerceAtLeast(1.5f)))

    if (thrusting) {
        val flameTip = point(r * 1.2f, Math.PI.toFloat())
        val flamePath = Path().apply {
            moveTo(left.x * 0.5f + tailCenter.x * 0.5f, left.y * 0.5f + tailCenter.y * 0.5f)
            lineTo(flameTip.x, flameTip.y)
            lineTo(right.x * 0.5f + tailCenter.x * 0.5f, right.y * 0.5f + tailCenter.y * 0.5f)
            close()
        }
        drawPath(flamePath, THRUST_COLOR.copy(alpha = 0.85f))
    }
}

/** A jagged polygon seeded by [Asteroid.shapeSeed] so the silhouette stays fixed across frames while [Asteroid.rotation] spins it. */
private fun DrawScope.drawAsteroid(asteroid: Asteroid, cx: Float, cy: Float, scale: Float) {
    val rng = kotlin.random.Random(asteroid.shapeSeed)
    val baseRadius = asteroid.size.radius * scale
    val vertexCount = 10
    val path = Path()
    for (i in 0 until vertexCount) {
        val fraction = 0.72f + rng.nextFloat() * 0.5f
        val angle = asteroid.rotation + (i.toFloat() / vertexCount) * (2 * Math.PI).toFloat()
        val px = cx + cos(angle) * baseRadius * fraction
        val py = cy + sin(angle) * baseRadius * fraction
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, ASTEROID_COLOR.copy(alpha = 0.18f))
    drawPath(path, ASTEROID_COLOR, style = Stroke(width = (baseRadius * 0.1f).coerceAtLeast(1.5f)))
}

private fun DrawScope.drawBullet(cx: Float, cy: Float, scale: Float) {
    drawCircle(BULLET_COLOR, radius = (0.006f * scale).coerceAtLeast(2f), center = Offset(cx, cy))
}
