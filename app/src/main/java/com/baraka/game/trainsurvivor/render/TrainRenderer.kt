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
import com.baraka.game.trainsurvivor.model.AvatarBody
import com.baraka.game.trainsurvivor.model.AvatarExtra
import com.baraka.game.trainsurvivor.model.AvatarLook
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.CraftedWeapon
import com.baraka.game.trainsurvivor.model.DamageKind
import com.baraka.game.trainsurvivor.model.HatStyle
import com.baraka.game.trainsurvivor.model.Livery
import com.baraka.game.trainsurvivor.model.SURVIVORS
import com.baraka.game.trainsurvivor.model.TrainShape
import com.baraka.game.trainsurvivor.model.DangerLevel
import com.baraka.game.trainsurvivor.model.WeaponType
import com.baraka.game.trainsurvivor.model.ShotStyle
import com.baraka.game.trainsurvivor.model.TrailLook
import com.baraka.game.trainsurvivor.model.TrailShapeKind
import com.baraka.game.trainsurvivor.model.WeaponEffect
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.CAR_W
import com.baraka.game.trainsurvivor.systems.COWCATCHER
import com.baraka.game.trainsurvivor.systems.LOCO_W
import com.baraka.game.trainsurvivor.systems.ParticleKind
import com.baraka.game.trainsurvivor.systems.LANE_DY
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.Tracer
import com.baraka.game.trainsurvivor.systems.TrainGame
import com.baraka.game.trainsurvivor.systems.Zombie
import com.baraka.game.trainsurvivor.systems.weaponLength
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

internal const val HORIZON = 470f
private val DAY_TOP = Color(0xFF3C8DD9)
private val DAY_BOTTOM = Color(0xFFBFE4F5)
private val NIGHT_TOP = Color(0xFF070B1F)
private val NIGHT_BOTTOM = Color(0xFF1D2B4F)
private val SUNSET_TOP = Color(0xFF4B3470)
private val SUNSET_BOTTOM = Color(0xFFFF9E5E)
private val SHIRTS = intArrayOf(0xFFD84315.toInt(), 0xFF1E88E5.toInt(), 0xFF43A047.toInt(), 0xFF8E24AA.toInt(), 0xFFFDD835.toInt())
private val CAR_COLORS = longArrayOf(0xFF7A3E2B, 0xFF2F5D62, 0xFF5B4B8A, 0xFF4F6B3A, 0xFF8A5A2B)

internal val starField: FloatArray = Random(11).let { r -> FloatArray(90 * 3) { i -> if (i % 3 == 0) r.nextFloat() else if (i % 3 == 1) r.nextFloat() * 380f else 0.8f + r.nextFloat() * 1.6f } }
internal val path = Path()

/** Rebuilds a HUD string only when its key changes, so the HUD allocates nothing on most frames. */
private class TextCache {
    var key = Int.MIN_VALUE
    var text = ""
    inline fun get(newKey: Int, build: () -> String): String {
        if (newKey != key) {
            key = newKey
            text = build()
        }
        return text
    }
}

private val hpText = TextCache()
private val coinText = TextCache()
private val distText = TextCache()
private val killText = TextCache()
private val biomeText = TextCache()
private val bossText = TextCache()
private val speedText = TextCache()
private val dangerText = TextCache()

private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    textAlign = Paint.Align.CENTER
}

private fun c(argb: Int) = Color(argb)
internal fun smooth(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }
internal fun hash(i: Int): Float { var h = i * 374761393 + 668265263; h = (h xor (h ushr 13)) * 1274126177; return ((h xor (h ushr 16)) and 0xFFFF) / 65535f }

internal fun DrawScope.box(color: Color, x: Float, y: Float, w: Float, h: Float) =
    drawRect(color, Offset(x, y), Size(w, h))

internal fun DrawScope.rbox(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r, r))

internal fun DrawScope.outlinedText(text: String, x: Float, y: Float, size: Float, color: Int, alpha: Float = 1f) {
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
 * In a shared world [ally] is the second train: its lane is drawn [LANE_DY] above [g]'s.
 */
fun DrawScope.drawTrainScene(g: TrainGame, topOffset: Float = 0f, showHud: Boolean = true, ally: TrainGame? = null, hud: TrainGame = g) {
    val s = size.width / g.worldW
    val sunHeight = sin(2f * PI.toFloat() * g.dayCycle)
    val light = smooth((sunHeight + 0.25f) / 0.6f)
    val sunset = (1f - abs(sunHeight) / 0.4f).coerceIn(0f, 1f)
    val night = 1f - light
    palette = g.livery
    val shakeAmount = if (g.shakeEnabled) g.shake else 0f
    val shakeX = sin(g.time * 90f) * shakeAmount * 7f
    val shakeY = cos(g.time * 77f) * shakeAmount * 7f

    // In a shared world the train on track 1 is drawn above the one on track 0, whichever of the two it is (tracks can swap).
    val lower = if (ally != null && g.lane == 1) ally else g
    val upper = if (ally == null) null else if (g.lane == 1) g else ally
    withTransform({
        scale(s, s, Offset.Zero)
        translate(shakeX, -topOffset + shakeY)
    }) {
        forRegions(g) { biome -> RouteArtRegistry.artFor(biome).backdrop(this, g, light, sunset) }
        drawTransitionArt(g)
        if (upper != null) {
            withTransform({ translate(0f, -LANE_DY) }) {
                drawViaduct(upper)
                drawLaneBody(upper, light, 1, front = false)
            }
        }
        drawSpeedLines(g)
        if (ally == null) drawGhostTrain(g)
        drawLaneBody(lower, light, if (upper != null) 0 else -1, front = true)
        forRegions(g) { biome ->
            val art = RouteArtRegistry.artFor(biome)
            art.foreground(this, g, light)
            art.weather(this, g)
        }
        forRegions(g) { biome ->
            val art = RouteArtRegistry.artFor(biome)
            drawRect(Color(0xFF050A24).copy(alpha = art.darkness(night)), Offset(-200f, -1200f), Size(g.worldW + 400f, 2400f))
            art.afterDarkness(this, g)
        }
        if (upper != null) {
            withTransform({ translate(0f, -LANE_DY) }) {
                drawGlow(upper, RouteArtRegistry.artFor(upper.biome).glowNight(night), 1)
                drawParticles(upper, glow = true, laneFilter = 1)
                drawLaneTexts(upper, 1)
            }
        }
        val frontLane = if (upper != null) 0 else -1
        drawGlow(lower, RouteArtRegistry.artFor(lower.biome).glowNight(night), frontLane)
        drawParticles(lower, glow = true, laneFilter = frontLane)
        drawLaneTexts(lower, frontLane)
    }
    if (showHud) drawHud(hud)
}

/** Everything that lives in one lane: rails, train, its zombies, shells, hazards and effects. [laneFilter] is -1 when there is only one lane. */
private fun DrawScope.drawLaneBody(rig: TrainGame, light: Float, laneFilter: Int, front: Boolean) {
    palette = rig.livery // each train wears its own paint
    drawRails(rig)
    drawTrain(rig, light)
    drawPuddles(rig)
    val order = rig.drawOrder
    for (i in order.indices) {
        val z = order[i]
        if (laneFilter >= 0 && z.lane != laneFilter) continue
        drawZombie(z)
        if (z.elite != null) drawEliteMarks(z, rig.time)
    }
    if (front) {
        drawCrate(rig)
        drawHitcher(rig)
    }
    drawShells(rig)
    drawHazards(rig)
    if (rig.rescueTime > 0f) drawStranded(rig)
    drawParticles(rig, glow = false, laneFilter = laneFilter)
}

private fun DrawScope.drawLaneTexts(rig: TrainGame, laneFilter: Int) {
    for (i in rig.texts.indices) {
        val t = rig.texts[i]
        if (laneFilter >= 0 && t.lane != laneFilter) continue
        outlinedText(t.text, t.x, t.y, t.size, t.color, t.life.coerceIn(0f, 1f))
    }
}

/** The raised deck the second lane runs on: a gravel bed beside the rails (where its zombies walk) and pillars down to the ground. */
private fun DrawScope.drawViaduct(rig: TrainGame) {
    val top = RAIL_Y + 14f
    box(Color(0xFF3C4148), -200f, top, rig.worldW + 400f, 66f)
    box(Color(0xFF5A6068), -200f, top, rig.worldW + 400f, 5f)
    box(Color(0xFF2B2F35), -200f, top + 66f, rig.worldW + 400f, 10f)
    val spacing = 210f
    var x = -(rig.scroll % spacing) - spacing
    while (x < rig.worldW + spacing) {
        box(Color(0xFF353A41), x, top + 76f, 26f, HORIZON + LANE_DY - (top + 76f) + 40f)
        box(Color(0xFF454B53), x - 5f, top + 76f, 36f, 8f)
        x += spacing
    }
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
    Biome.SWAMP -> BiomeStyle(Color(0xFF5C7F6B), Color(0xFFB8D4B0), Color(0xFF4D6B5A), Color(0xFF3C5648), 90f, 55f, false, Color(0xFF4B5B3A), Color(0xFF2A3322), Color(0xFF2F4A38), Color(0xFF5F8F3F))
    else -> styleOf(Biome.PLAINS)
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

internal fun DrawScope.drawRange(g: TrainGame, base: Float, amp: Float, freq: Float, parallax: Float, brush: Brush, phase: Float) {
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

internal fun rangeBrush(base: Float, amp: Float, rock: Color, snowCaps: Boolean, light: Float): Brush {
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
            Biome.SWAMP -> {
                if (r < 0.55f) {
                    // Mangrove: trunk, arching roots, a canopy with hanging moss.
                    drawLine(trunk, Offset(x, baseY), Offset(x, baseY - h * 0.9f), 5f)
                    drawLine(trunk, Offset(x, baseY - h * 0.3f), Offset(x - 16f, baseY + 2f), 3f)
                    drawLine(trunk, Offset(x, baseY - h * 0.3f), Offset(x + 16f, baseY + 2f), 3f)
                    drawOval(leaf, Offset(x - 26f, baseY - h * 1.15f), Size(52f, 30f))
                    for (k in -1..1) drawLine(leaf.copy(alpha = 0.8f), Offset(x + k * 16f, baseY - h * 1.0f), Offset(x + k * 16f, baseY - h * 0.6f), 2f)
                } else {
                    // Reeds with cattails.
                    for (k in -2..2) {
                        val rx = x + k * 6f
                        val rh = 28f + ((k + 2) % 3) * 9f
                        drawLine(cactus, Offset(rx, baseY), Offset(rx + k * 1.5f, baseY - rh), 2.5f)
                        drawOval(Color(0xFF5D4037), Offset(rx + k * 1.5f - 2.5f, baseY - rh - 7f), Size(5f, 12f))
                    }
                }
            }
            Biome.TUNNEL -> {}
            else -> {}
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
            if (biome == Biome.SWAMP) {
                val w = 90f + r * 60f
                drawOval(lerp(Color(0xFF0C1A18), Color(0xFF2E5E5A), light), Offset(x, y), Size(w, 26f))
                drawOval(Color.White.copy(alpha = 0.12f * light), Offset(x + w * 0.2f, y + 5f), Size(w * 0.4f, 6f))
                drawLine(grass, Offset(x + w * 0.8f, y + 14f), Offset(x + w * 0.8f - 6f, y - 8f), 2.5f)
            } else {
                drawOval(rock, Offset(x, y), Size(46f + r * 30f, 22f + r * 10f))
            }
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
        Biome.SWAMP -> {
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFB7D3C0).copy(alpha = 0.22f), Color.Transparent), startY = 430f, endY = 610f),
                Offset(-200f, 430f), Size(g.worldW + 400f, 180f)
            )
            for (i in 0 until 9) {
                val span = g.worldW + 400f
                val x = ((i * 260f - g.time * (10f + i * 3f)) % span + span) % span - 200f
                drawOval(Color(0xFFCFE3D3).copy(alpha = 0.08f), Offset(x, 480f + (i % 3) * 36f), Size(300f, 46f))
            }
            for (i in 0 until 14) { // fireflies
                val fx = hash(i + 40) * g.worldW + sin(g.time * 0.7f + i) * 30f
                val fy = 400f + hash(i + 60) * 200f + cos(g.time * 0.9f + i * 2f) * 14f
                val blink = 0.4f + 0.6f * sin(g.time * 3f + i * 1.7f).coerceAtLeast(0f)
                drawCircle(Color(0xFFE6FF70).copy(alpha = 0.7f * blink), 2.4f, Offset(fx, fy))
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

internal fun DrawScope.drawWheel(cx: Float, cy: Float, r: Float, angle: Float) {
    drawCircle(Color(0xFF1E1F24), r, Offset(cx, cy))
    drawCircle(Color(0xFF6A6D76), r - 2f, Offset(cx, cy), style = Stroke(2.5f))
    for (k in 0 until 4) {
        val a = angle + k * PI.toFloat() / 2f
        drawLine(Color(0xFF8A8E98), Offset(cx, cy), Offset(cx + cos(a) * (r - 3f), cy + sin(a) * (r - 3f)), 2f)
    }
    drawCircle(Color(0xFFC9A227), 3.2f, Offset(cx, cy))
}

internal fun DrawScope.drawArmorPlates(x: Float, w: Float, level: Int) {
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
    rbox(Color(palette.boiler), x0 + 66f, 420f, 124f, 56f, 20f)
    box(Color(palette.boilerHi), x0 + 70f, 424f, 116f, 10f)
    for (bx in floatArrayOf(96f, 132f, 168f)) box(Color(palette.trim), x0 + bx, 420f, 6f, 56f)
    drawCircle(Color(0xFF2B2B31), 22f, Offset(x0 + 186f, 448f)) // smokebox
    drawCircle(Color(0xFF55575E), 17f, Offset(x0 + 186f, 448f), style = Stroke(3f))
    // Stack, dome.
    path.rewind()
    path.moveTo(x0 + 150f, 424f); path.lineTo(x0 + 156f, 388f); path.lineTo(x0 + 176f, 388f); path.lineTo(x0 + 182f, 424f); path.close()
    drawPath(path, Color(0xFF1C1C21))
    box(Color(0xFF2E2E35), x0 + 152f, 382f, 28f, 8f)
    drawArc(Color(palette.trim), 180f, 180f, true, Offset(x0 + 104f, 400f), Size(34f, 40f))
    // Cab.
    box(Color(palette.cab), x0, 396f, 68f, 84f)
    box(Color(palette.cabDark), x0 - 4f, 388f, 76f, 10f)
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

internal fun DrawScope.drawCar(g: TrainGame, x0: Float, index: Int, slot: Int, corner: Float = 4f) {
    val alive = g.carAlive(slot)
    box(Color(0xFF1A1A1F), x0 + 4f, 480f, CAR_W - 8f, 8f)
    val body = if (alive) Color(palette.cars[index % palette.cars.size]) else Color(0xFF2B2522)
    rbox(body, x0, 424f, CAR_W, 60f, corner)
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
    if (!alive) {
        // Burnt-out wreck: flickering flames on the roof.
        for (k in 0..2) {
            val fx = x0 + 30f + k * 45f
            val h = 24f + sin(g.time * 9f + k * 2f) * 9f
            path.rewind(); path.moveTo(fx - 10f, 424f); path.lineTo(fx, 424f - h); path.lineTo(fx + 10f, 424f); path.close()
            drawPath(path, Color(0xFFFF7043))
            path.rewind(); path.moveTo(fx - 5f, 424f); path.lineTo(fx, 424f - h * 0.55f); path.lineTo(fx + 5f, 424f); path.close()
            drawPath(path, Color(0xFFFFEB3B))
        }
    } else if (g.carHp[slot] < g.carMaxHp[slot] - 0.5f) {
        val frac = (g.carHp[slot] / g.carMaxHp[slot]).coerceIn(0f, 1f)
        box(Color.Black.copy(alpha = 0.6f), x0 + 20f, 432f, 110f, 6f)
        box(lerp(Color(0xFFE53935), Color(0xFF66BB6A), frac), x0 + 20f, 432f, 110f * frac, 6f)
    }
}

private fun DrawScope.drawWindows(g: TrainGame, color: Color) {
    box(color, g.locoLeft + 12f, 404f, 22f, 22f)
    box(color, g.locoLeft + 40f, 404f, 18f, 22f)
    for (i in 1 until g.slotCount) {
        if (!g.carAlive(i)) continue
        val x = g.carLeft(i)
        for (k in 0..2) box(color, x + 20f + k * 38f, 440f, 22f, 14f)
    }
}

private fun DrawScope.drawTrain(g: TrainGame, light: Float) {
    val bob = sin(g.time * 17f) * 0.8f
    withTransform({ translate(0f, if (g.over) 0f else bob) }) {
        val shapeArt = TrainShapeArtRegistry.artFor(g.shape)
        for (i in g.slotCount - 1 downTo 1) shapeArt.paintCar(this, g, g.carLeft(i), i - 1, i)
        shapeArt.paintLoco(this, g, g.locoLeft)
        shapeArt.paintWindows(this, g, lerp(Color(0xFF2A3440), Color(0xFF9BC4E2), light))
        if (palette.glow) drawGlowTrim(g)
        // Route gear: the old route's kit fades out while the new route's fades in.
        if (g.kitBlend < 1f) RouteArtRegistry.artFor(g.kitFrom).trainKit(this, g, 1f - g.kitBlend)
        RouteArtRegistry.artFor(g.biome).trainKit(this, g, g.kitBlend)
        // Rear spikes.
        for (k in 0 until min(g.spikeLevel, 6)) {
            val sy = 444f + k * 7f
            path.rewind()
            path.moveTo(g.rearEnd + 2f, sy); path.lineTo(g.rearEnd - 16f, sy + 3.5f); path.lineTo(g.rearEnd + 2f, sy + 7f); path.close()
            drawPath(path, Color(0xFFD7DBE2))
        }
        g.trail?.let { drawCoinTrail(g, it) }
        if (g.repairLevel > 0) drawRepairCrew(g)
        if (g.flakLevel > 0) drawFlak(g)
        if (g.teslaLevel > 0) drawTesla(g)
        if (g.mortarLevel > 0) drawMortar(g)
        g.workshop?.let { drawWorkshop(g, it) }
        if (g.flameLevel > 0) drawFlamer(g)
        if (g.guestPresent) drawGuestMount(g)
        for (i in 0..g.slotCount) drawGunner(g, i)
        drawPassenger(g)
        drawAwayMarkers(g)
    }
}

private fun DrawScope.drawMortar(g: TrainGame) {
    val x = g.mortarX
    val y = g.mortarY
    box(Color(0xFF37474F), x - 11f, y - 6f, 22f, 6f)
    withTransform({ translate(x, y - 6f); rotate(-64f, Offset.Zero) }) {
        box(Color(0xFF455A64), 0f, -6f, 28f, 12f)
        box(Color(0xFF263238), 24f, -8f, 6f, 16f)
        box(Color(0xFF8A6A3A), 8f, -6f, 3f, 12f)
    }
}

private fun DrawScope.drawFlamer(g: TrainGame) {
    rbox(Color(0xFFB71C1C), g.flameX - 30f, g.flameY - 12f, 20f, 15f, 4f)
    box(Color(0xFF37474F), g.flameX - 10f, g.flameY - 6f, 22f, 8f)
    box(Color(0xFF90A4AE), g.flameX + 10f, g.flameY - 8f, 5f, 12f)
}

private fun DrawScope.drawCrate(g: TrainGame) {
    val c = g.crate ?: return
    val fade = if (c.landed) (c.life / 1.5f).coerceIn(0f, 1f) else 1f
    if (!c.landed) {
        drawArc(Color(0xFFE53935).copy(alpha = fade), 180f, 180f, true, Offset(c.x - 36f, c.y - 78f), Size(72f, 56f))
        drawArc(Color.White.copy(alpha = fade), 220f, 50f, true, Offset(c.x - 36f, c.y - 78f), Size(72f, 56f))
        drawLine(Color.White.copy(alpha = fade), Offset(c.x - 34f, c.y - 50f), Offset(c.x - 16f, c.y - 16f), 1.5f)
        drawLine(Color.White.copy(alpha = fade), Offset(c.x + 34f, c.y - 50f), Offset(c.x + 16f, c.y - 16f), 1.5f)
    } else {
        val pulse = 0.5f + 0.5f * sin(c.phase * 8f)
        drawCircle(Color(0xFFFFD54F).copy(alpha = 0.25f * pulse * fade), 46f, Offset(c.x, c.y))
        outlinedText("TAP!", c.x, c.y - 30f, 20f, 0xFFFFD54F.toInt(), fade)
    }
    rbox(Color(0xFF8D6E63).copy(alpha = fade), c.x - 18f, c.y - 18f, 36f, 36f, 4f)
    box(Color(0xFF5D4037).copy(alpha = fade), c.x - 18f, c.y - 3f, 36f, 6f)
    box(Color(0xFF5D4037).copy(alpha = fade), c.x - 3f, c.y - 18f, 6f, 36f)
    drawCircle(Color(0xFFFFC107).copy(alpha = fade), 5f, Offset(c.x, c.y))
}

private fun DrawScope.drawShells(g: TrainGame) {
    for (i in g.shells.indices) {
        val s = g.shells[i]
        drawLine(Color(0xFF90A4AE).copy(alpha = 0.6f), Offset(s.x - s.vx * 0.04f, s.y - s.vy * 0.04f), Offset(s.x, s.y), 3f)
        drawCircle(Color(0xFF263238), 6f, Offset(s.x, s.y))
        drawCircle(Color(0xFFFF7043), 2.5f, Offset(s.x, s.y))
    }
}

private fun DrawScope.drawFlame(g: TrainGame) {
    if (g.flameLevel <= 0 || g.flameFlash <= 0f) return
    val nx = g.flameX + 14f
    val ny = g.flameY - 2f
    val reach = g.flameReach * (0.85f + 0.15f * sin(g.time * 40f))
    path.rewind()
    path.moveTo(nx, ny - 3f); path.lineTo(nx + reach, ny - 30f + sin(g.time * 33f) * 6f); path.lineTo(nx + reach * 0.92f, ny + 12f); path.lineTo(nx, ny + 4f); path.close()
    drawPath(path, Color(0xFFFF6D00).copy(alpha = 0.8f))
    path.rewind()
    path.moveTo(nx, ny - 2f); path.lineTo(nx + reach * 0.7f, ny - 14f + sin(g.time * 47f) * 4f); path.lineTo(nx + reach * 0.65f, ny + 6f); path.lineTo(nx, ny + 3f); path.close()
    drawPath(path, Color(0xFFFFEB3B).copy(alpha = 0.9f))
}

private fun DrawScope.drawOverdrive(g: TrainGame) {
    if (g.overdriveTime <= 0f) return
    val pulse = 0.55f + 0.45f * sin(g.time * 14f)
    for (i in 0..g.slotCount) {
        if (!g.gunners[i].alive) continue
        drawCircle(Color(0xFFFFAB00).copy(alpha = 0.55f * pulse), 30f, Offset(g.gunX(i), g.gunY(i) - 26f), style = Stroke(3f))
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

/** The Workshop's crafted turret: a generic rig whose colour comes from its element and whose details come
 * from its effect, so any procedurally-crafted weapon (model/Workshop.kt) always has something to draw without
 * needing hand-authored art. */
private fun DrawScope.drawWorkshop(g: TrainGame, w: CraftedWeapon) {
    val x = g.workshopX
    val y = g.workshopY
    val tint = when (w.kind) {
        DamageKind.FIRE -> Color(0xFFFF7043)
        DamageKind.ELECTRIC -> Color(0xFF40E0FF)
        DamageKind.EARTH -> Color(0xFF8D6E63)
        DamageKind.PHYSICAL -> Color(0xFFCFD8DC)
    }
    box(Color(0xFF37474F), x - 10f, y - 8f, 20f, 8f) // mount
    box(Color(0xFF263238), x - 4f, y - 20f, 8f, 14f) // barrel base
    box(tint, x - 3f, y - 26f, 6f, 8f) // barrel tip, coloured by element
    if (w.effect == WeaponEffect.VOLATILE) drawCircle(tint.copy(alpha = 0.6f), 6f, Offset(x, y - 22f)) // bulging charge
    if (w.effect == WeaponEffect.PRECISE) { box(Color(0xFF263238), x + 4f, y - 24f, 7f, 3f); drawCircle(tint, 1.6f, Offset(x + 10f, y - 22.5f)) } // scope
    if (w.effect == WeaponEffect.OVERCHARGED) { box(tint.copy(alpha = 0.7f), x - 8f, y - 18f, 2.4f, 10f); box(tint.copy(alpha = 0.7f), x + 5.6f, y - 18f, 2.4f, 10f) } // vents
    if (g.workshopFlash > 0f) {
        drawCircle(Color.White.copy(alpha = 0.8f), 5f, Offset(x, y - 26f))
        drawCircle(tint.copy(alpha = 0.5f), 10f, Offset(x, y - 26f))
    }
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
            WeaponType.ANUBIS_CURSE -> {
                box(Color(0xFF1A1A1A), 4f, -3f, 38f, 5f) // dark staff shaft
                box(Color(0xFFD4AF37), 38f, -9f, 7f, 17f) // gold head
                drawCircle(Color(0xFF7C4DFF).copy(alpha = 0.6f), 6f, Offset(44f, -1f)) // curse glow
                drawCircle(Color(0xFF1565C0), 2.6f, Offset(44f, -1f)) // lapis eye
            }
            WeaponType.CONDUCTORS_REVOLVER -> {
                box(wood, 2f, -2f, 8f, 6f); box(Color(0xFF5D4037), 8f, -3f, 6f, 8f) // cylinder
                box(dark, 14f, -2f, 16f, 5f); box(Color(0xFFFFCC80), 8f, -4f, 3f, 2f) // brass lantern bead on the barrel
            }
            WeaponType.TWIN_KUNAI -> {
                box(dark, 4f, -4f, 6f, 3f) // grip wrap
                path.rewind(); path.moveTo(10f, -4f); path.lineTo(30f, -3f); path.lineTo(10f, -2f); path.close()
                drawPath(path, steel) // upper blade
                box(dark, 4f, 1f, 6f, 3f)
                path.rewind(); path.moveTo(10f, 1f); path.lineTo(30f, 2f); path.lineTo(10f, 3f); path.close()
                drawPath(path, steel) // lower blade
            }
            WeaponType.TRAUMA_BEAM -> {
                box(Color(0xFFECEFF1), 6f, -3f, 30f, 6f); box(Color(0xFFE53935), 12f, -1f, 20f, 2.4f)
                box(dark, 34f, -3f, 8f, 6f); drawCircle(Color(0xFFFF8A80), 2.6f, Offset(42f, 0f))
            }
            WeaponType.SCRAP_CANNON -> {
                box(Color(0xFF6D4C41), 6f, -9f, 36f, 18f); box(Color(0xFFFF8F00), 10f, -7f, 6f, 6f) // welded patch
                box(dark, 40f, -10f, 8f, 20f); drawCircle(Color(0xFF263238), 7f, Offset(16f, 14f))
            }
        }
    }
}

private fun DrawScope.drawGunner(g: TrainGame, slot: Int) {
    val gs = g.gunners[slot]
    if (!gs.alive) return
    val gx = g.gunX(slot)
    val gy = g.gunY(slot)
    val look = SURVIVORS[g.survivorOf(slot) % SURVIVORS.size].look
    val shirt = Color(look.shirt)
    val weapon = gs.weapon
    val skin = Color(look.skin)
    val heat = gs.heat[gs.weapon.ordinal]
    if (heat > 0.03f) {
        box(Color.Black.copy(alpha = 0.6f), gx - 16f, gy - 58f, 32f, 5f)
        box(if (gs.isHot) Color(0xFFFF1744) else lerp(Color(0xFFFFEB3B), Color(0xFFFF6D00), heat), gx - 16f, gy - 58f, 32f * heat.coerceIn(0f, 1f), 5f)
    }
    withTransform({ translate(gx, gy); scale(gs.facing * look.width, look.height, Offset.Zero) }) {
        if (weapon == WeaponType.CANNON) {
            // Cannon sits on a wheeled carriage in front of the gunner.
            box(Color(0xFF4E342E), 6f, -12f, 26f, 12f)
            drawCircle(Color(0xFF222226), 8f, Offset(18f, -6f))
        }
        drawAvatarBody(look)
    }
    withTransform({
        translate(gx, gy - 30f * look.height)
        scale(gs.facing.toFloat(), 1f, Offset.Zero)
        rotate(gs.aim * 180f / PI.toFloat(), Offset.Zero)
    }) {
        box(if (look.body == AvatarBody.ROBOT) Color(0xFF90A4AE) else skin, 0f, -2.5f, 14f, 5f)
        drawWeapon(weapon, gs.recoil)
    }
}

// ------------------------------------------------------------------ zombies

private fun DrawScope.drawZombie(z: Zombie) {
    val type = z.type
    val size = type.size
    val isClown = type in CLOWN_TYPES
    val dir = z.facing.toFloat()
    val customArt = MobArtRegistry[type]
    if (customArt != null) {
        customArt.draw(this, z, dir)
        drawElementBadge(z)
        return
    }
    val walk = sin(z.anim)
    val flash = z.flash * 0.75f
    fun tint(argb: Int) = lerp(Color(argb), Color.White, flash)

    val skin = tint(MobLooks.skin(type))
    val shirt = tint(MobLooks.shirt(type, z.seed))
    val pants = tint(if (isClown) 0xFF1E88E5.toInt() else 0xFF3A3F47.toInt())
    val hop = if (isClown && !z.attacking) abs(sin(z.anim * 1.2f)) * 7f else 0f
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
            in BULKY -> rbox(shirt, -11f, -40f + bob, 22f, 28f, 6f)
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
        HumanoidExtras.draw(this, z, headY, bob, skin, shirt)
        if (type == ZombieType.YETI || type == ZombieType.YETI_KING) {
            for (k in 0..3) { // shaggy fur tufts and icy eyes
                path.rewind(); path.moveTo(-8f + k * 5f, headY - 6f); path.lineTo(-6f + k * 5f, headY - 15f); path.lineTo(-3f + k * 5f, headY - 6f); path.close()
                drawPath(path, skin)
            }
            for (k in 0..2) drawLine(skin, Offset(-8f, -34f + k * 7f + bob), Offset(-14f, -30f + k * 7f + bob), 3f)
            drawCircle(Color(0xFF4FC3F7), 2f, Offset(6f, headY - 1.5f))
        }
        if (type == ZombieType.SKELETON) {
            box(Color(0xFF111111), -8f, -35f + bob, 16f, 21f) // dark gap between the ribs
            for (k in 0..3) drawLine(skin, Offset(-7f, -32f + k * 5f + bob), Offset(7f, -32f + k * 5f + bob), 2.2f)
            drawLine(skin, Offset(0f, -36f + bob), Offset(0f, -14f + bob), 2.2f)
            drawCircle(Color(0xFF1B1B1B), 2.6f, Offset(6f, headY - 1.5f)) // eye socket
            drawCircle(Color(0xFFFF5252), 1f, Offset(6f, headY - 1.5f))
            box(Color(0xFF1B1B1B), 3f, headY + 3f, 6f, 1.6f)
        }
        if (type == ZombieType.CLOWN_PRINCE) {
            for (k in 0..3) { // crown
                path.rewind(); path.moveTo(-6f + k * 4.6f, headY - 12f); path.lineTo(-4f + k * 4.6f, headY - 22f); path.lineTo(-2f + k * 4.6f, headY - 12f); path.close()
                drawPath(path, Color(0xFFFFC107))
            }
            path.rewind(); path.moveTo(1f, -34f + bob); path.lineTo(-9f, -40f + bob); path.lineTo(-9f, -28f + bob); path.close(); drawPath(path, Color(0xFFFF4081))
            path.rewind(); path.moveTo(1f, -34f + bob); path.lineTo(11f, -40f + bob); path.lineTo(11f, -28f + bob); path.close(); drawPath(path, Color(0xFFFF4081))
        }
        if (isClown) {
            val afro = intArrayOf(0xFFE53935.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt())
            for (k in afro.indices) drawCircle(tint(afro[k]), 5.6f, Offset(-7f + k * 3.3f, headY - 8f - (k % 2) * 3f))
            drawCircle(Color(0xFFE53935), 3.6f, Offset(8f, headY + 1.5f)) // red nose
            drawCircle(Color.White, 2.2f, Offset(-3f, -28f + bob)); drawCircle(Color.White, 2.2f, Offset(3f, -22f + bob)); drawCircle(Color.White, 2.2f, Offset(-2f, -17f + bob))
            drawCircle(Color.White, 4.5f, Offset(1f, -36f + bob)) // ruff collar
            drawOval(Color(0xFFE53935), Offset(-10f, -4.5f), Size(14f, 5.5f)); drawOval(Color(0xFFE53935), Offset(0f, -4.5f), Size(14f, 5.5f)) // big shoes
        }
        // Arms reaching toward the train; raised and swinging while clawing.
        val armAngle = if (type == ZombieType.BALLOON) -82f + sin(z.anim) * 4f else if (z.attacking) -55f + sin(z.anim * 2.2f) * 22f else -12f + walk * 6f
        val armW = if (type in BULKY || type == ZombieType.TITAN) 8f else 5f
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
        val y = z.y - (z.type.aimHeight * 2f + 6f) * size
        box(Color.Black.copy(alpha = 0.6f), z.x - w / 2f, y, w, 4f)
        box(Color(0xFFE53935), z.x - w / 2f, y, w * (z.hp / z.maxHp).coerceIn(0f, 1f), 4f)
    }
}

/** Elites get a pulsing ring in their trait colour, a diamond marker, a shield bubble and an always-visible health bar. */
private fun DrawScope.drawEliteMarks(z: Zombie, time: Float) {
    val trait = z.elite ?: return
    val size = z.type.size
    val cx = z.x
    val cy = z.y - z.type.aimHeight * size * 1.1f
    val radius = (24f + z.type.aimHeight * 0.6f) * size
    val pulse = 0.55f + 0.35f * sin(time * 6f + z.seed * 6f)
    drawCircle(Color(trait.color).copy(alpha = 0.55f * pulse), radius, Offset(cx, cy), style = Stroke(2.5f))
    val second = z.elite2
    if (second != null) drawCircle(Color(second.color).copy(alpha = 0.5f * pulse), radius + 6f, Offset(cx, cy), style = Stroke(2f))
    if (z.shield > 0f) {
        val fill = (z.shield / (z.maxHp * 0.6f)).coerceIn(0f, 1f)
        drawCircle(Color(0xFF40C4FF).copy(alpha = 0.14f + 0.18f * fill), radius, Offset(cx, cy))
    }
    val my = z.y - (z.type.aimHeight * 2f + 16f) * size
    path.rewind(); path.moveTo(cx, my - 6f); path.lineTo(cx + 5f, my); path.lineTo(cx, my + 6f); path.lineTo(cx - 5f, my); path.close()
    drawPath(path, Color(trait.color))
    drawHpBar(z)
}

internal fun DrawScope.drawHpBar(z: Zombie) {
    val size = z.type.size
    val w = 26f * size
    val y = z.y - (z.type.aimHeight * 2f + 6f) * size
    box(Color.Black.copy(alpha = 0.6f), z.x - w / 2f, y, w, 4f)
    box(Color(0xFFE53935), z.x - w / 2f, y, w * (z.hp / z.maxHp).coerceIn(0f, 1f), 4f)
}

/** The four-legged and crawling signature mobs: hound, rat, scorpion and crocodile. */
private fun DrawScope.drawCreature(z: Zombie, dir: Float) {
    val type = z.type
    val size = type.size
    val flash = z.flash * 0.75f
    fun tint(argb: Int) = lerp(Color(argb), Color.White, flash)
    val scuttle = sin(z.anim * 1.6f)
    drawOval(Color.Black.copy(alpha = 0.28f), Offset(z.x - 24f * size, z.y - 3f * size), Size(48f * size, 7f * size))
    withTransform({ translate(z.x, z.y); scale(size * dir, size, Offset.Zero) }) {
        when (CREATURE_KIND[type] ?: type) {
            ZombieType.ZOMBIE_DOG, ZombieType.ICE_WOLF -> {
                val icy = type == ZombieType.ICE_WOLF
                val fur = tint(if (icy) 0xFFDCEBF5.toInt() else 0xFF7B6F57.toInt())
                val dark = tint(if (icy) 0xFF8FA9BB.toInt() else 0xFF4E4538.toInt())
                for (k in 0..3) box(dark, -14f + k * 9f + sin(z.anim * 1.6f + k * 1.7f) * 4f, -12f, 4f, 12f)
                drawOval(fur, Offset(-20f, -27f), Size(40f, 18f))
                for (k in 0..2) drawLine(Color(0xFFE6DFC8).copy(alpha = 0.8f), Offset(-8f + k * 7f, -23f), Offset(-8f + k * 7f, -15f), 1.5f)
                drawLine(dark, Offset(-20f, -22f), Offset(-30f, -31f + scuttle * 3f), 3f)
                drawOval(fur, Offset(14f, -35f), Size(18f, 14f))
                path.rewind(); path.moveTo(18f, -34f); path.lineTo(21f, -43f); path.lineTo(25f, -34f); path.close()
                drawPath(path, dark)
                box(dark, 28f, -31f, 9f, 6f)
                drawCircle(Color(0xFFFF3B30), 1.8f, Offset(25f, -30f))
                if (z.attacking) drawOval(Color(0xFFFF6E91), Offset(33f, -26f), Size(4f, 7f))
            }
            ZombieType.RAT -> {
                val fur = tint(0xFF6D6258.toInt())
                val dark = tint(0xFF4A423B.toInt())
                for (k in 0..2) box(dark, -8f + k * 7f + sin(z.anim * 2f + k) * 2.5f, -6f, 3f, 6f)
                drawOval(fur, Offset(-13f, -15f), Size(26f, 11f))
                path.rewind(); path.moveTo(11f, -14f); path.lineTo(23f, -9f); path.lineTo(11f, -5f); path.close()
                drawPath(path, fur)
                drawCircle(Color(0xFFE0A0A0), 3f, Offset(8f, -16f))
                drawLine(Color(0xFFD7A0A0), Offset(-13f, -9f), Offset(-26f, -14f + scuttle * 2f), 1.8f)
                drawCircle(Color(0xFFFF3B30), 1.3f, Offset(15f, -11f))
            }
            ZombieType.SCARAB -> {
                val shell = tint(0xFF1B2E1C.toInt())
                val sheen = tint(0xFF4CAF50.toInt())
                for (k in 0..2) box(shell, -8f + k * 7f + sin(z.anim * 2.4f + k) * 2.5f, -6f, 3f, 6f)
                drawOval(shell, Offset(-12f, -16f), Size(24f, 12f))
                drawOval(sheen.copy(alpha = 0.5f), Offset(-9f, -15f), Size(10f, 9f))
                drawLine(shell, Offset(0f, -16f), Offset(0f, -5f), 1.6f) // wing-case seam
                drawCircle(Color(0xFFFF3B30), 1.3f, Offset(9f, -12f))
            }
            ZombieType.SCORPION -> {
                val shell = tint(0xFFB8803F.toInt())
                val dark = tint(0xFF7A5226.toInt())
                for (k in 0..2) {
                    val lx = -14f + k * 10f
                    val sw = sin(z.anim * 1.8f + k * 2f) * 4f
                    drawLine(dark, Offset(lx, -12f), Offset(lx + sw - 5f, 0f), 2f)
                    drawLine(dark, Offset(lx + 4f, -12f), Offset(lx + 4f + sw + 5f, 0f), 2f)
                }
                drawOval(shell, Offset(-20f, -24f), Size(40f, 14f))
                drawOval(dark, Offset(-8f, -23f), Size(10f, 8f))
                // claws
                drawLine(dark, Offset(18f, -19f), Offset(30f, -26f + scuttle * 3f), 3f)
                drawLine(dark, Offset(18f, -16f), Offset(30f, -12f - scuttle * 3f), 3f)
                path.rewind(); path.moveTo(29f, -29f); path.lineTo(37f, -25f + scuttle * 3f); path.lineTo(29f, -22f); path.close(); drawPath(path, shell)
                path.rewind(); path.moveTo(29f, -15f); path.lineTo(37f, -12f - scuttle * 3f); path.lineTo(29f, -8f); path.close(); drawPath(path, shell)
                // curled tail with stinger
                val tail = floatArrayOf(-18f, -20f, -27f, -28f, -27f, -40f, -17f, -48f, -4f, -48f)
                for (k in 0 until 4) drawLine(shell, Offset(tail[k * 2], tail[k * 2 + 1]), Offset(tail[k * 2 + 2], tail[k * 2 + 3]), 5f)
                path.rewind(); path.moveTo(-4f, -53f); path.lineTo(5f, -44f); path.lineTo(-4f, -43f); path.close(); drawPath(path, Color(0xFFC62828))
                drawCircle(Color(0xFFFF3B30), 1.6f, Offset(12f, -21f))
            }
            ZombieType.CROCODILE -> {
                val skin = tint(0xFF5E7B3A.toInt())
                val belly = tint(0xFF9BB36A.toInt())
                val dark = tint(0xFF3E5326.toInt())
                for (k in 0..3) box(dark, -30f + k * 17f + sin(z.anim * 1.4f + k * 1.6f) * 3.5f, -10f, 6f, 10f)
                rbox(skin, -42f, -25f, 84f, 17f, 8f)
                box(belly, -38f, -13f, 76f, 4f)
                path.rewind(); path.moveTo(-40f, -23f); path.lineTo(-82f, -12f + scuttle * 2f); path.lineTo(-40f, -10f); path.close(); drawPath(path, skin)
                for (k in 0..8) {
                    path.rewind(); path.moveTo(-36f + k * 9f, -25f); path.lineTo(-32f + k * 9f, -32f); path.lineTo(-28f + k * 9f, -25f); path.close()
                    drawPath(path, dark)
                }
                // jaws: they snap open while lunging and clawing
                val jaw = if (z.attacking) 10f + abs(sin(z.anim * 3f)) * 16f else if (z.retarget > 0f) 28f else 5f
                rbox(skin, 36f, -27f, 36f, 9f, 4f)
                withTransform({ translate(38f, -18f); rotate(jaw, Offset.Zero) }) {
                    rbox(skin, 0f, 0f, 34f, 6f, 3f)
                    for (k in 0..3) {
                        path.rewind(); path.moveTo(4f + k * 8f, 0f); path.lineTo(7.5f + k * 8f, -5f); path.lineTo(11f + k * 8f, 0f); path.close()
                        drawPath(path, Color.White)
                    }
                }
                for (k in 0..4) {
                    path.rewind(); path.moveTo(40f + k * 7f, -18f); path.lineTo(43.5f + k * 7f, -13f); path.lineTo(47f + k * 7f, -18f); path.close()
                    drawPath(path, Color.White)
                }
                drawCircle(Color(0xFFFFEB3B), 3.6f, Offset(41f, -28f))
                drawCircle(Color.Black, 1.4f, Offset(42f, -28f))
            }
            else -> {}
        }
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

private fun DrawScope.drawBat(z: Zombie, dir: Float) {
    val flap = sin(z.anim)
    val big = z.type == ZombieType.GARGOYLE
    val bigScale = if (big) 2.6f else 1f
    val body = lerp(if (big) Color(0xFF7A7F8C) else Color(0xFF4A2F5E), Color.White, z.flash * 0.75f)
    val wing = lerp(if (big) Color(0xFF4B4F5A) else Color(0xFF301E40), Color.White, z.flash * 0.75f)
    withTransform({ translate(z.x, z.y - 20f * bigScale); scale(dir * bigScale, bigScale, Offset.Zero) }) {
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
        if (big) {
            for (k in 0..2) { // crown of horns
                path.rewind(); path.moveTo(-6f + k * 6f, -10f); path.lineTo(-4f + k * 6f, -20f); path.lineTo(-2f + k * 6f, -10f); path.close()
                drawPath(path, Color(0xFFFFC107))
            }
        }
    }
}

private fun shirtDark(type: ZombieType): Int = if (type == ZombieType.BLOATER) 0xFF9AAB44.toInt() else 0xFF6F9A52.toInt()

// ------------------------------------------------------------------ effects

private fun DrawScope.drawParticles(g: TrainGame, glow: Boolean, laneFilter: Int = -1) {
    for (pi in g.particles.indices) {
        val p = g.particles[pi]
        if (laneFilter >= 0 && p.lane != laneFilter) continue
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

private fun DrawScope.drawGlow(g: TrainGame, night: Float, laneFilter: Int = -1) {
    if (night > 0.05f) {
        // Headlamp cone, lit windows, glowing zombie eyes: only visible once the sky darkens.
        val lampX = g.locoLeft + g.shape.locoW + 2f
        path.rewind()
        path.moveTo(lampX, 436f); path.lineTo(lampX + 420f, 400f); path.lineTo(lampX + 420f, 575f); path.lineTo(lampX, 441f); path.close()
        drawPath(path, Brush.horizontalGradient(listOf(Color(0xFFFFF3B0).copy(alpha = 0.42f * night), Color.Transparent), startX = lampX, endX = lampX + 420f))
        drawCircle(Color(0xFFFFF8D0).copy(alpha = night), 7f, Offset(lampX, 438f))
        TrainShapeArtRegistry.artFor(g.shape).paintWindows(this, g, Color(0xFFFFC96B).copy(alpha = 0.9f * night))
        for (zi in g.zombies.indices) {
            val z = g.zombies[zi]
            if (laneFilter >= 0 && z.lane != laneFilter) continue
            val dir = z.facing.toFloat()
            val ex = z.x + dir * 6f * z.type.size
            val ey = z.y - (if (z.type.aimHeight < 20f) z.type.aimHeight * 1.5f else 45f) * z.type.size
            val er = if (z.isBoss) 5f else 2.4f
            drawCircle(Color(0xFFFF3B30).copy(alpha = night), er, Offset(ex, ey))
            drawCircle(Color(0xFFFF3B30).copy(alpha = 0.25f * night), er * 3f, Offset(ex, ey))
        }
    }
    for (i in 0..g.slotCount) {
        val gs = g.gunners[i]
        if (gs.flash <= 0f) continue
        val len = weaponLength(gs.weapon)
        val mx = g.gunX(i) + gs.facing * cos(gs.aim) * len
        val my = g.shoulderY(i) + sin(gs.aim) * len
        val r = if (gs.weapon == WeaponType.CANNON) 20f else 11f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300).copy(alpha = 0.6f), Color.Transparent), center = Offset(mx, my), radius = r), r, Offset(mx, my))
    }
    drawFlame(g)
    drawOverdrive(g)
    if (g.teslaLevel > 0 && g.teslaFlash > 0f) {
        val cx = g.teslaX
        val cy = g.teslaY - 38f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFB3F0FF), Color(0xFF40C4FF).copy(alpha = 0.5f), Color.Transparent), center = Offset(cx, cy), radius = 34f), 34f, Offset(cx, cy))
    }
    if (g.flakLevel > 0 && g.flakFlash > 0f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color.Transparent), center = Offset(g.flakX + 12f, g.flakY - 28f), radius = 18f), 18f, Offset(g.flakX + 12f, g.flakY - 28f))
    }
    for (ti in g.tracers.indices) {
        val t = g.tracers[ti]
        if (laneFilter >= 0 && t.lane != laneFilter) continue
        if (t.style != ShotStyle.LINE) {
            drawShot(t)
            continue
        }
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
    outlinedText(hpText.get(g.hp.toInt() * 100000 + g.maxHp.toInt()) { "TRAIN  ${g.hp.toInt()}/${g.maxHp.toInt()}" }, barX + barW / 2f, barY + barH - 3.5f * u, 11f * u, 0xFFFFFFFF.toInt())

    // Coins.
    drawCircle(Color(0xFFFFC107), 8f * u, Offset(barX + 8f * u, barY + 36f * u))
    drawCircle(Color(0xFFFFE082), 4.5f * u, Offset(barX + 8f * u, barY + 36f * u), style = Stroke(1.6f * u))
    withTransformLeft(barX + 22f * u) { outlinedText(coinText.get(g.runCoins) { g.runCoins.toString() }, 0f, barY + 41f * u, 15f * u, 0xFFFFD54F.toInt()) }

    // Distance / kills on the right.
    outlinedText(distText.get(g.distanceMeters) { "${g.distanceMeters} m" }, size.width - 110f * u, barY + 14f * u, 15f * u, 0xFFFFFFFF.toInt())
    outlinedText(killText.get(g.kills) { "${g.kills} kills" }, size.width - 110f * u, barY + 32f * u, 11f * u, 0xFFE0E0E0.toInt())

    outlinedText(
        biomeText.get(g.biome.ordinal) {
            if (g.biome == Biome.PLAINS) g.biome.displayName
            else g.biome.displayName + "  x" + "%.2f".format(g.biome.coinMult).trimEnd('0').trimEnd('.') + " coins"
        },
        size.width - 110f * u, barY + 46f * u, 10f * u, g.biome.accent.toInt()
    )
    outlinedText(speedText.get((g.speedFactor * 100f).toInt()) { "${(60f * g.speedFactor).toInt()} km/h" }, size.width - 110f * u, barY + 59f * u, 10f * u, 0xFFB0BEC5.toInt())
    if (g.danger != DangerLevel.NORMAL) {
        outlinedText(dangerText.get(g.danger.ordinal) { "Danger: " + g.danger.displayName }, size.width - 110f * u, barY + 72f * u, 10f * u, g.danger.color.toInt())
    }

    g.boss?.let { boss ->
        val w = 240f * u
        val x = (size.width - w) / 2f
        val y = 14f * u
        rbox(Color.Black.copy(alpha = 0.6f), x, y, w, 14f * u, 6f * u)
        val bossColor = when (boss.type) {
            ZombieType.GARGOYLE -> Color(0xFF78909C)
            ZombieType.CLOWN_PRINCE -> Color(0xFFE91E63)
            else -> Color(0xFF8E24AA)
        }
        rbox(bossColor, x + 2f * u, y + 2f * u, (w - 4f * u) * (boss.hp / boss.maxHp).coerceIn(0f, 1f), 10f * u, 5f * u)
        outlinedText(bossText.get(g.bossName.hashCode()) { g.bossName.uppercase() }, size.width / 2f, y + 11f * u, 10f * u, 0xFFFFFFFF.toInt())
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
    if (g.hordeWarning > 0f) {
        val pulse = 0.65f + 0.35f * sin(g.time * 14f)
        outlinedText(if (g.hordeFromLeft) "HORDE INCOMING FROM THE LEFT!" else "HORDE INCOMING FROM THE RIGHT!", size.width / 2f, size.height * 0.24f, 22f * u, 0xFFFF7043.toInt(), pulse)
    } else if (g.hordeTime > 0f) {
        outlinedText("HORDE - coins x1.5", size.width / 2f, size.height * 0.2f, 12f * u, 0xFFFFAB91.toInt())
    }
    if (g.overdriveTime > 0f) outlinedText("OVERDRIVE", size.width / 2f, size.height * 0.16f, 14f * u, 0xFFFFAB00.toInt())
    if (g.buffActive) outlinedText("DAMAGE BOOST +30%", size.width / 2f, size.height * 0.16f + 16f * u, 11f * u, 0xFFFF8A65.toInt())
}

// ------------------------------------------------------------------ field guide portraits

/** Draws one animated zombie of [type] scaled to fill this canvas (used by the Field Guide). */
fun DrawScope.drawZombiePortrait(type: ZombieType, timeSeconds: Float) {
    val height = PortraitSizes.heightOf(type) ?: (74f * max(1f, type.size))
    val k = min(size.height / height, size.width / (height * 0.9f))
    val z = Zombie(type, 0f, 0f, 1, 1f, 1f, 0.9f)
    z.facing = 1
    z.anim = timeSeconds * 3f
    z.x = size.width / (2f * k)
    z.y = (PortraitSizes.floatingAt(type) ?: 1f) * height - (if (PortraitSizes.floatingAt(type) == null) 4f else 0f)
    withTransform({ scale(k, k, Offset.Zero) }) { drawZombie(z) }
}

/** Draws [weapon] centred in this canvas (used by the Field Guide). */
fun DrawScope.drawWeaponPortrait(weapon: WeaponType) {
    val k = size.width / 70f
    withTransform({ translate(size.width * 0.1f, size.height / 2f); scale(k * 0.85f, k * 0.85f, Offset.Zero) }) {
        drawWeapon(weapon, 0f)
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


// ------------------------------------------------------------------ glue: palette, hats, default art and registration

/** The livery in use this frame (set at the start of [drawTrainScene]). */
internal var palette: Livery = Livery.CLASSIC

/** Humanoids drawn with the broad torso and thick arms. */
private val BULKY = setOf(ZombieType.BRUTE, ZombieType.YETI, ZombieType.STRONGMAN, ZombieType.MAGMA_GOLEM, ZombieType.YETI_KING, ZombieType.MAGMA_TITAN, ZombieType.DEMOLISHER)

/** Boss versions of the animal enemies are drawn with their smaller cousin's body. */
private val CREATURE_KIND = mapOf(
    ZombieType.RAT_KING to ZombieType.RAT,
    ZombieType.SCORPION_QUEEN to ZombieType.SCORPION,
    ZombieType.SWAMP_TYRANT to ZombieType.CROCODILE
)

private val CLOWN_TYPES = setOf(ZombieType.CLOWN, ZombieType.CLOWN_PRINCE, ZombieType.JUGGLER, ZombieType.MINI_CLOWN)

/** Neon and gold liveries have lit trim that glows along the train. */
private fun DrawScope.drawGlowTrim(g: TrainGame) {
    val pulse = 0.55f + 0.3f * sin(g.time * 3f)
    val trim = Color(palette.trim)
    drawLine(trim.copy(alpha = pulse), Offset(g.rearEnd, 446f), Offset(g.frontEnd - g.shape.nose, 446f), 2.5f)
    drawLine(trim.copy(alpha = pulse * 0.4f), Offset(g.rearEnd, 446f), Offset(g.frontEnd - g.shape.nose, 446f), 7f)
}

/** A procedurally-generated trail (model/Trails.kt) streaming behind the train; immediate-mode, so no particle pool needed. */
private fun DrawScope.drawCoinTrail(g: TrainGame, look: TrailLook) {
    val baseX = g.rearEnd
    val c1 = Color(look.color1)
    val c2 = Color(look.color2)
    for (i in 0 until look.density) {
        val t = ((g.time * look.speed + i / look.density.toFloat()) % 1f)
        val x = baseX - t * 260f
        val y = 450f + sin(g.time * 3f + i) * look.spread * t
        val mote = lerp(c1, c2, t).copy(alpha = (1f - t) * 0.9f)
        val size = 6.5f - t * 4.5f
        when (look.shape) {
            TrailShapeKind.DOT -> drawCircle(mote, size, Offset(x, y))
            TrailShapeKind.SPARK -> drawLine(mote, Offset(x, y), Offset(x - 10f, y), size * 0.7f, cap = StrokeCap.Round)
            TrailShapeKind.RIBBON -> drawLine(mote, Offset(x, y - size), Offset(x, y + size), size * 0.6f, cap = StrokeCap.Round)
            TrailShapeKind.DIAMOND -> {
                path.rewind()
                path.moveTo(x, y - size); path.lineTo(x + size, y); path.lineTo(x, y + size); path.lineTo(x - size, y); path.close()
                drawPath(path, mote)
            }
            TrailShapeKind.STAR -> {
                drawLine(mote, Offset(x - size, y), Offset(x + size, y), size * 0.3f)
                drawLine(mote, Offset(x, y - size), Offset(x, y + size), size * 0.3f)
            }
        }
    }
}

/** One survivor's body in local coordinates (feet at the origin, facing +x); shared by the train and the Garage portrait. */
internal fun DrawScope.drawAvatarBody(look: AvatarLook) {
    val shirt = Color(look.shirt)
    val skin = Color(look.skin)
    val pants = Color(look.pants)
    val accent = Color(look.accent)
    when (look.body) {
        AvatarBody.HUMAN -> {
            box(pants, -6f, -14f, 5f, 14f)
            box(pants, 1f, -14f, 5f, 14f)
            rbox(shirt, -8f, -34f, 16f, 22f, 4f)
            box(Color(0xFF4E342E), -9f, -32f, 5f, 14f) // pack
            if (look.extra == AvatarExtra.PONCHO) {
                path.rewind(); path.moveTo(-9f, -35f); path.lineTo(9f, -35f); path.lineTo(14f, -12f); path.lineTo(-14f, -12f); path.close()
                drawPath(path, accent)
                box(Color(0xFFFFCC80), -13f, -20f, 26f, 3f)
                box(Color(0xFFE53935), -7f, -38f, 14f, 3f) // scarf
            }
            if (look.extra == AvatarExtra.WELDER) { // tool belt
                box(Color(0xFF5D4037), -8f, -20f, 16f, 4f)
                box(Color(0xFF8D6E63), -7f, -18f, 5f, 5f)
                box(Color(0xFF8D6E63), 3f, -18f, 5f, 5f)
            }
            drawCircle(skin, 7f, Offset(1f, -42f))
            drawAvatarHat(look)
            if (look.extra == AvatarExtra.GOGGLES) {
                box(Color(look.hair), -10f, -46f, 6f, 12f) // ponytail
                box(Color(0xFF263238), -6f, -48f, 14f, 1.6f) // goggle strap
                drawCircle(Color(0xFF80DEEA), 2.8f, Offset(4f, -47f))
            }
            if (look.extra == AvatarExtra.WELDER) { // welding mask pushed up
                box(Color(0xFF37474F), -7f, -57f, 16f, 6f)
                box(Color(0xFF80DEEA), -5f, -55.5f, 12f, 3f)
            }
            drawCircle(Color(0xFF111111), 1.2f, Offset(4f, -43f))
        }
        AvatarBody.ROBOT -> {
            box(pants, -8f, -12f, 7f, 12f)
            box(pants, 1f, -12f, 7f, 12f)
            box(Color(0xFF90A4AE), -12f, -32f, 4f, 14f) // power pack
            rbox(shirt, -9f, -36f, 18f, 25f, 3f)
            box(accent, -1.5f, -32f, 3f, 10f) // red cross on the chest
            box(accent, -5f, -28.5f, 10f, 3f)
            rbox(skin, -7f, -52f, 16f, 14f, 3f)
            box(Color(0xFF00E5FF), -2f, -48f, 10f, 4f) // visor
            drawLine(Color(0xFF90A4AE), Offset(1f, -52f), Offset(1f, -58f), 2f)
            drawCircle(accent, 2f, Offset(1f, -59f))
        }
    }
}

/** A survivor standing at attention, filling this canvas (used by the Garage crew cards). */
fun DrawScope.drawSurvivorPortrait(slot: Int) {
    val look = SURVIVORS[slot % SURVIVORS.size].look
    val k = size.height / 72f
    withTransform({ translate(size.width / 2f, size.height - 3f); scale(k * look.width, k * look.height, Offset.Zero) }) {
        drawAvatarBody(look)
        box(Color(0xFF263238), 8f, -33f, 14f, 5f)
    }
}

/** Hair and headwear in the head's local coordinates (head centre at (1, -42)). */
private fun DrawScope.drawAvatarHat(look: AvatarLook) {
    val hat = Color(look.hatColor)
    box(Color(look.hair), -6f, -50f, 14f, 6f)
    when (look.hat) {
        HatStyle.NONE -> {}
        HatStyle.CAP -> { box(hat, -6f, -51f, 15f, 5f); box(hat, 6f, -48f, 7f, 2f) }
        HatStyle.COWBOY -> { drawOval(hat, Offset(-10f, -51f), Size(22f, 5f)); box(hat, -4f, -58f, 10f, 8f) }
        HatStyle.HELMET -> { drawArc(hat, 180f, 180f, true, Offset(-7f, -52f), Size(16f, 14f)); box(hat, -8f, -46f, 18f, 2f) }
        HatStyle.BANDANA -> { box(hat, -7f, -48f, 16f, 3f); box(hat, -10f, -47f, 4f, 5f) }
        HatStyle.BEANIE -> { drawArc(hat, 180f, 180f, true, Offset(-7f, -54f), Size(16f, 14f)); drawCircle(hat, 2f, Offset(1f, -55f)) }
        HatStyle.PHARAOH -> {
            val stripe = Color(look.accent)
            box(hat, -7f, -58f, 16f, 7f) // flat crown
            path.rewind(); path.moveTo(-9f, -52f); path.lineTo(-5f, -52f); path.lineTo(-7f, -38f); path.lineTo(-11f, -38f); path.close()
            drawPath(path, hat) // left lappet, hanging past the shoulder
            path.rewind(); path.moveTo(7f, -52f); path.lineTo(11f, -52f); path.lineTo(13f, -38f); path.lineTo(9f, -38f); path.close()
            drawPath(path, hat) // right lappet
            box(stripe, -7f, -55f, 16f, 1.6f) // lapis stripe across the crown
            drawCircle(stripe, 1.6f, Offset(1f, -53f)) // uraeus cobra at the brow
        }
        HatStyle.NINJA -> {
            box(hat, -7f, -52f, 15f, 11f) // hood wrapping the head and lower face
            box(Color(0xFFECEFF1), -5f, -47f, 11f, 2.2f) // eye slit
            path.rewind(); path.moveTo(-7f, -46f); path.lineTo(-14f, -40f); path.lineTo(-7f, -41f); path.close()
            drawPath(path, hat) // trailing hood tail
        }
    }
}

/** The original six routes are drawn by this file's style-driven scenery functions. */
private class ClassicRouteArt(private val biome: Biome) : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) = with(s) { drawBackdrop(g, biome, light, sunset) }
    override fun foreground(s: DrawScope, g: TrainGame, light: Float) = with(s) { drawForeground(g, biome, light) }
    override fun weather(s: DrawScope, g: TrainGame) = with(s) { drawWeather(g, biome) }
    override fun darkness(night: Float): Float = if (biome == Biome.TUNNEL) 0.62f else night * 0.5f
    override fun glowNight(night: Float): Float = if (biome == Biome.TUNNEL) 0.9f else night
    override fun afterDarkness(s: DrawScope, g: TrainGame) = with(s) { if (biome == Biome.TUNNEL) drawTunnelLampGlow(g) }
}

/** The classic steam locomotive and boxcars. */
private object SteamShapeArt : TrainShapeArt {
    override fun paintLoco(s: DrawScope, g: TrainGame, x0: Float) = with(s) { drawLoco(g, x0) }
    override fun paintCar(s: DrawScope, g: TrainGame, x0: Float, index: Int, slot: Int) = with(s) { drawCar(g, x0, index, slot) }
    override fun paintWindows(s: DrawScope, g: TrainGame, color: Color) = with(s) { drawWindows(g, color) }
}

/** Registers everything this file draws, then the art files for routes, enemies and train shapes. Runs once. */
private fun installRenderRegistries() {
    for (biome in listOf(Biome.PLAINS, Biome.MOUNTAIN, Biome.TUNNEL, Biome.DESERT, Biome.GRAVEYARD, Biome.SWAMP)) {
        RouteArtRegistry.register(biome, ClassicRouteArt(biome))
    }
    TrainShapeArtRegistry.register(TrainShape.STEAM, SteamShapeArt)
    MobArtRegistry.register(listOf(ZombieType.BAT, ZombieType.GARGOYLE)) { s, z, d -> with(s) { drawBat(z, d) } }
    MobArtRegistry.register(listOf(ZombieType.ZOMBIE_DOG, ZombieType.ICE_WOLF, ZombieType.RAT, ZombieType.SCORPION, ZombieType.CROCODILE, ZombieType.SCARAB)) { s, z, d -> with(s) { drawCreature(z, d) } }
    registerMobLooks()
    registerMobArts()
    registerRouteArts()
    registerTrainShapes()
}

private val renderRegistered = installRenderRegistries()

/** Streaks along the track once the train is really moving, so speed is felt as well as shown. */
private fun DrawScope.drawSpeedLines(g: TrainGame) {
    val speed = g.speedFactor
    if (speed < 1.5f) return
    val count = min(14, ((speed - 1.3f) * 5f).toInt())
    val span = g.worldW + 400f
    for (i in 0 until count) {
        val y = 524f + hash(i + 31) * 190f
        val x = wrap(hash(i) * span - g.scroll * (1.5f + hash(i + 7)), span) - 200f
        val len = 40f + speed * 26f
        drawLine(Color.White.copy(alpha = 0.16f + 0.1f * hash(i + 9)), Offset(x, y), Offset(x + len, y), 2f)
    }
}

/** A small railed platform for the partner's lent passenger, with a cyan glow so it reads as a visitor. */
private fun DrawScope.drawGuestMount(g: TrainGame) {
    val x = g.gunX(g.slotCount)
    val y = g.gunY(g.slotCount)
    box(Color(0xFF455A64), x - 16f, y, 32f, 5f)
    box(Color(0xFF90A4AE), x - 16f, y - 12f, 2f, 12f)
    box(Color(0xFF90A4AE), x + 14f, y - 12f, 2f, 12f)
    drawCircle(Color(0xFF40E0FF).copy(alpha = 0.18f + 0.1f * sin(g.time * 5f)), 22f, Offset(x, y - 22f))
}

/** Shows how long a lent passenger is still away from their own seat. */
private fun DrawScope.drawAwayMarkers(g: TrainGame) {
    for (i in g.seatAwayTime.indices) {
        val left = g.seatAwayTime[i]
        if (left <= 0f || !g.carAlive(i)) continue
        val x = g.gunX(i)
        val y = g.gunY(i)
        box(Color(0xFF263238).copy(alpha = 0.7f), x - 14f, y - 4f, 28f, 4f)
        outlinedText("away " + left.toInt() + "s", x, y - 14f, 12f, 0xFF80DEEA.toInt())
    }
}

/** The stranded survivor: waving next to a flare, with a ring that fills while a lent passenger is helping. */
private fun DrawScope.drawStranded(g: TrainGame) {
    val x = (g.trainLeft + g.trainLength + 110f).coerceAtMost(g.worldW - 40f)
    val y = RAIL_Y + 30f
    val pulse = 0.5f + 0.5f * sin(g.time * 6f)
    drawCircle(Color(0xFFFFD54F).copy(alpha = 0.15f + 0.15f * pulse), 46f, Offset(x, y - 40f))
    box(Color(0xFFFFEB3B), x - 12f, y - 34f, 24f, 24f) // jacket
    box(Color(0xFF455A64), x - 8f, y - 10f, 16f, 10f)
    drawCircle(Color(0xFFE0AC69), 8f, Offset(x, y - 44f))
    val wave = sin(g.time * 9f) * 8f
    drawLine(Color(0xFFFFEB3B), Offset(x + 10f, y - 30f), Offset(x + 20f + wave, y - 56f), 4f)
    drawLine(Color(0xFFFF5252), Offset(x - 26f, y), Offset(x - 26f, y - 70f), 3f) // flare stick
    drawCircle(Color(0xFFFF8A65), 6f + 3f * pulse, Offset(x - 26f, y - 72f))
    if (g.rescueFraction > 0f) {
        drawArc(Color(0xFF66FF8A), -90f, 360f * g.rescueFraction, false, Offset(x - 34f, y - 84f), Size(68f, 68f), style = Stroke(5f))
    }
    outlinedText("SOS " + g.rescueTime.toInt() + "s", x, y - 98f, 14f, 0xFFFFD54F.toInt())
}

/** Spilled puddles on the ground beside the train: a glossy pool with ripples and rising wisps, fading out at the end. */
private fun DrawScope.drawPuddles(rig: TrainGame) {
    for (i in rig.puddles.indices) {
        val p = rig.puddles[i]
        val fade = p.left.coerceIn(0f, 1f)
        val c = Color(p.who.puddleColor)
        val y = RAIL_Y + 42f
        drawOval(c.copy(alpha = 0.5f * fade), Offset(p.x - p.radius, y - 9f), Size(p.radius * 2f, 22f))
        drawOval(Color.White.copy(alpha = 0.25f * fade), Offset(p.x - p.radius * 0.6f, y - 6f), Size(p.radius * 1.0f, 7f))
        for (k in 0..1) {
            val t = (rig.time * 0.7f + k * 0.5f) % 1f
            drawOval(c.copy(alpha = 0.55f * (1f - t) * fade), Offset(p.x - p.radius * t, y + 2f - 11f * t), Size(p.radius * 2f * t, 22f * t), style = Stroke(1.6f))
        }
        for (k in 0..2) {
            val rise = (rig.time * 22f + k * 9f) % 16f
            drawCircle(Color.White.copy(alpha = 0.5f * fade * (1f - rise / 16f)), 2f, Offset(p.x - p.radius * 0.5f + k * p.radius * 0.5f, y - 4f - rise))
        }
    }
}

/** A pigeon on its way to a target (see Hitchhiker.birds): it arcs out of the cage, strikes, then flutters up and away. */
private fun DrawScope.drawPigeon(t: Tracer) {
    val u = (1f - t.life / t.total).coerceIn(0f, 1f)
    val hitAt = 0.36f
    val dir = if (t.x2 >= t.x1) 1f else -1f
    val x: Float
    val y: Float
    var alpha = 1f
    if (u < hitAt) {
        val k = u / hitAt
        x = t.x1 + (t.x2 - t.x1) * k
        y = t.y1 + (t.y2 - t.y1) * k - sin(k * 3.1416f) * 26f
    } else {
        val k = (u - hitAt) / (1f - hitAt)
        x = t.x2 + dir * k * 90f
        y = t.y2 - k * 140f
        alpha = 1f - k
    }
    val flap = sin(u * 70f)
    withTransform({ translate(x, y); scale(dir * 1.3f, 1.3f, Offset.Zero) }) {
        for (k in 1..3) drawCircle(Color.White.copy(alpha = alpha * (0.55f - k * 0.13f)), 2f, Offset(-10f - k * 6f, k * 1.2f)) // feather trail
        path.rewind(); path.moveTo(-3f, -4f); path.lineTo(-14f, -8f - 14f * flap); path.lineTo(4f, -3f); path.close()
        drawPath(path, Color(0xFF78909C).copy(alpha = alpha)) // far wing
        drawOval(Color(0xFFB0BEC5).copy(alpha = alpha), Offset(-10f, -6f), Size(20f, 11f))
        drawOval(Color(0xFFCFD8DC).copy(alpha = alpha), Offset(-5f, -2f), Size(12f, 5f))
        drawCircle(Color(0xFF90A4AE).copy(alpha = alpha), 4.6f, Offset(10f, -6f))
        drawCircle(Color(0xFF7E57C2).copy(alpha = alpha * 0.8f), 2.6f, Offset(8f, -3.5f)) // iridescent neck
        path.rewind(); path.moveTo(14f, -6.5f); path.lineTo(20f, -5f); path.lineTo(14f, -4f); path.close()
        drawPath(path, Color(0xFFFFA726).copy(alpha = alpha)) // beak
        drawCircle(Color(0xFFFF6D00).copy(alpha = alpha), 1.2f, Offset(11.5f, -7f))
        path.rewind(); path.moveTo(-2f, -4f); path.lineTo(-12f, -6f + 12f * flap); path.lineTo(5f, -2f); path.close()
        drawPath(path, Color(0xFFCFD8DC).copy(alpha = alpha)) // near wing
    }
}

// ------------------------------------------------------------------ hitchhiker projectiles (see ShotStyle)

/** Draws one flying shot of a hitchhiker's weapon, dispatching on its style. */
private fun DrawScope.drawShot(t: Tracer) {
    when (t.style) {
        ShotStyle.BIRD -> drawPigeon(t)
        ShotStyle.PAN -> drawPan(t)
        ShotStyle.HARPOON -> drawHarpoon(t)
        ShotStyle.BOX -> drawMimeBox(t)
        ShotStyle.GUST -> drawGust(t)
        ShotStyle.SOUP -> drawLob(t, soup = true)
        ShotStyle.SHELL -> drawLob(t, soup = false)
        ShotStyle.CONFETTI -> drawStreamers(t)
        ShotStyle.BOOT -> drawBoot(t)
        ShotStyle.LINE -> {}
    }
}

/** Progress of a shot 0..1 over its visible life, the fraction of it spent flying, and the same split for the part after the hit. */
private fun shotProgress(t: Tracer): Float = (1f - t.life / t.total).coerceIn(0f, 1f)
private fun shotHitAt(t: Tracer): Float = t.style.flight / t.style.life

/** Grandma's frying pan: thrown spinning, it clangs off the zombie and tumbles away. */
private fun DrawScope.drawPan(t: Tracer) {
    val hitAt = shotHitAt(t)
    val u = shotProgress(t)
    val k = (u / hitAt).coerceAtMost(1f)
    val after = if (u > hitAt) (u - hitAt) / (1f - hitAt) else 0f
    val dir = if (t.x2 >= t.x1) 1f else -1f
    val x = t.x1 + (t.x2 - t.x1) * k + after * 14f * dir
    val y = t.y1 + (t.y2 - t.y1) * k - sin(k * 3.1416f) * 18f + after * after * 90f
    val alpha = 1f - after
    withTransform({ translate(x, y); rotate(u * 1100f, Offset.Zero) }) {
        drawLine(Color(0xFF6D4C41).copy(alpha = alpha), Offset.Zero, Offset(16f, 0f), 3.4f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF263238).copy(alpha = alpha), 9f, Offset(-6f, 0f))
        drawCircle(Color(0xFF607D8B).copy(alpha = alpha), 7f, Offset(-6f, 0f))
        drawCircle(Color.White.copy(alpha = alpha * 0.4f), 2.4f, Offset(-8f, -2.5f))
    }
    if (u > hitAt && u < hitAt + 0.18f) outlinedText("CLANG", t.x2, t.y2 - 26f - after * 20f, 14f, 0xFFFFF59D.toInt(), 1f - after * 3f)
}

/** Dave's harpoon: a spear trailing a fishing line back to the gun; it sticks in the target, then the line goes slack and fades. */
private fun DrawScope.drawHarpoon(t: Tracer) {
    val hitAt = shotHitAt(t)
    val u = shotProgress(t)
    val k = (u / hitAt).coerceAtMost(1f)
    val after = if (u > hitAt) (u - hitAt) / (1f - hitAt) else 0f
    val tx = t.x1 + (t.x2 - t.x1) * k
    val ty = t.y1 + (t.y2 - t.y1) * k
    val alpha = 1f - after * after
    val sag = sin(k * 3.1416f) * 6f + after * 14f
    drawLine(Color(0xFFE0E0E0).copy(alpha = alpha * 0.8f), Offset(t.x1, t.y1), Offset((t.x1 + tx) / 2f, (t.y1 + ty) / 2f + sag), 1.4f)
    drawLine(Color(0xFFE0E0E0).copy(alpha = alpha * 0.8f), Offset((t.x1 + tx) / 2f, (t.y1 + ty) / 2f + sag), Offset(tx, ty), 1.4f)
    val angle = atan2(t.y2 - t.y1, t.x2 - t.x1) * 57.2958f
    withTransform({ translate(tx, ty); rotate(angle, Offset.Zero) }) {
        drawLine(Color(0xFF8D6E63).copy(alpha = alpha), Offset(-24f, 0f), Offset(0f, 0f), 3f, cap = StrokeCap.Round)
        path.rewind(); path.moveTo(0f, -4f); path.lineTo(11f, 0f); path.lineTo(0f, 4f); path.close()
        drawPath(path, Color(0xFFCFD8DC).copy(alpha = alpha))
        drawLine(Color(0xFFCFD8DC).copy(alpha = alpha), Offset(2f, -3f), Offset(-4f, -8f), 2f)
        drawLine(Color(0xFFCFD8DC).copy(alpha = alpha), Offset(2f, 3f), Offset(-4f, 8f), 2f)
    }
}

/** Marcel's invisible box: a glass cube closes in around the target and traps it. */
private fun DrawScope.drawMimeBox(t: Tracer) {
    val hitAt = shotHitAt(t)
    val u = shotProgress(t)
    val closing = 1.9f - 0.9f * (u / hitAt).coerceAtMost(1f)
    val alpha = if (u < 0.8f) 1f else (1f - (u - 0.8f) / 0.2f)
    val half = 22f * closing
    val c = Color(0xFFE3F2FD).copy(alpha = alpha * 0.9f)
    val off = 7f
    drawRect(c, Offset(t.x2 - half, t.y2 - half), Size(half * 2f, half * 2f), style = Stroke(2f))
    drawRect(c.copy(alpha = alpha * 0.5f), Offset(t.x2 - half + off, t.y2 - half - off), Size(half * 2f, half * 2f), style = Stroke(1.6f))
    for (corner in 0..3) {
        val cx = t.x2 + (if (corner and 1 == 0) -half else half)
        val cy = t.y2 + (if (corner and 2 == 0) -half else half)
        drawLine(c.copy(alpha = alpha * 0.6f), Offset(cx, cy), Offset(cx + off, cy - off), 1.6f)
    }
    drawRect(Color(0xFF90CAF9).copy(alpha = alpha * 0.12f), Offset(t.x2 - half, t.y2 - half), Size(half * 2f, half * 2f))
    if (u > hitAt) drawCircle(Color.White.copy(alpha = alpha * 0.8f), 7f, Offset(t.x2 + half + off + 4f, t.y2 - half - off - 2f)) // a gloved hand taps the glass
}

/** Sparky's leaf blower: a wide rush of air with leaves tumbling along it. */
private fun DrawScope.drawGust(t: Tracer) {
    val u = shotProgress(t)
    val head = (u / shotHitAt(t)).coerceAtMost(1.15f)
    val dx = t.x2 - t.x1
    val dy = t.y2 - t.y1
    val alpha = if (u < 0.6f) 1f else (1f - (u - 0.6f) / 0.4f)
    for (i in 0..4) {
        val off = (i - 2) * 8f * (0.4f + head)
        val sx = t.x1 + dx * (head - 0.45f).coerceAtLeast(0f)
        val sy = t.y1 + dy * (head - 0.45f).coerceAtLeast(0f) + off
        drawLine(Color(0xFFE1F5FE).copy(alpha = alpha * 0.55f), Offset(sx, sy), Offset(t.x1 + dx * head, t.y1 + dy * head + off), 2.6f, cap = StrokeCap.Round)
    }
    for (i in 0..3) {
        val k = (head - i * 0.12f).coerceIn(0f, 1.1f)
        val lx = t.x1 + dx * k
        val ly = t.y1 + dy * k + sin(k * 14f + i) * 9f
        drawCircle((if (i % 2 == 0) Color(0xFFFF8F00) else Color(0xFF7CB342)).copy(alpha = alpha), 3f, Offset(lx, ly))
    }
}

/** The soup pot (or the general's shell): lobbed in a high arc, shedding droplets (or smoke) behind it. */
private fun DrawScope.drawLob(t: Tracer, soup: Boolean) {
    val hitAt = shotHitAt(t)
    val u = shotProgress(t)
    if (!soup && u < 0.5f) { // the megaphone's shout rings out of the launcher
        for (r in 0..1) {
            val rr = 8f + (u * 2f + r * 0.35f) * 70f
            drawCircle(Color(0xFFFFE082).copy(alpha = ((1f - u * 2f) * 0.6f).coerceAtLeast(0f)), rr, Offset(t.x1, t.y1), style = Stroke(2.4f))
        }
    }
    if (u > hitAt) return
    val k = u / hitAt
    val arc = if (soup) 80f else 120f
    fun px(kk: Float) = t.x1 + (t.x2 - t.x1) * kk
    fun py(kk: Float) = t.y1 + (t.y2 - t.y1) * kk - sin(kk * 3.1416f) * arc
    val x = px(k)
    val y = py(k)
    for (i in 1..4) {
        val kk = (k - i * 0.04f).coerceAtLeast(0f)
        val c = if (soup) Color(0xFFFFB74D) else Color(0xFFB0B0B0)
        drawCircle(c.copy(alpha = 0.55f - i * 0.12f), (if (soup) 2.6f else 4f) - i * 0.3f, Offset(px(kk), py(kk) + i * 1.5f))
    }
    if (soup) {
        withTransform({ translate(x, y); rotate(k * 540f, Offset.Zero) }) {
            drawRect(Color(0xFFB71C1C), Offset(-7f, -6f), Size(14f, 11f))
            drawRect(Color(0xFF7F0000), Offset(-8f, -8f), Size(16f, 3f))
            drawLine(Color(0xFF263238), Offset(-7f, -4f), Offset(-12f, -8f), 2f)
            drawLine(Color(0xFF263238), Offset(7f, -4f), Offset(12f, -8f), 2f)
            drawCircle(Color(0xFFFFB74D), 3f, Offset(0f, -8f))
        }
    } else {
        val angle = atan2(py(k + 0.02f) - y, px(k + 0.02f) - x) * 57.2958f
        withTransform({ translate(x, y); rotate(angle, Offset.Zero) }) {
            drawRect(Color(0xFF607D8B), Offset(-9f, -3.5f), Size(16f, 7f))
            path.rewind(); path.moveTo(7f, -3.5f); path.lineTo(14f, 0f); path.lineTo(7f, 3.5f); path.close()
            drawPath(path, Color(0xFFD32F2F))
            drawLine(Color(0xFF455A64), Offset(-9f, -3.5f), Offset(-13f, -7f), 2f)
            drawLine(Color(0xFF455A64), Offset(-9f, 3.5f), Offset(-13f, 7f), 2f)
        }
    }
}

/** Mr. Confetti's party cannon: ribbons of confetti spiral to the target and pop there. */
private fun DrawScope.drawStreamers(t: Tracer) {
    val hitAt = shotHitAt(t)
    val u = shotProgress(t)
    val colors = longArrayOf(0xFFFF4081, 0xFFFFEB3B, 0xFF40C4FF, 0xFF69F0AE, 0xFFFF9100, 0xFFB388FF)
    val dx = t.x2 - t.x1
    val dy = t.y2 - t.y1
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    if (u <= hitAt) {
        val k = u / hitAt
        for (r in 0..11) {
            val kk = (k - (r % 4) * 0.05f).coerceAtLeast(0f)
            val wob = sin(kk * 11f + r) * 14f * (1f - kk * 0.6f)
            val x = t.x1 + dx * kk + nx * wob
            val y = t.y1 + dy * kk + ny * wob - sin(kk * 3.1416f) * 10f
            drawCircle(Color(colors[r % colors.size]), 2.6f, Offset(x, y))
            drawLine(Color(colors[r % colors.size]).copy(alpha = 0.7f), Offset(x, y), Offset(x - dx / len * 6f, y - dy / len * 6f), 1.6f)
        }
    } else {
        val a = (u - hitAt) / (1f - hitAt)
        for (r in 0..9) {
            val ang = r * 0.628f
            drawLine(Color(colors[r % colors.size]).copy(alpha = 1f - a), Offset(t.x2 + cos(ang) * 6f, t.y2 + sin(ang) * 6f), Offset(t.x2 + cos(ang) * (10f + 26f * a), t.y2 + sin(ang) * (10f + 26f * a)), 2f)
        }
        drawCircle(Color(0xFFFF4081).copy(alpha = 0.4f * (1f - a)), 14f * (0.6f + a), Offset(t.x2, t.y2), style = Stroke(2f)) // popped balloon
    }
}

/** The fisherman's old boot: an old boot flips through the air instead of a harpoon. */
private fun DrawScope.drawBoot(t: Tracer) {
    val u = shotProgress(t)
    val x = t.x1 + (t.x2 - t.x1) * u
    val y = t.y1 + (t.y2 - t.y1) * u - sin(u * 3.1416f) * 30f
    val alpha = if (u < 0.8f) 1f else (1f - (u - 0.8f) / 0.2f)
    withTransform({ translate(x, y); rotate(u * 720f, Offset.Zero) }) {
        drawRect(Color(0xFF5D4037).copy(alpha = alpha), Offset(-4f, -10f), Size(8f, 14f))
        drawOval(Color(0xFF4E342E).copy(alpha = alpha), Offset(-4f, 2f), Size(15f, 8f))
        drawRect(Color(0xFF3E2723).copy(alpha = alpha), Offset(-5f, -11f), Size(10f, 3f))
        drawCircle(Color(0xFF29B6F6).copy(alpha = alpha * 0.8f), 2f, Offset(6f, 12f)) // dripping
    }
}
