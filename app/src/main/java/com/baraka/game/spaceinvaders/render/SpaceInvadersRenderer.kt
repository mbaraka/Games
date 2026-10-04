package com.baraka.game.spaceinvaders.render

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
import com.baraka.game.spaceinvaders.model.Alien
import com.baraka.game.spaceinvaders.model.Bullet
import com.baraka.game.spaceinvaders.model.Shield
import com.baraka.game.spaceinvaders.model.SpaceInvadersState
import com.baraka.game.spaceinvaders.model.alienX
import com.baraka.game.spaceinvaders.model.alienY
import com.baraka.game.knightsurvivor.model.VectorIcon
import com.baraka.game.knightsurvivor.render.drawVectorIcon

private val PLAYER_COLOR = Color(0xFF66BB6A)
private val PLAYER_BULLET_COLOR = Color(0xFFECEFF1)
private val ALIEN_BULLET_COLOR = Color(0xFFFF8A65)
private val SHIELD_COLOR = Color(0xFF4FC3F7)

/** Row 0 (top, worth the most) to row 4 — a cool-to-warm gradient plus a distinct icon per row so the formation reads at a glance. */
private val ROW_COLORS = listOf(
    Color(0xFFEF5350), Color(0xFFFFB74D), Color(0xFFFFF176), Color(0xFF81C784), Color(0xFF64B5F6)
)
private val ROW_ICONS = listOf(
    VectorIcon.Imp, VectorIcon.Ghost, VectorIcon.Slime, VectorIcon.Golem, VectorIcon.Mimic
)

@Composable
fun SpaceInvadersRenderer(state: SpaceInvadersState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawStarfield(state.wave)
        state.shields.forEach { drawShield(it) }
        state.aliens.forEach { alien -> if (alien.alive) drawAlien(alien, state.alienFormationOffsetX, state.alienDescent) }
        state.bullets.forEach { drawBullet(it) }
        drawPlayer(state)
    }
}

private fun DrawScope.px(xLogical: Float): Float = xLogical * size.width
private fun DrawScope.py(yLogical: Float): Float = yLogical * size.height

/** A handful of fixed "stars" seeded by wave number, purely decorative. */
private fun DrawScope.drawStarfield(wave: Int) {
    val rng = kotlin.random.Random(wave * 7919)
    repeat(36) {
        val x = rng.nextFloat()
        val y = rng.nextFloat()
        val r = size.width * (0.0015f + rng.nextFloat() * 0.002f)
        drawCircle(Color.White.copy(alpha = 0.25f + rng.nextFloat() * 0.3f), radius = r, center = Offset(px(x), py(y)))
    }
}

private fun DrawScope.drawAlien(alien: Alien, offsetX: Float, descent: Float) {
    val cx = px(alienX(offsetX, alien.col))
    val cy = py(alienY(alien.row, descent))
    val radius = px(SpaceInvadersState.ALIEN_HALF_SIZE) * 1.15f
    val color = ROW_COLORS[alien.row % ROW_COLORS.size]
    val icon = ROW_ICONS[alien.row % ROW_ICONS.size]
    drawVectorIcon(icon = icon, center = Offset(cx, cy), radius = radius, color = color)
}

private fun DrawScope.drawShield(shield: Shield) {
    if (shield.hp <= 0) return
    val alpha = (0.35f + 0.65f * (shield.hp / SpaceInvadersState.SHIELD_MAX_HP.toFloat())).coerceIn(0.15f, 1f)
    val cx = px(shield.x)
    val top = py(SpaceInvadersState.SHIELD_Y - SpaceInvadersState.SHIELD_HALF_HEIGHT)
    val w = px(SpaceInvadersState.SHIELD_HALF_WIDTH) * 2f
    val h = py(SpaceInvadersState.SHIELD_HALF_HEIGHT) * 2f
    drawRoundRect(
        color = SHIELD_COLOR.copy(alpha = alpha),
        topLeft = Offset(cx - w / 2f, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(w * 0.18f)
    )
}

private fun DrawScope.drawBullet(bullet: Bullet) {
    val halfW = px(SpaceInvadersState.BULLET_HALF_WIDTH)
    val halfH = halfW * 2.2f
    val color = if (bullet.fromPlayer) PLAYER_BULLET_COLOR else ALIEN_BULLET_COLOR
    drawRoundRect(
        color = color,
        topLeft = Offset(px(bullet.x) - halfW, py(bullet.y) - halfH),
        size = Size(halfW * 2f, halfH * 2f),
        cornerRadius = CornerRadius(halfW)
    )
}

private fun DrawScope.drawPlayer(state: SpaceInvadersState) {
    val flashing = state.playerInvincibleSeconds > 0f && (state.playerInvincibleSeconds * 12).toInt() % 2 == 0
    val color = if (flashing) Color.White else PLAYER_COLOR
    val cx = px(state.playerX)
    val cy = py(SpaceInvadersState.PLAYER_Y)
    val halfW = px(SpaceInvadersState.PLAYER_HALF_WIDTH)
    val height = halfW * 1.1f

    val hull = Path().apply {
        moveTo(cx, cy - height * 0.8f)
        lineTo(cx + halfW, cy + height * 0.5f)
        lineTo(cx - halfW, cy + height * 0.5f)
        close()
    }
    drawPath(hull, color)
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - halfW * 0.5f, cy + height * 0.3f),
        size = Size(halfW, height * 0.35f),
        cornerRadius = CornerRadius(halfW * 0.15f)
    )
    drawCircle(Color.White.copy(alpha = 0.7f), radius = halfW * 0.18f, center = Offset(cx, cy - height * 0.15f))
}
