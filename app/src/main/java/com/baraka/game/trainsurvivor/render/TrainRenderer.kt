package com.baraka.game.trainsurvivor.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import com.baraka.game.trainsurvivor.model.Biome
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

/** Draws [block] once for the whole screen, or twice (old biome left of the route boundary, new biome right of it) while a route change sweeps past. */
private inline fun DrawScope.forRegions(g: TrainGame, block: DrawScope.(Biome) -> Unit) {
    if (!g.transitioning) {
        block(g.biome)
        return
    }
    clipRect(-400f, -1400f, g.boundaryX, 1400f) { block(g.fromBiome) }
    clipRect(g.boundaryX, -1400f, g.worldW + 400f, 1400f) { block(g.nextBiome) }
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
        forRegions(g) { biome -> drawBackdrop(g, biome, light, sunset) }
        drawTransitionArt(g)
        drawRails(g)
        drawTrain(g, light)
        for (z in g.zombies.sortedBy { it.y }) drawZombie(z)
        drawParticles(g, glow = false)
        forRegions(g) { biome ->
            drawForeground(g, biome, light)
            drawWeather(g, biome)
        }
        forRegions(g) { biome ->
            val darkness = if (biome == Biome.TUNNEL) 0.62f else night * 0.5f
            drawRect(Color(0xFF050A24).copy(alpha = darkness), Offset(-200f, -1200f), Size(g.worldW + 400f, 2400f))
            if (biome == Biome.TUNNEL) drawTunnelLampGlow(g)
        }
        drawGlow(g, if (g.biome == Biome.TUNNEL) 0.9f else night)
        drawParticles(g, glow = true)
        for (t in g.texts) outlinedText(t.text, t.x, t.y, 22f, t.color, (t.life / 1f).coerceIn(0f, 1f))
    }
    if (showHud) drawHud(g)
}

// ------------------------------------------------------------------ environment

private class BiomeStyle(
    val dayTop: Color, val dayBottom: Color,
    val farRock: Color, val nearRock: Color, val farAmp: Float, val nearAmp: Float, val snowCaps: Boolean,
    val groundTop: Color, val groundBottom: Color,
    val foreground: Color, val tuft: Color
)

private fun styleOf(biome: Biome): BiomeStyle = when (biome) {
    Biome.PLAINS -> BiomeStyle(DAY_TOP, DAY_BOTTOM, Color(0xFF8FA9C9), Color(0xFF6F8A9E), 190f, 120f, false, Color(0xFF9A8A58), Color(0xFF5E5236), Color(0xFF6B6558), Color(0xFF55703A))
    Biome.MOUNTAIN -> BiomeStyle(Color(0xFF5B9BD5), Color(0xFFDCEBF7), Color(0xFF7E8DA0), Color(0xFF5F6F82), 280f, 170f, true, Color(0xFFE6EEF4), Color(0xFFB0C0D2), Color(0xFFC9D6E2), Color(0xFFF2F7FB))
    Biome.DESERT -> BiomeStyle(Color(0xFF4A9AD6), Color(0xFFFAD9A0), Color(0xFFC48E5E), Color(0xFFD9A86C), 110f, 70f, false, Color(0xFFD9B26F), Color(0xFF9A7B45), Color(0xFFB38A55), Color(0xFF7E9A3A))
    Biome.GRAVEYARD -> BiomeStyle(Color(0xFF5A5470), Color(0xFFB9A7C8), Color(0xFF4A4562), Color(0xFF383450), 170f, 110f, false, Color(0xFF4C5A44), Color(0xFF2B3427), Color(0xFF3A3A44), Color(0xFF3A5A34))
    Biome.TUNNEL -> BiomeStyle(DAY_TOP, DAY_BOTTOM, Color.Black, Color.Black, 0f, 0f, false, Color(0xFF3A3530), Color(0xFF1E1B18), Color(0xFF2A2622), Color(0xFF3A3530))
}

/** Draws one region's static world (sky or tunnel wall, mountains, ground, back scenery) in the given biome's look. */
private fun DrawScope.drawBackdrop(g: TrainGame, biome: Biome, light: Float, sunset: Float) {
    val style = styleOf(biome)
    if (biome == Biome.TUNNEL) drawTunnelWall(g) else {
        drawSky(g, style, light, sunset)
        drawMountains(g, style, light)
    }
    drawGround(g, style, light)
    if (biome != Biome.TUNNEL) drawBackScenery(g, biome, light)
}

private fun DrawScope.drawSky(g: TrainGame, style: BiomeStyle, light: Float, sunset: Float) {
    val top = lerp(lerp(NIGHT_TOP, style.dayTop, light), SUNSET_TOP, sunset * 0.65f)
    val bottom = lerp(lerp(NIGHT_BOTTOM, style.dayBottom, light), SUNSET_BOTTOM, sunset * 0.8f)
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

private fun DrawScope.drawRange(g: TrainGame, base: Float, amp: Float, freq: Float, parallax: Float, brush: Brush, phase: Float) {
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
    drawPath(path, brush)
}

private fun rangeBrush(base: Float, amp: Float, rock: Color, snowCaps: Boolean, light: Float): Brush {
    if (!snowCaps) return SolidColor(rock)
    val snow = lerp(Color(0xFF3A4668), Color.White, light)
    return Brush.verticalGradient(listOf(snow, snow, rock), startY = base - amp, endY = base)
}

private fun DrawScope.drawMountains(g: TrainGame, style: BiomeStyle, light: Float) {
    val farBase = HORIZON - 8f
    val nearBase = HORIZON + 6f
    val far = lerp(Color(0xFF111B38), style.farRock, light)
    val near = lerp(Color(0xFF0B1228), style.nearRock, light)
    drawRange(g, farBase, style.farAmp, 0.0042f, 0.06f, rangeBrush(farBase, style.farAmp, far, style.snowCaps, light), 0f)
    drawRange(g, nearBase, style.nearAmp, 0.0068f, 0.14f, rangeBrush(nearBase, style.nearAmp, near, style.snowCaps, light), 2.4f)
}

private fun DrawScope.drawGround(g: TrainGame, style: BiomeStyle, light: Float) {
    val top = lerp(Color(0xFF1B2230), style.groundTop, light.coerceAtLeast(0.15f))
    val bottom = lerp(Color(0xFF0E121B), style.groundBottom, light.coerceAtLeast(0.15f))
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = HORIZON, endY = 720f), Offset(-200f, HORIZON), Size(g.worldW + 400f, 800f))
}

private fun DrawScope.drawTunnelWall(g: TrainGame) {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF14110F), Color(0xFF3A342E), Color(0xFF2A2622)), startY = -100f, endY = HORIZON + 20f),
        Offset(-200f, -1200f), Size(g.worldW + 400f, HORIZON + 1220f)
    )
    // Rock strata.
    val span = 190f
    val first = (g.scroll / span).toInt() - 1
    val count = (g.worldW / span).toInt() + 3
    for (idx in first until first + count) {
        for (row in 0 until 7) {
            val h = hash(idx * 31 + row)
            val x = idx * span + h * 120f - g.scroll
            box(Color.Black.copy(alpha = 0.16f), x, 110f + row * 52f + h * 10f, 50f + h * 110f, 3f)
        }
    }
    // Timber supports and hanging lamps.
    val gap = 300f
    val f = (g.scroll / gap).toInt() - 1
    for (i in f..f + (g.worldW / gap).toInt() + 3) {
        val x = i * gap - g.scroll
        box(Color(0xFF3B2A1C), x, 80f, 18f, 400f)
        box(Color(0xFF4A3524), x - 8f, 76f, gap * 0.5f + 10f, 14f)
        drawLine(Color(0xFF3B2A1C), Offset(x + 18f, 150f), Offset(x + 70f, 92f), 8f)
        val lx = x + gap * 0.5f + 30f
        drawLine(Color(0xFF222222), Offset(lx, 0f), Offset(lx, 118f), 2f)
        drawCircle(Color(0xFFFFE0A0), 7f, Offset(lx, 122f))
        drawCircle(Color(0xFF333333), 8f, Offset(lx, 122f), style = Stroke(2f))
    }
    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent), startY = -50f, endY = 140f), Offset(-200f, -50f), Size(g.worldW + 400f, 190f))
}

private fun DrawScope.drawTunnelLampGlow(g: TrainGame) {
    val gap = 300f
    val f = (g.scroll / gap).toInt() - 1
    for (i in f..f + (g.worldW / gap).toInt() + 3) {
        val lx = i * gap - g.scroll + gap * 0.5f + 30f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD58A).copy(alpha = 0.55f), Color.Transparent), center = Offset(lx, 122f), radius = 190f), 190f, Offset(lx, 122f))
    }
}

private fun DrawScope.drawDeadTree(color: Color, x: Float, baseY: Float, h: Float) {
    drawLine(color, Offset(x, baseY), Offset(x, baseY - h), 4f)
    drawLine(color, Offset(x, baseY - h * 0.55f), Offset(x - 18f, baseY - h * 0.85f), 3f)
    drawLine(color, Offset(x, baseY - h * 0.7f), Offset(x + 16f, baseY - h * 0.98f), 3f)
}

private fun DrawScope.drawBackScenery(g: TrainGame, biome: Biome, light: Float) {
    val spacing = if (biome == Biome.DESERT) 300f else 230f
    val parallax = 0.55f
    val first = ((g.scroll * parallax - 100f) / spacing).toInt() - 1
    val last = ((g.scroll * parallax + g.worldW + 100f) / spacing).toInt() + 1
    val trunk = lerp(Color(0xFF0B0F18), Color(0xFF4A3A2A), light)
    val leaf = lerp(Color(0xFF0B1A14), Color(0xFF3E6B3A), light)
    val snow = lerp(Color(0xFF3A4668), Color.White, light)
    val stone = lerp(Color(0xFF0C0E14), Color(0xFF8A8A94), light)
    val cactus = lerp(Color(0xFF0B1A12), Color(0xFF4F8A45), light)
    for (i in first..last) {
        val r = hash(i)
        if (r < 0.25f) continue
        val x = i * spacing + r * 120f - g.scroll * parallax
        val h = 50f + hash(i + 77) * 50f
        val baseY = RAIL_Y - 4f
        when (biome) {
            Biome.DESERT -> {
                drawLine(cactus, Offset(x, baseY), Offset(x, baseY - h), 8f, cap = StrokeCap.Round)
                drawLine(cactus, Offset(x, baseY - h * 0.45f), Offset(x - 16f, baseY - h * 0.45f), 6f, cap = StrokeCap.Round)
                drawLine(cactus, Offset(x - 16f, baseY - h * 0.45f), Offset(x - 16f, baseY - h * 0.75f), 6f, cap = StrokeCap.Round)
                drawLine(cactus, Offset(x, baseY - h * 0.6f), Offset(x + 15f, baseY - h * 0.6f), 6f, cap = StrokeCap.Round)
                drawLine(cactus, Offset(x + 15f, baseY - h * 0.6f), Offset(x + 15f, baseY - h * 0.85f), 6f, cap = StrokeCap.Round)
            }
            Biome.GRAVEYARD -> when {
                r < 0.5f -> drawDeadTree(trunk, x, baseY, h)
                r < 0.75f -> {
                    box(stone, x - 3f, baseY - 34f, 6f, 36f)
                    box(stone, x - 12f, baseY - 26f, 24f, 6f)
                }
                else -> {
                    rbox(stone, x - 11f, baseY - 30f, 22f, 32f, 10f)
                    box(Color.Black.copy(alpha = 0.25f), x - 6f, baseY - 20f, 12f, 2f)
                }
            }
            Biome.MOUNTAIN, Biome.PLAINS -> {
                if (biome == Biome.PLAINS && r < 0.6f) {
                    drawDeadTree(trunk, x, baseY, h)
                } else {
                    for (k in 0..2) {
                        path.rewind()
                        val y0 = baseY - k * h * 0.28f
                        path.moveTo(x - 20f + k * 4f, y0)
                        path.lineTo(x, y0 - h * 0.45f)
                        path.lineTo(x + 20f - k * 4f, y0)
                        path.close()
                        drawPath(path, leaf)
                        if (biome == Biome.MOUNTAIN) {
                            path.rewind()
                            path.moveTo(x - 9f + k * 2f, y0 - h * 0.2f)
                            path.lineTo(x, y0 - h * 0.45f)
                            path.lineTo(x + 9f - k * 2f, y0 - h * 0.2f)
                            path.close()
                            drawPath(path, snow)
                        }
                    }
                    drawRect(trunk, Offset(x - 2f, baseY - 6f), Size(4f, 8f))
                }
            }
            Biome.TUNNEL -> {}
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

private fun DrawScope.drawForeground(g: TrainGame, biome: Biome, light: Float) {
    if (biome == Biome.TUNNEL) return
    val style = styleOf(biome)
    val spacing = 310f
    val parallax = 1.35f
    val first = ((g.scroll * parallax - 100f) / spacing).toInt() - 1
    val last = ((g.scroll * parallax + g.worldW + 100f) / spacing).toInt() + 1
    val rock = lerp(Color(0xFF0A0D14), style.foreground, light)
    val grass = lerp(Color(0xFF0C1710), style.tuft, light)
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

/** Biome weather drawn over the action: snowfall, graveyard fog, desert dust. */
private fun DrawScope.drawWeather(g: TrainGame, biome: Biome) {
    when (biome) {
        Biome.MOUNTAIN -> {
            val span = g.worldW + 300f
            for (i in 0 until 110) {
                val x = ((hash(i) * span - g.time * (40f + hash(i + 1) * 50f) + sin(g.time + i) * 14f) % span + span) % span - 100f
                val y = (hash(i + 9) * 760f + g.time * (50f + hash(i + 3) * 60f)) % 740f - 20f
                drawCircle(Color.White.copy(alpha = 0.85f), 1.3f + hash(i + 5) * 2f, Offset(x, y))
            }
        }
        Biome.GRAVEYARD -> {
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFC9B8E0).copy(alpha = 0.26f), Color.Transparent), startY = 410f, endY = 600f),
                Offset(-200f, 410f), Size(g.worldW + 400f, 190f)
            )
            for (i in 0 until 6) {
                val span = g.worldW + 500f
                val x = ((i * 330f - g.time * (14f + i * 4f)) % span + span) % span - 250f
                drawOval(Color(0xFFD8CCE8).copy(alpha = 0.09f), Offset(x, 470f + (i % 3) * 38f), Size(340f, 50f))
            }
        }
        Biome.DESERT -> {
            val span = g.worldW + 300f
            for (i in 0 until 14) {
                val x = ((hash(i) * span - g.time * (320f + hash(i + 2) * 200f)) % span + span) % span - 100f
                val y = 520f + hash(i + 7) * 190f
                drawLine(Color(0xFFE8CFA0).copy(alpha = 0.3f), Offset(x, y), Offset(x + 34f + hash(i + 4) * 30f, y), 1.8f)
            }
        }
        else -> {}
    }
}

/** The fork where the route changes: signpost, lever and the branch not taken, plus a stone portal when a tunnel is involved. */
private fun DrawScope.drawTransitionArt(g: TrainGame) {
    if (!g.transitioning) return
    val bx = g.boundaryX
    if (g.fromBiome == Biome.TUNNEL || g.nextBiome == Biome.TUNNEL) {
        box(Color(0xFF59544C), bx - 30f, -1300f, 60f, 2000f)
        var y = -1300f
        while (y < 720f) { box(Color.Black.copy(alpha = 0.25f), bx - 30f, y, 60f, 2f); y += 26f }
        box(Color(0xFF3C3832), bx - 30f, -1300f, 8f, 2000f)
    }
    // The branch not taken peels off toward the foreground.
    drawLine(Color(0xFF8A9098), Offset(bx, RAIL_Y + 6f), Offset(bx + 320f, RAIL_Y + 78f), 4f)
    drawLine(Color(0xFF3A2E24), Offset(bx, RAIL_Y + 14f), Offset(bx + 320f, RAIL_Y + 86f), 3f)
    // Signpost with the destination board and a switch lever.
    val px = bx + 70f
    drawLine(Color(0xFF4A4F58), Offset(px, RAIL_Y - 2f), Offset(px, RAIL_Y - 120f), 5f)
    rbox(Color(0xFF3E2F1F), px - 70f, RAIL_Y - 150f, 140f, 30f, 6f)
    outlinedText(g.nextBiome.displayName, px, RAIL_Y - 129f, 15f, g.nextBiome.accent.toInt())
    drawCircle(Color(0xFF4CAF50), 7f, Offset(px, RAIL_Y - 104f))
    drawCircle(Color(0xFF4CAF50).copy(alpha = 0.3f), 15f, Offset(px, RAIL_Y - 104f))
    box(Color(0xFF30343B), px + 14f, RAIL_Y - 12f, 20f, 12f)
    drawLine(Color(0xFFD32F2F), Offset(px + 24f, RAIL_Y - 10f), Offset(px + 40f, RAIL_Y - 34f), 4f, cap = StrokeCap.Round)
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
        if (g.repairLevel > 0) drawRepairCrew(g)
        if (g.flakLevel > 0) drawFlak(g)
        if (g.teslaLevel > 0) drawTesla(g)
        for (i in 0 until g.slotCount) drawGunner(g, i)
    }
}

private fun DrawScope.drawFlak(g: TrainGame) {
    val kick = if (g.flakFlash > 0f) -3f else 0f
    box(Color(0xFF37474F), g.flakX - 11f, g.flakY - 8f, 22f, 8f)
    drawArc(Color(0xFF546E7A), 180f, 180f, true, Offset(g.flakX - 9f, g.flakY - 17f), Size(18f, 18f))
    withTransform({ translate(g.flakX, g.flakY - 12f); rotate(-58f, Offset.Zero) }) {
        box(Color(0xFF263238), kick, -4f, 28f, 3.5f)
        box(Color(0xFF263238), kick, 0.5f, 28f, 3.5f)
        box(Color(0xFF90A4AE), 24f + kick, -5f, 5f, 10f)
    }
}

private fun DrawScope.drawTesla(g: TrainGame) {
    val x = g.teslaX
    val y = g.teslaY
    box(Color(0xFF4E342E), x - 9f, y - 8f, 18f, 8f)
    val copper = Color(0xFFC77D3A)
    for (k in 0..3) rbox(copper, x - 7f + k * 0.8f, y - 14f - k * 6f, 14f - k * 1.6f, 4.5f, 2f)
    box(Color(0xFF8D6E63), x - 1.5f, y - 14f, 3f, 24f)
    val glow = g.teslaFlash > 0f
    drawCircle(if (glow) Color(0xFFB3F0FF) else Color(0xFFCFD8DC), 7f, Offset(x, y - 38f))
    drawCircle(Color(0xFF607D8B), 7f, Offset(x, y - 38f), style = Stroke(1.8f))
}

private fun DrawScope.drawRepairCrew(g: TrainGame) {
    val width = g.frontEnd - g.rearEnd - 4f
    box(Color(0xFF6D4C41), g.rearEnd, g.repairBoardY, width, 4f)
    box(Color(0xFF3E2723), g.rearEnd, g.repairBoardY + 4f, width, 2f)
    for (r in g.repairers) {
        val swing = sin(r.anim)
        val skin = Color(0xFFD9A066)
        withTransform({ translate(r.x, g.repairBoardY); scale(0.95f * r.dir, 0.95f, Offset.Zero) }) {
            val stride = if (r.hammering) 0f else swing * 3f
            box(Color(0xFF37474F), -4.5f + stride, -12f, 4f, 12f)
            box(Color(0xFF37474F), 0.5f - stride, -12f, 4f, 12f)
            box(Color(0xFF263238), -5f + stride, -2f, 6f, 2f); box(Color(0xFF263238), 0f - stride, -2f, 6f, 2f)
            rbox(Color(0xFFFF9800), -6.5f, -29f, 13f, 18f, 3f)
            box(Color(0xFFFFF59D), -6.5f, -23f, 13f, 2.5f)
            drawCircle(skin, 5.5f, Offset(1f, -34f))
            drawArc(Color(0xFFFFEB3B), 180f, 180f, true, Offset(-5.5f, -43f), Size(13f, 11f))
            box(Color(0xFFFFEB3B), -6.5f, -37.5f, 16f, 2.5f)
            val angle = if (r.hammering) -75f + sin(r.anim) * 55f else -8f + swing * 14f
            withTransform({ translate(3f, -26f); rotate(angle, Offset.Zero) }) {
                box(skin, 0f, -2f, 9f, 4f)
                box(Color(0xFF8D6E63), 8f, -1f, 10f, 2f)
                box(Color(0xFFB0BEC5), 15f, -5f, 6f, 10f)
            }
        }
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
            WeaponType.ROCKET -> {
                box(Color(0xFF455A64), 4f, -7f, 42f, 14f); box(dark, 0f, -9f, 7f, 18f)
                box(Color(0xFFB0BEC5), 44f, -8f, 6f, 16f); box(Color(0xFFE53935), 20f, -7f, 4f, 14f)
                drawCircle(Color(0xFFFF7043), 4f, Offset(52f, 0f))
            }
            WeaponType.LASER -> {
                box(Color(0xFFECEFF1), 6f, -4f, 36f, 8f); box(Color(0xFF40E0FF), 14f, -1.5f, 24f, 3f)
                box(dark, 40f, -3f, 10f, 6f); drawCircle(Color(0xFF80F0FF), 3.2f, Offset(50f, 0f)); box(dark, 10f, 3f, 5f, 8f)
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
    val dir = z.facing.toFloat()
    if (type == ZombieType.BAT) {
        drawBat(z, dir)
        return
    }
    val walk = sin(z.anim)
    val flash = z.flash * 0.75f
    fun tint(argb: Int) = lerp(Color(argb), Color.White, flash)

    val skin = tint(when (type) {
        ZombieType.WALKER -> 0xFF86B864.toInt()
        ZombieType.RUNNER -> 0xFF7DBB5E.toInt()
        ZombieType.BRUTE -> 0xFF6E8F52.toInt()
        ZombieType.BLOATER -> 0xFFB4C45C.toInt()
        ZombieType.TITAN -> 0xFF5C7A4A.toInt()
        ZombieType.BALLOON -> 0xFF86B864.toInt()
        ZombieType.CLOWN -> 0xFFE8DCC8.toInt()
        ZombieType.BAT -> 0xFF4A2F5E.toInt()
    })
    val shirt = tint(when (type) {
        ZombieType.WALKER -> intArrayOf(0xFF5D6D7E.toInt(), 0xFF6C5B3E.toInt(), 0xFF3F6C5B.toInt(), 0xFF7B4B4B.toInt())[(z.seed * 4).toInt().coerceIn(0, 3)]
        ZombieType.RUNNER -> 0xFF8D3B3B.toInt()
        ZombieType.BRUTE -> 0xFF4A3F36.toInt()
        ZombieType.BLOATER -> 0xFF8A9A3A.toInt()
        ZombieType.TITAN -> 0xFF3B2F2F.toInt()
        ZombieType.BALLOON -> 0xFFFF7043.toInt()
        ZombieType.CLOWN -> 0xFFE53935.toInt()
        ZombieType.BAT -> 0xFF4A2F5E.toInt()
    })
    val pants = tint(if (type == ZombieType.CLOWN) 0xFF1E88E5.toInt() else 0xFF3A3F47.toInt())
    val hop = if (type == ZombieType.CLOWN && !z.attacking) abs(sin(z.anim * 1.2f)) * 7f else 0f
    val hat = if (type == ZombieType.WALKER || type == ZombieType.RUNNER) ((z.seed * 997f).toInt() % 7) else 0

    // Ground shadow.
    if (!z.flying) drawOval(Color.Black.copy(alpha = 0.28f), Offset(z.x - 15f * size, z.y - 3f * size), Size(30f * size, 7f * size))

    withTransform({
        translate(z.x, z.y - hop)
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
        // Costume props: every walker and runner gets a silly accessory.
        when (hat) {
            1 -> { // traffic cone
                path.rewind(); path.moveTo(-4f, headY - 6f); path.lineTo(3f, headY - 26f); path.lineTo(9f, headY - 6f); path.close()
                drawPath(path, Color(0xFFFF6D00)); box(Color.White, -1f, headY - 15f, 8f, 3f)
            }
            2 -> { // party hat
                path.rewind(); path.moveTo(-3f, headY - 6f); path.lineTo(3f, headY - 25f); path.lineTo(8f, headY - 6f); path.close()
                drawPath(path, Color(0xFFFF4081)); drawCircle(Color(0xFFFFEB3B), 3f, Offset(3f, headY - 26f))
                box(Color(0xFF40C4FF), -1f, headY - 14f, 8f, 2.5f)
            }
            3 -> { // bucket helmet
                rbox(Color(0xFF9E9E9E), -5f, headY - 15f, 15f, 11f, 2f); box(Color(0xFF757575), -7f, headY - 6f, 19f, 3f)
            }
            4 -> { // chef hat
                drawCircle(Color.White, 5f, Offset(-1f, headY - 14f)); drawCircle(Color.White, 5.5f, Offset(4f, headY - 17f)); drawCircle(Color.White, 5f, Offset(8f, headY - 14f))
                box(Color.White, -3f, headY - 12f, 13f, 6f)
            }
            5 -> { // rubber-duck swim ring
                drawOval(Color(0xFFFFD600), Offset(-12f, -25f), Size(24f, 10f), style = Stroke(4.5f))
                drawCircle(Color(0xFFFFD600), 4.2f, Offset(11f, -29f)); box(Color(0xFFFF6D00), 14f, -30f, 5f, 2.5f)
            }
            6 -> { // cool shades
                box(Color(0xFF111111), -1f, headY - 4f, 11f, 4f); box(Color(0xFF111111), -3f, headY - 3f, 3f, 1.5f)
            }
        }
        if (type == ZombieType.WALKER && z.seed > 0.82f && hat != 6) { // googly eyes
            drawCircle(Color.White, 3.6f, Offset(5f, headY - 2f)); drawCircle(Color.White, 3.2f, Offset(-1f, headY - 2f))
            drawCircle(Color.Black, 1.5f, Offset(5f + sin(z.anim * 2f) * 1.3f, headY - 2f + cos(z.anim * 2f)))
            drawCircle(Color.Black, 1.4f, Offset(-1f + sin(z.anim * 2f) * 1.3f, headY - 2f + cos(z.anim * 2f)))
        }
        if (type == ZombieType.CLOWN) {
            val afro = intArrayOf(0xFFE53935.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt())
            for (k in afro.indices) drawCircle(tint(afro[k]), 5.6f, Offset(-7f + k * 3.3f, headY - 8f - (k % 2) * 3f))
            drawCircle(Color(0xFFE53935), 3.6f, Offset(8f, headY + 1.5f)) // red nose
            drawCircle(Color.White, 2.2f, Offset(-3f, -28f + bob)); drawCircle(Color.White, 2.2f, Offset(3f, -22f + bob)); drawCircle(Color.White, 2.2f, Offset(-2f, -17f + bob))
            drawCircle(Color.White, 4.5f, Offset(1f, -36f + bob)) // ruff collar
            drawOval(Color(0xFFE53935), Offset(-10f, -4.5f), Size(14f, 5.5f)); drawOval(Color(0xFFE53935), Offset(0f, -4.5f), Size(14f, 5.5f)) // big shoes
        }
        // Arms reaching toward the train; raised and swinging while clawing.
        val armAngle = if (type == ZombieType.BALLOON) -82f + sin(z.anim) * 4f else if (z.attacking) -55f + sin(z.anim * 2.2f) * 22f else -12f + walk * 6f
        val armW = when (type) { ZombieType.BRUTE, ZombieType.TITAN -> 8f; else -> 5f }
        val armLen = when (type) { ZombieType.BRUTE -> 20f; ZombieType.TITAN -> 24f; else -> 16f }
        withTransform({ translate(2f, -32f + bob); rotate(armAngle, Offset.Zero) }) { box(skin, 0f, -armW / 2f, armLen, armW) }
        withTransform({ translate(0f, -30f + bob); rotate(armAngle + 12f, Offset.Zero) }) { box(tint(shirtDark(type)), 0f, -armW / 2f, armLen, armW) }
    }

    if (type == ZombieType.BALLOON) {
        val party = intArrayOf(0xFFFF4081.toInt(), 0xFF40C4FF.toInt(), 0xFFFFEB3B.toInt())
        val handY = z.y - 66f * size
        for (i in -1..1) {
            val bx = z.x + i * 17f * size + sin(z.anim + i) * 3f
            val by = z.y - 118f * size - abs(i) * -9f
            drawLine(Color(0xFFEEEEEE), Offset(z.x + 2f * size, handY), Offset(bx, by + 14f), 1.2f)
            drawOval(tint(party[i + 1]), Offset(bx - 12f, by - 16f), Size(24f, 30f))
            drawOval(Color.White.copy(alpha = 0.45f), Offset(bx - 7f, by - 11f), Size(6f, 9f))
        }
    }
    if (z.hp < z.maxHp && !z.isBoss) {
        val w = 26f * size
        val y = z.y - 62f * size
        box(Color.Black.copy(alpha = 0.6f), z.x - w / 2f, y, w, 4f)
        box(Color(0xFFE53935), z.x - w / 2f, y, w * (z.hp / z.maxHp).coerceIn(0f, 1f), 4f)
    }
}

private fun DrawScope.drawBat(z: Zombie, dir: Float) {
    val flap = sin(z.anim)
    val body = lerp(Color(0xFF4A2F5E), Color.White, z.flash * 0.75f)
    val wing = lerp(Color(0xFF301E40), Color.White, z.flash * 0.75f)
    withTransform({ translate(z.x, z.y - 20f); scale(dir, 1f, Offset.Zero) }) {
        for (sgn in floatArrayOf(-1f, 1f)) {
            path.rewind()
            path.moveTo(sgn * 3f, -3f)
            path.lineTo(sgn * 30f, -16f - flap * 15f)
            path.lineTo(sgn * 24f, 0f - flap * 8f)
            path.lineTo(sgn * 17f, 4f - flap * 5f)
            path.lineTo(sgn * 9f, 7f)
            path.close()
            drawPath(path, wing)
        }
        drawOval(body, Offset(-8f, -10f), Size(16f, 20f))
        for (sgn in floatArrayOf(-1f, 1f)) {
            path.rewind(); path.moveTo(sgn * 7f, -8f); path.lineTo(sgn * 4f, -17f); path.lineTo(sgn * 1f, -9f); path.close()
            drawPath(path, body)
        }
        drawCircle(Color(0xFFFFEB3B), 2.4f, Offset(-3.5f, -3f)); drawCircle(Color(0xFFFFEB3B), 2.4f, Offset(3.5f, -3f))
        drawCircle(Color(0xFFD50000), 1f, Offset(-3.5f, -3f)); drawCircle(Color(0xFFD50000), 1f, Offset(3.5f, -3f))
        box(Color.White, -3f, 3.5f, 2f, 4f); box(Color.White, 1f, 3.5f, 2f, 4f) // fangs
        drawOval(Color(0xFFFF6E91), Offset(-2f, 5f), Size(4f, 6f)) // tongue out
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
            ParticleKind.HEAL -> {
                val a = Color(0xFF66FF8A).copy(alpha = t.coerceIn(0f, 1f))
                box(a, p.x - p.size, p.y - p.size / 3f, p.size * 2f, p.size * 2f / 3f)
                box(a, p.x - p.size / 3f, p.y - p.size, p.size * 2f / 3f, p.size * 2f)
            }
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
            val dir = z.facing.toFloat()
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
    if (g.teslaLevel > 0 && g.teslaFlash > 0f) {
        val cx = g.teslaX
        val cy = g.teslaY - 38f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFB3F0FF), Color(0xFF40C4FF).copy(alpha = 0.5f), Color.Transparent), center = Offset(cx, cy), radius = 34f), 34f, Offset(cx, cy))
    }
    if (g.flakLevel > 0 && g.flakFlash > 0f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color.Transparent), center = Offset(g.flakX + 12f, g.flakY - 28f), radius = 18f), 18f, Offset(g.flakX + 12f, g.flakY - 28f))
    }
    for (t in g.tracers) {
        val a = (t.life / 0.1f).coerceIn(0.3f, 1f)
        if (t.jagged) {
            var px = t.x1
            var py = t.y1
            val segments = 7
            val seed = (g.time * 40f).toInt()
            for (k in 1..segments) {
                val f = k / segments.toFloat()
                val last = k == segments
                val nx = t.x1 + (t.x2 - t.x1) * f + if (last) 0f else (hash(k * 13 + seed) - 0.5f) * 34f
                val ny = t.y1 + (t.y2 - t.y1) * f + if (last) 0f else (hash(k * 29 + seed) - 0.5f) * 34f
                drawLine(c(t.color).copy(alpha = a), Offset(px, py), Offset(nx, ny), t.width)
                drawLine(Color.White.copy(alpha = a), Offset(px, py), Offset(nx, ny), t.width * 0.4f)
                px = nx
                py = ny
            }
        } else {
            drawLine(c(t.color).copy(alpha = a), Offset(t.x1, t.y1), Offset(t.x2, t.y2), t.width)
        }
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

    val coinText = "%.2f".format(g.biome.coinMult).trimEnd('0').trimEnd('.')
    outlinedText(
        if (g.biome == Biome.PLAINS) g.biome.displayName else g.biome.displayName + "  x" + coinText + " coins",
        size.width - 110f * u, barY + 46f * u, 10f * u, g.biome.accent.toInt()
    )

    g.boss?.let { boss ->
        val w = 240f * u
        val x = (size.width - w) / 2f
        val y = 14f * u
        rbox(Color.Black.copy(alpha = 0.6f), x, y, w, 14f * u, 6f * u)
        rbox(Color(0xFF8E24AA), x + 2f * u, y + 2f * u, (w - 4f * u) * (boss.hp / boss.maxHp).coerceIn(0f, 1f), 10f * u, 5f * u)
        outlinedText("TITAN", size.width / 2f, y + 11f * u, 10f * u, 0xFFFFFFFF.toInt())
    }
    if (g.biomeBanner > 0f) {
        val a = g.biomeBanner.coerceIn(0f, 1f)
        outlinedText("ENTERING " + g.biome.displayName.uppercase(), size.width / 2f, size.height * 0.45f, 22f * u, g.biome.accent.toInt(), a)
        outlinedText(g.biome.tagline, size.width / 2f, size.height * 0.45f + 20f * u, 12f * u, 0xFFFFFFFF.toInt(), a)
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
