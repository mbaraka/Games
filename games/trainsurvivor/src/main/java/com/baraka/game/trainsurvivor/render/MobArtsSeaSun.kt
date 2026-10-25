package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

// Enemy art for the Deep Sea Tube and Solar Rail routes (registered from registerMobArts in MobArts.kt).

internal fun registerSeaAndSunMobs() {
    val l = MobLooks
    l.set(ZombieType.PIRANHA, 0xFF5E7B6A, 0xFF4A6355)
    l.set(ZombieType.JELLYFISH, 0xFFF48FB1, 0xFFE91E63)
    l.set(ZombieType.DEEP_CRAB, 0xFFD84315, 0xFFBF360C)
    l.set(ZombieType.EMBER, 0xFF3B2A25, 0xFF2B1D1A)
    l.set(ZombieType.SOLAR_SPRITE, 0xFFFFCA28, 0xFFFF6F00)
    l.set(ZombieType.SUN_PRIEST, 0xFFE0A070, 0xFFFFC107)

    l.set(ZombieType.SHARK, 0xFF607D8B, 0xFF455A64)
    l.set(ZombieType.LEAPING_SHARK, 0xFF607D8B, 0xFF455A64)
    MobArtRegistry.register(listOf(ZombieType.SHARK, ZombieType.LEAPING_SHARK)) { s, z, d -> with(s) { drawShark(z, d) } }
    MobArtRegistry.register(ZombieType.PIRANHA) { s, z, d -> with(s) { drawPiranha(z, d) } }
    MobArtRegistry.register(ZombieType.JELLYFISH) { s, z, _ -> with(s) { drawJellyfish(z) } }
    MobArtRegistry.register(ZombieType.DEEP_CRAB) { s, z, d -> with(s) { drawDeepCrab(z, d) } }
    MobArtRegistry.register(ZombieType.SOLAR_SPRITE) { s, z, _ -> with(s) { drawSolarSprite(z) } }

    HumanoidExtras.register(ZombieType.EMBER) { s, z, headY, bob, _, _ ->
        with(s) {
            for (k in 0..2) { // flames on the head
                val tipX = -3f + k * 5f + sin(z.anim * 8f + k) * 2f
                val tipY = headY - 18f - (k % 2) * 4f
                path.rewind(); path.moveTo(-5f + k * 5f, headY - 6f); path.lineTo(tipX, tipY); path.lineTo(1f + k * 5f, headY - 6f); path.close()
                drawPath(path, Color(0xFFFF6D00))
                path.rewind(); path.moveTo(-3f + k * 5f, headY - 6f); path.lineTo(tipX, tipY + 5f); path.lineTo(-1f + k * 5f, headY - 6f); path.close()
                drawPath(path, Color(0xFFFFEB3B))
            }
            val glow = 0.6f + 0.4f * sin(z.anim * 3f)
            for (k in 0..3) drawLine(Color(0xFFFF6D00).copy(alpha = glow), Offset(-7f + k * 4f, -36f + bob), Offset(-3f + k * 3f, -16f + bob), 1.8f)
            drawCircle(Color(0xFFFFEB3B), 2.2f, Offset(6f, headY - 1.5f))
        }
    }
    HumanoidExtras.register(ZombieType.SUN_PRIEST) { s, z, headY, bob, _, _ ->
        with(s) {
            path.rewind(); path.moveTo(-8f, headY + 3f); path.lineTo(2f, headY - 19f); path.lineTo(12f, headY + 3f); path.close()
            drawPath(path, Color(0xFFB71C1C)) // hood
            box(Color(0xFFFFD54F), -8f, headY + 1f, 20f, 2f)
            box(Color(0xFFFFC107), -9f, -20f + bob, 18f, 8f) // golden hem
            drawLine(Color(0xFF8D6E63), Offset(16f, -2f), Offset(16f, -64f + bob), 3f, cap = StrokeCap.Round) // staff
            val pulse = 0.6f + 0.4f * sin(z.anim * 4f)
            drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.4f * pulse), 11f, Offset(16f, -66f + bob))
            drawCircle(Color(0xFFFFEB3B), 5f, Offset(16f, -66f + bob))
        }
    }
}

internal fun registerPortraitSizes() {
    val p = PortraitSizes
    p.set(ZombieType.BALLOON, 150f)
    p.set(ZombieType.TITAN, 150f)
    p.set(ZombieType.BAT, 48f, floating = 0.62f)
    p.set(ZombieType.GARGOYLE, 120f, floating = 0.75f)
    p.set(ZombieType.DRONE, 48f, floating = 0.62f)
    p.set(ZombieType.ZOMBIE_DOG, 55f)
    p.set(ZombieType.RAT, 40f)
    p.set(ZombieType.SCORPION, 75f)
    p.set(ZombieType.CROCODILE, 100f)
    p.set(ZombieType.FROST_WRAITH, 90f)
    p.set(ZombieType.TENTACLE, 110f)
    p.set(ZombieType.PIRANHA, 44f, floating = 0.62f)
    p.set(ZombieType.JELLYFISH, 90f)
    p.set(ZombieType.SHARK, 70f, floating = 0.6f)
    p.set(ZombieType.LEAPING_SHARK, 70f, floating = 0.6f)
    p.set(ZombieType.DEEP_CRAB, 70f)
    p.set(ZombieType.SOLAR_SPRITE, 48f, floating = 0.62f)
}

/** A zombie piranha: a fast, toothy flyer (it swims through the tube's water) that arrives in packs. */
internal fun DrawScope.drawPiranha(z: Zombie, dir: Float, k: Float = 1f) {
    val body = lerp(Color(0xFF5E7B6A), Color.White, z.flash * 0.75f)
    val tail = sin(z.anim * 1.5f) * 4f
    withTransform({ translate(z.x, z.y - 16f * k); scale(dir * k, k, Offset.Zero) }) {
        path.rewind(); path.moveTo(-10f, 0f); path.lineTo(-22f, -8f + tail); path.lineTo(-22f, 8f + tail); path.close()
        drawPath(path, body)
        drawOval(body, Offset(-12f, -9f), Size(26f, 18f))
        drawOval(Color(0xFFFF8A65), Offset(-8f, 1f), Size(20f, 7f))
        path.rewind(); path.moveTo(-4f, -9f); path.lineTo(2f, -15f); path.lineTo(6f, -9f); path.close()
        drawPath(path, Color(0xFF3E5A4B))
        box(Color(0xFF263238), 8f, 0f, 8f, 3f)
        for (k in 0..2) {
            path.rewind(); path.moveTo(8f + k * 3f, 0f); path.lineTo(9.5f + k * 3f, -4f); path.lineTo(11f + k * 3f, 0f); path.close()
            drawPath(path, Color.White)
        }
        drawCircle(Color.White, 3f, Offset(5f, -3f))
        drawCircle(Color(0xFFD50000), 1.5f, Offset(6f, -3f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A jellyfish zombie: a translucent pulsing bell with trailing stinging tentacles. */
private fun DrawScope.drawJellyfish(z: Zombie) {
    val pulse = 1f + 0.1f * sin(z.anim * 3f)
    val bell = lerp(Color(0xFFF48FB1), Color.White, z.flash * 0.75f).copy(alpha = 0.82f)
    withTransform({ translate(z.x, z.y - 40f) }) {
        drawArc(bell, 180f, 180f, true, Offset(-18f, -22f * pulse), Size(36f, 30f * pulse))
        drawArc(Color.White.copy(alpha = 0.4f), 200f, 60f, false, Offset(-14f, -18f), Size(28f, 22f), style = Stroke(2f))
        for (k in -2..2) {
            var px = k * 7f
            var py = 8f
            for (i in 1..4) {
                val nx = k * 7f + sin(z.anim * 2f + i + k) * 4f
                val ny = 8f + i * 9f
                drawLine(Color(0xFFE91E63).copy(alpha = 0.75f), Offset(px, py), Offset(nx, ny), 2.2f)
                px = nx
                py = ny
            }
        }
        drawCircle(Color(0xFF880E4F), 2f, Offset(-6f, -4f))
        drawCircle(Color(0xFF880E4F), 2f, Offset(6f, -4f))
        drawLine(Color(0xFF880E4F), Offset(-9f, -8f), Offset(-3f, -6f), 1.6f)
        drawLine(Color(0xFF880E4F), Offset(9f, -8f), Offset(3f, -6f), 1.6f)
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A deep-sea crab: a low armored scuttler with a big raised claw. */
private fun DrawScope.drawDeepCrab(z: Zombie, dir: Float) {
    val size = z.type.size
    val flash = z.flash * 0.75f
    val shell = lerp(Color(0xFFD84315), Color.White, flash)
    val dark = lerp(Color(0xFF8E2A0F), Color.White, flash)
    drawOval(Color.Black.copy(alpha = 0.28f), Offset(z.x - 24f * size, z.y - 3f * size), Size(48f * size, 7f * size))
    withTransform({ translate(z.x, z.y); scale(size * dir, size, Offset.Zero) }) {
        val sc = sin(z.anim * 1.8f)
        for (k in 0..2) {
            val lx = -14f + k * 10f
            drawLine(dark, Offset(lx, -12f), Offset(lx - 8f + sc * 3f, 0f), 2f)
            drawLine(dark, Offset(lx + 4f, -12f), Offset(lx + 10f - sc * 3f, 0f), 2f)
        }
        drawOval(shell, Offset(-22f, -27f), Size(44f, 20f))
        drawOval(dark, Offset(-12f, -25f), Size(18f, 8f))
        drawLine(dark, Offset(10f, -24f), Offset(12f, -35f), 2f)
        drawCircle(Color.White, 3f, Offset(12f, -36f))
        drawCircle(Color.Black, 1.4f, Offset(13f, -36f))
        val raise = if (z.attacking) sin(z.anim * 3f) * 8f else 0f
        drawLine(dark, Offset(18f, -18f), Offset(32f, -26f + raise), 4f)
        drawOval(shell, Offset(28f, -36f + raise), Size(16f, 14f))
        path.rewind(); path.moveTo(38f, -34f + raise); path.lineTo(46f, -29f + raise); path.lineTo(38f, -26f + raise); path.close()
        drawPath(path, dark)
        drawLine(dark, Offset(12f, -14f), Offset(24f, -8f - raise), 3f)
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A solar sprite: a flickering ball of plasma that darts around erratically. Fire-proof. */
private fun DrawScope.drawSolarSprite(z: Zombie) {
    val flick = 1f + 0.12f * sin(z.anim * 14f)
    val core = lerp(Color(0xFFFFEB3B), Color.White, z.flash * 0.75f)
    withTransform({ translate(z.x, z.y - 18f) }) {
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFFFF59D).copy(alpha = 0.9f), Color(0xFFFF9800).copy(alpha = 0.4f), Color.Transparent), center = Offset.Zero, radius = 26f * flick),
            26f * flick, Offset.Zero
        )
        drawCircle(core, 9f, Offset.Zero)
        for (k in 0 until 6) {
            val a = z.anim * 2f + k * 1.047f
            val len = 18f + 4f * sin(z.anim * 9f + k)
            drawLine(Color(0xFFFF6D00), Offset(cos(a) * 9f, sin(a) * 9f), Offset(cos(a) * len, sin(a) * len), 3f, cap = StrokeCap.Round)
        }
        drawCircle(Color(0xFFB71C1C), 1.8f, Offset(-3f, -2f))
        drawCircle(Color(0xFFB71C1C), 1.8f, Offset(3f, -2f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A zombie shark: bandaged and red-eyed. The Zombie Shark swims; the Breaching Shark leaps out of the water onto a roof. */
private fun DrawScope.drawShark(z: Zombie, dir: Float) {
    val size = z.type.size
    val flash = z.flash * 0.75f
    val skin = lerp(Color(0xFF607D8B), Color.White, flash)
    val belly = lerp(Color(0xFFCFD8DC), Color.White, flash)
    val dark = lerp(Color(0xFF455A64), Color.White, flash)
    val swim = sin(z.anim * 1.6f)
    val tilt = if (z.jumpState == 1) (z.vy / 6f).coerceIn(-50f, 50f) * dir else swim * 4f
    if (!z.flying && z.jumpState == 0) drawOval(Color.Black.copy(alpha = 0.28f), Offset(z.x - 36f * size, z.y - 3f * size), Size(72f * size, 7f * size))
    withTransform({ translate(z.x, z.y - z.type.aimHeight * size); scale(size * dir, size, Offset.Zero); rotate(tilt, Offset.Zero) }) {
        path.rewind(); path.moveTo(-26f, 0f); path.lineTo(-44f, -12f + swim * 5f); path.lineTo(-38f, 0f); path.lineTo(-44f, 12f + swim * 5f); path.close()
        drawPath(path, dark)
        drawOval(skin, Offset(-30f, -12f), Size(62f, 24f))
        drawOval(belly, Offset(-22f, 2f), Size(48f, 9f))
        path.rewind(); path.moveTo(28f, -9f); path.lineTo(42f, 2f); path.lineTo(28f, 10f); path.close()
        drawPath(path, skin)
        path.rewind(); path.moveTo(-4f, -12f); path.lineTo(6f, -26f); path.lineTo(14f, -12f); path.close()
        drawPath(path, dark)
        path.rewind(); path.moveTo(4f, 8f); path.lineTo(-6f, 20f); path.lineTo(12f, 10f); path.close()
        drawPath(path, dark)
        drawLine(Color(0xFF263238), Offset(18f, 4f), Offset(40f, 3f), 2f)
        for (k in 0..3) {
            path.rewind(); path.moveTo(22f + k * 5f, 4f); path.lineTo(24.5f + k * 5f, 8f); path.lineTo(27f + k * 5f, 4f); path.close()
            drawPath(path, Color.White)
        }
        for (k in 0..2) drawLine(Color(0xFFE8DCC8).copy(alpha = 0.85f), Offset(-12f + k * 9f, -11f), Offset(-8f + k * 9f, 9f), 3f)
        for (k in 0..2) drawLine(Color(0xFF263238), Offset(10f + k * 3f, -2f), Offset(10f + k * 3f, 6f), 1.2f)
        drawCircle(Color.White, 3.4f, Offset(24f, -4f))
        drawCircle(Color(0xFFD50000), 1.8f, Offset(25f, -4f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}
