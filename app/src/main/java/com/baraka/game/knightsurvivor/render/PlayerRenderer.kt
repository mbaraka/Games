package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.WeaponActivationEvent
import com.baraka.game.knightsurvivor.model.WeaponTargeting
import com.baraka.game.knightsurvivor.systems.WeaponFiringSystem

private val PLAYER_BODY_COLOR = Color(0xFF42A5F5)
private val INVULNERABLE_FLASH_COLOR = Color(0xFFFF5252)
private val HELMET_COLOR = Color(0xFFB0BEC5)
private val PLUME_COLOR = Color(0xFFEF5350)
private val VISOR_COLOR = Color(0xFF12141A)
private val BLADE_COLOR = Color(0xFFCFD8DC)

/** Angle the held-out blade is drawn at, relative to the knight facing "up." */
private const val BLADE_ANGLE_DEGREES = -69.2f

/** Draws the knight — a helmed, sword-bearing little character with a plain shield body — its currently-owned ORBIT weapons spinning around it, and its HP bar. */
fun DrawScope.drawPlayer(player: PlayerState, nowMs: Long) {
    val isInvulnerable = player.isInvulnerableAt(nowMs)
    val flashOn = isInvulnerable && (nowMs / 80L) % 2L == 0L
    val bodyColor = if (flashOn) INVULNERABLE_FLASH_COLOR else PLAYER_BODY_COLOR
    val center = player.position.toOffset()
    val radius = player.radius

    drawShield(center = Offset(center.x, center.y + radius * 0.32f), radius = radius * 0.82f, color = bodyColor, emblem = false)
    drawHelmet(center = Offset(center.x, center.y - radius * 0.62f), radius = radius * 0.5f)
    drawKnightBlade(knightCenter = center, knightRadius = radius)

    drawOrbitWeapons(player)
    drawPlayerHpBar(player)
}

/** A winged/plumed helm with a single horizontal eye-slit sitting above the shield body — gives the knight a face, the same way enemies get one, instead of reading as a bare geometric shape. */
private fun DrawScope.drawHelmet(center: Offset, radius: Float) {
    val domePath = Path().apply {
        addArc(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius), 180f, 180f)
        lineTo(center.x + radius, center.y + radius * 0.55f)
        lineTo(center.x - radius, center.y + radius * 0.55f)
        close()
    }
    drawPath(domePath, color = HELMET_COLOR)
    drawPath(domePath, color = Color.White, style = Stroke(width = 1.5f))

    val plumePath = Path().apply {
        moveTo(center.x, center.y - radius * 1.05f)
        lineTo(center.x - radius * 0.22f, center.y - radius * 0.5f)
        lineTo(center.x + radius * 0.22f, center.y - radius * 0.5f)
        close()
    }
    drawPath(plumePath, color = PLUME_COLOR)
    drawPath(plumePath, color = Color.White, style = Stroke(width = 1.5f))

    // A single horizontal eye-slit — a T-slit (vertical + horizontal) read as a plus/cross at small size.
    drawLine(
        color = VISOR_COLOR,
        start = Offset(center.x - radius * 0.42f, center.y + radius * 0.2f),
        end = Offset(center.x + radius * 0.42f, center.y + radius * 0.2f),
        strokeWidth = radius * 0.2f,
        cap = StrokeCap.Round
    )
}

/** The knight's sword, held out to the side at an angle — clear of the shield's own silhouette instead of hidden behind it. */
private fun DrawScope.drawKnightBlade(knightCenter: Offset, knightRadius: Float) {
    val bladeCenter = Offset(knightCenter.x + knightRadius * 1.05f, knightCenter.y + knightRadius * 0.55f)
    rotate(degrees = BLADE_ANGLE_DEGREES, pivot = bladeCenter) {
        drawBlade(center = bladeCenter, radius = knightRadius * 0.62f, lengthRatio = 2.3f, bladeColor = BLADE_COLOR)
    }
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
