package com.baraka.game.trainsurvivor.model

import java.util.Random
import kotlin.math.abs

/** The shape trail motes are drawn as (render/TrainRenderer.kt draws them). */
enum class TrailShapeKind { DOT, SPARK, DIAMOND, RIBBON, STAR }

/** One day's trail effect: everything about how it looks. */
class TrailLook(
    val name: String,
    val color1: Long,
    val color2: Long,
    val shape: TrailShapeKind,
    val spread: Float,
    val density: Int,
    val speed: Float
)

private val TRAIL_ADJECTIVES = arrayOf(
    "Solar", "Lunar", "Crimson", "Azure", "Golden", "Spectral", "Emerald", "Violet", "Infernal", "Glacial",
    "Radiant", "Shadow", "Cosmic", "Molten", "Verdant", "Obsidian", "Phantom", "Astral", "Electric", "Royal",
    "Toxic", "Celestial", "Blazing", "Frosted", "Rogue", "Ancient", "Wild", "Neon", "Blood", "Silver"
)
private val TRAIL_NOUNS = arrayOf(
    "Comet", "Ember", "Blaze", "Ribbon", "Trail", "Wisp", "Storm", "Flare", "Spark", "Nova",
    "Veil", "Echo", "Pulse", "Streak", "Dust", "Aurora", "Rift", "Glow", "Shard", "Halo",
    "Current", "Mirage", "Cinder", "Drift", "Tempest", "Glimmer", "Plume", "Cascade", "Beacon", "Surge"
)

private fun hsv(h: Float, s: Float, v: Float): Long {
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, gr, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val ri = ((r + m) * 255f).toInt().coerceIn(0, 255)
    val gi = ((gr + m) * 255f).toInt().coerceIn(0, 255)
    val bi = ((b + m) * 255f).toInt().coerceIn(0, 255)
    return (0xFFL shl 24) or (ri.toLong() shl 16) or (gi.toLong() shl 8) or bi.toLong()
}

/**
 * The trail effect for [day] (see [Progression.today]): generated purely from the day number, so the
 * same day always gives the same look (stable to save, show in the shop and share between players), yet a
 * brand new one appears every day forever - nothing here needs the app to ever be updated.
 */
fun trailForDay(day: Long): TrailLook {
    val random = Random(day * 104729L + 7L)
    val hue = random.nextFloat() * 360f
    val hue2 = (hue + 40f + random.nextFloat() * 80f) % 360f
    val name = "${TRAIL_ADJECTIVES[random.nextInt(TRAIL_ADJECTIVES.size)]} ${TRAIL_NOUNS[random.nextInt(TRAIL_NOUNS.size)]}"
    return TrailLook(
        name = name,
        color1 = hsv(hue, 0.75f + random.nextFloat() * 0.25f, 0.95f),
        color2 = hsv(hue2, 0.6f + random.nextFloat() * 0.3f, 0.9f),
        shape = TrailShapeKind.entries[random.nextInt(TrailShapeKind.entries.size)],
        spread = 10f + random.nextFloat() * 22f,
        density = 8 + random.nextInt(8),
        speed = 0.5f + random.nextFloat() * 0.9f
    )
}
