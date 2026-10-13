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
import kotlin.math.sin

// The Elemental Wilds critters (see Biome.WILDS and model/Elements.kt). Each is its own little creature; the element badge that
// shows its weakness is drawn by drawElementBadge (called for every custom-art enemy in TrainRenderer.kt).

internal fun registerWildsMobs() {
    MobArtRegistry.register(ZombieType.CINDER_PUP) { s, z, d -> with(s) { drawCinderPup(z, d) } }
    MobArtRegistry.register(ZombieType.TIDE_SLUG) { s, z, d -> with(s) { drawTideSlug(z, d) } }
    MobArtRegistry.register(ZombieType.THORNBACK) { s, z, d -> with(s) { drawThornback(z, d) } }
    MobArtRegistry.register(ZombieType.SPARK_HARE) { s, z, d -> with(s) { drawSparkHare(z, d) } }
    MobArtRegistry.register(ZombieType.WISP) { s, z, d -> with(s) { drawWisp(z, d) } }
    MobArtRegistry.register(ZombieType.BOULDER_BEETLE) { s, z, d -> with(s) { drawBoulderBeetle(z, d) } }
    MobArtRegistry.register(ZombieType.PRISM_DRAKE) { s, z, d -> with(s) { drawPrismDrake(z, d) } }
    val p = PortraitSizes
    p.set(ZombieType.CINDER_PUP, 56f)
    p.set(ZombieType.TIDE_SLUG, 70f)
    p.set(ZombieType.THORNBACK, 88f)
    p.set(ZombieType.SPARK_HARE, 66f)
    p.set(ZombieType.WISP, 70f, floating = 0.58f)
    p.set(ZombieType.BOULDER_BEETLE, 84f)
    p.set(ZombieType.PRISM_DRAKE, 180f)
}

/** A small badge above an elemental enemy: its element's colour and letter, so the player knows which weapon to reach for. */
internal fun DrawScope.drawElementBadge(z: Zombie) {
    val element = z.elementNow() ?: return
    val size = z.type.size
    val y = if (z.type.flying) z.y - 46f * size else z.y - 56f * size - 8f
    val c = Color(element.color)
    drawCircle(Color.Black.copy(alpha = 0.55f), 9.5f, Offset(z.x, y))
    drawCircle(c, 8f, Offset(z.x, y))
    drawCircle(Color.White.copy(alpha = 0.9f), 8f, Offset(z.x, y), style = Stroke(1.4f))
    outlinedText(element.symbol, z.x, y + 4.5f, 12f, 0xFF212121.toInt())
}

private fun DrawScope.shadow(z: Zombie, width: Float) {
    val k = z.type.size
    drawOval(Color.Black.copy(alpha = 0.28f), Offset(z.x - width / 2f * k, z.y - 3f * k), Size(width * k, 7f * k))
}

private fun DrawScope.zombieEye(x: Float, y: Float, r: Float) {
    drawCircle(Color(0xFFFFEB3B), r, Offset(x, y))
    drawCircle(Color(0xFFD50000), r * 0.45f, Offset(x + r * 0.2f, y))
}

/** A fire hound with a flaming mane and tail, ember eyes. */
private fun DrawScope.drawCinderPup(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val fur = lerp(Color(0xFFC75B2A), Color.White, flash)
    val dark = lerp(Color(0xFF6B2B14), Color.White, flash)
    shadow(z, 44f)
    withTransform({ translate(z.x, z.y); scale(k * dir, k, Offset.Zero) }) {
        for (i in 0..3) box(dark, -14f + i * 9f + sin(z.anim * 1.8f + i * 1.7f) * 4f, -12f, 4f, 12f)
        drawOval(fur, Offset(-20f, -27f), Size(40f, 18f))
        drawOval(Color(0xFFFFB074), Offset(-12f, -17f), Size(24f, 8f))
        for (i in 0..3) { // flame mane
            val h = 11f + sin(z.anim * 6f + i) * 4f
            path.rewind(); path.moveTo(-16f + i * 8f, -26f); path.lineTo(-12f + i * 8f, -26f - h); path.lineTo(-8f + i * 8f, -26f); path.close()
            drawPath(path, Color(0xFFFF6D00))
            path.rewind(); path.moveTo(-14f + i * 8f, -26f); path.lineTo(-12f + i * 8f, -26f - h * 0.6f); path.lineTo(-10f + i * 8f, -26f); path.close()
            drawPath(path, Color(0xFFFFEB3B))
        }
        path.rewind(); path.moveTo(-20f, -22f); path.lineTo(-36f, -32f + sin(z.anim * 5f) * 4f); path.lineTo(-20f, -28f); path.close()
        drawPath(path, Color(0xFFFF8F00))
        drawOval(fur, Offset(14f, -35f), Size(18f, 14f))
        path.rewind(); path.moveTo(18f, -34f); path.lineTo(21f, -43f); path.lineTo(25f, -34f); path.close()
        drawPath(path, dark)
        box(dark, 28f, -31f, 9f, 6f)
        zombieEye(25f, -30f, 1.9f)
        if (z.attacking) drawOval(Color(0xFFFF8A65), Offset(33f, -26f), Size(5f, 7f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A slow, dripping water blob with two stalk eyes. */
private fun DrawScope.drawTideSlug(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val body = lerp(Color(0xFF3F8FD2), Color.White, flash)
    val wobble = 1f + 0.06f * sin(z.anim * 1.6f)
    shadow(z, 50f)
    withTransform({ translate(z.x, z.y); scale(k * dir, k * wobble, Offset.Zero) }) {
        drawOval(Color(0xFF2F6FA8), Offset(-28f, -8f), Size(56f, 8f))
        drawOval(body, Offset(-26f, -34f), Size(52f, 30f))
        drawOval(Color.White.copy(alpha = 0.3f), Offset(-16f, -30f), Size(20f, 8f))
        for (i in 0..2) drawCircle(Color(0xFFB3E5FC).copy(alpha = 0.6f), 2.5f, Offset(-12f + i * 12f, -18f - (i % 2) * 6f)) // bubbles
        for (i in 0..1) {
            drawLine(body, Offset(14f + i * 7f, -32f), Offset(17f + i * 8f, -44f + sin(z.anim * 2f + i) * 2f), 3f, cap = StrokeCap.Round)
            zombieEye(17f + i * 8f, -45f + sin(z.anim * 2f + i) * 2f, 2.6f)
        }
        drawArc(Color(0xFF1A3C5E), 20f, 140f, false, Offset(14f, -24f), Size(14f, 8f), style = Stroke(2f))
        val drip = (z.anim * 0.5f) % 1f
        drawCircle(body.copy(alpha = 1f - drip), 2.4f, Offset(-20f, -8f + drip * 8f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A spiny, vine-wrapped grass boar. */
private fun DrawScope.drawThornback(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val hide = lerp(Color(0xFF4E8C4A), Color.White, flash)
    val dark = lerp(Color(0xFF2A5A2B), Color.White, flash)
    shadow(z, 56f)
    withTransform({ translate(z.x, z.y); scale(k * dir, k, Offset.Zero) }) {
        for (i in 0..3) box(dark, -20f + i * 13f + sin(z.anim * 1.5f + i * 1.6f) * 3f, -14f, 8f, 14f)
        drawOval(hide, Offset(-26f, -44f), Size(56f, 32f))
        for (i in 0..5) { // thorns along the back
            path.rewind(); path.moveTo(-22f + i * 9f, -40f); path.lineTo(-18f + i * 9f, -54f - (i % 2) * 5f); path.lineTo(-14f + i * 9f, -40f); path.close()
            drawPath(path, Color(0xFFE8F5C8))
        }
        for (i in 0..2) drawLine(Color(0xFF8BC34A), Offset(-14f + i * 14f, -30f), Offset(-6f + i * 14f, -20f), 2.4f) // vines
        drawOval(hide, Offset(22f, -38f), Size(24f, 20f))
        drawLine(Color(0xFFF5F0D8), Offset(38f, -24f), Offset(46f, -34f), 3.4f, cap = StrokeCap.Round) // tusk
        zombieEye(34f, -31f, 2.2f)
        drawCircle(Color(0xFF8E2A2A), 4f, Offset(-8f, -26f)) // wound
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** An electric hare: long ears, a lightning-bolt tail and crackling arcs; it hops. */
private fun DrawScope.drawSparkHare(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val fur = lerp(Color(0xFFFFD54F), Color.White, flash)
    val hop = abs(sin(z.anim * 1.4f)) * 9f
    shadow(z, 34f)
    withTransform({ translate(z.x, z.y - hop); scale(k * dir, k, Offset.Zero) }) {
        box(Color(0xFFB8860B), -8f, -9f, 5f, 9f)
        box(Color(0xFFB8860B), 4f, -9f, 5f, 9f)
        drawOval(fur, Offset(-14f, -28f), Size(30f, 20f))
        path.rewind(); path.moveTo(-14f, -22f); path.lineTo(-22f, -26f); path.lineTo(-18f, -20f); path.lineTo(-27f, -18f); path.lineTo(-17f, -14f); path.lineTo(-14f, -17f); path.close()
        drawPath(path, Color(0xFFFFEB3B)) // bolt tail
        drawOval(fur, Offset(10f, -34f), Size(16f, 14f))
        drawLine(fur, Offset(14f, -34f), Offset(12f, -52f), 4.4f, cap = StrokeCap.Round) // ears
        drawLine(fur, Offset(19f, -34f), Offset(22f, -50f + sin(z.anim * 2f) * 2f), 4.4f, cap = StrokeCap.Round)
        drawLine(Color(0xFF424242), Offset(12f, -50f), Offset(12f, -53f), 3f)
        zombieEye(22f, -29f, 1.8f)
        for (i in 0..2) { // crackling arcs
            val a = z.anim * 4f + i * 2.1f
            drawLine(Color(0xFFFFF59D).copy(alpha = 0.9f), Offset(cos(a) * 14f, -20f + sin(a) * 9f), Offset(cos(a + 0.6f) * 22f, -22f + sin(a + 0.6f) * 14f), 1.6f)
        }
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A flitting spirit flame: a hollow-eyed ghost with a trailing tail of wisps. */
private fun DrawScope.drawWisp(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val glowCol = lerp(Color(0xFFB39DDB), Color.White, flash)
    val bob = sin(z.anim * 1.2f) * 3f
    withTransform({ translate(z.x, z.y - 18f * k + bob); scale(dir * k, k, Offset.Zero) }) {
        drawCircle(glowCol.copy(alpha = 0.22f), 24f, Offset.Zero)
        path.rewind(); path.moveTo(14f, 0f); path.lineTo(-4f, -14f); path.lineTo(-26f, -4f + sin(z.anim * 2f) * 4f); path.lineTo(-10f, 2f); path.lineTo(-24f, 10f + sin(z.anim * 2.4f) * 4f); path.lineTo(-2f, 12f); path.close()
        drawPath(path, glowCol.copy(alpha = 0.85f))
        drawCircle(glowCol, 11f, Offset(2f, -1f))
        drawOval(Color(0xFF311B92), Offset(0f, -6f), Size(5f, 8f))
        drawOval(Color(0xFF311B92), Offset(8f, -6f), Size(5f, 8f))
        drawCircle(Color(0xFFFF4081), 1.4f, Offset(2.5f, -3f))
        drawCircle(Color(0xFFFF4081), 1.4f, Offset(10.5f, -3f))
        for (i in 0..2) drawCircle(Color.White.copy(alpha = 0.6f), 1.5f, Offset(cos(z.anim + i * 2.1f) * 22f, sin(z.anim * 1.3f + i * 2.1f) * 15f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** A heavy rock beetle: a domed shell of stone plates, six scuttling legs and crushing mandibles. */
private fun DrawScope.drawBoulderBeetle(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val shell = lerp(Color(0xFF8D7B68), Color.White, flash)
    val dark = lerp(Color(0xFF5A4B3E), Color.White, flash)
    shadow(z, 54f)
    withTransform({ translate(z.x, z.y); scale(k * dir, k, Offset.Zero) }) {
        for (i in 0..2) {
            val sw = sin(z.anim * 1.8f + i * 2f) * 4f
            drawLine(dark, Offset(-14f + i * 12f, -12f), Offset(-20f + i * 12f + sw, 0f), 3f)
            drawLine(dark, Offset(-8f + i * 12f, -12f), Offset(-2f + i * 12f - sw, 0f), 3f)
        }
        drawArc(shell, 180f, 180f, true, Offset(-28f, -52f), Size(56f, 80f))
        for (i in 0..3) drawArc(dark, 180f, 180f, false, Offset(-28f + i * 6f, -52f + i * 3f), Size(56f - i * 12f, 80f - i * 6f), style = Stroke(1.8f)) // plates
        drawCircle(Color(0xFF7CB342).copy(alpha = 0.8f), 5f, Offset(-10f, -40f)) // moss
        drawCircle(Color(0xFF7CB342).copy(alpha = 0.8f), 3.5f, Offset(8f, -46f))
        drawOval(dark, Offset(22f, -26f), Size(18f, 14f))
        val bite = if (z.attacking) abs(sin(z.anim * 6f)) * 4f else 0f
        drawLine(Color(0xFFEFE6C8), Offset(38f, -22f - bite), Offset(46f, -16f), 3.2f, cap = StrokeCap.Round)
        drawLine(Color(0xFFEFE6C8), Offset(38f, -16f + bite), Offset(46f, -20f), 3.2f, cap = StrokeCap.Round)
        zombieEye(32f, -22f, 2.2f)
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** The Prism Drake: a crystal-spined drake whose colour is whatever element it is currently attuned to. */
private fun DrawScope.drawPrismDrake(z: Zombie, dir: Float) {
    val k = z.type.size
    val flash = z.flash * 0.75f
    val el = z.elementNow()
    val glow = if (el != null) Color(el.color) else Color.White
    val body = lerp(lerp(Color(0xFF455A64), glow, 0.55f), Color.White, flash)
    val dark = lerp(Color(0xFF263238), glow, 0.3f)
    val run = sin(z.anim * 1.5f)
    val bite = if (z.attacking) 0.5f + 0.5f * sin(z.anim * 7f) else 0f
    shadow(z, 64f)
    withTransform({ translate(z.x, z.y); scale(k * dir, k, Offset.Zero) }) {
        path.rewind(); path.moveTo(-14f, -46f); path.lineTo(-48f, -30f + sin(z.anim) * 5f); path.lineTo(-14f, -30f); path.close()
        drawPath(path, dark)
        for (leg in 0..1) {
            val ph = if (leg == 0) run else -run
            drawLine(if (leg == 0) dark else body, Offset(-2f + leg * 6f, -30f), Offset(5f + ph * 6f + leg * 6f, -17f), 9f, cap = StrokeCap.Round)
            drawLine(if (leg == 0) dark else body, Offset(5f + ph * 6f + leg * 6f, -17f), Offset(5f + ph * 10f + leg * 6f, -2f), 6f, cap = StrokeCap.Round)
        }
        drawOval(body, Offset(-22f, -58f), Size(48f, 32f))
        // Wing and crystal spines in rainbow colours.
        path.rewind(); path.moveTo(-6f, -52f); path.lineTo(-24f, -78f + sin(z.anim * 1.2f) * 4f); path.lineTo(10f, -56f); path.close()
        drawPath(path, dark)
        val rainbow = longArrayOf(0xFFFF7043, 0xFFFFEE58, 0xFF66BB6A, 0xFF29B6F6, 0xFFB39DDB)
        for (i in 0..4) {
            path.rewind(); path.moveTo(-18f + i * 9f, -56f); path.lineTo(-15f + i * 9f, -70f - (i % 2) * 5f); path.lineTo(-11f + i * 9f, -56f); path.close()
            drawPath(path, Color(rainbow[i]).copy(alpha = 0.9f))
        }
        drawLine(body, Offset(18f, -52f), Offset(30f, -70f), 14f, cap = StrokeCap.Round)
        drawOval(body, Offset(26f, -80f), Size(30f, 16f))
        drawOval(lerp(body, Color.White, 0.3f), Offset(36f, -70f + bite * 7f), Size(22f, 6f))
        path.rewind(); path.moveTo(30f, -80f); path.lineTo(24f, -94f); path.lineTo(38f, -82f); path.close()
        drawPath(path, Color(rainbow[((z.anim * 0.5f).toInt()) % 5]))
        zombieEye(40f, -76f, 2.6f)
        drawCircle(glow.copy(alpha = 0.25f + 0.15f * sin(z.anim * 3f)), 30f, Offset(0f, -44f))
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}
