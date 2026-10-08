package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.trainsurvivor.systems.HullBreaches
import com.baraka.game.trainsurvivor.systems.LavaGeysers
import com.baraka.game.trainsurvivor.systems.MeteorShower
import com.baraka.game.trainsurvivor.systems.OverpassDrops
import com.baraka.game.trainsurvivor.systems.Tentacles
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws every active hazard (see systems/Hazards.kt). The `when` is exhaustive on purpose: adding a hazard class
 * makes this fail to compile until it has art, so a hazard can never ship invisible.
 */
internal fun DrawScope.drawHazards(g: TrainGame) {
    for (i in g.hazards.indices) {
        when (val h = g.hazards[i]) {
            is MeteorShower -> drawMeteors(h, g)
            is LavaGeysers -> drawGeysers(h, g)
            is HullBreaches -> drawBreach(h, g)
            is OverpassDrops -> drawOverpass(h, g)
            is Tentacles -> {} // tentacles are ordinary enemies; see MobArts.kt
        }
    }
}

private fun DrawScope.drawMeteors(h: MeteorShower, g: TrainGame) {
    for (i in h.meteors.indices) {
        val m = h.meteors[i]
        if (!m.falling) {
            // Warning marker over the car about to be hit.
            val pulse = 0.4f + 0.4f * abs(sin(g.time * 10f))
            drawCircle(Color(0xFFFF1744).copy(alpha = pulse), 34f, Offset(m.x, 404f), style = Stroke(3f))
            outlinedText("!", m.x, 412f, 30f, 0xFFFF5252.toInt())
        } else {
            drawLine(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFFF9800)), startY = m.y - 130f, endY = m.y), Offset(m.x - 14f, m.y - 130f), Offset(m.x, m.y), 10f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFAB40).copy(alpha = 0.7f), Color.Transparent), center = Offset(m.x, m.y), radius = 40f), 40f, Offset(m.x, m.y))
            drawCircle(Color(0xFF6D4C41), 16f, Offset(m.x, m.y))
            drawCircle(Color(0xFF3E2723), 7f, Offset(m.x + 4f, m.y + 3f))
            if (m.y > 60f && m.y < 300f) outlinedText("TAP!", m.x, m.y - 28f, 16f, 0xFFFFD54F.toInt())
        }
    }
}

private fun DrawScope.drawGeysers(h: LavaGeysers, g: TrainGame) {
    for (i in h.geysers.indices) {
        val gz = h.geysers[i]
        if (gz.telegraph > 0f) {
            val pulse = 0.3f + 0.4f * abs(sin(g.time * 9f))
            drawOval(Color(0xFFFF6D00).copy(alpha = pulse), Offset(gz.x - 50f, 548f), Size(100f, 26f))
            drawLine(Color(0xFFFFAB00).copy(alpha = pulse + 0.2f), Offset(gz.x - 22f, 562f), Offset(gz.x + 20f, 558f), 3f)
        } else {
            val rise = min(1f, (0.6f - gz.erupt) / 0.15f + 0.3f).coerceIn(0.3f, 1f)
            val top = 575f - 220f * rise
            path.rewind()
            path.moveTo(gz.x - 30f, 575f); path.lineTo(gz.x - 12f, top); path.lineTo(gz.x + 12f, top); path.lineTo(gz.x + 30f, 575f); path.close()
            drawPath(path, Color(0xFFFF5722).copy(alpha = 0.85f))
            path.rewind()
            path.moveTo(gz.x - 14f, 575f); path.lineTo(gz.x - 5f, top + 24f); path.lineTo(gz.x + 5f, top + 24f); path.lineTo(gz.x + 14f, 575f); path.close()
            drawPath(path, Color(0xFFFFEB3B).copy(alpha = 0.9f))
            for (k in 0..4) drawCircle(Color(0xFFFFAB00), 3f, Offset(gz.x + sin(g.time * 14f + k * 2f) * 34f, top + k * 32f))
        }
    }
}

private fun DrawScope.drawBreach(h: HullBreaches, g: TrainGame) {
    val b = h.breach ?: return
    val cx = g.carCenterX(b.slot)
    val pulse = 0.45f + 0.45f * abs(sin(g.time * 8f))
    drawCircle(Color(0xFFFF1744).copy(alpha = pulse), 52f, Offset(cx, 452f), style = Stroke(4f))
    for (k in 0..4) { // air hissing out
        val a = (g.time * 3f + k * 0.37f) % 1f
        drawLine(Color.White.copy(alpha = 0.7f * (1f - a)), Offset(cx + k * 9f - 18f, 430f - a * 60f), Offset(cx + k * 9f - 18f + 10f, 420f - a * 80f), 2f)
    }
    outlinedText("TAP TO SEAL " + ceil(b.timeLeft).toInt(), cx, 366f, 16f, 0xFFFF5252.toInt())
}

private fun DrawScope.drawOverpass(h: OverpassDrops, g: TrainGame) {
    if (h.overpassTime <= 0f) return
    val a = min(1f, h.overpassTime * 1.5f)
    box(Color(0xFF5B5B63).copy(alpha = a), -200f, 232f, g.worldW + 400f, 44f)
    box(Color(0xFF3B3B42).copy(alpha = a), -200f, 268f, g.worldW + 400f, 10f)
    var x = -120f
    while (x < g.worldW + 120f) {
        drawLine(Color(0xFF2B2B30).copy(alpha = a), Offset(x, 236f), Offset(x + 30f, 270f), 2f)
        x += 70f
    }
    box(Color(0xFFFFC107).copy(alpha = 0.8f * a), -200f, 232f, g.worldW + 400f, 4f)
}
