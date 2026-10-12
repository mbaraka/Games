package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.sin

// Jurassic Valley (registered in registerRouteArts in RouteArts.kt): a prehistoric jungle under a smoking volcano,
// with grazing giants in the distance, pterodactyls overhead and ferns, cycads and bones beside the track.

internal object JurassicArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF2F7F8F), Color(0xFFFFD08A))
            // A huge low sun and a belt of warm haze.
            val sunX = g.worldW * 0.7f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE0B2).copy(alpha = 0.6f), Color.Transparent), center = Offset(sunX, 190f), radius = 190f), 190f, Offset(sunX, 190f))
            drawCircle(Color(0xFFFFF3C4), 46f, Offset(sunX, 190f))
            // The volcano, slowly sliding by, with a smoke plume and a glowing crater.
            val vx = wrap(g.worldW * 0.35f - g.scroll * 0.03f, g.worldW + 700f) - 250f
            path.rewind(); path.moveTo(vx - 260f, HORIZON); path.lineTo(vx - 40f, HORIZON - 230f); path.lineTo(vx + 40f, HORIZON - 230f); path.lineTo(vx + 260f, HORIZON); path.close()
            drawPath(path, Color(0xFF5A4A52))
            drawLine(Color(0xFFFF6D00).copy(alpha = 0.8f), Offset(vx - 36f, HORIZON - 228f), Offset(vx - 70f, HORIZON - 120f), 5f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFF6D00).copy(alpha = 0.7f), Offset(vx + 30f, HORIZON - 226f), Offset(vx + 62f, HORIZON - 130f), 4f, cap = StrokeCap.Round)
            drawOval(Color(0xFFFF8F00).copy(alpha = 0.85f), Offset(vx - 42f, HORIZON - 238f), Size(84f, 16f))
            for (k in 0..5) {
                val t = wrap(g.time * 0.18f + k * 0.17f, 1f)
                drawCircle(Color(0xFF4D4044).copy(alpha = 0.5f * (1f - t)), 26f + t * 60f, Offset(vx + sin(g.time + k) * 14f + t * 40f, HORIZON - 244f - t * 190f))
            }
            // Pterodactyls gliding across the sky.
            for (k in 0..3) {
                val span = g.worldW + 300f
                val px = wrap(k * 380f + g.time * (24f + k * 7f), span) - 150f
                val py = 90f + k * 46f + sin(g.time * 0.8f + k) * 14f
                val flap = sin(g.time * 3f + k * 2f) * 6f
                drawLine(Color(0xFF2B3A3E).copy(alpha = 0.7f), Offset(px - 18f, py - flap), Offset(px, py), 3f, cap = StrokeCap.Round)
                drawLine(Color(0xFF2B3A3E).copy(alpha = 0.7f), Offset(px + 18f, py - flap), Offset(px, py), 3f, cap = StrokeCap.Round)
                drawLine(Color(0xFF2B3A3E).copy(alpha = 0.7f), Offset(px, py), Offset(px + 10f, py + 3f), 2.4f, cap = StrokeCap.Round)
            }
            drawRange(g, HORIZON - 6f, 70f, 0.0062f, 0.09f, SolidColor(Color(0xFF3D6B4A)), 1.1f)
            // Long-necked giants grazing on the far plain.
            scenery(g, 640f, 0.14f, 0.1f) { x, r ->
                val base = HORIZON - 4f
                val step = sin(g.time * 0.9f + r * 9f) * 2f
                val col = Color(0xFF2E5A47)
                drawOval(col, Offset(x, base - 54f), Size(86f, 40f))
                var px = x + 78f
                var py = base - 44f
                for (i in 1..5) {
                    val nx = x + 78f + i * 9f
                    val ny = base - 44f - i * 17f
                    drawLine(col, Offset(px, py), Offset(nx, ny), 17f - i * 2f, cap = StrokeCap.Round)
                    px = nx
                    py = ny
                }
                drawOval(col, Offset(px - 3f, py - 7f), Size(22f, 12f))
                for (k in 0..3) box(col, x + 8f + k * 22f + (if (k % 2 == 0) step else -step), base - 22f, 11f, 22f)
                path.rewind(); path.moveTo(x + 4f, base - 46f); path.lineTo(x - 70f, base - 22f + step); path.lineTo(x + 8f, base - 30f); path.close()
                drawPath(path, col)
            }
            drawRange(g, HORIZON + 6f, 46f, 0.0095f, 0.2f, SolidColor(Color(0xFF2F5A34)), 2.7f)
            fillGround(g, Color(0xFF6E8E3A), Color(0xFF3A4A1E))
            // Tall cycads and tree ferns beside the track, with the odd nest and skull.
            scenery(g, 230f, 0.55f, 0.1f) { x, r ->
                val base = RAIL_Y - 4f
                if (r < 0.45f) { // tree fern
                    val h = 70f + r * 90f
                    drawLine(Color(0xFF5D4630), Offset(x, base), Offset(x + sin(r * 7f) * 6f, base - h), 7f, cap = StrokeCap.Round)
                    for (k in 0..6) {
                        val a = -2.9f + k * 0.5f
                        val sway = sin(g.time * 0.9f + k + r * 9f) * 3f
                        drawLine(Color(0xFF3E8E41), Offset(x, base - h), Offset(x + kotlin.math.cos(a) * 44f, base - h + kotlin.math.sin(a) * 30f + sway), 4f, cap = StrokeCap.Round)
                    }
                } else if (r < 0.7f) { // big fern clump
                    for (k in 0..8) {
                        val a = -3.1f + k * 0.4f
                        drawLine(Color(0xFF4F9A3C), Offset(x, base), Offset(x + kotlin.math.cos(a) * 40f, base + kotlin.math.sin(a) * 52f), 3.4f, cap = StrokeCap.Round)
                    }
                } else if (r < 0.85f) { // nest of eggs
                    drawOval(Color(0xFF7A5C3A), Offset(x - 26f, base - 10f), Size(56f, 14f))
                    for (k in 0..2) drawOval(Color(0xFFEFE6C8), Offset(x - 16f + k * 16f, base - 22f - (k % 2) * 4f), Size(14f, 18f))
                } else { // a bleached skull and ribs
                    drawOval(Color(0xFFE8DFC4), Offset(x, base - 30f), Size(42f, 24f))
                    drawOval(Color(0xFF3A3226), Offset(x + 24f, base - 24f), Size(9f, 9f))
                    for (k in 0..3) drawLine(Color(0xFFE8DFC4), Offset(x - 8f - k * 9f, base - 4f), Offset(x - 2f - k * 9f, base - 28f - k * 3f), 4f, cap = StrokeCap.Round)
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            // Big leaves sweeping past in front of the camera.
            scenery(g, 260f, 1.4f, 0.25f) { x, r ->
                val base = 700f + r * 30f
                for (k in 0..4) {
                    val a = -2.6f + k * 0.4f
                    drawLine(Color(0xFF2F6B2E), Offset(x, base), Offset(x + kotlin.math.cos(a) * 70f, base + kotlin.math.sin(a) * 80f), 6f, cap = StrokeCap.Round)
                }
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 26) { // drifting ash and pollen
                val x = wrap(hash(i) * span - g.time * (16f + hash(i + 3) * 22f), span) - 50f
                val y = wrap(hash(i + 8) * 700f + g.time * (8f + hash(i + 5) * 12f), 700f)
                drawCircle((if (i % 3 == 0) Color(0xFFFFB74D) else Color(0xFFBCAAA4)).copy(alpha = 0.55f), 1.2f + hash(i + 9) * 1.6f, Offset(x, y))
            }
        }
    }

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { tintTrain(g, Color(0xFF558B2F).copy(alpha = 0.1f), alpha) }
}
