package com.baraka.game.basedefender.systems

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.baraka.game.basedefender.model.Tuning

/** Particles and rings in flat, pre-sized arrays: spawning and updating allocate nothing. */
class Particles {
    val cap = Tuning.MAX_PARTICLES
    var count = 0
        private set
    val x = FloatArray(cap)
    val y = FloatArray(cap)
    val vx = FloatArray(cap)
    val vy = FloatArray(cap)
    val size = FloatArray(cap)
    val life = IntArray(cap)
    val maxLife = IntArray(cap)
    val color = IntArray(cap)
    /** true = expanding ring, false = dot. */
    val ring = BooleanArray(cap)

    private var seed = 12345

    private fun rnd(): Float {
        seed = seed * 1103515245 + 12345
        return ((seed ushr 8) and 0xFFFF) / 65535f
    }

    private fun add(px: Float, py: Float, pvx: Float, pvy: Float, psize: Float, plife: Int, c: Color, isRing: Boolean) {
        if (count >= cap) return
        val i = count++
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy
        size[i] = psize; life[i] = plife; maxLife[i] = plife
        color[i] = c.toArgb(); ring[i] = isRing
    }

    fun burst(px: Float, py: Float, c: Color, n: Int, speed: Float, psize: Float) {
        for (k in 0 until n) {
            val a = rnd() * 6.2831855f
            val s = speed * (0.35f + rnd() * 0.65f)
            add(px, py, kotlin.math.cos(a) * s, kotlin.math.sin(a) * s, psize * (0.6f + rnd() * 0.8f), 24 + (rnd() * 22).toInt(), c, false)
        }
    }

    fun spark(px: Float, py: Float, dx: Float, dy: Float, c: Color) {
        add(px, py, dx * 1.5f + (rnd() - 0.5f), dy * 1.5f + (rnd() - 0.5f), 2f, 10, c, false)
    }

    fun ring(px: Float, py: Float, c: Color, radius: Float, ticks: Int = 18) {
        add(px, py, 0f, 0f, radius, ticks, c, true)
    }

    fun clear() { count = 0 }

    fun update() {
        var i = 0
        while (i < count) {
            life[i]--
            if (life[i] <= 0) {
                val last = --count
                x[i] = x[last]; y[i] = y[last]; vx[i] = vx[last]; vy[i] = vy[last]
                size[i] = size[last]; life[i] = life[last]; maxLife[i] = maxLife[last]
                color[i] = color[last]; ring[i] = ring[last]
                continue
            }
            x[i] += vx[i]
            y[i] += vy[i]
            if (!ring[i]) vy[i] += 0.03f
            i++
        }
    }
}

/** Floating numbers ("+25", "-12"). The strings are cached by the caller, so this allocates nothing. */
class FloatTexts {
    val cap = Tuning.MAX_TEXTS
    var count = 0
        private set
    val text = arrayOfNulls<String>(cap)
    val x = FloatArray(cap)
    val y = FloatArray(cap)
    val life = IntArray(cap)
    val maxLife = IntArray(cap)
    val color = IntArray(cap)
    val big = BooleanArray(cap)

    fun add(s: String, px: Float, py: Float, c: Color, isBig: Boolean = false) {
        if (count >= cap) return
        val i = count++
        text[i] = s; x[i] = px; y[i] = py; life[i] = 40; maxLife[i] = 40
        color[i] = c.toArgb(); big[i] = isBig
    }

    fun clear() { count = 0 }

    fun update() {
        var i = 0
        while (i < count) {
            life[i]--
            if (life[i] <= 0) {
                val last = --count
                text[i] = text[last]; x[i] = x[last]; y[i] = y[last]
                life[i] = life[last]; maxLife[i] = maxLife[last]; color[i] = color[last]; big[i] = big[last]
                continue
            }
            y[i] -= 0.7f
            i++
        }
    }
}
