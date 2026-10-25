package com.baraka.game.battleship

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** The game's whole palette for the sea and the ships, in one place. */
private object Sea {
    val Water = Color(0xFF1565C0)
    val WaterDeep = Color(0xFF0F4C95)
    val Fog = Color(0xFF1A237E)
    val FogDeep = Color(0xFF151C66)
    val Wave = Color(0x26FFFFFF)
    val GridLine = Color(0x33000000)
    val FireOuter = Color(0xFFFF6D00)
    val FireInner = Color(0xFFFFEB3B)
    val Cross = Color(0xFFFF5252)
    val Shadow = Color(0x55000000)
    val Foam = Color(0x66FFFFFF)
}

private class ShipPalette(
    val hull: Color,
    val deck: Color,
    val dark: Color,
    val light: Color,
    val accent: Color
)

private val AlivePalette = ShipPalette(
    hull = Color(0xFF546E7A), deck = Color(0xFF90A4AE), dark = Color(0xFF37474F),
    light = Color(0xFFCFD8DC), accent = Color(0xFFFFCA28)
)
private val SunkPalette = ShipPalette(
    hull = Color(0xFF1C262B), deck = Color(0xFF2B383E), dark = Color(0xFF11181B),
    light = Color(0xFF3E5059), accent = Color(0xFF3E5059)
)

/** Carrier, battleship, cruiser, submarine, destroyer: the ship id is its art. */
private const val CARRIER = 0
private const val BATTLESHIP = 1
private const val CRUISER = 2
private const val SUBMARINE = 3
private const val DESTROYER = 4

/**
 * A 10x10 sea. Layers, bottom to top: water, ships (only those in [visible]), shot markers.
 * Ships are drawn whole across their cells (bow pointing right or down), not cell by cell.
 * Taps are turned into a cell index and sent to [onCellClick].
 */
@Composable
internal fun Board(
    ids: List<Int>,
    shots: List<ShotKind?>?,
    visible: BooleanArray,
    sunk: BooleanArray,
    modifier: Modifier = Modifier,
    fog: Boolean = false,
    onCellClick: ((Int) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) { drawWater(fog) }
        Canvas(Modifier.fillMaxSize()) { drawFleet(ids, visible, sunk) }
        if (shots != null) {
            Canvas(Modifier.fillMaxSize()) { drawShots(ids, shots, visible, sunk) }
        }
        if (onCellClick != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(onCellClick) {
                        detectTapGestures { p ->
                            val col = (p.x / size.width * 10f).toInt().coerceIn(0, 9)
                            val row = (p.y / size.height * 10f).toInt().coerceIn(0, 9)
                            onCellClick(row * 10 + col)
                        }
                    }
            )
        }
    }
}

private fun DrawScope.drawWater(fog: Boolean) {
    val c = size.width / 10f
    drawRect(if (fog) Sea.Fog else Sea.Water)
    // a darker checker so cells are easy to tell apart
    for (i in 0 until 100) {
        if ((i / 10 + i % 10) % 2 == 0) continue
        drawRect(
            if (fog) Sea.FogDeep else Sea.WaterDeep,
            Offset((i % 10) * c, (i / 10) * c),
            Size(c, c)
        )
    }
    for (i in 0 until 100) {
        val x = (i % 10) * c
        val y = (i / 10) * c
        val wy = y + c * (0.3f + 0.1f * ((i * 7) % 4))
        drawLine(Sea.Wave, Offset(x + c * 0.15f, wy), Offset(x + c * 0.5f, wy), c * 0.05f, StrokeCap.Round)
        drawLine(Sea.Wave, Offset(x + c * 0.45f, wy + c * 0.26f), Offset(x + c * 0.85f, wy + c * 0.26f), c * 0.05f, StrokeCap.Round)
    }
    for (i in 1..9) {
        drawLine(Sea.GridLine, Offset(i * c, 0f), Offset(i * c, size.height), 1f)
        drawLine(Sea.GridLine, Offset(0f, i * c), Offset(size.width, i * c), 1f)
    }
}

private fun DrawScope.drawFleet(ids: List<Int>, visible: BooleanArray, sunk: BooleanArray) {
    val c = size.width / 10f
    for (id in 0 until BattleShipFleetLengths.size) {
        if (!visible[id]) continue
        var first = -1
        var n = 0
        for (i in ids.indices) {
            if (ids[i] == id) {
                if (first < 0) first = i
                n++
            }
        }
        if (first < 0) continue
        val horizontal = first % 10 < 9 && ids[first + 1] == id
        val x0 = (first % 10) * c
        val y0 = (first / 10) * c
        if (horizontal) {
            translate(x0, y0) { drawShip(id, n * c, c, sunk[id]) }
        } else {
            // Turn the drawing a quarter turn so the bow points down.
            translate(x0 + c, y0) {
                rotate(90f, Offset.Zero) { drawShip(id, n * c, c, sunk[id]) }
            }
        }
    }
}

private fun DrawScope.drawShots(ids: List<Int>, shots: List<ShotKind?>, visible: BooleanArray, sunk: BooleanArray) {
    val c = size.width / 10f
    for (i in 0 until 100) {
        val shot = shots[i] ?: continue
        val id = ids[i]
        val gone = shot == ShotKind.HIT && id >= 0 && sunk[id] && visible[id]
        drawMarker(((i % 10) + 0.5f) * c, ((i / 10) + 0.5f) * c, c, shot, gone)
    }
}

/** Miss = white splash ring, hit = fire, sunk = red cross on the charred wreck. */
internal fun DrawScope.drawMarker(cx: Float, cy: Float, c: Float, shot: ShotKind, sunk: Boolean) {
    val centre = Offset(cx, cy)
    when {
        shot == ShotKind.MISS -> {
            drawCircle(Color.White, c * 0.24f, centre, alpha = 0.9f, style = Stroke(c * 0.07f))
            drawCircle(Color.White, c * 0.08f, centre)
        }
        sunk -> {
            val a = c * 0.25f
            val st = c * 0.11f
            drawLine(Sea.Cross, Offset(cx - a, cy - a), Offset(cx + a, cy + a), st, StrokeCap.Round)
            drawLine(Sea.Cross, Offset(cx + a, cy - a), Offset(cx - a, cy + a), st, StrokeCap.Round)
        }
        else -> {
            drawCircle(Color(0x66FF6D00), c * 0.42f, centre)
            drawCircle(Sea.FireOuter, c * 0.3f, centre)
            drawCircle(Sea.Cross, c * 0.3f, centre, style = Stroke(c * 0.06f))
            drawCircle(Sea.FireInner, c * 0.14f, centre)
        }
    }
}

/** Marker for the legend: a sunk ship sits on a dark hull so the cross is visible. */
internal fun DrawScope.drawLegendIcon(shot: ShotKind, sunk: Boolean) {
    val c = size.width
    drawRect(Sea.Water)
    if (sunk) {
        drawRoundRect(SunkPalette.hull, Offset(0f, c * 0.2f), Size(c, c * 0.6f), CornerRadius(c * 0.3f))
    }
    drawMarker(c / 2f, c / 2f, c, shot, sunk)
}

// --- ships -----------------------------------------------------------------------------------
// Everything below draws in the ship's own frame: x runs stern (0) to bow (len), y across the
// beam (0..c), so one function serves both horizontal and vertical ships.

private fun DrawScope.drawShip(id: Int, len: Float, c: Float, sunk: Boolean) {
    val p = if (sunk) SunkPalette else AlivePalette
    val cy = c / 2f
    val pad = c * 0.07f
    val x0 = pad
    val x1 = len - pad
    when (id) {
        CARRIER -> carrier(x0, x1, cy, c, p, sunk)
        BATTLESHIP -> battleship(x0, x1, cy, c, p)
        CRUISER -> cruiser(x0, x1, cy, c, p)
        SUBMARINE -> submarine(x0, x1, cy, c, p)
        DESTROYER -> destroyer(x0, x1, cy, c, p)
        else -> hull(x0, x1, cy, c * 0.3f, c * 0.8f, p)
    }
    if (!sunk) bowWave(x1, cy, c)
}

private fun hullPath(x0: Float, x1: Float, cy: Float, hw: Float, bow: Float): Path = Path().apply {
    moveTo(x0, cy - hw * 0.85f)
    quadraticBezierTo(x0 - hw * 0.1f, cy, x0, cy + hw * 0.85f)
    // keep the beam up to the shoulder, then curve into a pointed bow
    quadraticBezierTo(x1 - bow * 0.3f, cy + hw, x1, cy)
    quadraticBezierTo(x1 - bow * 0.3f, cy - hw, x1 - bow, cy - hw)
    close()
}

private fun DrawScope.hull(x0: Float, x1: Float, cy: Float, hw: Float, bow: Float, p: ShipPalette) {
    val sh = Path().apply { addPath(hullPath(x0, x1, cy, hw, bow), Offset(hw * 0.12f, hw * 0.18f)) }
    drawPath(sh, Sea.Shadow)
    drawPath(hullPath(x0, x1, cy, hw, bow), p.hull)
    drawPath(hullPath(x0 + hw * 0.18f, x1 - hw * 0.28f, cy, hw * 0.78f, bow * 0.75f), p.deck)
}

private fun DrawScope.turret(cx: Float, cy: Float, r: Float, barrels: Int, p: ShipPalette) {
    val barrel = r * 1.9f
    val w = r * 0.38f
    if (barrels == 1) {
        drawLine(p.dark, Offset(cx, cy), Offset(cx + barrel, cy), w, StrokeCap.Round)
    } else {
        drawLine(p.dark, Offset(cx, cy - r * 0.32f), Offset(cx + barrel, cy - r * 0.32f), w, StrokeCap.Round)
        drawLine(p.dark, Offset(cx, cy + r * 0.32f), Offset(cx + barrel, cy + r * 0.32f), w, StrokeCap.Round)
    }
    drawCircle(p.dark, r, Offset(cx, cy))
    drawCircle(p.hull, r * 0.7f, Offset(cx, cy))
    drawCircle(p.light, r * 0.28f, Offset(cx - r * 0.1f, cy - r * 0.1f))
}

private fun DrawScope.bridge(cx: Float, cy: Float, w: Float, h: Float, p: ShipPalette) {
    drawRoundRect(p.dark, Offset(cx - w / 2f, cy - h / 2f), Size(w, h), CornerRadius(h * 0.25f))
    drawRoundRect(p.light, Offset(cx - w / 2f + w * 0.08f, cy - h / 2f + h * 0.12f), Size(w * 0.84f, h * 0.76f), CornerRadius(h * 0.2f))
    // windows
    drawLine(p.dark, Offset(cx + w * 0.1f, cy - h * 0.28f), Offset(cx + w * 0.1f, cy + h * 0.28f), h * 0.12f)
}

private fun DrawScope.funnel(cx: Float, cy: Float, r: Float, p: ShipPalette) {
    drawOval(p.dark, Offset(cx - r * 1.3f, cy - r), Size(r * 2.6f, r * 2f))
    drawOval(p.accent, Offset(cx - r * 0.9f, cy - r * 0.65f), Size(r * 1.8f, r * 1.3f))
}

private fun DrawScope.carrier(x0: Float, x1: Float, cy: Float, c: Float, p: ShipPalette, sunk: Boolean) {
    val hw = c * 0.42f
    val len = x1 - x0
    hull(x0, x1, cy, hw, c * 0.9f, p)
    // flight deck markings
    val dash = PathEffect.dashPathEffect(floatArrayOf(c * 0.3f, c * 0.22f))
    drawLine(p.light, Offset(x0 + len * 0.08f, cy), Offset(x1 - c * 0.7f, cy), c * 0.05f, pathEffect = dash)
    drawLine(p.light, Offset(x0 + len * 0.1f, cy - hw * 0.55f), Offset(x1 - c * 0.9f, cy - hw * 0.55f), c * 0.03f)
    // aircraft parked on the deck
    for (k in 0 until 3) {
        val px = x0 + len * (0.16f + 0.13f * k)
        val py = cy - hw * 0.28f
        drawLine(p.dark, Offset(px - c * 0.17f, py), Offset(px + c * 0.17f, py), c * 0.07f, StrokeCap.Round)
        drawLine(p.dark, Offset(px, py - c * 0.17f), Offset(px, py + c * 0.17f), c * 0.05f, StrokeCap.Round)
        drawLine(p.dark, Offset(px - c * 0.15f, py - c * 0.07f), Offset(px - c * 0.15f, py + c * 0.07f), c * 0.04f, StrokeCap.Round)
    }
    // island on the starboard edge
    val ix = x0 + len * 0.58f
    drawRoundRect(p.dark, Offset(ix - c * 0.5f, cy + hw * 0.18f), Size(c * 1.0f, hw * 0.78f), CornerRadius(c * 0.1f))
    drawRoundRect(p.light, Offset(ix - c * 0.42f, cy + hw * 0.28f), Size(c * 0.84f, hw * 0.58f), CornerRadius(c * 0.08f))
    drawLine(p.dark, Offset(ix, cy + hw * 0.55f), Offset(ix + c * 0.3f, cy + hw * 0.55f), c * 0.05f, StrokeCap.Round)
    if (!sunk) drawCircle(p.accent, c * 0.05f, Offset(x0 + len * 0.04f, cy))
}

private fun DrawScope.battleship(x0: Float, x1: Float, cy: Float, c: Float, p: ShipPalette) {
    val hw = c * 0.36f
    val len = x1 - x0
    hull(x0, x1, cy, hw, c * 1.0f, p)
    turret(x0 + len * 0.76f, cy, c * 0.17f, 2, p)
    turret(x0 + len * 0.63f, cy, c * 0.17f, 2, p)
    funnel(x0 + len * 0.36f, cy, c * 0.1f, p)
    bridge(x0 + len * 0.5f, cy, c * 0.5f, hw * 1.05f, p)
    turret(x0 + len * 0.2f, cy, c * 0.15f, 2, p)
}

private fun DrawScope.cruiser(x0: Float, x1: Float, cy: Float, c: Float, p: ShipPalette) {
    val hw = c * 0.32f
    val len = x1 - x0
    hull(x0, x1, cy, hw, c * 0.9f, p)
    turret(x0 + len * 0.76f, cy, c * 0.15f, 2, p)
    bridge(x0 + len * 0.52f, cy, c * 0.5f, hw * 1.0f, p)
    funnel(x0 + len * 0.34f, cy, c * 0.09f, p)
    turret(x0 + len * 0.17f, cy, c * 0.13f, 1, p)
}

private fun DrawScope.submarine(x0: Float, x1: Float, cy: Float, c: Float, p: ShipPalette) {
    val hw = c * 0.27f
    val len = x1 - x0
    drawRoundRect(Sea.Shadow, Offset(x0 + hw * 0.1f, cy - hw + hw * 0.2f), Size(len, hw * 2f), CornerRadius(hw))
    drawRoundRect(p.hull, Offset(x0, cy - hw), Size(len, hw * 2f), CornerRadius(hw))
    drawRoundRect(p.deck, Offset(x0 + hw * 0.5f, cy - hw * 0.55f), Size(len - hw * 1.2f, hw * 0.5f), CornerRadius(hw * 0.25f))
    // propeller guard at the stern and a deck gun
    drawLine(p.dark, Offset(x0, cy - hw * 0.5f), Offset(x0 - c * 0.06f, cy - hw * 0.5f), c * 0.05f, StrokeCap.Round)
    drawLine(p.dark, Offset(x0, cy + hw * 0.5f), Offset(x0 - c * 0.06f, cy + hw * 0.5f), c * 0.05f, StrokeCap.Round)
    drawCircle(p.dark, c * 0.07f, Offset(x0 + len * 0.76f, cy))
    // conning tower with periscope
    val tx = x0 + len * 0.46f
    drawRoundRect(p.dark, Offset(tx - c * 0.34f, cy - hw * 0.75f), Size(c * 0.68f, hw * 1.5f), CornerRadius(hw * 0.7f))
    drawRoundRect(p.light, Offset(tx - c * 0.28f, cy - hw * 0.55f), Size(c * 0.56f, hw * 1.1f), CornerRadius(hw * 0.5f))
    drawLine(p.dark, Offset(tx + c * 0.05f, cy), Offset(tx + c * 0.3f, cy), c * 0.04f, StrokeCap.Round)
}

private fun DrawScope.destroyer(x0: Float, x1: Float, cy: Float, c: Float, p: ShipPalette) {
    val hw = c * 0.27f
    val len = x1 - x0
    hull(x0, x1, cy, hw, c * 0.85f, p)
    turret(x0 + len * 0.7f, cy, c * 0.12f, 1, p)
    bridge(x0 + len * 0.47f, cy, c * 0.34f, hw * 1.0f, p)
    funnel(x0 + len * 0.28f, cy, c * 0.07f, p)
    drawCircle(p.dark, c * 0.07f, Offset(x0 + len * 0.13f, cy))
}

/** Foam curling off the bow of a ship that is still afloat. */
private fun DrawScope.bowWave(x1: Float, cy: Float, c: Float) {
    drawLine(Sea.Foam, Offset(x1 - c * 0.5f, cy - c * 0.42f), Offset(x1 - c * 0.05f, cy - c * 0.1f), c * 0.04f, StrokeCap.Round)
    drawLine(Sea.Foam, Offset(x1 - c * 0.5f, cy + c * 0.42f), Offset(x1 - c * 0.05f, cy + c * 0.1f), c * 0.04f, StrokeCap.Round)
}
