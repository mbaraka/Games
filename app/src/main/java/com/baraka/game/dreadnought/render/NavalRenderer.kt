package com.baraka.game.dreadnought.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.baraka.game.dreadnought.data.DreadSettings
import com.baraka.game.dreadnought.model.Theatre
import com.baraka.game.dreadnought.systems.NavalGame
import com.baraka.game.dreadnought.systems.P_BLAST
import com.baraka.game.dreadnought.systems.P_BUBBLE
import com.baraka.game.dreadnought.systems.P_DEBRIS
import com.baraka.game.dreadnought.systems.P_FLASH
import com.baraka.game.dreadnought.systems.P_SMOKE
import com.baraka.game.dreadnought.systems.P_SPARK
import com.baraka.game.dreadnought.systems.P_SPLASH
import com.baraka.game.dreadnought.systems.P_WAKE
import com.baraka.game.dreadnought.systems.PHASE_BRIEF
import com.baraka.game.dreadnought.systems.PHASE_CLEARED
import com.baraka.game.dreadnought.systems.Ship
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Where the sea and the HUD are painted from. Everything here is read-only over [NavalGame]. */
object HudColors {
    val Good = Color(0xFF66BB6A)
    val Warn = Color(0xFFFFB74D)
    val Bad = Color(0xFFEF5350)
    val Panel = Color(0xAA06121C)
    val Text = Color(0xFFEAF2F8)
}

private const val DEG = 57.29578f
private const val VIEW_WIDTH_M = 1500f
private const val VIEW_HEIGHT_M = 560f

/**
 * Draws the scene: ocean, wakes, ships, shells, torpedoes, particles, aim marks and the HUD. Allocation-free per
 * frame: paints and paths are created once, HUD text is rebuilt only when its value changes.
 */
class NavalRenderer {
    /** Pixels per metre, set every frame; the screen uses it to turn a touch into a world point. */
    var scale = 1f
    var camX = 0f
    var camY = 0f
    var halfW = 1f
    var halfH = 1f
    var density = 1f

    private val text = Paint().apply { isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
    private val layer = androidx.compose.ui.graphics.Paint()
    private val arrow = Path()
    private val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))

    // cached HUD strings
    private var lastHp = -1
    private var lastMaxHp = -1
    private var hullStr = ""
    private var lastWave = -1
    private var waveStr = ""
    private var lastLeft = -1
    private var leftStr = ""
    private var lastScore = -1
    private var scoreStr = ""
    private var lastCoins = -1
    private var coinStr = ""
    private var lastClearCoins = -1
    private var clearStr = ""

    fun worldToScreenX(wx: Float) = (wx - camX) * scale + halfW
    fun worldToScreenY(wy: Float) = (wy - camY) * scale + halfH
    fun screenToWorldX(px: Float) = (px - halfW) / scale + camX
    fun screenToWorldY(py: Float) = (py - halfH) / scale + camY

    fun draw(d: DrawScope, g: NavalGame, theatre: Theatre, st: DreadSettings, density: Float) = with(d) {
        this@NavalRenderer.density = density
        halfW = size.width / 2f
        halfH = size.height / 2f
        scale = min(size.width / (VIEW_WIDTH_M / st.zoom), size.height / (VIEW_HEIGHT_M / st.zoom))
        val amp = if (st.screenShake) g.shake * 9f else 0f
        camX = g.camX + (if (amp > 0f) sin(g.time * 83f) * amp / scale else 0f)
        camY = g.camY + (if (amp > 0f) cos(g.time * 71f) * amp / scale else 0f)
        val t = g.time
        lockedShip = g.lock

        drawOcean(theatre, t)
        drawParticles(g, under = true)
        drawTorpedoes(g)
        for (i in g.ships.size - 1 downTo 0) drawShip(g.ships[i], t)
        drawShells(g)
        drawParticles(g, under = false)
        drawAim(g, st)
        drawNumbers(g)
        drawEdgeArrows(g)
        drawHud(g)
    }

    // --- sea ------------------------------------------------------------------------------------------

    private fun hash01(x: Int, y: Int, s: Int): Float {
        var h = x * 374761393 + y * 668265263 + s * 1274126177.toInt()
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0xFFFFFF) / 16777216f
    }

    private fun DrawScope.drawOcean(th: Theatre, t: Float) {
        drawRect(Color(th.deep))
        val shallow = Color(th.shallow)
        val swell = Color(th.swell)
        val x0 = camX - halfW / scale
        val x1 = camX + halfW / scale
        val y0 = camY - halfH / scale
        val y1 = camY + halfH / scale

        // broad lighter patches: depth variation that slides past as you sail
        val big = 380f
        var ix = floor((x0 - big) / big).toInt()
        while (ix * big < x1 + big) {
            var iy = floor((y0 - big) / big).toInt()
            while (iy * big < y1 + big) {
                val h = hash01(ix, iy, 1)
                if (h > 0.3f) {
                    val cx = (ix + hash01(ix, iy, 2)) * big
                    val cy = (iy + hash01(ix, iy, 3)) * big
                    drawCircle(shallow.copy(alpha = 0.5f * h), big * 0.62f * scale, Offset(worldToScreenX(cx), worldToScreenY(cy)))
                }
                iy++
            }
            ix++
        }

        // swell streaks
        val cell = 64f
        ix = floor(x0 / cell).toInt()
        while (ix * cell < x1) {
            var iy = floor(y0 / cell).toInt()
            while (iy * cell < y1) {
                val h = hash01(ix, iy, 7)
                if (h > 0.42f) {
                    val wx = (ix + hash01(ix, iy, 8)) * cell + sin(t * 0.35f + h * 6.28f) * 4f
                    val wy = (iy + hash01(ix, iy, 9)) * cell
                    val len = (14f + 24f * hash01(ix, iy, 10)) * scale
                    val a = 0.07f + 0.15f * (0.5f + 0.5f * sin(t * 0.8f + h * 12f))
                    val sx = worldToScreenX(wx)
                    val sy = worldToScreenY(wy)
                    val w = max(1f, 1.1f * scale)
                    drawLine(swell.copy(alpha = a), Offset(sx, sy), Offset(sx + len * 0.55f, sy - len * 0.12f), w)
                    drawLine(swell.copy(alpha = a), Offset(sx + len * 0.55f, sy - len * 0.12f), Offset(sx + len, sy), w)
                }
                iy++
            }
            ix++
        }

        if (th.ice) {
            val ice = 340f
            ix = floor((x0 - 60f) / ice).toInt()
            while (ix * ice < x1 + 60f) {
                var iy = floor((y0 - 60f) / ice).toInt()
                while (iy * ice < y1 + 60f) {
                    val h = hash01(ix, iy, 21)
                    if (h > 0.55f) {
                        val cx = (ix + hash01(ix, iy, 22)) * ice + sin(t * 0.12f + h * 9f) * 5f
                        val cy = (iy + hash01(ix, iy, 23)) * ice
                        val r = (12f + 22f * hash01(ix, iy, 24)) * scale
                        val sx = worldToScreenX(cx)
                        val sy = worldToScreenY(cy)
                        drawCircle(Color(0x44000000), r * 1.1f, Offset(sx + r * 0.12f, sy + r * 0.16f))
                        drawCircle(Color(0xFFDCEBF4), r, Offset(sx, sy))
                        drawCircle(Color(0xFFF4FAFD), r * 0.62f, Offset(sx - r * 0.18f, sy - r * 0.12f))
                        drawCircle(Color(0xFFDCEBF4), r * 0.5f, Offset(sx + r * 0.85f, sy + r * 0.3f))
                    }
                    iy++
                }
                ix++
            }
        }
    }

    // --- ships ----------------------------------------------------------------------------------------

    private fun DrawScope.drawShip(s: Ship, t: Float) {
        val c = s.cls
        val reach = (c.length * 0.62f + 30f) * scale
        val sx = worldToScreenX(s.x)
        val sy = worldToScreenY(s.y)
        if (sx < -reach || sx > size.width + reach || sy < -reach || sy > size.height + reach) return

        val enemy = s.side == 1
        val canvas = drawContext.canvas
        val hull = Hulls.of(c)
        val sink = s.sinkT
        val fade = if (!s.alive) max(0f, 1f - sink * sink * 1.1f) else 1f - s.depth * 0.8f
        if (fade <= 0.01f) return
        val shrink = 1f - 0.12f * sink
        val roll = (if (s.heading > 0f) 1f else -1f) * 9f * sink

        // soft shadow on the water
        if (s.depth < 0.3f) {
            canvas.save()
            canvas.translate(sx + 3f * scale, sy + 4f * scale)
            canvas.rotate(s.heading * DEG)
            canvas.scale(scale * shrink)
            drawPath(hull, Color(0x40000000))
            canvas.restore()
        }

        val faded = fade < 0.99f
        if (faded) {
            layer.alpha = fade
            val r = (c.length * 0.7f + 20f) * scale
            canvas.saveLayer(Rect(sx - r, sy - r, sx + r, sy + r), layer)
        }
        canvas.save()
        canvas.translate(sx, sy)
        canvas.rotate(s.heading * DEG + roll)
        canvas.scale(scale * shrink)
        drawShipBody(s, enemy, t)
        // foam: bow wave and the white streak along the hull, stronger with speed
        val sp = (s.speed / c.maxSpeed).coerceIn(0f, 1.1f)
        if (sp > 0.1f && s.depth < 0.5f) {
            val a = 0.5f * min(1f, sp)
            val l = c.length
            val hw = c.beam * 0.5f
            val foam = Color(0xFFFFFFFF).copy(alpha = a)
            val spread = hw + 1.5f + sp * 3.5f
            drawLine(foam, Offset(l * 0.5f - 1f, 0f), Offset(l * 0.5f - l * 0.1f, spread), 1.1f)
            drawLine(foam, Offset(l * 0.5f - 1f, 0f), Offset(l * 0.5f - l * 0.1f, -spread), 1.1f)
            drawLine(foam, Offset(l * 0.38f, hw + 0.4f), Offset(-l * 0.1f, hw + 1f + sp), 0.8f)
            drawLine(foam, Offset(l * 0.38f, -hw - 0.4f), Offset(-l * 0.1f, -hw - 1f - sp), 0.8f)
        }
        if (!s.alive) drawPath(hull, Color.Black.copy(alpha = 0.55f * sink))
        canvas.restore()
        if (faded) canvas.restore()

        if (s.alive && enemy && s.depth < 0.5f) {
            if (s.hp < s.maxHp) drawHpBar(sx, sy - (c.beam * 0.5f + 12f) * scale - 6f * density, c, s)
            if (s === lockedShip) drawBrackets(sx, sy, max(c.length * 0.3f * scale, 26f * density))
        }
    }

    private var lockedShip: Ship? = null

    private fun DrawScope.drawHpBar(cx: Float, y: Float, c: com.baraka.game.dreadnought.model.ShipClass, s: Ship) {
        val w = (min(c.length * 0.5f * scale, 70f * density)).coerceAtLeast(26f * density)
        val h = 4f * density
        val f = (s.hp / s.maxHp).coerceIn(0f, 1f)
        drawRect(Color(0xAA000000), Offset(cx - w / 2f - 1f, y - 1f), androidx.compose.ui.geometry.Size(w + 2f, h + 2f))
        drawRect(if (f > 0.5f) HudColors.Good else if (f > 0.25f) HudColors.Warn else HudColors.Bad, Offset(cx - w / 2f, y), androidx.compose.ui.geometry.Size(w * f, h))
    }

    private fun DrawScope.drawBrackets(cx: Float, cy: Float, r: Float) {
        val k = r * 0.38f
        val col = HudColors.Bad
        val w = 2f * density
        for (sxn in -1..1 step 2) {
            for (syn in -1..1 step 2) {
                val px = cx + sxn * r
                val py = cy + syn * r
                drawLine(col, Offset(px, py), Offset(px - sxn * k, py), w)
                drawLine(col, Offset(px, py), Offset(px, py - syn * k), w)
            }
        }
    }

    // --- projectiles ----------------------------------------------------------------------------------

    private fun DrawScope.drawShells(g: NavalGame) {
        for (i in 0 until NavalGame.SH_MAX) {
            if (!g.shAlive[i]) continue
            val p = g.shellProgress(i)
            val x = g.shOx[i] + (g.shTx[i] - g.shOx[i]) * p
            val y = g.shOy[i] + (g.shTy[i] - g.shOy[i]) * p
            val h = 4f * p * (1f - p)
            val big = g.shKind[i] == 0
            val sx = worldToScreenX(x)
            val sy = worldToScreenY(y)
            val lift = h * (if (big) 22f else 10f) * scale
            val r = max(1.4f * density, (if (big) 1.7f else 1.0f) * scale * (1f + 0.6f * h))
            drawCircle(Color(0x44000000), r * 0.9f, Offset(sx, sy))
            val dx = g.shTx[i] - g.shOx[i]
            val dy = g.shTy[i] - g.shOy[i]
            val dl = max(1f, hypot(dx, dy))
            val tail = (if (big) 14f else 8f) * scale
            val tint = if (g.shSide[i] == 0) Color(0xFFFFF3B0) else Color(0xFFFFAB91)
            drawLine(tint.copy(alpha = 0.5f), Offset(sx - dx / dl * tail, sy - lift - dy / dl * tail), Offset(sx, sy - lift), r * 0.8f)
            drawCircle(tint, r, Offset(sx, sy - lift))
        }
    }

    private fun DrawScope.drawTorpedoes(g: NavalGame) {
        for (i in 0 until NavalGame.TP_MAX) {
            if (!g.tpAlive[i]) continue
            val sx = worldToScreenX(g.tpX[i])
            val sy = worldToScreenY(g.tpY[i])
            val vx = g.tpVx[i]
            val vy = g.tpVy[i]
            val v = max(1f, hypot(vx, vy))
            val tail = 46f * scale
            drawLine(Color(0x88FFFFFF), Offset(sx, sy), Offset(sx - vx / v * tail, sy - vy / v * tail), max(1f, 1.6f * scale))
            val head = Offset(sx, sy)
            val hostile = g.tpSide[i] == 1
            if (hostile) {
                val pulse = 0.5f + 0.5f * sin(g.time * 14f)
                drawCircle(HudColors.Bad.copy(alpha = 0.25f + 0.3f * pulse), 11f * density, head, style = Stroke(2f * density))
            }
            drawLine(Color(0xFF263238), head, Offset(sx - vx / v * 7f * scale, sy - vy / v * 7f * scale), max(2f, 2.4f * scale))
            drawCircle(if (hostile) HudColors.Bad else Color(0xFFFFF176), max(1.6f * density, 1.3f * scale), head)
        }
    }

    // --- particles ------------------------------------------------------------------------------------

    private fun DrawScope.drawParticles(g: NavalGame, under: Boolean) {
        for (i in 0 until NavalGame.P_MAX) {
            if (!g.pAlive[i]) continue
            val k = g.pKind[i]
            val isUnder = k == P_WAKE || k == P_BUBBLE || k == P_SPLASH
            if (isUnder != under) continue
            val sx = worldToScreenX(g.pX[i])
            val sy = worldToScreenY(g.pY[i])
            if (sx < -80f || sx > size.width + 80f || sy < -80f || sy > size.height + 80f) continue
            val f = (g.pLife[i] / g.pMax[i]).coerceIn(0f, 1f)
            val r = max(0.8f, g.pSize[i] * scale)
            val c = Offset(sx, sy)
            when (k) {
                P_WAKE -> drawCircle(Color(1f, 1f, 1f, 0.34f * f), r, c)
                P_BUBBLE -> drawCircle(Color(1f, 1f, 1f, 0.5f * f), r, c, style = Stroke(max(1f, 0.5f * scale)))
                P_SPLASH -> drawCircle(Color(1f, 1f, 1f, 0.8f * f), r, c, style = Stroke(max(1.5f, 2.4f * scale * f)))
                P_SMOKE -> drawCircle(Color(0.1f, 0.11f, 0.12f, 0.55f * f * f + 0.05f * f), r, c)
                P_BLAST -> {
                    drawCircle(Color(1f, 0.43f, 0f, 0.75f * f), r, c)
                    drawCircle(Color(1f, 0.92f, 0.35f, 0.9f * f), r * 0.6f, c)
                }
                P_FLASH -> {
                    drawCircle(Color(1f, 0.85f, 0.4f, 0.55f * f), r, c)
                    drawCircle(Color(1f, 1f, 0.9f, 0.95f * f), r * 0.5f, c)
                }
                P_SPARK -> drawCircle(Color(1f, 0.85f, 0.5f, f), max(1.2f, r), c)
                P_DEBRIS -> drawCircle(Color(0.15f, 0.16f, 0.17f, f), max(1.2f, r), c)
            }
        }
    }

    // --- aiming ---------------------------------------------------------------------------------------

    private fun DrawScope.drawAim(g: NavalGame, st: DreadSettings) {
        val p = g.player
        lockedShip = g.lock
        if (!p.alive) return
        val px = worldToScreenX(p.x)
        val py = worldToScreenY(p.y)
        if (st.rangeRing) {
            drawCircle(Color(0x33FFFFFF), g.mainRange() * scale, Offset(px, py), style = Stroke(1.4f * density, pathEffect = dash))
        }
        val ax = worldToScreenX(g.aimX)
        val ay = worldToScreenY(g.aimY)
        val col = if (g.aimInRange) Color(0xFFFFF176) else HudColors.Bad
        drawLine(col.copy(alpha = 0.16f), Offset(px, py), Offset(ax, ay), 1.2f * density)
        val r = 15f * density
        drawCircle(col, r, Offset(ax, ay), style = Stroke(1.8f * density))
        drawCircle(col, 2.2f * density, Offset(ax, ay))
        val tick = 6f * density
        drawLine(col, Offset(ax - r - tick, ay), Offset(ax - r + tick * 0.4f, ay), 1.8f * density)
        drawLine(col, Offset(ax + r + tick, ay), Offset(ax + r - tick * 0.4f, ay), 1.8f * density)
        drawLine(col, Offset(ax, ay - r - tick), Offset(ax, ay - r + tick * 0.4f), 1.8f * density)
        drawLine(col, Offset(ax, ay + r + tick), Offset(ax, ay + r - tick * 0.4f), 1.8f * density)
    }

    private fun DrawScope.drawEdgeArrows(g: NavalGame) {
        val m = 26f * density
        for (i in 1 until g.ships.size) {
            val s = g.ships[i]
            if (!s.alive || s.depth > 0.5f) continue
            val sx = worldToScreenX(s.x)
            val sy = worldToScreenY(s.y)
            if (sx in 0f..size.width && sy in 0f..size.height) continue
            val dx = sx - halfW
            val dy = sy - halfH
            val k = min((halfW - m) / max(1f, abs(dx)), (halfH - m) / max(1f, abs(dy)))
            val ex = halfW + dx * k
            val ey = halfH + dy * k
            val a = atan2(dy, dx)
            val size = (7f + min(8f, s.cls.length / 40f)) * density
            val ca = cos(a)
            val sa = sin(a)
            arrow.reset()
            arrow.moveTo(ex + ca * size, ey + sa * size)
            arrow.lineTo(ex - ca * size * 0.6f - sa * size * 0.7f, ey - sa * size * 0.6f + ca * size * 0.7f)
            arrow.lineTo(ex - ca * size * 0.6f + sa * size * 0.7f, ey - sa * size * 0.6f - ca * size * 0.7f)
            arrow.close()
            drawPath(arrow, HudColors.Bad.copy(alpha = 0.85f))
        }
    }

    private fun DrawScope.drawNumbers(g: NavalGame) {
        text.textSize = 13f * density
        text.textAlign = Paint.Align.CENTER
        val canvas = drawContext.canvas.nativeCanvas
        for (i in 0 until NavalGame.TX_MAX) {
            if (!g.txAlive[i]) continue
            val s = g.txStr[i] ?: continue
            text.color = if (g.txColor[i] == 1) 0xFFFF6E6E.toInt() else 0xFFFFFFFF.toInt()
            text.alpha = (g.txLife[i].coerceIn(0f, 1f) * 255f).toInt()
            canvas.drawText(s, worldToScreenX(g.txX[i]), worldToScreenY(g.txY[i]), text)
        }
        text.alpha = 255
    }

    // --- HUD ------------------------------------------------------------------------------------------

    private fun DrawScope.drawHud(g: NavalGame) {
        val d = density
        val canvas = drawContext.canvas.nativeCanvas
        val p = g.player
        val hp = max(0f, p.hp).toInt()
        val maxHp = p.maxHp.toInt()
        if (hp != lastHp || maxHp != lastMaxHp) {
            lastHp = hp
            lastMaxHp = maxHp
            hullStr = "HULL $hp / $maxHp"
        }
        if (g.wave != lastWave) {
            lastWave = g.wave
            waveStr = "WAVE ${g.wave}"
        }
        if (g.enemiesLeft != lastLeft) {
            lastLeft = g.enemiesLeft
            leftStr = "${g.enemiesLeft} hostile"
        }
        if (g.score != lastScore) {
            lastScore = g.score
            scoreStr = "SCORE ${g.score}"
        }
        if (g.runCoins != lastCoins) {
            lastCoins = g.runCoins
            coinStr = "${g.runCoins} coins"
        }

        // hull panel, top left
        val x = 12f * d + safeLeft
        val y = 10f * d + safeTop
        val w = 210f * d
        drawRoundRectCompat(x, y, w, 52f * d, 8f * d, HudColors.Panel)
        val f = (p.hp / p.maxHp).coerceIn(0f, 1f)
        drawRect(Color(0xFF1B2A36), Offset(x + 10f * d, y + 22f * d), androidx.compose.ui.geometry.Size(w - 20f * d, 8f * d))
        drawRect(if (f > 0.5f) HudColors.Good else if (f > 0.25f) HudColors.Warn else HudColors.Bad, Offset(x + 10f * d, y + 22f * d), androidx.compose.ui.geometry.Size((w - 20f * d) * f, 8f * d))
        text.textAlign = Paint.Align.LEFT
        text.textSize = 12f * d
        text.color = 0xFFEAF2F8.toInt()
        canvas.drawText(hullStr, x + 10f * d, y + 17f * d, text)
        // main battery: one bar per turret, fills as it reloads
        val mains = p.cls.mainCount
        val bw = (w - 20f * d - (mains - 1) * 4f * d) / mains
        var bi = 0
        for (i in p.cls.mounts.indices) {
            if (p.cls.mounts[i].kind != com.baraka.game.dreadnought.model.MountKind.MAIN) continue
            val r = g.reloadFrac(i)
            val bx = x + 10f * d + bi * (bw + 4f * d)
            drawRect(Color(0xFF1B2A36), Offset(bx, y + 37f * d), androidx.compose.ui.geometry.Size(bw, 7f * d))
            drawRect(if (r >= 1f) Color(0xFFFFD54F) else Color(0xFF8D6E3F), Offset(bx, y + 37f * d), androidx.compose.ui.geometry.Size(bw * r, 7f * d))
            bi++
        }

        // wave / score, top centre
        val cx = halfW
        drawRoundRectCompat(cx - 96f * d, y, 192f * d, 52f * d, 8f * d, HudColors.Panel)
        text.textAlign = Paint.Align.CENTER
        text.textSize = 15f * d
        text.color = 0xFFFFB74D.toInt()
        canvas.drawText(waveStr, cx, y + 20f * d, text)
        text.textSize = 11f * d
        text.color = 0xFFEAF2F8.toInt()
        canvas.drawText(leftStr, cx, y + 35f * d, text)
        text.color = 0xFF93A9BA.toInt()
        canvas.drawText(scoreStr + "  |  " + coinStr, cx, y + 48f * d, text)

        // banners
        if (g.phase == PHASE_BRIEF) {
            text.textSize = 40f * d
            text.color = 0xFFFFFFFF.toInt()
            text.alpha = (min(1f, g.phaseT) * 255).toInt()
            canvas.drawText(waveStr, cx, halfH - 30f * d, text)
            if (g.wave % 5 == 0) {
                text.textSize = 16f * d
                text.color = 0xFFEF5350.toInt()
                canvas.drawText("FLAGSHIP INBOUND", cx, halfH - 6f * d, text)
            }
            text.alpha = 255
        } else if (g.phase == PHASE_CLEARED) {
            val bonus = 20 + g.wave * 8
            if (bonus != lastClearCoins) {
                lastClearCoins = bonus
                clearStr = "+$bonus coins"
            }
            text.textSize = 34f * d
            text.color = 0xFF66BB6A.toInt()
            canvas.drawText("WAVE CLEARED", cx, halfH - 30f * d, text)
            text.textSize = 16f * d
            text.color = 0xFFEAF2F8.toInt()
            canvas.drawText(clearStr, cx, halfH - 6f * d, text)
        }
    }

    var safeLeft = 0f
    var safeTop = 0f

    private fun DrawScope.drawRoundRectCompat(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color) {
        drawRoundRect(c, Offset(x, y), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(r))
    }
}

/** Draws one ship class centred and fitted in the scope (bow to the right), for the harbour and field guide. */
fun DrawScope.drawShipPortrait(ship: Ship, enemy: Boolean, t: Float) {
    val c = ship.cls
    val fit = min(size.width * 0.9f / c.length, size.height * 0.78f / (c.beam * 1.15f))
    val canvas = drawContext.canvas
    canvas.save()
    canvas.translate(size.width / 2f, size.height / 2f)
    canvas.scale(fit)
    canvas.save()
    canvas.translate(2f, 3f)
    drawPath(Hulls.of(c), Color(0x44000000))
    canvas.restore()
    drawShipBody(ship, enemy, t)
    canvas.restore()
}
