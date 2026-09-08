package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.baraka.game.knightsurvivor.model.KnightDesign

/** The Sworn Blade's default shield tint — used whenever a caller doesn't need to override it (e.g. an invulnerability flash). */
val KNIGHT_DEFAULT_BODY_COLOR = Color(0xFF42A5F5)

private val HELMET_COLOR = Color(0xFFB0BEC5)
private val PLUME_COLOR = Color(0xFFEF5350)
private val VISOR_COLOR = Color(0xFF12141A)
private val BLADE_COLOR = Color(0xFFCFD8DC)
private val GRIP_COLOR_KNIGHT = Color(0xFF6D4C41)

/** Angle the Sworn Blade's held-out sword is drawn at, relative to the knight facing "up." */
private const val BLADE_ANGLE_DEGREES = -69.2f

/**
 * Draws the knight's visual identity for whichever [KnightDesign] is selected — a purely
 * cosmetic dispatch, same shape as [drawVectorIcon]'s `when`, but kept in its own file
 * since each knight design is a small hand-built scene (multiple shapes) rather than one
 * [com.baraka.game.knightsurvivor.model.VectorIcon]. [bodyColor] only affects Sworn Blade
 * (its shield tints with invulnerability-flash/knight-preview color); the other designs
 * have their own fixed palettes since a shield-tint doesn't make sense on a robe or a vest.
 */
fun DrawScope.drawKnightIcon(center: Offset, radius: Float, design: KnightDesign, bodyColor: Color = KNIGHT_DEFAULT_BODY_COLOR) {
    when (design) {
        KnightDesign.SWORN_BLADE -> drawSwornBlade(center, radius, bodyColor)
        KnightDesign.BERSERKER -> drawBerserker(center, radius)
        KnightDesign.BATTLEMAGE -> drawBattlemage(center, radius)
        KnightDesign.GUNNER -> drawGunner(center, radius)
    }
}

// --- Design 1: Sworn Blade — helmet, shield body, held sword. The original/default look. ---

private fun DrawScope.drawSwornBlade(center: Offset, radius: Float, bodyColor: Color) {
    drawShield(center = Offset(center.x, center.y + radius * 0.32f), radius = radius * 0.82f, color = bodyColor)
    drawHelmet(center = Offset(center.x, center.y - radius * 0.62f), radius = radius * 0.5f)

    val bladeCenter = Offset(center.x + radius * 1.05f, center.y + radius * 0.55f)
    rotate(degrees = BLADE_ANGLE_DEGREES, pivot = bladeCenter) {
        drawBlade(center = bladeCenter, radius = radius * 0.62f, lengthRatio = 2.3f, bladeColor = BLADE_COLOR)
    }
}

private fun DrawScope.drawHelmet(center: Offset, radius: Float) {
    val domePath = Path().apply {
        addArc(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius), 180f, 180f)
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

    drawLine(
        color = VISOR_COLOR,
        start = Offset(center.x - radius * 0.42f, center.y + radius * 0.2f),
        end = Offset(center.x + radius * 0.42f, center.y + radius * 0.2f),
        strokeWidth = radius * 0.2f,
        cap = StrokeCap.Round
    )
}

// --- Design 2: Berserker — no helmet, wild hair, war paint, two-handed axe raised overhead. ---

private val BERSERKER_SKIN = Color(0xFFC48958)

private fun DrawScope.drawBerserker(center: Offset, radius: Float) {
    val torsoPath = Path().apply {
        moveTo(center.x - radius * 0.95f, center.y - radius * 0.15f)
        lineTo(center.x - radius * 0.5f, center.y + radius * 0.85f)
        lineTo(center.x + radius * 0.5f, center.y + radius * 0.85f)
        lineTo(center.x + radius * 0.95f, center.y - radius * 0.15f)
        lineTo(center.x + radius * 0.5f, center.y - radius * 0.3f)
        lineTo(center.x - radius * 0.5f, center.y - radius * 0.3f)
        close()
    }
    drawPath(torsoPath, color = BERSERKER_SKIN)
    drawPath(torsoPath, color = Color.White, style = Stroke(width = 2f))

    drawRect(
        color = Color(0xFF4E342E),
        topLeft = Offset(center.x - radius * 0.55f, center.y + radius * 0.55f),
        size = Size(radius * 1.1f, radius * 0.18f)
    )

    val paintColor = Color(0xFF2E2E2E)
    listOf(-0.2f, 0.05f, 0.3f).forEach { t ->
        drawLine(
            color = paintColor,
            start = Offset(center.x + t * radius - radius * 0.15f, center.y - radius * 0.1f),
            end = Offset(center.x + t * radius + radius * 0.15f, center.y + radius * 0.35f),
            strokeWidth = radius * 0.06f
        )
    }

    val headCenter = Offset(center.x, center.y - radius * 0.55f)
    drawCircle(color = BERSERKER_SKIN, radius = radius * 0.32f, center = headCenter)
    drawCircle(color = Color.White, radius = radius * 0.32f, center = headCenter, style = Stroke(width = 2f))

    val beardPath = Path().apply {
        moveTo(headCenter.x - radius * 0.3f, headCenter.y + radius * 0.1f)
        quadraticBezierTo(headCenter.x, headCenter.y + radius * 0.65f, headCenter.x + radius * 0.3f, headCenter.y + radius * 0.1f)
        close()
    }
    drawPath(beardPath, color = Color(0xFF5D4037))
    drawPath(beardPath, color = Color.White, style = Stroke(width = 1.5f))

    listOf(-1f, -0.3f, 0.5f).forEach { t ->
        val spikePath = Path().apply {
            moveTo(headCenter.x + t * radius * 0.3f, headCenter.y - radius * 0.22f)
            lineTo(headCenter.x + t * radius * 0.3f - radius * 0.1f, headCenter.y - radius * 0.65f - kotlin.math.abs(t) * radius * 0.1f)
            lineTo(headCenter.x + t * radius * 0.3f + radius * 0.1f, headCenter.y - radius * 0.22f)
            close()
        }
        drawPath(spikePath, color = Color(0xFF3E2723))
        drawPath(spikePath, color = Color.White, style = Stroke(width = 1f))
    }

    drawLine(Color(0xFF1A1A1A), Offset(headCenter.x - radius * 0.18f, headCenter.y - radius * 0.08f), Offset(headCenter.x - radius * 0.04f, headCenter.y - radius * 0.02f), strokeWidth = 2f)
    drawLine(Color(0xFF1A1A1A), Offset(headCenter.x + radius * 0.18f, headCenter.y - radius * 0.08f), Offset(headCenter.x + radius * 0.04f, headCenter.y - radius * 0.02f), strokeWidth = 2f)

    val axeCenter = Offset(center.x + radius * 0.15f, center.y - radius * 1.35f)
    drawAxe(center = axeCenter, radius = radius * 0.85f, lengthRatio = 1.9f, headColor = Color(0xFFB0BEC5))
}

// --- Design 3: Battlemage — hooded robe, glowing orb-staff. ---

private fun DrawScope.drawBattlemage(center: Offset, radius: Float) {
    val robePath = Path().apply {
        moveTo(center.x, center.y - radius * 0.15f)
        lineTo(center.x - radius * 0.85f, center.y + radius * 0.95f)
        lineTo(center.x + radius * 0.85f, center.y + radius * 0.95f)
        close()
    }
    drawPath(robePath, color = Color(0xFF5E35B1))
    drawPath(robePath, color = Color.White, style = Stroke(width = 2f))
    drawLine(
        color = Color(0xFFB39DDB),
        start = Offset(center.x - radius * 0.85f, center.y + radius * 0.95f),
        end = Offset(center.x + radius * 0.85f, center.y + radius * 0.95f),
        strokeWidth = 2f
    )

    val hoodCenterY = center.y - radius * 0.4f
    val hoodPath = Path().apply {
        moveTo(center.x, hoodCenterY - radius * 0.95f)
        quadraticBezierTo(center.x - radius * 0.55f, hoodCenterY - radius * 0.1f, center.x - radius * 0.4f, hoodCenterY + radius * 0.35f)
        lineTo(center.x + radius * 0.4f, hoodCenterY + radius * 0.35f)
        quadraticBezierTo(center.x + radius * 0.55f, hoodCenterY - radius * 0.1f, center.x, hoodCenterY - radius * 0.95f)
        close()
    }
    drawPath(hoodPath, color = Color(0xFF4527A0))
    drawPath(hoodPath, color = Color.White, style = Stroke(width = 2f))

    drawOval(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(center.x - radius * 0.3f, hoodCenterY + radius * 0.1f - radius * 0.28f),
        size = Size(radius * 0.6f, radius * 0.56f)
    )
    drawCircle(color = Color(0xFF80DEEA), radius = radius * 0.06f, center = Offset(center.x - radius * 0.12f, hoodCenterY + radius * 0.08f))
    drawCircle(color = Color(0xFF80DEEA), radius = radius * 0.06f, center = Offset(center.x + radius * 0.12f, hoodCenterY + radius * 0.08f))

    val staffBase = Offset(center.x + radius * 1.05f, center.y + radius * 0.9f)
    val staffTop = Offset(center.x + radius * 0.85f, center.y - radius * 0.55f)
    drawLine(color = GRIP_COLOR_KNIGHT, start = staffBase, end = staffTop, strokeWidth = radius * 0.1f, cap = StrokeCap.Round)
    drawCircle(color = Color(0xFF80DEEA), radius = radius * 0.18f, center = staffTop)
    drawCircle(color = Color.White, radius = radius * 0.18f, center = staffTop, style = Stroke(width = 1.5f))
}

// --- Design 4: Gunner — tactical helmet, flak vest, machine gun. Deliberately anachronistic. ---

private fun DrawScope.drawGunner(center: Offset, radius: Float) {
    val vestPath = Path().apply {
        moveTo(center.x - radius * 0.6f, center.y - radius * 0.2f)
        lineTo(center.x - radius * 0.68f, center.y + radius * 0.9f)
        lineTo(center.x + radius * 0.68f, center.y + radius * 0.9f)
        lineTo(center.x + radius * 0.6f, center.y - radius * 0.2f)
        close()
    }
    drawPath(vestPath, color = Color(0xFF6D7A5C))
    drawPath(vestPath, color = Color.White, style = Stroke(width = 2f))

    val panelColor = Color(0xFF4A5540)
    drawLine(panelColor, Offset(center.x, center.y - radius * 0.15f), Offset(center.x, center.y + radius * 0.85f), strokeWidth = 1.5f)
    drawRectOutline(panelColor, Offset(center.x - radius * 0.42f, center.y + radius * 0.15f), Size(radius * 0.32f, radius * 0.32f))
    drawRectOutline(panelColor, Offset(center.x + radius * 0.1f, center.y + radius * 0.15f), Size(radius * 0.32f, radius * 0.32f))

    val headCenter = Offset(center.x, center.y - radius * 0.55f)
    val helmetPath = Path().apply {
        addArc(androidx.compose.ui.geometry.Rect(headCenter.x - radius * 0.4f, headCenter.y - radius * 0.4f, headCenter.x + radius * 0.4f, headCenter.y + radius * 0.4f), 180f, 180f)
        lineTo(headCenter.x + radius * 0.4f, headCenter.y + radius * 0.25f)
        lineTo(headCenter.x - radius * 0.4f, headCenter.y + radius * 0.25f)
        close()
    }
    drawPath(helmetPath, color = Color(0xFF556052))
    drawPath(helmetPath, color = Color.White, style = Stroke(width = 2f))

    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(center.x - radius * 0.36f, headCenter.y + radius * 0.02f),
        size = Size(radius * 0.72f, radius * 0.16f)
    )
    drawCircle(color = Color(0xFF80DEEA), radius = radius * 0.06f, center = Offset(center.x - radius * 0.16f, headCenter.y + radius * 0.1f))
    drawCircle(color = Color(0xFF80DEEA), radius = radius * 0.06f, center = Offset(center.x + radius * 0.16f, headCenter.y + radius * 0.1f))

    // machine gun: receiver/barrel + stock + curved magazine, held level to the side
    val gunOrigin = Offset(center.x + radius * 0.55f, center.y + radius * 0.35f)
    drawRect(color = Color(0xFF37474F), topLeft = Offset(gunOrigin.x, gunOrigin.y - radius * 0.1f), size = Size(radius * 0.95f, radius * 0.22f))
    drawRectOutline(Color.White, Offset(gunOrigin.x, gunOrigin.y - radius * 0.1f), Size(radius * 0.95f, radius * 0.22f))

    drawRect(color = Color(0xFF37474F), topLeft = Offset(gunOrigin.x - radius * 0.3f, gunOrigin.y - radius * 0.05f), size = Size(radius * 0.3f, radius * 0.12f))
    drawRectOutline(Color.White, Offset(gunOrigin.x - radius * 0.3f, gunOrigin.y - radius * 0.05f), Size(radius * 0.3f, radius * 0.12f))

    val magazinePath = Path().apply {
        moveTo(gunOrigin.x + radius * 0.28f, gunOrigin.y + radius * 0.12f)
        quadraticBezierTo(gunOrigin.x + radius * 0.22f, gunOrigin.y + radius * 0.5f, gunOrigin.x + radius * 0.4f, gunOrigin.y + radius * 0.6f)
        lineTo(gunOrigin.x + radius * 0.5f, gunOrigin.y + radius * 0.5f)
        quadraticBezierTo(gunOrigin.x + radius * 0.42f, gunOrigin.y + radius * 0.2f, gunOrigin.x + radius * 0.44f, gunOrigin.y + radius * 0.12f)
        close()
    }
    drawPath(magazinePath, color = Color(0xFF263238))
    drawPath(magazinePath, color = Color.White, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawRectOutline(color: Color, topLeft: Offset, size: Size) {
    drawRect(color = color, topLeft = topLeft, size = size, style = Stroke(width = 1.5f))
}
