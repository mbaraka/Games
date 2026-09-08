package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Small reusable polygon primitives shared by every renderer file under render/ (see
 * EnemyRenderer.kt, PlayerRenderer.kt, etc.), so "how to draw an N-sided shape" lives in
 * one place instead of being copy-pasted per entity. Each entity renderer only decides
 * WHICH shape/color/size fits it — plain circles read as flat and interchangeable, so
 * every game entity here gets a shape with actual character.
 */

private fun regularPolygonPath(center: Offset, radius: Float, sides: Int, rotationRadians: Float): Path {
    val path = Path()
    val angleStep = (2.0 * Math.PI / sides).toFloat()
    for (i in 0 until sides) {
        val angle = rotationRadians - HALF_PI + i * angleStep
        val point = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}

/** A regular polygon with [sides] corners, point-up by default, optionally rotated. Fill + outline in one call. */
fun DrawScope.drawPolygon(
    center: Offset,
    radius: Float,
    sides: Int,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f,
    rotationRadians: Float = 0f
) {
    val path = regularPolygonPath(center, radius, sides, rotationRadians)
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
}

/** A five/six/N-pointed star — used to make the boss (or anything else that should feel special) stand out from regular enemies. */
fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    points: Int,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path()
    val angleStep = (Math.PI / points).toFloat()
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -HALF_PI + i * angleStep
        val point = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
}

/** A diamond (rotated square/kite) — the classic "gem" silhouette. Independent half-width/height lets it be squashed into a dagger-like shape for projectiles. */
fun DrawScope.drawDiamond(
    center: Offset,
    halfWidth: Float,
    halfHeight: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path().apply {
        moveTo(center.x, center.y - halfHeight)
        lineTo(center.x + halfWidth, center.y)
        lineTo(center.x, center.y + halfHeight)
        lineTo(center.x - halfWidth, center.y)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
}

/**
 * A pentagon "heater shield" silhouette (flat top, pointed bottom) with a heraldic cross
 * emblem — the one recurring "this is knight gear" cue shared by the Knight's own body
 * and any weapon (Orbit Shield) that should visually read as "a shield," not just an
 * arbitrary polygon.
 */
fun DrawScope.drawShield(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f,
    emblem: Boolean = true
) {
    val height = radius * 1.15f
    val path = Path().apply {
        moveTo(center.x - radius, center.y - height * 0.5f)
        lineTo(center.x - radius, center.y - height * 0.1f)
        lineTo(center.x, center.y + height * 0.6f)
        lineTo(center.x + radius, center.y - height * 0.1f)
        lineTo(center.x + radius, center.y - height * 0.5f)
        lineTo(center.x, center.y - height * 0.8f)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))

    if (emblem) {
        drawRect(
            color = outlineColor,
            topLeft = Offset(center.x - radius * 0.09f, center.y - radius * 0.55f),
            size = Size(radius * 0.18f, radius * 0.75f)
        )
        drawRect(
            color = outlineColor,
            topLeft = Offset(center.x - radius * 0.4f, center.y - radius * 0.28f),
            size = Size(radius * 0.8f, radius * 0.18f)
        )
    }
}

/**
 * A bladed weapon: pointed blade, crossguard, grip, and pommel — reads as a sword or
 * dagger instead of an abstract elongated diamond. [lengthRatio] (relative to [radius])
 * differentiates a short dagger from a longer sword; a shared silhouette otherwise so the
 * two still visually belong to the same "bladed weapon" family.
 */
fun DrawScope.drawBlade(
    center: Offset,
    radius: Float,
    lengthRatio: Float,
    bladeColor: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val tipY = center.y - radius * lengthRatio
    val shoulderY = center.y - radius * 0.35f
    val bladeHalfWidth = radius * 0.26f

    val bladePath = Path().apply {
        moveTo(center.x, tipY)
        lineTo(center.x + bladeHalfWidth, shoulderY)
        lineTo(center.x, shoulderY + radius * 0.16f)
        lineTo(center.x - bladeHalfWidth, shoulderY)
        close()
    }
    drawPath(bladePath, color = bladeColor)
    drawPath(bladePath, color = outlineColor, style = Stroke(width = outlineWidth))

    val guardY = shoulderY + radius * 0.2f
    drawLine(
        color = outlineColor,
        start = Offset(center.x - radius * 0.5f, guardY),
        end = Offset(center.x + radius * 0.5f, guardY),
        strokeWidth = radius * 0.18f,
        cap = StrokeCap.Round
    )

    val gripBottom = center.y + radius * 0.55f
    drawLine(
        color = GRIP_COLOR,
        start = Offset(center.x, guardY + radius * 0.08f),
        end = Offset(center.x, gripBottom),
        strokeWidth = radius * 0.22f,
        cap = StrokeCap.Round
    )

    val pommelCenter = Offset(center.x, gripBottom + radius * 0.12f)
    drawCircle(color = bladeColor, radius = radius * 0.16f, center = pommelCenter)
    drawCircle(color = outlineColor, radius = radius * 0.16f, center = pommelCenter, style = Stroke(width = outlineWidth))
}

/** A handle with a crescent axe-head — distinct silhouette from [drawBlade] so a thrown axe reads as an axe, not another dagger. */
fun DrawScope.drawAxe(
    center: Offset,
    radius: Float,
    lengthRatio: Float,
    headColor: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val handleTop = center.y - radius * lengthRatio * 0.5f
    val handleBottom = center.y + radius * 0.6f
    drawLine(
        color = GRIP_COLOR,
        start = Offset(center.x, handleTop),
        end = Offset(center.x, handleBottom),
        strokeWidth = radius * 0.16f,
        cap = StrokeCap.Round
    )

    val headCenter = Offset(center.x + radius * 0.1f, handleTop)
    val headRadius = radius * 0.6f
    val headPath = Path().apply {
        addArc(
            Rect(headCenter.x - headRadius, headCenter.y - headRadius, headCenter.x + headRadius, headCenter.y + headRadius),
            startAngleDegrees = -110f,
            sweepAngleDegrees = 220f
        )
        lineTo(center.x, handleTop - radius * 0.1f)
        close()
    }
    drawPath(headPath, color = headColor)
    drawPath(headPath, color = outlineColor, style = Stroke(width = outlineWidth))

    val pommelCenter = Offset(center.x, handleBottom + radius * 0.1f)
    drawCircle(color = headColor, radius = radius * 0.14f, center = pommelCenter)
    drawCircle(color = outlineColor, radius = radius * 0.14f, center = pommelCenter, style = Stroke(width = outlineWidth))
}

/** A heart via two symmetric cubic beziers meeting at a bottom point. */
fun DrawScope.drawHeart(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path().apply {
        moveTo(center.x, center.y + radius * 0.7f)
        cubicTo(
            center.x - radius * 1.3f, center.y - radius * 0.1f,
            center.x - radius * 0.5f, center.y - radius * 1.1f,
            center.x, center.y - radius * 0.3f
        )
        cubicTo(
            center.x + radius * 0.5f, center.y - radius * 1.1f,
            center.x + radius * 1.3f, center.y - radius * 0.1f,
            center.x, center.y + radius * 0.7f
        )
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
}

private val GRIP_COLOR = Color(0xFF6D4C41)
private val HALF_PI = (Math.PI / 2).toFloat()
