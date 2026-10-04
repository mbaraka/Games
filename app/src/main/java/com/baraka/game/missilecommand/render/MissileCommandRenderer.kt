package com.baraka.game.missilecommand.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.missilecommand.model.City
import com.baraka.game.missilecommand.model.EnemyMissile
import com.baraka.game.missilecommand.model.Explosion
import com.baraka.game.missilecommand.model.Interceptor
import com.baraka.game.missilecommand.model.MissileCommandState
import com.baraka.game.missilecommand.model.Silo
import com.baraka.game.missilecommand.model.explosionRadius

private val GROUND_COLOR = Color(0xFF2E3B2E)
private val CITY_COLOR = Color(0xFF66BB6A)
private val CITY_RUBBLE_COLOR = Color(0xFF3A3A3A)
private val SILO_COLOR = Color(0xFF4FC3F7)
private val SILO_DEAD_COLOR = Color(0xFF3A3A3A)
private val ENEMY_TRAIL_COLOR = Color(0xFFFF5252)
private val ENEMY_TRAIL_FAST_COLOR = Color(0xFFFFEB3B)
private val INTERCEPTOR_TRAIL_COLOR = Color(0xFF81D4FA)

@Composable
fun MissileCommandRenderer(state: MissileCommandState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawStarfield(state.wave)
        drawGround()
        state.cities.forEach { drawCity(it) }
        state.silos.forEach { drawSilo(it) }
        state.enemyMissiles.forEach { drawEnemyMissile(it) }
        state.interceptors.forEach { drawInterceptor(it) }
        state.explosions.forEach { drawExplosion(it) }
    }
}

private fun DrawScope.px(xLogical: Float): Float = xLogical * size.width
private fun DrawScope.py(yLogical: Float): Float = yLogical * size.height

private fun DrawScope.drawStarfield(wave: Int) {
    val rng = kotlin.random.Random(wave * 7919)
    repeat(40) {
        val x = rng.nextFloat()
        val y = rng.nextFloat() * MissileCommandState.GROUND_Y
        val r = size.width * (0.0012f + rng.nextFloat() * 0.0018f)
        drawCircle(Color.White.copy(alpha = 0.2f + rng.nextFloat() * 0.3f), radius = r, center = Offset(px(x), py(y)))
    }
}

private fun DrawScope.drawGround() {
    drawRect(
        color = GROUND_COLOR,
        topLeft = Offset(0f, py(MissileCommandState.GROUND_Y)),
        size = Size(size.width, size.height - py(MissileCommandState.GROUND_Y))
    )
}

private fun DrawScope.drawCity(city: City) {
    val cx = px(city.x)
    val baseY = py(MissileCommandState.GROUND_Y)
    val halfW = px(MissileCommandState.CITY_HALF_WIDTH)
    if (!city.alive) {
        drawLine(CITY_RUBBLE_COLOR, Offset(cx - halfW, baseY), Offset(cx + halfW, baseY - halfW * 0.25f), strokeWidth = 4f)
        drawLine(CITY_RUBBLE_COLOR, Offset(cx - halfW * 0.5f, baseY), Offset(cx + halfW * 0.6f, baseY - halfW * 0.5f), strokeWidth = 4f)
        return
    }
    // A little three-block skyline so a city reads as more than a rectangle.
    val blocks = listOf(0.5f to 0.6f, 1f to 1f, 0.6f to 0.75f)
    val blockWidth = halfW * 2f / 3.4f
    blocks.forEachIndexed { i, (widthFrac, heightFrac) ->
        val w = blockWidth * widthFrac
        val h = halfW * 1.6f * heightFrac
        val left = cx - halfW + i * blockWidth * 1.15f
        drawRoundRect(
            color = CITY_COLOR,
            topLeft = Offset(left, baseY - h),
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.15f)
        )
    }
}

private fun DrawScope.drawSilo(silo: Silo) {
    val cx = px(silo.x)
    val baseY = py(MissileCommandState.GROUND_Y)
    val halfW = px(MissileCommandState.SILO_HALF_WIDTH)
    val color = if (silo.alive) SILO_COLOR else SILO_DEAD_COLOR
    val path = Path().apply {
        moveTo(cx, baseY - halfW * 1.8f)
        lineTo(cx + halfW, baseY)
        lineTo(cx - halfW, baseY)
        close()
    }
    drawPath(path, color)
    if (silo.alive) {
        drawCircle(Color.White.copy(alpha = 0.6f), radius = halfW * 0.22f, center = Offset(cx, baseY - halfW * 1.1f))
    }
}

private fun DrawScope.drawEnemyMissile(missile: EnemyMissile) {
    val headX = px(missile.x)
    val headY = py(missile.y)
    val speed = kotlin.math.hypot(missile.vx, missile.vy).coerceAtLeast(0.0001f)
    val trailLen = size.height * 0.05f
    val tailX = headX - (missile.vx / speed) * trailLen
    val tailY = headY - (missile.vy / speed) * trailLen
    val color = if (missile.fast) ENEMY_TRAIL_FAST_COLOR else ENEMY_TRAIL_COLOR
    drawLine(color.copy(alpha = 0.55f), Offset(tailX, tailY), Offset(headX, headY), strokeWidth = if (missile.fast) 4f else 3f)
    drawCircle(color, radius = if (missile.fast) 6f else 4.5f, center = Offset(headX, headY))
}

private fun DrawScope.drawInterceptor(interceptor: Interceptor) {
    val headX = px(interceptor.x)
    val headY = py(interceptor.y)
    val speed = kotlin.math.hypot(interceptor.vx, interceptor.vy).coerceAtLeast(0.0001f)
    val trailLen = size.height * 0.06f
    val tailX = headX - (interceptor.vx / speed) * trailLen
    val tailY = headY - (interceptor.vy / speed) * trailLen
    drawLine(INTERCEPTOR_TRAIL_COLOR.copy(alpha = 0.6f), Offset(tailX, tailY), Offset(headX, headY), strokeWidth = 3f)
    drawCircle(Color.White, radius = 4f, center = Offset(headX, headY))
}

private fun DrawScope.drawExplosion(explosion: Explosion) {
    val radius = explosionRadius(explosion)
    if (radius <= 0.0005f) return
    val center = Offset(px(explosion.x), py(explosion.y))
    val t = (explosion.ageSeconds / MissileCommandState.EXPLOSION_LIFETIME_SECONDS).coerceIn(0f, 1f)
    val alpha = (1f - t * 0.6f).coerceIn(0f, 1f)
    val r = px(radius)
    drawCircle(Color(0xFFFFE082).copy(alpha = alpha), radius = r, center = center)
    drawCircle(Color(0xFFFF7043).copy(alpha = alpha * 0.8f), radius = r, center = center, style = Stroke(width = r * 0.18f))
    drawCircle(Color.White.copy(alpha = alpha), radius = r * 0.35f, center = center)
}
