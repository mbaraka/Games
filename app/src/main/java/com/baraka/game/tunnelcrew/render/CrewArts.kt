package com.baraka.game.tunnelcrew.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.tunnelcrew.systems.Mode

/** Where and how one crew member is drawn this frame. One instance is reused for everybody. */
class Pose {
    var cx = 0f
    var cy = 0f
    var dir = 1
    var frame = 0
    var swing = 0f
}

/** Draws the extras of one activity (tool, raised arms, umbrella...) on top of the base character. */
typealias ModeArt = DrawScope.(fit: WorldFit, p: Pose) -> Unit

internal val SKIN = Color(0xFFFFD2A8)
private val TOOL_HANDLE = Color(0xFF8A5A2B)
private val TOOL_HEAD = Color(0xFFDDE4EE)
private val UMBRELLA_RED = Color(0xFFFF5C8A)
private val UMBRELLA_STRIPE = Color(0xFFFFFFFF)
private val BRICK_COLOR = Color(0xFFE0903A)

internal fun DrawScope.seg(fit: WorldFit, x1: Float, y1: Float, x2: Float, y2: Float, w: Float, c: Color) {
    drawLine(
        c,
        Offset(fit.ox + x1 * fit.scale, fit.oy + y1 * fit.scale),
        Offset(fit.ox + x2 * fit.scale, fit.oy + y2 * fit.scale),
        strokeWidth = w * fit.scale,
        cap = StrokeCap.Round
    )
}

private val umbrella: ModeArt = { fit, p ->
    val s = fit.scale
    seg(fit, p.cx, p.cy - 10f, p.cx, p.cy - 7.5f, 0.5f, TOOL_HANDLE)
    seg(fit, p.cx, p.cy - 4.8f, p.cx + 1.6f * p.dir, p.cy - 8f, 0.9f, SKIN)
    drawArc(
        UMBRELLA_RED, 180f, 180f, true,
        Offset(fit.ox + (p.cx - 5f) * s, fit.oy + (p.cy - 14.5f) * s), Size(10f * s, 7f * s)
    )
    drawArc(
        UMBRELLA_STRIPE, 235f, 40f, true,
        Offset(fit.ox + (p.cx - 5f) * s, fit.oy + (p.cy - 14.5f) * s), Size(10f * s, 7f * s)
    )
}

private val falling: ModeArt = { fit, p ->
    seg(fit, p.cx, p.cy - 4.8f, p.cx - 2.2f, p.cy - 8.6f, 0.9f, SKIN)
    seg(fit, p.cx, p.cy - 4.8f, p.cx + 2.2f, p.cy - 8.6f, 0.9f, SKIN)
}

private val shrug: ModeArt = { fit, p ->
    seg(fit, p.cx, p.cy - 4.6f, p.cx - 2.6f, p.cy - 5.6f - p.frame, 0.9f, SKIN)
    seg(fit, p.cx, p.cy - 4.6f, p.cx + 2.6f, p.cy - 5.6f - p.frame, 0.9f, SKIN)
}

private val climbing: ModeArt = { fit, p ->
    val reach = p.cx + 2.4f * p.dir
    seg(fit, p.cx, p.cy - 4.8f, reach, p.cy - 7.2f + p.frame * 1.2f, 0.9f, SKIN)
    seg(fit, p.cx, p.cy - 4.2f, reach, p.cy - 4.2f - p.frame * 1.2f, 0.9f, SKIN)
}

private val blocking: ModeArt = { fit, p ->
    seg(fit, p.cx - 4.4f, p.cy - 4.7f, p.cx + 4.4f, p.cy - 4.7f, 1f, SKIN)
}

private val bashing: ModeArt = { fit, p ->
    val hx = p.cx + p.dir * (2.6f + p.frame)
    seg(fit, p.cx, p.cy - 4.6f, hx, p.cy - 4.2f, 0.9f, SKIN)
    seg(fit, hx, p.cy - 6.4f, hx, p.cy - 2f, 0.7f, TOOL_HANDLE)
    seg(fit, hx - 1.4f * p.dir, p.cy - 6.4f, hx + 1.4f * p.dir, p.cy - 6.4f, 1.3f, TOOL_HEAD)
}

private val mining: ModeArt = { fit, p ->
    val sw = p.frame * 1.1f
    val hx = p.cx + p.dir * 2.4f
    seg(fit, p.cx, p.cy - 4.6f, hx, p.cy - 3.6f, 0.9f, SKIN)
    seg(fit, hx - p.dir * 1.2f, p.cy - 6.6f + sw, hx + p.dir * 2.6f, p.cy - 0.6f + sw, 0.7f, TOOL_HANDLE)
    seg(fit, hx + p.dir * 0.4f, p.cy - 1.8f + sw, hx + p.dir * 3.6f, p.cy - 2.6f + sw, 1.2f, TOOL_HEAD)
}

private val digging: ModeArt = { fit, p ->
    val sw = p.frame * 1.2f
    seg(fit, p.cx, p.cy - 4.6f, p.cx + p.dir * 1.8f, p.cy - 3f, 0.9f, SKIN)
    seg(fit, p.cx + p.dir * 1.8f, p.cy - 3.6f, p.cx + p.dir * 1.8f, p.cy + 1.6f + sw, 0.7f, TOOL_HANDLE)
    seg(fit, p.cx + p.dir * 0.6f, p.cy + 2.2f + sw, p.cx + p.dir * 3f, p.cy + 2.2f + sw, 1.6f, TOOL_HEAD)
}

private val building: ModeArt = { fit, p ->
    val s = fit.scale
    seg(fit, p.cx, p.cy - 4.6f, p.cx + p.dir * 2.2f, p.cy - 4f, 0.9f, SKIN)
    drawRoundRect(
        BRICK_COLOR,
        Offset(fit.ox + (p.cx + p.dir * 2.2f - 1.4f) * s, fit.oy + (p.cy - 5.8f) * s),
        Size(2.8f * s, 1.6f * s),
        CornerRadius(0.4f * s)
    )
}

/**
 * Activity -> extra drawing. To give a new [Mode] a look, add one entry; modes without an entry just
 * show the plain walking character.
 */
internal val MODE_ARTS: Array<ModeArt?> = arrayOfNulls<ModeArt>(Mode.GONE + 1).also {
    it[Mode.FALL] = falling
    it[Mode.FLOAT] = umbrella
    it[Mode.CLIMB] = climbing
    it[Mode.BLOCK] = blocking
    it[Mode.BASH] = bashing
    it[Mode.MINE] = mining
    it[Mode.DIG] = digging
    it[Mode.BUILD] = building
    it[Mode.SHRUG] = shrug
}
