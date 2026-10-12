package com.baraka.game.tunnelcrew.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baraka.game.tunnelcrew.model.Skill

/** Draws one skill icon inside a square of side [s] at the scope's origin, using [c] for everything. */
typealias IconPainter = DrawScope.(s: Float, c: Color) -> Unit

private fun DrawScope.line(s: Float, c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 0.09f) =
    drawLine(c, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = w * s, cap = StrokeCap.Round)

private val Shovel: IconPainter = { s, c ->
    line(s, c, 0.5f, 0.08f, 0.5f, 0.58f)
    line(s, c, 0.36f, 0.1f, 0.64f, 0.1f)
    val blade = Path().apply {
        moveTo(0.33f * s, 0.56f * s)
        lineTo(0.67f * s, 0.56f * s)
        quadraticBezierTo(0.7f * s, 0.8f * s, 0.5f * s, 0.95f * s)
        quadraticBezierTo(0.3f * s, 0.8f * s, 0.33f * s, 0.56f * s)
        close()
    }
    drawPath(blade, c)
}

/** The Build skill lays a staircase; its icon is the ladder. */
private val Ladder: IconPainter = { s, c ->
    line(s, c, 0.3f, 0.06f, 0.3f, 0.94f, 0.1f)
    line(s, c, 0.7f, 0.06f, 0.7f, 0.94f, 0.1f)
    for (y in floatArrayOf(0.22f, 0.42f, 0.62f, 0.82f)) line(s, c, 0.3f, y, 0.7f, y, 0.08f)
}

private val Pickaxe: IconPainter = { s, c ->
    line(s, c, 0.22f, 0.9f, 0.68f, 0.32f, 0.1f)
    val head = Path().apply {
        moveTo(0.2f * s, 0.34f * s)
        quadraticBezierTo(0.5f * s, 0.04f * s, 0.92f * s, 0.3f * s)
    }
    drawPath(head, c, style = Stroke(width = 0.11f * s, cap = StrokeCap.Round))
}

private val Hammer: IconPainter = { s, c ->
    line(s, c, 0.28f, 0.92f, 0.62f, 0.4f, 0.11f)
    rotate(-35f, Offset(0.62f * s, 0.3f * s)) {
        drawRoundRect(c, Offset(0.36f * s, 0.12f * s), Size(0.5f * s, 0.26f * s), androidx.compose.ui.geometry.CornerRadius(0.05f * s))
    }
}

private val Climb: IconPainter = { s, c ->
    line(s, c, 0.78f, 0.06f, 0.78f, 0.94f, 0.12f)
    line(s, c, 0.3f, 0.9f, 0.3f, 0.2f, 0.1f)
    line(s, c, 0.12f, 0.38f, 0.3f, 0.16f, 0.1f)
    line(s, c, 0.48f, 0.38f, 0.3f, 0.16f, 0.1f)
    line(s, c, 0.78f, 0.34f, 0.58f, 0.34f, 0.07f)
    line(s, c, 0.78f, 0.62f, 0.58f, 0.62f, 0.07f)
}

private val Umbrella: IconPainter = { s, c ->
    drawArc(c, 180f, 180f, true, Offset(0.06f * s, 0.1f * s), Size(0.88f * s, 0.7f * s))
    val hook = Path().apply {
        moveTo(0.5f * s, 0.45f * s)
        lineTo(0.5f * s, 0.84f * s)
        quadraticBezierTo(0.5f * s, 0.96f * s, 0.38f * s, 0.92f * s)
    }
    drawPath(hook, c, style = Stroke(width = 0.08f * s, cap = StrokeCap.Round))
}

private val Bomb: IconPainter = { s, c ->
    drawCircle(c, 0.34f * s, Offset(0.44f * s, 0.6f * s))
    line(s, c, 0.62f, 0.34f, 0.76f, 0.2f, 0.08f)
    for (a in floatArrayOf(0f, 60f, 120f)) {
        rotate(a, Offset(0.82f * s, 0.14f * s)) { line(s, c, 0.82f, 0.04f, 0.82f, 0.24f, 0.05f) }
    }
}

private val Stop: IconPainter = { s, c ->
    val p = Path()
    for (k in 0 until 8) {
        val a = Math.toRadians(22.5 + k * 45.0)
        val x = (0.5 + 0.46 * Math.cos(a)).toFloat() * s
        val y = (0.5 + 0.46 * Math.sin(a)).toFloat() * s
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, c)
    drawRoundRect(Color(0xFF10131A), Offset(0.24f * s, 0.43f * s), Size(0.52f * s, 0.14f * s), androidx.compose.ui.geometry.CornerRadius(0.04f * s))
}

private val Bucket: IconPainter = { s, c ->
    rotate(-25f, Offset(0.45f * s, 0.4f * s)) {
        val body = Path().apply {
            moveTo(0.2f * s, 0.18f * s)
            lineTo(0.7f * s, 0.18f * s)
            lineTo(0.62f * s, 0.62f * s)
            lineTo(0.28f * s, 0.62f * s)
            close()
        }
        drawPath(body, c)
        line(s, c, 0.18f, 0.16f, 0.72f, 0.16f, 0.08f)
    }
    drawCircle(c, 0.055f * s, Offset(0.78f * s, 0.62f * s))
    drawCircle(c, 0.055f * s, Offset(0.82f * s, 0.8f * s))
    line(s, c, 0.1f, 0.94f, 0.9f, 0.94f, 0.1f)
}

/** Deck = a flat plank bridge between two posts. */
private val Deck: IconPainter = { s, c ->
    line(s, c, 0.08f, 0.5f, 0.92f, 0.5f, 0.16f)
    line(s, c, 0.2f, 0.58f, 0.2f, 0.92f, 0.1f)
    line(s, c, 0.8f, 0.58f, 0.8f, 0.92f, 0.1f)
    line(s, c, 0.5f, 0.1f, 0.5f, 0.38f, 0.07f)
    line(s, c, 0.38f, 0.26f, 0.5f, 0.38f, 0.07f)
    line(s, c, 0.62f, 0.26f, 0.5f, 0.38f, 0.07f)
}

/** Stack = bricks climbing up in steps. */
private val StackIcon: IconPainter = { s, c ->
    for (k in 0 until 4) {
        drawRoundRect(
            c,
            Offset((0.08f + k * 0.2f) * s, (0.72f - k * 0.2f) * s),
            Size(0.3f * s, 0.18f * s),
            androidx.compose.ui.geometry.CornerRadius(0.03f * s)
        )
    }
    line(s, c, 0.1f, 0.94f, 0.9f, 0.94f, 0.08f)
}

/** One painter per skill. A new [Skill] without an entry here falls back to a plain dot instead of crashing. */
private val ICONS: Map<Skill, IconPainter> = mapOf(
    Skill.DIGGER to Shovel,
    Skill.BUILDER to Ladder,
    Skill.MINER to Pickaxe,
    Skill.BASHER to Hammer,
    Skill.CLIMBER to Climb,
    Skill.FLOATER to Umbrella,
    Skill.BOMBER to Bomb,
    Skill.BLOCKER to Stop,
    Skill.FILLER to Bucket,
    Skill.PLATFORMER to Deck,
    Skill.STACKER to StackIcon
)

private val FallbackIcon: IconPainter = { s, c -> drawCircle(c, 0.3f * s, Offset(0.5f * s, 0.5f * s)) }

@Composable
fun SkillIcon(skill: Skill, tint: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val painter = ICONS[skill] ?: FallbackIcon
    Canvas(modifier.size(size)) { painter(this, this.size.minDimension, tint) }
}
