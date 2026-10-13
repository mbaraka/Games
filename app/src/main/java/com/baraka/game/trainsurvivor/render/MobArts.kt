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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Enemy art for the route enemies. To add an enemy:
//   - plain humanoid with its own colours        -> add a MobLooks.set(...) line in registerMobLooks()
//   - humanoid with accessories (hat, shield...)  -> HumanoidExtras.register(...) in registerMobArts()
//   - anything else (animal, flyer, machine)      -> write a DrawScope function and MobArtRegistry.register(...) it

internal fun registerMobLooks() {
    val l = MobLooks
    l.set(ZombieType.WALKER, 0xFF86B864, 0xFF5D6D7E)
    l.set(ZombieType.RUNNER, 0xFF7DBB5E, 0xFF8D3B3B)
    l.set(ZombieType.BRUTE, 0xFF6E8F52, 0xFF4A3F36)
    l.set(ZombieType.BLOATER, 0xFFB4C45C, 0xFF8A9A3A)
    l.set(ZombieType.TITAN, 0xFF5C7A4A, 0xFF3B2F2F)
    l.set(ZombieType.BALLOON, 0xFF86B864, 0xFFFF7043)
    l.set(ZombieType.CLOWN, 0xFFE8DCC8, 0xFFE53935)
    l.set(ZombieType.CLOWN_PRINCE, 0xFFE8DCC8, 0xFF8E24AA)
    l.set(ZombieType.GARGOYLE, 0xFF7A7F8C, 0xFF4B4F5A)
    l.set(ZombieType.BAT, 0xFF4A2F5E, 0xFF4A2F5E)
    l.set(ZombieType.ZOMBIE_DOG, 0xFF7B6F57, 0xFF5E5444)
    l.set(ZombieType.YETI, 0xFFEAF2F8, 0xFFD6E4EF)
    l.set(ZombieType.RAT, 0xFF6D6258, 0xFF574E46)
    l.set(ZombieType.SCORPION, 0xFFB8803F, 0xFF8A5E2B)
    l.set(ZombieType.SKELETON, 0xFFEDE7D6, 0xFF2B2B2B)
    l.set(ZombieType.CROCODILE, 0xFF5E7B3A, 0xFF46602B)
    l.set(ZombieType.LEAPER, 0xFF86B864, 0xFFF5F5F5)
    l.set(ZombieType.DRONE, 0xFF607D8B, 0xFF37474F)
    l.set(ZombieType.MAGMA_GOLEM, 0xFF4E342E, 0xFF3E2723)
    l.set(ZombieType.ICE_WOLF, 0xFFDCEBF5, 0xFFB8CBD8)
    l.set(ZombieType.FROST_WRAITH, 0xFFBBDEFB, 0xFF90CAF9)
    l.set(ZombieType.GUNSLINGER, 0xFF86B864, 0xFF795548)
    l.set(ZombieType.JUGGLER, 0xFFE8DCC8, 0xFFAB47BC)
    l.set(ZombieType.STRONGMAN, 0xFF86B864, 0xFFE53935)
    l.set(ZombieType.MINI_CLOWN, 0xFFE8DCC8, 0xFFFF9800)
    l.set(ZombieType.DROWNED, 0xFF6FA58A, 0xFF3E6B5A)
    l.set(ZombieType.TENTACLE, 0xFF7E57C2, 0xFF5E35B1)
    l.set(ZombieType.RIOT, 0xFF86B864, 0xFF37474F)
}

internal fun registerMobArts() {
    registerRouteMobs()
    registerSeaAndSunMobs()
    registerDinoMobs()
    registerRailMobs()
    registerPortraitSizes()
    registerBossArts()
}

private fun registerRouteMobs() {
    MobArtRegistry.register(ZombieType.DRONE) { s, z, d -> with(s) { drawDrone(z, d) } }
    MobArtRegistry.register(ZombieType.FROST_WRAITH) { s, z, d -> with(s) { drawWraith(z, d) } }
    MobArtRegistry.register(ZombieType.TENTACLE) { s, z, _ -> with(s) { drawTentacle(z) } }

    // Humanoid accessories, drawn in the zombie's local coordinates (feet at the origin, facing +x).
    HumanoidExtras.register(ZombieType.LEAPER) { s, _, headY, bob, _, _ ->
        with(s) {
            drawCircle(Color(0xFFB3E5FC).copy(alpha = 0.4f), 10.5f, Offset(2f, headY)) // helmet glass
            drawCircle(Color(0xFFE1F5FE), 10.5f, Offset(2f, headY), style = Stroke(1.6f))
            box(Color(0xFFCFD8DC), -12f, -36f + bob, 5f, 16f) // oxygen pack
            drawLine(Color(0xFF90A4AE), Offset(-10f, -36f + bob), Offset(-10f, -46f + bob), 1.5f)
            drawCircle(Color(0xFF00E5FF), 1.8f, Offset(3f, -28f + bob))
        }
    }
    HumanoidExtras.register(ZombieType.MAGMA_GOLEM) { s, z, headY, bob, _, _ ->
        with(s) {
            val glow = 0.6f + 0.4f * sin(z.anim * 2f)
            for (k in 0..3) drawLine(Color(0xFFFF6D00).copy(alpha = glow), Offset(-7f + k * 4f, -38f + bob), Offset(-3f + k * 3f, -16f + bob), 1.8f)
            drawCircle(Color(0xFFFF6D00).copy(alpha = 0.5f * glow), 6f, Offset(2f, headY + 2f))
            drawCircle(Color(0xFFFFEB3B), 2f, Offset(6f, headY - 1.5f))
        }
    }
    HumanoidExtras.register(ZombieType.GUNSLINGER) { s, _, headY, bob, _, _ ->
        with(s) {
            drawOval(Color(0xFF5D4037), Offset(-8f, headY - 10f), Size(22f, 6f)) // hat brim
            box(Color(0xFF4E342E), -3f, headY - 17f, 12f, 8f)
            box(Color(0xFFD32F2F), -4f, headY + 2f, 12f, 3f) // bandana
            box(Color(0xFF263238), 14f, -35f + bob, 11f, 3f) // revolver
            box(Color(0xFF263238), 15f, -33f + bob, 3f, 6f)
        }
    }
    HumanoidExtras.register(ZombieType.JUGGLER) { s, z, headY, _, _, _ ->
        with(s) {
            val colors = intArrayOf(0xFFFF4081.toInt(), 0xFFFFEB3B.toInt(), 0xFF40C4FF.toInt())
            for (k in 0..2) {
                val a = z.anim * 3f + k * 2.09f
                drawCircle(Color(colors[k]), 3.2f, Offset(2f + cos(a) * 14f, headY - 20f + sin(a) * 7f))
            }
        }
    }
    HumanoidExtras.register(ZombieType.STRONGMAN) { s, _, headY, bob, _, _ ->
        with(s) {
            for (k in 0..3) box(Color.White.copy(alpha = 0.9f), -11f, -38f + k * 6f + bob, 22f, 3f) // striped singlet
            box(Color(0xFF3E2723), 3f, headY + 3f, 8f, 2.5f) // moustache
        }
    }
    HumanoidExtras.register(ZombieType.DROWNED) { s, z, headY, _, _, _ ->
        with(s) {
            for (k in 0..3) drawLine(Color(0xFF2E7D32), Offset(-4f + k * 4f, headY - 6f), Offset(-6f + k * 4f, headY + 8f + sin(z.anim + k) * 2f), 2f)
            drawCircle(Color(0xFFE0D8C0), 1.6f, Offset(-8f, -26f))
        }
    }
    HumanoidExtras.register(ZombieType.RIOT) { s, _, headY, bob, _, _ ->
        with(s) {
            box(Color(0xFF263238), 3f, headY - 3f, 9f, 5f) // visor
            rbox(Color(0xFF90CAF9).copy(alpha = 0.55f), 12f, -44f + bob, 8f, 40f, 3f) // riot shield
            box(Color(0xFFE3F2FD), 12f, -44f + bob, 8f, 2f)
            box(Color(0xFFE3F2FD), 12f, -6f + bob, 8f, 2f)
        }
    }
}

/** A hovering hunter drone: twin rotors, a camera eye and a gun. */
internal fun DrawScope.drawDrone(z: Zombie, dir: Float, k: Float = 1f) {
    val body = lerp(Color(0xFF455A64), Color.White, z.flash * 0.75f)
    withTransform({ translate(z.x, z.y - 18f * k); scale(dir * k, k, Offset.Zero) }) {
        for (sx in floatArrayOf(-16f, 16f)) {
            drawLine(Color(0xFF263238), Offset(sx * 0.7f, -4f), Offset(sx, -11f), 2f)
            val blur = 0.25f + 0.2f * abs(sin(z.anim * 6f))
            drawOval(Color.White.copy(alpha = blur), Offset(sx - 13f, -15f), Size(26f, 4f))
        }
        rbox(body, -12f, -6f, 24f, 14f, 6f)
        drawCircle(Color(0xFF263238), 6f, Offset(4f, 2f))
        drawCircle(Color(0xFFFF1744).copy(alpha = 0.5f + 0.5f * sin(z.anim * 3f)), 2.6f, Offset(4f, 2f))
        box(Color(0xFF90A4AE), 10f, 6f, 10f, 3f)
        drawCircle(Color(0xFF00E5FF), 1.8f, Offset(-8f, -2f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A frost wraith: a translucent hooded ghost that trails a wavy cloak. */
internal fun DrawScope.drawWraith(z: Zombie, dir: Float, k: Float = 1f) {
    val fade = 0.72f
    val cloak = lerp(Color(0xFFBBDEFB), Color.White, z.flash * 0.75f).copy(alpha = fade)
    val wave = sin(z.anim * 2f) * 3f
    withTransform({ translate(z.x, z.y - 36f * k); scale(dir * k, k, Offset.Zero) }) {
        path.rewind()
        path.moveTo(-13f, -8f); path.lineTo(13f, -8f); path.lineTo(10f, 18f + wave)
        path.lineTo(4f, 10f); path.lineTo(0f, 22f - wave); path.lineTo(-5f, 10f); path.lineTo(-10f, 18f + wave); path.close()
        drawPath(path, cloak)
        drawCircle(cloak, 10f, Offset(0f, -16f))
        drawOval(Color(0xFF1A237E).copy(alpha = 0.85f), Offset(-6f, -19f), Size(5f, 7f))
        drawOval(Color(0xFF1A237E).copy(alpha = 0.85f), Offset(2f, -19f), Size(5f, 7f))
        drawCircle(Color(0xFF80F0FF), 1.3f, Offset(-3.5f, -16f))
        drawCircle(Color(0xFF80F0FF), 1.3f, Offset(4.5f, -16f))
        drawLine(Color(0xFF90CAF9).copy(alpha = fade), Offset(9f, -4f), Offset(24f, -2f + sin(z.anim * 3f) * 4f), 4f, cap = StrokeCap.Round)
        for (k in 0..2) drawCircle(Color.White.copy(alpha = 0.7f), 1.5f, Offset(cos(z.anim + k * 2.1f) * 20f, -10f + sin(z.anim * 1.3f + k * 2.1f) * 16f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A sea tentacle: rises from the water beside the bridge and wraps a wagon. [Zombie.y] is where it enters the water. */
private fun DrawScope.drawTentacle(z: Zombie) {
    val base = z.y
    val length = max(90f, base - 468f)
    val segments = 12
    var px = z.x
    var py = base
    for (i in 1..segments) {
        val t = i / segments.toFloat()
        val nx = z.x + sin(z.anim * 1.4f + t * 3.2f) * (10f + 22f * t) + t * t * 14f
        val ny = base - length * t
        val w = 17f * (1f - t * 0.7f) * z.type.size
        val c = lerp(Color(0xFF5E35B1), Color(0xFF9575CD), t)
        drawLine(lerp(c, Color.White, z.flash * 0.7f), Offset(px, py), Offset(nx, ny), w, cap = StrokeCap.Round)
        if (i % 2 == 0) drawCircle(Color(0xFFE1BEE7).copy(alpha = 0.8f), w * 0.18f, Offset(nx - w * 0.25f, ny))
        px = nx
        py = ny
    }
    if (z.hp < z.maxHp) {
        box(Color.Black.copy(alpha = 0.6f), px - 16f, py - 16f, 32f, 4f)
        box(Color(0xFFE53935), px - 16f, py - 16f, 32f * (z.hp / z.maxHp).coerceIn(0f, 1f), 4f)
    }
}
