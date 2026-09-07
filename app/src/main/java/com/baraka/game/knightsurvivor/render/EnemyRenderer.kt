package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.model.EnemyInstance

/**
 * Draws every enemy using its [com.baraka.game.knightsurvivor.model.EnemyKind.icon]
 * silhouette — distinct shapes read faster at a glance than same-shape/different-color
 * squares. A new EnemyKind just needs an icon assigned in its own enum entry; this file
 * never changes.
 */
fun DrawScope.drawEnemies(enemies: List<EnemyInstance>, nowMs: Long) {
    enemies.forEach { enemy -> drawEnemy(enemy, nowMs) }
}

private fun DrawScope.drawEnemy(enemy: EnemyInstance, nowMs: Long) {
    val isFlashing = nowMs < enemy.hitFlashUntilMs
    val color = if (isFlashing) Color.White else Color(enemy.kind.colorArgb)

    drawVectorIcon(icon = enemy.kind.icon, center = enemy.position.toOffset(), radius = enemy.radius, color = color)

    if (enemy.isBoss) {
        drawBossHealthBar(enemy)
    }
}

private fun DrawScope.drawBossHealthBar(enemy: EnemyInstance) {
    val barWidth = enemy.radius * 2.4f
    val barHeight = 6f
    val topLeft = Offset(enemy.position.x - barWidth / 2f, enemy.position.y - enemy.radius - 16f)
    val hpFraction = (enemy.hp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)

    drawRect(color = Color.DarkGray, topLeft = topLeft, size = Size(barWidth, barHeight))
    drawRect(color = Color(0xFFE53935), topLeft = topLeft, size = Size(barWidth * hpFraction, barHeight))
}
