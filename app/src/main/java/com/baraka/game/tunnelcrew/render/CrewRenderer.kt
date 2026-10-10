package com.baraka.game.tunnelcrew.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.baraka.game.tunnelcrew.model.CrewTheme
import com.baraka.game.tunnelcrew.systems.CrewSim
import com.baraka.game.tunnelcrew.systems.Flag
import com.baraka.game.tunnelcrew.systems.Mode
import com.baraka.game.tunnelcrew.systems.TICKS_PER_SEC
import com.baraka.game.tunnelcrew.systems.WORLD_H
import com.baraka.game.tunnelcrew.systems.WORLD_W
import kotlin.math.sin

private val HAIR = arrayOf(Color(0xFF3FBF5A), Color(0xFF4DA3FF), Color(0xFFFF5C8A), Color(0xFFFFC93C))
private val TUNIC = Color(0xFF4A63E0)
private val TUNIC_BLOCK = Color(0xFFE5483F)
private val BOOT = Color(0xFF2B2F3A)
private val EYE = Color(0xFF1B1B1B)
private val WOOD = Color(0xFF7A5230)
private val WOOD_DARK = Color(0xFF3E2A18)
private val STONE = Color(0xFF6B7385)
private val GLOW = Color(0xFFFFE8A3)
private val FLAG = Color(0xFF69F0AE)
private val LAVA_A = Color(0xFFFF5A1F)
private val LAVA_B = Color(0xFFFFB02E)
private val SHADOW = ColorFilter.tint(Color(0x40000000))

private val DIGITS = arrayOf("0", "1", "2", "3", "4", "5", "6")
private val LEG_SWING = floatArrayOf(-1.1f, 0f, 1.1f, 0f)
private val POSE = Pose()

private val digitPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    isAntiAlias = true
    isFakeBoldText = true
    textAlign = android.graphics.Paint.Align.CENTER
    setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
}

fun skyBrush(theme: CrewTheme, fit: WorldFit): Brush =
    Brush.verticalGradient(
        listOf(Color(theme.skyTop), Color(theme.skyBottom)),
        startY = fit.oy,
        endY = fit.oy + WORLD_H * fit.scale
    )

/** Scale and letterbox offset that fit the 320x176 world into a canvas of [w] x [h] pixels. */
class WorldFit {
    var scale = 1f
    var ox = 0f
    var oy = 0f
    fun update(w: Float, h: Float) {
        scale = minOf(w / WORLD_W, h / WORLD_H)
        ox = (w - WORLD_W * scale) / 2f
        oy = (h - WORLD_H * scale) / 2f
    }
}

private fun DrawScope.circle(fit: WorldFit, x: Float, y: Float, r: Float, c: Color) =
    drawCircle(c, r * fit.scale, Offset(fit.ox + x * fit.scale, fit.oy + y * fit.scale))

private fun DrawScope.rrect(fit: WorldFit, x: Float, y: Float, w: Float, h: Float, r: Float, c: Color) =
    drawRoundRect(
        c,
        Offset(fit.ox + x * fit.scale, fit.oy + y * fit.scale),
        Size(w * fit.scale, h * fit.scale),
        CornerRadius(r * fit.scale)
    )

private fun DrawScope.oval(fit: WorldFit, x: Float, y: Float, w: Float, h: Float, c: Color) =
    drawOval(c, Offset(fit.ox + x * fit.scale, fit.oy + y * fit.scale), Size(w * fit.scale, h * fit.scale))

/** Sky decoration behind the terrain: sun, two layers of soft hills and drifting clouds, all from the level's theme. */
private fun DrawScope.drawBackdrop(theme: CrewTheme, tick: Int, fit: WorldFit) {
    circle(fit, 262f, 34f, 20f, Color(theme.sun).copy(alpha = 0.35f))
    circle(fit, 262f, 34f, 12f, Color(theme.sun))
    val far = Color(theme.farHills)
    oval(fit, -40f, 96f, 220f, 160f, far)
    oval(fit, 120f, 108f, 260f, 150f, far)
    val near = Color(theme.nearHills)
    oval(fit, -70f, 124f, 230f, 140f, near)
    oval(fit, 150f, 132f, 250f, 120f, near)
    val cloud = Color(theme.cloud).copy(alpha = 0.85f)
    for (k in 0 until 3) {
        val x = ((k * 120f + tick * 0.05f * (1f + k * 0.4f)) % (WORLD_W + 80f)) - 40f
        val y = 22f + k * 17f
        rrect(fit, x, y, 34f, 8f, 4f, cloud)
        circle(fit, x + 12f, y, 6f, cloud)
        circle(fit, x + 22f, y + 1f, 5f, cloud)
    }
}

/** Draws the whole scene for one frame; allocation-free apart from what Compose itself does. */
fun DrawScope.drawCrewScene(sim: CrewSim, image: ImageBitmap, sky: Brush, fit: WorldFit) {
    val s = fit.scale
    val theme = sim.level.theme
    drawRect(Color(theme.frame))
    clipRect(fit.ox, fit.oy, fit.ox + WORLD_W * s, fit.oy + WORLD_H * s) {
        drawRect(sky, Offset(fit.ox, fit.oy), Size(WORLD_W * s, WORLD_H * s))
        drawBackdrop(theme, sim.tick, fit)

        val dst = IntSize((WORLD_W * s).toInt(), (WORLD_H * s).toInt())
        drawImage(
            image, IntOffset.Zero, IntSize(WORLD_W, WORLD_H),
            IntOffset((fit.ox + s * 1.2f).toInt(), (fit.oy + s * 1.6f).toInt()), dst,
            colorFilter = SHADOW, filterQuality = FilterQuality.Low
        )
        drawImage(
            image, IntOffset.Zero, IntSize(WORLD_W, WORLD_H),
            IntOffset(fit.ox.toInt(), fit.oy.toInt()), dst,
            filterQuality = FilterQuality.Low
        )

        drawHazards(sim, fit)
        drawHatch(sim, fit)
        drawExit(sim, fit)

        for (i in 0 until sim.spawned) {
            if (sim.mode[i] < Mode.GONE) drawCrewMember(sim, i, fit)
        }
        drawFuseDigits(sim, fit)

        for (p in 0 until sim.plife.size) {
            val life = sim.plife[p]
            if (life <= 0) continue
            drawCircle(
                Color(sim.pcolor[p]).copy(alpha = (life / 14f).coerceAtMost(1f)),
                (0.5f + life * 0.04f) * s,
                Offset(fit.ox + sim.px[p] * s, fit.oy + sim.py[p] * s)
            )
        }
    }
}

private fun DrawScope.drawHazards(sim: CrewSim, fit: WorldFit) {
    val hz = sim.level.hazards
    val t = sim.tick * 0.18f
    for (i in hz.indices) {
        val h = hz[i]
        drawRect(
            Brush.verticalGradient(
                listOf(LAVA_B, LAVA_A),
                startY = fit.oy + h[1] * fit.scale,
                endY = fit.oy + (h[1] + h[3]) * fit.scale
            ),
            Offset(fit.ox + h[0] * fit.scale, fit.oy + h[1] * fit.scale),
            Size(h[2] * fit.scale, h[3] * fit.scale)
        )
        var xx = h[0]
        while (xx < h[0] + h[2]) {
            circle(fit, xx + 3f, h[1] + sin(t + xx * 0.5f) * 1.2f, 3.2f, LAVA_B)
            xx += 6
        }
    }
}

private fun DrawScope.drawHatch(sim: CrewSim, fit: WorldFit) {
    val hx = sim.level.spawnX.toFloat()
    val hy = (sim.level.spawnY - 16).toFloat()
    rrect(fit, hx - 11f, hy, 22f, 7f, 2f, WOOD)
    rrect(fit, hx - 7f, hy + 5f, 14f, 3f, 1.2f, WOOD_DARK)
    seg(fit, hx - 4f, hy + 0.8f, hx - 4f, hy + 5.5f, 0.5f, WOOD_DARK)
    seg(fit, hx + 4f, hy + 0.8f, hx + 4f, hy + 5.5f, 0.5f, WOOD_DARK)
}

private fun DrawScope.drawExit(sim: CrewSim, fit: WorldFit) {
    val e = sim.level.exit
    val x = e[0].toFloat()
    val y = e[1].toFloat()
    val w = e[2].toFloat()
    val h = e[3].toFloat()
    val pulse = 0.75f + 0.25f * sin(sim.tick * 0.15f)
    circle(fit, x + w / 2f, y + h / 2f, w * 0.95f, GLOW.copy(alpha = 0.22f * pulse))
    rrect(fit, x - 1.5f, y, w + 3f, h, w * 0.5f, STONE)
    rrect(fit, x + 1f, y + 2.5f, w - 2f, h - 2.5f, w * 0.4f, GLOW.copy(alpha = pulse))
    seg(fit, x + w / 2f, y - 8f, x + w / 2f, y, 0.6f, WOOD_DARK)
    val wave = sin(sim.tick * 0.25f) * 0.8f
    rrect(fit, x + w / 2f + 0.3f, y - 8f + wave, 5f, 3f, 0.8f, FLAG)
}

private fun DrawScope.drawCrewMember(sim: CrewSim, i: Int, fit: WorldFit) {
    val cx = sim.x[i] + 0.5f
    val cy = sim.y[i] + 1f
    val d = sim.dir[i]
    val m = sim.mode[i]
    val anim = sim.anim[i]
    val moving = m == Mode.WALK || m == Mode.BUILD || m == Mode.BASH || m == Mode.MINE
    val swing = if (moving) LEG_SWING[(anim / 2) and 3] else 0f
    val hair = HAIR[(if ((sim.flags[i] and Flag.CLIMBER) != 0) 1 else 0) + (if ((sim.flags[i] and Flag.FLOATER) != 0) 2 else 0)]

    seg(fit, cx - 0.6f, cy - 2.6f, cx - 0.6f + swing, cy, 1.2f, BOOT)
    seg(fit, cx + 0.6f, cy - 2.6f, cx + 0.6f - swing, cy, 1.2f, BOOT)
    rrect(fit, cx - 1.8f, cy - 5.6f, 3.6f, 3.4f, 1.3f, if (m == Mode.BLOCK) TUNIC_BLOCK else TUNIC)
    if (moving) seg(fit, cx, cy - 4.8f, cx - swing * d, cy - 3.2f, 0.8f, SKIN)
    circle(fit, cx, cy - 6.6f, 1.9f, SKIN)
    val s = fit.scale
    drawArc(
        hair, 180f, 180f, true,
        Offset(fit.ox + (cx - 2f) * s, fit.oy + (cy - 8.6f) * s), Size(4f * s, 3.2f * s)
    )
    circle(fit, cx + d * 0.8f, cy - 6.5f, 0.38f, EYE)

    val art = MODE_ARTS[m]
    if (art != null) {
        POSE.cx = cx
        POSE.cy = cy
        POSE.dir = d
        POSE.frame = (anim / 3) and 1
        POSE.swing = swing
        art(this, fit, POSE)
    }
}

private fun DrawScope.drawFuseDigits(sim: CrewSim, fit: WorldFit) {
    val nc = drawContext.canvas.nativeCanvas
    digitPaint.textSize = 8f * fit.scale
    for (i in 0 until sim.spawned) {
        val b = sim.bomb[i]
        if (b > 0 && sim.mode[i] < Mode.GONE) {
            val secs = (b + TICKS_PER_SEC - 1) / TICKS_PER_SEC
            nc.drawText(DIGITS[secs.coerceIn(0, 6)], fit.ox + (sim.x[i] + 0.5f) * fit.scale, fit.oy + (sim.y[i] - 14f) * fit.scale, digitPaint)
        }
    }
}
