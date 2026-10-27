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
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.model.WeaponTargeting
import com.baraka.game.knightsurvivor.systems.WeaponFiringSystem
import com.baraka.game.ui.icons.drawVectorIcon

private val INVULNERABLE_FLASH_COLOR = Color(0xFFFF5252)
private val PARTNER_DEFAULT_BODY_COLOR = Color(0xFFFFA726)

/**
 * Draws the knight — whichever [KnightDesign] was picked in Settings — its currently-owned
 * ORBIT weapons spinning around it, and its HP bar. [isPartner] only changes the Sworn
 * Blade design's shield tint (amber instead of blue), so in co-op two players both wearing
 * the same design can still be told apart at a glance.
 */
fun DrawScope.drawPlayer(player: PlayerState, design: KnightDesign, nowMs: Long, isPartner: Boolean = false) {
    val isInvulnerable = player.isInvulnerableAt(nowMs)
    val flashOn = isInvulnerable && (nowMs / 80L) % 2L == 0L
    val defaultColor = if (isPartner) PARTNER_DEFAULT_BODY_COLOR else KNIGHT_DEFAULT_BODY_COLOR
    val bodyColor = if (flashOn) INVULNERABLE_FLASH_COLOR else defaultColor

    drawKnightIcon(center = player.position.toOffset(), radius = player.radius * GameTunables.VISUAL_SCALE, design = design, bodyColor = bodyColor)

    drawOrbitWeapons(player)
    drawPlayerHpBar(player)
}

/** Draws both knights in a co-op run — the local/host player plus, if present, the co-op partner. Solo play (no [com.baraka.game.knightsurvivor.model.GameState.partner]) draws just the one. */
fun DrawScope.drawPlayers(player: PlayerState, partner: PlayerState?, design: KnightDesign, partnerDesign: KnightDesign, nowMs: Long) {
    drawPlayer(player, design, nowMs)
    if (partner != null) drawPlayer(partner, partnerDesign, nowMs, isPartner = true)
}

private fun DrawScope.drawOrbitWeapons(player: PlayerState) {
    player.weapons
        .filter { weapon -> weapon.kind.targeting == WeaponTargeting.ORBIT }
        .forEach { weapon ->
            val orbCount = weapon.kind.orbCountAtLevel(weapon.level)
            val radius = weapon.kind.radiusAtLevel(weapon.level)
            val orbColor = Color(weapon.kind.colorArgb)
            // Grows with level too, not just the orbit distance/count - a maxed-out shield
            // should visibly read as a bigger shield, not just a farther-out small one.
            val orbIconRadius = (10f + (weapon.level - 1) * 3f) * GameTunables.VISUAL_SCALE

            WeaponFiringSystem.orbitPositions(player.position, radius, weapon.orbitAngleRadians, orbCount)
                .forEach { orbPosition ->
                    drawVectorIcon(icon = weapon.kind.icon, center = orbPosition.toOffset(), radius = orbIconRadius, color = orbColor)
                }
        }
}

private fun DrawScope.drawPlayerHpBar(player: PlayerState) {
    val visualRadius = player.radius * GameTunables.VISUAL_SCALE
    val barWidth = visualRadius * 2.2f
    val barHeight = 5f
    val topLeft = Offset(player.position.x - barWidth / 2f, player.position.y - visualRadius - 14f)
    val hpFraction = (player.hp.toFloat() / player.effectiveMaxHp.toFloat()).coerceIn(0f, 1f)

    drawRect(color = Color.DarkGray, topLeft = topLeft, size = Size(barWidth, barHeight))
    drawRect(color = Color(0xFF66BB6A), topLeft = topLeft, size = Size(barWidth * hpFraction, barHeight))
}

/**
 * Draws each AREA_AROUND_PLAYER weapon's most recent activation(s). Most weapons get a
 * swinging slash arc that rotates through the effect's whole lifetime, reading as a weapon
 * swing. Sunburst gets its own expanding radiant-pulse instead - a field weapon scorching
 * everything in range should read as a burst of light, not another blade swing, so it stays
 * visually distinct from Sword Slash/Frying Pan even though all four share the same
 * AREA_AROUND_PLAYER targeting (see model/WeaponKind.kt's "unique effect" rule).
 */
fun DrawScope.drawWeaponActivationEffects(activations: List<WeaponActivationEvent>, nowMs: Long) {
    activations.forEach { event ->
        val ageMs = nowMs - event.activatedAtMs
        val lifeFraction = (1f - ageMs.toFloat() / GameTunables.WEAPON_EFFECT_VISIBLE_MS).coerceIn(0f, 1f)
        if (lifeFraction <= 0f) return@forEach

        if (event.kind == WeaponKind.SUNBURST) {
            drawSunburstPulse(event, lifeFraction)
        } else {
            drawSlashArc(event, lifeFraction)
        }
    }
}

private fun DrawScope.drawSlashArc(event: WeaponActivationEvent, lifeFraction: Float) {
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

/** An expanding, fading ring of sunlight plus a faint filled glow - grows from the knight out to [WeaponActivationEvent.radius] over the effect's lifetime. */
private fun DrawScope.drawSunburstPulse(event: WeaponActivationEvent, lifeFraction: Float) {
    val pulseProgress = 1f - lifeFraction
    val pulseRadius = event.radius * (0.25f + 0.75f * pulseProgress)
    val color = Color(event.kind.colorArgb)

    drawCircle(color = color.copy(alpha = lifeFraction * 0.18f), radius = pulseRadius, center = event.position.toOffset())
    drawCircle(
        color = color.copy(alpha = lifeFraction * 0.85f),
        radius = pulseRadius,
        center = event.position.toOffset(),
        style = Stroke(width = 4f)
    )
}

private const val SLASH_START_ANGLE_DEGREES = -50f
private const val SLASH_SWEEP_DEGREES = 100f
private const val SLASH_TOTAL_ROTATION_DEGREES = 260f
