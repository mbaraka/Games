package com.baraka.game.trainsurvivor.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import com.baraka.game.trainsurvivor.model.WeaponType
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.CAR_W
import com.baraka.game.trainsurvivor.systems.COWCATCHER
import com.baraka.game.trainsurvivor.systems.LOCO_W
import com.baraka.game.trainsurvivor.systems.ParticleKind
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import com.baraka.game.trainsurvivor.systems.Zombie
import com.baraka.game.trainsurvivor.systems.weaponLength
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val HORIZON = 470f
private val DAY_TOP = Color(0xFF3C8DD9)
private val DAY_BOTTOM = Color(0xFFBFE4F5)
private val NIGHT_TOP = Color(0xFF070B1F)
private val NIGHT_BOTTOM = Color(0xFF1D2B4F)
private val SUNSET_TOP = Color(0xFF4B3470)
private val SUNSET_BOTTOM = Color(0xFFFF9E5E)
private val SHIRTS = intArrayOf(0xFFD84315.toInt(), 0xFF1E88E5.toInt(), 0xFF43A047.toInt(), 0xFF8E24AA.toInt(), 0xFFFDD835.toInt())
private val CAR_COLORS = longArrayOf(0xFF7A3E2B, 0xFF2F5D62, 0xFF5B4B8A, 0xFF4F6B3A, 0xFF8A5A2B)

private val starField: FloatArray = Random(11).let { r -> FloatArray(90 * 3) { i -> if (i % 3 == 0) r.nextFloat() else if (i % 3 == 1) r.nextFloat() * 380f else 0.8f + r.nextFloat() * 1.6f } }
private val path = Path()

private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    textAlign = Paint.Align.CENTER
}

private fun c(argb: Int) = Color(argb)
private fun smooth(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }
private fun hash(i: Int): Float { var h = i * 374761393 + 668265263; h = (h xor (h ushr 13)) * 1274126177; return ((h xor (h ushr 16)) and 0xFFFF) / 65535f }

private fun DrawScope.box(color: Color, x: Float, y: Float, w: Float, h: Float) =
    drawRect(color, Offset(x, y), Size(w, h))

private fun DrawScope.rbox(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r, r))

private fun DrawScope.outlinedText(text: String, x: Float, y: Float, size: Float, color: Int, alpha: Float = 1f) {
    val canvas = drawContext.canvas.nativeCanvas
    textPaint.textSize = size
    textPaint.style = Paint.Style.STROKE
    textPaint.strokeWidth = size * 0.18f
    textPaint.color = android.graphics.Color.argb((alpha * 200).toInt(), 0, 0, 0)
    canvas.drawText(text, x, y, textPaint)
    textPaint.style = Paint.Style.FILL
    textPaint.color = (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
    canvas.drawText(text, x, y, textPaint)
}

/**
 * Draws one frame of the whole scene. [topOffset] shifts the camera down (garage preview crops to the train);
 * the world is scaled so [TrainGame.worldW] logical units span the canvas width.
 */
fun DrawScope.drawTrainScene(g: TrainGame, topOffset: Float = 0f, showHud: Boolean = true) {
    val s = size.width / g.worldW
    val sunHeight = sin(2f * PI.toFloat() * g.dayCycle)
    val light = smooth((sunHeight + 0.25f) / 0.6f)
    val sunset = (1f - abs(sunHeight) / 0.4f).coerceIn(0f, 1f)
    val night = 1f - light
    val shakeX = (sin(g.time * 90f) * g.shake * 7f)
    val shakeY = (cos(g.time * 77f) * g.shake * 7f)

    withTransform({
        scale(s, s, Offset.Zero)
        translate(shakeX, -topOffset + shakeY)
    }) {
        drawSky(g, light, sunset)
        drawMountains(g, light)
        drawGround(g, light)
        drawBackScenery(g, light)
        drawRails(g)
        drawTrain(g, light)
        for (z in g.zombies.sortedBy { it.y }) drawZombie(z)
        drawParticles(g, glow = false)
        drawForeground(g, light)
        drawRect(Color(0xFF050A24).copy(alpha = night * 0.5f), Offset(-200f, -1200f), Size(g.worldW + 400f, 2400f))
        drawGlow(g, night)
        drawParticles(g, glow = true)
        for (t in g.texts) outlinedText(t.text, t.x, t.y, 22f, t.color, (t.life / 1f).coerceIn(0f, 1f))
    }
    if (showHud) drawHud(g)
}

// ------------------------------------------------------------------ environment

private fun DrawScope.drawSky(g: TrainGame, light: Float, sunset: Float) {
    val top = lerp(lerp(NIGHT_TOP, DAY_TOP, light), SUNSET_TOP, sunset * 0.65f)
    val bottom = lerp(lerp(NIGHT_BOTTOM, DAY_BOTTOM, light), SUNSET_BOTTOM, sunset * 0.8f)
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = HORIZON + 30f), Offset(-200f, -1200f), Size(g.worldW + 400f, HORIZON + 1230f))

    val night = 1f - light
    for (i in 0 until starField.size / 3) {
        val twinkle = 0.6f + 0.4f * sin(g.time * 2f + i)
        drawCircle(Color.White.copy(alpha = (night * night * twinkle).coerceIn(0f, 1f)), starField[i * 3 + 2], Offset(starField[i * 3] * g.worldW, starField[i * 3 + 1]))
    }

    // Sun by day, moon by night, riding the same arc.
    val cycle = g.dayCycle
    val isDay = cycle < 0.5f
    val u = if (isDay) cycle / 0.5f else (cycle - 0.5f) / 0.5f
    val cx = g.worldW * (0.08f + 0.84f * u)
    val cy = HORIZON - sin(PI.toFloat() * u) * 340f
    if (isDay) {
        val core = lerp(Color(0xFFFFF6C2), Color(0xFFFF8A3D), sunset)
        drawCircle(Brush.radialGradient(listOf(core.copy(alpha = 0.55f), Color.Transparent), center = Offset(cx, cy), radius = 130f), 130f, Offset(cx, cy))
        drawCircle(core, 34f, Offset(cx, cy))
    } else {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFBFD4FF).copy(alpha = 0.25f), Color.Transparent), center = Offset(cx, cy), radius = 100f), 100f, Offset(cx, cy))
        drawCircle(Color(0xFFE8EEF8), 28f, Offset(cx, cy))
        drawCircle(lerp(NIGHT_TOP, NIGHT_BOTTOM, 0.3f), 24f, Offset(cx + 14f, cy - 6f))
        drawCircle(Color(0xFFCBD5E8), 5f, Offset(cx - 8f, cy + 8f))
    }

    for (i in 0 until 5) {
        val span = g.worldW + 400f
        val x = ((i * 470f - g.scroll * 0.04f * (1 + i % 3)) % span + span) % span - 200f
        val y = 70f + (i * 53 % 130)
        val a = 0.15f + 0.7f * light
        val col = lerp(Color(0xFF9FB2D6), Color.White, light).copy(alpha = a)
        drawOval(col, Offset(x, y), Size(120f, 30f))
        drawOval(col, Offset(x + 30f, y - 16f), Size(80f, 34f))
        drawOval(col, Offset(x + 70f, y + 2f), Size(90f, 24f))
    }
}

private fun DrawScope.drawRange(g: TrainGame, base: Float, amp: Float, freq: Float, parallax: Float, color: Color, phase: Float) {
    path.rewind()
    path.moveTo(-20f, HORIZON + 60f)
    var x = -20f
    while (x <= g.worldW + 30f) {
        val wx = x + g.scroll * parallax
        val h = amp * (0.5f + 0.5f * (0.6f * sin(wx * freq + phase) + 0.3f * sin(wx * freq * 2.3f + 1.7f + phase) + 0.1f * sin(wx * freq * 5.1f + 0.4f)))
        path.lineTo(x, base - h)
        x += 18f
    }
    path.lineTo(g.worldW + 30f, HORIZON + 60f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawMountains(g: TrainGame, light: Float) {
    drawRange(g, HORIZON - 8f, 190f, 0.0042f, 0.06f, lerp(Color(0xFF111B38), Color(0xFF8FA9C9), light), 0f)
    drawRange(g, HORIZON + 6f, 120f, 0.0068f, 0.14f, lerp(Color(0xFF0B1228), Color(0xFF6F8A9E), light), 2.4f)
}

private fun DrawScope.drawGround(g: TrainGame, light: Float) {
    val top = lerp(Color(0xFF1B2230), Color(0xFF9A8A58), light)
    val bottom = lerp(Color(0xFF0E121B), Color(0xFF5E5236), light)
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = HORIZON, endY = 720f), Offset(-200f, HORIZON), Size(g.worldW + 400f, 800f))
}

private fun DrawScope.drawBackScenery(g: TrainGame, light: Float) {
    val spacing = 230f
    val parallax = 0.55f
    val first = ((g.scroll * parallax - 100f) / spacing).toInt() - 1
    val last = ((g.scroll * parallax + g.worldW + 100f) / spacing).toInt() + 1
    val trunk = lerp(Color(0xFF0B0F18), Color(0xFF4A3A2A), light)
    val leaf = lerp(Color(0xFF0B1A14), Color(0xFF3E6B3A), light)
    for (i in first..last) {
        val r = hash(i)
        if (r < 0.25f) continue
        val x = i * spacing + r * 120f - g.scroll * parallax
        val h = 50f + hash(i + 77) * 50f
        val baseY = RAIL_Y - 4f
        if (r < 0.6f) {
            // Dead tree.
            drawLine(trunk, Offset(x, baseY), Offset(x, baseY - h), 4f)
            drawLine(trunk, Offset(x, baseY - h * 0.55f), Offset(x - 18f, baseY - h * 0.85f), 3f)
            drawLine(trunk, Offset(x, baseY - h * 0.7f), Offset(x + 16f, baseY - h * 0.98f), 3f)
        } else {
            // Pine.
            for (k in 0..2) {
                path.rewind()
                val y0 = baseY - k * h * 0.28f
                path.moveTo(x - 20f + k * 4f, y0)
                path.lineTo(x, y0 - h * 0.45f)
                path.lineTo(x + 20f - k * 4f, y0)
                path.close()
                drawPath(path, leaf)
            }
            drawRect(trunk, Offset(x - 2f, baseY - 6f), Size(4f, 8f))
        }
    }
}

private fun DrawScope.drawRails(g: TrainGame) {
    box(Color(0xFF50505A), -200f, RAIL_Y, g.worldW + 400f, 16f)
    val spacing = 34f
    var x = -(g.scroll % spacing) - spacing
    while (x < g.worldW + spacing) {
        box(Color(0xFF3A2E24), x, RAIL_Y + 5f, 20f, 9f)
        x += spacing
    }
    box(Color(0xFFAEB4BC), -200f, RAIL_Y - 1f, g.worldW + 400f, 4f)
    box(Color(0xFF6D737C), -200f, RAIL_Y + 3f, g.worldW + 400f, 2f)
}

private fun DrawScope.drawForeground(g: TrainGame, light: Float) {
    val spacing = 310f
    val parallax = 1.35f
    val first = ((g.scroll * parallax - 100f) / spacing).toInt() - 1
    val last = ((g.scroll * parallax + g.worldW + 100f) / spacing).toInt() + 1
    val rock = lerp(Color(0xFF0A0D14), Color(0xFF6B6558), light)
    val grass = lerp(Color(0xFF0C1710), Color(0xFF55703A), light)
    for (i in first..last) {
        val r = hash(i + 500)
        val x = i * spacing + r * 200f - g.scroll * parallax
        val y = 650f + hash(i + 900) * 50f
        if (r > 0.5f) {
            drawOval(rock, Offset(x, y), Size(46f + r * 30f, 22f + r * 10f))
        } else {
            for (k in 0..4) drawLine(grass, Offset(x + k * 5f, y + 14f), Offset(x + k * 7f - 8f, y - 10f - (k % 3) * 5f), 2.5f)
        }
    }
}

// ------------------------------------------------------------------ train

private fun DrawScope.drawWheel(cx: Float, cy: Float, r: Float, angle: Float) {
    drawCircle(Color(0xFF1E1F24), r, Offset(cx, cy))
    drawCircle(Color(0xFF6A6D76), r - 2f, Offset(cx, cy), style = Stroke(2.5f))
    for (k in 0 until 4) {
        val a = angle + k * PI.toFloat() / 2f
        drawLine(Color(0xFF8A8E98), Offset(cx, cy), Offset(cx + cos(a) * (r - 3f), cy + sin(a) * (r - 3f)), 2f)
    }
    drawCircle(Color(0xFFC9A227), 3.2f, Offset(cx, cy))
}

private fun DrawScope.drawArmorPlates(x: Float, w: Float, level: Int) {
    if (level <= 0) return
    val steel = Color(0xFF626A78)
    box(steel, x + 2f, 460f, w - 4f, 24f)
    box(Color(0xFF7B8494), x + 2f, 460f, w - 4f, 3f)
    var rx = x + 8f
    while (rx < x + w - 6f) { drawCircle(Color(0xFF3B414C), 1.8f, Offset(rx, 472f)); rx += 14f }
    if (level >= 4) {
        box(steel, x + 2f, 428f, w - 4f, 14f)
        box(Color(0xFF7B8494), x + 2f, 428f, w - 4f, 3f)
    }
    if (level >= 8) box(Color(0xFFE0B93D), x + 2f, 458f, w - 4f, 2.5f)
}

private fun DrawScope.drawLoco(g: TrainGame, x0: Float) {
    box(Color(0xFF1A1A1F), x0, 480f, LOCO_W, 8f) // frame
    // Boiler.
    rbox(Color(0xFF9E2A24), x0 + 66f, 420f, 124f, 56f, 20f)
    box(Color(0xFFB8403A), x0 + 70f, 424f, 116f, 10f)
    for (bx in floatArrayOf(96f, 132f, 168f)) box(Color(0xFFD1A63A), x0 + bx, 420f, 6f, 56f)
    drawCircle(Color(0xFF2B2B31), 22f, Offset(x0 + 186f, 448f)) // smokebox
    drawCircle(Color(0xFF55575E), 17f, Offset(x0 + 186f, 448f), style = Stroke(3f))
    // Stack, dome.
    path.rewind()
    path.moveTo(x0 + 150f, 424f); path.lineTo(x0 + 156f, 388f); path.lineTo(x0 + 176f, 388f); path.lineTo(x0 + 182f, 424f); path.close()
    drawPath(path, Color(0xFF1C1C21))
    box(Color(0xFF2E2E35), x0 + 152f, 382f, 28f, 8f)
    drawArc(Color(0xFFD1A63A), 180f, 180f, true, Offset(x0 + 104f, 400f), Size(34f, 40f))
    // Cab.
    box(Color(0xFF5E2A24), x0, 396f, 68f, 84f)
    box(Color(0xFF3F1B17), x0 - 4f, 388f, 76f, 10f)
    // Cowcatcher.
    path.rewind()
    path.moveTo(x0 + LOCO_W - 4f, 470f); path.lineTo(x0 + LOCO_W + COWCATCHER, 498f); path.lineTo(x0 + LOCO_W - 4f, 498f); path.close()
    drawPath(path, Color(0xFF3D4048))
    for (k in 0..3) drawLine(Color(0xFF8A8F99), Offset(x0 + LOCO_W - 4f + k * 8f, 474f + k * 7f), Offset(x0 + LOCO_W - 4f, 498f), 2f)
    drawArmorPlates(x0, LOCO_W, g.armorLevel)
    // Wheels and connecting rod.
    for (wx in floatArrayOf(44f, 96f, 148f)) drawWheel(x0 + wx, 484f, 18f, g.wheelAngle)
    val rodY = 484f + sin(g.wheelAngle) * 6f
    drawLine(Color(0xFFC9CDD5), Offset(x0 + 44f + cos(g.wheelAngle) * 6f, rodY), Offset(x0 + 148f + cos(g.wheelAngle) * 6f, rodY), 3f)
    // Headlamp.
    drawCircle(Color(0xFFFFF3B0), 6f, Offset(x0 + LOCO_W + 2f, 438f))
    drawCircle(Color(0xFF3D4048), 7f, Offset(x0 + LOCO_W + 2f, 438f), style = Stroke(2f))
    // Spikes on the cowcatcher.
    for (k in 0 until min(g.spikeLevel, 6)) {
        val sy = 470f + k * 4.5f
        path.rewind()
        path.moveTo(x0 + LOCO_W + COWCATCHER - 8f, sy); path.lineTo(x0 + LOCO_W + COWCATCHER + 12f, sy + 3f); path.lineTo(x0 + LOCO_W + COWCATCHER - 8f, sy + 6f); path.close()
        if (k < 5) drawPath(path, Color(0xFFD7DBE2))
    }
}

private fun DrawScope.drawCar(g: TrainGame, x0: Float, index: Int) {
    box(Color(0xFF1A1A1F), x0 + 4f, 480f, CAR_W - 8f, 8f)
    val body = Color(CAR_COLORS[index % CAR_COLORS.size])
    rbox(body, x0, 424f, CAR_W, 60f, 4f)
    for (k in 1..5) box(Color.Black.copy(alpha = 0.18f), x0 + k * CAR_W / 6f, 426f, 2f, 56f)
    box(Color.White.copy(alpha = 0.12f), x0, 424f, CAR_W, 4f)
    // Roof deck with railing and sandbags.
    box(Color(0xFF2A2A30), x0 - 2f, 418f, CAR_W + 4f, 7f)
    for (k in 0..3) box(Color(0xFF8A8F99), x0 + 6f + k * 46f, 400f, 3f, 18f)
    box(Color(0xFF8A8F99), x0 + 6f, 400f, 141f, 3f)
    for (k in 0..2) rbox(Color(0xFFB59B62), x0 + 8f + k * 18f, 410f, 16f, 8f, 4f)
    drawArmorPlates(x0, CAR_W, g.armorLevel)
    for (wx in floatArrayOf(24f, 50f, 100f, 126f)) drawWheel(x0 + wx, 486f, 13f, g.wheelAngle * 1.3f)
    // Coupling.
    box(Color(0xFF111114), x0 + CAR_W - 2f, 474f, 12f, 5f)
}

private fun DrawScope.drawWindows(g: TrainGame, color: Color) {
    box(color, g.locoLeft + 12f, 404f, 22f, 22f)
    box(color, g.locoLeft + 40f, 404f, 18f, 22f)
    for (i in 1 until g.slotCount) {
        val x = g.locoLeft - 8f - i * 158f + 8f
        for (k in 0..2) box(color, x + 20f + k * 38f, 440f, 22f, 14f)
    }
}

private fun DrawScope.drawTrain(g: TrainGame, light: Float) {
    val bob = sin(g.time * 17f) * 0.8f
    withTransform({ translate(0f, if (g.over) 0f else bob) }) {
        for (i in g.slotCount - 1 downTo 1) drawCar(g, g.locoLeft - 8f - i * 158f + 8f, i - 1)
        drawLoco(g, g.locoLeft)
        drawWindows(g, lerp(Color(0xFF2A3440), Color(0xFF9BC4E2), light))
        // Rear spikes.
        for (k in 0 until min(g.spikeLevel, 6)) {
            val sy = 444f + k * 7f
            path.rewind()
            path.moveTo(g.rearEnd + 2f, sy); path.lineTo(g.rearEnd - 16f, sy + 3.5f); path.lineTo(g.rearEnd + 2f, sy + 7f); path.close()
            drawPath(path, Color(0xFFD7DBE2))
        }
        for (i in 0 until g.slotCount) drawGunner(g, i)
    }
}

// ------------------------------------------------------------------ gunners

private fun DrawScope.drawWeapon(weapon: WeaponType, recoil: Float) {
    val k = -recoil * 4f
    val dark = Color(0xFF263238)
    val steel = Color(0xFF90A4AE)
    val wood = Color(0xFF6D4C41)
    withTransform({ translate(k, 0f) }) {
        when (weapon) {
            WeaponType.PISTOL -> { box(dark, 10f, -3f, 14f, 5f); box(dark, 12f, 0f, 4f, 7f) }
            WeaponType.SMG -> { box(dark, 10f, -3f, 24f, 6f); box(dark, 18f, 2f, 4f, 9f); box(steel, 30f, -2f, 8f, 3f) }
            WeaponType.SHOTGUN -> { box(wood, 4f, -2f, 10f, 5f); box(steel, 12f, -3f, 28f, 4f); box(wood, 22f, 1f, 9f, 4f) }
            WeaponType.RIFLE -> {
                box(wood, 2f, -2f, 12f, 6f); box(dark, 12f, -3f, 34f, 5f); box(Color(0xFF111418), 20f, -9f, 13f, 5f)
                box(Color(0xFF4FC3F7), 31f, -8f, 2f, 3f); box(steel, 46f, -2f, 8f, 2f)
            }
            WeaponType.MINIGUN -> {
                box(dark, 8f, -7f, 14f, 14f); box(steel, 22f, -7f, 24f, 3f); box(steel, 22f, -1.5f, 24f, 3f); box(steel, 22f, 4f, 24f, 3f)
                box(Color(0xFF55633A), 6f, 7f, 14f, 10f)
            }
            WeaponType.CANNON -> {
                box(Color(0xFF37474F), 8f, -8f, 40f, 16f); box(dark, 46f, -10f, 7f, 20f); box(Color(0xFF8A6A3A), 22f, -8f, 4f, 16f)
                drawCircle(Color(0xFF263238), 8f, Offset(14f, 12f))
            }
        }
    }
}

private fun DrawScope.drawGunner(g: TrainGame, slot: Int) {
    val gs = g.gunners[slot]
    val gx = g.gunX(slot)
    val gy = g.gunY(slot)
    val shirt = Color(SHIRTS[slot % SHIRTS.size])
    val weapon = g.crew[slot].weapon
    val skin = Color(0xFFE0AC69)
    withTransform({ translate(gx, gy); scale(gs.facing.toFloat(), 1f, Offset.Zero) }) {
        if (weapon == WeaponType.CANNON) {
            // Cannon sits on a wheeled carriage in front of the gunner.
            box(Color(0xFF4E342E), 6f, -12f, 26f, 12f)
            drawCircle(Color(0xFF222226), 8f, Offset(18f, -6f))
        }
        box(Color(0xFF37474F), -6f, -14f, 5f, 14f)
        box(Color(0xFF37474F), 1f, -14f, 5f, 14f)
        rbox(shirt, -8f, -34f, 16f, 22f, 4f)
        box(Color(0xFF4E342E), -9f, -32f, 5f, 14f) // pack
        drawCircle(skin, 7f, Offset(1f, -42f))
        box(if (slot % 2 == 0) Color(0xFF3E2723) else Color(0xFF212121), -6f, -50f, 14f, 6f)
        drawCircle(Color(0xFF111111), 1.2f, Offset(4f, -43f))
    }
    withTransform({
        translate(gx, gy - 30f)
        scale(gs.facing.toFloat(), 1f, Offset.Zero)
        rotate(gs.aim * 180f / PI.toFloat(), Offset.Zero)
    }) {
        box(skin, 0f, -2.5f, 14f, 5f)
        drawWeapon(weapon, gs.recoil)
    }
}

// ------------------------------------------------------------------ zombies

private fun DrawScope.drawZombie(z: Zombie) {
    val type = z.type
    val size = type.size
    val dir = (-z.side).toFloat()
    val walk = sin(z.anim)
    val flash = z.flash * 0.75f
    fun tint(argb: Int) = lerp(Color(argb), Color.White, flash)

    val skin = tint(when (type) {
        ZombieType.WALKER -> 0xFF86B864.toInt()
        ZombieType.RUNNER -> 0xFF7DBB5E.toInt()
        ZombieType.BRUTE -> 0xFF6E8F52.toInt()
        ZombieType.BLOATER -> 0xFFB4C45C.toInt()
        ZombieType.TITAN -> 0xFF5C7A4A.toInt()
    })
    val shirt = tint(when (type) {
        ZombieType.WALKER -> intArrayOf(0xFF5D6D7E.toInt(), 0xFF6C5B3E.toInt(), 0xFF3F6C5B.toInt(), 0xFF7B4B4B.toInt())[(z.seed * 4).toInt().coerceIn(0, 3)]
        ZombieType.RUNNER -> 0xFF8D3B3B.toInt()
        ZombieType.BRUTE -> 0xFF4A3F36.toInt()
        ZombieType.BLOATER -> 0xFF8A9A3A.toInt()
        ZombieType.TITAN -> 0xFF3B2F2F.toInt()
    })
    val pants = tint(0xFF3A3F47.toInt())

    // Ground shadow.
    drawOval(Color.Black.copy(alpha = 0.28f), Offset(z.x - 15f * size, z.y - 3f * size), Size(30f * size, 7f * size))

    withTransform({
        translate(z.x, z.y)
        scale(size * dir, size, Offset.Zero)
        if (type == ZombieType.RUNNER) rotate(-9f, Offset.Zero)
    }) {
        val bob = if (z.attacking) sin(z.anim * 1.5f) * 2f else 0f
        // Legs.
        if (type != ZombieType.BLOATER) {
            box(pants, -5f + walk * 4f, -16f, 5f, 16f)
            box(pants, 1f - walk * 4f, -16f, 5f, 16f)
        } else {
            box(pants, -6f + walk * 3f, -12f, 5f, 12f)
            box(pants, 2f - walk * 3f, -12f, 5f, 12f)
        }
        // Spikes along the Titan's back.
        if (type == ZombieType.TITAN) {
            for (k in 0..3) {
                path.rewind()
                path.moveTo(-9f, -34f + k * 6f); path.lineTo(-18f, -40f + k * 6f); path.lineTo(-9f, -28f + k * 6f); path.close()
                drawPath(path, tint(0xFFB9B29A.toInt()))
            }
        }
        // Torso.
        when (type) {
            ZombieType.BRUTE -> rbox(shirt, -11f, -40f + bob, 22f, 28f, 6f)
            ZombieType.BLOATER -> {
                drawCircle(shirt, 17f, Offset(0f, -26f + bob))
                drawCircle(skin, 14f, Offset(0f, -26f + bob))
                drawCircle(Color(0xFF8DA032), 4f, Offset(-5f, -22f + bob)); drawCircle(Color(0xFFD6E27A), 3f, Offset(6f, -30f + bob))
            }
            ZombieType.TITAN -> {
                rbox(shirt, -11f, -40f + bob, 22f, 28f, 6f)
                box(Color(0xFFFF7043), -2f, -36f + bob, 2.5f, 18f) // glowing crack
            }
            else -> {
                rbox(shirt, -8f, -36f + bob, 16f, 22f, 4f)
                box(skin, -8f, -17f + bob, 3f, 4f); box(skin, 0f, -16f + bob, 3f, 3f) // torn hem
            }
        }
        // Head.
        val headY = when (type) { ZombieType.BRUTE -> -46f; ZombieType.BLOATER -> -48f; else -> -44f } + bob
        val headR = if (type == ZombieType.BLOATER) 6.5f else 8f
        drawCircle(skin, headR, Offset(2f, headY))
        box(tint(0xFF3E4A32.toInt()), -3f, headY - headR, 10f, 3f)
        drawCircle(Color(0xFFFFEB3B), 1.8f, Offset(6f, headY - 1.5f))
        box(Color(0xFF3A1010), 3f, headY + 3f, 6f, 2f)
        if (type == ZombieType.BRUTE) box(Color(0xFFB71C1C), -4f, headY - 5f, 12f, 3f)
        if (type == ZombieType.TITAN) {
            for (k in 0..2) {
                path.rewind()
                path.moveTo(-3f + k * 5f, headY - headR + 1f); path.lineTo(-1f + k * 5f, headY - headR - 9f); path.lineTo(1f + k * 5f, headY - headR + 1f); path.close()
                drawPath(path, tint(0xFFE0D8BE.toInt()))
            }
        }
        // Arms reaching toward the train; raised and swinging while clawing.
        val armAngle = if (z.attacking) -55f + sin(z.anim * 2.2f) * 22f else -12f + walk * 6f
        val armW = when (type) { ZombieType.BRUTE, ZombieType.TITAN -> 8f; else -> 5f }
        val armLen = when (type) { ZombieType.BRUTE -> 20f; ZombieType.TITAN -> 24f; else -> 16f }
        withTransform({ translate(2f, -32f + bob); rotate(armAngle, Offset.Zero) }) { box(skin, 0f, -armW / 2f, armLen, armW) }
        withTransform({ translate(0f, -30f + bob); rotate(armAngle + 12f, Offset.Zero) }) { box(tint(shirtDark(type)), 0f, -armW / 2f, armLen, armW) }
    }

    if (z.hp < z.maxHp && !z.isBoss) {
        val w = 26f * size
        val y = z.y - 62f * size
        box(Color.Black.copy(alpha = 0.6f), z.x - w / 2f, y, w, 4f)
        box(Color(0xFFE53935), z.x - w / 2f, y, w * (z.hp / z.maxHp).coerceIn(0f, 1f), 4f)
    }
}

private fun shirtDark(type: ZombieType): Int = if (type == ZombieType.BLOATER) 0xFF9AAB44.toInt() else 0xFF6F9A52.toInt()

// ------------------------------------------------------------------ effects

private fun DrawScope.drawParticles(g: TrainGame, glow: Boolean) {
    for (p in g.particles) {
        val t = (p.life / p.maxLife).coerceIn(0f, 1f)
        val isGlow = p.kind == ParticleKind.FIRE || p.kind == ParticleKind.SPARK
        if (isGlow != glow) continue
        when (p.kind) {
            ParticleKind.BLOOD, ParticleKind.DEBRIS -> drawCircle(c(p.color).copy(alpha = t), p.size, Offset(p.x, p.y))
            ParticleKind.SMOKE -> drawCircle(c(p.color).copy(alpha = 0.45f * t), p.size * (1f + (1f - t) * 2.4f), Offset(p.x, p.y))
            ParticleKind.COIN -> {
                val squash = abs(cos(p.life * 14f))
                drawOval(Color(0xFFFFC107).copy(alpha = t.coerceAtLeast(0.3f)), Offset(p.x - p.size * squash, p.y - p.size), Size(p.size * 2f * squash, p.size * 2f))
            }
            ParticleKind.SPARK -> drawCircle(c(p.color).copy(alpha = t), p.size, Offset(p.x, p.y))
            ParticleKind.FIRE -> {
                val r = p.size * (1f + (1f - t) * 1.6f)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3B0).copy(alpha = t), Color(0xFFFF8F00).copy(alpha = 0.8f * t), Color.Transparent), center = Offset(p.x, p.y), radius = r), r, Offset(p.x, p.y))
            }
        }
    }
}

private fun DrawScope.drawGlow(g: TrainGame, night: Float) {
    if (night > 0.05f) {
        // Headlamp cone, lit windows, glowing zombie eyes: only visible once the sky darkens.
        val lampX = g.locoLeft + LOCO_W + 2f
        path.rewind()
        path.moveTo(lampX, 436f); path.lineTo(lampX + 420f, 400f); path.lineTo(lampX + 420f, 575f); path.lineTo(lampX, 441f); path.close()
        drawPath(path, Brush.horizontalGradient(listOf(Color(0xFFFFF3B0).copy(alpha = 0.42f * night), Color.Transparent), startX = lampX, endX = lampX + 420f))
        drawCircle(Color(0xFFFFF8D0).copy(alpha = night), 7f, Offset(lampX, 438f))
        drawWindows(g, Color(0xFFFFC96B).copy(alpha = 0.9f * night))
        for (z in g.zombies) {
            val dir = -z.side.toFloat()
            val ex = z.x + dir * 6f * z.type.size
            val ey = z.y - 45f * z.type.size
            val er = if (z.isBoss) 5f else 2.4f
            drawCircle(Color(0xFFFF3B30).copy(alpha = night), er, Offset(ex, ey))
            drawCircle(Color(0xFFFF3B30).copy(alpha = 0.25f * night), er * 3f, Offset(ex, ey))
        }
    }
    for (i in 0 until g.slotCount) {
        val gs = g.gunners[i]
        if (gs.flash <= 0f) continue
        val len = weaponLength(g.crew[i].weapon)
        val mx = g.gunX(i) + gs.facing * cos(gs.aim) * len
        val my = g.shoulderY(i) + sin(gs.aim) * len
        val r = if (g.crew[i].weapon == WeaponType.CANNON) 20f else 11f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300).copy(alpha = 0.6f), Color.Transparent), center = Offset(mx, my), radius = r), r, Offset(mx, my))
    }
    for (t in g.tracers) {
        val a = (t.life / 0.1f).coerceIn(0.3f, 1f)
        drawLine(c(t.color).copy(alpha = a), Offset(t.x1, t.y1), Offset(t.x2, t.y2), t.width)
    }
}

// ------------------------------------------------------------------ HUD

private fun DrawScope.drawHud(g: TrainGame) {
    val u = size.height / 360f
    if (g.hitFlash > 0f) drawRect(Color(0xFFB71C1C).copy(alpha = g.hitFlash * 0.25f))

    // Train health.
    val barX = 14f * u
    val barY = 12f * u
    val barW = 170f * u
    val barH = 16f * u
    rbox(Color.Black.copy(alpha = 0.55f), barX, barY, barW, barH, 6f * u)
    val frac = (g.hp / g.maxHp).coerceIn(0f, 1f)
    val hpColor = lerp(Color(0xFFE53935), Color(0xFF66BB6A), frac)
    rbox(hpColor, barX + 2f * u, barY + 2f * u, (barW - 4f * u) * frac, barH - 4f * u, 5f * u)
    outlinedText("TRAIN  ${g.hp.toInt()}/${g.maxHp.toInt()}", barX + barW / 2f, barY + barH - 3.5f * u, 11f * u, 0xFFFFFFFF.toInt())

    // Coins.
    drawCircle(Color(0xFFFFC107), 8f * u, Offset(barX + 8f * u, barY + 36f * u))
    drawCircle(Color(0xFFFFE082), 4.5f * u, Offset(barX + 8f * u, barY + 36f * u), style = Stroke(1.6f * u))
    withTransformLeft(barX + 22f * u) { outlinedText("${g.runCoins}", 0f, barY + 41f * u, 15f * u, 0xFFFFD54F.toInt()) }

    // Distance / kills on the right.
    outlinedText("${g.distanceMeters} m", size.width - 110f * u, barY + 14f * u, 15f * u, 0xFFFFFFFF.toInt())
    outlinedText("${g.kills} kills", size.width - 110f * u, barY + 32f * u, 11f * u, 0xFFE0E0E0.toInt())

    g.boss?.let { boss ->
        val w = 240f * u
        val x = (size.width - w) / 2f
        val y = 14f * u
        rbox(Color.Black.copy(alpha = 0.6f), x, y, w, 14f * u, 6f * u)
        rbox(Color(0xFF8E24AA), x + 2f * u, y + 2f * u, (w - 4f * u) * (boss.hp / boss.maxHp).coerceIn(0f, 1f), 10f * u, 5f * u)
        outlinedText("TITAN", size.width / 2f, y + 11f * u, 10f * u, 0xFFFFFFFF.toInt())
    }
    if (g.bossAlert > 0f) {
        val pulse = 0.65f + 0.35f * sin(g.time * 12f)
        outlinedText("BOSS APPROACHING!", size.width / 2f, size.height * 0.3f, 26f * u, 0xFFFF5252.toInt(), pulse)
    }
}

private inline fun DrawScope.withTransformLeft(x: Float, block: DrawScope.() -> Unit) {
    withTransform({ translate(x, 0f) }) {
        // Text is center-aligned; shift by half a typical label so it sits right of the coin icon.
        textPaint.textAlign = Paint.Align.LEFT
        block()
        textPaint.textAlign = Paint.Align.CENTER
    }
}
