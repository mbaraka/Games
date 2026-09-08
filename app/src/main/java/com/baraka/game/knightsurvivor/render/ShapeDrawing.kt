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

/** A pentagon "heater shield" silhouette (flat top, pointed bottom) — the Knight's own body, and any weapon that should visually read as "a shield." */
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

/** A classic rubber duck: oval body, round head with a small orange beak, and an eye. */
fun DrawScope.drawDuck(
    center: Offset,
    radius: Float,
    bodyColor: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val bodyCenter = Offset(center.x, center.y + radius * 0.25f)
    val bodySize = Size(radius * 1.8f, radius * 1.1f)
    val bodyTopLeft = Offset(bodyCenter.x - bodySize.width / 2f, bodyCenter.y - bodySize.height / 2f)
    drawOval(color = bodyColor, topLeft = bodyTopLeft, size = bodySize)
    drawOval(color = outlineColor, topLeft = bodyTopLeft, size = bodySize, style = Stroke(width = outlineWidth))

    val headCenter = Offset(center.x + radius * 0.45f, center.y - radius * 0.5f)
    val headRadius = radius * 0.45f
    drawCircle(color = bodyColor, radius = headRadius, center = headCenter)
    drawCircle(color = outlineColor, radius = headRadius, center = headCenter, style = Stroke(width = outlineWidth))

    val beakPath = Path().apply {
        moveTo(headCenter.x + headRadius * 0.75f, headCenter.y - headRadius * 0.1f)
        lineTo(headCenter.x + headRadius * 1.7f, headCenter.y + headRadius * 0.15f)
        lineTo(headCenter.x + headRadius * 0.75f, headCenter.y + headRadius * 0.55f)
        close()
    }
    drawPath(beakPath, color = DUCK_BEAK_COLOR)
    drawPath(beakPath, color = outlineColor, style = Stroke(width = outlineWidth * 0.7f))

    drawCircle(
        color = Color(0xFF1A1A1A),
        radius = headRadius * 0.16f,
        center = Offset(headCenter.x + headRadius * 0.2f, headCenter.y - headRadius * 0.25f)
    )
}

/** A hexagonal shell: the hexagon outline plus radial lines to each corner, like turtle-shell panels. */
fun DrawScope.drawTurtleShell(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    drawPolygon(center, radius, sides = 6, color = color, outlineColor = outlineColor, outlineWidth = outlineWidth)

    val panelColor = outlineColor.copy(alpha = outlineColor.alpha * 0.7f)
    val angleStep = (2.0 * Math.PI / 6).toFloat()
    for (i in 0 until 6) {
        val angle = -HALF_PI + i * angleStep
        val corner = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        drawLine(color = panelColor, start = center, end = corner, strokeWidth = outlineWidth * 0.8f)
    }
}

/** A round pan (circle + rim highlight) with a handle, plus a few "bonk" impact lines. */
fun DrawScope.drawFryingPan(
    center: Offset,
    radius: Float,
    panColor: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val panCenter = Offset(center.x - radius * 0.15f, center.y - radius * 0.2f)
    val panRadius = radius * 0.65f
    drawCircle(color = panColor, radius = panRadius, center = panCenter)
    drawCircle(color = outlineColor, radius = panRadius, center = panCenter, style = Stroke(width = outlineWidth))
    drawCircle(color = RIM_HIGHLIGHT_COLOR, radius = panRadius * 0.68f, center = panCenter, style = Stroke(width = outlineWidth * 0.7f))

    val handleStart = Offset(panCenter.x + cos(HANDLE_ANGLE) * panRadius, panCenter.y + sin(HANDLE_ANGLE) * panRadius)
    val handleEnd = Offset(center.x + radius * 0.95f, center.y + radius * 0.85f)
    drawLine(color = GRIP_COLOR, start = handleStart, end = handleEnd, strokeWidth = radius * 0.16f, cap = StrokeCap.Round)

    listOf(Offset(-1f, -0.3f), Offset(1f, -0.5f), Offset(0.2f, -1f)).forEach { dir ->
        drawLine(
            color = BONK_SPARK_COLOR,
            start = Offset(panCenter.x + dir.x * panRadius * 0.7f, panCenter.y + dir.y * panRadius * 0.7f),
            end = Offset(panCenter.x + dir.x * panRadius * 0.95f, panCenter.y + dir.y * panRadius * 0.95f),
            strokeWidth = outlineWidth
        )
    }
}

/**
 * A curved banana/crescent — two arcs of different radii sharing endpoints, not a straight
 * stick, so it reads as a boomerang rather than a stick or a blade.
 */
fun DrawScope.drawBoomerang(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path().apply {
        addArc(
            Rect(center.x - radius * 1.05f, center.y - radius * 1.05f, center.x + radius * 1.05f, center.y + radius * 1.05f),
            startAngleDegrees = 100f,
            sweepAngleDegrees = 150f
        )
        val outerEnd = Offset(
            center.x + radius * 1.05f * cos(Math.toRadians(250.0)).toFloat(),
            center.y + radius * 1.05f * sin(Math.toRadians(250.0)).toFloat()
        )
        lineTo(outerEnd.x, outerEnd.y)
        arcTo(
            Rect(center.x - radius * 0.55f, center.y - radius * 0.55f, center.x + radius * 0.55f, center.y + radius * 0.55f),
            startAngleDegrees = 250f,
            sweepAngleDegrees = -150f,
            forceMoveTo = false
        )
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
}

/** A horseshoe magnet: a thick red arc with clearly separated, outward-angled silver tips. */
fun DrawScope.drawMagnet(
    center: Offset,
    radius: Float,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val armWidth = radius * 0.34f
    drawArc(
        color = MAGNET_RED,
        startAngle = 9f,
        sweepAngle = 162f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.6f),
        size = Size(radius, radius),
        style = Stroke(width = armWidth, cap = StrokeCap.Butt)
    )
    listOf(-1f, 1f).forEach { side ->
        val tipTop = Offset(center.x + side * radius * 0.5f, center.y + radius * 0.42f)
        rotate(degrees = side * 7f, pivot = tipTop) {
            val rectPath = Path().apply {
                moveTo(tipTop.x - armWidth * 0.5f, tipTop.y)
                lineTo(tipTop.x + armWidth * 0.5f, tipTop.y)
                lineTo(tipTop.x + armWidth * 0.5f, tipTop.y + radius * 0.55f)
                lineTo(tipTop.x - armWidth * 0.5f, tipTop.y + radius * 0.55f)
                close()
            }
            drawPath(rectPath, color = Color(0xFFCFD8DC))
            drawPath(rectPath, color = outlineColor, style = Stroke(width = outlineWidth))
        }
    }
}

/** Three small bundled sticks with a lit fuse — Firecracker String. */
fun DrawScope.drawFirecracker(
    center: Offset,
    radius: Float,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    rotate(degrees = -18f, pivot = center) {
        listOf(-1, 0, 1).forEach { i ->
            val stickColor = if (i == 0) Color(0xFFE53935) else Color(0xFFEF5350)
            val topLeft = Offset(center.x + i * radius * 0.5f - radius * 0.16f, center.y - radius * 0.6f)
            val path = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        Rect(topLeft, Size(radius * 0.32f, radius * 1.1f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.08f)
                    )
                )
            }
            drawPath(path, color = stickColor)
            drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
        }
        val fusePath = Path().apply {
            moveTo(center.x, center.y - radius * 0.6f)
            quadraticBezierTo(center.x + radius * 0.2f, center.y - radius * 0.95f, center.x + radius * 0.05f, center.y - radius * 1.15f)
        }
        drawPath(fusePath, color = GRIP_COLOR, style = Stroke(width = outlineWidth))
        drawCircle(color = Color(0xFFFFB300), radius = radius * 0.09f, center = Offset(center.x + radius * 0.05f, center.y - radius * 1.2f))
    }
}

/** An egg silhouette (two beziers meeting at top and bottom) with a soft highlight. */
fun DrawScope.drawEgg(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        cubicTo(
            center.x + radius * 0.85f, center.y - radius * 0.6f,
            center.x + radius * 0.75f, center.y + radius * 0.9f,
            center.x, center.y + radius * 0.95f
        )
        cubicTo(
            center.x - radius * 0.75f, center.y + radius * 0.9f,
            center.x - radius * 0.85f, center.y - radius * 0.6f,
            center.x, center.y - radius
        )
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
    drawOval(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(center.x - radius * 0.5f, center.y - radius * 0.5f),
        size = Size(radius * 0.3f, radius * 0.3f)
    )
}

/** A rounded blob with a glossy highlight and two dot eyes — Slime. */
fun DrawScope.drawSlime(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path().apply {
        moveTo(center.x - radius, center.y + radius * 0.3f)
        quadraticBezierTo(center.x - radius * 1.05f, center.y - radius * 0.6f, center.x, center.y - radius * 0.75f)
        quadraticBezierTo(center.x + radius * 1.05f, center.y - radius * 0.6f, center.x + radius, center.y + radius * 0.3f)
        quadraticBezierTo(center.x + radius * 0.6f, center.y + radius * 0.95f, center.x, center.y + radius * 0.95f)
        quadraticBezierTo(center.x - radius * 0.6f, center.y + radius * 0.95f, center.x - radius, center.y + radius * 0.3f)
        close()
    }
    drawPath(path, color = color, alpha = 0.85f)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
    drawOval(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(center.x - radius * 0.52f, center.y - radius * 0.34f),
        size = Size(radius * 0.44f, radius * 0.28f)
    )
    listOf(-1f, 1f).forEach { side ->
        drawCircle(color = Color(0xFF1A1A1A), radius = radius * 0.09f, center = Offset(center.x + side * radius * 0.28f, center.y + radius * 0.05f))
    }
}

/** A classic wavy-bottomed ghost silhouette, semi-transparent. */
fun DrawScope.drawGhost(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val waves = 4
    val path = Path().apply {
        addArc(
            Rect(center.x - radius * 0.85f, center.y - radius * 0.95f, center.x + radius * 0.85f, center.y + radius * 0.75f),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f
        )
        lineTo(center.x + radius * 0.85f, center.y + radius * 0.6f)
        for (i in 0 until waves) {
            val x1 = center.x + radius * 0.85f - (i + 0.5f) * (radius * 1.7f / waves)
            val x2 = center.x + radius * 0.85f - (i + 1) * (radius * 1.7f / waves)
            val dipY = if (i % 2 == 0) center.y + radius * 0.85f else center.y + radius * 0.55f
            quadraticBezierTo(x1, dipY, x2, center.y + radius * 0.6f)
        }
        close()
    }
    drawPath(path, color = color, alpha = 0.8f)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
    listOf(-1f, 1f).forEach { side ->
        drawOval(
            color = Color(0xFF1A1A1A),
            topLeft = Offset(center.x + side * radius * 0.28f - radius * 0.12f, center.y - radius * 0.26f),
            size = Size(radius * 0.24f, radius * 0.32f)
        )
    }
}

/** A small red face with dark horns and a grin — Imp. */
fun DrawScope.drawImp(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    listOf(-1f, 1f).forEach { side ->
        val hornPath = Path().apply {
            moveTo(center.x + side * radius * 0.5f, center.y - radius * 0.65f)
            lineTo(center.x + side * radius * 0.95f, center.y - radius * 1.35f)
            lineTo(center.x + side * radius * 0.25f, center.y - radius * 0.85f)
            close()
        }
        drawPath(hornPath, color = Color(0xFF212121))
        drawPath(hornPath, color = outlineColor, style = Stroke(width = outlineWidth * 0.7f))
    }
    drawCircle(color = color, radius = radius * 0.75f, center = center)
    drawCircle(color = outlineColor, radius = radius * 0.75f, center = center, style = Stroke(width = outlineWidth))
    listOf(-1f, 1f).forEach { side ->
        drawCircle(color = Color(0xFFFFEE58), radius = radius * 0.11f, center = Offset(center.x + side * radius * 0.28f, center.y - radius * 0.05f))
    }
    drawArc(
        color = Color(0xFF1A1A1A),
        startAngle = 27f,
        sweepAngle = 126f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.35f, center.y - radius * 0.2f),
        size = Size(radius * 0.7f, radius * 0.7f),
        style = Stroke(width = outlineWidth)
    )
}

/** A blocky, cracked rock silhouette with glowing eyes — Golem. */
fun DrawScope.drawGolem(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path().apply {
        moveTo(center.x - radius * 0.9f, center.y + radius * 0.8f)
        lineTo(center.x - radius, center.y - radius * 0.4f)
        lineTo(center.x - radius * 0.4f, center.y - radius * 0.95f)
        lineTo(center.x + radius * 0.4f, center.y - radius * 0.95f)
        lineTo(center.x + radius, center.y - radius * 0.4f)
        lineTo(center.x + radius * 0.9f, center.y + radius * 0.8f)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))

    val crackColor = Color(0xFF37474F)
    drawPath(
        Path().apply {
            moveTo(center.x - radius * 0.3f, center.y - radius * 0.5f)
            lineTo(center.x - radius * 0.1f, center.y + radius * 0.1f)
            lineTo(center.x - radius * 0.35f, center.y + radius * 0.5f)
        },
        color = crackColor,
        style = Stroke(width = outlineWidth * 0.8f)
    )
    drawLine(
        color = crackColor,
        start = Offset(center.x + radius * 0.25f, center.y - radius * 0.3f),
        end = Offset(center.x + radius * 0.4f, center.y + radius * 0.3f),
        strokeWidth = outlineWidth * 0.8f
    )
    listOf(-1f, 1f).forEach { side ->
        drawCircle(color = Color(0xFFFF8F00), radius = radius * 0.12f, center = Offset(center.x + side * radius * 0.32f, center.y - radius * 0.2f))
    }
}

/** A gem-lidded chest with teeth — disguises as a pickup until it ambushes. */
fun DrawScope.drawMimic(
    center: Offset,
    radius: Float,
    bodyColor: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val lidPath = Path().apply {
        moveTo(center.x - radius * 0.85f, center.y)
        lineTo(center.x - radius * 0.6f, center.y - radius * 0.7f)
        lineTo(center.x - radius * 0.3f, center.y - radius * 0.8f)
        lineTo(center.x + radius * 0.3f, center.y - radius * 0.8f)
        lineTo(center.x + radius * 0.6f, center.y - radius * 0.7f)
        lineTo(center.x + radius * 0.85f, center.y)
        close()
    }
    drawPath(lidPath, color = Color(0xFFAB47BC))
    drawPath(lidPath, color = outlineColor, style = Stroke(width = outlineWidth))
    drawPath(
        Path().apply {
            moveTo(center.x - radius * 0.3f, center.y - radius * 0.8f)
            lineTo(center.x, center.y - radius * 0.35f)
            lineTo(center.x + radius * 0.3f, center.y - radius * 0.8f)
        },
        color = Color(0xFF8E24AA),
        style = Stroke(width = outlineWidth * 0.7f)
    )

    val jawPath = Path().apply {
        moveTo(center.x - radius * 0.85f, center.y)
        lineTo(center.x - radius * 0.65f, center.y + radius * 0.8f)
        lineTo(center.x + radius * 0.65f, center.y + radius * 0.8f)
        lineTo(center.x + radius * 0.85f, center.y)
        close()
    }
    drawPath(jawPath, color = bodyColor)
    drawPath(jawPath, color = outlineColor, style = Stroke(width = outlineWidth))

    for (i in -2..2) {
        val toothPath = Path().apply {
            moveTo(center.x + i * radius * 0.28f, center.y)
            lineTo(center.x + i * radius * 0.28f + radius * 0.12f, center.y)
            lineTo(center.x + i * radius * 0.28f + radius * 0.06f, center.y + radius * 0.22f)
            close()
        }
        drawPath(toothPath, color = Color(0xFFFFFDE7))
    }
    listOf(-1f, 1f).forEach { side ->
        drawCircle(color = Color(0xFFFFEE58), radius = radius * 0.08f, center = Offset(center.x + side * radius * 0.25f, center.y - radius * 0.35f))
    }
}

/** A jagged lightning bolt — Static Zapper. */
fun DrawScope.drawBolt(
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    val path = Path().apply {
        moveTo(center.x + radius * 0.15f, center.y - radius)
        lineTo(center.x - radius * 0.5f, center.y + radius * 0.1f)
        lineTo(center.x - radius * 0.05f, center.y + radius * 0.1f)
        lineTo(center.x - radius * 0.15f, center.y + radius)
        lineTo(center.x + radius * 0.55f, center.y - radius * 0.15f)
        lineTo(center.x + radius * 0.1f, center.y - radius * 0.15f)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
}

private val RIM_HIGHLIGHT_COLOR = Color(0xFF546E7A)
private val BONK_SPARK_COLOR = Color(0xFFFFD54F)
private val MAGNET_RED = Color(0xFFE53935)
private const val HANDLE_ANGLE = (Math.PI * 0.22).toFloat()
private val DUCK_BEAK_COLOR = Color(0xFFFF9800)
private val GRIP_COLOR = Color(0xFF6D4C41)
private val HALF_PI = (Math.PI / 2).toFloat()
