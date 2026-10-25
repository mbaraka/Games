package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.Zombie
import kotlin.math.cos
import kotlin.math.sin

// The railway enemies: the Rail Hopper and Signalman (humanoids with gear), the Demolisher boss (a hard-hatted giant) and the
// Runaway Engine boss (a ghost locomotive). See Biome.SWITCHYARD and Biome.BROKEN_VIADUCT.

internal fun registerRailMobs() {
    val l = MobLooks
    l.set(ZombieType.HOPPER, 0xFF86B864, 0xFFFF8F00)
    l.set(ZombieType.SIGNALMAN, 0xFFB7C48E, 0xFF263238)
    l.set(ZombieType.DEMOLISHER, 0xFF86B864, 0xFF616161)

    HumanoidExtras.register(ZombieType.HOPPER) { s, z, headY, bob, _, _ ->
        with(s) {
            for (k in 0..1) box(Color(0xFFFFF59D), -8f, -34f + k * 7f + bob, 16f, 2.5f) // hi-vis stripes
            box(Color(0xFF263238), -6f, headY - 12f, 14f, 6f) // cap
            box(Color(0xFF263238), 6f, headY - 8f, 8f, 2f)
            val swing = sin(z.anim * 2f) * 3f
            drawLine(Color(0xFF90A4AE), Offset(14f, -30f + bob), Offset(24f, -38f + bob + swing), 2.4f, cap = StrokeCap.Round) // grappling hook
            drawArc(Color(0xFF90A4AE), 200f, 220f, false, Offset(20f, -46f + bob + swing), Size(10f, 10f), style = Stroke(2f))
        }
    }
    HumanoidExtras.register(ZombieType.SIGNALMAN) { s, z, headY, bob, _, _ ->
        with(s) {
            box(Color(0xFF263238), -7f, headY - 13f, 15f, 6f) // station cap
            box(Color(0xFFFFC107), -7f, headY - 9f, 15f, 2f)
            val sway = sin(z.anim * 1.5f) * 3f
            drawLine(Color(0xFF8D6E63), Offset(14f, -30f + bob), Offset(20f + sway, -18f + bob), 2f) // lantern
            val glow = if (sin(z.anim * 3f) > 0f) Color(0xFFFF5252) else Color(0xFF69F0AE)
            drawCircle(glow.copy(alpha = 0.35f), 9f, Offset(20f + sway, -14f + bob))
            drawCircle(glow, 4f, Offset(20f + sway, -14f + bob))
            for (k in 0..2) box(Color(0xFFFFC107), -6f + k * 5f, -22f + bob, 3f, 3f) // brass buttons
        }
    }
    HumanoidExtras.register(ZombieType.DEMOLISHER) { s, z, headY, bob, _, _ ->
        with(s) {
            path.rewind(); path.moveTo(-9f, headY - 5f); path.lineTo(2f, headY - 17f); path.lineTo(13f, headY - 5f); path.close()
            drawPath(path, Color(0xFFFF8F00)) // hard hat
            box(Color(0xFFFFB74D), -10f, headY - 5f, 24f, 2.5f)
            for (k in 0..2) drawLine(Color(0xFF8D5A3A), Offset(-12f + k * 3f, -38f + bob), Offset(-16f + k * 3f, -62f + bob), 2.2f) // rebar bundle
            box(Color(0xFF9E9E9E), -9f, -20f + bob, 18f, 6f) // concrete-dusted belt
        }
    }
    MobArtRegistry.register(ZombieType.RUNAWAY_ENGINE) { s, z, d -> with(s) { drawGhostEngine(z, d) } }
    PortraitSizes.set(ZombieType.RUNAWAY_ENGINE, 190f)
    PortraitSizes.set(ZombieType.DEMOLISHER, 190f)
}

/** The Runaway Engine: a translucent ghost locomotive with angry eyes on its boiler, spinning glowing wheels and a trail of steam. */
private fun DrawScope.drawGhostEngine(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val body = lerp(Color(0xFF6A7F96), Color.White, flash).copy(alpha = 0.86f)
    val dark = lerp(Color(0xFF37475A), Color.White, flash).copy(alpha = 0.9f)
    val glow = 0.6f + 0.4f * sin(z.anim * 2f)
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(z.x - 40f * k, z.y - 3f * k), Size(80f * k, 7f * k))
    withTransform({ translate(z.x, z.y); scale(k * dir, k, Offset.Zero) }) {
        rbox(dark, -38f, -22f, 74f, 8f, 3f) // frame
        rbox(body, -34f, -46f, 54f, 26f, 12f) // boiler
        box(body, -36f, -66f, 24f, 46f) // cab
        box(dark, -38f, -70f, 28f, 6f)
        box(Color(0xFFB3E5FC).copy(alpha = 0.7f), -32f, -58f, 12f, 12f) // cab window
        box(dark, 8f, -64f, 9f, 18f) // chimney
        box(dark, 5f, -68f, 15f, 5f)
        for (i in 0..2) { // steam
            val t = ((z.anim * 0.4f + i * 0.33f) % 1f)
            drawCircle(Color.White.copy(alpha = 0.35f * (1f - t)), 5f + t * 12f, Offset(12f - t * 28f, -72f - t * 30f))
        }
        path.rewind(); path.moveTo(34f, -22f); path.lineTo(48f, 0f); path.lineTo(32f, 0f); path.close()
        drawPath(path, dark) // cowcatcher
        drawCircle(Color(0xFFFFF59D).copy(alpha = 0.4f * glow), 14f, Offset(30f, -34f))
        drawCircle(Color(0xFFFFF59D), 5f, Offset(30f, -34f)) // headlamp
        drawCircle(Color(0xFFFF1744), 3.4f, Offset(6f, -38f)) // angry eyes
        drawCircle(Color(0xFFFF1744), 3.4f, Offset(18f, -38f))
        drawLine(Color(0xFF1A237E), Offset(2f, -44f), Offset(10f, -41f), 2.2f)
        drawLine(Color(0xFF1A237E), Offset(22f, -44f), Offset(14f, -41f), 2.2f)
        for (w in 0..3) {
            val wx = -24f + w * 18f
            drawCircle(dark, 9f, Offset(wx, -9f))
            drawCircle(Color(0xFF80DEEA).copy(alpha = 0.8f * glow), 9f, Offset(wx, -9f), style = Stroke(1.8f))
            val a = z.anim * 3f + w
            drawLine(Color(0xFF80DEEA), Offset(wx, -9f), Offset(wx + cos(a) * 8f, -9f + sin(a) * 8f), 1.6f)
        }
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}
