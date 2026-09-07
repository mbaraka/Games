package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

/** A pentagon "heater shield" silhouette (flat top, pointed bottom) — the knight's body and its orbiting shields. */
fun DrawScope.drawShield(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
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
}

private val HALF_PI = (Math.PI / 2).toFloat()
