package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.data.TrainSoundCue
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.EliteTrait
import com.baraka.game.trainsurvivor.model.HazardKind
import com.baraka.game.trainsurvivor.model.WeaponType
import com.baraka.game.trainsurvivor.model.ZombieType
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * SHARED WORLD, over Bluetooth: how the host's simulation is turned into bytes and back.
 *
 * The host runs the real simulation (SharedRun). Several times a second it writes a snapshot: the world (clock, route,
 * junction, crate), each train (health, guns, cooldowns, hazards), every zombie, and the log of effects since the last
 * snapshot. The guest owns two "mirror" trains (never simulated) and applies each snapshot to them; between snapshots
 * [visualTick] keeps things moving smoothly, so the guest's screen is rendered by exactly the same renderer.
 *
 * Adding game state to the sync: write it in writeRig/writeWorld and read it in the same order in readRig/readWorld.
 * Adding an effect: record it in TrainGame (fx.add) and replay it in [replayFx]. Adding a hazard: implement
 * Hazard.writeState/readState.
 */

// ------------------------------------------------------------------ world

fun TrainGame.writeWorld(out: DataOutputStream) {
    out.writeFloat(time)
    out.writeFloat(scroll)
    out.writeFloat(worldW)
    out.writeByte(biome.ordinal)
    out.writeByte(fromBiome.ordinal)
    out.writeByte(nextBiome.ordinal)
    out.writeBoolean(transitioning)
    out.writeFloat(boundaryX)
    out.writeFloat(biomeBanner)
    out.writeByte(kitFrom.ordinal)
    out.writeFloat(kitBlend)
    out.writeFloat(hordeTime)
    out.writeFloat(hordeWarning)
    out.writeByte(hordeSide + 1)
    val c = crate
    out.writeBoolean(c != null)
    if (c != null) {
        out.writeFloat(c.x); out.writeFloat(c.y); out.writeFloat(c.phase); out.writeBoolean(c.landed); out.writeFloat(c.life)
    }
    val j = junction
    out.writeBoolean(j != null)
    if (j != null) {
        out.writeByte(j.optionA.ordinal); out.writeByte(j.optionB.ordinal); out.writeFloat(j.timeLeft)
        out.writeByte((j.chosen?.ordinal ?: -1) + 1)
    }
}

fun TrainGame.readWorld(input: DataInputStream) {
    time = input.readFloat()
    scroll = input.readFloat()
    worldW = input.readFloat()
    biome = Biome.entries[input.readUnsignedByte().coerceAtMost(Biome.entries.lastIndex)]
    fromBiome = Biome.entries[input.readUnsignedByte().coerceAtMost(Biome.entries.lastIndex)]
    nextBiome = Biome.entries[input.readUnsignedByte().coerceAtMost(Biome.entries.lastIndex)]
    transitioning = input.readBoolean()
    boundaryX = input.readFloat()
    biomeBanner = input.readFloat()
    kitFrom = Biome.entries[input.readUnsignedByte().coerceAtMost(Biome.entries.lastIndex)]
    kitBlend = input.readFloat()
    hordeTime = input.readFloat()
    hordeWarning = input.readFloat()
    hordeSide = input.readUnsignedByte() - 1
    crate = if (input.readBoolean()) {
        val c = Crate(input.readFloat(), input.readFloat())
        c.phase = input.readFloat(); c.landed = input.readBoolean(); c.life = input.readFloat()
        c
    } else null
    if (input.readBoolean()) {
        val a = Biome.entries[input.readUnsignedByte().coerceAtMost(Biome.entries.lastIndex)]
        val b = Biome.entries[input.readUnsignedByte().coerceAtMost(Biome.entries.lastIndex)]
        val timeLeft = input.readFloat()
        val chosen = input.readUnsignedByte() - 1
        val existing = junction
        val j = if (existing != null && existing.optionA == a && existing.optionB == b) existing else Junction(a, b, timeLeft)
        j.timeLeft = timeLeft
        j.chosen = if (chosen >= 0) Biome.entries[chosen.coerceAtMost(Biome.entries.lastIndex)] else null
        junction = j
    } else {
        junction = null
    }
}

// ------------------------------------------------------------------ one train

fun TrainGame.writeRig(out: DataOutputStream) {
    out.writeByte(lane)
    out.writeFloat(switchWarning)
    out.writeBoolean(over)
    out.writeFloat(hp)
    for (i in 0 until slotCount) out.writeFloat(carHp[i])
    out.writeFloat(distance)
    out.writeInt(kills); out.writeInt(flyerKills); out.writeInt(bossKills); out.writeInt(routesTaken); out.writeInt(runCoins)
    out.writeFloat(shake); out.writeFloat(hitFlash); out.writeFloat(bossAlert)
    out.writeUTF(bossName)
    out.writeInt(boss?.netId ?: 0)
    out.writeFloat(buffTime)
    out.writeFloat(dynamiteCooldown); out.writeFloat(overdriveTime); out.writeFloat(overdriveCooldown); out.writeFloat(fieldRepairCooldown)
    out.writeFloat(towCooldown); out.writeFloat(repairSendCooldown); out.writeFloat(coinGiftCooldown); out.writeFloat(outgoingLoanCooldown)
    for (i in 0 until slotCount) out.writeFloat(seatAwayTime[i])
    out.writeFloat(guestTime)
    out.writeByte(guestSurvivor)
    out.writeByte(loanSeat + 1)
    out.writeByte((lockedBy?.ordinal ?: -1) + 1)
    out.writeFloat(lockTime)
    out.writeByte((partnerHelp?.ordinal ?: -1) + 1)
    out.writeFloat(partnerHelpTime)
    out.writeFloat(rescueTime); out.writeFloat(rescueProgress); out.writeFloat(partnerRescueTime)
    out.writeInt(rescues)
    out.writeInt(comboCount)
    out.writeFloat(flakFlash); out.writeFloat(teslaFlash); out.writeFloat(flameFlash)
    for (i in 0..slotCount) {
        val g = gunners[i]
        out.writeBoolean(g.alive)
        out.writeBoolean(g.facing > 0)
        out.writeFloat(g.aim)
        out.writeByte(g.weapon.ordinal)
        out.writeByte(g.mode + 1)
        out.writeBoolean(g.flash > 0f)
        out.writeFloat(g.recoil)
        out.writeBoolean(g.overheated[g.weapon.ordinal])
    }
    out.writeByte(hazards.size)
    for (h in hazards) {
        out.writeByte(h.kind.ordinal)
        h.writeState(out)
    }
}

fun TrainGame.readRig(input: DataInputStream, zombieIndex: Map<Int, Zombie>) {
    lane = input.readUnsignedByte()
    fxLane = lane
    switchWarning = input.readFloat()
    over = input.readBoolean()
    hp = input.readFloat()
    for (i in 0 until slotCount) carHp[i] = input.readFloat()
    distance = input.readFloat()
    kills = input.readInt(); flyerKills = input.readInt(); bossKills = input.readInt(); routesTaken = input.readInt(); runCoins = input.readInt()
    shake = max(shake, input.readFloat()); hitFlash = max(hitFlash, input.readFloat()); bossAlert = input.readFloat()
    bossName = input.readUTF()
    boss = zombieIndex[input.readInt()]
    buffTime = input.readFloat()
    dynamiteCooldown = input.readFloat(); overdriveTime = input.readFloat(); overdriveCooldown = input.readFloat(); fieldRepairCooldown = input.readFloat()
    towCooldown = input.readFloat(); repairSendCooldown = input.readFloat(); coinGiftCooldown = input.readFloat(); outgoingLoanCooldown = input.readFloat()
    for (i in 0 until slotCount) seatAwayTime[i] = input.readFloat()
    guestTime = input.readFloat()
    guestSurvivor = input.readUnsignedByte()
    loanSeat = input.readUnsignedByte() - 1
    val lock = input.readUnsignedByte() - 1
    lockedBy = if (lock >= 0) HazardKind.entries.getOrNull(lock) else null
    lockTime = input.readFloat()
    val help = input.readUnsignedByte() - 1
    partnerHelp = if (help >= 0) HazardKind.entries.getOrNull(help) else null
    partnerHelpTime = input.readFloat()
    rescueTime = input.readFloat(); rescueProgress = input.readFloat(); partnerRescueTime = input.readFloat()
    rescues = input.readInt()
    comboCount = input.readInt()
    flakFlash = input.readFloat(); teslaFlash = input.readFloat(); flameFlash = input.readFloat()
    for (i in 0..slotCount) {
        val g = gunners[i]
        g.alive = input.readBoolean()
        g.facing = if (input.readBoolean()) 1 else -1
        g.aim = input.readFloat()
        g.weapon = WeaponType.entries[input.readUnsignedByte().coerceAtMost(WeaponType.entries.lastIndex)]
        g.mode = input.readUnsignedByte() - 1
        if (input.readBoolean()) g.flash = 0.07f
        g.recoil = max(g.recoil, input.readFloat())
        g.overheated[g.weapon.ordinal] = input.readBoolean()
    }
    // Hazards: keep our objects while the kinds match, otherwise rebuild the list.
    val count = input.readUnsignedByte()
    var same = hazards.size == count
    // Read each hazard into the matching object (creating it if needed): the state follows the kind byte.
    val rebuilt = ArrayList<Hazard>(count)
    for (n in 0 until count) {
        val kind = HazardKind.entries[input.readUnsignedByte().coerceAtMost(HazardKind.entries.lastIndex)]
        var h: Hazard? = null
        if (same && hazards[n].kind == kind) h = hazards[n]
        if (h == null) { h = HazardRegistry.create(kind); same = false }
        h.readState(input)
        rebuilt.add(h)
    }
    if (!same) {
        hazards.clear()
        hazards.addAll(rebuilt)
    }
}

// ------------------------------------------------------------------ zombies

/** Writes every living zombie (both lanes). The host gives each one a network id the first time it is sent. */
fun TrainGame.writeZombies(out: DataOutputStream) {
    var n = 0
    for (i in zombies.indices) if (!zombies[i].dead) n++
    out.writeShort(n)
    for (i in zombies.indices) {
        val z = zombies[i]
        if (z.dead) continue
        if (z.netId == 0) z.netId = nextNetId()
        out.writeInt(z.netId)
        out.writeByte(z.type.ordinal)
        val elite = (z.elite?.ordinal ?: -1) + 1
        out.writeByte(
            z.lane or (if (z.attacking) 2 else 0) or (if (z.facing > 0) 4 else 0) or (elite shl 3) or (if (z.flash > 0.3f) 64 else 0) or (if (z.shield > 0f) 128 else 0)
        )
        out.writeByte(z.jumpState)
        out.writeShort(z.x.toInt())
        out.writeShort(z.y.toInt())
        out.writeByte((z.hp / z.maxHp * 100f).toInt().coerceIn(1, 100))
    }
}

/** Applies a zombie list to the mirror: creates, moves (eased in [visualTick]) and removes zombies so the list matches the host's. */
fun TrainGame.readZombies(input: DataInputStream, index: HashMap<Int, Zombie>, stamp: Int) {
    val n = input.readUnsignedShort()
    for (k in 0 until n) {
        val id = input.readInt()
        val type = ZombieType.entries[input.readUnsignedByte().coerceAtMost(ZombieType.entries.lastIndex)]
        val flags = input.readUnsignedByte()
        val jump = input.readUnsignedByte()
        val x = input.readShort().toFloat()
        val y = input.readShort().toFloat()
        val hpPct = input.readUnsignedByte()
        var z = index[id]
        if (z == null) {
            z = Zombie(type, x, y, if (flags and 4 != 0) -1 else 1, 100f, 1f, (id % 97) / 97f)
            z.netId = id
            index[id] = z
            zombies.add(z)
        }
        z.netX = x
        z.netY = y
        if (z.jumpState != 0 || jump != 0) { z.x = x; z.y = y } // leaps and drops are fast: don't ease them
        z.lane = flags and 1
        z.attacking = flags and 2 != 0
        z.facing = if (flags and 4 != 0) 1 else -1
        val elite = (flags shr 3) and 7
        z.elite = if (elite == 0) null else EliteTrait.entries.getOrNull(elite - 1)
        if (flags and 64 != 0) z.flash = 1f
        z.shield = if (flags and 128 != 0) 1f else 0f
        z.jumpState = jump
        z.hp = hpPct.toFloat()
        z.netStamp = stamp
    }
    var i = zombies.size - 1
    while (i >= 0) {
        val z = zombies[i]
        if (z.netStamp != stamp) {
            index.remove(z.netId)
            zombies.removeAt(i)
        }
        i--
    }
}

// ------------------------------------------------------------------ effects

fun TrainGame.writeFx(out: DataOutputStream) {
    val log = fx
    out.writeShort(log.count)
    for (i in 0 until log.count) {
        out.writeByte(log.type[i])
        out.writeByte(log.lane[i])
        out.writeShort(log.x[i].toInt())
        out.writeShort(log.y[i].toInt())
        when (log.type[i]) {
            FxLog.TRACER -> {
                out.writeShort(log.x2[i].toInt()); out.writeShort(log.y2[i].toInt())
                out.writeByte((log.w[i] * 10f).toInt().coerceIn(0, 255))
                out.writeInt(log.color[i])
                out.writeShort(log.n[i])
            }
            FxLog.EXPLOSION -> out.writeByte(log.w[i].toInt().coerceIn(0, 255))
            FxLog.GREEN_BURST, FxLog.CONFETTI, FxLog.COINS, FxLog.SOUND -> out.writeByte(log.n[i].coerceIn(0, 255))
            FxLog.TEXT -> {
                out.writeByte((log.x2[i] * 20f).toInt().coerceIn(1, 255))
                out.writeByte(log.w[i].toInt().coerceIn(1, 255))
                out.writeInt(log.color[i])
                out.writeUTF(log.text[i] ?: "")
            }
        }
    }
    log.clear()
}

/** Replays a partner's effects on this mirror: tracers, bursts, floating texts and sounds. */
fun TrainGame.replayFx(input: DataInputStream) {
    val n = input.readUnsignedShort()
    val previous = fxLane
    for (k in 0 until n) {
        val type = input.readUnsignedByte()
        fxLane = input.readUnsignedByte()
        val x = input.readShort().toFloat()
        val y = input.readShort().toFloat()
        when (type) {
            FxLog.TRACER -> {
                val x2 = input.readShort().toFloat(); val y2 = input.readShort().toFloat()
                val width = input.readUnsignedByte() / 10f
                val color = input.readInt()
                val packed = input.readShort().toInt()
                addTracer(Tracer(x, y, x2, y2, (packed shr 1) / 100f, width, color, jagged = packed and 1 != 0))
            }
            FxLog.EXPLOSION -> explosion(x, y, input.readUnsignedByte().toFloat())
            FxLog.SPARK -> spark(x, y)
            FxLog.GREEN_BURST -> greenBurst(x, y, input.readUnsignedByte())
            FxLog.CONFETTI -> confetti(x, y, input.readUnsignedByte())
            FxLog.COINS -> {
                repeat(input.readUnsignedByte()) {
                    addParticle(Particle(x, y, (rng.nextFloat() - 0.5f) * 160f, -150f - rng.nextFloat() * 120f, 0.9f, 0.9f, 5f, 0xFFFFC107.toInt(), ParticleKind.COIN))
                }
            }
            FxLog.TEXT -> {
                val life = input.readUnsignedByte() / 20f
                val size = input.readUnsignedByte().toFloat()
                val color = input.readInt()
                val text = input.readUTF()
                if (texts.size < MAX_TEXTS) addText(FloatingText(x, y, text, life, color, size))
            }
            FxLog.SOUND -> {
                val cue = TrainSoundCue.entries.getOrNull(input.readUnsignedByte())
                if (cue != null) onSound?.invoke(cue)
            }
        }
    }
    fxLane = previous
}

// ------------------------------------------------------------------ keeping the mirror alive between snapshots

/**
 * One frame of "just animation" for a mirror train: the clock and scenery keep moving, effects age, guns recoil, the repair
 * crew walks, and zombies ease toward where the last snapshot said they are. Nothing here changes game state.
 */
fun TrainGame.visualTick(dt: Float) {
    val leader = lead
    if (leader != null) syncWorld(leader) else {
        time += dt
        scroll += WORLD_SPEED * speedFactor * dt
    }
    wheelAngle += WORLD_SPEED * speedFactor * dt / 17f
    shake = max(0f, shake - dt * 2.2f)
    hitFlash = max(0f, hitFlash - dt * 3f)
    bossAlert = max(0f, bossAlert - dt)
    biomeBanner = max(0f, biomeBanner - dt)
    hordeWarning = max(0f, hordeWarning - dt)
    kitBlend = min(1f, kitBlend + dt / 1.4f)
    flakFlash = max(0f, flakFlash - dt)
    teslaFlash = max(0f, teslaFlash - dt)
    flameFlash = max(0f, flameFlash - dt)
    dynamiteCooldown = max(0f, dynamiteCooldown - dt)
    overdriveCooldown = max(0f, overdriveCooldown - dt)
    overdriveTime = max(0f, overdriveTime - dt)
    fieldRepairCooldown = max(0f, fieldRepairCooldown - dt)
    towCooldown = max(0f, towCooldown - dt)
    repairSendCooldown = max(0f, repairSendCooldown - dt)
    coinGiftCooldown = max(0f, coinGiftCooldown - dt)
    outgoingLoanCooldown = max(0f, outgoingLoanCooldown - dt)
    for (i in gunners.indices) {
        val g = gunners[i]
        g.recoil = max(0f, g.recoil - dt * 9f)
        g.flash = max(0f, g.flash - dt)
    }
    if (!over) {
        updateRepairers(dt)
        emitSmoke(dt)
    }
    if (leader == null) {
        // The zombie list is shared by both mirrors: ease it once.
        val k = min(1f, dt * 14f)
        for (i in zombies.indices) {
            val z = zombies[i]
            if (z.jumpState == 0) {
                z.x += (z.netX - z.x) * k
                z.y += (z.netY - z.y) * k
            }
            if (!z.attacking) z.anim += dt * (z.type.speed / 14f + 2f) else z.anim += dt * 6f
            z.flash = max(0f, z.flash - dt * 6f)
        }
        updateEffects(dt)
        rebuildDrawOrder()
    }
}
