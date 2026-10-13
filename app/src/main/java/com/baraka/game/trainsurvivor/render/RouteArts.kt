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
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Route art. To add a route: add an entry to Biome (model/Routes.kt), then write a RouteArt object here and
// register it in registerRouteArts(). Only `backdrop` is required; foreground, weather, lighting and the train's
// route gear (trainKit) are optional. Helpers below keep a route to a few dozen lines.

internal fun registerRouteArts() {
    RouteArtRegistry.register(Biome.MOON, MoonArt)
    RouteArtRegistry.register(Biome.ORBITAL, OrbitalArt)
    RouteArtRegistry.register(Biome.VOLCANO, VolcanoArt)
    RouteArtRegistry.register(Biome.TUNDRA, TundraArt)
    RouteArtRegistry.register(Biome.GHOST_TOWN, GhostTownArt)
    RouteArtRegistry.register(Biome.CARNIVAL, CarnivalArt)
    RouteArtRegistry.register(Biome.SEA_BRIDGE, SeaBridgeArt)
    RouteArtRegistry.register(Biome.JURASSIC, JurassicArt)
    RouteArtRegistry.register(Biome.WILDS, WildsArt)
    RouteArtRegistry.register(Biome.SWITCHYARD, SwitchyardArt)
    RouteArtRegistry.register(Biome.BROKEN_VIADUCT, BrokenViaductArt)
    RouteArtRegistry.register(Biome.FAULT_LINE, FaultLineArt)
    RouteArtRegistry.register(Biome.DEEP_SEA, DeepSeaArt)
    RouteArtRegistry.register(Biome.SOLAR, SolarArt)
    RouteArtRegistry.register(Biome.TWIN_BRIDGE, TwinBridgeArt)
    RouteArtRegistry.register(Biome.ECHO_MINE, EchoMineArt)
}

// ------------------------------------------------------------------ helpers

internal fun DrawScope.fillSky(g: TrainGame, top: Color, bottom: Color) {
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = HORIZON + 30f), Offset(-200f, -1200f), Size(g.worldW + 400f, HORIZON + 1230f))
}

internal fun DrawScope.fillGround(g: TrainGame, top: Color, bottom: Color) {
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = HORIZON, endY = 720f), Offset(-200f, HORIZON), Size(g.worldW + 400f, 800f))
}

private fun DrawScope.stars(g: TrainGame, strength: Float) {
    for (i in 0 until starField.size / 3) {
        val twinkle = 0.6f + 0.4f * sin(g.time * 2f + i)
        drawCircle(Color.White.copy(alpha = (strength * twinkle).coerceIn(0f, 1f)), starField[i * 3 + 2], Offset(starField[i * 3] * g.worldW, starField[i * 3 + 1]))
    }
}

/** Calls [block] with the x position and a 0..1 random value for every scenery item currently on screen. */
internal inline fun scenery(g: TrainGame, spacing: Float, parallax: Float, skip: Float = 0.2f, block: (x: Float, r: Float) -> Unit) {
    val first = ((g.scroll * parallax - 150f) / spacing).toInt() - 1
    val last = ((g.scroll * parallax + g.worldW + 150f) / spacing).toInt() + 1
    for (i in first..last) {
        val r = hash(i * 7 + 3)
        if (r < skip) continue
        block(i * spacing + r * 100f - g.scroll * parallax, r)
    }
}

/** A translucent tint over the whole train (soot, dust, sea spray). */
internal fun DrawScope.tintTrain(g: TrainGame, color: Color, alpha: Float) {
    drawRect(color.copy(alpha = color.alpha * alpha), Offset(g.rearEnd, 384f), Size(g.frontEnd - g.rearEnd, 120f))
}

internal fun wrap(value: Float, span: Float): Float = ((value % span) + span) % span

// ------------------------------------------------------------------ Lunar Rails

/** Domes, thrusters and a magnetic strip: the gear the train wears in space. */
private fun DrawScope.drawSpaceKit(g: TrainGame, alpha: Float) {
    if (alpha < 0.02f) return
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
    val tx = g.rearEnd - 4f
    box(Color(0xFF37474F).copy(alpha = alpha), tx - 12f, 466f, 14f, 24f)
    val flame = 38f + 10f * sin(g.time * 40f)
    path.rewind(); path.moveTo(tx - 12f, 470f); path.lineTo(tx - 12f - flame, 478f); path.lineTo(tx - 12f, 486f); path.close()
    drawPath(path, Color(0xFF40C4FF).copy(alpha = 0.8f * alpha))
    path.rewind(); path.moveTo(tx - 12f, 473f); path.lineTo(tx - 12f - flame * 0.55f, 478f); path.lineTo(tx - 12f, 483f); path.close()
    drawPath(path, Color.White.copy(alpha = 0.9f * alpha))
    drawLine(Color(0xFF00E5FF).copy(alpha = (0.5f + 0.3f * sin(g.time * 5f)) * alpha), Offset(g.rearEnd, 506f), Offset(g.frontEnd, 506f), 3f)
}

private object MoonArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF02030A), Color(0xFF0B0E1E))
            stars(g, 1f)
            val ex = g.worldW * 0.8f
            val ey = 150f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF80B8FF).copy(alpha = 0.35f), Color.Transparent), center = Offset(ex, ey), radius = 100f), 100f, Offset(ex, ey))
            drawCircle(Color(0xFF2B6CDB), 62f, Offset(ex, ey))
            drawOval(Color(0xFF3C9A4A), Offset(ex - 40f, ey - 30f), Size(34f, 26f))
            drawOval(Color(0xFF3C9A4A), Offset(ex + 4f, ey + 6f), Size(30f, 34f))
            drawOval(Color.White.copy(alpha = 0.55f), Offset(ex - 30f, ey - 6f), Size(50f, 10f))
            drawOval(Color.White.copy(alpha = 0.45f), Offset(ex - 10f, ey + 30f), Size(44f, 9f))
            drawCircle(Color(0xFF02030A).copy(alpha = 0.5f), 62f, Offset(ex + 22f, ey - 8f))
            drawRange(g, HORIZON - 4f, 150f, 0.0052f, 0.07f, SolidColor(Color(0xFF58606E)), 0.8f)
            drawRange(g, HORIZON + 8f, 90f, 0.0085f, 0.16f, SolidColor(Color(0xFF3C424E)), 3.1f)
            fillGround(g, Color(0xFF747A86), Color(0xFF383C46))
            scenery(g, 260f, 0.55f) { x, r ->
                val h = 14f + r * 22f
                drawOval(Color(0xFF59606C), Offset(x - h, RAIL_Y - 4f - h * 0.8f), Size(h * 2f, h * 1.1f))
                drawOval(Color(0xFF8A919D), Offset(x - h * 0.6f, RAIL_Y - 4f - h * 0.8f), Size(h * 0.9f, h * 0.5f))
                if (r > 0.75f) {
                    drawLine(Color(0xFFCFD8DC), Offset(x + h + 10f, RAIL_Y - 4f), Offset(x + h + 10f, RAIL_Y - 60f), 2f)
                    box(Color(0xFFE53935), x + h + 10f, RAIL_Y - 60f, 22f, 14f)
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 330f, 1.35f, 0.3f) { x, r ->
                val y = 650f + r * 50f
                drawOval(Color(0xFF2C3038), Offset(x, y), Size(70f + r * 40f, 20f + r * 8f))
                drawOval(Color(0xFF9097A3).copy(alpha = 0.7f), Offset(x, y), Size(70f + r * 40f, 20f + r * 8f), style = Stroke(2f))
            }
        }
    }

    override fun darkness(night: Float) = 0.12f
    override fun glowNight(night: Float) = 0.8f
    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { drawSpaceKit(g, alpha) }
}

// ------------------------------------------------------------------ Orbital Station

private object OrbitalArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF04040F), Color(0xFF150B30))
            stars(g, 1f)
            drawOval(Color(0xFF7C4DFF).copy(alpha = 0.10f), Offset(g.worldW * 0.4f, 60f), Size(500f, 160f))
            drawOval(Color(0xFF00BCD4).copy(alpha = 0.07f), Offset(g.worldW * 0.6f, 140f), Size(420f, 120f))
            val cx = g.worldW * 0.2f
            val cy = 200f
            drawCircle(Color(0xFF5C6BC0), 110f, Offset(cx, cy))
            drawOval(Color(0xFF3949AB).copy(alpha = 0.6f), Offset(cx - 100f, cy - 34f), Size(200f, 22f))
            drawOval(Color(0xFF7986CB).copy(alpha = 0.5f), Offset(cx - 106f, cy + 6f), Size(212f, 18f))
            drawOval(Color(0xFF3949AB).copy(alpha = 0.5f), Offset(cx - 80f, cy + 46f), Size(160f, 16f))
            drawCircle(Color(0xFF04040F).copy(alpha = 0.4f), 110f, Offset(cx + 36f, cy - 14f))
            withTransform({ rotate(-16f, Offset(cx, cy)) }) {
                drawOval(Color(0xFFB39DDB).copy(alpha = 0.55f), Offset(cx - 170f, cy - 26f), Size(340f, 52f), style = Stroke(7f))
            }
            fillGround(g, Color(0xFF3F4654), Color(0xFF1A1E27))
            var x = -wrap(g.scroll, 60f) - 60f
            var flip = false
            while (x < g.worldW + 60f) {
                box(if (flip) Color(0xFF212121) else Color(0xFFFFC107), x, HORIZON, 30f, 14f)
                flip = !flip
                x += 30f
            }
            x = -wrap(g.scroll, 90f) - 90f
            while (x < g.worldW + 90f) {
                box(Color.Black.copy(alpha = 0.18f), x, HORIZON + 14f, 2f, 260f)
                x += 90f
            }
            scenery(g, 300f, 0.55f) { px, r ->
                val h = 50f + r * 40f
                rbox(Color(0xFF59606E), px - 16f, RAIL_Y - 4f - h, 32f, h, 8f)
                drawLine(Color(0xFF90A4AE), Offset(px, RAIL_Y - 4f - h), Offset(px, RAIL_Y - 4f - h - 22f), 2f)
                drawCircle(Color(0xFFFF1744).copy(alpha = 0.4f + 0.6f * abs(sin(g.time * 3f + r * 6f))), 3f, Offset(px, RAIL_Y - 4f - h - 24f))
                box(Color(0xFF2A3F6B), px + 16f, RAIL_Y - 4f - h * 0.7f, 54f, 26f)
                drawLine(Color(0xFF5C7CC0), Offset(px + 43f, RAIL_Y - 4f - h * 0.7f), Offset(px + 43f, RAIL_Y - 4f - h * 0.7f + 26f), 1.5f)
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 130f, 1.35f, 0.15f) { x, _ -> box(Color(0xFF00E5FF).copy(alpha = 0.7f), x, 690f, 46f, 4f) }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 200f
            for (i in 0 until 18) {
                val x = wrap(hash(i) * span - g.time * (14f + hash(i + 2) * 20f), span) - 100f
                val y = 60f + hash(i + 9) * 600f + sin(g.time * 0.6f + i) * 10f
                drawCircle(Color(0xFFB0BEC5).copy(alpha = 0.55f), 1.2f + hash(i + 4) * 2f, Offset(x, y))
            }
        }
    }

    override fun darkness(night: Float) = 0.1f
    override fun glowNight(night: Float) = 0.8f
    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { drawSpaceKit(g, alpha) }
}

// ------------------------------------------------------------------ Volcano Line

private object VolcanoArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF1A0707), Color(0xFFE0541A))
            scenery(g, 1100f, 0.08f, 0f) { x, _ ->
                path.rewind(); path.moveTo(x - 260f, HORIZON); path.lineTo(x - 40f, 190f); path.lineTo(x + 40f, 190f); path.lineTo(x + 260f, HORIZON); path.close()
                drawPath(path, Color(0xFF2A1210))
                drawOval(Color(0xFFFF6D00), Offset(x - 40f, 184f), Size(80f, 16f))
                drawLine(Color(0xFFFF6D00), Offset(x - 20f, 196f), Offset(x - 70f, 300f), 4f)
                drawLine(Color(0xFFFF8F00), Offset(x + 18f, 196f), Offset(x + 60f, 330f), 3f)
                for (k in 0..2) drawCircle(Color(0xFF3A2B28).copy(alpha = 0.5f - k * 0.12f), 36f + k * 14f, Offset(x + k * 26f + sin(g.time * 0.4f + k) * 10f, 160f - k * 38f))
            }
            drawRange(g, HORIZON - 6f, 140f, 0.005f, 0.07f, SolidColor(Color(0xFF2A1512)), 0.5f)
            drawRange(g, HORIZON + 6f, 80f, 0.0085f, 0.16f, SolidColor(Color(0xFF1E0E0C)), 2.2f)
            fillGround(g, Color(0xFF2B2220), Color(0xFF140E0D))
            scenery(g, 220f, 1.0f, 0.1f) { x, r ->
                val y0 = 540f + r * 140f
                val pulse = 0.6f + 0.4f * sin(g.time * 2f + r * 6f)
                drawLine(Color(0xFFFF6D00).copy(alpha = 0.35f * pulse), Offset(x, y0), Offset(x + 60f, y0 + 14f), 7f)
                drawLine(Color(0xFFFFAB00).copy(alpha = pulse), Offset(x, y0), Offset(x + 60f, y0 + 14f), 3f)
                drawLine(Color(0xFFFFAB00).copy(alpha = pulse), Offset(x + 60f, y0 + 14f), Offset(x + 92f, y0 - 4f), 3f)
            }
            scenery(g, 250f, 0.55f) { x, r ->
                val h = 40f + r * 50f
                if (r < 0.6f) {
                    drawLine(Color(0xFF0B0807), Offset(x, RAIL_Y - 4f), Offset(x, RAIL_Y - 4f - h), 4f)
                    drawLine(Color(0xFF0B0807), Offset(x, RAIL_Y - 4f - h * 0.6f), Offset(x - 16f, RAIL_Y - 4f - h * 0.9f), 3f)
                } else {
                    path.rewind(); path.moveTo(x - 14f, RAIL_Y - 4f); path.lineTo(x, RAIL_Y - 4f - h); path.lineTo(x + 14f, RAIL_Y - 4f); path.close()
                    drawPath(path, Color(0xFF1B1210))
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 340f, 1.35f, 0.3f) { x, r ->
                val y = 650f + r * 40f
                drawOval(Color(0xFFFF5722).copy(alpha = 0.9f), Offset(x, y), Size(80f + r * 50f, 20f))
                drawOval(Color(0xFFFFC107).copy(alpha = 0.8f), Offset(x + 12f, y + 4f), Size(50f + r * 30f, 10f))
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 36) {
                val x = wrap(hash(i) * span + sin(g.time + i) * 18f, span) - 50f
                val y = 740f - wrap(hash(i + 5) * 760f + g.time * (40f + hash(i + 3) * 40f), 760f)
                drawCircle(Color(0xFFFF9800).copy(alpha = 0.8f), 1.4f + hash(i + 8) * 1.5f, Offset(x, y))
            }
        }
    }

    override fun darkness(night: Float) = night * 0.25f

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) {
        with(s) {
            if (alpha < 0.02f) return
            val pulse = 0.6f + 0.4f * sin(g.time * 4f)
            drawRect(Color(0xFFFF6D00).copy(alpha = 0.4f * alpha * pulse), Offset(g.rearEnd, 490f), Size(g.frontEnd - g.rearEnd, 12f))
            tintTrain(g, Color(0xFF1B1210).copy(alpha = 0.18f), alpha)
            for (k in 0 until 8) {
                val x = g.rearEnd + hash(k) * (g.frontEnd - g.rearEnd)
                val y = 420f - wrap(g.time * 40f + k * 53f, 70f)
                drawCircle(Color(0xFFFF9800).copy(alpha = 0.7f * alpha), 1.6f, Offset(x, y))
            }
        }
    }
}

// ------------------------------------------------------------------ Frozen Tundra

private object TundraArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF9FB6C8), Color(0xFFE8F1F7))
            val farBase = HORIZON - 8f
            val nearBase = HORIZON + 6f
            drawRange(g, farBase, 230f, 0.0042f, 0.06f, rangeBrush(farBase, 230f, Color(0xFF9DB7CC), true, light), 0f)
            drawRange(g, nearBase, 150f, 0.0068f, 0.14f, rangeBrush(nearBase, 150f, Color(0xFF7E9AB0), true, light), 2.4f)
            fillGround(g, Color(0xFFEAF2F8), Color(0xFFB9CBDB))
            scenery(g, 230f, 0.55f) { x, r ->
                val h = 46f + r * 44f
                if (r < 0.7f) {
                    for (k in 0..2) {
                        val y0 = RAIL_Y - 4f - k * h * 0.28f
                        path.rewind(); path.moveTo(x - 20f + k * 4f, y0); path.lineTo(x, y0 - h * 0.45f); path.lineTo(x + 20f - k * 4f, y0); path.close()
                        drawPath(path, Color(0xFF6F96A8))
                        path.rewind(); path.moveTo(x - 9f + k * 2f, y0 - h * 0.2f); path.lineTo(x, y0 - h * 0.45f); path.lineTo(x + 9f - k * 2f, y0 - h * 0.2f); path.close()
                        drawPath(path, Color.White)
                    }
                } else {
                    for (k in -1..1) {
                        path.rewind(); path.moveTo(x + k * 12f - 6f, RAIL_Y - 4f); path.lineTo(x + k * 12f, RAIL_Y - 4f - h * (0.7f - abs(k) * 0.25f)); path.lineTo(x + k * 12f + 6f, RAIL_Y - 4f); path.close()
                        drawPath(path, Color(0xFFA5D8F0).copy(alpha = 0.9f))
                    }
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 300f, 1.35f, 0.25f) { x, r ->
                val y = 650f + r * 40f
                drawOval(Color(0xFFBEE3F5).copy(alpha = 0.8f), Offset(x, y), Size(110f, 24f))
                drawLine(Color.White.copy(alpha = 0.7f), Offset(x + 18f, y + 8f), Offset(x + 70f, y + 6f), 2f)
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 300f
            for (i in 0 until 150) {
                val x = wrap(hash(i) * span - g.time * (420f + hash(i + 3) * 260f), span) - 100f
                val y = wrap(hash(i + 9) * 740f + g.time * 60f, 740f) - 20f
                drawLine(Color.White.copy(alpha = 0.7f), Offset(x, y), Offset(x + 26f, y + 10f), 1.6f)
            }
            drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), startX = 0f, endX = 170f), Offset(0f, 0f), Size(170f, 720f))
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.4f)), startX = g.worldW - 170f, endX = g.worldW), Offset(g.worldW - 170f, 0f), Size(170f, 720f))
            drawRect(Color.White.copy(alpha = 0.1f), Offset(-200f, -1200f), Size(g.worldW + 400f, 2400f))
        }
    }

    override fun darkness(night: Float) = night * 0.45f

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) {
        with(s) {
            if (alpha < 0.02f) return
            val snow = Color.White.copy(alpha = 0.92f * alpha)
            rbox(snow, g.locoLeft + g.shape.locoGunX - 36f, g.shape.locoRoofY - 8f, 80f, 8f, 4f)
            for (i in 1 until g.slotCount) if (g.carAlive(i)) rbox(snow, g.carLeft(i) - 2f, 414f, g.shape.carW + 4f, 7f, 3f)
            for (k in 0 until 10) { // icicles under the roof edge
                val x = g.rearEnd + 8f + k * ((g.frontEnd - g.rearEnd - 40f) / 10f)
                path.rewind(); path.moveTo(x, 424f); path.lineTo(x + 3f, 424f + 6f + (k % 3) * 4f); path.lineTo(x + 6f, 424f); path.close()
                drawPath(path, Color(0xFFB3E5FC).copy(alpha = 0.9f * alpha))
            }
            path.rewind() // snow plough on the nose
            path.moveTo(g.frontEnd - 10f, 460f); path.lineTo(g.frontEnd + 22f, 498f); path.lineTo(g.frontEnd - 10f, 498f); path.close()
            drawPath(path, Color(0xFFE3F2FD).copy(alpha = alpha))
        }
    }
}

// ------------------------------------------------------------------ Ghost Town

private object GhostTownArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF5E4B6B), Color(0xFFFFB067))
            val sx = g.worldW * 0.7f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD08A).copy(alpha = 0.6f), Color.Transparent), center = Offset(sx, HORIZON - 40f), radius = 130f), 130f, Offset(sx, HORIZON - 40f))
            drawCircle(Color(0xFFFFE0A0), 44f, Offset(sx, HORIZON - 40f))
            drawRange(g, HORIZON - 4f, 130f, 0.0045f, 0.07f, SolidColor(Color(0xFF7B4B3A)), 0.3f)
            drawRange(g, HORIZON + 6f, 80f, 0.0075f, 0.15f, SolidColor(Color(0xFF5E3A2E)), 2.2f)
            fillGround(g, Color(0xFFC9975A), Color(0xFF8A5E34))
            scenery(g, 330f, 0.55f, 0.15f) { x, r ->
                val base = RAIL_Y - 4f
                if (r < 0.3f) {
                    drawLine(Color(0xFF3E2723), Offset(x, base), Offset(x, base - 70f), 4f)
                    drawLine(Color(0xFF3E2723), Offset(x, base - 40f), Offset(x - 16f, base - 62f), 3f)
                } else {
                    val w = 90f + r * 50f
                    val h = 62f + r * 40f
                    box(Color(0xFF6D4B33), x, base - h, w, h)
                    box(Color(0xFF4E3524), x - 4f, base - h - 14f, w + 8f, 16f)
                    box(Color(0xFF3B2A1C), x + w * 0.4f, base - 36f, 16f, 36f)
                    box(if (r > 0.7f) Color(0xFFFFD27F) else Color(0xFF1C1410), x + 10f, base - h + 12f, 18f, 18f)
                    box(Color(0xFF1C1410), x + w - 28f, base - h + 12f, 18f, 18f)
                    if (r > 0.55f) outlinedText("SALOON", x + w / 2f, base - h - 2f, 12f, 0xFFFFE0A0.toInt())
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 300f, 1.35f, 0.3f) { x, r ->
                val y = 650f + r * 40f
                if (r > 0.6f) drawOval(Color(0xFF7A5A3A), Offset(x, y), Size(50f + r * 20f, 22f))
                else for (k in 0..4) drawLine(Color(0xFF6B5A2E), Offset(x + k * 5f, y + 14f), Offset(x + k * 7f - 8f, y - 8f - (k % 3) * 5f), 2.5f)
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 300f
            for (k in 0..2) { // tumbleweeds
                val x = wrap(k * 520f - g.time * (110f + k * 25f), span) - 100f
                val y = 640f + k * 22f - abs(sin(g.time * 3f + k)) * 12f
                withTransform({ rotate(g.time * 220f, Offset(x, y)) }) {
                    drawCircle(Color(0xFF8D6E4A), 14f, Offset(x, y), style = Stroke(2f))
                    drawLine(Color(0xFF8D6E4A), Offset(x - 14f, y), Offset(x + 14f, y), 2f)
                    drawLine(Color(0xFF8D6E4A), Offset(x, y - 14f), Offset(x, y + 14f), 2f)
                }
            }
            for (i in 0 until 12) {
                val x = wrap(hash(i) * span - g.time * (300f + hash(i + 2) * 200f), span) - 100f
                val y = 540f + hash(i + 7) * 170f
                drawLine(Color(0xFFE8CFA0).copy(alpha = 0.3f), Offset(x, y), Offset(x + 40f, y), 1.8f)
            }
        }
    }

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { tintTrain(g, Color(0xFFD7B27A).copy(alpha = 0.16f), alpha) }
}

// ------------------------------------------------------------------ Carnival of Horrors

private val bulbs = intArrayOf(0xFFFF4081.toInt(), 0xFFFFEB3B.toInt(), 0xFF40C4FF.toInt(), 0xFF69F0AE.toInt(), 0xFFFF9100.toInt())

private object CarnivalArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF3B1E6E), Color(0xFFFF8AD8))
            var x = -20f
            var k = 0
            while (x < g.worldW + 40f) {
                val y = 62f + sin(x * 0.02f) * 9f
                drawCircle(Color(bulbs[(k + (g.time * 2f).toInt()) % bulbs.size]), 4.5f, Offset(x, y))
                x += 38f
                k++
            }
            drawRange(g, HORIZON - 4f, 90f, 0.0045f, 0.07f, SolidColor(Color(0xFF6A3FA0)), 0.4f)
            drawRange(g, HORIZON + 6f, 60f, 0.0075f, 0.15f, SolidColor(Color(0xFF4F2E80)), 2.0f)
            fillGround(g, Color(0xFFF8BBD0), Color(0xFFAD5A8A))
            x = -wrap(g.scroll, 120f) - 120f
            while (x < g.worldW + 120f) {
                box(Color.White.copy(alpha = 0.12f), x, HORIZON, 60f, 250f)
                x += 120f
            }
            scenery(g, 360f, 0.55f, 0.1f) { px, r ->
                val base = RAIL_Y - 4f
                if (r < 0.3f) { // ferris wheel
                    val cy = base - 100f
                    drawCircle(Color(0xFFFFD54F), 78f, Offset(px, cy), style = Stroke(3f))
                    for (a in 0 until 8) {
                        val ang = g.time * 0.4f + a * (PI.toFloat() / 4f)
                        drawLine(Color(0xFFFFD54F), Offset(px, cy), Offset(px + cos(ang) * 78f, cy + sin(ang) * 78f), 2f)
                        drawCircle(Color(bulbs[a % bulbs.size]), 7f, Offset(px + cos(ang) * 78f, cy + sin(ang) * 78f))
                    }
                    drawLine(Color(0xFF8D6E63), Offset(px, cy), Offset(px - 40f, base), 5f)
                    drawLine(Color(0xFF8D6E63), Offset(px, cy), Offset(px + 40f, base), 5f)
                } else if (r < 0.65f) { // big top
                    val w = 120f
                    for (stripe in 0 until 6) box(if (stripe % 2 == 0) Color(0xFFE53935) else Color.White, px + stripe * (w / 6f), base - 56f, w / 6f, 56f)
                    path.rewind(); path.moveTo(px - 8f, base - 56f); path.lineTo(px + w / 2f, base - 120f); path.lineTo(px + w + 8f, base - 56f); path.close()
                    drawPath(path, Color(0xFF7E57C2))
                    drawLine(Color(0xFF5D4037), Offset(px + w / 2f, base - 120f), Offset(px + w / 2f, base - 140f), 2f)
                    box(Color(0xFFFFEB3B), px + w / 2f, base - 140f, 18f, 10f)
                } else { // balloons tied to a stake
                    drawLine(Color(0xFF6D4C41), Offset(px, base), Offset(px, base - 18f), 4f)
                    for (b in -1..1) {
                        val bx = px + b * 18f + sin(g.time + b) * 4f
                        drawLine(Color.White.copy(alpha = 0.8f), Offset(px, base - 18f), Offset(bx, base - 76f), 1.2f)
                        drawOval(Color(bulbs[(b + 3) % bulbs.size]), Offset(bx - 10f, base - 96f), Size(20f, 26f))
                    }
                }
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            scenery(g, 280f, 1.35f, 0.25f) { x, r ->
                val y = 655f + r * 35f
                if (r > 0.55f) {
                    drawCircle(Color(bulbs[(r * 5f).toInt() % bulbs.size]), 14f, Offset(x, y))
                    drawCircle(Color.White.copy(alpha = 0.5f), 4f, Offset(x - 4f, y - 5f))
                } else {
                    box(Color.White, x, y - 10f, 22f, 22f)
                    for (k in 0..2) box(Color(0xFFE53935), x + k * 8f, y - 10f, 4f, 22f)
                }
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 100f
            for (i in 0 until 60) {
                val x = wrap(hash(i) * span + sin(g.time * 1.5f + i) * 16f, span) - 50f
                val y = wrap(hash(i + 3) * 740f + g.time * (40f + hash(i + 8) * 30f), 740f) - 20f
                box(Color(bulbs[i % bulbs.size]), x, y, 5f, 3f)
            }
            for (k in 0..2) { // balloons drifting up
                val bx = wrap(k * 410f + 120f, g.worldW)
                val by = 760f - wrap(g.time * 28f + k * 260f, 820f)
                drawLine(Color.White.copy(alpha = 0.7f), Offset(bx, by + 14f), Offset(bx + sin(g.time + k) * 6f, by + 60f), 1.2f)
                drawOval(Color(bulbs[(k + 1) % bulbs.size]), Offset(bx - 10f, by - 14f), Size(20f, 28f))
            }
        }
    }

    override fun darkness(night: Float) = night * 0.35f

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) {
        with(s) {
            if (alpha < 0.02f) return
            var x = g.rearEnd + 6f
            var k = 0
            while (x < g.frontEnd - 36f) { // bunting along the roofs
                val y = 408f + sin((x - g.rearEnd) * 0.05f) * 4f
                path.rewind(); path.moveTo(x, y); path.lineTo(x + 12f, y); path.lineTo(x + 6f, y + 12f); path.close()
                drawPath(path, Color(bulbs[k % bulbs.size]).copy(alpha = alpha))
                x += 16f
                k++
            }
            val bx = g.locoLeft + g.shape.stackX
            for (b in -1..1) { // balloons tied to the stack
                val by = 330f + sin(g.time * 2f + b) * 6f
                drawLine(Color.White.copy(alpha = 0.7f * alpha), Offset(bx, g.shape.locoRoofY - 8f), Offset(bx + b * 14f, by + 12f), 1.2f)
                drawOval(Color(bulbs[(b + 3) % bulbs.size]).copy(alpha = alpha), Offset(bx + b * 14f - 9f, by - 14f), Size(18f, 26f))
            }
        }
    }
}

// ------------------------------------------------------------------ Sea Bridge

private object SeaBridgeArt : RouteArt {
    override fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float) {
        with(s) {
            fillSky(g, Color(0xFF4FA3E0), Color(0xFFCDEBFA))
            for (i in 0 until 4) {
                val span = g.worldW + 400f
                val x = wrap(i * 470f - g.scroll * 0.04f * (1 + i % 3), span) - 200f
                val y = 70f + (i * 53 % 130)
                drawOval(Color.White.copy(alpha = 0.8f), Offset(x, y), Size(120f, 30f))
                drawOval(Color.White.copy(alpha = 0.8f), Offset(x + 30f, y - 16f), Size(80f, 34f))
            }
            drawRange(g, HORIZON - 2f, 50f, 0.006f, 0.05f, SolidColor(Color(0xFF5E8F7A)), 1f)
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1E88B5), Color(0xFF0B3C5D)), startY = HORIZON, endY = 720f), Offset(-200f, HORIZON - 2f), Size(g.worldW + 400f, 800f))
            val span = g.worldW + 300f
            for (row in 0 until 9) {
                val y = HORIZON + 14f + row * 22f
                for (j in 0 until 7) {
                    val x = wrap(j * 210f + row * 57f - g.scroll * (0.25f + row * 0.05f), span) - 120f
                    drawOval(Color.White.copy(alpha = 0.16f), Offset(x, y), Size(54f + row * 5f, 5f))
                }
            }
            scenery(g, 700f, 1.0f, 0f) { x, _ ->
                box(Color(0xFF59606E), x, 90f, 20f, RAIL_Y - 76f)
                box(Color(0xFF2E343D), x - 6f, 90f, 32f, 8f)
                drawLine(Color(0xFFB0BEC5), Offset(x + 10f, 98f), Offset(x - 360f, RAIL_Y + 14f), 2f)
                drawLine(Color(0xFFB0BEC5), Offset(x + 10f, 98f), Offset(x + 380f, RAIL_Y + 14f), 2f)
                drawLine(Color(0xFFB0BEC5), Offset(x + 10f, 98f), Offset(x - 180f, RAIL_Y + 14f), 1.5f)
                drawLine(Color(0xFFB0BEC5), Offset(x + 10f, 98f), Offset(x + 190f, RAIL_Y + 14f), 1.5f)
            }
            box(Color(0xFF454B55), -200f, RAIL_Y + 14f, g.worldW + 400f, 62f)
            box(Color(0xFF5C6370), -200f, RAIL_Y + 14f, g.worldW + 400f, 4f)
            var x = -wrap(g.scroll, 60f) - 60f
            while (x < g.worldW + 60f) {
                box(Color(0xFF2E343D), x, RAIL_Y + 20f, 6f, 54f)
                drawLine(Color(0xFF2E343D), Offset(x, RAIL_Y + 20f), Offset(x + 60f, RAIL_Y + 74f), 2f)
                x += 60f
            }
        }
    }

    override fun foreground(s: DrawScope, g: TrainGame, light: Float) {
        with(s) {
            box(Color(0xFF222831), -200f, 572f, g.worldW + 400f, 4f)
            var x = -wrap(g.scroll * 1.35f, 80f) - 80f
            while (x < g.worldW + 80f) {
                box(Color(0xFF222831), x, 554f, 4f, 22f)
                x += 80f
            }
        }
    }

    override fun weather(s: DrawScope, g: TrainGame) {
        with(s) {
            val span = g.worldW + 300f
            for (k in 0..3) { // gulls
                val x = wrap(k * 350f - g.time * 60f, span) - 100f
                val y = 120f + k * 35f + sin(g.time * 2f + k) * 10f
                val flap = sin(g.time * 8f + k) * 5f
                drawLine(Color.White, Offset(x - 9f, y - flap), Offset(x, y), 2f)
                drawLine(Color.White, Offset(x, y), Offset(x + 9f, y - flap), 2f)
            }
        }
    }

    override fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) = with(s) { tintTrain(g, Color(0xFF26C6DA).copy(alpha = 0.1f), alpha) }
}
