package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.WeaponActivationEvent
import com.baraka.game.knightsurvivor.model.WeaponTargeting
import com.baraka.game.knightsurvivor.systems.WeaponFiringSystem

private val PLAYER_BODY_COLOR = Color(0xFF42A5F5)
private val INVULNERABLE_FLASH_COLOR = Color(0xFFFF5252)

/** Draws the knight as a heater-shield silhouette, its currently-owned ORBIT weapons spinning around it, and its HP bar. */
fun DrawScope.drawPlayer(player: PlayerState, nowMs: Long) {
    val isInvulnerable = player.isInvulnerableAt(nowMs)
    val flashOn = isInvulnerable && (nowMs / 80L) % 2L == 0L
    val bodyColor = if (flashOn) INVULNERABLE_FLASH_COLOR else PLAYER_BODY_COLOR

    drawShield(center = player.position.toOffset(), radius = player.radius, color = bodyColor)

    drawOrbitWeapons(player)
    drawPlayerHpBar(player)
}

private fun DrawScope.drawOrbitWeapons(player: PlayerState) {
    player.weapons
        .filter { weapon -> weapon.kind.targeting == WeaponTargeting.ORBIT }
        .forEach { weapon ->
            val orbCount = weapon.kind.orbCountAtLevel(weapon.level)
            val radius = weapon.kind.radiusAtLevel(weapon.level)
            val orbColor = Color(weapon.kind.colorArgb)

            WeaponFiringSystem.orbitPositions(player.position, radius, weapon.orbitAngleRadians, orbCount)
                .forEach { orbPosition ->
                    drawVectorIcon(icon = weapon.kind.icon, center = orbPosition.toOffset(), radius = 8f, color = orbColor)
                }
        }
}

private fun DrawScope.drawPlayerHpBar(player: PlayerState) {
    val barWidth = player.radius * 2.2f
    val barHeight = 5f
    val topLeft = Offset(player.position.x - barWidth / 2f, player.position.y - player.radius - 14f)
    val hpFraction = (player.hp.toFloat() / player.effectiveMaxHp.toFloat()).coerceIn(0f, 1f)

    drawRect(color = Color.DarkGray, topLeft = topLeft, size = Size(barWidth, barHeight))
    drawRect(color = Color(0xFF66BB6A), topLeft = topLeft, size = Size(barWidth * hpFraction, barHeight))
}

/** Draws the fading ring left behind by an AREA_AROUND_PLAYER weapon's most recent activation(s). */
fun DrawScope.drawWeaponActivationEffects(activations: List<WeaponActivationEvent>, nowMs: Long) {
    activations.forEach { event ->
        val ageMs = nowMs - event.activatedAtMs
        val lifeFraction = (1f - ageMs.toFloat() / GameTunables.WEAPON_EFFECT_VISIBLE_MS).coerceIn(0f, 1f)
        if (lifeFraction <= 0f) return@forEach

        drawCircle(
            color = Color(event.kind.colorArgb).copy(alpha = lifeFraction * 0.6f),
            radius = event.radius,
            center = event.position.toOffset(),
            style = Stroke(width = 3f)
        )
    }
}
