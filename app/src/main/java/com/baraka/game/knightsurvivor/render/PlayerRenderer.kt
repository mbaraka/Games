package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.KnightDesign
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.WeaponActivationEvent
import com.baraka.game.knightsurvivor.model.WeaponTargeting
import com.baraka.game.knightsurvivor.systems.WeaponFiringSystem

private val INVULNERABLE_FLASH_COLOR = Color(0xFFFF5252)

/** Draws the knight — whichever [KnightDesign] the player picked in Settings — its currently-owned ORBIT weapons spinning around it, and its HP bar. */
fun DrawScope.drawPlayer(player: PlayerState, design: KnightDesign, nowMs: Long) {
    val isInvulnerable = player.isInvulnerableAt(nowMs)
    val flashOn = isInvulnerable && (nowMs / 80L) % 2L == 0L
    val bodyColor = if (flashOn) INVULNERABLE_FLASH_COLOR else KNIGHT_DEFAULT_BODY_COLOR

    drawKnightIcon(center = player.position.toOffset(), radius = player.radius, design = design, bodyColor = bodyColor)

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

/**
 * Draws an actual swinging slash arc for each AREA_AROUND_PLAYER weapon's most recent
 * activation(s) — the arc rotates through the effect's whole lifetime, reading as a sword
 * swing instead of a static fading ring.
 */
fun DrawScope.drawWeaponActivationEffects(activations: List<WeaponActivationEvent>, nowMs: Long) {
    activations.forEach { event ->
        val ageMs = nowMs - event.activatedAtMs
        val lifeFraction = (1f - ageMs.toFloat() / GameTunables.WEAPON_EFFECT_VISIBLE_MS).coerceIn(0f, 1f)
        if (lifeFraction <= 0f) return@forEach

        val swingProgress = 1f - lifeFraction
        val startAngleDegrees = SLASH_START_ANGLE_DEGREES + swingProgress * SLASH_TOTAL_ROTATION_DEGREES

        drawArc(
            color = Color(event.kind.colorArgb).copy(alpha = lifeFraction * 0.9f),
            startAngle = startAngleDegrees,
            sweepAngle = SLASH_SWEEP_DEGREES,
            useCenter = false,
            topLeft = Offset(event.position.x - event.radius, event.position.y - event.radius),
            size = Size(event.radius * 2f, event.radius * 2f),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
    }
}

private const val SLASH_START_ANGLE_DEGREES = -50f
private const val SLASH_SWEEP_DEGREES = 100f
private const val SLASH_TOTAL_ROTATION_DEGREES = 260f
