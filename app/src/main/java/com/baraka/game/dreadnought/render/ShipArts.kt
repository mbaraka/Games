package com.baraka.game.dreadnought.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.dreadnought.model.MountKind
import com.baraka.game.dreadnought.model.MountSpec
import com.baraka.game.dreadnought.model.ShipClass
import com.baraka.game.dreadnought.systems.MAX_FIRES
import com.baraka.game.dreadnought.systems.Ship
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Ship drawing. Everything here works in the ship's own space: metres, +x towards the bow, +y to starboard,
 * origin amidships, so the same code draws a ship at any heading and zoom. A ship is built in layers:
 * hull plan (from the class's station table) -> deck details -> gun turrets (rotating, from the class's
 * mounts) -> superstructure. A new ship needs one [ShipArt] registered in [ShipArts]; nothing else here changes.
 */
class ShipPalette(
    val boot: Color,
    val hull: Color,
    val deck: Color,
    val steel: Color,
    val light: Color,
    val dark: Color,
    val accent: Color
)

val PlayerPalette = ShipPalette(
    boot = Color(0xFF28323B), hull = Color(0xFF66798A), deck = Color(0xFF7D8E9B), steel = Color(0xFF93A3AF),
    light = Color(0xFFC9D4DC), dark = Color(0xFF2F3A44), accent = Color(0xFF4FC3F7)
)
val EnemyPalette = ShipPalette(
    boot = Color(0xFF6B1F1F), hull = Color(0xFF4B5056), deck = Color(0xFF5E636B), steel = Color(0xFF777D86),
    light = Color(0xFFB4B9C0), dark = Color(0xFF22262A), accent = Color(0xFFE53935)
)
private val SubPalette = ShipPalette(
    boot = Color(0xFF0E1114), hull = Color(0xFF1F262C), deck = Color(0xFF2A3239), steel = Color(0xFF3C464E),
    light = Color(0xFF59646D), dark = Color(0xFF0C0F11), accent = Color(0xFFE53935)
)

/** One ship's decorations. [deck] draws under the turrets, [structure] over them. */
abstract class ShipArt {
    open val palette: ShipPalette? = null
    /** Where the flight deck / main deck is drawn instead of the default inset hull. */
    open fun deck(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) {}
    open fun structure(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) {}
}

object ShipArts {
    private val table: Map<ShipClass, ShipArt> = mapOf(
        ShipClass.DREADNOUGHT to CapitalArt(
            towerX = 12f, towerLen = 30f, towerWid = 17f, tiers = 4, funnels = floatArrayOf(-22f, -50f), funnelLen = 12f, funnelWid = 8.5f,
            masts = floatArrayOf(14f, -36f), planeX = -118f, boats = true
        ),
        ShipClass.BATTLESHIP to CapitalArt(
            towerX = 8f, towerLen = 26f, towerWid = 16f, tiers = 4, funnels = floatArrayOf(-28f), funnelLen = 15f, funnelWid = 11f,
            masts = floatArrayOf(10f, -46f), planeX = -4f, boats = true
        ),
        ShipClass.FLAGSHIP to CapitalArt(
            towerX = 6f, towerLen = 30f, towerWid = 19f, tiers = 6, funnels = floatArrayOf(-34f), funnelLen = 16f, funnelWid = 12f,
            masts = floatArrayOf(10f, -58f), planeX = -128f, boats = true
        ),
        ShipClass.CRUISER to CapitalArt(
            towerX = 10f, towerLen = 20f, towerWid = 11f, tiers = 3, funnels = floatArrayOf(-12f, -36f), funnelLen = 9f, funnelWid = 6.5f,
            masts = floatArrayOf(12f, -28f), planeX = Float.NaN, boats = false
        ),
        ShipClass.DESTROYER to DestroyerArt,
        ShipClass.PATROL_BOAT to PatrolArt,
        ShipClass.TORPEDO_BOAT to TorpedoBoatArt,
        ShipClass.SUBMARINE to SubmarineArt,
        ShipClass.CARRIER to CarrierArt
    )

    fun of(c: ShipClass): ShipArt = table[c] ?: CapitalArt(0f, 20f, 10f, 2, floatArrayOf(-20f), 8f, 6f, floatArrayOf(), Float.NaN, false)
}

// --- small drawing helpers -------------------------------------------------------------------------

private fun DrawScope.rect(cx: Float, cy: Float, w: Float, h: Float, c: Color) =
    drawRect(c, Offset(cx - w / 2f, cy - h / 2f), Size(w, h))

private fun DrawScope.rrect(cx: Float, cy: Float, w: Float, h: Float, c: Color, r: Float) =
    drawRoundRect(c, Offset(cx - w / 2f, cy - h / 2f), Size(w, h), CornerRadius(r))

private fun DrawScope.oval(cx: Float, cy: Float, w: Float, h: Float, c: Color) =
    drawOval(c, Offset(cx - w / 2f, cy - h / 2f), Size(w, h))

private fun DrawScope.disc(cx: Float, cy: Float, r: Float, c: Color) = drawCircle(c, r, Offset(cx, cy))

private fun DrawScope.ln(x0: Float, y0: Float, x1: Float, y1: Float, w: Float, c: Color) =
    drawLine(c, Offset(x0, y0), Offset(x1, y1), w)

private fun DrawScope.funnel(cx: Float, cy: Float, len: Float, wid: Float, p: ShipPalette) {
    oval(cx, cy, len * 1.14f, wid * 1.14f, p.dark)
    oval(cx, cy, len, wid, p.steel)
    oval(cx - len * 0.04f, cy, len * 0.84f, wid * 0.8f, p.hull)
    oval(cx + len * 0.02f, cy, len * 0.62f, wid * 0.52f, p.dark)
}

private fun DrawScope.mast(cx: Float, cy: Float, span: Float, p: ShipPalette) {
    ln(cx - 0.4f, cy - span / 2f, cx - 0.4f, cy + span / 2f, 0.7f, p.dark)
    disc(cx, cy, 1.1f, p.dark)
    disc(cx, cy, 0.5f, p.light)
}

private fun DrawScope.radar(cx: Float, cy: Float, len: Float, t: Float, p: ShipPalette) {
    val a = t * 2.2f
    val dx = cos(a) * len / 2f
    val dy = sin(a) * len / 2f
    ln(cx - dx, cy - dy, cx + dx, cy + dy, 0.8f, p.accent)
    disc(cx, cy, 0.8f, p.dark)
}

/** A stacked bridge: each tier smaller and lighter than the one below, with a window strip on the front. */
private fun DrawScope.tower(cx: Float, cy: Float, len: Float, wid: Float, tiers: Int, p: ShipPalette) {
    rrect(cx, cy, len * 1.08f, wid * 1.08f, p.dark, wid * 0.18f)
    for (k in 0 until tiers) {
        val f = 1f - k * (0.62f / tiers)
        val tone = if (k % 2 == 0) p.steel else p.light
        rrect(cx + k * len * 0.025f, cy, len * f, wid * f, tone, wid * 0.14f * f)
        rect(cx + k * len * 0.025f + len * f * 0.36f, cy, len * 0.05f, wid * f * 0.78f, p.dark)
    }
}

private fun DrawScope.plane(cx: Float, cy: Float, size: Float, p: ShipPalette) {
    ln(cx - size * 0.5f, cy, cx + size * 0.5f, cy, size * 0.12f, p.dark)
    ln(cx + size * 0.08f, cy - size * 0.55f, cx + size * 0.08f, cy + size * 0.55f, size * 0.1f, p.dark)
    ln(cx - size * 0.45f, cy - size * 0.2f, cx - size * 0.45f, cy + size * 0.2f, size * 0.07f, p.dark)
}

// --- capital ships: dreadnought, battleships, cruisers ----------------------------------------------

private class CapitalArt(
    val towerX: Float,
    val towerLen: Float,
    val towerWid: Float,
    val tiers: Int,
    val funnels: FloatArray,
    val funnelLen: Float,
    val funnelWid: Float,
    val masts: FloatArray,
    val planeX: Float,
    val boats: Boolean
) : ShipArt() {
    override fun deck(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        val l = c.length
        val b = c.beam
        // centre line, and the steps where the forecastle and quarterdeck drop away
        ln(-l * 0.44f, 0f, l * 0.42f, 0f, 0.35f, p.dark.copy(alpha = 0.45f))
        ln(l * 0.27f, -b * 0.4f, l * 0.27f, b * 0.4f, 0.5f, p.dark.copy(alpha = 0.7f))
        ln(-l * 0.31f, -b * 0.4f, -l * 0.31f, b * 0.4f, 0.5f, p.dark.copy(alpha = 0.7f))
        // deck edge rail
        ln(-l * 0.4f, b * 0.425f, l * 0.34f, b * 0.425f, 0.3f, p.light.copy(alpha = 0.5f))
        ln(-l * 0.4f, -b * 0.425f, l * 0.34f, -b * 0.425f, 0.3f, p.light.copy(alpha = 0.5f))
        // anchor windlass and hawse at the bow, mooring bitts at the stern
        disc(l * 0.40f, b * 0.12f, 1.4f, p.dark)
        disc(l * 0.40f, -b * 0.12f, 1.4f, p.dark)
        rect(l * 0.36f, 0f, 3f, 4f, p.dark)
        disc(-l * 0.46f, b * 0.2f, 1f, p.dark)
        disc(-l * 0.46f, -b * 0.2f, 1f, p.dark)
        if (!planeX.isNaN()) {
            // catapult track with a floatplane on it
            ln(planeX - 14f, 0f, planeX + 14f, 0f, 1f, p.dark.copy(alpha = 0.6f))
            plane(planeX, 0f, 13f, p)
            rect(planeX + 18f, b * 0.2f, 3f, 3f, p.dark)
        }
        // hatches
        for (k in 0 until 3) rect(l * 0.15f - k * 4.5f, b * 0.22f, 2.4f, 2.4f, p.dark.copy(alpha = 0.8f))
    }

    override fun structure(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        val b = c.beam
        if (boats) {
            // boat deck: a rank of lifeboats on each side of the funnels
            val x0 = funnels.minOrNull() ?: 0f
            for (k in 0 until 5) {
                val bx = x0 - 8f + k * 7f
                rrect(bx, b * 0.28f, 5.4f, 2.1f, p.light, 1f)
                rrect(bx, -b * 0.28f, 5.4f, 2.1f, p.light, 1f)
            }
        }
        for (f in funnels) funnel(f, 0f, funnelLen, funnelWid, p)
        // secondary directors on the after end of the ship
        rrect(c.length * -0.27f, 0f, 7f, 5.5f, p.dark, 1.5f)
        rrect(c.length * -0.27f, 0f, 5.6f, 4.2f, p.steel, 1.2f)
        tower(towerX, 0f, towerLen, towerWid, tiers, p)
        for (m in masts) mast(m, 0f, b * 0.5f, p)
        radar(towerX + towerLen * 0.18f, 0f, 9f, t, p)
        // accent stripe on the forecastle: the ship's identity at a glance
        rect(c.length * 0.34f, 0f, 1.6f, 0.9f, p.accent)
    }
}

// --- destroyer ---------------------------------------------------------------------------------------

private object DestroyerArt : ShipArt() {
    override fun deck(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        ln(-c.length * 0.44f, 0f, c.length * 0.42f, 0f, 0.3f, p.dark.copy(alpha = 0.4f))
        // quintuple torpedo bank amidships, depth-charge racks aft
        rrect(-26f, 0f, 10f, 5.2f, p.dark, 1f)
        for (k in -2..2) ln(-30.5f, k * 0.95f, -21.5f, k * 0.95f, 0.6f, p.steel)
        disc(-55f, 2.6f, 1.2f, p.dark)
        disc(-55f, -2.6f, 1.2f, p.dark)
        rect(c.length * 0.43f, 0f, 2.4f, 1.8f, p.dark)
    }

    override fun structure(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        funnel(-6f, 0f, 6.5f, 4.8f, p)
        funnel(-17f, 0f, 6.5f, 4.8f, p)
        tower(12f, 0f, 12f, 6.4f, 2, p)
        mast(12f, 0f, 7f, p)
        mast(-12f, 0f, 5f, p)
        radar(14f, 0f, 5f, t, p)
        rect(c.length * 0.38f, 0f, 1.4f, 0.8f, p.accent)
    }
}

// --- gunboats ------------------------------------------------------------------------------------------

private object PatrolArt : ShipArt() {
    override fun deck(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        ln(-c.length * 0.4f, 0f, c.length * 0.34f, 0f, 0.25f, p.dark.copy(alpha = 0.4f))
        rect(-24f, 0f, 8f, 3.4f, p.dark.copy(alpha = 0.8f))
    }

    override fun structure(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        rrect(-2f, 0f, 20f, 6.4f, p.dark, 1.6f)
        rrect(-1f, 0f, 17.5f, 5.2f, p.steel, 1.4f)
        rect(4f, 0f, 1.4f, 4.4f, p.dark)
        funnel(-9f, 0f, 3.6f, 3f, p)
        mast(3f, 0f, 5f, p)
        rect(c.length * 0.4f, 0f, 1.2f, 0.7f, p.accent)
    }
}

private object TorpedoBoatArt : ShipArt() {
    override fun deck(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        // twin torpedo tubes angled out of the bow quarters, engine hatch, exhaust at the transom
        ln(8f, 1.3f, 27f, 2.5f, 2.2f, p.dark)
        ln(8f, -1.3f, 27f, -2.5f, 2.2f, p.dark)
        ln(10f, 1.3f, 26f, 2.4f, 1.1f, p.steel)
        ln(10f, -1.3f, 26f, -2.4f, 1.1f, p.steel)
        rect(-16f, 0f, 14f, 4.4f, p.dark.copy(alpha = 0.85f))
        disc(-c.length * 0.46f, 1.6f, 0.9f, p.dark)
        disc(-c.length * 0.46f, -1.6f, 0.9f, p.dark)
    }

    override fun structure(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        rrect(2f, 0f, 11f, 4.6f, p.dark, 1.4f)
        rrect(2.4f, 0f, 9f, 3.6f, p.steel, 1.2f)
        rect(6f, 0f, 1.2f, 3.2f, p.dark)
        mast(1f, 0f, 4f, p)
        rect(c.length * 0.4f, 0f, 1.2f, 0.7f, p.accent)
    }
}

// --- submarine -------------------------------------------------------------------------------------------

private object SubmarineArt : ShipArt() {
    override val palette: ShipPalette = SubPalette

    override fun deck(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        // free-flood slots along the casing, hydroplanes bow and stern
        for (k in 0 until 11) {
            rect(-38f + k * 6f, 2.1f, 3f, 0.8f, p.dark)
            rect(-38f + k * 6f, -2.1f, 3f, 0.8f, p.dark)
        }
        rect(-42f, 4.6f, 5f, 1.8f, p.steel)
        rect(-42f, -4.6f, 5f, 1.8f, p.steel)
        rect(30f, 3.9f, 3.4f, 1.3f, p.steel)
        rect(30f, -3.9f, 3.4f, 1.3f, p.steel)
    }

    override fun structure(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        rrect(6f, 0f, 17f, 5.4f, p.dark, 2.7f)
        rrect(6.4f, 0f, 14.5f, 4.2f, p.steel, 2.1f)
        ln(8f, -1.2f, 8f, 1.2f, 0.5f, p.dark)
        disc(10f, 1.1f, 0.5f, p.light)
        disc(10f, -1.1f, 0.5f, p.light)
    }
}

// --- carrier -----------------------------------------------------------------------------------------------

private object CarrierArt : ShipArt() {
    private val flightDeck = Color(0xFF454B52)

    private fun DrawScope.lift(x: Float, y: Float) {
        rect(x, y, 15f, 14f, Color(0xFF353A40))
        drawRect(Color(0xFFF2C94C), Offset(x - 7.5f, y - 7f), Size(15f, 14f), style = Stroke(0.7f))
    }

    override fun deck(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        val l = c.length
        val b = c.beam
        val canvas = drawContext.canvas
        // the flight deck: the whole top of the hull, slightly overhanging
        canvas.save()
        canvas.scale(0.975f, 0.93f)
        drawPath(Hulls.of(c), flightDeck)
        canvas.restore()
        ln(-l * 0.46f, b * 0.405f, l * 0.44f, b * 0.405f, 0.5f, p.light.copy(alpha = 0.7f))
        ln(-l * 0.46f, -b * 0.405f, l * 0.44f, -b * 0.405f, 0.5f, p.light.copy(alpha = 0.7f))
        // centre line
        for (k in 0 until 13) rect(-l * 0.43f + k * 19f, 0f, 9f, 0.9f, Color(0xFFE9E9D8))
        // arrester wires
        for (k in 0 until 4) ln(-l * 0.4f + k * 6f, -b * 0.3f, -l * 0.4f + k * 6f, b * 0.3f, 0.3f, p.light.copy(alpha = 0.5f))
        // two aircraft lifts
        lift(l * 0.27f, -b * 0.06f)
        lift(-l * 0.17f, -b * 0.06f)
        // parked aircraft on the after deck
        for (k in 0 until 6) {
            val px = -l * 0.40f + (k % 3) * 9f
            val py = if (k < 3) -b * 0.2f else b * 0.0f
            plane(px, py, 8f, p)
        }
        ln(l * 0.36f, -b * 0.2f, l * 0.36f, b * 0.2f, 0.5f, p.dark.copy(alpha = 0.7f))
    }

    override fun structure(d: DrawScope, c: ShipClass, p: ShipPalette, t: Float) = with(d) {
        val b = c.beam
        val iy = b * 0.40f
        // the island, built out over the starboard edge
        rrect(12f, iy, 38f, 7.4f, p.dark, 1.4f)
        rrect(12f, iy, 35f, 6f, p.steel, 1.2f)
        rrect(16f, iy, 20f, 4.6f, p.light, 1f)
        rect(24f, iy, 1.4f, 4f, p.dark)
        funnel(0f, iy, 10f, 5.2f, p)
        mast(18f, iy, 8f, p)
        radar(10f, iy, 8f, t, p)
        rect(c.length * 0.40f, -b * 0.2f, 1.5f, 0.9f, p.accent)
    }
}

// --- hulls and turrets --------------------------------------------------------------------------------------

/** Hull outlines, built once per class from the station table and shared by every ship of that class. */
object Hulls {
    private val cache = arrayOfNulls<Path>(ShipClass.entries.size)

    fun of(c: ShipClass): Path {
        cache[c.ordinal]?.let { return it }
        val built = build(c)
        cache[c.ordinal] = built
        return built
    }

    private fun build(c: ShipClass): Path {
        val n = c.profile.size / 2
        val xs = FloatArray(n) { (-0.5f + c.profile[it * 2]) * c.length }
        val ws = FloatArray(n) { c.profile[it * 2 + 1] * c.beam * 0.5f }
        val path = Path()
        path.moveTo(xs[0], ws[0])
        // starboard side, stern to bow, as a smooth curve through the stations (Catmull-Rom as cubic Beziers)
        for (i in 0 until n - 1) {
            val p0 = max(i - 1, 0)
            val p3 = minOf(i + 2, n - 1)
            path.cubicTo(
                xs[i] + (xs[i + 1] - xs[p0]) / 6f, max(0f, ws[i] + (ws[i + 1] - ws[p0]) / 6f),
                xs[i + 1] - (xs[p3] - xs[i]) / 6f, max(0f, ws[i + 1] - (ws[p3] - ws[i]) / 6f),
                xs[i + 1], ws[i + 1]
            )
        }
        path.lineTo(xs[n - 1], -ws[n - 1])
        // port side, bow to stern
        for (i in n - 1 downTo 1) {
            val p0 = minOf(i + 1, n - 1)
            val p3 = max(i - 2, 0)
            path.cubicTo(
                xs[i] + (xs[i - 1] - xs[p0]) / 6f, -max(0f, ws[i] + (ws[i - 1] - ws[p0]) / 6f),
                xs[i - 1] - (xs[p3] - xs[i]) / 6f, -max(0f, ws[i - 1] - (ws[p3] - ws[i]) / 6f),
                xs[i - 1], -ws[i - 1]
            )
        }
        path.close()
        return path
    }
}

private const val RAD_TO_DEG = 57.29578f

/** One gun mount at its current traverse. Barrels slide back while [kick] runs out after a shot. */
private fun DrawScope.drawMount(m: MountSpec, off: Float, kick: Float, p: ShipPalette) {
    val canvas = drawContext.canvas
    val r = m.radius
    val main = m.kind == MountKind.MAIN
    canvas.save()
    canvas.translate(m.x, m.y)
    canvas.rotate((m.arcCenter + off) * RAD_TO_DEG)
    if (main) disc(0f, 0f, r * 1.12f, p.dark)
    val len = r * (if (main) 2.5f else 1.9f) - kick * r * 0.55f
    val w = r * (if (main) 0.22f else 0.17f)
    for (k in 0 until m.barrels) {
        val y = (k - (m.barrels - 1) / 2f) * r * (if (main) 0.62f else 0.5f)
        ln(r * 0.3f, y, r * 0.3f + len, y, w, p.dark)
        ln(r * 0.3f + len - r * 0.3f, y, r * 0.3f + len, y, w * 1.5f, p.dark)
    }
    rrect(-r * 0.2f, 0f, r * 1.55f, r * 1.95f, p.dark, r * 0.38f)
    rrect(-r * 0.22f, 0f, r * 1.32f, r * 1.62f, p.hull, r * 0.3f)
    if (main) {
        rect(-r * 0.62f, 0f, r * 0.34f, r * 0.9f, p.dark)
        disc(-r * 0.05f, 0f, r * 0.22f, p.steel)
    }
    canvas.restore()
}

/**
 * Draws one whole ship in its own space (the caller has already moved, rotated and scaled the canvas).
 * [enemy] picks the palette; [t] is the clock for the animated radar and fire.
 */
fun DrawScope.drawShipBody(ship: Ship, enemy: Boolean, t: Float) {
    val c = ship.cls
    val art = ShipArts.of(c)
    val p = art.palette ?: if (enemy) EnemyPalette else PlayerPalette
    val hull = Hulls.of(c)
    val canvas = drawContext.canvas

    // waterline stripe and hull sides
    canvas.save()
    canvas.scale(1f, 1.07f)
    drawPath(hull, p.boot)
    canvas.restore()
    drawPath(hull, p.hull)
    // main deck: the hull plan inset, so the flared sides show as a band around it
    canvas.save()
    canvas.scale(0.955f, 0.8f)
    drawPath(hull, p.deck)
    canvas.restore()
    art.deck(this, c, p, t)

    // turrets sit on the deck, under the superstructure
    for (i in c.mounts.indices) drawMount(c.mounts[i], ship.turretOff[i], ship.kick[i], p)
    art.structure(this, c, p, t)

    if (ship.flash > 0f) drawPath(hull, Color.White.copy(alpha = ship.flash * 0.45f))

    // burning hits
    for (k in 0 until minOf(ship.fireN, MAX_FIRES)) {
        val fx = ship.fireX[k]
        val fy = ship.fireY[k]
        val f = 1f + 0.25f * sin(t * 17f + k * 2.1f) + 0.12f * sin(t * 31f + k)
        disc(fx, fy, 6f * f, Color(0x33FF6D00))
        disc(fx, fy, 3.2f * f, Color(0xDDFF8F00))
        disc(fx + 0.4f, fy, 1.6f * f, Color(0xFFFFEE58))
    }
}
