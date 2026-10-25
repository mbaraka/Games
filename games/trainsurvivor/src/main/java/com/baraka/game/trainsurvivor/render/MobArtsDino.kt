package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.Zombie
import kotlin.math.sin

// The Jurassic Valley dinosaurs (see Biome.JURASSIC). Three bodies cover them all, each tinted and shaped by a Dino look:
//   - two-legged hunters (compy, raptor, spitter, T-Rex)   -> drawTheropod
//   - four-legged giants (triceratops, brontosaurus)       -> drawQuad
//   - the flyer (pterodactyl)                              -> drawPtero
// To add another dinosaur: add its ZombieType, give it a Dino look here and list it in the right MobArtRegistry line.

/** A species' colours and proportions. [head] and [arms] scale those parts; [tail] stretches the tail. */
private class Dino(
    val body: Int,
    val belly: Int,
    val accent: Int,
    val head: Float = 1f,
    val arms: Float = 1f,
    val tail: Float = 1f,
    /** A fan of spines behind the head (the spitter). */
    val frill: Boolean = false
)

private val DINOS = HashMap<ZombieType, Dino>()

internal fun registerDinoMobs() {
    DINOS[ZombieType.COMPY] = Dino(0xFF9DB85A.toInt(), 0xFFE3EDB0.toInt(), 0xFF5C7030.toInt(), head = 1.15f, tail = 1.25f)
    DINOS[ZombieType.RAPTOR] = Dino(0xFF5F8A3C.toInt(), 0xFFCADA8E.toInt(), 0xFF2E4A1B.toInt(), tail = 1.15f)
    DINOS[ZombieType.SPITTER] = Dino(0xFF7BA34B.toInt(), 0xFFEDE6A0.toInt(), 0xFFE5732F.toInt(), frill = true)
    DINOS[ZombieType.TREX] = Dino(0xFF4B7538.toInt(), 0xFFB7C67D.toInt(), 0xFF283F1E.toInt(), head = 1.4f, arms = 0.45f)
    DINOS[ZombieType.TRICERATOPS] = Dino(0xFF7C8C4A.toInt(), 0xFFC8CE92.toInt(), 0xFFB5543A.toInt())
    DINOS[ZombieType.BRONTOSAURUS] = Dino(0xFF6E8E7A.toInt(), 0xFFC4D4B4.toInt(), 0xFF3F5A4A.toInt(), tail = 1.6f)
    DINOS[ZombieType.PTERODACTYL] = Dino(0xFF8A6B52.toInt(), 0xFFD9B48C.toInt(), 0xFF5A3E2E.toInt())

    MobArtRegistry.register(listOf(ZombieType.COMPY, ZombieType.RAPTOR, ZombieType.SPITTER, ZombieType.TREX)) { s, z, d -> with(s) { drawTheropod(z, d) } }
    MobArtRegistry.register(listOf(ZombieType.TRICERATOPS, ZombieType.BRONTOSAURUS)) { s, z, d -> with(s) { drawQuad(z, d) } }
    MobArtRegistry.register(ZombieType.PTERODACTYL) { s, z, d -> with(s) { drawPtero(z, d) } }

    val p = PortraitSizes
    p.set(ZombieType.COMPY, 52f)
    p.set(ZombieType.RAPTOR, 86f)
    p.set(ZombieType.SPITTER, 96f)
    p.set(ZombieType.TRICERATOPS, 120f)
    p.set(ZombieType.BRONTOSAURUS, 330f)
    p.set(ZombieType.TREX, 250f)
    p.set(ZombieType.PTERODACTYL, 110f, floating = 0.58f)
}

/** Zombie touches shared by every dino: a gouged wound and a glowing eye. */
private fun DrawScope.zombieWound(x: Float, y: Float, k: Float) {
    drawCircle(Color(0xFF8E2A2A), 4.5f * k, Offset(x, y))
    drawCircle(Color(0xFF5C1717), 2.5f * k, Offset(x + 1f, y))
    for (i in 0..2) drawLine(Color(0xFFE8DDB5), Offset(x - 5f * k + i * 3f * k, y - 3f * k), Offset(x - 5f * k + i * 3f * k, y + 4f * k), 1.4f)
}

private fun DrawScope.dinoEye(x: Float, y: Float, r: Float) {
    drawCircle(Color(0xFFFFEB3B), r, Offset(x, y))
    drawCircle(Color(0xFFD50000), r * 0.45f, Offset(x + r * 0.2f, y))
}

/** A two-legged hunter: running legs, a swaying tail, a big toothy head that snaps while it attacks. */
private fun DrawScope.drawTheropod(z: Zombie, dir: Float) {
    val dino = DINOS[z.type] ?: return
    val size = z.type.size
    val flash = z.flash * 0.75f
    val body = lerp(Color(dino.body), Color.White, flash)
    val belly = lerp(Color(dino.belly), Color.White, flash)
    val accent = lerp(Color(dino.accent), Color.White, flash)
    val run = sin(z.anim * 1.7f)
    val bite = if (z.attacking) 0.5f + 0.5f * sin(z.anim * 8f) else 0f
    val hs = dino.head
    drawOval(Color.Black.copy(alpha = 0.28f), Offset(z.x - 30f * size, z.y - 3f * size), Size(60f * size, 7f * size))
    withTransform({ translate(z.x, z.y); scale(size * dir, size, Offset.Zero) }) {
        // Tail first, so the body overlaps it.
        val tailLen = 34f * dino.tail
        path.rewind()
        path.moveTo(-12f, -48f); path.lineTo(-12f - tailLen, -34f + sin(z.anim * 1.1f) * 4f); path.lineTo(-12f, -32f); path.close()
        drawPath(path, accent)
        // Legs: thigh, shin and a three-toed foot, in opposite phase.
        for (leg in 0..1) {
            val ph = if (leg == 0) run else -run
            val hipX = -2f + leg * 4f
            val kneeX = hipX + 7f + ph * 6f
            val footX = hipX + 3f + ph * 11f
            val col = if (leg == 0) accent else body
            drawLine(col, Offset(hipX, -34f), Offset(kneeX, -19f), 8f, cap = StrokeCap.Round)
            drawLine(col, Offset(kneeX, -19f), Offset(footX, -3f), 5f, cap = StrokeCap.Round)
            box(col, footX - 2f, -3f, 11f, 3f)
        }
        // Torso, belly and stripes.
        drawOval(body, Offset(-20f, -56f), Size(42f, 28f))
        drawOval(belly, Offset(-12f, -40f), Size(28f, 11f))
        for (k in 0..2) drawLine(accent, Offset(-12f + k * 9f, -54f), Offset(-8f + k * 9f, -44f), 2.6f)
        zombieWound(-4f, -43f, 1f)
        // Spitter frill: a fan of spines behind the head.
        val hx = 14f
        val hy = -70f
        if (dino.frill) {
            for (k in 0..4) {
                val ang = -2.5f + k * 0.42f
                drawLine(accent, Offset(hx + 6f, hy + 8f), Offset(hx + 6f + kotlin.math.cos(ang) * 17f, hy + 8f + kotlin.math.sin(ang) * 17f), 3.2f, cap = StrokeCap.Round)
            }
        }
        // Neck, head, jaw and teeth.
        drawLine(body, Offset(8f, -48f), Offset(hx + 6f, hy + 8f), 12f, cap = StrokeCap.Round)
        drawOval(body, Offset(hx, hy), Size(26f * hs, 14f * hs))
        drawOval(body, Offset(hx + 13f * hs, hy + 2f * hs), Size(17f * hs, 9f * hs))
        drawOval(belly, Offset(hx + 9f * hs, hy + 9f * hs + bite * 6f), Size(20f * hs, 5.5f * hs))
        for (k in 0..3) {
            val tx = hx + 16f * hs + k * 3.4f * hs
            path.rewind(); path.moveTo(tx, hy + 9f * hs); path.lineTo(tx + 1.6f * hs, hy + 13.5f * hs + bite * 3f); path.lineTo(tx + 3.2f * hs, hy + 9f * hs); path.close()
            drawPath(path, Color.White)
        }
        dinoEye(hx + 10f * hs, hy + 4f * hs, 2.4f * hs)
        drawCircle(accent, 1f * hs, Offset(hx + 28f * hs, hy + 4f * hs)) // nostril
        // Tiny arms with claws (the T-Rex's are famously small).
        val armLen = 8f * dino.arms
        drawLine(body, Offset(11f, -46f), Offset(11f + armLen, -39f + sin(z.anim * 2f) * 2f), 3.4f * dino.arms.coerceAtLeast(0.7f), cap = StrokeCap.Round)
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A four-legged giant: the triceratops with a bony frill and three horns, or the brontosaurus with its long neck. */
private fun DrawScope.drawQuad(z: Zombie, dir: Float) {
    val dino = DINOS[z.type] ?: return
    val size = z.type.size
    val flash = z.flash * 0.75f
    val body = lerp(Color(dino.body), Color.White, flash)
    val belly = lerp(Color(dino.belly), Color.White, flash)
    val accent = lerp(Color(dino.accent), Color.White, flash)
    val bronto = z.type == ZombieType.BRONTOSAURUS
    val charge = if (z.attacking) 0.5f + 0.5f * sin(z.anim * 6f) else 0f
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(z.x - 36f * size, z.y - 3f * size), Size(72f * size, 8f * size))
    withTransform({ translate(z.x, z.y); scale(size * dir, size, Offset.Zero) }) {
        // Tail.
        val tailLen = 26f * dino.tail
        path.rewind()
        path.moveTo(-26f, -44f); path.lineTo(-26f - tailLen, -28f + sin(z.anim * 0.9f) * 4f); path.lineTo(-26f, -30f); path.close()
        drawPath(path, accent)
        // Four stout legs, walking in diagonal pairs.
        for (k in 0..3) {
            val lx = -22f + k * 14f
            val swing = sin(z.anim * 1.4f + (if (k % 2 == 0) 0f else 3.14f)) * 3.5f
            box(if (k % 2 == 0) accent else body, lx + swing, -19f, 9f, 19f)
            box(Color(0xFF3B3226), lx + swing - 1f, -3f, 11f, 3f)
        }
        drawOval(body, Offset(-34f, -52f), Size(68f, 36f))
        drawOval(belly, Offset(-22f, -34f), Size(46f, 12f))
        for (k in 0..3) drawCircle(accent.copy(alpha = 0.55f), 3.5f, Offset(-16f + k * 11f, -46f + (k % 2) * 4f)) // mottling
        zombieWound(-8f, -38f, 1.2f)
        if (bronto) {
            // A long neck swaying up to a small head.
            var px = 24f
            var py = -46f
            for (i in 1..6) {
                val nx = 24f + i * 5f + sin(z.anim * 0.7f + i * 0.4f) * 2f
                val ny = -46f - i * 10f
                drawLine(body, Offset(px, py), Offset(nx, ny), 15f - i * 1.4f, cap = StrokeCap.Round)
                px = nx
                py = ny
            }
            drawOval(body, Offset(px - 2f, py - 7f), Size(20f, 12f))
            drawOval(belly, Offset(px + 2f, py + 2f + charge * 3f), Size(15f, 4f))
            dinoEye(px + 8f, py - 2f, 2f)
        } else {
            // Triceratops: a big frill behind the head, two brow horns and a nose horn.
            val hx = 28f
            val hy = -44f
            path.rewind()
            path.moveTo(hx - 6f, hy + 2f); path.lineTo(hx - 12f, hy - 26f); path.lineTo(hx + 4f, hy - 30f); path.lineTo(hx + 12f, hy - 14f); path.close()
            drawPath(path, accent)
            for (k in 0..4) drawCircle(Color(0xFFEFE0B0), 2f, Offset(hx - 9f + k * 4.6f, hy - 24f + (k % 2) * 4f + kotlin.math.abs(k - 2) * 3f))
            drawOval(body, Offset(hx - 4f, hy - 8f), Size(32f, 22f))
            path.rewind(); path.moveTo(hx + 22f, hy - 4f); path.lineTo(hx + 36f, hy + 4f + charge * 3f); path.lineTo(hx + 22f, hy + 11f); path.close()
            drawPath(path, belly)
            drawLine(Color(0xFFF2EAD0), Offset(hx + 14f, hy - 6f), Offset(hx + 28f, hy - 26f), 5f, cap = StrokeCap.Round)
            drawLine(Color(0xFFF2EAD0), Offset(hx + 8f, hy - 7f), Offset(hx + 20f, hy - 25f), 5f, cap = StrokeCap.Round)
            drawLine(Color(0xFFF2EAD0), Offset(hx + 30f, hy + 2f), Offset(hx + 38f, hy - 8f), 4f, cap = StrokeCap.Round)
            dinoEye(hx + 12f, hy - 1f, 2.6f)
        }
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A zombie pterodactyl: leathery wings that flap, a long crested head and a toothy beak. */
private fun DrawScope.drawPtero(z: Zombie, dir: Float) {
    val dino = DINOS[z.type] ?: return
    val k = z.type.size
    val flash = z.flash * 0.75f
    val body = lerp(Color(dino.body), Color.White, flash)
    val belly = lerp(Color(dino.belly), Color.White, flash)
    val accent = lerp(Color(dino.accent), Color.White, flash)
    val flap = sin(z.anim * 1.7f)
    withTransform({ translate(z.x, z.y - 14f * k); scale(dir * k, k, Offset.Zero) }) {
        // Far wing (darker), body, near wing.
        path.rewind()
        path.moveTo(-2f, -3f); path.lineTo(-30f, -4f - 22f * flap); path.lineTo(-16f, -2f - 8f * flap); path.lineTo(-6f, 5f); path.close()
        drawPath(path, accent)
        drawOval(body, Offset(-14f, -9f), Size(30f, 13f))
        drawOval(belly, Offset(-8f, -3f), Size(20f, 6f))
        path.rewind()
        path.moveTo(4f, -4f); path.lineTo(32f, -2f - 20f * flap); path.lineTo(18f, 0f - 6f * flap); path.lineTo(12f, 6f); path.close()
        drawPath(path, lerp(accent, body, 0.5f))
        for (i in 0..2) drawLine(Color(0xFF3A2A20), Offset(6f + i * 6f, -2f), Offset(14f + i * 6f, -2f - (14f - i * 4f) * flap), 1.4f) // wing fingers
        zombieWound(-4f, -4f, 0.6f)
        // Neck, head with a backward crest, long beak.
        drawLine(body, Offset(12f, -5f), Offset(22f, -12f), 6f, cap = StrokeCap.Round)
        drawOval(body, Offset(18f, -18f), Size(15f, 8f))
        path.rewind(); path.moveTo(20f, -17f); path.lineTo(6f, -25f); path.lineTo(24f, -13f); path.close()
        drawPath(path, accent)
        path.rewind(); path.moveTo(31f, -16f); path.lineTo(50f, -13f); path.lineTo(31f, -11f); path.close()
        drawPath(path, belly)
        for (i in 0..2) {
            path.rewind(); path.moveTo(36f + i * 4f, -12f); path.lineTo(37.5f + i * 4f, -8.5f); path.lineTo(39f + i * 4f, -12f); path.close()
            drawPath(path, Color.White)
        }
        dinoEye(27f, -15f, 1.9f)
        // Hind legs tucked, tail stub.
        drawLine(body, Offset(-12f, 1f), Offset(-20f, 8f), 2.4f)
        drawLine(accent, Offset(-14f, -3f), Offset(-24f, -1f), 2f)
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}
