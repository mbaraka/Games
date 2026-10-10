package com.baraka.game.tunnelcrew.systems

import android.graphics.Bitmap
import com.baraka.game.tunnelcrew.model.CrewTheme

const val WORLD_W = 320
const val WORLD_H = 176

object Mat {
    const val AIR: Byte = 0
    const val DIRT: Byte = 1
    const val STEEL: Byte = 2
    const val BRICK: Byte = 3
}

private const val STEEL_BASE = 0xFF9AA8BA.toInt()
private const val STEEL_DARK = 0xFF55637A.toInt()
private const val STEEL_LIGHT = 0xFFD2DAE6.toInt()
private const val BRICK_BASE = 0xFFE0903A.toInt()
private const val BRICK_MORTAR = 0xFFB9652A.toInt()
private const val BRICK_RIM = 0xFF7A3E17.toInt()

/**
 * The destructible level: one material byte per pixel plus a mirrored ARGB bitmap that is only
 * re-coloured and uploaded for the rectangle that changed since the last [flush].
 * Everything here works on preallocated arrays, so digging allocates nothing per tick.
 */
class Terrain(private val theme: CrewTheme) {
    private val cell = ByteArray(WORLD_W * WORLD_H)
    private val pixels = IntArray(WORLD_W * WORLD_H)
    val bitmap: Bitmap = Bitmap.createBitmap(WORLD_W, WORLD_H, Bitmap.Config.ARGB_8888)

    private var dx0 = 0
    private var dy0 = 0
    private var dx1 = WORLD_W - 1
    private var dy1 = WORLD_H - 1
    private var dirty = true

    fun solid(x: Int, y: Int): Boolean =
        x >= 0 && x < WORLD_W && y >= 0 && y < WORLD_H && cell[y * WORLD_W + x].toInt() != 0

    fun isSteel(x: Int, y: Int): Boolean =
        x >= 0 && x < WORLD_W && y >= 0 && y < WORLD_H && cell[y * WORLD_W + x] == Mat.STEEL

    private fun mark(x: Int, y: Int) {
        if (x < dx0) dx0 = x
        if (x > dx1) dx1 = x
        if (y < dy0) dy0 = y
        if (y > dy1) dy1 = y
        dirty = true
    }

    /** Level-building helper: paints a rectangle of [material] (AIR clears even steel). */
    fun fillRect(x: Int, y: Int, w: Int, h: Int, material: Byte) {
        val xa = maxOf(0, x)
        val xb = minOf(WORLD_W, x + w)
        val ya = maxOf(0, y)
        val yb = minOf(WORLD_H, y + h)
        for (yy in ya until yb) for (xx in xa until xb) cell[yy * WORLD_W + xx] = material
        if (xa < xb && ya < yb) {
            mark(xa, ya)
            mark(xb - 1, yb - 1)
        }
    }

    fun dirt(x: Int, y: Int, w: Int, h: Int) = fillRect(x, y, w, h, Mat.DIRT)
    fun steel(x: Int, y: Int, w: Int, h: Int) = fillRect(x, y, w, h, Mat.STEEL)
    fun air(x: Int, y: Int, w: Int, h: Int) = fillRect(x, y, w, h, Mat.AIR)

    fun hasSteel(x: Int, y: Int, w: Int, h: Int): Boolean {
        for (yy in maxOf(0, y) until minOf(WORLD_H, y + h)) {
            for (xx in maxOf(0, x) until minOf(WORLD_W, x + w)) {
                if (cell[yy * WORLD_W + xx] == Mat.STEEL) return true
            }
        }
        return false
    }

    fun discHasSteel(cx: Int, cy: Int, r: Int): Boolean {
        val rr = r * r
        for (yy in maxOf(0, cy - r)..minOf(WORLD_H - 1, cy + r)) {
            for (xx in maxOf(0, cx - r)..minOf(WORLD_W - 1, cx + r)) {
                val dx = xx - cx
                val dy = yy - cy
                if (dx * dx + dy * dy <= rr && cell[yy * WORLD_W + xx] == Mat.STEEL) return true
            }
        }
        return false
    }

    /** Removes every non-steel pixel in the rectangle; returns how many were removed. */
    fun carveRect(x: Int, y: Int, w: Int, h: Int): Int {
        var removed = 0
        for (yy in maxOf(0, y) until minOf(WORLD_H, y + h)) {
            for (xx in maxOf(0, x) until minOf(WORLD_W, x + w)) {
                val idx = yy * WORLD_W + xx
                val m = cell[idx]
                if (m != Mat.AIR && m != Mat.STEEL) {
                    cell[idx] = Mat.AIR
                    removed++
                    mark(xx, yy)
                }
            }
        }
        return removed
    }

    fun carveDisc(cx: Int, cy: Int, r: Int): Int {
        var removed = 0
        val rr = r * r
        for (yy in maxOf(0, cy - r)..minOf(WORLD_H - 1, cy + r)) {
            for (xx in maxOf(0, cx - r)..minOf(WORLD_W - 1, cx + r)) {
                val dx = xx - cx
                val dy = yy - cy
                if (dx * dx + dy * dy > rr) continue
                val idx = yy * WORLD_W + xx
                val m = cell[idx]
                if (m != Mat.AIR && m != Mat.STEEL) {
                    cell[idx] = Mat.AIR
                    removed++
                    mark(xx, yy)
                }
            }
        }
        return removed
    }

    /** Places one brick pixel if the spot is free. */
    fun brick(x: Int, y: Int) {
        if (x < 0 || x >= WORLD_W || y < 0 || y >= WORLD_H) return
        val idx = y * WORLD_W + x
        if (cell[idx] == Mat.AIR) {
            cell[idx] = Mat.BRICK
            mark(x, y)
        }
    }

    private fun airAt(x: Int, y: Int): Boolean =
        x < 0 || x >= WORLD_W || y < 0 || y >= WORLD_H || cell[y * WORLD_W + x] == Mat.AIR

    /** Paint for one pixel: themed dirt with depth shading and pebbles, grass on top, darker rim against open air. */
    private fun colorAt(x: Int, y: Int): Int {
        val m = cell[y * WORLD_W + x]
        if (m == Mat.AIR) return 0
        val h = x * 374761393 + y * 668265263
        val n = (h xor (h ushr 13)) and 3
        val edge = airAt(x - 1, y) || airAt(x + 1, y) || airAt(x, y + 1) || (y > 0 && airAt(x, y - 1))
        return when (m) {
            Mat.DIRT -> when {
                airAt(x, y - 1) || airAt(x, y - 2) -> if (airAt(x, y - 1)) theme.grass[n] else shade(theme.grass[n], 0.88f)
                edge -> theme.rim
                else -> {
                    var depth = 0
                    while (depth < 6 && !airAt(x, y - 3 - depth)) depth++
                    val base = blend(theme.dirt[n], theme.dirtDeep, depth / 7f)
                    if (((h ushr 5) and 31) == 0) shade(base, 0.8f) else base
                }
            }
            Mat.STEEL -> when {
                edge -> STEEL_DARK
                (x and 7) == 1 && (y and 7) == 1 -> STEEL_DARK
                (y and 7) == 0 -> STEEL_LIGHT
                else -> STEEL_BASE
            }
            else -> if (edge) BRICK_RIM else if ((y and 1) == 0) BRICK_MORTAR else BRICK_BASE
        }
    }

    private fun shade(c: Int, f: Float): Int {
        val r = ((c shr 16 and 0xFF) * f).toInt()
        val g = ((c shr 8 and 0xFF) * f).toInt()
        val b = ((c and 0xFF) * f).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val r = ((a shr 16 and 0xFF) * (1 - t) + (b shr 16 and 0xFF) * t).toInt()
        val g = ((a shr 8 and 0xFF) * (1 - t) + (b shr 8 and 0xFF) * t).toInt()
        val bl = ((a and 0xFF) * (1 - t) + (b and 0xFF) * t).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    /** Re-colours and uploads only the region that changed since the last call. */
    fun flush() {
        if (!dirty) return
        val x0 = maxOf(0, dx0 - 1)
        val x1 = minOf(WORLD_W - 1, dx1 + 1)
        val y0 = maxOf(0, dy0 - 2)
        val y1 = minOf(WORLD_H - 1, dy1 + 10)
        for (yy in y0..y1) for (xx in x0..x1) pixels[yy * WORLD_W + xx] = colorAt(xx, yy)
        bitmap.setPixels(pixels, y0 * WORLD_W + x0, WORLD_W, x0, y0, x1 - x0 + 1, y1 - y0 + 1)
        dx0 = WORLD_W
        dy0 = WORLD_H
        dx1 = -1
        dy1 = -1
        dirty = false
    }
}
