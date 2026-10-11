package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Deep Sea Tube and Solar Rail (registered in registerRouteArts in RouteArts.kt).

// ------------------------------------------------------------------ Deep Sea Tube

/** Porthole domes over the driver and wagons, a propeller and a trail of bubbles: the train's diving gear. */
private fun DrawScope.drawDiveKit(g: TrainGame, alpha: Float) {
    if (alpha < 0.02f) return
    tintTrain(g, Color(0xFF26C6DA).copy(alpha = 0.12f), alpha)
    val glass = Color(0xFF9FE8FF)
    val dx = g.locoLeft + g.shape.locoGunX - 34f
    val roof = g.shape.locoRoofY
    drawRoundRect(glass.copy(alpha = 0.2f * alpha), Offset(dx, roof - 16f), Size(76f, 66f), CornerRadius(24f))
    drawRoundRect(glass.copy(alpha = 0.7f * alpha), Offset(dx, roof - 16f), Size(76f, 66f), CornerRadius(24f), style = Stroke(2f))
    for (i in 1 until g.slotCount) {
        if (!g.carAlive(i)) continue
        val x0 = g.carLeft(i)
        drawArc(glass.copy(alpha = 0.16f * alpha), 180f, 180f, true, Offset(x0 + 4f, 372f), Size(g.shape.carW - 8f, 64f))
        drawArc(glass.copy(alpha = 0.6f * alpha), 180f, 180f, false, Offset(x0 + 4f, 372f), Size(g.shape.carW - 8f, 64f), style = Stroke(2f))
    }
    val px = g.rearEnd - 8f
    val py = 478f
    box(Color(0xFF37474F).copy(alpha = alpha), px - 4f, py - 8f, 10f, 16f)
    withTransform({ rotate(g.time * 600f, Offset(px - 6f, py)) }) {
        drawOval(Color(0xFFB0BEC5).copy(alpha = 0.8f * alpha), Offset(px - 8f, py - 14f), Size(4f, 28f))
    }
    for (k in 0..5) {
        val t = wrap(g.time * 1.2f + k * 0.17f, 1f)
        drawCircle(Color.White.copy(alpha = (1f - t) * 0.7f * alpha), 3f + k % 3, Offset(px - 18f - t * 60f, py - 4f - t * 30f + sin(t * 9f + k) * 4f), style = Stroke(1.5f))
    }
}

internal object DeepSeaArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF0E6FA3), Color(0xFF062A47)) // the whole view is water
            for (k in 0 until 6) { // light rays from the surface
                val x = g.worldW * (k / 6f) + sin(g.time * 0.3f + k) * 40f
                path.rewind(); path.moveTo(x, -10f); path.lineTo(x + 70f, -10f); path.lineTo(x + 190f, HORIZON); path.lineTo(x + 40f, HORIZON); path.close()
                drawPath(path, Color.White.copy(alpha = 0.05f))
            }
            val span = g.worldW + 300f
            for (f in 0 until 9) { // fish schools in the distance
                val x = wrap(f * 220f - g.time * (30f + f * 6f), span) - 100f
                val y = 90f + f * 36f + sin(g.time + f) * 10f
                drawOval(Color(0xFF80DEEA).copy(alpha = 0.35f), Offset(x, y), Size(22f, 9f))
                path.rewind(); path.moveTo(x + 20f, y + 4f); path.lineTo(x + 30f, y); path.lineTo(x + 30f, y + 9f); path.close()
                drawPath(path, Color(0xFF80DEEA).copy(alpha = 0.35f))
            }
            drawRange(g, HORIZON - 4f, 120f, 0.0048f, 0.07f, SolidColor(Color(0xFF0B4A6B)), 1.2f)
            drawRange(g, HORIZON + 8f, 70f, 0.008f, 0.16f, SolidColor(Color(0xFF083A56)), 2.6f)
            fillGround(g, Color(0xFF8F8A5E), Color(0xFF3F3B28))
            scenery(g, 250f, 0.55f, 0.15f) { x, r ->
                val base = RAIL_Y - 4f
                if (r < 0.4f) { // branching coral
                    val c = if (r < 0.2f) Color(0xFFFF7043) else Color(0xFFEC407A)
                    for (b in -2..2) drawLine(c, Offset(x, base), Offset(x + b * 12f, base - 46f + abs(b) * 8f), 5f, cap = StrokeCap.Round)
                    drawCircle(c, 6f, Offset(x, base - 46f))
                } else if (r < 0.75f) { // swaying kelp
                    var px = x
                    var py = base
                    for (i in 1..6) {
                        val nx = x + sin(g.time * 1.2f + i * 0.8f + r * 6f) * (4f + i * 3f)
                        val ny = base - i * 16f
                        drawLine(Color(0xFF2E7D32), Offset(px, py), Offset(nx, ny), 5f, cap = StrokeCap.Round)
                        px = nx
                        py = ny
                    }
                } else { // rock with a clam
                    drawOval(Color(0xFF4E5B63), Offset(x - 24f, base - 20f), Size(48f, 24f))
                    drawArc(Color(0xFFF8BBD0), 180f, 180f, true, Offset(x - 8f, base - 22f), Size(16f, 12f))
                }
            }
            // The pressure tube the train runs in: steel rails, glass and ribs.
            val top = 330f
            val bottom = 574f
            drawRect(Color(0xFF80DEEA).copy(alpha = 0.06f), Offset(-200f, top), Size(g.worldW + 400f, bottom - top))
            box(Color(0xFF37474F), -200f, top - 8f, g.worldW + 400f, 12f)
            box(Color(0xFF263238), -200f, bottom, g.worldW + 400f, 10f)
            var x = -wrap(g.scroll, 180f) - 180f
            while (x < g.worldW + 180f) {
                box(Color(0xFF455A64), x, top - 8f, 10f, bottom - top + 18f)
                drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.85f), 4f, Offset(x + 5f, top + 8f))
                x += 180f
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 300f, 1.35f, 0.3f) { x, r ->
                val y = 655f + r * 40f
                drawOval(Color(0xFF4E5B63), Offset(x, y), Size(60f + r * 30f, 22f))
                drawArc(Color(0xFFFFCCBC), 180f, 180f, true, Offset(x + 14f, y - 8f), Size(22f, 14f))
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 40) {
                val x = wrap(hash(i) * span + sin(g.time * 0.8f + i) * 14f, span) - 50f
                val y = 740f - wrap(hash(i + 5) * 760f + g.time * (30f + hash(i + 3) * 40f), 760f)
                drawCircle(Color.White.copy(alpha = 0.5f), 1.5f + hash(i + 8) * 3f, Offset(x, y), style = Stroke(1.2f))
            }
        }
    }

    override fun darkness(night: Float) = 0.22f
    override fun glowNight(night: Float) = 0.7f
    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { drawDiveKit(g, alpha) }
}

// ------------------------------------------------------------------ Solar Rail

/** Gold heat-shield foil on the roofs, a sun visor over the cab and shimmering heat. */
private fun DrawScope.drawHeatShieldKit(g: TrainGame, alpha: Float) {
    if (alpha < 0.02f) return
    tintTrain(g, Color(0xFFFFD54F).copy(alpha = 0.1f), alpha)
    val foil = Color(0xFFFFE082).copy(alpha = 0.9f * alpha)
    rbox(foil, g.locoLeft + g.shape.locoGunX - 36f, g.shape.locoRoofY - 8f, 80f, 7f, 3f)
    for (i in 1 until g.slotCount) if (g.carAlive(i)) rbox(foil, g.carLeft(i) - 2f, 414f, g.shape.carW + 4f, 6f, 3f)
    box(Color(0xFF263238).copy(alpha = 0.6f * alpha), g.locoLeft + 8f, 398f, 74f, 8f)
    val len = g.frontEnd - g.rearEnd
    for (k in 0..5) {
        val shimmer = 0.5f + 0.5f * sin(g.time * 5f + k)
        val y = 400f - wrap(g.time * 30f + k * 20f, 40f)
        drawLine(Color.White.copy(alpha = 0.18f * alpha * shimmer), Offset(g.rearEnd + k * (len / 6f), y), Offset(g.rearEnd + k * (len / 6f) + 30f, y), 2f)
    }
}

internal object SolarArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFFFF6F00), Color(0xFFFFE082))
            val cx = g.worldW * 0.28f
            val cy = 140f
            withTransform({ rotate(g.time * 3f, Offset(cx, cy)) }) { // corona rays
                for (k in 0 until 18) {
                    val a = k * (2f * PI.toFloat() / 18f)
                    val tip = 380f + 40f * sin(g.time * 2f + k)
                    path.rewind()
                    path.moveTo(cx + cos(a - 0.07f) * 230f, cy + sin(a - 0.07f) * 230f)
                    path.lineTo(cx + cos(a) * tip, cy + sin(a) * tip)
                    path.lineTo(cx + cos(a + 0.07f) * 230f, cy + sin(a + 0.07f) * 230f)
                    path.close()
                    drawPath(path, Color(0xFFFFF59D).copy(alpha = 0.22f))
                }
            }
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), Color(0xFFFFEB3B).copy(alpha = 0.55f), Color.Transparent), center = Offset(cx, cy), radius = 330f), 330f, Offset(cx, cy))
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFFF176)), center = Offset(cx, cy), radius = 210f), 210f, Offset(cx, cy))
            for (k in 0..2) { // prominences
                val start = -30f + k * 55f + sin(g.time * 0.5f + k) * 6f
                drawArc(Color(0xFFFF6D00).copy(alpha = 0.8f), start, 40f, false, Offset(cx - 240f, cy - 240f), Size(480f, 480f), style = Stroke(10f, cap = StrokeCap.Round))
            }
            val span = g.worldW + 400f
            for (i in 0 until 5) drawOval(Color.White.copy(alpha = 0.07f), Offset(wrap(i * 300f - g.time * 20f, span) - 200f, HORIZON - 40f + i * 6f), Size(360f, 14f))
            drawRange(g, HORIZON - 4f, 110f, 0.005f, 0.07f, SolidColor(Color(0xFF8A3B0F)), 0.9f)
            drawRange(g, HORIZON + 6f, 70f, 0.0085f, 0.16f, SolidColor(Color(0xFF5E2808)), 2.3f)
            fillGround(g, Color(0xFFB85C1A), Color(0xFF5A2A0C))
            scenery(g, 220f, 1.0f, 0.1f) { x, r ->
                val y0 = 540f + r * 140f
                val pulse = 0.6f + 0.4f * sin(g.time * 2f + r * 6f)
                drawLine(Color(0xFFFFE082).copy(alpha = 0.3f * pulse), Offset(x, y0), Offset(x + 60f, y0 + 14f), 7f)
                drawLine(Color(0xFFFFF59D).copy(alpha = pulse), Offset(x, y0), Offset(x + 60f, y0 + 14f), 3f)
                drawLine(Color(0xFFFFF59D).copy(alpha = pulse), Offset(x + 60f, y0 + 14f), Offset(x + 92f, y0 - 4f), 3f)
            }
            scenery(g, 300f, 0.55f, 0.15f) { x, r -> // solar panel arrays
                val base = RAIL_Y - 4f
                box(Color(0xFF37474F), x - 3f, base - 56f, 6f, 56f)
                path.rewind(); path.moveTo(x - 40f, base - 66f); path.lineTo(x + 40f, base - 88f); path.lineTo(x + 40f, base - 54f); path.lineTo(x - 40f, base - 32f); path.close()
                drawPath(path, Color(0xFF1A237E))
                drawPath(path, Color(0xFFFFD54F), style = Stroke(2.5f))
                for (k in 1..3) drawLine(Color(0xFFFFD54F).copy(alpha = 0.6f), Offset(x - 40f + k * 20f, base - 66f + k * 3f - 22f * (k / 4f)), Offset(x - 40f + k * 20f, base - 32f + k * 3f - 22f * (k / 4f)), 1.2f)
                if (r > 0.6f) for (k in 0..3) box(Color(0xFF90A4AE), x + 44f, base - 40f + k * 8f, 16f, 3f) // radiator fins
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 300f, 1.35f, 0.3f) { x, r ->
                val y = 655f + r * 35f
                drawOval(Color(0xFF3E1F0A), Offset(x, y), Size(60f + r * 30f, 22f))
                drawOval(Color(0xFFFF9800).copy(alpha = 0.7f), Offset(x, y), Size(60f + r * 30f, 22f), style = Stroke(2f))
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 30) { // plasma motes
                val x = wrap(hash(i) * span + sin(g.time + i) * 22f, span) - 50f
                val y = 740f - wrap(hash(i + 5) * 760f + g.time * (50f + hash(i + 3) * 50f), 760f)
                drawCircle(Color(0xFFFFFDE7).copy(alpha = 0.8f), 1.5f + hash(i + 8) * 1.8f, Offset(x, y))
            }
        }
    }

    override fun darkness(night: Float) = 0f
    override fun glowNight(night: Float) = 0f
    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { drawHeatShieldKit(g, alpha) }
}
