package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.Zombie
import kotlin.math.sin

// Area bosses (see ZombieType and Biome.boss). Most are bigger versions of an existing enemy, so their art is an
// alias: the type's own `size` scales it up. To give a new boss unique art, register a MobArt for it instead.
internal fun registerBossArts() {
    val l = MobLooks
    l.set(ZombieType.YETI_KING, 0xFFE3F2FD, 0xFFB3E5FC)
    l.set(ZombieType.LUNAR_COLOSSUS, 0xFF86B864, 0xFFF5F5F5)
    l.set(ZombieType.MAGMA_TITAN, 0xFF3E2723, 0xFF2B1D1A)
    l.set(ZombieType.OUTLAW_KING, 0xFF86B864, 0xFF4E342E)
    l.set(ZombieType.SUN_KING, 0xFFE0A070, 0xFFFFA000)
    l.set(ZombieType.STRIKER, 0xFF86B864, 0xFF6A1B9A)
    l.set(ZombieType.TWIN_BULWARK, 0xFF86B864, 0xFF00838F)
    l.set(ZombieType.MIRROR_WARDEN, 0xFFB39DDB, 0xFF4A148C)

    // Animal bosses reuse their cousin's body (drawn bigger by their size).
    MobArtRegistry.alias(ZombieType.RAT_KING, ZombieType.RAT)
    MobArtRegistry.alias(ZombieType.SCORPION_QUEEN, ZombieType.SCORPION)
    MobArtRegistry.alias(ZombieType.SWAMP_TYRANT, ZombieType.CROCODILE)
    MobArtRegistry.alias(ZombieType.MEGALODON, ZombieType.SHARK)

    // Flying bosses are scaled-up versions of small flyers.
    MobArtRegistry.register(ZombieType.OVERSEER) { s, z, d -> with(s) { drawDrone(z, d, 2.6f) } }
    MobArtRegistry.register(ZombieType.FROST_QUEEN) { s, z, d -> with(s) { drawWraith(z, d, 2.4f) } }
    MobArtRegistry.register(ZombieType.ANGLERFISH) { s, z, d ->
        with(s) {
            drawPiranha(z, d, 3f)
            drawAnglerLure(z, d)
        }
    }
    MobArtRegistry.register(ZombieType.SPHINX) { s, z, d -> with(s) { drawSphinx(z, d) } }

    // Humanoid bosses wear their cousin's accessories (hats, shields, glowing cracks...).
    HumanoidExtras.alias(ZombieType.LUNAR_COLOSSUS, ZombieType.LEAPER)
    HumanoidExtras.alias(ZombieType.MAGMA_TITAN, ZombieType.MAGMA_GOLEM)
    HumanoidExtras.alias(ZombieType.OUTLAW_KING, ZombieType.GUNSLINGER)
    HumanoidExtras.alias(ZombieType.SUN_KING, ZombieType.SUN_PRIEST)
    HumanoidExtras.alias(ZombieType.TWIN_BULWARK, ZombieType.RIOT)
    HumanoidExtras.alias(ZombieType.MIRROR_WARDEN, ZombieType.GUNSLINGER)

    val p = PortraitSizes
    p.set(ZombieType.RAT_KING, 110f)
    p.set(ZombieType.SCORPION_QUEEN, 130f)
    p.set(ZombieType.SWAMP_TYRANT, 140f)
    p.set(ZombieType.OVERSEER, 120f, floating = 0.6f)
    p.set(ZombieType.FROST_QUEEN, 150f)
    p.set(ZombieType.ANGLERFISH, 140f, floating = 0.6f)
    p.set(ZombieType.MEGALODON, 150f, floating = 0.6f)
    p.set(ZombieType.SPHINX, 150f)
}

/** The Sphinx: a reclining stone lion with a pharaoh's head, guarding Pyramid Valley. */
private fun DrawScope.drawSphinx(z: Zombie, dir: Float) {
    val flash = z.flash * 0.75f
    fun tint(argb: Int) = lerp(Color(argb), Color.White, flash)
    val stone = tint(0xFFD9C08A.toInt())
    val dark = tint(0xFFB39656.toInt())
    val gold = tint(0xFFD4AF37.toInt())
    val lapis = tint(0xFF1565C0.toInt())
    val k = 1.6f
    withTransform({ translate(z.x, z.y - 4f * k); scale(k * dir, k, Offset.Zero) }) {
        path.rewind(); path.moveTo(-46f, -12f); path.lineTo(-58f, -20f); path.lineTo(-64f, -36f); path.lineTo(-58f, -34f); path.close()
        drawPath(path, stone) // tail, curling up the flank
        drawOval(stone, Offset(-50f, -34f), Size(26f, 30f)) // haunches
        rbox(stone, -44f, -26f, 86f, 20f, 10f) // reclining body
        box(dark, -40f, -8f, 30f, 8f) // front paws
        box(dark, -8f, -8f, 30f, 8f)
        rbox(stone, 28f, -52f, 24f, 26f, 4f) // head
        box(gold, 26f, -56f, 28f, 7f) // nemes crown band
        path.rewind(); path.moveTo(26f, -50f); path.lineTo(20f, -34f); path.lineTo(28f, -34f); path.close()
        drawPath(path, gold) // left lappet
        path.rewind(); path.moveTo(54f, -50f); path.lineTo(60f, -34f); path.lineTo(52f, -34f); path.close()
        drawPath(path, gold) // right lappet
        box(lapis, 26f, -53f, 28f, 1.8f) // lapis stripe
        drawCircle(Color.Black, 2f, Offset(44f, -42f)) // eye
    }
    if (z.hp < z.maxHp) drawHpBar(z)
}

/** The angler's glowing lure, dangling on a stalk in front of its jaws. */
private fun DrawScope.drawAnglerLure(z: Zombie, dir: Float) {
    val k = 3f
    val x0 = z.x + dir * 6f * k
    val y0 = z.y - 16f * k - 8f * k
    val x1 = z.x + dir * 30f * k
    val y1 = y0 - 10f * k + sin(z.anim * 2f) * 6f
    drawLine(Color(0xFF263238), Offset(x0, y0), Offset(x1, y1), 4f)
    drawCircle(Color(0xFFFFF59D).copy(alpha = 0.35f), 22f, Offset(x1, y1))
    drawCircle(Color(0xFFFFEB3B), 8f, Offset(x1, y1))
}
