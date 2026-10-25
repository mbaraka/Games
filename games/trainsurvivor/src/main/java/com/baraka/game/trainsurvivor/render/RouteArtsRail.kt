package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.abs
import kotlin.math.sin

// The three maps built around two trains (registered in registerRouteArts in RouteArts.kt):
//   Switchyard (shared worlds)      - a busy rail yard under signal gantries
//   Broken Viaduct (co-op)          - a crumbling concrete viaduct over a dusty canyon
//   Fault Line (shared worlds)      - a cracked red canyon split by a glowing fissure

internal object SwitchyardArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF37474F), Color(0xFFFFB74D))
            drawCircle(Color(0xFFFFE0B2).copy(alpha = 0.8f), 36f, Offset(g.worldW * 0.25f, 200f))
            drawRange(g, HORIZON - 10f, 60f, 0.006f, 0.08f, SolidColor(Color(0xFF455A64)), 0.6f)
            // Sheds, a water tower and smoking stacks along the back of the yard.
            scenery(g, 330f, 0.3f, 0.05f) { x, r ->
                val w = 120f + r * 90f
                val h = 54f + r * 40f
                box(Color(0xFF3B4147), x, HORIZON - h, w, h + 8f)
                path.rewind(); path.moveTo(x - 6f, HORIZON - h); path.lineTo(x + w / 2f, HORIZON - h - 26f); path.lineTo(x + w + 6f, HORIZON - h); path.close()
                drawPath(path, Color(0xFF2B3035))
                for (k in 0..2) box(Color(0xFFFFCA6B).copy(alpha = 0.55f), x + 14f + k * (w / 3.4f), HORIZON - h + 14f, 16f, 12f)
                if (r > 0.6f) { // water tower
                    box(Color(0xFF2B3035), x + w + 14f, HORIZON - h - 40f, 6f, 70f)
                    rbox(Color(0xFF4A4036), x + w, HORIZON - h - 64f, 34f, 28f, 6f)
                }
            }
            fillGround(g, Color(0xFF5D5149), Color(0xFF2B2622))
            // Extra tracks fanning across the yard floor.
            for (k in 0..3) {
                val y = 560f + k * 38f
                box(Color(0xFF8A8F95).copy(alpha = 0.55f), -50f, y, g.worldW + 100f, 3f)
                var tx = -wrap(g.scroll * (0.7f + k * 0.12f), 36f) - 36f
                while (tx < g.worldW + 36f) {
                    box(Color(0xFF3A2E24).copy(alpha = 0.7f), tx, y - 3f, 10f, 9f)
                    tx += 36f
                }
            }
            // Signal gantries with blinking lamps, and stacks of containers.
            scenery(g, 340f, 0.55f, 0.1f) { x, r ->
                val base = RAIL_Y - 4f
                if (r < 0.5f) {
                    box(Color(0xFF263238), x, base - 120f, 8f, 120f)
                    box(Color(0xFF263238), x - 30f, base - 120f, 70f, 6f)
                    val on = sin(g.time * 2.5f + x * 0.01f) > 0f
                    drawCircle((if (on) Color(0xFFFF5252) else Color(0xFF7A2A2A)), 6f, Offset(x - 22f, base - 106f))
                    drawCircle((if (on) Color(0xFF2E5A2E) else Color(0xFF69F0AE)), 6f, Offset(x + 30f, base - 106f))
                } else {
                    val colors = longArrayOf(0xFFC0392B, 0xFF2E86C1, 0xFFD4AC0D, 0xFF229954)
                    for (k in 0..2) {
                        val c = Color(colors[(x.toInt() / 7 + k).mod(4)])
                        box(c, x + k * 46f, base - 30f, 44f, 30f)
                        for (n in 0..4) box(Color.Black.copy(alpha = 0.18f), x + k * 46f + 6f + n * 8f, base - 28f, 2f, 26f)
                    }
                    box(Color(0xFF8E44AD), x + 20f, base - 62f, 44f, 30f)
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 300f, 1.4f, 0.3f) { x, r ->
                box(Color(0xFF4A4038), x, 690f + r * 20f, 60f, 6f)
                box(Color(0xFFFFB300), x + 10f, 676f + r * 20f, 6f, 14f) // points lever
                drawCircle(Color(0xFFFFB300), 5f, Offset(x + 13f, 674f + r * 20f))
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            for (k in 0..3) { // steam from the yard engines
                val x = g.worldW * (0.15f + k * 0.25f)
                for (i in 0..3) {
                    val t = wrap(g.time * 0.35f + i * 0.25f + k * 0.13f, 1f)
                    drawCircle(Color.White.copy(alpha = 0.16f * (1f - t)), 10f + t * 34f, Offset(x + sin(g.time + i) * 10f - t * 20f, HORIZON - 30f - t * 130f))
                }
            }
        }
    }

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { tintTrain(g, Color(0xFF263238).copy(alpha = 0.12f), alpha) }
}

internal object BrokenViaductArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF5C6B73), Color(0xFFD7CCC8))
            drawCircle(Color(0xFFFFF3E0).copy(alpha = 0.7f), 40f, Offset(g.worldW * 0.8f, 170f))
            drawRange(g, HORIZON - 30f, 100f, 0.0048f, 0.06f, SolidColor(Color(0xFF8A7B72)), 0.3f)
            drawRange(g, HORIZON - 4f, 60f, 0.0075f, 0.14f, SolidColor(Color(0xFF6B5E57)), 1.7f)
            // A far viaduct: arches under a deck, some of them broken.
            val span = 90f
            val first = ((g.scroll * 0.2f) / span).toInt() - 1
            for (i in first..first + (g.worldW / span).toInt() + 3) {
                val x = i * span - g.scroll * 0.2f
                val broken = hash(i * 5 + 1) > 0.78f
                box(Color(0xFF7A6F69), x, HORIZON - 70f, span + 1f, 8f)
                if (!broken) {
                    drawArc(Color(0xFF6B605A), 180f, 180f, false, Offset(x + 6f, HORIZON - 64f), Size(span - 12f, 64f), style = Stroke(9f))
                } else {
                    drawLine(Color(0xFF6B605A), Offset(x + 8f, HORIZON - 62f), Offset(x + 14f, HORIZON - 30f), 8f)
                }
                box(Color(0xFF5F554F), x - 4f, HORIZON - 62f, 9f, 66f)
            }
            fillGround(g, Color(0xFF6D635B), Color(0xFF332D29))
            // Cracked pillars with rebar, hanging cables and fallen slabs.
            scenery(g, 280f, 0.55f, 0.1f) { x, r ->
                val base = RAIL_Y - 4f
                if (r < 0.5f) {
                    box(Color(0xFF8D837B), x, base - 130f, 26f, 130f)
                    drawLine(Color(0xFF3A332E), Offset(x + 4f, base - 100f), Offset(x + 14f, base - 70f), 2f)
                    drawLine(Color(0xFF3A332E), Offset(x + 14f, base - 70f), Offset(x + 8f, base - 40f), 2f)
                    for (k in 0..2) drawLine(Color(0xFF8D5A3A), Offset(x + 4f + k * 8f, base - 130f), Offset(x + 2f + k * 8f + sin(g.time + k) * 2f, base - 150f), 2f)
                } else if (r < 0.8f) {
                    drawLine(Color(0xFF263238), Offset(x, base - 140f), Offset(x + 20f, base - 80f + sin(g.time * 1.2f + r * 8f) * 4f), 2.5f)
                    drawLine(Color(0xFF263238), Offset(x + 20f, base - 80f), Offset(x + 6f, base - 40f), 2.5f)
                } else {
                    path.rewind(); path.moveTo(x, base); path.lineTo(x + 20f, base - 24f); path.lineTo(x + 80f, base - 14f); path.lineTo(x + 90f, base); path.close()
                    drawPath(path, Color(0xFF7A706A))
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 280f, 1.35f, 0.25f) { x, r ->
                drawOval(Color(0xFF3F3833), Offset(x, 670f + r * 30f), Size(48f + r * 36f, 24f))
                drawOval(Color(0xFF584F48), Offset(x + 16f, 662f + r * 30f), Size(26f, 14f))
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 36) { // falling dust and pebbles
                val x = wrap(hash(i) * span - g.time * (10f + hash(i + 2) * 14f), span) - 50f
                val y = wrap(hash(i + 7) * 700f + g.time * (30f + hash(i + 4) * 40f), 700f)
                drawCircle(Color(0xFFBCAAA4).copy(alpha = 0.5f), 1f + hash(i + 6) * 2f, Offset(x, y))
            }
            for (k in 0..1) drawOval(Color.White.copy(alpha = 0.07f), Offset(g.worldW * (0.1f + k * 0.5f) + sin(g.time * 0.2f + k) * 30f, 330f), Size(420f, 40f))
        }
    }

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { tintTrain(g, Color(0xFF8D6E63).copy(alpha = 0.1f), alpha) }
}

internal object FaultLineArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF6A2D2D), Color(0xFFFFAB91))
            drawCircle(Color(0xFFFFCCBC).copy(alpha = 0.8f), 44f, Offset(g.worldW * 0.3f, 190f))
            drawRange(g, HORIZON - 20f, 110f, 0.0052f, 0.07f, SolidColor(Color(0xFF8E3B34)), 0.5f)
            drawRange(g, HORIZON, 70f, 0.0085f, 0.16f, SolidColor(Color(0xFF6B2B27)), 2.1f)
            // Floating rock slabs lifted by the quake.
            scenery(g, 360f, 0.2f, 0.2f) { x, r ->
                val y = 150f + r * 140f + sin(g.time * 0.7f + r * 9f) * 8f
                path.rewind(); path.moveTo(x, y); path.lineTo(x + 50f, y - 10f); path.lineTo(x + 70f, y + 12f); path.lineTo(x + 20f, y + 22f); path.close()
                drawPath(path, Color(0xFF5A2824))
            }
            fillGround(g, Color(0xFF8D4A3A), Color(0xFF3A1F1A))
            // The glowing fissure running across the ground, and spires beside the track.
            scenery(g, 220f, 0.55f, 0f) { x, r ->
                val y = 600f + r * 70f
                val pulse = 0.6f + 0.4f * sin(g.time * 2f + r * 6f)
                var px = x
                var py = y
                for (k in 1..4) {
                    val nx = x + k * 26f
                    val ny = y + (hash((x / 3f).toInt() + k) - 0.5f) * 22f
                    drawLine(Color(0xFFFF6D00).copy(alpha = 0.35f * pulse), Offset(px, py), Offset(nx, ny), 9f, cap = StrokeCap.Round)
                    drawLine(Color(0xFFFFD180).copy(alpha = pulse), Offset(px, py), Offset(nx, ny), 3f, cap = StrokeCap.Round)
                    px = nx
                    py = ny
                }
            }
            scenery(g, 260f, 0.5f, 0.1f) { x, r ->
                val base = RAIL_Y - 4f
                val h = 40f + r * 80f
                path.rewind(); path.moveTo(x, base); path.lineTo(x + 14f, base - h); path.lineTo(x + 30f, base - h * 0.6f); path.lineTo(x + 44f, base); path.close()
                drawPath(path, Color(0xFF5A2824))
                drawLine(Color(0xFF8E3B34), Offset(x + 14f, base - h), Offset(x + 30f, base - h * 0.6f), 2f)
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 300f, 1.35f, 0.3f) { x, r ->
                val y = 690f + r * 20f
                path.rewind(); path.moveTo(x, y); path.lineTo(x + 20f, y - 40f - r * 30f); path.lineTo(x + 46f, y); path.close()
                drawPath(path, Color(0xFF3A1815))
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 32) { // embers rising from the fissure
                val x = wrap(hash(i) * span - g.time * (12f + hash(i + 2) * 12f), span) - 50f
                val y = 720f - wrap(hash(i + 7) * 700f + g.time * (28f + hash(i + 4) * 40f), 700f)
                drawCircle(Color(0xFFFFB74D).copy(alpha = 0.7f), 1.2f + hash(i + 6) * 1.8f, Offset(x, y))
            }
            if (abs(sin(g.time * 0.35f)) > 0.985f) drawRect(Color.White.copy(alpha = 0.12f), Offset(-200f, -1200f), Size(g.worldW + 400f, 2400f)) // aftershock flash
        }
    }

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { tintTrain(g, Color(0xFFBF360C).copy(alpha = 0.1f), alpha) }
}
