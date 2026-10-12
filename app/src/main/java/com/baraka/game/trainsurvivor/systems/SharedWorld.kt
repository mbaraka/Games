package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.model.EliteTrait
import com.baraka.game.trainsurvivor.model.ZombieType
import kotlin.math.abs
import kotlin.math.min

/*
 * SHARED WORLD (co-op maps)
 *
 * A map with `Biome.sharedWorld = true` turns the two trains into one battlefield. Each phone still runs its own train
 * and its own zombies (so nothing about the solo engine changes), but it also streams its zombies to the partner, who sees
 * them on the far track next to the partner's train and can shoot them across the gap. Hits are sent back to the phone
 * that owns the zombie, which applies them. Because it is generic, every future shared map is just a Biome entry with
 * sharedWorld = true plus its scenery; this file, the messages and the renderer do not change.
 */

/** The most partner zombies mirrored at once (also caps what is sent each snapshot). */
const val SHARED_MAX = 40
/** How big the far track is drawn compared with ours, and where its ground line is. */
const val FAR_SCALE = 0.5f
const val FAR_GROUND = 438f
/** Gunners reach this fraction of their normal range across the gap. */
const val ASSIST_RANGE = 0.75f
/** Damage multiplier for shots fired across the gap. */
const val ASSIST_DAMAGE = 0.8f

class SharedWorld {
    /** The partner's zombies, in OUR view coordinates (already projected onto the far track). */
    val mirrored = ArrayList<Zombie>(SHARED_MAX)
    private var stamp = 0
    private var nextNetId = 0

    // Hits we scored on the partner's zombies, waiting to be sent: (their zombie id, damage).
    private val hitIds = IntArray(64)
    private val hitDamage = FloatArray(64)
    private var hitCount = 0

    /** Gives [z] a network id the first time it is streamed. */
    fun idOf(z: Zombie): Int {
        if (z.netId == 0) z.netId = ++nextNetId
        return z.netId
    }

    fun clear() {
        mirrored.clear()
    }

    /**
     * Takes the partner's snapshot. [centerX] is where the partner's train sits in our view; positions arrive relative to the
     * partner's own train centre ([xs]) and in their world height ([ys]), and are shrunk onto the far track here.
     */
    fun apply(ids: IntArray, types: IntArray, xs: IntArray, ys: IntArray, hpPercent: IntArray, flags: IntArray, centerX: Float, railY: Float) {
        stamp++
        for (n in ids.indices) {
            val type = ZombieType.entries.getOrNull(types[n]) ?: continue
            val px = centerX + xs[n] * FAR_SCALE
            val py = FAR_GROUND + (ys[n] - railY) * FAR_SCALE
            var z: Zombie? = null
            for (k in mirrored.indices) if (mirrored[k].netId == ids[n]) { z = mirrored[k]; break }
            if (z == null) {
                if (mirrored.size >= SHARED_MAX) continue
                z = Zombie(type, px, py, 1, type.hp, 1f, (ids[n] % 97) / 97f)
                z.netId = ids[n]
                mirrored.add(z)
            }
            z.netX = px
            z.netY = py
            z.hp = z.maxHp * hpPercent[n] / 100f
            z.attacking = flags[n] and 1 != 0
            z.facing = if (flags[n] and 2 != 0) 1 else -1
            val elite = (flags[n] shr 2) and 7
            z.elite = if (elite == 0) null else EliteTrait.entries.getOrNull(elite - 1)
            z.netStamp = stamp
        }
        var i = mirrored.size - 1
        while (i >= 0) {
            if (mirrored[i].netStamp != stamp) mirrored.removeAt(i)
            i--
        }
    }

    /** Eases the mirrored zombies towards their latest positions and drops ones that stopped being sent. */
    fun update(dt: Float) {
        val k = min(1f, dt * 10f)
        var i = mirrored.size - 1
        while (i >= 0) {
            val z = mirrored[i]
            z.x += (z.netX - z.x) * k
            z.y += (z.netY - z.y) * k
            if (!z.attacking) z.anim += dt * 5f
            z.flash = (z.flash - dt * 6f).coerceAtLeast(0f)
            i--
        }
    }

    /** The mirrored zombie nearest to a gun at [gunX] within [range] (horizontally), or null. Allocation-free. */
    fun nearest(gunX: Float, range: Float, groundOnly: Boolean): Zombie? {
        var best: Zombie? = null
        var bestD = range
        for (i in mirrored.indices) {
            val z = mirrored[i]
            if (z.hp <= 0f) continue
            if (groundOnly && z.flying) continue
            val d = abs(z.x - gunX)
            if (d < bestD) { bestD = d; best = z }
        }
        return best
    }

    /** Records a shot at a partner zombie and shows the damage right away (the owner decides whether it dies). */
    fun hit(z: Zombie, damage: Float) {
        if (hitCount < hitIds.size) {
            hitIds[hitCount] = z.netId
            hitDamage[hitCount] = damage
            hitCount++
        }
        z.flash = 1f
        z.hp = (z.hp - damage).coerceAtLeast(z.maxHp * 0.02f)
    }

    val hasHits get() = hitCount > 0

    /** Hands over the queued hits and empties the queue. */
    fun takeHits(): Pair<IntArray, FloatArray> {
        val ids = hitIds.copyOf(hitCount)
        val dmg = hitDamage.copyOf(hitCount)
        hitCount = 0
        return ids to dmg
    }
}
