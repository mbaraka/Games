package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.sin

// The two co-op-only routes (registered in registerRouteArts in RouteArts.kt). Both leave a clear band above the
// horizon for the partner's ghost train (see GhostTrain.kt), so the scenery there stays light.

/** Twin Bridge: a steel suspension bridge over a misty gorge at dusk. */
internal object TwinBridgeArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF1F4E6E), Color(0xFFF2A488))
            drawCircle(Color(0xFFFFE0B2).copy(alpha = 0.9f), 52f, Offset(g.worldW * 0.72f, 150f))
            drawCircle(Color(0xFFFFE0B2).copy(alpha = 0.2f), 100f, Offset(g.worldW * 0.72f, 150f))
            drawRange(g, HORIZON - 40f, 90f, 0.0045f, 0.05f, SolidColor(Color(0xFF3B5B78)), 0.4f)
            drawRange(g, HORIZON - 6f, 60f, 0.007f, 0.12f, SolidColor(Color(0xFF2B4560)), 1.9f)
            // Mist in the gorge
            val span = g.worldW + 400f
            for (i in 0 until 5) drawOval(Color.White.copy(alpha = 0.10f), Offset(wrap(i * 320f - g.time * 14f, span) - 200f, HORIZON - 18f + i * 5f), Size(380f, 20f))
            fillGround(g, Color(0xFF546E7A), Color(0xFF263238))
            // Bridge towers with cables: the two tracks run between them
            scenery(g, 520f, 0.5f, 0.0f) { x, _ ->
                val base = HORIZON + 6f
                box(Color(0xFF37474F), x - 9f, base - 250f, 18f, 250f)
                box(Color(0xFF37474F), x + 70f, base - 250f, 18f, 250f)
                box(Color(0xFF455A64), x - 9f, base - 250f, 106f, 14f)
                box(Color(0xFF455A64), x - 9f, base - 150f, 106f, 10f)
                drawLine(Color(0xFF90A4AE), Offset(x - 9f, base - 242f), Offset(x - 520f * 0.5f, base - 60f), 2f)
                drawLine(Color(0xFF90A4AE), Offset(x + 88f, base - 242f), Offset(x + 88f + 520f * 0.5f, base - 60f), 2f)
                for (k in 0..4) drawLine(Color(0xFF90A4AE).copy(alpha = 0.6f), Offset(x - 9f + k * 22f, base - 140f), Offset(x + 13f + k * 22f, base - 250f), 2f)
                drawCircle(Color(0xFFFF5252).copy(alpha = 0.5f + 0.5f * sin(g.time * 3f + x)), 4f, Offset(x + 40f, base - 258f))
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 260f, 1.3f, 0.2f) { x, _ ->
                box(Color(0xFF263238), x, 660f, 10f, 80f)
                box(Color(0xFF263238), x - 6f, 654f, 22f, 8f)
            }
        }
    }

    override fun darkness(night: Float) = night * 0.35f
}

/** Echo Mine: timbered shafts and glowing crystals; the partner's track runs through the next tunnel over. */
internal object EchoMineArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF120A24), Color(0xFF3A1F5C)) // the cavern roof
            // Hanging stalactites
            scenery(g, 120f, 0.2f, 0.1f) { x, r ->
                path.rewind(); path.moveTo(x - 24f, -10f); path.lineTo(x + 24f, -10f); path.lineTo(x + 2f, 60f + r * 110f); path.close()
                drawPath(path, Color(0xFF241338))
            }
            drawRange(g, HORIZON - 30f, 80f, 0.006f, 0.1f, SolidColor(Color(0xFF2A1745)), 0.8f)
            drawRange(g, HORIZON, 50f, 0.009f, 0.18f, SolidColor(Color(0xFF1D0F33)), 2.2f)
            fillGround(g, Color(0xFF4A3B5C), Color(0xFF1A1226))
            // Glowing crystals that pulse in time
            scenery(g, 210f, 0.45f, 0.1f) { x, r ->
                val base = HORIZON + 4f
                val pulse = 0.55f + 0.45f * sin(g.time * 2f + r * 9f)
                val c = if (r < 0.5f) Color(0xFFCE93D8) else Color(0xFF80DEEA)
                for (k in -1..1) {
                    path.rewind(); path.moveTo(x + k * 14f - 7f, base); path.lineTo(x + k * 14f, base - 60f - r * 40f + k * 8f); path.lineTo(x + k * 14f + 7f, base); path.close()
                    drawPath(path, c.copy(alpha = 0.35f + 0.4f * pulse))
                }
                drawCircle(c.copy(alpha = 0.10f * pulse), 60f, Offset(x, base - 40f))
            }
            // Wooden support frames
            scenery(g, 300f, 0.8f, 0.15f) { x, _ ->
                val base = RAIL_Y - 4f
                box(Color(0xFF5D4037), x - 40f, base - 120f, 10f, 120f)
                box(Color(0xFF5D4037), x + 30f, base - 120f, 10f, 120f)
                box(Color(0xFF6D4C41), x - 46f, base - 126f, 96f, 10f)
                box(Color(0xFFFFB74D).copy(alpha = 0.7f), x - 3f, base - 112f, 6f, 8f) // hanging lamp
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 18) { // drifting crystal dust
                val x = wrap(hash(i) * span + sin(g.time * 0.6f + i) * 20f, span) - 50f
                val y = wrap(hash(i + 7) * 700f - g.time * (10f + hash(i + 2) * 12f), 700f)
                drawCircle(Color(0xFFE1BEE7).copy(alpha = 0.6f), 1.5f, Offset(x, y))
            }
        }
    }

    override fun darkness(night: Float) = 0.35f + night * 0.2f
    override fun glowNight(night: Float) = 1f
}
