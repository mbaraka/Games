package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.sin


private const val GHOST_SCALE = 0.5f
private const val GHOST_GROUND = 438f

/**
 * Draws the partner's train on a second, parallel track (co-op routes only). It is built from what the partner sends:
 * their train shape and paint, each wagon's health, which wagons are destroyed and which guns just fired. It is a simple
 * stand-in rather than the full art, so it stays cheap to draw and to stream.
 */
internal fun DrawScope.drawGhostTrain(g: TrainGame) {
    val ghost = g.ghost
    if (!ghost.active || !g.biome.coopOnly) return
    val shape = ghost.shape
    val livery = ghost.livery
    val count = ghost.count
    val len = shape.locoW + (count - 1) * (shape.carW + shape.carGap)
    val left = (g.worldW * 0.58f) - len * GHOST_SCALE / 2f
    val away = ghost.away

    // The far track: a rail with sleepers across the whole view.
    drawLine(Color(0xFF263238), Offset(-50f, GHOST_GROUND + 2f), Offset(g.worldW + 50f, GHOST_GROUND + 2f), 3f)
    var tx = -((g.scroll * 0.55f) % 24f)
    while (tx < g.worldW + 24f) {
        box(Color(0xFF3E2F28), tx, GHOST_GROUND + 3f, 8f, 3f)
        tx += 24f
    }

    withTransform({ translate(left, GHOST_GROUND); scale(GHOST_SCALE, GHOST_SCALE, Offset.Zero) }) {
        val bob = sin(g.time * 7f) * 0.8f
        // Chassis and wheels
        box(Color(0xFF1A1A1F), 0f, -14f + bob, len, 6f)
        // Locomotive
        val loco = ghost.hp[0]
        val locoW = shape.locoW
        rbox(Color(livery.boiler), 56f, -64f + bob, locoW - 56f, 46f, 14f)
        box(Color(livery.boilerHi), 60f, -60f + bob, locoW - 64f, 8f)
        box(Color(livery.trim), locoW - 34f, -78f + bob, 12f, 16f)
        rbox(Color(livery.cab), 0f, -86f + bob, 62f, 72f, 5f)
        box(Color(livery.cabDark), -3f, -92f + bob, 68f, 8f)
        box(Color(0xFF9BC4E2), 14f, -72f + bob, 28f, 20f)
        drawCircle(Color(0xFFFFF3B0), 6f, Offset(locoW + 3f, -44f + bob))
        if (loco < 0.5f) drawCircle(Color(0xFFFF5252).copy(alpha = (0.5f - loco) * 0.9f), 40f, Offset(locoW / 2f, -50f + bob))
        hpBar(0f, locoW, loco, bob)
        gunner(ghost.flash[0], 36f, -86f + bob)
        for (k in 0..3) wheel(30f + k * ((locoW - 60f) / 3f), bob)

        // Wagons
        for (i in 1 until count) {
            val x = locoW + shape.carGap + (i - 1) * (shape.carW + shape.carGap)
            val hp = ghost.hp[i]
            val alive = hp > 0f
            val body = if (alive) Color(livery.cars[(i - 1) % livery.cars.size]) else Color(0xFF2B2522)
            rbox(body, x, -66f + bob, shape.carW, 52f, 6f)
            box(Color(0xFF1A1A1F), x, -70f + bob, shape.carW, 5f)
            if (alive) {
                for (w in 0..2) box(Color(0xFF9BC4E2).copy(alpha = 0.8f), x + 10f + w * ((shape.carW - 20f) / 3f), -56f + bob, (shape.carW - 20f) / 3f - 6f, 14f)
                if (hp < 0.5f) drawCircle(Color(0xFFFF5252).copy(alpha = (0.5f - hp) * 0.9f), 34f, Offset(x + shape.carW / 2f, -40f + bob))
                hpBar(x, shape.carW, hp, bob)
                gunner(ghost.flash[i], x + shape.carW / 2f, -70f + bob)
            } else {
                drawCircle(Color(0xFF455A64).copy(alpha = 0.5f), 12f, Offset(x + shape.carW / 2f - 8f, -80f + bob - (g.time * 30f) % 30f))
            }
            wheel(x + 24f, bob)
            wheel(x + shape.carW - 24f, bob)
        }
        outlinedText(if (away) "PARTNER (no signal)" else "PARTNER", len / 2f, -112f, 28f, if (away) 0xFFFF8A80.toInt() else 0xFF80DEEA.toInt())
    }
}

private fun DrawScope.wheel(x: Float, bob: Float) {
    drawCircle(Color(0xFF111418), 11f, Offset(x, -9f + bob))
    drawCircle(Color(0xFF78909C), 4f, Offset(x, -9f + bob))
}

private fun DrawScope.hpBar(x: Float, w: Float, hp: Float, bob: Float) {
    box(Color(0xFF263238), x + 4f, -98f + bob, w - 8f, 7f)
    box(if (hp > 0.4f) Color(0xFF66BB6A) else Color(0xFFEF5350), x + 4f, -98f + bob, (w - 8f) * hp.coerceIn(0f, 1f), 7f)
}

/** A gunner's head on the roof; a bright flash and a streak when that gun has just fired. */
private fun DrawScope.gunner(flash: Float, x: Float, y: Float) {
    drawCircle(Color(0xFFE0AC69), 7f, Offset(x, y - 10f))
    box(Color(0xFF455A64), x - 6f, y - 4f, 12f, 8f)
    if (flash > 0f) {
        drawCircle(Color(0xFFFFEB3B).copy(alpha = flash), 11f * flash + 4f, Offset(x + 16f, y - 8f))
        drawLine(Color(0xFFFFF59D).copy(alpha = flash), Offset(x + 16f, y - 8f), Offset(x + 70f, y - 40f - 30f * (1f - flash)), 3f)
    }
}
