package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.knightsurvivor.model.VectorIcon

private val EYE_COLOR = Color(0xFF1A1A1A)
private val TUSK_COLOR = Color(0xFFFFFDE7)
private val CROWN_COLOR = Color(0xFFFFD54F)

/**
 * Draws a [VectorIcon.Face]: a round head plus whichever features [config] turns on.
 * Distinct feature combinations (ears vs. tusks vs. hollow sockets vs. a crown) are what
 * make a creature recognizable at a glance instead of reading as an abstract polygon a
 * player has to memorize a legend for.
 */
fun DrawScope.drawFace(
    center: Offset,
    radius: Float,
    headColor: Color,
    config: VectorIcon.Face,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    drawEars(center, radius, config.earStyle, headColor, outlineColor, outlineWidth)

    drawCircle(color = headColor, radius = radius, center = center)
    drawCircle(color = outlineColor, radius = radius, center = center, style = Stroke(width = outlineWidth))

    if (config.hasCrown) drawCrown(center, radius, outlineColor, outlineWidth)

    when (config.eyeStyle) {
        VectorIcon.EyeStyle.DOT -> drawDotEyes(center, radius)
        VectorIcon.EyeStyle.HOLLOW_SOCKET -> drawSkullFeatures(center, radius)
    }

    if (config.hasTusks) drawTusks(center, radius, outlineColor)
}

private fun DrawScope.drawEars(
    center: Offset,
    radius: Float,
    style: VectorIcon.EarStyle,
    headColor: Color,
    outlineColor: Color,
    outlineWidth: Float
) {
    when (style) {
        VectorIcon.EarStyle.NONE -> Unit

        VectorIcon.EarStyle.POINTY -> listOf(-1f, 1f).forEach { side ->
            val path = Path().apply {
                moveTo(center.x + side * radius * 0.75f, center.y - radius * 0.2f)
                lineTo(center.x + side * radius * 1.45f, center.y - radius * 0.85f)
                lineTo(center.x + side * radius * 0.55f, center.y - radius * 0.55f)
                close()
            }
            drawPath(path, color = headColor)
            drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
        }

        VectorIcon.EarStyle.ROUND -> listOf(-1f, 1f).forEach { side ->
            val earCenter = Offset(center.x + side * radius * 0.85f, center.y - radius * 0.35f)
            drawCircle(color = headColor, radius = radius * 0.32f, center = earCenter)
            drawCircle(color = outlineColor, radius = radius * 0.32f, center = earCenter, style = Stroke(width = outlineWidth))
        }
    }
}

private fun DrawScope.drawCrown(center: Offset, radius: Float, outlineColor: Color, outlineWidth: Float) {
    val baseY = center.y - radius * 0.85f
    val path = Path().apply {
        moveTo(center.x - radius * 0.7f, baseY)
        lineTo(center.x - radius * 0.7f, baseY - radius * 0.15f)
        lineTo(center.x - radius * 0.35f, baseY + radius * 0.15f)
        lineTo(center.x, baseY - radius * 0.5f)
        lineTo(center.x + radius * 0.35f, baseY + radius * 0.15f)
        lineTo(center.x + radius * 0.7f, baseY - radius * 0.15f)
        lineTo(center.x + radius * 0.7f, baseY)
        close()
    }
    drawPath(path, color = CROWN_COLOR)
    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
}

private fun DrawScope.drawDotEyes(center: Offset, radius: Float) {
    val dx = radius * 0.38f
    val dy = -radius * 0.08f
    val eyeRadius = radius * 0.15f
    drawCircle(color = EYE_COLOR, radius = eyeRadius, center = Offset(center.x - dx, center.y + dy))
    drawCircle(color = EYE_COLOR, radius = eyeRadius, center = Offset(center.x + dx, center.y + dy))
}

/** Hollow eye sockets, a nasal slit, and jaw teeth marks — reads as a skull rather than a plain face. */
private fun DrawScope.drawSkullFeatures(center: Offset, radius: Float) {
    val dx = radius * 0.38f
    val dy = -radius * 0.08f
    val socketSize = Size(radius * 0.44f, radius * 0.56f)

    listOf(-1f, 1f).forEach { side ->
        val socketCenter = Offset(center.x + side * dx, center.y + dy)
        drawOval(
            color = EYE_COLOR,
            topLeft = Offset(socketCenter.x - socketSize.width / 2f, socketCenter.y - socketSize.height / 2f),
            size = socketSize
        )
    }

    val noseTop = center.y + dy + radius * 0.25f
    val nosePath = Path().apply {
        moveTo(center.x, noseTop)
        lineTo(center.x - radius * 0.08f, noseTop + radius * 0.25f)
        lineTo(center.x + radius * 0.08f, noseTop + radius * 0.25f)
        close()
    }
    drawPath(nosePath, color = EYE_COLOR)

    for (i in -2..2) {
        val x = center.x + i * radius * 0.18f
        drawLine(
            color = EYE_COLOR,
            start = Offset(x, center.y + radius * 0.55f),
            end = Offset(x, center.y + radius * 0.85f),
            strokeWidth = 1.5f
        )
    }
}

private fun DrawScope.drawTusks(center: Offset, radius: Float, outlineColor: Color) {
    listOf(-1f, 1f).forEach { side ->
        val path = Path().apply {
            moveTo(center.x + side * radius * 0.3f, center.y + radius * 0.55f)
            lineTo(center.x + side * radius * 0.45f, center.y + radius * 0.95f)
            lineTo(center.x + side * radius * 0.18f, center.y + radius * 0.65f)
            close()
        }
        drawPath(path, color = TUSK_COLOR)
        drawPath(path, color = outlineColor, style = Stroke(width = 1f))
    }
}
