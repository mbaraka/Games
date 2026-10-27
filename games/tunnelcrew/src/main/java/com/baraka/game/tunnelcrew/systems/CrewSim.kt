package com.baraka.game.tunnelcrew.systems

import com.baraka.game.tunnelcrew.model.Level
import com.baraka.game.tunnelcrew.model.Skill
import kotlin.math.abs

const val TICKS_PER_SEC = 30
const val MAX_CREW = 64
const val BODY_H = 8
const val MAX_STEP = 3
const val FALL_DEATH = 64
private const val FLOAT_AFTER = 6
private const val FUSE_TICKS = 150
private const val BRICKS_PER_BUILD = 12
private const val MAX_PARTICLES = 160
private const val POUR_REACH = 30
private const val POUR_DEPTH = 30
private const val POUR_CAP = 700
private const val POOL_MAX = (2 * POUR_REACH + 1) * (POUR_DEPTH + 1)
private val NB_X = intArrayOf(1, -1, 0, 0)
private val NB_Y = intArrayOf(0, 0, 1, -1)
private const val RATE_BASE = 120
private const val RATE_STEP = 5
private const val MIN_INTERVAL = 6

/** Activity of one crew member. Values from [GONE] up mean "no longer on the map". */
object Mode {
    const val WALK = 0
    const val FALL = 1
    const val FLOAT = 2
    const val CLIMB = 3
    const val BLOCK = 4
    const val BASH = 5
    const val MINE = 6
    const val DIG = 7
    const val BUILD = 8
    const val SHRUG = 9
    const val POUR = 10
    const val PLATFORM = 11
    const val STACK = 12
    const val GONE = 13
}

object Flag {
    const val CLIMBER = 1
    const val FLOATER = 2
}

/** Counters the UI drains after stepping to play sounds and vibrations. */
object Ev {
    const val SPLAT = 0
    const val BOOM = 1
    const val SAVE = 2
    const val CLINK = 3
    const val HATCH = 4
    const val FLAME = 5
    const val WARN = 6
    const val REFUSE = 7
    const val COUNT = 8
}

const val RESULT_RUNNING = 0
const val RESULT_WON = 1
const val RESULT_LOST = 2

/**
 * The whole simulation of one level, advanced in fixed [TICKS_PER_SEC] steps. Crew state lives in
 * parallel primitive arrays and particles in a fixed ring, so [step] never allocates.
 * Drawing lives in `render/CrewRenderer.kt`; this file only decides what happens.
 */
class CrewSim(val level: Level) {
    val terrain = Terrain(level.theme).also { level.build(it); it.flush() }

    val x = IntArray(MAX_CREW)
    val y = IntArray(MAX_CREW)
    val dir = IntArray(MAX_CREW)
    val mode = IntArray(MAX_CREW)
    val flags = IntArray(MAX_CREW)
    val bomb = IntArray(MAX_CREW)
    val anim = IntArray(MAX_CREW)
    private val timer = IntArray(MAX_CREW)
    private val fall = IntArray(MAX_CREW)
    private val aux = IntArray(MAX_CREW)

    val skills: IntArray = level.skills.copyOf()
    val events = IntArray(Ev.COUNT)

    var spawned = 0
        private set
    var saved = 0
        private set
    var dead = 0
        private set
    var tick = 0
        private set
    var timeLeft = level.timeSec * TICKS_PER_SEC
    var result = RESULT_RUNNING
        private set
    var nuked = false
        private set

    var god = false
    var freezeTime = false
    var infiniteSkills = false

    private var spawnLimit = minOf(level.total, MAX_CREW)
    private var spawnTimer = level.spawnEvery - 15

    /** Ticks between crew members leaving the hatch. The player may shorten it (faster release) but never go slower than the level's own rate. */
    var interval = level.spawnEvery
        private set
    val rate: Int get() = RATE_BASE - interval
    val minRate: Int get() = RATE_BASE - level.spawnEvery

    fun faster() { interval = (interval - RATE_STEP).coerceAtLeast(MIN_INTERVAL) }
    fun slower() { interval = (interval + RATE_STEP).coerceAtMost(level.spawnEvery) }

    private val blockX = IntArray(MAX_CREW)
    private val blockY = IntArray(MAX_CREW)
    private var blockCount = 0

    // Particles: a ring of fixed arrays.
    val px = FloatArray(MAX_PARTICLES)
    val py = FloatArray(MAX_PARTICLES)
    private val pvx = FloatArray(MAX_PARTICLES)
    private val pvy = FloatArray(MAX_PARTICLES)
    val plife = IntArray(MAX_PARTICLES)
    val pcolor = IntArray(MAX_PARTICLES)
    private var pnext = 0

    private var rng = 0x2545F491

    // Pourer: one pour at a time, cells filled bottom-up from a flood of the pit in front of the pourer.
    private val poolQueue = IntArray(POOL_MAX)
    private val poolOrder = IntArray(POOL_MAX)
    private val rowCount = IntArray(POUR_DEPTH + 2)
    private val visited = IntArray(WORLD_W * WORLD_H)
    private var stamp = 0
    private var poolN = 0
    private var poolPos = 0
    private var pourOwner = -1

    val alive: Int get() = spawned - saved - dead
    val totalCrew: Int get() = spawnLimit

    // region stepping

    fun step() {
        stepParticles()
        if (result != RESULT_RUNNING) return
        tick++
        if (!freezeTime) timeLeft--

        if (spawned < spawnLimit) {
            spawnTimer++
            if (spawnTimer >= interval) {
                spawnTimer = 0
                spawnOne()
            }
        }

        blockCount = 0
        for (i in 0 until spawned) {
            if (mode[i] == Mode.BLOCK) {
                blockX[blockCount] = x[i]
                blockY[blockCount] = y[i]
                blockCount++
            }
        }
        for (i in 0 until spawned) updateCrew(i)

        if (spawned >= spawnLimit && alive == 0) finish(saved >= level.needed)
        else if (timeLeft <= 0) finish(saved >= level.needed)
    }

    private fun finish(won: Boolean) {
        result = if (won) RESULT_WON else RESULT_LOST
    }

    private fun spawnOne() {
        val i = spawned++
        x[i] = level.spawnX
        y[i] = level.spawnY
        dir[i] = 1
        mode[i] = Mode.FALL
        flags[i] = 0
        bomb[i] = 0
        timer[i] = 0
        fall[i] = 0
        aux[i] = 0
        anim[i] = i * 3
        events[Ev.HATCH]++
    }

    private fun updateCrew(i: Int) {
        val m = mode[i]
        if (m >= Mode.GONE) return
        anim[i]++

        if (bomb[i] > 0) {
            bomb[i]--
            if (bomb[i] == 0) {
                explode(i)
                return
            }
        }

        val cx = x[i]
        val cy = y[i]
        if (cx < 1 || cx >= WORLD_W - 1 || cy >= WORLD_H) {
            kill(i, Ev.SPLAT)
            return
        }
        if (terrain.solid(cx, cy)) {
            // Buried by cement or bricks: rise with the new surface, or die if it is too deep.
            var up = 1
            while (up < 24 && terrain.solid(cx, cy - up)) up++
            if (up >= 24) {
                kill(i, Ev.SPLAT)
                return
            }
            y[i] = cy - up
            return
        }
        val hz = level.hazards
        for (h in hz.indices) {
            val r = hz[h]
            if (cx >= r[0] && cx < r[0] + r[2] && cy >= r[1] && cy < r[1] + r[3]) {
                kill(i, Ev.SPLAT)
                return
            }
        }
        val tr = level.traps
        for (k in tr.indices) {
            val r = tr[k]
            if (trapOn(r) && cx >= r[0] && cx < r[0] + r[2] && cy >= r[1] && cy < r[1] + r[3]) {
                kill(i, Ev.FLAME)
                return
            }
        }
        if (m != Mode.BLOCK) {
            val e = level.exit
            if (cx >= e[0] && cx < e[0] + e[2] && cy >= e[1] && cy <= e[1] + e[3]) {
                rescue(i)
                return
            }
        }

        when (m) {
            Mode.WALK -> walk(i)
            Mode.FALL, Mode.FLOAT -> fallStep(i)
            Mode.CLIMB -> climb(i)
            Mode.BLOCK -> if (!terrain.solid(cx, cy + 1)) startFall(i)
            Mode.BASH -> bash(i)
            Mode.MINE -> mine(i)
            Mode.DIG -> dig(i)
            Mode.BUILD -> build(i)
            Mode.SHRUG -> shrug(i)
            Mode.POUR -> pour(i)
            Mode.PLATFORM -> platform(i)
            Mode.STACK -> stack(i)
        }
    }

    /** Whether the timed trap `[x, y, w, h, period, onTicks, phase]` is currently firing. */
    fun trapOn(r: IntArray): Boolean = (tick + r[6]) % r[4] < r[5]

    private fun startFall(i: Int) {
        mode[i] = Mode.FALL
        fall[i] = 0
    }

    // endregion

    // region behaviours

    private fun walk(i: Int) {
        val t = terrain
        val cx = x[i]
        val cy = y[i]
        val d = dir[i]
        if (!t.solid(cx, cy + 1)) {
            startFall(i)
            fallStep(i)
            return
        }
        val nx = cx + d
        for (b in 0 until blockCount) {
            if (abs(blockY[b] - cy) <= 6 && (blockX[b] - cx) * d > 0 && abs(blockX[b] - nx) <= 4) {
                dir[i] = -d
                return
            }
        }
        var ny = Int.MIN_VALUE
        var k = cy - MAX_STEP
        while (k <= cy + MAX_STEP) {
            if (!t.solid(nx, k) && t.solid(nx, k + 1)) {
                ny = k
                break
            }
            k++
        }
        if (ny != Int.MIN_VALUE) {
            var h = 1
            while (h < BODY_H) {
                if (t.solid(nx, ny - h)) {
                    dir[i] = -d
                    return
                }
                h++
            }
            x[i] = nx
            y[i] = ny
            return
        }
        if (t.solid(nx, cy) || t.solid(nx, cy - MAX_STEP)) {
            if ((flags[i] and Flag.CLIMBER) != 0) mode[i] = Mode.CLIMB else dir[i] = -d
            return
        }
        x[i] = nx
        startFall(i)
    }

    private fun fallStep(i: Int) {
        val t = terrain
        val cx = x[i]
        var cy = y[i]
        var f = fall[i]
        if (mode[i] == Mode.FALL && (flags[i] and Flag.FLOATER) != 0 && f >= FLOAT_AFTER) mode[i] = Mode.FLOAT
        val speed = if (mode[i] == Mode.FLOAT) 1 else 3
        var s = 0
        while (s < speed && !t.solid(cx, cy + 1)) {
            cy++
            f++
            s++
        }
        y[i] = cy
        fall[i] = f
        if (t.solid(cx, cy + 1)) {
            if (mode[i] == Mode.FALL && f > FALL_DEATH) {
                kill(i, Ev.SPLAT)
            } else {
                mode[i] = Mode.WALK
                fall[i] = 0
            }
        }
    }

    private fun climb(i: Int) {
        val t = terrain
        val cx = x[i]
        val d = dir[i]
        val cy = y[i] - 1
        y[i] = cy
        if (t.solid(cx, cy - BODY_H + 1)) {
            dir[i] = -d
            x[i] = cx - d
            startFall(i)
            return
        }
        if (!t.solid(cx + d, cy)) {
            x[i] = cx + d
            if (t.solid(cx + d, cy + 1)) mode[i] = Mode.WALK else startFall(i)
        }
    }

    private fun bash(i: Int) {
        val t = terrain
        val cx = x[i]
        val cy = y[i]
        val d = dir[i]
        if (!t.solid(cx, cy + 1)) {
            startFall(i)
            return
        }
        timer[i]++
        if ((timer[i] and 1) != 0) return
        val x0 = if (d > 0) cx + 1 else cx - 2
        if (t.hasSteel(x0, cy - 8, 2, 9, d)) {
            events[Ev.CLINK]++
            mode[i] = Mode.WALK
            return
        }
        if (t.carveRect(x0, cy - 8, 2, 9, d) == 0) {
            aux[i]++
            if (aux[i] >= 6) {
                mode[i] = Mode.WALK
                return
            }
        } else {
            aux[i] = 0
        }
        x[i] = cx + d
    }

    private fun mine(i: Int) {
        val t = terrain
        val cx = x[i]
        val cy = y[i]
        val d = dir[i]
        if (!t.solid(cx, cy + 1)) {
            startFall(i)
            return
        }
        timer[i]++
        if (timer[i] % 4 != 0) return
        if (t.discHasSteel(cx + 2 * d, cy - 4, 5, d)) {
            events[Ev.CLINK]++
            mode[i] = Mode.WALK
            return
        }
        t.carveDisc(cx + d, cy - 3, 5, d)
        t.carveDisc(cx + 2 * d, cy - 4, 5, d)
        x[i] = cx + 2 * d
        y[i] = cy + 1
    }

    private fun dig(i: Int) {
        val t = terrain
        val cx = x[i]
        val cy = y[i]
        if (!t.solid(cx, cy + 1)) {
            startFall(i)
            return
        }
        timer[i]++
        if (timer[i] % 6 != 0) return
        if (t.hasSteel(cx - 4, cy + 1, 9, 2)) {
            events[Ev.CLINK]++
            mode[i] = Mode.WALK
            return
        }
        t.carveRect(cx - 4, cy + 1, 9, 2)
        y[i] = cy + 2
    }

    private fun build(i: Int) {
        val t = terrain
        val cx = x[i]
        val cy = y[i]
        val d = dir[i]
        if (!t.solid(cx, cy + 1)) {
            startFall(i)
            return
        }
        timer[i]++
        if (timer[i] % 6 != 0) return
        if (aux[i] <= 0) {
            mode[i] = Mode.SHRUG
            timer[i] = 0
            return
        }
        val nx = cx + 2 * d
        var h = 1
        while (h <= BODY_H) {
            if (t.solid(nx, cy - h) || (h <= 2 && t.solid(cx + d, cy - h))) {
                // Out of room: stop building and let normal walking decide (step up a low ledge, or turn at a real wall).
                mode[i] = Mode.WALK
                return
            }
            h++
        }
        for (k in 1..6) t.brick(cx + d * k, cy)
        if (aux[i] == 3) events[Ev.WARN]++
        x[i] = nx
        y[i] = cy - 1
        aux[i]--
    }

    private fun platform(i: Int) {
        val t = terrain
        val cx = x[i]
        val cy = y[i]
        val d = dir[i]
        if (!t.solid(cx, cy + 1)) {
            startFall(i)
            return
        }
        timer[i]++
        if (timer[i] % 6 != 0) return
        if (aux[i] <= 0) {
            mode[i] = Mode.SHRUG
            timer[i] = 0
            return
        }
        val nx = cx + 2 * d
        for (h in 0 until BODY_H) {
            if (t.solid(nx, cy - h)) {
                mode[i] = Mode.WALK
                return
            }
        }
        for (k in 1..6) t.brick(cx + d * k, cy + 1)
        if (aux[i] == 3) events[Ev.WARN]++
        x[i] = nx
        aux[i]--
    }

    private fun stack(i: Int) {
        val t = terrain
        val cx = x[i]
        val cy = y[i]
        val d = dir[i]
        if (!t.solid(cx, cy + 1)) {
            startFall(i)
            return
        }
        timer[i]++
        if (timer[i] % 5 != 0) return
        if (aux[i] <= 0) {
            mode[i] = Mode.SHRUG
            timer[i] = 0
            return
        }
        val nx = cx + d
        for (h in 1..BODY_H) {
            if (t.solid(nx, cy - h)) {
                mode[i] = Mode.WALK
                return
            }
        }
        t.brick(cx + d, cy)
        t.brick(cx + 2 * d, cy)
        if (aux[i] == 3) events[Ev.WARN]++
        x[i] = nx
        y[i] = cy - 1
        aux[i]--
    }

    /** Floods the pit in front of pourer [i]; false if there is no pit or it leaks (no floor, too wide or too deep). */
    private fun startPour(i: Int): Boolean {
        val t = terrain
        val top = y[i] + 1
        var sx = x[i]
        var reach = 0
        do {
            sx += dir[i]
            reach++
        } while (reach <= 12 && t.solid(sx, top))
        if (t.solid(sx, top)) return false
        val lo = x[i] - POUR_REACH
        val hi = x[i] + POUR_REACH
        val bottom = top + POUR_DEPTH
        stamp++
        var head = 0
        var tail = 0
        poolQueue[tail++] = top * WORLD_W + sx
        visited[top * WORLD_W + sx] = stamp
        while (head < tail) {
            val c = poolQueue[head++]
            val cy = c / WORLD_W
            val cx = c - cy * WORLD_W
            if (cx <= lo || cx >= hi || cy >= bottom || cx <= 0 || cx >= WORLD_W - 1 || cy >= WORLD_H - 1) return false
            for (k in 0 until 4) {
                val nx = cx + NB_X[k]
                val ny = cy + NB_Y[k]
                if (ny < top || t.solid(nx, ny)) continue
                val nc = ny * WORLD_W + nx
                if (visited[nc] == stamp) continue
                if (tail >= POOL_MAX) return false
                visited[nc] = stamp
                poolQueue[tail++] = nc
            }
        }
        java.util.Arrays.fill(rowCount, 0)
        for (n in 0 until tail) rowCount[poolQueue[n] / WORLD_W - top]++
        var pos = 0
        for (r in POUR_DEPTH downTo 0) {
            val c = rowCount[r]
            rowCount[r] = pos
            pos += c
        }
        for (n in 0 until tail) {
            val r = poolQueue[n] / WORLD_W - top
            poolOrder[rowCount[r]++] = poolQueue[n]
        }
        poolN = minOf(tail, POUR_CAP)
        poolPos = 0
        pourOwner = i
        return true
    }

    private fun pour(i: Int) {
        if (!terrain.solid(x[i], y[i] + 1)) {
            if (pourOwner == i) pourOwner = -1
            startFall(i)
            return
        }
        var k = 0
        while (k < 6 && poolPos < poolN) {
            val c = poolOrder[poolPos++]
            terrain.cement(c % WORLD_W, c / WORLD_W)
            k++
        }
        if (poolPos >= poolN) {
            if (pourOwner == i) pourOwner = -1
            mode[i] = Mode.SHRUG
            timer[i] = 0
        }
    }

    private fun shrug(i: Int) {
        if (!terrain.solid(x[i], y[i] + 1)) {
            startFall(i)
            return
        }
        timer[i]++
        if (timer[i] >= 20) mode[i] = Mode.WALK
    }

    // endregion

    // region outcomes

    private fun kill(i: Int, ev: Int) {
        if (pourOwner == i) pourOwner = -1
        if (god) {
            x[i] = level.spawnX
            y[i] = level.spawnY
            bomb[i] = 0
            startFall(i)
            return
        }
        burst(x[i].toFloat(), (y[i] - 3).toFloat(), 0xFFE53935.toInt(), 14)
        mode[i] = Mode.GONE
        dead++
        events[ev]++
    }

    private fun explode(i: Int) {
        if (pourOwner == i && !god) pourOwner = -1
        val cx = x[i]
        val cy = y[i] - 4
        terrain.carveDisc(cx, cy, 13)
        burst(cx.toFloat(), cy.toFloat(), 0xFFFFB300.toInt(), 28)
        burst(cx.toFloat(), cy.toFloat(), 0xFFE53935.toInt(), 14)
        events[Ev.BOOM]++
        if (god) {
            bomb[i] = 0
            startFall(i)
            return
        }
        mode[i] = Mode.GONE
        dead++
    }

    private fun rescue(i: Int) {
        if (pourOwner == i) pourOwner = -1
        burst(x[i].toFloat(), (y[i] - 4).toFloat(), 0xFF69F0AE.toInt(), 10)
        mode[i] = Mode.GONE
        saved++
        events[Ev.SAVE]++
    }

    // endregion

    // region player actions

    fun canAssign(i: Int, skill: Skill): Boolean {
        if (i < 0 || i >= spawned || result != RESULT_RUNNING) return false
        if (!infiniteSkills && skills[skill.ordinal] <= 0) return false
        val m = mode[i]
        if (m >= Mode.GONE) return false
        return when (skill) {
            Skill.CLIMBER -> (flags[i] and Flag.CLIMBER) == 0
            Skill.FLOATER -> (flags[i] and Flag.FLOATER) == 0
            Skill.BOMBER -> bomb[i] == 0
            Skill.BUILDER -> m == Mode.WALK || m == Mode.SHRUG
            Skill.PLATFORMER, Skill.STACKER -> m == Mode.WALK || m == Mode.SHRUG
            Skill.FILLER -> m == Mode.WALK && pourOwner == -1
            Skill.BLOCKER, Skill.BASHER, Skill.MINER, Skill.DIGGER -> m == Mode.WALK
        }
    }

    /** Gives [skill] to crew member [i]; returns false (and changes nothing) if that is not allowed right now. */
    fun assign(i: Int, skill: Skill): Boolean {
        if (!canAssign(i, skill)) return false
        when (skill) {
            Skill.CLIMBER -> flags[i] = flags[i] or Flag.CLIMBER
            Skill.FLOATER -> flags[i] = flags[i] or Flag.FLOATER
            Skill.BOMBER -> bomb[i] = FUSE_TICKS
            Skill.BLOCKER -> begin(i, Mode.BLOCK)
            Skill.BUILDER -> {
                begin(i, Mode.BUILD)
                aux[i] = BRICKS_PER_BUILD
            }
            Skill.BASHER -> begin(i, Mode.BASH)
            Skill.MINER -> begin(i, Mode.MINE)
            Skill.DIGGER -> begin(i, Mode.DIG)
            Skill.PLATFORMER -> {
                begin(i, Mode.PLATFORM)
                aux[i] = BRICKS_PER_BUILD
            }
            Skill.STACKER -> {
                begin(i, Mode.STACK)
                aux[i] = 16
            }
            Skill.FILLER -> {
                begin(i, Mode.POUR)
                if (!startPour(i)) {
                    mode[i] = Mode.WALK
                    events[Ev.REFUSE]++
                    return false
                }
            }
        }
        if (!infiniteSkills) skills[skill.ordinal]--
        return true
    }

    private fun begin(i: Int, newMode: Int) {
        mode[i] = newMode
        timer[i] = 0
        aux[i] = 0
    }

    /** Index of the nearest crew member within [radius] world px of the point that can take [skill], or -1. */
    fun pick(wx: Float, wy: Float, radius: Float, skill: Skill): Int {
        var best = -1
        var bestD = radius * radius
        for (i in 0 until spawned) {
            if (mode[i] >= Mode.GONE) continue
            val dx = x[i] - wx
            val dy = (y[i] - 4) - wy
            val d2 = dx * dx + dy * dy
            if (d2 <= bestD && canAssign(i, skill)) {
                best = i
                bestD = d2
            }
        }
        return best
    }

    /** "Armageddon": stop the hatch and give everyone still walking a short fuse. */
    fun nuke() {
        if (nuked || result != RESULT_RUNNING) return
        nuked = true
        spawnLimit = spawned
        for (i in 0 until spawned) {
            if (mode[i] < Mode.GONE && bomb[i] == 0) bomb[i] = 30 + (rand() % 60)
        }
    }

    // endregion

    // region debug hooks (go through the same state the game uses)

    fun addSkills(n: Int) {
        for (s in skills.indices) skills[s] += n
    }

    /** Spawns the whole remaining crew immediately. */
    fun releaseAll() {
        while (spawned < spawnLimit) spawnOne()
    }

    /** Ends the level as if everyone had been saved (or nobody). */
    fun forceFinish(win: Boolean) {
        if (result != RESULT_RUNNING) return
        releaseAll()
        for (i in 0 until spawned) {
            if (mode[i] < Mode.GONE) {
                mode[i] = Mode.GONE
                if (win) saved++ else dead++
            }
        }
        finish(saved >= level.needed)
    }

    // endregion

    // region particles

    private fun rand(): Int {
        var v = rng
        v = v xor (v shl 13)
        v = v xor (v ushr 17)
        v = v xor (v shl 5)
        rng = v
        return v and 0x7fffffff
    }

    private fun burst(cx: Float, cy: Float, color: Int, n: Int) {
        for (k in 0 until n) {
            val p = pnext
            pnext = (pnext + 1) % MAX_PARTICLES
            px[p] = cx
            py[p] = cy
            pvx[p] = ((rand() % 200) - 100) / 60f
            pvy[p] = -((rand() % 140) / 50f) - 0.4f
            plife[p] = 14 + rand() % 14
            pcolor[p] = color
        }
    }

    private fun stepParticles() {
        for (p in 0 until MAX_PARTICLES) {
            if (plife[p] <= 0) continue
            plife[p]--
            px[p] += pvx[p]
            py[p] += pvy[p]
            pvy[p] += 0.18f
        }
    }

    // endregion
}
