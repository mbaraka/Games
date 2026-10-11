package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.data.TrainSoundCue
import com.baraka.game.trainsurvivor.model.HazardKind
import com.baraka.game.trainsurvivor.model.ZombieType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Environmental dangers tied to a route (see [com.baraka.game.trainsurvivor.model.Biome.hazards]).
 *
 * Each hazard is a small self-contained object: the game creates it when the train enters a route that lists
 * its [HazardKind], calls [update] every frame, and retires it (no new threats, existing ones play out) when the
 * train leaves. To add a hazard: add a [HazardKind], write a class here, add it to [HazardRegistry], and draw it in
 * render/HazardArt.kt.
 */
sealed class Hazard {
    abstract val kind: HazardKind

    /** Set when the route is left: no new threats spawn, existing ones finish. */
    var retiring = false

    /** True when nothing of this hazard is left on screen, so a retiring hazard can be dropped. */
    open val idle: Boolean get() = true

    abstract fun update(g: TrainGame, dt: Float)

    /** A screen tap in world coordinates; return true if the hazard used it. */
    open fun onTap(g: TrainGame, lx: Float, ly: Float): Boolean = false

    /** The flak turret asks hazards if it can shoot one of their threats down. */
    open fun shootDown(g: TrainGame): Boolean = false

    /** Debug tools: make the next threat appear right away. */
    open fun trigger() {}
}

/** Creates hazards from route data. */
object HazardRegistry {
    fun create(kind: HazardKind): Hazard = when (kind) {
        HazardKind.METEORS -> MeteorShower()
        HazardKind.GEYSERS -> LavaGeysers()
        HazardKind.HULL_BREACH -> HullBreaches()
        HazardKind.TENTACLES -> Tentacles()
        HazardKind.OVERPASS_DROPS -> OverpassDrops()
        HazardKind.SOLAR_FLARES -> SolarFlares()
    }
}

/** A falling rock: shown as a warning, then crashes into a car unless tapped or shot down. */
class Meteor(val slot: Int, var x: Float) {
    var y = 0f
    var telegraph = 1.4f
    var falling = false
}

class MeteorShower : Hazard() {
    override val kind = HazardKind.METEORS
    val meteors = ArrayList<Meteor>(4)
    private var timer = 5f

    override val idle: Boolean get() = meteors.isEmpty()

    override fun trigger() {
        timer = 0f
    }

    override fun update(g: TrainGame, dt: Float) {
        var i = 0
        while (i < meteors.size) {
            val m = meteors[i]
            if (m.telegraph > 0f) {
                m.telegraph -= dt
                if (m.telegraph <= 0f) {
                    m.falling = true
                    m.y = -40f
                }
                i++
            } else {
                m.y += 850f * dt
                if (m.y >= 430f) {
                    g.explosion(m.x, 440f, 42f)
                    g.hurtCar(m.slot, 22f * g.danger.dmgMult * (1f + g.time / 240f))
                    g.shake = max(g.shake, 0.5f)
                    meteors[i] = meteors[meteors.size - 1]
                    meteors.removeAt(meteors.size - 1)
                } else {
                    i++
                }
            }
        }
        if (!retiring) {
            timer -= dt
            if (timer <= 0f) {
                timer = 7f + g.rng.nextFloat() * 3f
                if (meteors.size < 3) {
                    val slot = g.rng.nextInt(g.slotCount)
                    meteors.add(Meteor(slot, g.carCenterX(slot)))
                }
            }
        }
    }

    override fun onTap(g: TrainGame, lx: Float, ly: Float): Boolean {
        for (i in meteors.indices) {
            val m = meteors[i]
            if (m.falling && abs(lx - m.x) < 70f && abs(ly - m.y) < 90f) {
                g.explosion(m.x, m.y, 36f)
                g.runCoins += 4
                meteors[i] = meteors[meteors.size - 1]
                meteors.removeAt(meteors.size - 1)
                g.sound(TrainSoundCue.CRATE)
                return true
            }
        }
        return false
    }

    override fun shootDown(g: TrainGame): Boolean {
        for (i in meteors.indices) {
            val m = meteors[i]
            if (m.falling && m.y > 80f) {
                g.tracers.add(Tracer(g.flakX, g.flakY - 26f, m.x, m.y, 0.12f, 3f, 0xFFFFD54F.toInt()))
                g.explosion(m.x, m.y, 30f)
                g.sound(TrainSoundCue.FLAK)
                meteors[i] = meteors[meteors.size - 1]
                meteors.removeAt(meteors.size - 1)
                return true
            }
        }
        return false
    }
}

/** A lava geyser: warns, then erupts and burns everything standing on it. */
class Geyser(val x: Float) {
    var telegraph = 1.0f
    var erupt = 0f
}

class LavaGeysers : Hazard() {
    override val kind = HazardKind.GEYSERS
    val geysers = ArrayList<Geyser>(4)
    private var timer = 4f

    override val idle: Boolean get() = geysers.isEmpty()

    override fun trigger() {
        timer = 0f
    }

    override fun update(g: TrainGame, dt: Float) {
        var i = 0
        while (i < geysers.size) {
            val gz = geysers[i]
            if (gz.telegraph > 0f) {
                gz.telegraph -= dt
                if (gz.telegraph <= 0f) erupt(g, gz)
                i++
            } else {
                gz.erupt -= dt
                if (gz.erupt <= 0f) {
                    geysers[i] = geysers[geysers.size - 1]
                    geysers.removeAt(geysers.size - 1)
                } else {
                    i++
                }
            }
        }
        if (!retiring) {
            timer -= dt
            if (timer <= 0f) {
                timer = 4.5f + g.rng.nextFloat() * 2f
                if (geysers.size < 3) geysers.add(Geyser(80f + g.rng.nextFloat() * (g.worldW - 160f)))
            }
        }
    }

    private fun erupt(g: TrainGame, gz: Geyser) {
        gz.erupt = 0.6f
        g.explosion(gz.x, 560f, 40f)
        g.shooter = -1
        val damage = 160f + g.time * 1.5f
        for (k in g.zombies.indices) {
            val z = g.zombies[k]
            if (!z.dead && !z.flying && z.jumpState == 0 && abs(z.x - gz.x) < 75f) g.damageZombie(z, damage)
        }
        if (gz.x > g.rearEnd - 40f && gz.x < g.frontEnd + 40f) g.hurtCar(g.slotAt(gz.x), 14f * g.danger.dmgMult)
    }
}

/** A hull breach: tap the car to seal it before the air (and 12% of your run's coins) runs out. */
class Breach(val slot: Int) {
    var timeLeft = 6f
}

class HullBreaches : Hazard() {
    override val kind = HazardKind.HULL_BREACH
    var breach: Breach? = null
    private var timer = 12f

    override val idle: Boolean get() = breach == null

    override fun trigger() {
        timer = 0f
    }

    override fun update(g: TrainGame, dt: Float) {
        val b = breach
        if (b != null) {
            b.timeLeft -= dt
            if (b.timeLeft <= 0f) {
                breach = null
                val lost = (g.runCoins * 0.12f).toInt()
                g.runCoins -= lost
                g.hurtCar(b.slot, 22f * g.danger.dmgMult)
                g.floatText(g.carCenterX(b.slot), 380f, "Hull breach! -$lost coins", 1.6f, 0xFFFF5252.toInt(), 22f)
            }
        }
        if (!retiring) {
            timer -= dt
            if (timer <= 0f) {
                timer = 22f + g.rng.nextFloat() * 8f
                if (breach == null) breach = Breach(g.rng.nextInt(g.slotCount))
            }
        }
    }

    override fun onTap(g: TrainGame, lx: Float, ly: Float): Boolean {
        val b = breach ?: return false
        if (abs(lx - g.carCenterX(b.slot)) < 95f && ly > 350f && ly < 520f) {
            breach = null
            val coins = ((15f + g.time * 0.2f) * g.coinMultiplier).roundToInt()
            g.runCoins += coins
            g.floatText(g.carCenterX(b.slot), 380f, "Sealed! +$coins", 1.2f, 0xFF66FF8A.toInt(), 22f)
            g.sound(TrainSoundCue.CRATE)
            return true
        }
        return false
    }
}

/** Sea-bridge tentacles: spawns tentacle enemies that grab wagons from below (they are ordinary enemies once spawned). */
class Tentacles : Hazard() {
    override val kind = HazardKind.TENTACLES
    private var timer = 5f

    override fun trigger() {
        timer = 0f
    }

    override fun update(g: TrainGame, dt: Float) {
        if (retiring) return
        timer -= dt
        if (timer > 0f) return
        timer = (7f + g.rng.nextFloat() * 3f) / g.danger.spawnMult
        if (g.zombies.size >= MAX_ZOMBIES) return
        var count = 0
        for (k in g.zombies.indices) if (!g.zombies[k].dead && g.zombies[k].type == ZombieType.TENTACLE) count++
        if (count >= 2 + (if (g.slotCount > 3) 1 else 0)) return
        val slot = g.rng.nextInt(g.slotCount)
        for (k in g.zombies.indices) {
            val o = g.zombies[k]
            if (!o.dead && o.type == ZombieType.TENTACLE && g.slotAt(o.x) == slot) return
        }
        g.addZombie(ZombieType.TENTACLE, g.carCenterX(slot) + (g.rng.nextFloat() - 0.5f) * 40f, 600f, 1)
    }
}

/** City overpass: a concrete span slides over the train and zombies drop onto the roofs. */
class OverpassDrops : Hazard() {
    override val kind = HazardKind.OVERPASS_DROPS
    /** Seconds the overpass is still overhead (the renderer draws it while this is above zero). */
    var overpassTime = 0f
    private var timer = 8f

    override val idle: Boolean get() = overpassTime <= 0f

    override fun trigger() {
        timer = 0f
    }

    override fun update(g: TrainGame, dt: Float) {
        overpassTime = max(0f, overpassTime - dt)
        if (retiring) return
        timer -= dt
        if (timer > 0f) return
        timer = 16f + g.rng.nextFloat() * 6f
        overpassTime = 2.8f
        repeat(2 + g.danger.ordinal / 2) {
            if (g.zombies.size < MAX_ZOMBIES) {
                val type = if (g.rng.nextFloat() < 0.3f) ZombieType.RIOT else ZombieType.WALKER
                val z = g.addZombie(type, 0f, -40f, 1)
                g.startDrop(z, g.rng.nextInt(g.slotCount))
            }
        }
    }
}

/**
 * Solar flares: a warning counts down, then a heat wave burns every wagon and every flying enemy that isn't
 * fire-proof. Tapping during the warning raises the shields, which protects the wagons.
 */
class SolarFlares : Hazard() {
    override val kind = HazardKind.SOLAR_FLARES
    /** Seconds until the flare strikes (the renderer shows a warning while this is positive). */
    var warning = 0f
    var shielded = false
    /** White-hot flash right after a strike. */
    var flash = 0f
    private var timer = 10f

    override val idle: Boolean get() = warning <= 0f && flash <= 0f

    override fun trigger() {
        timer = 0f
    }

    override fun update(g: TrainGame, dt: Float) {
        flash = max(0f, flash - dt)
        if (warning > 0f) {
            warning -= dt
            if (warning <= 0f) strike(g)
        } else if (!retiring) {
            timer -= dt
            if (timer <= 0f) {
                timer = 14f + g.rng.nextFloat() * 6f
                warning = 2.6f
                shielded = false
                g.sound(TrainSoundCue.HORDE)
            }
        }
    }

    private fun strike(g: TrainGame) {
        flash = 0.5f
        g.shake = max(g.shake, 0.6f)
        if (!shielded) {
            for (slot in 0 until g.slotCount) if (g.carAlive(slot)) g.hurtCar(slot, 14f * g.danger.dmgMult)
        }
        g.shooter = -1
        val burn = 60f + g.time * 0.8f
        for (k in g.zombies.indices) {
            val z = g.zombies[k]
            if (!z.dead && z.flying && !z.isBoss && !z.type.fireImmune) g.damageZombie(z, burn)
        }
        shielded = false
    }

    override fun onTap(g: TrainGame, lx: Float, ly: Float): Boolean {
        if (warning <= 0f || shielded) return false
        shielded = true
        g.floatText(g.worldW / 2f, 330f, "Shields up!", 1.2f, 0xFF40E0FF.toInt(), 24f)
        g.sound(TrainSoundCue.CRATE)
        return true
    }

}
