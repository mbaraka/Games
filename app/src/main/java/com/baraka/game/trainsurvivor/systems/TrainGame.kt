package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.data.TrainSoundCue
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.TrainUpgrade
import com.baraka.game.trainsurvivor.model.WeaponType
import com.baraka.game.trainsurvivor.model.ZombieType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Logical world is 720 units tall; the renderer scales it to the screen. Everything below is in those units. */
const val RAIL_Y = 500f
const val LOCO_W = 190f
const val CAR_W = 150f
const val CAR_GAP = 8f
const val COWCATCHER = 34f
const val WORLD_SPEED = 170f
private const val MAX_ATTACKERS_PER_SIDE = 3
private const val MAX_ZOMBIES = 70
private const val MAX_PARTICLES = 420

enum class ParticleKind { BLOOD, SPARK, SMOKE, COIN, FIRE, DEBRIS, HEAL }

class Zombie(val type: ZombieType, var x: Float, var y: Float, val side: Int, val maxHp: Float, val dmgScale: Float, val seed: Float) {
    var hp = maxHp
    var anim = seed * 6f
    var flash = 0f
    var attacking = false
    var dead = false
    val isBoss get() = type == ZombieType.TITAN
    val flying get() = type.flying
    /** Which way the zombie looks (+1 right); ground zombies face the train, flyers face their target. */
    var facing = -side
    var targetX = x
    var cruiseY = y
    var retarget = 0f
}

/** One member of the visible repair crew walking the train's running board. */
class Repairer(var x: Float, var dir: Int) {
    var hammering = false
    var timer = 0.5f
    var anim = 0f
    var effectTimer = 0f
}

class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var life: Float, val maxLife: Float, val size: Float, val color: Int, val kind: ParticleKind
)

class Tracer(val x1: Float, val y1: Float, val x2: Float, val y2: Float, var life: Float, val width: Float, val color: Int, val jagged: Boolean = false)

/** A fork in the track: the player has until [timeLeft] runs out to pick; [chosen] is set once they (or the timeout) do. */
class Junction(val optionA: Biome, val optionB: Biome, var timeLeft: Float) {
    var chosen: Biome? = null
}

class FloatingText(var x: Float, var y: Float, val text: String, var life: Float, val color: Int)

/** Per-passenger runtime state the renderer needs (aim, recoil, muzzle flash). */
class GunnerState {
    var cooldown = 0f
    var recoil = 0f
    var flash = 0f
    var aim = -0.15f
    var facing = 1
}

fun weaponLength(weapon: WeaponType): Float = when (weapon) {
    WeaponType.PISTOL -> 18f
    WeaponType.SMG -> 26f
    WeaponType.SHOTGUN -> 36f
    WeaponType.RIFLE -> 44f
    WeaponType.MINIGUN -> 38f
    WeaponType.CANNON -> 46f
    WeaponType.ROCKET -> 46f
    WeaponType.LASER -> 44f
}

/**
 * The whole simulation for one run (or for the garage preview with [spawnEnabled] = false).
 * It is mutable on purpose — this is the hot loop and the renderer reads it directly every
 * frame. All gameplay decisions live here; render/TrainRenderer.kt only draws.
 */
class TrainGame(private val save: SaveData, private val spawnEnabled: Boolean = true, private val rng: Random = Random(System.nanoTime())) {

    var onSound: ((TrainSoundCue) -> Unit)? = null
    var worldW = 1280f

    // ---- train ----
    val slotCount = save.unlockedSlots
    val crew = save.slots.take(slotCount)
    val gunners = List(slotCount) { GunnerState() }
    val armorLevel = save.level(TrainUpgrade.ARMOR)
    val spikeLevel = save.level(TrainUpgrade.SPIKES)
    val dynamiteLevel = save.level(TrainUpgrade.DYNAMITE)
    val flakLevel = save.level(TrainUpgrade.FLAK)
    val teslaLevel = save.level(TrainUpgrade.TESLA)
    val repairLevel = save.level(TrainUpgrade.REPAIR)
    val repairers: List<Repairer> =
        if (repairLevel > 0) List(((repairLevel + 1) / 2).coerceIn(1, 4)) { i -> Repairer(0f, if (i % 2 == 0) 1 else -1).also { it.timer = 0.4f + i * 0.7f } } else emptyList()
    val maxHp = 100f + 40f * armorLevel
    var hp = maxHp
    private val regenPerSecond = 1.5f * save.level(TrainUpgrade.REPAIR)
    private val thornsPerSecond = 7f * spikeLevel
    private val bountyMultiplier = 1f + 0.12f * save.level(TrainUpgrade.BOUNTY)
    val trainLength = LOCO_W + (slotCount - 1) * (CAR_W + CAR_GAP)
    val trainLeft get() = worldW / 2f - trainLength / 2f
    val locoLeft get() = trainLeft + trainLength - LOCO_W
    val frontEnd get() = trainLeft + trainLength + COWCATCHER
    val rearEnd get() = trainLeft

    // ---- run state ----
    var time = 0f
    var scroll = 0f
    var wheelAngle = 0f
    var distance = 0f
    var kills = 0
    var bossKills = 0
    var runCoins = 0
    var over = false
    var shake = 0f
    var hitFlash = 0f
    var bossAlert = 0f
    var dynamiteCooldown = 0f
    val dynamiteCooldownMax = 16f - 1.5f * dynamiteLevel
    val dynamiteDamage = 90f + 70f * dynamiteLevel
    var boss: Zombie? = null
    var flakCooldown = 0f
    var flakFlash = 0f
    var teslaCooldown = 1.5f
    var teslaFlash = 0f
    private var healTextGap = 0f

    // ---- routes ----
    var biome = Biome.PLAINS
    var fromBiome = Biome.PLAINS
    var nextBiome = Biome.PLAINS
    var junction: Junction? = null
    var transitioning = false
    /** While [transitioning], the screen x where the old biome (left) meets the new one (right); it sweeps left as the train drives on. */
    var boundaryX = 0f
    var biomeBanner = 0f
    private var biomeSwitched = false
    private var nextJunctionAt = 40f

    val zombies = ArrayList<Zombie>()
    val particles = ArrayList<Particle>()
    val tracers = ArrayList<Tracer>()
    val texts = ArrayList<FloatingText>()

    private var spawnAccumulator = 0f
    private var smokeAccumulator = 0f
    private var nextBossAt = 75f
    private var bossNumber = 0
    private var hitSoundGap = 0f
    private val sideBuffer = ArrayList<Zombie>()

    /** Coins banked when the run ends: kill coins plus 1 per 25 m travelled. */
    val finalCoins get() = runCoins + (distance / 25f).toInt()
    val distanceMeters get() = distance.roundToInt()
    val dynamiteReady get() = dynamiteCooldown <= 0f

    /** 0 = deep night, 1 = bright noon; also exposes the cycle position for sun/moon placement. */
    val dayCycle get() = ((time + 17f) % 170f) / 170f

    // ---- geometry helpers shared with the renderer ----
    fun gunX(slot: Int): Float =
        if (slot == 0) locoLeft + 38f else locoLeft - CAR_GAP - CAR_W / 2f - (slot - 1) * (CAR_W + CAR_GAP)

    fun gunY(slot: Int): Float = if (slot == 0) 388f else 420f
    fun shoulderY(slot: Int): Float = gunY(slot) - 30f
    fun endX(side: Int): Float = if (side > 0) frontEnd else rearEnd
    val repairBoardY = 477f
    /** Flak turret sits on the rear car's roof; Tesla coil on the cab roof. */
    val flakX get() = if (slotCount > 1) trainLeft + 24f else locoLeft + 56f
    val flakY get() = if (slotCount > 1) 418f else 388f
    val teslaX get() = locoLeft + 62f
    val teslaY get() = 388f

    fun update(dt: Float) {
        time += dt
        scroll += WORLD_SPEED * dt
        wheelAngle += WORLD_SPEED * dt / 17f
        shake = max(0f, shake - dt * 2.2f)
        hitFlash = max(0f, hitFlash - dt * 3f)
        bossAlert = max(0f, bossAlert - dt)
        biomeBanner = max(0f, biomeBanner - dt)
        hitSoundGap -= dt

        if (!over) {
            distance += 14f * dt
            dynamiteCooldown = max(0f, dynamiteCooldown - dt)
            hp = min(maxHp, hp + regenPerSecond * dt)
            if (spawnEnabled) { updateRoute(dt); spawn(dt) }
            updateZombies(dt)
            updateGunners(dt)
            updateTrainWeapons(dt)
            updateRepairers(dt)
            if (hp <= 0f) endRun()
        } else {
            for (g in gunners) { g.flash = 0f }
        }
        emitSmoke(dt)
        updateEffects(dt)
        zombies.removeAll { it.dead }
        if (boss?.dead == true) boss = null
    }

    // ---------------------------------------------------------------- routes

    private fun updateRoute(dt: Float) {
        val j = junction
        if (j == null && !transitioning && time >= nextJunctionAt) {
            val options = Biome.entries.filter { it != biome }.shuffled(rng)
            junction = Junction(options[0], options[1], 8f)
        } else if (j != null && j.chosen == null) {
            j.timeLeft -= dt
            if (j.timeLeft <= 0f) chooseRoute(j.optionA)
        }
        if (transitioning) {
            boundaryX -= WORLD_SPEED * dt
            if (!biomeSwitched && boundaryX <= worldW / 2f) {
                biomeSwitched = true
                biome = nextBiome
                biomeBanner = 4f
                sound(TrainSoundCue.UPGRADE)
            }
            if (boundaryX < -200f) {
                transitioning = false
                junction = null
                nextJunctionAt = time + 50f
            }
        }
    }

    /** Throws the switch: the chosen route's scenery sweeps in from the right and the gameplay modifiers apply once it reaches the train. */
    fun chooseRoute(choice: Biome) {
        val j = junction ?: return
        if (j.chosen != null || transitioning) return
        j.chosen = choice
        fromBiome = biome
        nextBiome = choice
        boundaryX = worldW + 60f
        biomeSwitched = false
        transitioning = true
        sound(TrainSoundCue.UPGRADE)
    }

    fun useDynamite() {
        if (over || dynamiteCooldown > 0f) return
        dynamiteCooldown = dynamiteCooldownMax
        shake = 0.8f
        sound(TrainSoundCue.DYNAMITE)
        var cx = trainLeft - 20f
        while (cx < trainLeft + trainLength + 40f) {
            explosion(cx, 420f, 26f)
            cx += 70f
        }
        for (z in zombies.toList()) {
            if (z.dead || z.x < -20f || z.x > worldW + 20f) continue
            explosion(z.x, z.y - 20f, 22f)
            damageZombie(z, dynamiteDamage)
        }
    }

    // ---------------------------------------------------------------- spawning

    private fun spawn(dt: Float) {
        if (time >= nextBossAt) {
            spawnBoss()
            nextBossAt += 80f
        }
        val rate = (0.45f + time * 0.011f).coerceAtMost(2.8f) * biome.spawnMult
        spawnAccumulator += dt * rate
        while (spawnAccumulator >= 1f) {
            spawnAccumulator -= 1f
            if (zombies.size < MAX_ZOMBIES) spawnZombie(pickType())
        }
    }

    private fun pickType(): ZombieType {
        var runner = if (time > 20f) 4f + time / 30f else 0f
        var brute = if (time > 70f) 1.5f + (time - 70f) / 60f else 0f
        var bloater = if (time > 45f) 2.5f else 0f
        var bat = if (time > 25f) 3f + time / 40f else 0f
        var balloon = if (time > 40f) 2f + time / 80f else 0f
        val clown = if (time > 30f) 2f else 0f
        when (biome) {
            Biome.DESERT -> runner = max(runner, 4f) * 2.5f
            Biome.MOUNTAIN -> {
                brute = (if (time > 40f) max(brute, 1.5f) else brute) * 1.8f
                balloon *= 1.6f
            }
            Biome.GRAVEYARD -> {
                brute = (if (time > 30f) max(brute, 1.5f) else brute) * 2.5f
                bloater = (if (time > 20f) max(bloater, 2.5f) else bloater) * 3f
                bat = (if (time > 10f) max(bat, 3f) else bat) * 2f
            }
            Biome.TUNNEL -> bat = (if (time > 10f) max(bat, 3f) else bat) * 2.5f
            else -> {}
        }
        val weights = floatArrayOf(10f, runner, brute, bloater, bat, balloon, clown)
        val types = arrayOf(ZombieType.WALKER, ZombieType.RUNNER, ZombieType.BRUTE, ZombieType.BLOATER, ZombieType.BAT, ZombieType.BALLOON, ZombieType.CLOWN)
        var roll = rng.nextFloat() * weights.sum()
        for (i in types.indices) {
            roll -= weights[i]
            if (roll <= 0f) return types[i]
        }
        return ZombieType.WALKER
    }

    private fun spawnZombie(type: ZombieType) {
        val side = if (rng.nextBoolean()) 1 else -1
        val x = if (side > 0) worldW + 40f + rng.nextFloat() * 80f else -40f - rng.nextFloat() * 80f
        val hpScale = (1f + time / 45f) * biome.hpMult
        val feetY = if (type.flying) 110f + rng.nextFloat() * 170f else RAIL_Y + 24f + rng.nextFloat() * 36f
        val z = Zombie(type, x, feetY, side, type.hp * hpScale, (1f + time / 140f) * biome.dmgMult, rng.nextFloat())
        if (type.flying) {
            z.targetX = trainLeft + rng.nextFloat() * (trainLength + COWCATCHER)
            z.facing = if (z.targetX > x) 1 else -1
        }
        zombies.add(z)
    }

    private fun spawnBoss() {
        val side = if (rng.nextBoolean()) 1 else -1
        val x = if (side > 0) worldW + 120f else -120f
        val hp = ZombieType.TITAN.hp * (1f + 0.9f * bossNumber)
        val titan = Zombie(ZombieType.TITAN, x, RAIL_Y + 30f, side, hp, 1f + time / 200f, 0.5f)
        zombies.add(titan)
        boss = titan
        bossNumber++
        bossAlert = 3.5f
        sound(TrainSoundCue.BOSS)
    }

    // ---------------------------------------------------------------- zombies

    private fun updateZombies(dt: Float) {
        for (z in zombies) if (!z.dead && z.flying) updateFlyer(z, dt)
        for (side in intArrayOf(-1, 1)) {
            sideBuffer.clear()
            val end = endX(side)
            for (z in zombies) if (!z.dead && !z.flying && z.side == side) sideBuffer.add(z)
            sideBuffer.sortBy { abs(it.x - end) }
            for ((rank, z) in sideBuffer.withIndex()) {
                val spot = end + side * (14f + min(rank, 16) * 22f * z.type.size.coerceAtLeast(1f))
                val delta = spot - z.x
                val step = z.type.speed * dt
                val arrived = abs(delta) <= step + 1f
                if (arrived) z.x = spot else z.x += if (delta > 0f) step else -step
                z.facing = -side
                z.anim += dt * (z.type.speed / 14f + 2f)
                z.flash = max(0f, z.flash - dt * 6f)
                z.attacking = arrived && rank < MAX_ATTACKERS_PER_SIDE
                if (z.attacking) attack(z, dt)
            }
        }
    }

    /** Flyers ignore the queue: they cruise high, swoop over a car on the train and claw it from above. */
    private fun updateFlyer(z: Zombie, dt: Float) {
        val isBat = z.type == ZombieType.BAT
        z.anim += dt * (if (isBat) 12f else 5f)
        z.flash = max(0f, z.flash - dt * 6f)
        z.retarget -= dt
        val dx = z.targetX - z.x
        if (isBat && z.retarget <= 0f) {
            z.retarget = 1.8f + rng.nextFloat() * 1.5f
            z.targetX = trainLeft + rng.nextFloat() * (trainLength + COWCATCHER)
        }
        if (abs(dx) > 14f) {
            z.facing = if (dx > 0f) 1 else -1
            z.x += (if (dx > 0f) 1f else -1f) * min(abs(dx), z.type.speed * dt)
        }
        if (isBat) z.x += sin(z.anim * 0.7f) * 40f * dt
        val hoverY = if (isBat) 418f else 402f
        val closeIn = abs(dx) < 320f
        val wantY = (if (closeIn) hoverY else z.cruiseY) + sin(z.anim * 0.5f) * (if (isBat) 10f else 6f)
        val dy = wantY - z.y
        z.y += (if (dy > 0f) 1f else -1f) * min(abs(dy), 110f * dt)
        z.attacking = abs(dx) < 26f && abs(z.y - hoverY) < 24f
        if (z.attacking) hurtTrain(z.type.damage * z.dmgScale * dt)
    }

    private fun attack(z: Zombie, dt: Float) {
        if (z.type == ZombieType.BLOATER) {
            // Bloaters burst on contact instead of clawing.
            z.dead = true
            hurtTrain(30f * z.dmgScale)
            explosion(z.x, z.y - 20f, 34f)
            greenBurst(z.x, z.y - 24f, 22)
            return
        }
        hurtTrain(z.type.damage * z.dmgScale * dt)
        if (thornsPerSecond > 0f) {
            damageZombie(z, thornsPerSecond * dt, silent = true)
            if (rng.nextFloat() < dt * 8f) spark(z.x, z.y - 24f)
        }
    }

    private fun hurtTrain(amount: Float) {
        hp -= amount
        hitFlash = max(hitFlash, if (amount > 3f) 0.5f else hitFlash)
        if (hitSoundGap <= 0f) {
            hitSoundGap = 0.25f
            sound(TrainSoundCue.TRAIN_HIT)
        }
    }

    private fun damageZombie(z: Zombie, amount: Float, silent: Boolean = false) {
        if (z.dead) return
        z.hp -= amount
        if (!silent) z.flash = 1f
        if (z.hp <= 0f) killZombie(z)
    }

    private fun killZombie(z: Zombie) {
        z.dead = true
        kills++
        val coins = max(1, (z.type.coins * bountyMultiplier * biome.coinMult).roundToInt())
        runCoins += coins
        texts.add(FloatingText(z.x, z.y - 60f * z.type.size, "+$coins", 1.0f, 0xFFFFD54F.toInt()))
        greenBurst(z.x, z.y - 24f * z.type.size, if (z.isBoss) 60 else 12)
        repeat(if (z.isBoss) 18 else min(4, 1 + coins / 3)) {
            addParticle(Particle(z.x, z.y - 30f, (rng.nextFloat() - 0.5f) * 160f, -150f - rng.nextFloat() * 120f, 0.9f, 0.9f, 5f, 0xFFFFC107.toInt(), ParticleKind.COIN))
        }
        sound(TrainSoundCue.ZOMBIE_DIE)
        sound(TrainSoundCue.COIN)
        if (z.isBoss) {
            bossKills++
            shake = 0.7f
            explosion(z.x, z.y - 60f, 60f)
        }
        if (z.type == ZombieType.CLOWN) {
            sound(TrainSoundCue.HONK)
            texts.add(FloatingText(z.x, z.y - 90f, "HONK!", 1.0f, 0xFFFF5252.toInt()))
            confetti(z.x, z.y - 40f, 26)
        }
        if (z.type == ZombieType.BALLOON) confetti(z.x, z.y - 90f, 22)
        if (z.type == ZombieType.BLOATER) {
            // A shot bloater pops and hurts the zombies around it.
            explosion(z.x, z.y - 20f, 30f)
            for (other in zombies) if (!other.dead && abs(other.x - z.x) < 100f) damageZombie(other, 35f * z.dmgScale)
        }
    }

    // ---------------------------------------------------------------- gunners

    private fun updateGunners(dt: Float) {
        for (i in 0 until slotCount) {
            val g = gunners[i]
            val weapon = crew[i].weapon
            g.cooldown -= dt
            g.recoil = max(0f, g.recoil - dt * 9f)
            g.flash = max(0f, g.flash - dt)
            val gx = gunX(i)
            var target: Zombie? = null
            var best = weapon.range
            for (z in zombies) {
                if (z.dead || z.x < -10f || z.x > worldW + 10f) continue
                if (weapon.groundOnly && z.flying) continue
                val d = abs(z.x - gx)
                if (d < best) { best = d; target = z }
            }
            if (target == null) {
                g.facing = 1
                g.aim += (-0.15f - g.aim) * min(1f, dt * 5f)
                continue
            }
            val dx = target.x - gx
            g.facing = if (dx >= 0f) 1 else -1
            g.aim = atan2(targetY(target) - shoulderY(i), max(abs(dx), 20f))
            if (g.cooldown <= 0f) {
                g.cooldown = weapon.interval
                fire(i, target)
            }
        }
    }

    private fun targetY(z: Zombie) = z.y - 28f * z.type.size

    // ---------------------------------------------------------------- train-mounted weapons

    private fun updateTrainWeapons(dt: Float) {
        flakFlash = max(0f, flakFlash - dt)
        teslaFlash = max(0f, teslaFlash - dt)
        if (flakLevel > 0) {
            flakCooldown -= dt
            if (flakCooldown <= 0f) {
                var target: Zombie? = null
                var best = 560f
                for (z in zombies) {
                    if (z.dead || !z.flying || z.x < -10f || z.x > worldW + 10f) continue
                    val d = abs(z.x - flakX)
                    if (d < best) { best = d; target = z }
                }
                if (target != null) {
                    flakCooldown = max(0.5f, 1.2f - 0.1f * flakLevel)
                    flakFlash = 0.1f
                    val tx = target.x
                    val ty = targetY(target)
                    val dmg = 40f + 25f * flakLevel
                    tracers.add(Tracer(flakX, flakY - 26f, tx, ty, 0.12f, 3.5f, 0xFFFFD54F.toInt()))
                    explosion(tx, ty, 30f)
                    sound(TrainSoundCue.FLAK)
                    val splash = zombies.filter { it !== target && !it.dead && it.flying && abs(it.x - tx) < 90f }
                    damageZombie(target, dmg)
                    for (z in splash) damageZombie(z, dmg * 0.6f)
                } else {
                    flakCooldown = 0.15f
                }
            }
        }
        if (teslaLevel > 0) {
            teslaCooldown -= dt
            if (teslaCooldown <= 0f) zapChain()
        }
    }

    /** Tesla coil: lightning leaps from the coil to the nearest zombie, then chains to neighbours. */
    private fun zapChain() {
        var from: Zombie? = null
        var best = 520f
        for (z in zombies) {
            if (z.dead || z.x < -10f || z.x > worldW + 10f) continue
            val d = abs(z.x - teslaX)
            if (d < best) { best = d; from = z }
        }
        if (from == null) {
            teslaCooldown = 0.2f
            return
        }
        teslaCooldown = max(1.2f, 3.0f - 0.3f * teslaLevel)
        teslaFlash = 0.25f
        sound(TrainSoundCue.ZAP)
        val jumps = min(8, 2 + teslaLevel)
        val damage = 25f + 18f * teslaLevel
        val hit = ArrayList<Zombie>(jumps)
        var px = teslaX
        var py = teslaY - 34f
        var current: Zombie? = from
        while (current != null && hit.size < jumps) {
            val ty = targetY(current)
            tracers.add(Tracer(px, py, current.x, ty, 0.2f, 2.6f, 0xFF9FE8FF.toInt(), jagged = true))
            hit.add(current)
            px = current.x
            py = ty
            val here = current
            current = zombies
                .filter { !it.dead && it !in hit && abs(it.x - here.x) < 170f && abs(it.y - here.y) < 320f }
                .minByOrNull { abs(it.x - here.x) }
        }
        for (z in hit) {
            spark(z.x, targetY(z))
            damageZombie(z, damage)
        }
    }

    // ---------------------------------------------------------------- repair crew

    private fun updateRepairers(dt: Float) {
        if (repairers.isEmpty()) return
        val healing = hp < maxHp - 0.5f
        val left = trainLeft + 12f
        val right = frontEnd - 36f
        healTextGap -= dt
        for ((i, r) in repairers.withIndex()) {
            if (r.x == 0f) r.x = left + (right - left) * (i + 0.5f) / repairers.size
            r.timer -= dt
            if (r.hammering) {
                r.anim += dt * 16f
                r.effectTimer -= dt
                if (healing && r.effectTimer <= 0f) {
                    r.effectTimer = 0.18f
                    spark(r.x + r.dir * 11f, repairBoardY - 10f)
                }
                if (healing && rng.nextFloat() < dt * 3f) {
                    addParticle(Particle(r.x + (rng.nextFloat() - 0.5f) * 14f, repairBoardY - 28f, 0f, -42f, 1.1f, 1.1f, 5f, 0xFF66FF8A.toInt(), ParticleKind.HEAL))
                }
                if (healing && healTextGap <= 0f) {
                    healTextGap = 1.1f
                    texts.add(FloatingText(r.x, repairBoardY - 44f, "+" + "%.1f".format(regenPerSecond), 0.9f, 0xFF66FF8A.toInt()))
                }
                if (r.timer <= 0f) {
                    r.hammering = false
                    r.timer = 1.2f + rng.nextFloat() * 1.6f
                    if (rng.nextBoolean()) r.dir = -r.dir
                }
            } else {
                r.x += r.dir * 34f * dt
                if (r.x < left) { r.x = left; r.dir = 1 }
                if (r.x > right) { r.x = right; r.dir = -1 }
                r.anim += dt * 9f
                if (r.timer <= 0f) {
                    r.hammering = true
                    r.timer = 1.1f + rng.nextFloat() * 0.9f
                    r.effectTimer = 0f
                }
            }
        }
    }

    private fun fire(slot: Int, target: Zombie) {
        val g = gunners[slot]
        val weapon = crew[slot].weapon
        val damage = crew[slot].damage
        val gx = gunX(slot)
        val len = weaponLength(weapon)
        val mx = gx + g.facing * cos(g.aim) * len
        val my = shoulderY(slot) + sin(g.aim) * len
        val tx = target.x
        val ty = targetY(target)
        g.recoil = 1f
        g.flash = 0.07f

        when (weapon) {
            WeaponType.SHOTGUN -> repeat(3) { n ->
                tracers.add(Tracer(mx, my, tx, ty + (n - 1) * 14f, 0.07f, 1.5f, 0xFFFFE082.toInt()))
            }
            WeaponType.CANNON -> {
                tracers.add(Tracer(mx, my, tx, ty, 0.12f, 5f, 0xFFFF9800.toInt()))
                explosion(tx, ty, 26f)
            }
            WeaponType.RIFLE -> tracers.add(Tracer(mx, my, tx + g.facing * 160f, ty, 0.1f, 2.5f, 0xFFB3E5FC.toInt()))
            WeaponType.ROCKET -> {
                tracers.add(Tracer(mx, my, tx, ty, 0.14f, 4f, 0xFFFFAB91.toInt()))
                explosion(tx, ty, 34f)
            }
            WeaponType.LASER -> tracers.add(Tracer(mx, my, tx + g.facing * 240f, ty, 0.12f, 3.5f, 0xFF40E0FF.toInt()))
            WeaponType.MINIGUN -> tracers.add(Tracer(mx, my, tx, ty, 0.05f, 1.8f, 0xFFFFCC80.toInt()))
            else -> tracers.add(Tracer(mx, my, tx, ty, 0.06f, 1.6f, 0xFFFFF59D.toInt()))
        }
        spark(mx, my)
        sound(
            when (weapon) {
                WeaponType.PISTOL -> TrainSoundCue.PISTOL
                WeaponType.SMG, WeaponType.MINIGUN -> TrainSoundCue.SMG
                WeaponType.SHOTGUN -> TrainSoundCue.SHOTGUN
                WeaponType.RIFLE -> TrainSoundCue.RIFLE
                WeaponType.CANNON -> TrainSoundCue.CANNON
                WeaponType.ROCKET -> TrainSoundCue.ROCKET
                WeaponType.LASER -> TrainSoundCue.LASER
            }
        )

        val struck = ArrayList<Zombie>(4)
        struck.add(target)
        if (weapon.pierce > 1) {
            // Walk further along the line of fire and hit the next zombies on that side.
            zombies.filter { !it.dead && it !== target && !(weapon.groundOnly && it.flying) && (it.x - tx) * g.facing > 0f && abs(it.x - tx) < (if (weapon == WeaponType.LASER) 320f else 220f) }
                .sortedBy { abs(it.x - tx) }
                .take(weapon.pierce - 1)
                .forEach { struck.add(it) }
        }
        val splashTargets = if (weapon.splashRadius > 0f) {
            zombies.filter { !it.dead && it !== target && !(weapon.groundOnly && it.flying) && abs(it.x - tx) <= weapon.splashRadius }
        } else emptyList()

        damageZombie(target, damage)
        for (i in 1 until struck.size) damageZombie(struck[i], damage * 0.8f)
        for (z in splashTargets) damageZombie(z, damage * 0.6f)
    }

    // ---------------------------------------------------------------- effects

    private fun endRun() {
        over = true
        hp = 0f
        shake = 1f
        sound(TrainSoundCue.GAME_OVER)
        var x = trainLeft
        while (x < trainLeft + trainLength) {
            explosion(x, 440f, 40f)
            x += 55f
        }
    }

    private fun emitSmoke(dt: Float) {
        smokeAccumulator += dt
        if (smokeAccumulator < 0.1f) return
        smokeAccumulator = 0f
        addParticle(Particle(locoLeft + 160f, 380f, -50f - rng.nextFloat() * 20f, -45f, 1.8f, 1.8f, 9f, 0xFFB0B0B0.toInt(), ParticleKind.SMOKE))
    }

    private fun updateEffects(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            if (p.life <= 0f) { iterator.remove(); continue }
            p.x += p.vx * dt
            p.y += p.vy * dt
            when (p.kind) {
                ParticleKind.BLOOD, ParticleKind.DEBRIS, ParticleKind.COIN -> p.vy += 520f * dt
                ParticleKind.SMOKE -> { p.vx -= 6f * dt; p.vy -= 4f * dt }
                else -> {}
            }
        }
        for (t in tracers) t.life -= dt
        tracers.removeAll { it.life <= 0f }
        val textIterator = texts.iterator()
        while (textIterator.hasNext()) {
            val t = textIterator.next()
            t.life -= dt
            t.y -= 36f * dt
            if (t.life <= 0f) textIterator.remove()
        }
    }

    private fun addParticle(p: Particle) {
        if (particles.size < MAX_PARTICLES) particles.add(p)
    }

    private fun confetti(x: Float, y: Float, count: Int) {
        val colors = intArrayOf(0xFFFF4081.toInt(), 0xFFFFEB3B.toInt(), 0xFF40C4FF.toInt(), 0xFF69F0AE.toInt(), 0xFFFF9100.toInt())
        repeat(count) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            val s = 70f + rng.nextFloat() * 200f
            addParticle(Particle(x, y, cos(a) * s, sin(a) * s - 120f, 1.1f, 1.1f, 2.5f + rng.nextFloat() * 2f, colors[rng.nextInt(colors.size)], ParticleKind.DEBRIS))
        }
    }

    private fun spark(x: Float, y: Float) {
        repeat(2) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            addParticle(Particle(x, y, cos(a) * 120f, sin(a) * 120f, 0.18f, 0.18f, 2.5f, 0xFFFFE082.toInt(), ParticleKind.SPARK))
        }
    }

    private fun greenBurst(x: Float, y: Float, count: Int) {
        repeat(count) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            val s = 60f + rng.nextFloat() * 180f
            val color = if (rng.nextBoolean()) 0xFF6BA832.toInt() else 0xFF8E2A2A.toInt()
            addParticle(Particle(x, y, cos(a) * s, sin(a) * s - 90f, 0.7f, 0.7f, 2f + rng.nextFloat() * 3f, color, ParticleKind.BLOOD))
        }
    }

    private fun explosion(x: Float, y: Float, radius: Float) {
        addParticle(Particle(x, y, 0f, 0f, 0.35f, 0.35f, radius, 0xFFFF8F00.toInt(), ParticleKind.FIRE))
        repeat(7) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            val s = 80f + rng.nextFloat() * 200f
            addParticle(Particle(x, y, cos(a) * s, sin(a) * s - 60f, 0.6f, 0.6f, 3f + rng.nextFloat() * 3f, 0xFF4E342E.toInt(), ParticleKind.DEBRIS))
        }
        shake = max(shake, 0.25f)
    }

    private fun sound(cue: TrainSoundCue) {
        onSound?.invoke(cue)
    }
}
