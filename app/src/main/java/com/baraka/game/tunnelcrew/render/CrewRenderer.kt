package com.baraka.game.tunnelcrew.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.baraka.game.tunnelcrew.systems.CrewSim
import com.baraka.game.tunnelcrew.systems.Flag
import com.baraka.game.tunnelcrew.systems.Mode
import com.baraka.game.tunnelcrew.systems.TICKS_PER_SEC
import com.baraka.game.tunnelcrew.systems.WORLD_H
import com.baraka.game.tunnelcrew.systems.WORLD_W

private val HAIR = arrayOf(Color(0xFF43A047), Color(0xFF42A5F5), Color(0xFFEC407A), Color(0xFFFFCA28))
private val SKIN = Color(0xFFFFCC99)
private val TUNIC = Color(0xFF3F51B5)
private val TUNIC_BLOCK = Color(0xFFE53935)
private val BOOT = Color(0xFF263238)
private val EYE = Color(0xFF111111)
private val TOOL = Color(0xFFCFD8DC)
private val BRICK = Color(0xFFD9822B)
private val UMBRELLA = Color(0xFFEC407A)
private val HATCH_WOOD = Color(0xFF6D4C2F)
private val HATCH_DARK = Color(0xFF3E2A18)
private val DOOR = Color(0xFF3E2A18)
private val DOOR_GLOW = Color(0xFFFFE082)
private val FLAG = Color(0xFF69F0AE)
private val LAVA_A = Color(0xFFFF6D00)
private val LAVA_B = Color(0xFFFFAB00)

private val DIGITS = arrayOf("0", "1", "2", "3", "4", "5", "6")

private val digitPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    isAntiAlias = false
    isFakeBoldText = true
    textAlign = android.graphics.Paint.Align.CENTER
    setShadowLayer(2f, 0f, 0f, android.graphics.Color.BLACK)
}

fun skyBrush(top: Long, bottom: Long, height: Float): Brush =
    Brush.verticalGradient(listOf(Color(top), Color(bottom)), 0f, height)

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

private fun DrawScope.wr(fit: WorldFit, x: Int, y: Int, w: Int, h: Int, c: Color) {
    drawRect(c, Offset(fit.ox + x * fit.scale, fit.oy + y * fit.scale), Size(w * fit.scale, h * fit.scale))
}

/** Draws the whole scene for one frame; allocation-free apart from what Compose itself does. */
fun DrawScope.drawCrewScene(sim: CrewSim, image: ImageBitmap, sky: Brush, fit: WorldFit) {
    val s = fit.scale
    drawRect(Color.Black)
    drawRect(
        sky,
        Offset(fit.ox, fit.oy),
        Size(WORLD_W * s, WORLD_H * s)
    )
    drawImage(
        image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(WORLD_W, WORLD_H),
        dstOffset = IntOffset(fit.ox.toInt(), fit.oy.toInt()),
        dstSize = IntSize((WORLD_W * s).toInt(), (WORLD_H * s).toInt()),
        filterQuality = FilterQuality.None
    )

    val level = sim.level
    val phase = sim.tick / 6

    // Hazards (lava / water pits).
    for (i in level.hazards.indices) {
        val h = level.hazards[i]
        wr(fit, h[0], h[1], h[2], h[3], LAVA_A)
        var xx = h[0]
        while (xx < h[0] + h[2]) {
            if (((xx / 4) + phase) % 2 == 0) wr(fit, xx, h[1], 4, 2, LAVA_B)
            xx += 4
        }
    }

    // Entrance hatch.
    val hx = level.spawnX
    val hy = level.spawnY - 14
    wr(fit, hx - 10, hy, 20, 5, HATCH_WOOD)
    wr(fit, hx - 10, hy + 5, 20, 1, HATCH_DARK)
    wr(fit, hx - 6, hy + 5, 12, 2, HATCH_DARK)

    // Exit door.
    val e = level.exit
    wr(fit, e[0], e[1] + 2, e[2], e[3] - 2, DOOR)
    wr(fit, e[0] + 2, e[1], e[2] - 4, 3, DOOR)
    wr(fit, e[0] + 3, e[1] + 5, e[2] - 6, e[3] - 5, DOOR_GLOW)
    val flagUp = if ((sim.tick / 8) % 2 == 0) 0 else 1
    wr(fit, e[0] + e[2] / 2, e[1] - 6, 1, 7, DOOR)
    wr(fit, e[0] + e[2] / 2 + 1, e[1] - 6 + flagUp, 4, 3, FLAG)

    // Crew.
    for (i in 0 until sim.spawned) {
        val m = sim.mode[i]
        if (m >= Mode.GONE) continue
        drawCrewMember(sim, i, fit)
    }

    // Fuse digits.
    val nc = drawContext.canvas.nativeCanvas
    digitPaint.textSize = 6f * s
    for (i in 0 until sim.spawned) {
        val b = sim.bomb[i]
        if (b > 0 && sim.mode[i] < Mode.GONE) {
            val secs = (b + TICKS_PER_SEC - 1) / TICKS_PER_SEC
            nc.drawText(DIGITS[secs.coerceIn(0, 6)], fit.ox + sim.x[i] * s, fit.oy + (sim.y[i] - 13) * s, digitPaint)
        }
    }

    // Particles.
    for (p in 0 until sim.plife.size) {
        if (sim.plife[p] <= 0) continue
        drawRect(
            Color(sim.pcolor[p]),
            Offset(fit.ox + sim.px[p] * s, fit.oy + sim.py[p] * s),
            Size(1.5f * s, 1.5f * s)
        )
    }
}

private fun DrawScope.drawCrewMember(sim: CrewSim, i: Int, fit: WorldFit) {
    val cx = sim.x[i]
    val cy = sim.y[i]
    val d = sim.dir[i]
    val m = sim.mode[i]
    val frame = (sim.anim[i] / 3) and 1
    val hairIdx = (if ((sim.flags[i] and Flag.CLIMBER) != 0) 1 else 0) + (if ((sim.flags[i] and Flag.FLOATER) != 0) 2 else 0)
    val tunic = if (m == Mode.BLOCK) TUNIC_BLOCK else TUNIC

    wr(fit, cx - 2, cy - 7, 4, 2, HAIR[hairIdx])
    wr(fit, cx - 2, cy - 5, 4, 2, SKIN)
    wr(fit, if (d > 0) cx + 1 else cx - 2, cy - 5, 1, 1, EYE)
    wr(fit, cx - 2, cy - 3, 4, 2, tunic)
    val walking = m == Mode.WALK || m == Mode.BUILD || m == Mode.BASH || m == Mode.MINE
    if (walking && frame == 1) {
        wr(fit, cx - 2, cy - 1, 1, 1, BOOT)
        wr(fit, cx + 1, cy - 1, 1, 2, BOOT)
    } else {
        wr(fit, cx - 2, cy - 1, 1, 2, BOOT)
        wr(fit, cx + 1, cy - 1, 1, 1, BOOT)
    }

    when (m) {
        Mode.FLOAT -> {
            wr(fit, cx - 4, cy - 11, 8, 2, UMBRELLA)
            wr(fit, cx - 3, cy - 12, 6, 1, UMBRELLA)
            wr(fit, cx, cy - 9, 1, 2, TOOL)
        }
        Mode.BLOCK -> wr(fit, cx - 4, cy - 4, 8, 1, SKIN)
        Mode.CLIMB -> wr(fit, if (d > 0) cx + 2 else cx - 3, cy - 7 + frame, 1, 3, SKIN)
        Mode.BASH -> wr(fit, if (d > 0) cx + 2 else cx - 4, cy - 4 + frame, 3, 1, TOOL)
        Mode.MINE -> wr(fit, if (d > 0) cx + 2 else cx - 4, cy - 2 + frame, 3, 1, TOOL)
        Mode.DIG -> wr(fit, cx - 2 + frame, cy + 1, 3, 1, TOOL)
        Mode.BUILD -> wr(fit, if (d > 0) cx + 2 else cx - 3, cy - 3, 2, 1, BRICK)
        Mode.FALL -> wr(fit, cx - 4, cy - 6, 8, 1, SKIN)
    }
}
