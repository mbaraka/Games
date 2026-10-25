package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.trainsurvivor.model.Element
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.cos
import kotlin.math.sin

// Elemental Wilds (registered in registerRouteArts in RouteArts.kt): a bright meadow under a rainbow, with floating islands, glowing
// crystals in every element's colour, giant flowers and fireflies. Cheerful on purpose: the critters are the danger.

internal object WildsArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF7EC8E3), Color(0xFFFFF1C1))
            // A rainbow arching behind the hills.
            val rx = g.worldW * 0.62f
            val bands = longArrayOf(0xFFFF7043, 0xFFFFCA28, 0xFF9CCC65, 0xFF4FC3F7, 0xFF9575CD)
            for (i in bands.indices) {
                val r = 330f - i * 12f
                drawArc(Color(bands[i]).copy(alpha = 0.3f), 180f, 180f, false, Offset(rx - r, HORIZON - r + 20f), Size(r * 2f, r * 2f), style = Stroke(11f))
            }
            drawCircle(Color.White.copy(alpha = 0.9f), 34f, Offset(g.worldW * 0.18f, 150f))
            // Floating islands carrying crystals.
            scenery(g, 420f, 0.12f, 0.1f) { x, r ->
                val y = 120f + r * 120f + sin(g.time * 0.6f + r * 8f) * 7f
                drawOval(Color(0xFF6FA84E), Offset(x, y), Size(120f, 22f))
                path.rewind(); path.moveTo(x + 10f, y + 14f); path.lineTo(x + 60f, y + 62f + r * 20f); path.lineTo(x + 110f, y + 14f); path.close()
                drawPath(path, Color(0xFF7A5C3A))
                val c = Color(Element.entries[(r * 5.9f).toInt()].color)
                path.rewind(); path.moveTo(x + 52f, y + 6f); path.lineTo(x + 60f, y - 34f); path.lineTo(x + 70f, y + 6f); path.close()
                drawPath(path, c.copy(alpha = 0.85f))
                drawCircle(c.copy(alpha = 0.2f), 22f, Offset(x + 60f, y - 14f))
            }
            drawRange(g, HORIZON - 14f, 80f, 0.0058f, 0.08f, SolidColor(Color(0xFF86CC7A)), 0.7f)
            drawRange(g, HORIZON + 4f, 50f, 0.009f, 0.18f, SolidColor(Color(0xFF58AE5E)), 2.4f)
            fillGround(g, Color(0xFF8BD16A), Color(0xFF3F7F3A))
            // Giant glowing flowers and mushrooms, and element crystals beside the track.
            scenery(g, 230f, 0.55f, 0.1f) { x, r ->
                val base = RAIL_Y - 4f
                val pulse = 0.6f + 0.4f * sin(g.time * 2f + r * 7f)
                if (r < 0.4f) { // flower
                    drawLine(Color(0xFF3E8E41), Offset(x, base), Offset(x, base - 70f - r * 40f), 5f, cap = StrokeCap.Round)
                    val cy = base - 74f - r * 40f
                    val c = Color(Element.entries[(r * 14f).toInt() % 6].color)
                    for (k in 0..5) {
                        val a = k * 1.047f + g.time * 0.2f
                        drawCircle(c.copy(alpha = 0.85f), 9f, Offset(x + cos(a) * 14f, cy + sin(a) * 14f))
                    }
                    drawCircle(Color(0xFFFFF59D), 8f, Offset(x, cy))
                } else if (r < 0.7f) { // mushroom
                    box(Color(0xFFF5EBD8), x - 5f, base - 40f, 10f, 40f)
                    drawArc(Color(0xFFE57373), 180f, 180f, true, Offset(x - 28f, base - 62f), Size(56f, 44f))
                    for (k in 0..2) drawCircle(Color.White.copy(alpha = 0.85f), 4f, Offset(x - 14f + k * 14f, base - 44f - (k % 2) * 6f))
                } else { // crystal cluster
                    val c = Color(Element.entries[(r * 20f).toInt() % 6].color)
                    for (k in -1..1) {
                        path.rewind(); path.moveTo(x + k * 12f - 7f, base); path.lineTo(x + k * 12f, base - 50f - (1 - kotlin.math.abs(k)) * 24f); path.lineTo(x + k * 12f + 7f, base); path.close()
                        drawPath(path, c.copy(alpha = 0.55f + 0.35f * pulse))
                    }
                    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.35f * pulse), Color.Transparent), center = Offset(x, base - 30f), radius = 50f), 50f, Offset(x, base - 30f))
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 150f, 1.4f, 0.2f) { x, r ->
                val base = 705f
                for (k in 0..5) {
                    val sway = sin(g.time * 1.4f + k + r * 9f) * 5f
                    drawLine(Color(0xFF2E7D32), Offset(x + k * 5f, base), Offset(x + k * 5f + sway, base - 28f - r * 20f), 3f, cap = StrokeCap.Round)
                }
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 30) { // floating elemental orbs and fireflies
                val c = Color(Element.entries[i % 6].color)
                val x = wrap(hash(i) * span - g.time * (8f + hash(i + 3) * 14f), span) - 50f
                val y = 120f + hash(i + 8) * 520f + sin(g.time * 0.8f + i) * 14f
                drawCircle(c.copy(alpha = 0.7f), 2f + hash(i + 9) * 2.2f, Offset(x, y))
                drawCircle(c.copy(alpha = 0.18f), 7f, Offset(x, y))
            }
        }
    }

    override fun darkness(night: Float) = night * 0.4f
    override fun glowNight(night: Float) = night
    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { tintTrain(g, Color(0xFF69F0AE).copy(alpha = 0.08f), alpha) }
}
