package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.data.TrainSoundCue
import com.baraka.game.trainsurvivor.model.DamageKind
import com.baraka.game.trainsurvivor.model.Grade
import com.baraka.game.trainsurvivor.model.Hitchhiker
import com.baraka.game.trainsurvivor.model.Impact
import com.baraka.game.trainsurvivor.model.ShotStyle
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Hitchhikers: a funny stranger waves from the trackside every minute or two. Tap them to give them a ride on the roof
 * for 30-60 seconds; they shoot with their own silly weapon (see model/Hitchhikers.kt), chat, and tip when they hop off.
 * Works solo and in both co-op modes. The world owns the waiting [Hitcher] (like the supply crate); each train owns its
 * own [Passenger]. Everything here is an extension on [TrainGame] so the main file only needs a few hook lines.
 */
class Hitcher(val who: Hitchhiker, val grade: Grade, val x: Float, val side: Int) {
    var life = 14f
    var phase = 0f
}

class Strike(val target: Zombie, val tx: Float, val ty: Float, var t: Float)

class Passenger(val who: Hitchhiker, val grade: Grade, var left: Float) {
    var total = left
    var state = BOARDING
    var animT = 0f
    var fromX = 0f
    var fromY = 0f
    var cooldown = 1.2f
    var aim = -0.15f
    var facing = 1
    var recoil = 0f
    var flash = 0f
    var speech: String? = null
    var speechT = 0f
    var chatter = 8f
    var hurtGap = 0f
    var kills = 0
    internal val struck = ArrayList<Zombie>(6)
    /** Hits still in the air (the pigeons): they land after a moment and then do their damage. */
    internal val strikes = ArrayList<Strike>(4)

    fun say(lines: Array<String>, rng: Random, seconds: Float = 2.8f) {
        speech = lines[rng.nextInt(lines.size)]
        speechT = seconds
    }

    companion object {
        const val BOARDING = 0
        const val RIDING = 1
        const val LEAVING = 2
        const val BOARD_TIME = 0.9f
        const val LEAVE_TIME = 1.3f
    }
}

private const val BASE_STAY = 40f
private const val CHAIN_REACH = 170f
private const val PIERCE_REACH = 240f

// ---------------------------------------------------------------- where they ride

/** The roof seat: on the first wagon behind the locomotive (or the loco roof on a one-car train). */
fun TrainGame.perchX(): Float = if (slotCount > 1) carLeft(1) + 26f else locoLeft + 24f
fun TrainGame.perchY(): Float = if (slotCount > 1) gunY(1) else gunY(0)

// ---------------------------------------------------------------- aggregate effects the main loop reads

/** Share of damage to this train that the passenger soaks up (the mime's invisible wall). */
fun TrainGame.hitchShield(): Float {
    val p = passenger ?: return 0f
    if (p.state != Passenger.RIDING || p.who.shield <= 0f) return 0f
    return (p.who.shield + 0.1f * p.grade.ordinal).coerceAtMost(0.6f)
}

/** Fire-rate bonus for every gunner (the general barking orders). */
fun TrainGame.hitchRally(): Float {
    val p = passenger ?: return 0f
    if (p.state != Passenger.RIDING || p.who.rally <= 0f) return 0f
    return p.who.rally * (0.8f + 0.2f * p.grade.ordinal)
}

internal fun TrainGame.hitchOnHurt(amount: Float) {
    val p = passenger ?: return
    if (p.state != Passenger.RIDING || amount < 4f || p.hurtGap > 0f) return
    p.hurtGap = 8f
    if (p.speech == null) p.say(p.who.lines.hurt, rng)
}

// ---------------------------------------------------------------- the waiting hitchhiker

internal fun TrainGame.rollGrade(): Grade {
    debugGrade?.let { return it }
    val bonusLegend = (time / 120f).toInt().coerceAtMost(6)
    var total = 0
    for (g in Grade.entries) total += g.weight + (if (g == Grade.LEGENDARY) bonusLegend else 0)
    var roll = rng.nextInt(total)
    for (g in Grade.entries) {
        roll -= g.weight + (if (g == Grade.LEGENDARY) bonusLegend else 0)
        if (roll < 0) return g
    }
    return Grade.DECENT
}

/** World-owner only: a hitchhiker turns up now and then, never during a boss or while someone is already riding. */
internal fun TrainGame.updateHitchEvents(dt: Float) {
    val h = hitcher
    if (h != null) {
        h.phase += dt
        h.life -= dt
        if (h.life <= 0f) hitcher = null
        return
    }
    if (over || time < nextHitchAt) return
    if (passenger != null || ally?.passenger != null || boss != null || ally?.boss != null) {
        nextHitchAt = time + 6f
        return
    }
    callHitcher(Hitchhiker.entries[rng.nextInt(Hitchhiker.entries.size)])
}

internal fun TrainGame.callHitcher(who: Hitchhiker) {
    val side = if (rng.nextBoolean()) 1 else -1
    val x = if (side > 0) worldW - 130f else 130f
    hitcher = Hitcher(who, rollGrade(), x, side)
    nextHitchAt = time + 80f + rng.nextFloat() * 45f
    sound(TrainSoundCue.CRATE)
    floatText(x, 360f, "HITCHHIKER!", 1.6f, 0xFFFFD54F.toInt(), 22f)
}

/** A tap on the waiting hitchhiker invites them aboard. Returns true if the tap was theirs. */
internal fun TrainGame.tapHitcher(lx: Float, ly: Float): Boolean {
    val world = lead ?: this
    val h = world.hitcher ?: return false
    val ground = RAIL_Y + 30f
    val nearLower = abs(ly - (ground - 50f)) < 90f
    val nearUpper = abs(ly - (ground - 50f - LANE_DY)) < 90f
    if (over || abs(lx - h.x) > 60f || !(nearLower || nearUpper)) return false
    if (passenger != null) {
        floatText(perchX(), perchY() - 90f, "Seat taken!", 1.2f, 0xFFFF8A65.toInt(), 18f)
        return true
    }
    world.hitcher = null
    hitcher = null
    boardHitchhiker(h.who, h.grade, h.x, ground)
    return true
}

internal fun TrainGame.boardHitchhiker(who: Hitchhiker, grade: Grade, fromX: Float, fromY: Float) {
    val p = Passenger(who, grade, BASE_STAY * grade.stay)
    p.fromX = fromX
    p.fromY = fromY
    p.say(who.lines.board, rng)
    passenger = p
    sound(TrainSoundCue.CRATE)
    confetti(perchX(), perchY() - 20f, 10)
    floatText(perchX(), perchY() - 96f, who.weaponName(grade), 1.8f, grade.color.toInt(), 16f)
}

// ---------------------------------------------------------------- the passenger on the roof

internal fun TrainGame.updateHitchhiker(dt: Float) {
    tickStatus(dt)
    val p = passenger ?: return
    p.recoil = max(0f, p.recoil - dt * 8f)
    p.flash = max(0f, p.flash - dt)
    p.hurtGap -= dt
    if (p.speechT > 0f) {
        p.speechT -= dt
        if (p.speechT <= 0f) p.speech = null
    }
    when (p.state) {
        Passenger.BOARDING -> {
            p.animT += dt
            if (p.animT >= Passenger.BOARD_TIME) {
                p.state = Passenger.RIDING
                p.animT = 0f
            }
        }
        Passenger.RIDING -> ride(p, dt)
        else -> {
            p.animT += dt
            if (p.animT >= Passenger.LEAVE_TIME) passenger = null
        }
    }
}

/** Slow and burn effects tick on zombies even after the passenger who caused them has left. */
private fun TrainGame.tickStatus(dt: Float) {
    tickPuddles(dt)
    for (k in zombies.indices) {
        val z = zombies[k]
        if (z.dead || z.lane != lane) continue
        if (z.slowT > 0f) z.slowT -= dt
        if (z.vulnT > 0f) {
            z.vulnT -= dt
            if (z.vulnT <= 0f) z.vulnMul = 0f
        }
        if (z.burnT > 0f) {
            z.burnT -= dt
            shooter = -1
            strikeKind = DamageKind.FIRE
            damageZombie(z, z.burnDps * dt, silent = true)
            strikeKind = DamageKind.PHYSICAL
            if (!z.dead && rng.nextFloat() < dt * 6f) {
                addParticle(Particle(z.x + (rng.nextFloat() - 0.5f) * 14f, z.y - 20f * z.type.size, 0f, -40f, 0.4f, 0.4f, 3f, 0xFFFF7043.toInt(), ParticleKind.FIRE))
            }
        }
    }
}

private fun TrainGame.ride(p: Passenger, dt: Float) {
    val who = p.who
    p.left -= dt
    if (p.left <= 0f) {
        leave(p)
        return
    }
    p.chatter -= dt
    if (p.chatter <= 0f) {
        p.chatter = 14f + rng.nextFloat() * 10f
        if (p.speech == null) p.say(who.lines.idle, rng)
    }
    p.cooldown -= dt * (if (overdriveTime > 0f) 1.6f else 1f)
    // Birds that were sent out land now and strike (if the target died on the way, they just flutter off).
    var si = p.strikes.size - 1
    while (si >= 0) {
        val s = p.strikes[si]
        s.t -= dt
        if (s.t <= 0f) {
            p.strikes.removeAt(si)
            val z = s.target
            if (z.dead) impactFx(who, s.tx, s.ty) else strike(p, z, z.x, z.y - z.type.aimHeight * z.type.size)
        }
        si--
    }

    val px = perchX()
    val shoulder = perchY() - 30f * who.look.height
    var target: Zombie? = null
    var best = who.range
    for (k in zombies.indices) {
        val z = zombies[k]
        if (z.dead || z.lane != lane || z.x < -10f || z.x > worldW + 10f) continue
        if (who.groundOnly && z.flying) continue
        if (who.damage <= 0f && who.slow < 1f && z.slowT > 0.8f) continue // the mime doesn't re-freeze what is already stuck
        var d = reachFrom(px, z)
        if (who.prefersFlyers && z.flying) d -= 200f
        if (d < best) {
            best = d
            target = z
        }
    }
    if (target == null) {
        p.facing = 1
        p.aim += (-0.15f - p.aim) * min(1f, dt * 5f)
        return
    }
    val tx = target.x
    val ty = target.y - target.type.aimHeight * target.type.size
    val dx = tx - px
    p.facing = if (dx >= 0f) 1 else -1
    p.aim = atan2(ty - shoulder, max(abs(dx), 20f))
    if (p.cooldown <= 0f) {
        p.cooldown = who.interval * p.grade.rate
        shoot(p, target, tx, ty, px, shoulder)
    }
}

/** Range is measured from the hull edge zombies mass at, like the regular gunners'. */
private fun TrainGame.reachFrom(px: Float, z: Zombie): Float {
    val right = frontEnd
    val left = rearEnd
    return when {
        z.x > right -> (z.x - right) + 0.4f * max(0f, right - px)
        z.x < left -> (left - z.x) + 0.4f * max(0f, px - left)
        else -> abs(z.x - px)
    }
}

private fun TrainGame.leave(p: Passenger) {
    p.state = Passenger.LEAVING
    p.animT = 0f
    p.say(p.who.lines.leave, rng, 2.4f)
    val tip = ((50f + time * 0.5f) * p.grade.tip * coinMultiplier).roundToInt()
    runCoins += tip
    floatText(perchX(), perchY() - 110f, "+$tip tip", 1.6f, 0xFFFFD54F.toInt(), 22f)
    sound(TrainSoundCue.COIN)
}

// ---------------------------------------------------------------- shooting

private fun TrainGame.shoot(p: Passenger, target: Zombie, tx: Float, ty: Float, px: Float, shoulder: Float) {
    val who = p.who
    val grade = p.grade
    val mx = px + p.facing * cos(p.aim) * 34f
    val my = shoulder + sin(p.aim) * 34f
    p.recoil = 1f
    p.flash = 0.08f
    sound(who.cue)
    val color = who.tracerColor.toInt()
    if (rng.nextFloat() < who.misfire) {
        addTracer(Tracer(mx, my, tx, ty - 10f, ShotStyle.BOOT.life, who.tracerWidth, color).also { it.style = ShotStyle.BOOT })
        floatText(tx, ty - 30f, "BOOT!", 1.0f, 0xFFBCAAA4.toInt(), 22f)
        p.speech = "I caught a boot!"
        p.speechT = 2f
        return
    }
    if (who.shot.flight > 0f) { // the weapon's own projectile (pigeon, pan, harpoon, soup...) flies out and strikes when it lands
        addTracer(Tracer(mx, my, tx, ty, who.shot.life, who.tracerWidth, color).also { it.style = who.shot })
        launchFx(who, mx, my)
        p.strikes.add(Strike(target, tx, ty, who.shot.flight))
        return
    }
    addTracer(Tracer(mx, my, tx, ty, 0.1f, who.tracerWidth, color))
    spark(mx, my)
    strike(p, target, tx, ty)
}

/** The hit itself: the weapon's effect where it lands, its damage and side effects, and its bounces and piercing. */
private fun TrainGame.strike(p: Passenger, target: Zombie, tx: Float, ty: Float) {
    val who = p.who
    val grade = p.grade
    val color = who.tracerColor.toInt()
    impactFx(who, tx, ty)
    if (who.puddle > 0f) spill(who, grade, tx)

    val damage = who.damage * grade.damage * (if (buffTime > 0f) 1.3f else 1f) * (if (target.flying) who.flyerBonus else 1f)
    shooter = -1
    strikeKind = who.kind
    p.struck.clear()
    p.struck.add(target)
    touch(who, grade, target, damage)

    if (who.splash > 0f) {
        val reach = who.splash * (1f + 0.15f * grade.bonus)
        for (k in zombies.indices) {
            val z = zombies[k]
            if (z.dead || z === target || z.lane != lane) continue
            if (who.groundOnly && z.flying) continue
            if (abs(z.x - tx) <= reach) touch(who, grade, z, damage * 0.6f)
        }
    }
    if (who.bounces > 0) {
        // the shot ricochets from zombie to zombie
        var from = target
        repeat(who.bounces + grade.bonus) {
            var next: Zombie? = null
            var nextDist = CHAIN_REACH
            for (k in zombies.indices) {
                val z = zombies[k]
                if (z.dead || z.lane != lane || p.struck.contains(z)) continue
                if (who.groundOnly && z.flying) continue
                val d = abs(z.x - from.x) + abs(z.y - from.y) * 0.6f
                if (d < nextDist) {
                    nextDist = d
                    next = z
                }
            }
            if (next != null) {
                addTracer(Tracer(from.x, from.y - from.type.aimHeight * from.type.size, next.x, next.y - next.type.aimHeight * next.type.size, if (who.shot.flight > 0f) who.shot.hop else 0.1f, max(1.5f, who.tracerWidth * 0.6f), color).also { it.style = who.shot })
                impactFx(who, next.x, next.y - next.type.aimHeight * next.type.size)
                p.struck.add(next)
                touch(who, grade, next, damage * 0.7f)
                from = next
            }
        }
    }
    if (who.pierce > 0) {
        // the harpoon keeps going through whatever stands behind
        repeat(who.pierce + grade.bonus) {
            var next: Zombie? = null
            var nextDist = PIERCE_REACH
            for (k in zombies.indices) {
                val z = zombies[k]
                if (z.dead || z.lane != lane || p.struck.contains(z)) continue
                if (who.groundOnly && z.flying) continue
                val d = (z.x - tx) * p.facing
                if (d > 0f && d < nextDist) {
                    nextDist = d
                    next = z
                }
            }
            if (next != null) {
                p.struck.add(next)
                touch(who, grade, next, damage * 0.8f)
            }
        }
    }
    shooter = -1
    strikeKind = DamageKind.PHYSICAL

    if (target.dead) {
        p.kills++
        if (p.speech == null && rng.nextFloat() < 0.4f) p.say(who.lines.kill, rng, 1.8f)
    }
}

/** Damage plus the weapon's side effects (slow, burn, knock-back) on one zombie. */
private fun TrainGame.touch(who: Hitchhiker, grade: Grade, z: Zombie, damage: Float) {
    if (damage > 0f) damageZombie(z, damage)
    if (z.dead) return
    if (who.slow < 1f) {
        z.slowT = who.slowTime * (1f + 0.25f * grade.bonus)
        z.slowMul = if (z.isBoss) 1f - (1f - who.slow) * 0.4f else who.slow
    }
    if (who.burn > 0f) {
        z.burnT = 3f
        z.burnDps = who.burn * grade.damage
    }
    if (who.knock > 0f && z.jumpState == 0 && !z.type.stationary && !z.isBoss) {
        z.x += z.side * who.knock * (1f + 0.4f * grade.bonus)
    }
}

/** What happens at the passenger as the shot leaves: feathers from the cage, a popper burst, leaves, steam, a megaphone's shout... */
private fun TrainGame.launchFx(who: Hitchhiker, mx: Float, my: Float) {
    when (who.shot) {
        ShotStyle.BIRD -> repeat(4) {
            val a = rng.nextFloat() * 6.2832f
            addParticle(Particle(mx, my, cos(a) * 50f, sin(a) * 50f - 40f, 0.6f, 0.6f, 2.4f, 0xFFFFFFFF.toInt(), ParticleKind.DEBRIS))
        }
        ShotStyle.CONFETTI -> confetti(mx, my, 10)
        ShotStyle.GUST -> repeat(5) {
            addParticle(Particle(mx, my + (it - 2) * 5f, 140f + rng.nextFloat() * 90f, (rng.nextFloat() - 0.5f) * 40f, 0.5f, 0.5f, 3f, if (it % 2 == 0) 0xFFFF8F00.toInt() else 0xFF7CB342.toInt(), ParticleKind.DEBRIS))
        }
        ShotStyle.SOUP -> repeat(3) { addParticle(Particle(mx, my, 20f * (it - 1), -40f, 0.7f, 0.7f, 6f, 0xFFE0E0E0.toInt(), ParticleKind.SMOKE)) }
        ShotStyle.SHELL -> {
            spark(mx, my)
            addParticle(Particle(mx, my, 0f, -30f, 0.8f, 0.8f, 9f, 0xFFB0B0B0.toInt(), ParticleKind.SMOKE))
        }
        else -> spark(mx, my)
    }
}

private fun TrainGame.impactFx(who: Hitchhiker, x: Float, y: Float) {
    when (who.impact) {
        Impact.SPARK -> spark(x, y)
        Impact.CONFETTI -> confetti(x, y, 16)
        Impact.EXPLOSION -> explosion(x, y + 20f, max(24f, who.splash * 0.4f))
        Impact.FEATHERS -> repeat(6) {
            val a = rng.nextFloat() * 6.2832f
            addParticle(Particle(x, y, cos(a) * 70f, sin(a) * 70f - 50f, 0.8f, 0.8f, 2.6f, 0xFFFFFFFF.toInt(), ParticleKind.DEBRIS))
        }
        Impact.SOUP -> {
            addParticle(Particle(x, y, 0f, 0f, 0.3f, 0.3f, 22f, 0xFFFF7043.toInt(), ParticleKind.FIRE))
            repeat(6) {
                val a = rng.nextFloat() * 6.2832f
                addParticle(Particle(x, y, cos(a) * 90f, sin(a) * 90f - 60f, 0.6f, 0.6f, 3f, 0xFFFFB74D.toInt(), ParticleKind.BLOOD))
            }
        }
        Impact.AIR -> repeat(5) {
            addParticle(Particle(x, y + (it - 2) * 8f, 120f + rng.nextFloat() * 80f, 0f, 0.35f, 0.35f, 3f, 0xFFE1F5FE.toInt(), ParticleKind.SPARK))
        }
        Impact.BOX -> {
            val s = 22f
            for (c in 0 until 4) {
                val cx = x + (if (c and 1 == 0) -s else s)
                val cy = y + (if (c and 2 == 0) -s else s)
                addParticle(Particle(cx, cy, 0f, 0f, 0.6f, 0.6f, 4f, 0xFF90CAF9.toInt(), ParticleKind.SPARK))
            }
        }
    }
}

// ---------------------------------------------------------------- mirror train and debug

/** Animation-only tick for the partner's mirror: the passenger's timers keep running between snapshots. */
fun TrainGame.hitchVisualTick(dt: Float) {
    if (lead == null) hitcher?.let { it.phase += dt }
    var pi = puddles.size - 1
    while (pi >= 0) {
        puddles[pi].left -= dt
        if (puddles[pi].left <= 0f) puddles.removeAt(pi)
        pi--
    }
    val p = passenger ?: return
    p.recoil = max(0f, p.recoil - dt * 8f)
    p.flash = max(0f, p.flash - dt)
    if (p.speechT > 0f) {
        p.speechT -= dt
        if (p.speechT <= 0f) p.speech = null
    }
    if (p.state != Passenger.RIDING) p.animT += dt
}

fun TrainGame.debugBoard(who: Hitchhiker, grade: Grade? = null) {
    passenger = null
    boardHitchhiker(who, grade ?: rollGrade(), perchX() - 60f, RAIL_Y + 30f)
}

fun TrainGame.debugCallHitcher(who: Hitchhiker = Hitchhiker.entries[rng.nextInt(Hitchhiker.entries.size)]) {
    (lead ?: this).callHitcher(who)
}

// ---------------------------------------------------------------- shared-world snapshot

fun TrainGame.writeHitcher(out: DataOutputStream) {
    val h = hitcher
    out.writeBoolean(h != null)
    if (h != null) {
        out.writeByte(h.who.ordinal); out.writeByte(h.grade.ordinal); out.writeFloat(h.x); out.writeByte(h.side + 1)
        out.writeFloat(h.life); out.writeFloat(h.phase)
    }
}

fun TrainGame.readHitcher(input: DataInputStream) {
    if (input.readBoolean()) {
        val who = Hitchhiker.entries[input.readUnsignedByte().coerceAtMost(Hitchhiker.entries.lastIndex)]
        val grade = Grade.entries[input.readUnsignedByte().coerceAtMost(Grade.entries.lastIndex)]
        val x = input.readFloat()
        val side = input.readUnsignedByte() - 1
        val h = hitcher
        val fresh = if (h != null && h.who == who && h.grade == grade && h.x == x) h else Hitcher(who, grade, x, side)
        fresh.life = input.readFloat()
        fresh.phase = input.readFloat()
        hitcher = fresh
    } else {
        hitcher = null
    }
}

fun TrainGame.writePassenger(out: DataOutputStream) {
    val p = passenger
    out.writeBoolean(p != null)
    if (p != null) {
        out.writeByte(p.who.ordinal); out.writeByte(p.grade.ordinal)
        out.writeFloat(p.left); out.writeFloat(p.total); out.writeByte(p.state); out.writeFloat(p.animT)
        out.writeFloat(p.fromX); out.writeFloat(p.fromY)
        out.writeBoolean(p.facing > 0); out.writeFloat(p.aim); out.writeFloat(p.recoil); out.writeFloat(p.flash)
        val s = p.speech
        out.writeBoolean(s != null)
        if (s != null) out.writeUTF(s)
        out.writeFloat(p.speechT)
    }
}

fun TrainGame.readPassenger(input: DataInputStream) {
    if (!input.readBoolean()) {
        passenger = null
        return
    }
    val who = Hitchhiker.entries[input.readUnsignedByte().coerceAtMost(Hitchhiker.entries.lastIndex)]
    val grade = Grade.entries[input.readUnsignedByte().coerceAtMost(Grade.entries.lastIndex)]
    val left = input.readFloat()
    val existing = passenger
    val p = if (existing != null && existing.who == who && existing.grade == grade) existing else Passenger(who, grade, left)
    p.left = left
    p.total = input.readFloat()
    p.state = input.readUnsignedByte()
    p.animT = input.readFloat()
    p.fromX = input.readFloat(); p.fromY = input.readFloat()
    p.facing = if (input.readBoolean()) 1 else -1
    p.aim = input.readFloat()
    p.recoil = max(p.recoil, input.readFloat())
    p.flash = max(p.flash, input.readFloat())
    p.speech = if (input.readBoolean()) input.readUTF() else null
    p.speechT = input.readFloat()
    passenger = p
}

// ---------------------------------------------------------------- puddles (the soup cannon's spill)

/**
 * A spill on the ground: while a zombie stands in it, it slips (slowed) and takes extra damage. [x] is where it landed,
 * [left] how long it lasts and [radius] how far it spreads. Per train, in that train's lane.
 */
class Puddle(var x: Float, val who: Hitchhiker, var left: Float, val radius: Float, val vuln: Float) {
    val total = left
}

private const val MAX_PUDDLES = 6

/** Spills [who]'s puddle at [x]; a new one next to an old one just refreshes it, so firing at one spot doesn't pile them up. */
private fun TrainGame.spill(who: Hitchhiker, grade: Grade, x: Float) {
    val time = who.puddleTime * (1f + 0.25f * grade.bonus)
    val radius = who.puddle * (1f + 0.15f * grade.bonus)
    val vuln = who.puddleVuln * (0.8f + 0.2f * grade.ordinal)
    for (i in puddles.indices) {
        val p = puddles[i]
        if (abs(p.x - x) < radius * 0.6f && p.who == who) {
            p.left = max(p.left, time)
            return
        }
    }
    if (puddles.size >= MAX_PUDDLES) puddles.removeAt(0)
    puddles.add(Puddle(x, who, time, radius, vuln))
}

/** Ages the puddles and applies them: zombies standing in one slip and take more damage for as long as they stay. */
private fun TrainGame.tickPuddles(dt: Float) {
    if (puddles.isEmpty()) return
    var i = puddles.size - 1
    while (i >= 0) {
        puddles[i].left -= dt
        if (puddles[i].left <= 0f) puddles.removeAt(i)
        i--
    }
    for (k in zombies.indices) {
        val z = zombies[k]
        if (z.dead || z.lane != lane || z.flying || z.jumpState != 0) continue
        for (n in puddles.indices) {
            val p = puddles[n]
            if (abs(z.x - p.x) > p.radius) continue
            if (z.vulnT <= 0f) { // newly in the puddle: slip!
                floatText(z.x, z.y - 46f * z.type.size, "SLIP!", 0.7f, p.who.puddleColor.toInt(), 16f)
                addParticle(Particle(z.x, z.y - 6f, 0f, -50f, 0.4f, 0.4f, 3f, p.who.puddleColor.toInt(), ParticleKind.BLOOD))
            }
            val slow = if (z.isBoss) 1f - (1f - p.who.puddleSlow) * 0.4f else p.who.puddleSlow
            z.slowMul = if (z.slowT > 0.3f) min(z.slowMul, slow) else slow
            z.slowT = max(z.slowT, 0.3f)
            z.vulnT = 0.4f
            z.vulnMul = max(z.vulnMul, p.vuln)
        }
    }
}

fun TrainGame.writePuddles(out: DataOutputStream) {
    out.writeByte(puddles.size)
    for (p in puddles) {
        out.writeByte(p.who.ordinal); out.writeFloat(p.x); out.writeFloat(p.left); out.writeFloat(p.radius); out.writeFloat(p.vuln)
    }
}

fun TrainGame.readPuddles(input: DataInputStream) {
    puddles.clear()
    repeat(input.readUnsignedByte()) {
        val who = Hitchhiker.entries[input.readUnsignedByte().coerceAtMost(Hitchhiker.entries.lastIndex)]
        val x = input.readFloat()
        val left = input.readFloat()
        val radius = input.readFloat()
        val vuln = input.readFloat()
        puddles.add(Puddle(x, who, left, radius, vuln))
    }
}
