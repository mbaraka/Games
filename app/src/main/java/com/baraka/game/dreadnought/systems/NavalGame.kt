package com.baraka.game.dreadnought.systems

import com.baraka.game.dreadnought.model.MAIN_SHELL_SPEED
import com.baraka.game.dreadnought.model.MountKind
import com.baraka.game.dreadnought.model.Refit
import com.baraka.game.dreadnought.model.SEC_SHELL_SPEED
import com.baraka.game.dreadnought.model.ShipClass
import com.baraka.game.dreadnought.model.TORPEDO_SPEED
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

const val PI_F = Math.PI.toFloat()
const val TWO_PI_F = (Math.PI * 2).toFloat()

fun wrapAngle(a: Float): Float {
    var r = a
    while (r > PI_F) r -= TWO_PI_F
    while (r < -PI_F) r += TWO_PI_F
    return r
}

enum class DreadCue(val minGapMs: Long) {
    MAIN_GUN(60), SEC_GUN(45), SHELL_HIT(40), SPLASH(40), BOOM(120), TORPEDO(200),
    HIT_PLAYER(90), REPAIR(300), WAVE_CLEAR(500), GAME_OVER(800), UI(40)
}

const val PHASE_BRIEF = 0
const val PHASE_FIGHT = 1
const val PHASE_CLEARED = 2
const val PHASE_REFIT = 3
const val PHASE_OVER = 4

const val MAX_FIRES = 3

/** A ship afloat: the player's or an enemy's. All the numbers that change during play live here. */
class Ship(val cls: ShipClass, val side: Int) {
    var x = 0f
    var y = 0f
    var heading = 0f
    var speed = 0f
    var angVel = 0f
    var orderHeading = 0f
    var orderSpeed = 0f
    var hp = cls.maxHp
    var maxHp = cls.maxHp
    var dmgMult = 1f
    var skill = 0.5f
    var leadErr = 1f
    var alive = true
    var sinkT = 0f
    var depth = 0f
    var surfaceHold = 0f
    val turretOff = FloatArray(cls.mounts.size)
    val reload = FloatArray(cls.mounts.size)
    val kick = FloatArray(cls.mounts.size)
    var tubeReload = 0f
    var flash = 0f
    var fireN = 0
    val fireX = FloatArray(MAX_FIRES)
    val fireY = FloatArray(MAX_FIRES)
    var wakeT = 0f
    var fireT = 0f
    var orbitDir = 1f
    var retreatT = 0f
    var flipT = 12f
    var escortT = 8f
    var targetT = 0f
    var secTarget: Ship? = null
    var wakeSide = 1f
}

const val P_WAKE = 0
const val P_SMOKE = 1
const val P_SPARK = 2
const val P_SPLASH = 3
const val P_BLAST = 4
const val P_FLASH = 5
const val P_DEBRIS = 6
const val P_BUBBLE = 7

/**
 * The whole simulation: ships, shells, torpedoes, particles, waves. No drawing and no Android types, so the
 * renderer (`render/NavalRenderer.kt`) and the UI only read it. Everything gameplay can grow is a fixed-size
 * pool (see the `*_MAX` constants), so a long sortie allocates nothing per frame.
 */
class NavalGame(permanentLevels: List<Int>, startWave: Int = 1) {
    private val rnd = Random(System.nanoTime())

    // --- options (set by the screen from the settings) ---
    var shakeOn = true
    var numbersOn = true
    var assistOn = true
    var autoFire = false
    var god = false
    var instantReload = false

    // --- upgrades ---
    private val perm = IntArray(Refit.entries.size) { permanentLevels.getOrElse(it) { 0 } }
    val temp = IntArray(Refit.entries.size)
    fun level(r: Refit): Int = perm[r.ordinal] + temp[r.ordinal]

    // --- ships ---
    val ships = ArrayList<Ship>(24)
    val player: Ship = Ship(ShipClass.DREADNOUGHT, 0)

    // --- sortie state ---
    var time = 0f
    var wave = 0
    var phase = PHASE_BRIEF
    var phaseT = 2.6f
    var score = 0
    var runCoins = 0
    var kills = 0
    var flagships = 0
    var enemiesLeft = 0
    private val queue = ArrayList<ShipClass>(24)
    private var spawnT = 0f
    private var overT = 0f
    val refitOffer = IntArray(3)

    // --- controls ---
    var firing = false
    var aimX = 0f
    var aimY = 0f
    var lock: Ship? = null
    var repairCd = 0f
    var repairCdMax = 45f
    private var repairLeft = 0f
    private var repairRate = 0f
    var mainReady = 1f
    var aimInRange = false

    // --- camera ---
    var camX = 0f
    var camY = 0f
    var shake = 0f

    // --- shells ---
    val shAlive = BooleanArray(SH_MAX)
    val shOx = FloatArray(SH_MAX)
    val shOy = FloatArray(SH_MAX)
    val shTx = FloatArray(SH_MAX)
    val shTy = FloatArray(SH_MAX)
    val shDur = FloatArray(SH_MAX)
    val shE = FloatArray(SH_MAX)
    val shDmg = FloatArray(SH_MAX)
    val shSide = IntArray(SH_MAX)
    val shKind = IntArray(SH_MAX)
    private var shNext = 0

    // --- torpedoes ---
    val tpAlive = BooleanArray(TP_MAX)
    val tpX = FloatArray(TP_MAX)
    val tpY = FloatArray(TP_MAX)
    val tpVx = FloatArray(TP_MAX)
    val tpVy = FloatArray(TP_MAX)
    val tpLife = FloatArray(TP_MAX)
    val tpDmg = FloatArray(TP_MAX)
    val tpSide = IntArray(TP_MAX)
    val tpBubble = FloatArray(TP_MAX)
    private var tpNext = 0

    // --- particles ---
    val pAlive = BooleanArray(P_MAX)
    val pKind = IntArray(P_MAX)
    val pX = FloatArray(P_MAX)
    val pY = FloatArray(P_MAX)
    val pVx = FloatArray(P_MAX)
    val pVy = FloatArray(P_MAX)
    val pLife = FloatArray(P_MAX)
    val pMax = FloatArray(P_MAX)
    val pSize = FloatArray(P_MAX)
    val pGrow = FloatArray(P_MAX)
    private var pNext = 0

    // --- floating numbers ---
    val txAlive = BooleanArray(TX_MAX)
    val txX = FloatArray(TX_MAX)
    val txY = FloatArray(TX_MAX)
    val txLife = FloatArray(TX_MAX)
    val txColor = IntArray(TX_MAX)
    val txStr = arrayOfNulls<String>(TX_MAX)
    private var txNext = 0
    private val numberStrings = Array(1000) { it.toString() }

    // --- sound cues for the screen to play after each update ---
    val cueIds = IntArray(CUE_MAX)
    val cueVol = FloatArray(CUE_MAX)
    var cueCount = 0

    // scratch for lead calculation (no allocation per shot)
    private var leadX = 0f
    private var leadY = 0f

    init {
        ships.add(player)
        applyStats(heal = true)
        aimX = 400f
        aimY = 0f
        beginWave(startWave.coerceAtLeast(1))
    }

    // ---------------------------------------------------------------------------------------------
    // upgrade-derived stats

    private fun mainDmgMult() = 1f + 0.12f * level(Refit.MAIN_DAMAGE)
    private fun reloadMult() = 1f - 0.08f * level(Refit.RELOAD)
    private fun spreadMult() = 1f - 0.14f * level(Refit.FIRE_CONTROL)
    fun mainRange(): Float = player.cls.mainRange * (1f + 0.07f * level(Refit.RANGE))
    private fun engineMult() = 1f + 0.07f * level(Refit.ENGINES)
    private fun secMult() = 1f + 0.18f * level(Refit.SECONDARIES)
    fun secRange(): Float = player.cls.secRange * (1f + 0.18f * level(Refit.SECONDARIES) * 0.4f)

    private fun applyStats(heal: Boolean) {
        val frac = if (heal) 1f else player.hp / player.maxHp
        player.maxHp = player.cls.maxHp * (1f + 0.14f * level(Refit.ARMOR))
        player.hp = player.maxHp * frac
        repairCdMax = 45f - 4f * level(Refit.DAMAGE_CONTROL)
    }

    // ---------------------------------------------------------------------------------------------
    // controls

    /** Course and speed orders from the left stick: [heading] in radians, [throttle] 0..1. */
    fun setHelm(heading: Float, throttle: Float) {
        if (!player.alive) return
        player.orderHeading = heading
        player.orderSpeed = throttle.coerceIn(0f, 1f)
    }

    fun setThrottle(throttle: Float) {
        player.orderSpeed = throttle.coerceIn(0f, 1f)
    }

    /** Aim the main battery at a point of the sea, or lock onto a ship if the tap lands on one. */
    fun aimAt(wx: Float, wy: Float) {
        var best: Ship? = null
        var bestD = 1e9f
        for (i in 1 until ships.size) {
            val s = ships[i]
            if (!s.alive || s.depth > 0.35f) continue
            val d = hypot(s.x - wx, s.y - wy)
            val reach = s.cls.length * 0.35f + 18f
            if (d < reach && d < bestD) {
                bestD = d
                best = s
            }
        }
        lock = best
        aimX = wx
        aimY = wy
    }

    fun requestRepair() {
        if (!player.alive || repairCd > 0f || (player.hp >= player.maxHp && player.fireN == 0)) return
        repairLeft = player.maxHp * (0.28f + 0.04f * level(Refit.DAMAGE_CONTROL))
        repairRate = repairLeft / 5f
        repairCd = repairCdMax
        player.fireN = 0
        cue(DreadCue.REPAIR, player.x, player.y, 1f)
    }

    fun pickRefit(index: Int) {
        if (phase != PHASE_REFIT) return
        temp[refitOffer[index.coerceIn(0, 2)]]++
        applyStats(heal = false)
        player.hp = min(player.maxHp, player.hp + player.maxHp * 0.12f)
        beginWave(wave + 1)
    }

    // ---------------------------------------------------------------------------------------------
    // main update

    fun update(rawDt: Float) {
        val dt = min(rawDt, 0.05f)
        cueCount = 0
        if (phase == PHASE_REFIT) return
        time += dt

        when (phase) {
            PHASE_BRIEF -> {
                phaseT -= dt
                if (phaseT <= 0f) {
                    phase = PHASE_FIGHT
                    spawnT = 0f
                }
            }
            PHASE_FIGHT -> {
                spawnT -= dt
                if (spawnT <= 0f && queue.isNotEmpty() && aliveEnemies() < MAX_ACTIVE) {
                    spawnFromQueue()
                    spawnT = 0.9f
                }
                if (queue.isEmpty() && aliveEnemies() == 0) waveCleared()
            }
            PHASE_CLEARED -> {
                phaseT -= dt
                if (phaseT <= 0f) offerRefit()
            }
            PHASE_OVER -> {}
        }

        updatePlayerAim()
        val px0 = player.x
        val py0 = player.y
        var i = 0
        while (i < ships.size) {
            val s = ships[i]
            if (s.alive) {
                if (s.side == 1) runAi(s, dt)
                updateMounts(s, dt)
                if (s.side == 1 || s === player) tickTubes(s, dt)
            }
            moveShip(s, dt)
            shipEffects(s, dt)
            i++
        }
        separateShips()
        // an aim point on open sea travels with the ship (same bearing and range); a locked target tracks itself
        if (lock == null) {
            aimX += player.x - px0
            aimY += player.y - py0
        }
        updateShells(dt)
        updateTorpedoes(dt)
        updateParticles(dt)
        updateTexts(dt)
        updateRepair(dt)
        reapSunk(dt)
        enemiesLeft = aliveEnemies() + queue.size

        val lead = 0.9f
        val tx = player.x + cos(player.heading) * player.speed * lead
        val ty = player.y + sin(player.heading) * player.speed * lead
        val k = min(1f, dt * 3.5f)
        camX += (tx - camX) * k
        camY += (ty - camY) * k
        shake = max(0f, shake - dt * 2.4f)
    }

    private fun aliveEnemies(): Int {
        var n = 0
        for (i in 1 until ships.size) if (ships[i].alive) n++
        return n
    }

    // ---------------------------------------------------------------------------------------------
    // waves

    private fun beginWave(n: Int) {
        wave = n
        phase = PHASE_BRIEF
        phaseT = 2.6f
        queue.clear()
        var budget = 2.5f + n * 1.7f
        if (n % 5 == 0) {
            queue.add(ShipClass.FLAGSHIP)
            budget *= 0.45f
        }
        val pool = ShipClass.SPAWNABLE
        var guard = 0
        while (budget >= 1f && guard < 60) {
            guard++
            val c = pool[rnd.nextInt(pool.size)]
            if (c.minWave > n || c.cost > budget) continue
            queue.add(c)
            budget -= c.cost
        }
        if (queue.isEmpty()) queue.add(ShipClass.PATROL_BOAT)
        enemiesLeft = queue.size
    }

    private fun waveCleared() {
        phase = PHASE_CLEARED
        phaseT = 2.8f
        val bonus = 20 + wave * 8
        runCoins += bonus
        score += wave * 50
        player.hp = min(player.maxHp, player.hp + player.maxHp * 0.15f)
        cue(DreadCue.WAVE_CLEAR, player.x, player.y, 1f)
    }

    private fun offerRefit() {
        val n = Refit.entries.size
        val pool = IntArray(n) { it }
        for (i in 0 until 3) {
            val j = i + rnd.nextInt(n - i)
            val t = pool[i]
            pool[i] = pool[j]
            pool[j] = t
            refitOffer[i] = pool[i]
        }
        phase = PHASE_REFIT
    }

    private fun spawnFromQueue() {
        val cls = queue.removeAt(queue.size - 1)
        val a = rnd.nextFloat() * TWO_PI_F
        val d = 820f + rnd.nextFloat() * 200f
        spawn(cls, player.x + cos(a) * d, player.y + sin(a) * d)
    }

    /** Puts an enemy of [cls] at a world position, aimed roughly at the player, with this wave's strength. */
    fun spawn(cls: ShipClass, x: Float, y: Float): Ship {
        val s = Ship(cls, 1)
        s.x = x
        s.y = y
        s.heading = atan2(player.y - y, player.x - x) + (rnd.nextFloat() - 0.5f) * 0.8f
        s.orderHeading = s.heading
        s.speed = cls.maxSpeed * 0.6f
        s.orderSpeed = 0.8f
        val w = max(1, wave)
        val hpMult = 1f + 0.07f * (w - 1)
        s.maxHp = cls.maxHp * hpMult
        s.hp = s.maxHp
        s.dmgMult = 1f + 0.035f * (w - 1)
        s.skill = min(1f, 0.4f + 0.04f * w)
        s.leadErr = 0.85f + rnd.nextFloat() * 0.25f
        s.orbitDir = if (rnd.nextBoolean()) 1f else -1f
        s.flipT = 10f + rnd.nextFloat() * 12f
        s.escortT = 6f + rnd.nextFloat() * 6f
        s.tubeReload = 3f + rnd.nextFloat() * cls.torpReload * 0.6f
        for (i in s.reload.indices) s.reload[i] = rnd.nextFloat() * 3f
        if (cls.diver) s.depth = 1f
        ships.add(s)
        return s
    }

    // ---------------------------------------------------------------------------------------------
    // enemy brain

    private fun runAi(s: Ship, dt: Float) {
        val p = player
        val c = s.cls
        if (!p.alive) {
            s.orderSpeed = 0.35f
            return
        }
        val dx = p.x - s.x
        val dy = p.y - s.y
        val dist = hypot(dx, dy)
        val bearing = atan2(dy, dx)

        s.flipT -= dt
        if (s.flipT <= 0f) {
            s.orbitDir = -s.orbitDir
            s.flipT = 10f + rnd.nextFloat() * 14f
        }
        s.retreatT -= dt

        var desired: Float
        var thr: Float
        val near = c.standoff
        if (s.retreatT > 0f) {
            desired = bearing + PI_F + s.orbitDir * 0.7f
            thr = 1f
        } else if (dist > near * 1.2f) {
            desired = bearing
            thr = 1f
        } else if (dist < near * 0.75f) {
            desired = bearing + PI_F - s.orbitDir * 0.5f
            thr = 0.85f
        } else {
            desired = bearing + s.orbitDir * 1.25f
            thr = 0.6f
        }
        if (c.diver) {
            // a submarine creeps in, surfaces to shoot, then dives again
            s.surfaceHold -= dt
            val wantUp = dist < 540f && s.tubeReload <= 0.6f
            val target = if (wantUp || s.surfaceHold > 0f) 0f else 1f
            val step = dt / 1.4f
            s.depth = if (s.depth < target) min(target, s.depth + step) else max(target, s.depth - step)
            thr = if (s.depth > 0.5f) 0.8f else 0.4f
        }
        s.orderHeading = desired
        s.orderSpeed = thr

        if (c.escorts) {
            s.escortT -= dt
            if (s.escortT <= 0f && phase == PHASE_FIGHT) {
                var boats = 0
                for (i in 1 until ships.size) if (ships[i].alive && ships[i].cls == ShipClass.PATROL_BOAT) boats++
                if (boats < 3) {
                    val e = spawn(ShipClass.PATROL_BOAT, s.x - cos(s.heading) * 60f, s.y - sin(s.heading) * 60f)
                    e.heading = s.heading
                    e.speed = 40f
                }
                s.escortT = 16f
            }
        }
    }

    /** Torpedo launchers for any ship that has them (the enemies'; the player has none). */
    private fun tickTubes(s: Ship, dt: Float) {
        val c = s.cls
        if (c.tubes == 0) return
        s.tubeReload -= dt
        if (s.tubeReload > 0f || s.side == 0 || !player.alive) return
        if (c.diver && s.depth > 0.12f) return
        val dist = hypot(player.x - s.x, player.y - s.y)
        if (dist < 140f || dist > 560f) return
        leadPoint(s.x, s.y, TORPEDO_SPEED, player, 0.9f)
        val ang = atan2(leadY - s.y, leadX - s.x)
        if (kotlin.math.abs(wrapAngle(ang - s.heading)) > 0.7f) return
        val n = c.tubes
        for (k in 0 until n) {
            val a = ang + (k - (n - 1) / 2f) * 0.1f + (rnd.nextFloat() - 0.5f) * 0.05f * (1.4f - s.skill)
            launchTorpedo(s.x + cos(a) * c.length * 0.4f, s.y + sin(a) * c.length * 0.4f, a, c.torpDamage * s.dmgMult, s.side)
        }
        s.tubeReload = c.torpReload * (0.9f + rnd.nextFloat() * 0.3f)
        if (c.diver) s.surfaceHold = 3.2f else s.retreatT = 3.5f
        cue(DreadCue.TORPEDO, s.x, s.y, 0.8f)
    }

    // ---------------------------------------------------------------------------------------------
    // gunnery

    /** Where a shell or torpedo launched at [speed] from (sx, sy) should land to meet [t]; result in [leadX]/[leadY]. */
    private fun leadPoint(sx: Float, sy: Float, speed: Float, t: Ship, factor: Float) {
        val vx = cos(t.heading) * t.speed
        val vy = sin(t.heading) * t.speed
        var px = t.x
        var py = t.y
        for (k in 0 until 3) {
            val time = hypot(px - sx, py - sy) / speed
            px = t.x + vx * time * factor
            py = t.y + vy * time * factor
        }
        leadX = px
        leadY = py
    }

    private fun updatePlayerAim() {
        val l = lock
        if (l != null) {
            if (!l.alive || l.depth > 0.35f) {
                lock = null
            } else if (assistOn) {
                leadPoint(player.x, player.y, MAIN_SHELL_SPEED, l, 1f)
                aimX = leadX
                aimY = leadY
            } else {
                aimX = l.x
                aimY = l.y
            }
        }
        aimInRange = hypot(aimX - player.x, aimY - player.y) <= mainRange() * 1.02f
    }

    private fun updateMounts(s: Ship, dt: Float) {
        val c = s.cls
        val isPlayer = s === player
        var readySum = 0f
        var mains = 0
        s.targetT -= dt
        if (isPlayer && s.targetT <= 0f) {
            s.targetT = 0.35f
            s.secTarget = nearestEnemy(secRange())
        }
        val tgt: Ship? = if (isPlayer) s.secTarget else if (player.alive) player else null

        for (i in c.mounts.indices) {
            val m = c.mounts[i]
            val reloadTime = reloadTimeOf(s, m.kind)
            if (instantReload && isPlayer) s.reload[i] = 0f
            if (s.reload[i] > 0f) s.reload[i] -= dt
            if (s.kick[i] > 0f) s.kick[i] = max(0f, s.kick[i] - dt * 3.2f)
            val main = m.kind == MountKind.MAIN
            if (main && isPlayer) {
                mains++
                readySum += 1f - (max(0f, s.reload[i]) / reloadTime).coerceIn(0f, 1f)
            }

            // choose what this mount shoots at
            var tx: Float
            var ty: Float
            val speed = if (main) MAIN_SHELL_SPEED else SEC_SHELL_SPEED
            if (main && isPlayer) {
                tx = aimX
                ty = aimY
            } else if (tgt != null && tgt.alive && !(tgt.depth > 0.35f)) {
                leadPoint(s.x, s.y, speed, tgt, if (isPlayer) 1f else (0.55f + 0.45f * s.skill) * s.leadErr)
                tx = leadX
                ty = leadY
            } else {
                // nothing to shoot at: turn back to the rest position
                stepTurret(s, i, s.heading + m.arcCenter, dt, main)
                continue
            }

            val ca = cos(s.heading)
            val sa = sin(s.heading)
            val mx = s.x + ca * m.x - sa * m.y
            val my = s.y + sa * m.x + ca * m.y
            val aligned = stepTurret(s, i, atan2(ty - my, tx - mx), dt, main)
            val range = if (isPlayer) (if (main) mainRange() else secRange()) else if (main) c.mainRange else c.secRange
            val dist = hypot(tx - mx, ty - my)
            val want = if (isPlayer && main) (firing || (autoFire && aimInRange)) else true
            if (want && aligned && s.reload[i] <= 0f && dist <= range && s.depth < 0.35f && (!isPlayer || s.alive)) {
                if (!isPlayer || main || tgt != null) fireMount(s, i, mx, my, tx, ty)
            }
        }
        if (isPlayer) mainReady = if (mains > 0) readySum / mains else 1f
    }

    /** How far the player's mount [i] has reloaded, 0..1 (1 = ready to fire). */
    fun reloadFrac(i: Int): Float {
        val rt = reloadTimeOf(player, player.cls.mounts[i].kind)
        return (1f - max(0f, player.reload[i]) / rt).coerceIn(0f, 1f)
    }

    private fun reloadTimeOf(s: Ship, kind: MountKind): Float {
        val c = s.cls
        return if (kind == MountKind.MAIN) c.mainReload * (if (s === player) reloadMult() else 1f) else c.secReload
    }

    /** Swings turret [i] towards the absolute bearing [worldAngle], inside its arc. Returns true when it is on target. */
    private fun stepTurret(s: Ship, i: Int, worldAngle: Float, dt: Float, main: Boolean): Boolean {
        val m = s.cls.mounts[i]
        val rel = wrapAngle(worldAngle - s.heading)
        val off = wrapAngle(rel - m.arcCenter)
        val inArc = kotlin.math.abs(off) <= m.arcHalf
        val goal = off.coerceIn(-m.arcHalf, m.arcHalf)
        val rate = if (main) (if (s === player) 0.85f else 0.55f) else 2.4f
        val step = rate * dt
        val cur = s.turretOff[i]
        s.turretOff[i] = cur + (goal - cur).coerceIn(-step, step)
        return inArc && kotlin.math.abs(goal - s.turretOff[i]) < 0.05f
    }

    private fun nearestEnemy(range: Float): Ship? {
        var best: Ship? = null
        var bestD = range
        for (i in 1 until ships.size) {
            val s = ships[i]
            if (!s.alive || s.depth > 0.35f) continue
            val d = hypot(s.x - player.x, s.y - player.y)
            if (d < bestD) {
                bestD = d
                best = s
            }
        }
        return best
    }

    private fun gauss(): Float = (rnd.nextFloat() + rnd.nextFloat() + rnd.nextFloat() - 1.5f) * 2f

    private fun fireMount(s: Ship, i: Int, mx: Float, my: Float, tx0: Float, ty0: Float) {
        val c = s.cls
        val m = c.mounts[i]
        val isPlayer = s === player
        val main = m.kind == MountKind.MAIN
        val a = s.heading + m.arcCenter + s.turretOff[i]
        val ca = cos(a)
        val sa = sin(a)
        val dist = hypot(tx0 - mx, ty0 - my)
        val spread = when {
            isPlayer && main -> 0.035f * spreadMult()
            isPlayer -> 0.05f
            else -> 0.055f * (1.35f - 0.5f * s.skill)
        }
        val sigma = dist * spread
        val damage = when {
            isPlayer && main -> c.mainDamage * mainDmgMult()
            isPlayer -> c.secDamage * secMult()
            main -> c.mainDamage * s.dmgMult
            else -> c.secDamage * s.dmgMult
        }
        val speed = if (main) MAIN_SHELL_SPEED else SEC_SHELL_SPEED
        val barrels = if (main) m.barrels else 1
        val muzzle = m.radius * 2.1f
        for (k in 0 until barrels) {
            val lateral = (k - (barrels - 1) / 2f) * m.radius * 0.62f
            val ox = mx + ca * muzzle - sa * lateral
            val oy = my + sa * muzzle + ca * lateral
            val along = gauss() * sigma * 1.35f
            val across = gauss() * sigma
            val tx = tx0 + ca * along - sa * across
            val ty = ty0 + sa * along + ca * across
            val flight = max(0.2f, hypot(tx - ox, ty - oy) / speed)
            fireShell(ox, oy, tx, ty, flight, damage, s.side, if (main) 0 else 1)
        }
        s.reload[i] = reloadTimeOf(s, m.kind) * (0.92f + rnd.nextFloat() * 0.16f)
        s.kick[i] = 1f
        val fx = mx + ca * muzzle * 1.5f
        val fy = my + sa * muzzle * 1.5f
        emit(P_FLASH, fx, fy, 0f, 0f, if (main) 0.12f else 0.06f, m.radius * (if (main) 3.2f else 2f), 0f)
        val puffs = if (main) 3 else 1
        for (k in 0 until puffs) {
            emit(P_SMOKE, fx, fy, ca * 10f + gauss() * 3f, sa * 10f + gauss() * 3f, 1.2f + rnd.nextFloat() * 0.6f, m.radius * 0.9f, m.radius * 1.8f)
        }
        if (main && isPlayer) shake = max(shake, 0.28f)
        cue(if (main) DreadCue.MAIN_GUN else DreadCue.SEC_GUN, mx, my, if (main) 1f else 0.6f)
    }

    // ---------------------------------------------------------------------------------------------
    // shells and torpedoes

    private fun fireShell(ox: Float, oy: Float, tx: Float, ty: Float, dur: Float, dmg: Float, side: Int, kind: Int) {
        var idx = -1
        for (k in 0 until SH_MAX) {
            val j = (shNext + k) % SH_MAX
            if (!shAlive[j]) {
                idx = j
                break
            }
        }
        if (idx < 0) idx = shNext
        shNext = (idx + 1) % SH_MAX
        shAlive[idx] = true
        shOx[idx] = ox
        shOy[idx] = oy
        shTx[idx] = tx
        shTy[idx] = ty
        shDur[idx] = dur
        shE[idx] = 0f
        shDmg[idx] = dmg
        shSide[idx] = side
        shKind[idx] = kind
    }

    /** Current position of shell [i] and its height (0 at the muzzle and at the target, 1 at the top of the arc). */
    fun shellProgress(i: Int): Float = (shE[i] / shDur[i]).coerceIn(0f, 1f)

    private fun updateShells(dt: Float) {
        for (i in 0 until SH_MAX) {
            if (!shAlive[i]) continue
            shE[i] += dt
            if (shE[i] >= shDur[i]) {
                shAlive[i] = false
                impact(shTx[i], shTy[i], shDmg[i], shSide[i], shKind[i])
            }
        }
    }

    private fun impact(tx: Float, ty: Float, dmg: Float, side: Int, kind: Int) {
        val rr = if (kind == 0) 7f else 3f
        for (i in 0 until ships.size) {
            val s = ships[i]
            if (!s.alive || s.side == side || s.depth > 0.35f) continue
            val ca = cos(s.heading)
            val sa = sin(s.heading)
            val dx = tx - s.x
            val dy = ty - s.y
            val lx = dx * ca + dy * sa
            val ly = -dx * sa + dy * ca
            val a = s.cls.length * 0.5f + rr
            val b = s.cls.beam * 0.5f + rr
            if ((lx / a) * (lx / a) + (ly / b) * (ly / b) <= 1f) {
                hitShip(s, dmg, lx, ly, tx, ty, kind == 0)
                return
            }
        }
        splash(tx, ty, kind == 0)
    }

    private fun splash(x: Float, y: Float, big: Boolean) {
        emit(P_SPLASH, x, y, 0f, 0f, if (big) 1.1f else 0.6f, if (big) 5f else 2f, if (big) 26f else 10f)
        val n = if (big) 7 else 3
        for (k in 0 until n) {
            val a = rnd.nextFloat() * TWO_PI_F
            val v = 8f + rnd.nextFloat() * (if (big) 22f else 10f)
            emit(P_SPARK, x, y, cos(a) * v, sin(a) * v, 0.5f + rnd.nextFloat() * 0.4f, if (big) 2.2f else 1.2f, 0f)
        }
        cue(DreadCue.SPLASH, x, y, if (big) 0.9f else 0.4f)
    }

    private fun launchTorpedo(x: Float, y: Float, ang: Float, dmg: Float, side: Int) {
        var idx = -1
        for (k in 0 until TP_MAX) {
            val j = (tpNext + k) % TP_MAX
            if (!tpAlive[j]) {
                idx = j
                break
            }
        }
        if (idx < 0) idx = tpNext
        tpNext = (idx + 1) % TP_MAX
        tpAlive[idx] = true
        tpX[idx] = x
        tpY[idx] = y
        tpVx[idx] = cos(ang) * TORPEDO_SPEED
        tpVy[idx] = sin(ang) * TORPEDO_SPEED
        tpLife[idx] = 11f
        tpDmg[idx] = dmg
        tpSide[idx] = side
        tpBubble[idx] = 0f
    }

    private fun updateTorpedoes(dt: Float) {
        for (i in 0 until TP_MAX) {
            if (!tpAlive[i]) continue
            tpX[i] += tpVx[i] * dt
            tpY[i] += tpVy[i] * dt
            tpLife[i] -= dt
            tpBubble[i] -= dt
            if (tpBubble[i] <= 0f) {
                tpBubble[i] = 0.09f
                emit(P_BUBBLE, tpX[i], tpY[i], 0f, 0f, 1.1f, 1.6f, 1.2f)
            }
            if (tpLife[i] <= 0f) {
                tpAlive[i] = false
                continue
            }
            for (j in 0 until ships.size) {
                val s = ships[j]
                if (!s.alive || s.side == tpSide[i] || s.depth > 0.35f) continue
                val ca = cos(s.heading)
                val sa = sin(s.heading)
                val dx = tpX[i] - s.x
                val dy = tpY[i] - s.y
                val lx = dx * ca + dy * sa
                val ly = -dx * sa + dy * ca
                val a = s.cls.length * 0.5f
                val b = s.cls.beam * 0.5f + 2f
                if ((lx / a) * (lx / a) + (ly / b) * (ly / b) <= 1f) {
                    tpAlive[i] = false
                    hitShip(s, tpDmg[i], lx, ly, tpX[i], tpY[i], true)
                    emit(P_SPLASH, tpX[i], tpY[i], 0f, 0f, 1.2f, 6f, 30f)
                    cue(DreadCue.BOOM, tpX[i], tpY[i], 1f)
                    break
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // damage

    private fun hitShip(s: Ship, dmg: Float, lx: Float, ly: Float, wx: Float, wy: Float, heavy: Boolean) {
        val isPlayer = s === player
        val amount = if (isPlayer && god) 0f else dmg
        s.hp -= amount
        s.flash = 1f
        emit(P_BLAST, wx, wy, 0f, 0f, if (heavy) 0.45f else 0.25f, if (heavy) 7f else 3f, if (heavy) 12f else 5f)
        val sparks = if (heavy) 6 else 3
        for (k in 0 until sparks) {
            val a = rnd.nextFloat() * TWO_PI_F
            val v = 20f + rnd.nextFloat() * 50f
            emit(P_SPARK, wx, wy, cos(a) * v, sin(a) * v, 0.3f + rnd.nextFloat() * 0.3f, 1.3f, 0f)
        }
        emit(P_SMOKE, wx, wy, gauss() * 4f, gauss() * 4f, 1.6f, 4f, 9f)
        addNumber(wx, wy, amount, isPlayer)
        cue(DreadCue.SHELL_HIT, wx, wy, if (heavy) 1f else 0.5f)
        if (isPlayer) {
            cue(DreadCue.HIT_PLAYER, wx, wy, 1f)
            shake = max(shake, if (heavy) 0.7f else 0.25f)
        }
        if (s.hp > 0f && s.fireN < MAX_FIRES && rnd.nextFloat() < (if (heavy) 0.4f else 0.08f)) {
            val hw = s.cls.length * 0.5f
            val hb = s.cls.beam * 0.35f
            s.fireX[s.fireN] = lx.coerceIn(-hw * 0.8f, hw * 0.8f)
            s.fireY[s.fireN] = ly.coerceIn(-hb, hb)
            s.fireN++
        }
        if (s.hp <= 0f && s.alive) sink(s)
    }

    private fun sink(s: Ship) {
        s.alive = false
        s.sinkT = 0.001f
        s.orderSpeed = 0f
        val n = if (s.cls.length > 150f) 6 else 3
        for (k in 0 until n) {
            val lx = (rnd.nextFloat() - 0.5f) * s.cls.length * 0.8f
            val ly = (rnd.nextFloat() - 0.5f) * s.cls.beam * 0.6f
            val wx = s.x + cos(s.heading) * lx - sin(s.heading) * ly
            val wy = s.y + sin(s.heading) * lx + cos(s.heading) * ly
            emit(P_BLAST, wx, wy, 0f, 0f, 0.7f + rnd.nextFloat() * 0.4f, s.cls.beam * 0.5f, s.cls.beam * 0.9f)
            for (j in 0 until 4) {
                val a = rnd.nextFloat() * TWO_PI_F
                val v = 15f + rnd.nextFloat() * 40f
                emit(P_DEBRIS, wx, wy, cos(a) * v, sin(a) * v, 1.2f + rnd.nextFloat(), 1.8f, 0f)
            }
            emit(P_SMOKE, wx, wy, gauss() * 3f, gauss() * 3f, 3.2f, s.cls.beam * 0.5f, s.cls.beam * 1.1f)
        }
        cue(DreadCue.BOOM, s.x, s.y, 1f)
        shake = max(shake, if (s === player) 1f else min(0.8f, s.cls.length / 300f))
        if (s === player) {
            overT = 3f
            cue(DreadCue.GAME_OVER, s.x, s.y, 1f)
        } else {
            kills++
            score += s.cls.reward
            runCoins += s.cls.reward
            if (s.cls.boss) flagships++
        }
    }

    private fun addNumber(x: Float, y: Float, amount: Float, onPlayer: Boolean) {
        if (!numbersOn || amount < 1f) return
        val i = txNext
        txNext = (txNext + 1) % TX_MAX
        txAlive[i] = true
        txX[i] = x + (rnd.nextFloat() - 0.5f) * 14f
        txY[i] = y
        txLife[i] = 1f
        txColor[i] = if (onPlayer) 1 else 0
        txStr[i] = numberStrings[amount.toInt().coerceIn(0, 999)]
    }

    private fun updateTexts(dt: Float) {
        for (i in 0 until TX_MAX) {
            if (!txAlive[i]) continue
            txLife[i] -= dt * 0.9f
            txY[i] -= 22f * dt
            if (txLife[i] <= 0f) txAlive[i] = false
        }
    }

    private fun updateRepair(dt: Float) {
        if (repairCd > 0f) repairCd = max(0f, repairCd - dt)
        if (repairLeft > 0f && player.alive) {
            val h = min(repairLeft, repairRate * dt)
            player.hp = min(player.maxHp, player.hp + h)
            repairLeft -= h
        }
        if (player.alive && player.fireN > 0) {
            val burn = 0.9f * player.fireN * dt
            if (!god) player.hp -= burn
            if (player.hp <= 0f) sink(player)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // movement

    private fun moveShip(s: Ship, dt: Float) {
        val c = s.cls
        if (!s.alive) {
            s.speed *= (1f - min(1f, dt * 0.45f))
            s.angVel *= (1f - min(1f, dt * 0.8f))
        } else {
            val eng = if (s === player) engineMult() else 1f
            val dive = if (c.diver && s.depth > 0.5f) 0.7f else 1f
            val maxSp = c.maxSpeed * eng * dive
            val target = s.orderSpeed * maxSp
            val acc = c.accel * eng * dt
            s.speed += (target - s.speed).coerceIn(-acc * 1.6f, acc)
            val frac = (kotlin.math.abs(s.speed) / c.maxSpeed).coerceIn(0f, 1f)
            val lim = c.turnRate * eng * (0.35f + 0.65f * frac)
            val diff = wrapAngle(s.orderHeading - s.heading)
            val goal = (diff * 1.6f).coerceIn(-lim, lim)
            s.angVel += (goal - s.angVel) * min(1f, dt * 2.4f)
        }
        s.heading = wrapAngle(s.heading + s.angVel * dt)
        s.x += cos(s.heading) * s.speed * dt
        s.y += sin(s.heading) * s.speed * dt
        if (s.flash > 0f) s.flash = max(0f, s.flash - dt * 4f)
    }

    private fun separateShips() {
        val n = ships.size
        for (i in 0 until n) {
            val a = ships[i]
            if (!a.alive) continue
            for (j in i + 1 until n) {
                val b = ships[j]
                if (!b.alive) continue
                if (hypot(a.x - b.x, a.y - b.y) > (a.cls.length + b.cls.length) * 0.55f) continue
                val ra = a.cls.beam * 0.45f
                val rb = b.cls.beam * 0.45f
                var bestPen = 0f
                var nx = 0f
                var ny = 0f
                for (p in -1..1) {
                    val ax = a.x + cos(a.heading) * p * a.cls.length * 0.3f
                    val ay = a.y + sin(a.heading) * p * a.cls.length * 0.3f
                    for (q in -1..1) {
                        val bx = b.x + cos(b.heading) * q * b.cls.length * 0.3f
                        val by = b.y + sin(b.heading) * q * b.cls.length * 0.3f
                        val d = hypot(ax - bx, ay - by)
                        val pen = ra + rb - d
                        if (pen > bestPen && d > 0.01f) {
                            bestPen = pen
                            nx = (ax - bx) / d
                            ny = (ay - by) / d
                        }
                    }
                }
                if (bestPen > 0f) {
                    val ma = a.cls.length * a.cls.length
                    val mb = b.cls.length * b.cls.length
                    val wa = mb / (ma + mb)
                    val wb = 1f - wa
                    a.x += nx * bestPen * wa
                    a.y += ny * bestPen * wa
                    b.x -= nx * bestPen * wb
                    b.y -= ny * bestPen * wb
                }
            }
        }
    }

    private fun shipEffects(s: Ship, dt: Float) {
        val c = s.cls
        if (!s.alive) {
            s.sinkT += dt / (3f + c.length / 70f)
        }
        // wake
        s.wakeT -= dt
        if (s.wakeT <= 0f && (s.speed > 6f) && s.depth < 0.6f) {
            val frac = (s.speed / c.maxSpeed).coerceIn(0f, 1.2f)
            s.wakeT = 0.11f + (1f - min(1f, frac)) * 0.14f
            s.wakeSide = -s.wakeSide
            val back = c.length * 0.46f
            val ca = cos(s.heading)
            val sa = sin(s.heading)
            val px = s.x - ca * back
            val py = s.y - sa * back
            val off = c.beam * 0.3f * s.wakeSide
            emit(P_WAKE, px - sa * off, py + ca * off, -ca * 3f, -sa * 3f, 2.4f, c.beam * 0.45f, c.beam * 0.5f)
            if (s === player) emit(P_WAKE, px, py, -ca * 4f, -sa * 4f, 2.8f, c.beam * 0.6f, c.beam * 0.7f)
        }
        // fires: smoke trail and, for the player, heat
        if (s.fireN > 0) {
            s.fireT -= dt
            if (s.fireT <= 0f) {
                s.fireT = 0.24f
                val ca = cos(s.heading)
                val sa = sin(s.heading)
                for (k in 0 until s.fireN) {
                    val wx = s.x + ca * s.fireX[k] - sa * s.fireY[k]
                    val wy = s.y + sa * s.fireX[k] + ca * s.fireY[k]
                    emit(P_SMOKE, wx, wy, 5f + gauss() * 2f, -4f + gauss() * 2f, 2.4f, 3.5f, 9f)
                }
            }
        }
    }

    private fun reapSunk(dt: Float) {
        var i = ships.size - 1
        while (i >= 1) {
            if (!ships[i].alive && ships[i].sinkT >= 1f) {
                if (lock === ships[i]) lock = null
                ships.removeAt(i)
            }
            i--
        }
        if (!player.alive && phase != PHASE_OVER) {
            overT -= dt
            if (overT <= 0f) phase = PHASE_OVER
        }
    }

    // ---------------------------------------------------------------------------------------------
    // particles and sound

    fun emit(kind: Int, x: Float, y: Float, vx: Float, vy: Float, life: Float, size: Float, grow: Float) {
        val i = pNext
        pNext = (pNext + 1) % P_MAX
        pAlive[i] = true
        pKind[i] = kind
        pX[i] = x
        pY[i] = y
        pVx[i] = vx
        pVy[i] = vy
        pLife[i] = life
        pMax[i] = life
        pSize[i] = size
        pGrow[i] = grow
    }

    private fun updateParticles(dt: Float) {
        for (i in 0 until P_MAX) {
            if (!pAlive[i]) continue
            pLife[i] -= dt
            if (pLife[i] <= 0f) {
                pAlive[i] = false
                continue
            }
            pX[i] += pVx[i] * dt
            pY[i] += pVy[i] * dt
            pSize[i] += pGrow[i] * dt
            if (pKind[i] == P_SPARK || pKind[i] == P_DEBRIS) {
                pVx[i] *= (1f - dt * 1.5f)
                pVy[i] *= (1f - dt * 1.5f)
            }
        }
    }

    private fun cue(c: DreadCue, x: Float, y: Float, base: Float) {
        if (cueCount >= CUE_MAX) return
        val d = hypot(x - player.x, y - player.y)
        val v = base / (1f + d / 600f)
        if (v < 0.08f) return
        cueIds[cueCount] = c.ordinal
        cueVol[cueCount] = v
        cueCount++
    }

    // ---------------------------------------------------------------------------------------------
    // debug helpers (they go through the same code paths as play)

    fun debugKillAll() {
        queue.clear()
        for (i in ships.size - 1 downTo 1) {
            val s = ships[i]
            if (s.alive) hitShip(s, s.hp + 1f, 0f, 0f, s.x, s.y, true)
        }
    }

    fun debugHeal() {
        player.hp = player.maxHp
        player.fireN = 0
        repairCd = 0f
    }

    fun debugSpawnAhead(cls: ShipClass) {
        val a = player.heading + (rnd.nextFloat() - 0.5f) * 1.2f
        spawn(cls, player.x + cos(a) * 650f, player.y + sin(a) * 650f)
        if (phase == PHASE_BRIEF) phaseT = 0f
    }

    fun debugSkipWaves(n: Int) {
        queue.clear()
        for (i in ships.size - 1 downTo 1) ships.removeAt(i)
        beginWave(wave + n)
    }

    companion object {
        const val SH_MAX = 192
        const val TP_MAX = 32
        const val P_MAX = 720
        const val TX_MAX = 28
        const val CUE_MAX = 48
        const val MAX_ACTIVE = 10
    }
}
