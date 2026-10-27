package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.ui.icons.VectorIcon
import com.baraka.game.ui.icons.drawHeart
import com.baraka.game.ui.icons.drawVectorIcon

/**
 * Draws every enemy using its [com.baraka.game.knightsurvivor.model.EnemyKind.icon]
 * silhouette — distinct shapes read faster at a glance than same-shape/different-color
 * squares. A new EnemyKind just needs an icon assigned in its own enum entry; this file
 * never changes, except for the couple of kinds whose *appearance* depends on live state
 * (Mimic's disguise, Ghost's phase) rather than just its static kind.
 */
fun DrawScope.drawEnemies(enemies: List<EnemyInstance>, nowMs: Long) {
    enemies.forEach { enemy -> drawEnemy(enemy, nowMs) }
}

private fun DrawScope.drawEnemy(enemy: EnemyInstance, nowMs: Long) {
    if (enemy.disguisedAsPickup) {
        // Looks exactly like a health pickup (see render/HealthPickupRenderer.kt) until it wakes up.
        drawHeart(center = enemy.position.toOffset(), radius = 8f * GameTunables.VISUAL_SCALE, color = Color(0xFFE53935))
        return
    }

    val isFlashing = nowMs < enemy.hitFlashUntilMs
    val isPhasedOut = enemy.isPhaseInvulnerableAt(nowMs)
    val baseColor = if (isFlashing) Color.White else Color(enemy.kind.colorArgb)
    val color = if (isPhasedOut) baseColor.copy(alpha = 0.35f) else baseColor
    val visualRadius = enemy.radius * GameTunables.VISUAL_SCALE

    drawVectorIcon(icon = enemy.kind.icon, center = enemy.position.toOffset(), radius = visualRadius, color = color)

    if (nowMs < enemy.stunnedUntilMs) {
        drawStunMarks(enemy, visualRadius, nowMs)
    } else if (nowMs < enemy.slowUntilMs) {
        drawSlowRing(enemy, visualRadius)
    }

    if (enemy.isBoss) {
        drawBossHealthBar(enemy, visualRadius)
    }
}

/** A couple of small spinning ticks above a stunned enemy's head — a classic "seeing stars" cue. */
private fun DrawScope.drawStunMarks(enemy: EnemyInstance, visualRadius: Float, nowMs: Long) {
    val center = Offset(enemy.position.x, enemy.position.y - visualRadius - 10f)
    val spinAngle = (nowMs % 800L) / 800f * (2f * Math.PI.toFloat())
    listOf(0f, Math.PI.toFloat()).forEach { offset ->
        val angle = spinAngle + offset
        val markCenter = Offset(center.x + kotlin.math.cos(angle) * 8f, center.y + kotlin.math.sin(angle) * 4f)
        drawVectorIcon(icon = VectorIcon.Star(points = 4, innerRadiusRatio = 0.4f), center = markCenter, radius = 4f, color = Color(0xFFFFD54F))
    }
}

/** A faint blue ring around a slowed enemy. */
private fun DrawScope.drawSlowRing(enemy: EnemyInstance, visualRadius: Float) {
    drawCircle(color = Color(0xFF4FC3F7).copy(alpha = 0.5f), radius = visualRadius + 4f, center = enemy.position.toOffset(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
}

private fun DrawScope.drawBossHealthBar(enemy: EnemyInstance, visualRadius: Float) {
    val barWidth = visualRadius * 2.4f
    val barHeight = 6f
    val topLeft = Offset(enemy.position.x - barWidth / 2f, enemy.position.y - visualRadius - 16f)
    val hpFraction = (enemy.hp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)

    drawRect(color = Color.DarkGray, topLeft = topLeft, size = Size(barWidth, barHeight))
    drawRect(color = Color(0xFFE53935), topLeft = topLeft, size = Size(barWidth * hpFraction, barHeight))
}
