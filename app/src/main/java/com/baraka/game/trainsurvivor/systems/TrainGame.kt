package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.data.TrainSoundCue
import com.baraka.game.trainsurvivor.model.AUTO_WEAPON
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.DangerLevel
import com.baraka.game.trainsurvivor.model.EliteTrait
import com.baraka.game.trainsurvivor.model.HazardKind
import com.baraka.game.trainsurvivor.model.Perk
import com.baraka.game.trainsurvivor.model.SURVIVORS
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.TrainShape
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
/** In a shared world the second train's lane is drawn this many logical units above the first (see SharedRun). */
const val LANE_DY = 170f
const val LOCO_W = 190f
const val CAR_W = 150f
const val CAR_GAP = 8f
const val COWCATCHER = 34f
const val WORLD_SPEED = 170f
private const val MAX_ATTACKERS_PER_SIDE = 3
internal const val MAX_ZOMBIES = 70


private const val MAX_PARTICLES = 420
internal const val MAX_TEXTS = 40
private const val SHELL_GRAVITY = 1000f

private const val HEAT_COOL_RATE = 0.22f
private const val HEAT_RECOVER = 0.35f

enum class ParticleKind { BLOOD, SPARK, SMOKE, COIN, FIRE, DEBRIS, HEAL }

class Zombie(val type: ZombieType, var x: Float, var y: Float, val side: Int, var maxHp: Float, val dmgScale: Float, val seed: Float) {
    var hp = maxHp
    var anim = seed * 6f
    var flash = 0f
    var attacking = false
    var dead = false
    val isBoss get() = type.boss
    val flying get() = type.flying
    /** Which way the zombie looks (+1 right); ground zombies face the train, flyers face their target. */
    var facing = -side
    var targetX = x
    var cruiseY = y
    var retarget = 0f
    /** Which train's lane this zombie belongs to: 0 = the lead train, 1 = the second train of a shared world. */
    var lane = 0
    var hopTimer = 3f
    /** Countdown to this boss's next special move. */
    var special = 4f
    /** Separate timers so a boss can lunge, shoot and use its special at the same time. */
    var lungeTimer = 3f
    var shootTimer = 2f
    // Damage is shown as one floating number every quarter second instead of one per bullet.
    var pendingDamage = 0f
    var numberTimer = 0f
    var pendingCrit = false
    /** Elite traits (a second one appears on Inferno), the remaining shield, and movement/regen state. */
    var elite: EliteTrait? = null
    var elite2: EliteTrait? = null
    var shield = 0f
    var speedMul = 1f
    var sinceHit = 0f
    fun has(trait: EliteTrait) = elite == trait || elite2 == trait
    // Leaping or dropping onto a roof: 0 = on the ground, 1 = in the air, 2 = standing on a car roof.
    var jumpState = 0
    var vx = 0f
    var vy = 0f
    var landY = 0f
    var roofSlot = 0
    /** Shared worlds: the id the partner's mirror knows this zombie by, and where the mirror is easing it to. */
    var netId = 0
    var netX = 0f
    var netY = 0f
    var netStamp = 0
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
) {
    var lane = 0
}

class Tracer(val x1: Float, val y1: Float, val x2: Float, val y2: Float, var life: Float, val width: Float, val color: Int, val jagged: Boolean = false) {
    var lane = 0
}

/** A fork in the track: the player has until [timeLeft] runs out to pick; [chosen] is set once they (or the timeout) do. */
class Junction(val optionA: Biome, val optionB: Biome, var timeLeft: Float) {
    var chosen: Biome? = null
}

class FloatingText(var x: Float, var y: Float, val text: String, var life: Float, val color: Int, val size: Float = 22f) {
    var lane = 0
}

/** A supply crate drifting down on a parachute; tap it for a reward before it vanishes. */
class Crate(var x: Float, var y: Float) {
    var landed = false
    var life = 8f
    var phase = 0f
}

/** A mortar shell in flight. */
class Shell(var x: Float, var y: Float, var vx: Float, var vy: Float, val tx: Float, val ty: Float, val total: Float, val gravity: Float) {
    var age = 0f
}

/** Per-passenger runtime state the renderer needs (aim, recoil, muzzle flash) plus their current weapon. */
class GunnerState {
    /** The weapon in hand right now. */
    var weapon = WeaponType.PISTOL
    /** [AUTO_WEAPON] or the ordinal of the weapon the player forced. */
    var mode = AUTO_WEAPON
    var autoCheck = 0f
    var cooldown = 0f
    var recoil = 0f
    var flash = 0f
    var aim = -0.15f
    var facing = 1
    /** False once the passenger's wagon has been destroyed. */
    var alive = true
    /** Heat per weapon (0..1): rapid-fire weapons overheat and lock out until they cool. */
    val heat = FloatArray(WeaponType.entries.size)
    val overheated = BooleanArray(WeaponType.entries.size)
    val isHot get() = overheated[weapon.ordinal]
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
 *
 * Performance rules for this file: no allocation in per-frame loops (index loops, reusable scratch
 * lists, no `filter`/`sortedBy`/`toList` on hot paths); lists are capped (zombies, particles, texts).
 */
class TrainGame(
    private val save: SaveData,
    private val spawnEnabled: Boolean = true,
    internal val rng: Random = Random(System.nanoTime()),
    /** Set for the second train of a shared world: it shares the lead's zombies and effects, and follows its route and clock. */
    internal val lead: TrainGame? = null
) {

    var onSound: ((TrainSoundCue) -> Unit)? = null
    var worldW = 1280f

    /** Effects recorded for a partner's phone while a shared world is streamed (see FxLog). */
    val fx: FxLog = lead?.fx ?: FxLog()
    /** Set on a guest's mirror train: its buttons ask the host to act instead of acting here. */
    var remote: RemoteControl? = null

    // ---- shared world: two trains in one simulation (see SharedRun)
    /** 0 for a normal train and the lead train of a shared world; 1 for the second train. */
    var lane: Int = if (lead == null) 0 else 1
        internal set
    /** True when this train shares a world with another (see SharedRun). */
    val isShared: Boolean get() = lead != null || ally != null
    /** Seconds until the trains swap tracks (shown on both screens); set by the TrackSwitch hazard. */
    var switchWarning = 0f
    /** The second train sharing this world (set on the lead). */
    var ally: TrainGame? = null
    /** The other train of a shared world, whichever this is. */
    val buddy: TrainGame? get() = lead ?: ally
    /** Which lane effects created right now belong to (kills in the other lane draw their effects there). */
    internal var fxLane = lane
    internal val zombieCap get() = if (lead != null || ally != null) MAX_ZOMBIES * 23 / 10 else MAX_ZOMBIES

    // ---- options / debug switches ----
    var damageNumbers = true
    var shakeEnabled = true
    var godMode = false

    // ---- train ----
    /** The body chosen in the Garage: all car sizes and positions below come from it. */
    val shape: TrainShape = save.selectedShape
    /** The paint chosen in the Garage; each livery carries a small bonus (see Livery). */
    private val paint = save.selectedLivery
    val slotCount = save.unlockedSlots
    val crew: MutableList<CrewSlot> = save.slots.take(slotCount).toMutableList().also { it.add(CrewSlot()) }
    val gunners = List(slotCount + 1) { i ->
        GunnerState().also { g ->
            if (i < slotCount) {
                val slot = save.slots[i]
                g.mode = slot.selected
                g.weapon = if (slot.selected != AUTO_WEAPON) WeaponType.entries[slot.selected] else slot.bestWeapon
            } else {
                g.alive = false // the guest seat stays empty until a partner lends a passenger
            }
        }
    }

    // ---- co-op (see the coop package) ----
    var coopRole = CoopRole.SOLO
    /** Spawn-rate multiplier: two trains mean double the zombies. */
    var coopSpawnMult = 1f
    var onCoop: ((CoopSignal) -> Unit)? = null
    /** Boss damage (as a fraction of boss health) dealt here since the session last collected it; forwarded so both trains hurt the same boss. */
    var bossDamageOut = 0f
    private var applyingRemote = false
    val gangwayLevel = save.level(TrainUpgrade.GANGWAY)
    val linkLevel = save.level(TrainUpgrade.LINK)
    val towLevel = save.level(TrainUpgrade.TOW)
    /** Seconds each of this train's passengers is still away on the partner's train (0 = at home). */
    val seatAwayTime = FloatArray(slotCount)
    internal var loanSeat = -1
    var outgoingLoanCooldown = 0f
    /** The partner's passenger riding this train: seconds left, and which survivor it is. */
    var guestTime = 0f
    var guestSurvivor = 0
    val guestPresent get() = guestTime > 0f
    val loanDuration get() = 15f + 5f * gangwayLevel
    /** Which survivor stands in [seat]: its own, or the lent guest for the extra seat. */
    fun survivorOf(seat: Int): Int = if (seat >= slotCount) guestSurvivor else seat
    val armorLevel = save.level(TrainUpgrade.ARMOR)
    val spikeLevel = save.level(TrainUpgrade.SPIKES)
    val dynamiteLevel = save.level(TrainUpgrade.DYNAMITE)
    val flakLevel = save.level(TrainUpgrade.FLAK)
    val teslaLevel = save.level(TrainUpgrade.TESLA)
    val flameLevel = save.level(TrainUpgrade.FLAME)
    val engineLevel = save.level(TrainUpgrade.ENGINE)
    val mortarLevel = save.level(TrainUpgrade.MORTAR)
    val overdriveLevel = save.level(TrainUpgrade.OVERDRIVE)
    val fieldRepairLevel = save.level(TrainUpgrade.FIELD_REPAIR)
    val repairLevel = save.level(TrainUpgrade.REPAIR)
    val repairers: List<Repairer> =
        if (repairLevel > 0) List(((repairLevel + 1) / 2).coerceIn(1, 4)) { i -> Repairer(0f, if (i % 2 == 0) 1 else -1).also { it.timer = 0.4f + i * 0.7f } } else emptyList()

    /** The locomotive's health is "the train": the run ends when it reaches 0. Wagons have their own health below. */
    val maxHp = (100f + 40f * armorLevel) * (1f + 0.4f * crew[0].armor) * shape.hpMult * paint.hpMult
    var hp = maxHp
    val carMaxHp = FloatArray(slotCount) { i -> (70f + 25f * armorLevel) * (1f + 0.4f * crew[i].armor) * paint.hpMult }
    val carHp = carMaxHp.copyOf()
    private val regenPerSecond = 1.5f * repairLevel * paint.repairMult
    private val thornsPerSecond = 7f * spikeLevel
    internal val coinMultiplier = (1f + 0.12f * save.level(TrainUpgrade.BOUNTY)) * save.prestigeMultiplier * paint.coinMult

    /** The difficulty chosen in the Garage for this run. */
    val danger: DangerLevel = save.selectedDanger

    // Passive perks of the named survivors, resolved once per run.
    // Index slotCount is the guest seat (the partner's lent passenger), so these are one longer than the crew.
    private val damagePerk = FloatArray(slotCount + 1) { if (perkOf(it) == Perk.DAMAGE) 1.15f else 1f }
    private val ratePerk = FloatArray(slotCount + 1) { if (perkOf(it) == Perk.FIRE_RATE) 1.2f else 1f }
    private val rangePerk = FloatArray(slotCount + 1) { if (perkOf(it) == Perk.RANGE) 1.25f else 1f }
    private val critChance = FloatArray(slotCount + 1) { if (perkOf(it) == Perk.CRIT) 0.12f else 0f }
    private val coinPerk = FloatArray(slotCount + 1) { if (perkOf(it) == Perk.SCAVENGER) 1.25f else 1f }

    private fun perkOf(seat: Int): Perk? = SURVIVORS.getOrNull(seat)?.perk

    val trainLength = shape.locoW + (slotCount - 1) * (shape.carW + shape.carGap)
    val trainLeft get() = worldW / 2f - trainLength / 2f
    val locoLeft get() = trainLeft + trainLength - shape.locoW
    val frontEnd get() = trainLeft + trainLength + shape.nose
    val rearEnd get() = trainLeft

    // ---- run state ----
    var time = 0f
    var scroll = 0f
    var wheelAngle = 0f
    var distance = 0f
    var kills = 0
    var flyerKills = 0
    var bossKills = 0
    var routesTaken = 0
    var runCoins = 0
    var over = false
    var shake = 0f
    var hitFlash = 0f
    var bossAlert = 0f
    var bossName = ""
    var boss: Zombie? = null

    // ---- active abilities ----
    var dynamiteCooldown = 0f
    val dynamiteCooldownMax = (16f - 1.5f * dynamiteLevel) * paint.cooldownMult
    val dynamiteDamage = 90f + 70f * dynamiteLevel
    val dynamiteReady get() = dynamiteCooldown <= 0f
    var overdriveTime = 0f
    var overdriveCooldown = 0f
    val overdriveCooldownMax = (28f - 2f * overdriveLevel) * paint.cooldownMult
    val overdriveDuration = 5f + 0.6f * overdriveLevel
    val overdriveReady get() = overdriveLevel > 0 && overdriveCooldown <= 0f && overdriveTime <= 0f
    var fieldRepairCooldown = 0f
    val fieldRepairCooldownMax = (32f - 2.5f * fieldRepairLevel) * paint.cooldownMult
    val fieldRepairReady get() = fieldRepairLevel > 0 && fieldRepairCooldown <= 0f

    // ---- train-mounted weapons ----
    var flakCooldown = 0f
    var flakFlash = 0f
    var teslaCooldown = 1.5f
    var teslaFlash = 0f
    var flameFlash = 0f
    var flameCooldown = 0f
    val flameReach = 140f + 14f * flameLevel
    var mortarCooldown = 2f
    private var healTextGap = 0f

    // ---- events ----
    var crate: Crate? = null
    var hordeWarning = 0f
    var hordeTime = 0f
    internal var hordeSide = 1
    private var nextHordeAt = 55f
    private var nextCrateAt = 32f
    internal var buffTime = 0f
    val buffActive get() = buffTime > 0f

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

    /** While the train changes route it briefly morphs: [kitFrom]'s gear fades out as the new route's gear fades in. */
    var kitFrom = Biome.PLAINS
    var kitBlend = 1f
    val livery = save.selectedLivery

    // ---- entities (index-iterated, capped) ----
    val zombies: ArrayList<Zombie> = lead?.zombies ?: ArrayList(80)
    /** Zombies sorted back-to-front by feet position; rebuilt at the end of every update for the renderer. */
    val drawOrder: ArrayList<Zombie> = lead?.drawOrder ?: ArrayList(80)
    val particles: ArrayList<Particle> = lead?.particles ?: ArrayList(MAX_PARTICLES)
    val tracers: ArrayList<Tracer> = lead?.tracers ?: ArrayList(32)
    val texts: ArrayList<FloatingText> = lead?.texts ?: ArrayList(MAX_TEXTS)
    val shells = ArrayList<Shell>(8)

    private var spawnAccumulator = 0f
    private var smokeAccumulator = 0f
    internal var nextBossAt = 75f
    internal var bossNumber = 0
    private var hitSoundGap = 0f
    private val sideBuffer = ArrayList<Zombie>(40)
    private val scratch = ArrayList<Zombie>(16)
    private val struck = ArrayList<Zombie>(8)
    internal var shooter = -1

    /** Coins banked when the run ends: kill coins plus 1 per 25 m travelled. */
    val finalCoins get() = runCoins + (distance / 25f * save.prestigeMultiplier * danger.coinMult).toInt()
    val distanceMeters get() = distance.roundToInt()

    /**
     * How fast the train is running: 1.0 at the start, rising to 2.6 by 2,900 m, then multiplied by the train's body and
     * Engine Tuning (capped at 4x). Faster means more distance and coins per second, more zombies and faster-closing zombies.
     */
    val speedFactor get() = ((1f + min(1.6f, distance / 1800f)) * shape.speedMult * paint.speedMult * (1f + 0.08f * engineLevel)).coerceAtMost(4f) * (if (lockedBy == HazardKind.GATE_LOCK || lockedBy == HazardKind.COLLAPSE) 0.35f else 1f)

    /** Zombies close in a little faster as the train speeds up. */
    private val zombiePace get() = 1f + 0.25f * (speedFactor - 1f)

    /** 0 = deep night, 1 = bright noon; also exposes the cycle position for sun/moon placement. */
    val dayCycle get() = ((time + 17f) % 170f) / 170f

    // ---- geometry helpers shared with the renderer ----
    /** Left edge of the wagon in [slot] (slot 0 is the locomotive; use [locoLeft] for it). */
    fun carLeft(slot: Int): Float = locoLeft - slot * (shape.carW + shape.carGap)

    fun gunX(slot: Int): Float = when {
        slot >= slotCount -> locoLeft + shape.locoW - 24f // the guest mount, on the front of the locomotive
        slot == 0 -> locoLeft + shape.locoGunX
        else -> carLeft(slot) + shape.carW / 2f
    }

    fun gunY(slot: Int): Float = when {
        slot >= slotCount -> 422f
        slot == 0 -> shape.locoRoofY
        else -> 420f
    }
    fun shoulderY(slot: Int): Float = gunY(slot) - 30f
    fun endX(side: Int): Float = if (side > 0) frontEnd else rearEnd
    fun carCenterX(slot: Int): Float = if (slot == 0) locoLeft + shape.locoW / 2f else gunX(slot)
    fun carAlive(slot: Int): Boolean = slot == 0 || slot >= slotCount || carHp[slot] > 0f
    val repairBoardY = 477f

    /** Flak turret sits on the rear car's roof; Tesla coil on the cab roof; mortar between the cars; flame nozzle on the cowcatcher. */
    val flakX get() = if (slotCount > 1) trainLeft + 24f else locoLeft + 56f
    val flakY get() = if (slotCount > 1) 418f else shape.locoRoofY
    val teslaX get() = locoLeft + shape.locoGunX + 24f
    val teslaY get() = shape.locoRoofY
    val mortarX get() = if (slotCount > 1) locoLeft - 22f else locoLeft + 30f
    val mortarY get() = if (slotCount > 1) 418f else shape.locoRoofY
    val flameX get() = frontEnd - 14f
    val flameY get() = 486f

    internal fun slotAt(x: Float): Int {
        if (slotCount == 1 || x >= locoLeft) return 0
        return (((locoLeft - x) / (shape.carW + shape.carGap)).toInt() + 1).coerceIn(1, slotCount - 1)
    }

    fun update(dt: Float) {
        val leader = lead
        if (leader != null) syncWorld(leader) else time += dt
        val speed = WORLD_SPEED * speedFactor
        if (leader == null) scroll += speed * dt
        wheelAngle += speed * dt / 17f
        shake = max(0f, shake - dt * 2.2f)
        hitFlash = max(0f, hitFlash - dt * 3f)
        bossAlert = max(0f, bossAlert - dt)
        biomeBanner = max(0f, biomeBanner - dt)
        hordeWarning = max(0f, hordeWarning - dt)
        hitSoundGap -= dt
        kitBlend = min(1f, kitBlend + dt / 1.4f)

        if (!over) {
            distance += 14f * speedFactor * dt
            dynamiteCooldown = max(0f, dynamiteCooldown - dt)
            overdriveCooldown = max(0f, overdriveCooldown - dt)
            fieldRepairCooldown = max(0f, fieldRepairCooldown - dt)
            overdriveTime = max(0f, overdriveTime - dt)
            tickLoans(dt)
            buffTime = max(0f, buffTime - dt)
            regenerate(dt)
            if (spawnEnabled) {
                if (lead == null) {
                    updateRoute(dt)
                    updateEvents(dt)
                }
                updateHazards(dt)
                spawn(dt)
            }
            updateZombies(dt)
            updateGunners(dt)
            updateTrainWeapons(dt)
            updateShells(dt)
            updateRepairers(dt)
            if (hp <= 0f) endRun()
        } else {
            for (i in gunners.indices) gunners[i].flash = 0f
        }
        emitSmoke(dt)
        if (lead == null) updateEffects(dt)
        compactZombies()
        rebuildDrawOrder()
    }

    /** The second train of a shared world follows the lead's clock, scenery, route, crate and horde. */
    internal fun syncWorld(w: TrainGame) {
        time = w.time
        scroll = w.scroll
        worldW = w.worldW
        biome = w.biome
        fromBiome = w.fromBiome
        nextBiome = w.nextBiome
        junction = w.junction
        transitioning = w.transitioning
        boundaryX = w.boundaryX
        biomeBanner = w.biomeBanner
        kitFrom = w.kitFrom
        kitBlend = w.kitBlend
        crate = w.crate
        hordeTime = w.hordeTime
        hordeWarning = w.hordeWarning
        hordeSide = w.hordeSide
        damageNumbers = w.damageNumbers
        shakeEnabled = w.shakeEnabled
    }

    private var netCounter = 0

    /** The next zombie id for the shared-world snapshot (one counter per world, kept by the lead). */
    internal fun nextNetId(): Int {
        val w = lead ?: this
        return ++w.netCounter
    }

    /** Pairs this train with [other] as the two trains of one shared world (this one leads). */
    fun attachAlly(other: TrainGame) {
        ally = other
    }

    /** The train that owns [z]'s lane. */
    private fun rigOf(z: Zombie): TrainGame = if (z.lane == lane) this else buddy ?: this

    // ---------------------------------------------------------------- co-op: lending passengers and shared boss

    private fun tickLoans(dt: Float) {
        outgoingLoanCooldown = max(0f, outgoingLoanCooldown - dt)
        repairSendCooldown = max(0f, repairSendCooldown - dt)
        coinGiftCooldown = max(0f, coinGiftCooldown - dt)
        tickLocks(dt)
        ghost.update(dt)
        towCooldown = max(0f, towCooldown - dt)
        tickRescue(dt)
        for (i in seatAwayTime.indices) {
            if (seatAwayTime[i] > 0f) {
                seatAwayTime[i] -= dt
                if (seatAwayTime[i] <= 0f) {
                    seatAwayTime[i] = 0f
                    if (i == loanSeat) loanSeat = -1
                    floatText(gunX(i), gunY(i) - 40f, SURVIVORS[i].name + " is back", 1.2f, 0xFF66FF8A.toInt(), 18f)
                }
            }
        }
        if (guestTime > 0f) {
            guestTime -= dt
            if (guestTime <= 0f) {
                guestTime = 0f
                gunners[slotCount].alive = false
            }
        }
    }

    /** True when the Gangway is built, nobody of ours is already away, the wait is over and [seat] is alive and at home. */
    fun canSendPassenger(seat: Int): Boolean =
        coopRole != CoopRole.SOLO && gangwayLevel > 0 && !over && loanSeat < 0 && outgoingLoanCooldown <= 0f &&
            seat in 0 until slotCount && carAlive(seat) && seatAwayTime[seat] <= 0f

    /** Sends the passenger in [seat] across to the partner's train for [loanDuration] seconds. */
    fun sendPassenger(seat: Int): Boolean {
        remote?.let { if (canSendPassenger(seat)) { it.act(GuestAction.SEND_PASSENGER, seat); return true }; return false }
        if (!canSendPassenger(seat)) return false
        seatAwayTime[seat] = loanDuration
        loanSeat = seat
        outgoingLoanCooldown = loanDuration + (24f - 4f * gangwayLevel)
        floatText(gunX(seat), gunY(seat) - 40f, SURVIVORS[seat].name + " crosses over", 1.4f, 0xFF40E0FF.toInt(), 18f)
        onCoop?.invoke(CoopSignal.PassengerSent(seat, crew[seat], loanDuration))
        return true
    }

    /** The partner lent us a passenger: they take the guest mount for [seconds]. */
    fun receivePassenger(survivor: Int, slot: CrewSlot, seconds: Float) {
        val seat = slotCount
        guestSurvivor = survivor.coerceIn(0, SURVIVORS.size - 1)
        crew[seat] = slot.copy(selected = AUTO_WEAPON, armor = 0)
        damagePerk[seat] = if (perkOf(guestSurvivor) == Perk.DAMAGE) 1.15f else 1f
        ratePerk[seat] = if (perkOf(guestSurvivor) == Perk.FIRE_RATE) 1.2f else 1f
        rangePerk[seat] = if (perkOf(guestSurvivor) == Perk.RANGE) 1.25f else 1f
        critChance[seat] = if (perkOf(guestSurvivor) == Perk.CRIT) 0.12f else 0f
        coinPerk[seat] = if (perkOf(guestSurvivor) == Perk.SCAVENGER) 1.25f else 1f
        val g = gunners[seat]
        g.mode = AUTO_WEAPON
        g.weapon = crew[seat].bestWeapon
        g.cooldown = 0.5f
        g.alive = true
        for (k in g.heat.indices) {
            g.heat[k] = 0f
            g.overheated[k] = false
        }
        guestTime = seconds
        floatText(gunX(seat), gunY(seat) - 60f, SURVIVORS[guestSurvivor].name + " joins!", 1.6f, 0xFF40E0FF.toInt(), 20f)
    }

    /** True while a partner is connected and playing (some co-op rules only apply then). */
    var partnerPresent = false

    // ---- co-op locks (see PartnerLock): this train is jammed or gated until the partner clears it
    var lockedBy: HazardKind? = null
    var lockTime = 0f
    val jammed get() = lockedBy == HazardKind.SIGNAL_JAM
    /** The partner's lock we could clear right now, and how long it has left. */
    var partnerHelp: HazardKind? = null
    var partnerHelpTime = 0f

    fun lockTrain(kind: HazardKind) {
        lockedBy = kind
        lockTime = 0f
        shake = max(shake, 0.5f)
        sound(TrainSoundCue.BOSS)
        floatText(trainLeft + trainLength / 2f, 330f, when (kind) { HazardKind.SIGNAL_JAM -> "GUNS JAMMED! Ask your partner"; HazardKind.COLLAPSE -> "TRACK COLLAPSED! Ask your partner"; else -> "GATE! Ask your partner" }, 2f, 0xFFFF8A65.toInt(), 22f)
        if (kind == HazardKind.COLLAPSE) {
            val slot = rng.nextInt(slotCount)
            explosion(carCenterX(slot), 450f, 40f)
            hurtCar(slot, 20f * danger.dmgMult)
        }
        if (partnerPresent) onCoop?.invoke(CoopSignal.HelpNeeded(kind.ordinal))
    }

    fun unlockTrain(byPartner: Boolean) {
        if (lockedBy == null) return
        lockedBy = null
        if (byPartner) floatText(trainLeft + trainLength / 2f, 330f, "Cleared by your partner!", 1.6f, 0xFF66FF8A.toInt(), 22f)
    }

    fun partnerNeedsHelp(kind: Int) {
        partnerHelp = HazardKind.entries.getOrNull(kind)
        partnerHelpTime = 14f
    }

    /** Clears the partner's lock (the button on the co-op bar). */
    fun helpPartner() {
        remote?.let { if (partnerHelp != null) { it.act(GuestAction.HELP_PARTNER); partnerHelp = null }; return }
        if (partnerHelp == null) return
        partnerHelp = null
        onCoop?.invoke(CoopSignal.HelpGiven)
        floatText(trainLeft + trainLength / 2f, 340f, "Partner freed", 1.2f, 0xFF66FF8A.toInt(), 18f)
    }

    private fun tickLocks(dt: Float) {
        if (lockedBy != null) {
            lockTime += dt
            // Nobody to ask (solo, or the partner left) or nobody answered: it frees itself.
            if (lockTime > (if (partnerPresent) 14f else 5f)) unlockTrain(false)
        }
        if (partnerHelp != null) {
            partnerHelpTime -= dt
            if (partnerHelpTime <= 0f) partnerHelp = null
        }
    }

    // ---- co-op combo: both trains hit the same boss within 2 seconds of each other for a bonus
    private var lastOwnBossHit = -99f
    private var lastRemoteBossHit = -99f
    private var comboAt = -99f
    var comboCount = 0

    private fun tryCombo(boss: Zombie) {
        if (boss.dead || time - lastOwnBossHit > 2f || time - lastRemoteBossHit > 2f || time - comboAt < 2.5f) return
        comboAt = time
        comboCount++
        val wasRemote = applyingRemote
        applyingRemote = true // the bonus is local: the partner earns their own
        damageZombie(boss, boss.maxHp * (0.025f + 0.005f * min(comboCount, 6)), silent = true)
        applyingRemote = wasRemote
        floatText(boss.x, boss.y - 90f, "COMBO x$comboCount!", 1.4f, 0xFF40E0FF.toInt(), 26f)
        sound(TrainSoundCue.UPGRADE)
        shake = max(shake, 0.4f)
    }

    // ---- co-op: Emergency Tow (revive or repair the partner's train)
    var towCooldown = 0f
    val towCooldownMax get() = 80f - 10f * towLevel
    val towReady get() = partnerPresent && towLevel > 0 && !over && towCooldown <= 0f

    fun sendTow(): Boolean {
        remote?.let { if (towReady) { it.act(GuestAction.SEND_TOW); return true }; return false }
        if (!towReady) return false
        towCooldown = towCooldownMax
        floatText(trainLeft + trainLength / 2f, 340f, "Tow sent", 1.2f, 0xFF66FF8A.toInt(), 18f)
        onCoop?.invoke(CoopSignal.TowSent)
        return true
    }

    fun receiveTow() {
        if (over) return
        var revived = false
        for (i in 1 until slotCount) {
            if (carHp[i] <= 0f) {
                carHp[i] = carMaxHp[i] * 0.5f
                gunners[i].alive = true
                floatText(carCenterX(i), 360f, "Wagon towed back!", 1.6f, 0xFF66FF8A.toInt(), 22f)
                revived = true
                break
            }
        }
        if (!revived) healFraction(0.3f)
        sound(TrainSoundCue.REPAIR)
    }

    // ---- co-op: rescue events. A survivor is stranded on this track; only a lent passenger can fetch them.
    var rescueTime = 0f
    var rescues = 0
    internal var rescueProgress = 0f
    private var rescueTimer = 70f
    var partnerRescueTime = 0f
    /** 0..1 progress of the rescue in progress, for the beacon ring. */
    val rescueFraction get() = (rescueProgress / 4f).coerceIn(0f, 1f)

    fun startRescue() {
        if (rescueTime > 0f || over) return
        rescueTime = 45f
        rescueProgress = 0f
        floatText(trainLeft + trainLength + 80f, 300f, "SURVIVOR STRANDED!", 2f, 0xFFFFD54F.toInt(), 22f)
        sound(TrainSoundCue.BOSS)
        if (partnerPresent) onCoop?.invoke(CoopSignal.RescueCall)
    }

    private fun tickRescue(dt: Float) {
        if (partnerRescueTime > 0f) partnerRescueTime -= dt
        if (rescueTime > 0f) {
            rescueTime -= dt
            if (guestPresent) rescueProgress += dt else rescueProgress = max(0f, rescueProgress - dt * 0.5f)
            if (rescueProgress >= 4f) {
                val bonus = 200 + distanceMeters / 4
                rescueTime = 0f
                rescues++
                runCoins += bonus
                healFraction(0.25f)
                floatText(trainLeft + trainLength / 2f, 330f, "RESCUED! +$bonus coins", 2f, 0xFFFFD54F.toInt(), 24f)
                sound(TrainSoundCue.CRATE)
                onCoop?.invoke(CoopSignal.RescueDone(bonus))
            } else if (rescueTime <= 0f) {
                floatText(trainLeft + trainLength / 2f, 330f, "The survivor was lost...", 1.8f, 0xFFFF8A65.toInt(), 20f)
            }
        } else if (partnerPresent && !over) {
            rescueTimer -= dt
            if (rescueTimer <= 0f) {
                rescueTimer = 80f + 40f * rng.nextFloat()
                startRescue()
            }
        }
    }

    fun partnerRescueCall() {
        partnerRescueTime = 45f
    }

    /** The partner rescued their survivor with our passenger's help: we share the reward. */
    fun partnerRescueDone(bonus: Int) {
        partnerRescueTime = 0f
        receiveCoins(bonus)
    }

    /** The partner's train, as streamed to us (drawn on co-op routes). */
    val ghost = GhostTrain()
    private var shotMask = 0

    /** Which seats fired since the session last asked; sent so the partner sees our guns flash. */
    fun takeShotMask(): Int {
        val m = shotMask
        shotMask = 0
        return m
    }

    /** Health of the locomotive (seat 0) or a wagon as 0..1; 0 means destroyed. */
    fun carFraction(seat: Int): Float =
        if (seat == 0) (hp / maxHp).coerceIn(0f, 1f) else (carHp[seat] / carMaxHp[seat]).coerceIn(0f, 1f)

    var coinGiftCooldown = 0f
    /** What the Give Coins button would send right now: a quarter of this run's coins. */
    val coinGiftAmount get() = runCoins / 4
    val coinGiftReady get() = coopRole != CoopRole.SOLO && !over && coinGiftCooldown <= 0f && coinGiftAmount >= 10

    /** Gives a quarter of this run's coins to the partner (so a faster train can carry a slower one). */
    fun sendCoins(): Boolean {
        remote?.let { if (coinGiftReady) { it.act(GuestAction.GIVE_COINS); return true }; return false }
        if (!coinGiftReady) return false
        val amount = coinGiftAmount
        runCoins -= amount
        coinGiftCooldown = 4f
        floatText(trainLeft + trainLength / 2f, 340f, "-$amount coins sent", 1.2f, 0xFFFFD54F.toInt(), 18f)
        onCoop?.invoke(CoopSignal.CoinsSent(amount))
        return true
    }

    fun receiveCoins(amount: Int) {
        if (over || amount <= 0) return
        runCoins += amount
        sound(TrainSoundCue.CRATE)
        floatText(trainLeft + trainLength / 2f, 340f, "+$amount coins from your partner", 1.6f, 0xFFFFD54F.toInt(), 22f)
    }

    var repairSendCooldown = 0f
    val repairSendCooldownMax get() = 36f - 3f * gangwayLevel
    val repairSendReady get() = coopRole != CoopRole.SOLO && gangwayLevel > 0 && !over && repairSendCooldown <= 0f

    /** Sends a repair to the partner's train: a fraction of its health, growing with the Gangway level. */
    fun sendRepair(): Boolean {
        remote?.let { if (repairSendReady) { it.act(GuestAction.SEND_REPAIR); return true }; return false }
        if (!repairSendReady) return false
        repairSendCooldown = repairSendCooldownMax
        floatText(trainLeft + trainLength / 2f, 340f, "Repair sent", 1.2f, 0xFF66FF8A.toInt(), 18f)
        onCoop?.invoke(CoopSignal.RepairSent(0.2f + 0.04f * gangwayLevel))
        return true
    }

    /** The partner sent a repair: heals the locomotive and every wagon. */
    fun receiveRepair(fraction: Float) {
        if (over) return
        healFraction(fraction)
        sound(TrainSoundCue.REPAIR)
        floatText(trainLeft + trainLength / 2f, 340f, "Partner repair!", 1.4f, 0xFF66FF8A.toInt(), 22f)
    }

    /** Takes the damage the co-op session collected (boss hits dealt here, to forward to the partner). */
    fun takeBossDamage(): Float {
        val amount = bossDamageOut
        bossDamageOut = 0f
        return amount
    }

    /** The partner's boss hits land on our boss too, so both trains fight one boss. */
    fun applyBossDamage(fraction: Float) {
        val b = boss ?: return
        applyingRemote = true
        damageZombie(b, fraction * b.maxHp, silent = true)
        applyingRemote = false
        lastRemoteBossHit = time
        tryCombo(b)
    }

    // ---------------------------------------------------------------- active abilities

    fun useDynamite() {
        remote?.let { if (dynamiteReady && !over) it.act(GuestAction.BLAST); return }
        if (over || dynamiteCooldown > 0f) return
        dynamiteCooldown = dynamiteCooldownMax
        onCoop?.invoke(CoopSignal.Blast)
        detonate(dynamiteDamage, hitBoss = true)
    }

    /** The partner used Blast: it also goes off on our train (the boss already shares their damage, so it is spared here). */
    fun remoteBlast() {
        if (over) return
        detonate(dynamiteDamage * 0.7f, hitBoss = false)
    }

    private fun detonate(damage: Float, hitBoss: Boolean) {
        shake = max(shake, 0.8f)
        sound(TrainSoundCue.DYNAMITE)
        var cx = trainLeft - 20f
        while (cx < trainLeft + trainLength + 40f) {
            explosion(cx, 420f, 26f)
            cx += 70f
        }
        shooter = -1
        for (i in zombies.indices) {
            val z = zombies[i]
            if (z.dead || z.x < -20f || z.x > worldW + 20f || (!hitBoss && z.isBoss)) continue
            fxLane = z.lane
            explosion(z.x, z.y - 20f, 22f)
            fxLane = lane
            damageZombie(z, damage)
        }
    }

    fun useOverdrive() {
        remote?.let { if (overdriveReady && !over) it.act(GuestAction.BOOST); return }
        if (over || !overdriveReady) return
        overdriveTime = overdriveDuration
        overdriveCooldown = overdriveCooldownMax + overdriveDuration
        sound(TrainSoundCue.OVERDRIVE)
        for (i in 0 until slotCount) spark(gunX(i), gunY(i) - 20f)
    }

    fun useFieldRepair() {
        remote?.let { if (fieldRepairReady && !over) it.act(GuestAction.FIELD_REPAIR); return }
        if (over || !fieldRepairReady) return
        fieldRepairCooldown = fieldRepairCooldownMax
        val fraction = 0.35f + 0.06f * fieldRepairLevel
        hp = min(maxHp, hp + maxHp * fraction)
        for (i in 1 until slotCount) {
            if (carHp[i] <= 0f) {
                gunners[i].alive = true
                carHp[i] = carMaxHp[i] * 0.3f
                if (texts.size < MAX_TEXTS) addText(FloatingText(carCenterX(i), 360f, "Car restored!", 1.4f, 0xFF66FF8A.toInt(), 22f))
            }
            carHp[i] = min(carMaxHp[i], carHp[i] + carMaxHp[i] * fraction)
        }
        sound(TrainSoundCue.REPAIR)
        var x = trainLeft
        while (x < trainLeft + trainLength) {
            addParticle(Particle(x + rng.nextFloat() * 40f, 470f, 0f, -70f, 1.2f, 1.2f, 6f, 0xFF66FF8A.toInt(), ParticleKind.HEAL))
            x += 24f
        }
    }

    /** Screen tap in logical world coordinates: collects a supply crate if one was hit. */
    fun tapAt(lx: Float, ly: Float) {
        remote?.let { it.act(GuestAction.TAP, 0, lx, ly); return }
        if (!over && tapHazard(lx, ly)) return
        tapCrate(lx, ly)
    }

    /** A tap from the player of the second train, in the scene they see (their lane is drawn LANE_DY above the lead's). */
    fun tapFromGuest(lx: Float, ly: Float) {
        if (!over && tapHazard(lx, if (lane == 1) ly + LANE_DY else ly)) return
        tapCrate(lx, ly)
    }

    private fun tapCrate(lx: Float, ly: Float) {
        val world = lead ?: this
        val c = world.crate ?: return
        if (over || abs(lx - c.x) > 70f || abs(ly - c.y) > 80f) return
        world.crate = null
        crate = null
        sound(TrainSoundCue.CRATE)
        confetti(c.x, c.y, 24)
        when (rng.nextInt(3)) {
            0 -> {
                val coins = ((60 + time * 0.8f) * coinMultiplier).roundToInt()
                runCoins += coins
                addText(FloatingText(c.x, c.y - 30f, "+$coins coins", 1.4f, 0xFFFFD54F.toInt(), 24f))
            }
            1 -> {
                buffTime = 15f
                addText(FloatingText(c.x, c.y - 30f, "Damage boost!", 1.4f, 0xFFFF8A65.toInt(), 24f))
            }
            else -> {
                hp = min(maxHp, hp + maxHp * 0.4f)
                for (i in 1 until slotCount) if (carHp[i] > 0f) carHp[i] = min(carMaxHp[i], carHp[i] + carMaxHp[i] * 0.4f)
                addText(FloatingText(c.x, c.y - 30f, "Repairs!", 1.4f, 0xFF66FF8A.toInt(), 24f))
            }
        }
    }

    // ---------------------------------------------------------------- health

    private fun regenerate(dt: Float) {
        if (guestPresent) { // the lent passenger patches up this train (medics far better than gunners)
            val rate = SURVIVORS[guestSurvivor].coopHeal * dt
            hp = min(maxHp, hp + maxHp * rate)
            for (i in 1 until slotCount) if (carHp[i] > 0f) carHp[i] = min(carMaxHp[i], carHp[i] + carMaxHp[i] * rate)
        }
        val regen = regenPerSecond * biome.repairMult + biome.flatRegen
        if (regen <= 0f) return
        hp = min(maxHp, hp + regen * dt)
        for (i in 1 until slotCount) if (carHp[i] > 0f) carHp[i] = min(carMaxHp[i], carHp[i] + regen * 0.6f * dt)
    }

    private fun healFraction(fraction: Float) {
        hp = min(maxHp, hp + maxHp * fraction)
        for (i in 1 until slotCount) if (carHp[i] > 0f) carHp[i] = min(carMaxHp[i], carHp[i] + carMaxHp[i] * fraction)
        var x = trainLeft
        while (x < trainLeft + trainLength) {
            addParticle(Particle(x + rng.nextFloat() * 30f, 470f, 0f, -60f, 1.2f, 1.2f, 6f, 0xFF66FF8A.toInt(), ParticleKind.HEAL))
            x += 30f
        }
    }

    private fun isHurt(): Boolean {
        if (hp < maxHp * 0.5f) return true
        for (i in 1 until slotCount) if (carHp[i] < carMaxHp[i] * 0.5f) return true
        return false
    }

    /** Damages the car at [slot]; a wrecked car passes the hit inward (toward the locomotive) at reduced strength. */
    internal fun hurtCar(slot: Int, amount: Float) {
        if (godMode || over) return
        var s = slot
        var dmg = amount
        if (s > 0 && carHp[s] <= 0f) {
            while (s > 0 && carHp[s] <= 0f) s--
            dmg *= 0.6f
        }
        if (s == 0) {
            hp -= dmg
        } else {
            carHp[s] -= dmg
            if (carHp[s] <= 0f) destroyCar(s)
        }
        if (dmg > 3f) hitFlash = max(hitFlash, 0.5f)
        if (hitSoundGap <= 0f) {
            hitSoundGap = 0.25f
            sound(TrainSoundCue.TRAIN_HIT)
        }
    }

    private fun rearSlot(): Int = if (slotCount > 1) slotCount - 1 else 0

    private fun destroyCar(slot: Int) {
        carHp[slot] = 0f
        gunners[slot].alive = false
        shake = max(shake, 0.6f)
        val cx = carCenterX(slot)
        for (k in 0..2) explosion(cx - 50f + k * 50f, 450f, 32f)
        if (texts.size < MAX_TEXTS) addText(FloatingText(cx, 380f, "Car lost!", 1.6f, 0xFFFF5252.toInt(), 26f))
        sound(TrainSoundCue.DYNAMITE)
    }

    // ---------------------------------------------------------------- routes

    private fun updateRoute(dt: Float) {
        val j = junction
        if (j == null && !transitioning && time >= nextJunctionAt && coopRole != CoopRole.GUEST) {
            openJunction()
        } else if (j != null && j.chosen == null) {
            j.timeLeft -= dt
            // The guest waits for the host's decision (and only falls back to the first route if the message is lost).
            if (j.timeLeft <= (if (coopRole == CoopRole.GUEST) -3f else 0f)) applyRemoteRoute(j.optionA)
        }
        if (transitioning) {
            boundaryX -= WORLD_SPEED * speedFactor * dt
            if (!biomeSwitched && boundaryX <= worldW / 2f) {
                biomeSwitched = true
                kitFrom = fromBiome
                kitBlend = 0f
                var burstX = trainLeft
                while (burstX < frontEnd) {
                    spark(burstX, 440f)
                    burstX += 50f
                }
                biome = nextBiome
                biomeBanner = 4f
                if (biome == Biome.PLAINS) healFraction(0.3f)
                sound(TrainSoundCue.UPGRADE)
            }
            if (boundaryX < -200f) {
                transitioning = false
                junction = null
                nextJunctionAt = time + 50f
            }
        }
    }

    fun openJunction() {
        if (junction != null || transitioning) return
        val options = Biome.entries.filter { it != biome && (!it.needsPartner || partnerPresent) && (!it.sharedOnly || isShared) }.shuffled(rng).toMutableList()
        // Co-op runs often offer one of the co-op-only routes.
        if (partnerPresent && rng.nextFloat() < 0.6f && !options[0].needsPartner && !options[1].needsPartner) {
            val special = options.firstOrNull { it.needsPartner }
            if (special != null) { options.remove(special); options.add(0, special) }
        }
        var second = options[1]
        // A hurt train is always offered the calm Plains route as one of its two choices.
        if (biome != Biome.PLAINS && isHurt() && options[0] != Biome.PLAINS && options[1] != Biome.PLAINS) second = Biome.PLAINS
        junction = Junction(options[0], second, 8f)
        if (coopRole == CoopRole.HOST) onCoop?.invoke(CoopSignal.JunctionOpened(options[0], second))
    }

    /** Guest side: show the junction the host opened. */
    fun openJunctionWith(a: Biome, b: Biome) {
        if (junction != null || transitioning) return
        junction = Junction(a, b, 8f)
    }

    /** Applies a route decision that came from the partner (or the host's timeout fallback). */
    fun applyRemoteRoute(choice: Biome) {
        if (junction == null) junction = Junction(choice, choice, 0f)
        applyingRemote = true
        chooseRoute(choice)
        applyingRemote = false
    }

    /** Throws the switch: the chosen route's scenery sweeps in from the right and the gameplay modifiers apply once it reaches the train. */
    fun chooseRoute(choice: Biome) {
        remote?.let { it.act(GuestAction.CHOOSE_ROUTE, choice.ordinal); return }
        val j = junction ?: return
        if (j.chosen != null || transitioning) return
        if (coopRole == CoopRole.GUEST && !applyingRemote) {
            onCoop?.invoke(CoopSignal.RouteRequested(choice)) // the host decides
            return
        }
        j.chosen = choice
        fromBiome = biome
        nextBiome = choice
        boundaryX = worldW + 60f
        biomeSwitched = false
        transitioning = true
        routesTaken++
        sound(TrainSoundCue.UPGRADE)
        if (coopRole == CoopRole.HOST) onCoop?.invoke(CoopSignal.RouteChosen(choice))
    }

    // ---------------------------------------------------------------- events

    private fun updateEvents(dt: Float) {
        // Supply crate.
        val c = crate
        if (c == null) {
            if (time >= nextCrateAt) {
                crate = Crate(worldW * (0.25f + 0.5f * rng.nextFloat()), -50f)
                nextCrateAt = time + 50f + rng.nextFloat() * 20f
            }
        } else {
            c.phase += dt
            if (!c.landed) {
                c.y += 70f * dt
                if (c.y >= 300f) c.landed = true
            } else {
                c.life -= dt
                if (c.life <= 0f) crate = null
            }
            c.x += sin(c.phase * 1.6f) * 14f * dt
        }
        // Horde: a warning, then a burst of zombies from one side.
        if (hordeTime > 0f) {
            hordeTime -= dt
        } else if (!hordePending && hordeWarning <= 0f && time >= nextHordeAt) {
            hordeSide = if (rng.nextBoolean()) 1 else -1
            hordeWarning = 3f
            hordePending = true
            nextHordeAt = Float.MAX_VALUE
            sound(TrainSoundCue.HORDE)
        }
        if (hordePending && hordeWarning <= 0f) {
            hordePending = false
            hordeTime = 12f
            nextHordeAt = time + 70f + rng.nextFloat() * 20f
        }
    }

    internal var hordePending = false

    val hordeFromLeft get() = hordeSide < 0

    // ---------------------------------------------------------------- spawning

    private fun spawn(dt: Float) {
        if (time >= nextBossAt && coopRole != CoopRole.GUEST) {
            spawnBoss()
            nextBossAt += 80f
        }
        var rate = (0.45f + time * 0.011f).coerceAtMost(2.8f) * biome.spawnMult * danger.spawnMult * Math.pow(speedFactor.toDouble(), 0.85).toFloat() * coopSpawnMult
        if (hordeTime > 0f) rate *= 3f
        if (boss != null) rate *= 0.55f
        spawnAccumulator += dt * rate
        while (spawnAccumulator >= 1f) {
            spawnAccumulator -= 1f
            if (zombies.size < zombieCap) spawnZombie(pickType(), if (hordeTime > 0f) hordeSide else 0)
        }
    }

    /** Picks the next enemy from the current route's spawn table (see Biome.spawns). */
    private fun pickType(): ZombieType {
        val spawns = biome.spawns
        var total = 0f
        for (i in spawns.indices) total += spawns[i].weightFor(time, lane)
        if (total <= 0f) return ZombieType.WALKER
        var roll = rng.nextFloat() * total
        for (i in spawns.indices) {
            roll -= spawns[i].weightFor(time, lane)
            if (roll <= 0f) return spawns[i].type
        }
        for (i in spawns.indices.reversed()) if (spawns[i].weightFor(time, lane) > 0f) return spawns[i].type
        return ZombieType.WALKER
    }

    private fun spawnZombie(type: ZombieType, forcedSide: Int = 0) {
        val side = if (forcedSide != 0) forcedSide else if (rng.nextBoolean()) 1 else -1
        val x = if (side > 0) worldW + 40f + rng.nextFloat() * 80f else -40f - rng.nextFloat() * 80f
        val feetY = if (type.flying) 110f + rng.nextFloat() * 170f else RAIL_Y + 24f + rng.nextFloat() * 36f
        addZombie(type, x, feetY, side)
        // Packs: the rest of the group spawns beside the first.
        repeat(type.packSize - 1) {
            if (zombies.size < zombieCap) addZombie(type, x + side * (it + 1) * 22f, feetY + (if (type.flying) (it + 1) * 18f else (it % 2) * 10f), side)
        }
    }

    internal fun addZombie(type: ZombieType, x: Float, feetY: Float, side: Int): Zombie {
        val hpScale = (1f + time / 45f) * biome.hpMult * danger.hpMult
        val z = Zombie(type, x, feetY, side, type.hp * hpScale, (1f + time / 140f) * biome.dmgMult * danger.dmgMult, rng.nextFloat())
        z.lane = lane
        if (!type.boss && !type.stationary) rollElite(z)
        if (type.flying) {
            z.targetX = trainLeft + rng.nextFloat() * (trainLength + shape.nose)
            z.facing = if (z.targetX > x) 1 else -1
        }
        zombies.add(z)
        return z
    }

    /** Spawns the boss of the current route (or [type], for the debug tools). */
    fun spawnBoss(type: ZombieType = if (lane == 1) biome.bossUpper ?: biome.boss else biome.boss) {
        if (boss != null) return
        val side = if (rng.nextBoolean()) 1 else -1
        val x = if (side > 0) worldW + 120f else -120f
        val hp = type.hp * (1f + 0.55f * bossNumber) * danger.hpMult
        val y = if (type.flying) 190f else RAIL_Y + 30f
        val b = Zombie(type, x, y, side, hp, (1f + time / 200f) * danger.dmgMult, 0.5f)
        b.lane = lane
        b.cruiseY = 200f
        b.targetX = trainLeft + trainLength * 0.5f
        b.facing = if (side > 0) -1 else 1
        zombies.add(b)
        boss = b
        bossName = type.displayName
        bossNumber++
        bossAlert = 3.5f
        sound(TrainSoundCue.BOSS)
        if (coopRole == CoopRole.HOST) onCoop?.invoke(CoopSignal.BossSpawned(type))
    }

    // ---------------------------------------------------------------- zombies

    private fun updateZombies(dt: Float) {
        for (i in zombies.indices) {
            val z = zombies[i]
            if (z.dead || z.lane != lane) continue
            z.numberTimer -= dt
            z.sinceHit += dt
            if (z.elite != null && z.has(EliteTrait.REGEN) && z.sinceHit > 1.2f && z.hp < z.maxHp) z.hp = min(z.maxHp, z.hp + z.maxHp * 0.08f * dt)
            if (z.pendingDamage >= 1f && z.numberTimer <= 0f) emitDamageNumber(z)
            if (z.flying) updateFlyer(z, dt)
            if (z.jumpState != 0) updateAirborne(z, dt)
            if (z.type.stationary) updateStationary(z, dt)
            if (z.type.ranged > 0f && !z.flying) updateRangedGround(z, dt)
            if (z.isBoss) updateBossSpecial(z, dt)
        }
        var side = -1
        while (side <= 1) {
            sideBuffer.clear()
            val end = endX(side)
            for (i in zombies.indices) {
                val z = zombies[i]
                if (!z.dead && z.lane == lane && !z.flying && z.jumpState == 0 && !z.type.stationary && z.type.ranged <= 0f && z.side == side) sideBuffer.add(z)
            }
            sortByDistance(sideBuffer, end)
            for (rank in sideBuffer.indices) {
                val z = sideBuffer[rank]
                val spot = end + side * (14f + min(rank, 16) * 22f * z.type.size.coerceAtLeast(1f))
                val delta = spot - z.x
                var speedMul = 1f
                if (z.type.lunges) {
                    // Crocodiles creep, then LUNGE in short fast bursts.
                    z.lungeTimer -= dt
                    if (z.retarget > 0f) {
                        z.retarget -= dt
                        speedMul = 2.6f
                    } else if (z.lungeTimer <= 0f && !z.attacking) {
                        z.retarget = 0.7f
                        z.lungeTimer = 3.5f
                    }
                }
                val step = z.type.speed * dt * speedMul * z.speedMul * biome.speedMult * zombiePace
                val arrived = abs(delta) <= step + 1f
                if (arrived) z.x = spot else z.x += if (delta > 0f) step else -step
                z.facing = -side
                z.anim += dt * (z.type.speed / 14f + 2f)
                z.flash = max(0f, z.flash - dt * 6f)
                z.attacking = arrived && rank < MAX_ATTACKERS_PER_SIDE
                if (z.attacking) attack(z, side, dt)
                if (z.type.hops && buddy != null && !z.dead) {
                    z.hopTimer -= dt
                    if (z.hopTimer <= 0f && abs(z.x - end) < 280f) {
                        z.hopTimer = 4f + rng.nextFloat() * 3f
                        hopTrack(z)
                    }
                }
                if (z.type.leaper && z.jumpState == 0 && abs(z.x - end) < 240f) startLeap(z)
            }
            side += 2
        }
    }

    /** Insertion sort by distance to [end]; the list is short and nearly sorted frame to frame, and this allocates nothing. */
    private fun sortByDistance(list: ArrayList<Zombie>, end: Float) {
        for (i in 1 until list.size) {
            val item = list[i]
            val key = abs(item.x - end)
            var j = i - 1
            while (j >= 0 && abs(list[j].x - end) > key) {
                list[j + 1] = list[j]
                j--
            }
            list[j + 1] = item
        }
    }

    private fun emitDamageNumber(z: Zombie) {
        val amount = z.pendingDamage.toInt()
        z.pendingDamage = 0f
        z.numberTimer = 0.25f
        val crit = z.pendingCrit
        z.pendingCrit = false
        if (!damageNumbers || texts.size >= MAX_TEXTS || z.dead) return
        addText(
            FloatingText(
                z.x + (rng.nextFloat() - 0.5f) * 16f, z.y - 34f * z.type.size - 12f, amount.toString(), 0.7f,
                if (crit) 0xFFFFA726.toInt() else 0xFFFFFFFF.toInt(), if (crit) 24f else 16f
            )
        )
    }

    // ---------------------------------------------------------------- leapers, drops, ranged and stationary enemies

    private fun roofY(slot: Int): Float = if (slot == 0) shape.locoRoofY + 4f else 424f

    /** A leaper that has reached the train jumps over the queue onto a random car's roof. */
    private fun startLeap(z: Zombie) {
        val slot = rng.nextInt(slotCount)
        z.roofSlot = slot
        z.landY = roofY(slot)
        val tx = carCenterX(slot) + (rng.nextFloat() - 0.5f) * 50f
        val gravity = SHELL_GRAVITY * biome.gravityMult
        val flight = 0.9f / kotlin.math.sqrt(biome.gravityMult.coerceAtLeast(0.2f))
        z.vx = (tx - z.x) / flight
        z.vy = (z.landY - z.y) / flight - 0.5f * gravity * flight
        z.jumpState = 1
        z.facing = if (z.vx >= 0f) 1 else -1
    }

    /** Overpass drop: the zombie falls straight down onto a car's roof. */
    internal fun startDrop(z: Zombie, slot: Int) {
        z.roofSlot = slot
        z.landY = roofY(slot)
        z.x = carCenterX(slot) + (rng.nextFloat() - 0.5f) * 60f
        z.y = -40f
        z.vx = 0f
        z.vy = 0f
        z.jumpState = 1
    }

    /** Leapers and droppers: flying through the air (1) or standing on a roof clawing the car (2). */
    private fun updateAirborne(z: Zombie, dt: Float) {
        z.flash = max(0f, z.flash - dt * 6f)
        if (z.jumpState == 1) {
            val gravity = SHELL_GRAVITY * biome.gravityMult
            z.vy += gravity * dt
            z.x += z.vx * dt
            z.y += z.vy * dt
            z.anim += dt * 8f
            if (z.y >= z.landY && z.vy > 0f) {
                z.y = z.landY
                z.vx = 0f
                z.vy = 0f
                z.jumpState = 2
                shake = max(shake, 0.2f)
            }
        } else if (z.jumpState == 2) {
            z.anim += dt * 6f
            z.attacking = true
            hurtCar(z.roofSlot, z.type.damage * z.dmgScale * dt)
        }
    }

    /** A tentacle grabs a car from below and squeezes until it is killed (or lets go after a while). */
    private fun updateStationary(z: Zombie, dt: Float) {
        z.anim += dt * 4f
        z.flash = max(0f, z.flash - dt * 6f)
        z.attacking = true
        z.retarget += dt
        if (z.retarget > 18f) {
            z.dead = true
            return
        }
        hurtCar(slotAt(z.x), z.type.damage * z.dmgScale * dt)
    }

    private fun updateRangedGround(z: Zombie, dt: Float) {
        val hold = endX(z.side) + z.side * (z.type.ranged + (z.seed - 0.5f) * 80f)
        val dx = hold - z.x
        z.facing = -z.side
        z.flash = max(0f, z.flash - dt * 6f)
        z.anim += dt * (z.type.speed / 14f + 2f)
        if (abs(dx) > 8f) {
            z.attacking = false
            z.x += (if (dx > 0f) 1f else -1f) * min(abs(dx), z.type.speed * z.speedMul * biome.speedMult * zombiePace * dt)
        } else {
            z.attacking = true
            z.shootTimer -= dt
            if (z.shootTimer <= 0f) rangedShoot(z)
        }
    }

    private fun updateRangedFlyer(z: Zombie, dt: Float) {
        z.flash = max(0f, z.flash - dt * 6f)
        val hold = endX(z.side) + z.side * (z.type.ranged + (z.seed - 0.5f) * 60f)
        val dx = hold - z.x
        z.facing = -z.side
        if (abs(dx) > 10f) z.x += (if (dx > 0f) 1f else -1f) * min(abs(dx), z.type.speed * z.speedMul * dt)
        val wantY = 300f + sin(z.anim * 0.4f) * 14f
        val dy = wantY - z.y
        z.y += (if (dy > 0f) 1f else -1f) * min(abs(dy), 110f * dt)
        z.attacking = abs(dx) <= 30f
        if (z.attacking) {
            z.shootTimer -= dt
            if (z.shootTimer <= 0f) rangedShoot(z)
        }
    }

    private fun rangedShoot(z: Zombie) {
        z.shootTimer = 2.2f + rng.nextFloat() * 0.8f
        val slot = if (z.side > 0) 0 else rearSlot()
        val sy = z.y - (if (z.flying) 10f else 30f)
        val color = when (z.type) {
            ZombieType.JUGGLER -> 0xFFFF4081.toInt()
            ZombieType.DRONE -> 0xFF40E0FF.toInt()
            else -> 0xFFFFC107.toInt()
        }
        addTracer(Tracer(z.x, sy, carCenterX(slot), 450f, 0.14f, 2.4f, color))
        spark(z.x, sy)
        hurtCar(slot, z.type.damage * z.dmgScale * 1.6f)
    }

    // ---------------------------------------------------------------- route hazards (see Hazards.kt)

    val hazards = ArrayList<Hazard>(3)
    private var hazardBiome: Biome? = null

    /** Keeps the active hazards in step with the route: new ones start, ones from the old route retire once idle. */
    private fun updateHazards(dt: Float) {
        if (hazardBiome != biome) {
            hazardBiome = biome
            for (i in hazards.indices) hazards[i].retiring = true
            for (kind in biome.hazards) {
                var existing: Hazard? = null
                for (i in hazards.indices) if (hazards[i].kind == kind) existing = hazards[i]
                if (existing != null) existing.retiring = false else hazards.add(HazardRegistry.create(kind))
            }
        }
        var i = hazards.size - 1
        while (i >= 0) {
            val h = hazards[i]
            h.update(this, dt)
            if (h.retiring && h.idle) hazards.removeAt(i)
            i--
        }
    }

    internal fun tapHazard(lx: Float, ly: Float): Boolean {
        for (i in hazards.indices) if (hazards[i].onTap(this, lx, ly)) return true
        return false
    }

    private fun shootHazard(): Boolean {
        for (i in hazards.indices) if (hazards[i].shootDown(this)) return true
        return false
    }

    /** Debug: switch to [route] immediately (the train morphs as it would on a junction). */
    fun debugSetRoute(route: Biome) {
        kitFrom = biome
        kitBlend = 0f
        junction = null
        transitioning = false
        fromBiome = route
        nextBiome = route
        biome = route
        biomeBanner = 4f
    }

    fun debugSkipDistance(meters: Float) {
        distance += meters
    }

    fun debugHazard() {
        for (i in hazards.indices) hazards[i].trigger()
    }

    /** Flyers ignore the queue: they cruise high, swoop over a car on the train and claw it from above. */
    private fun updateFlyer(z: Zombie, dt: Float) {
        val isBat = z.type.erratic
        z.anim += dt * (if (isBat) 12f else if (z.isBoss) 4f else 5f)
        z.flash = max(0f, z.flash - dt * 6f)
        z.retarget -= dt
        if (z.type.ranged > 0f) {
            updateRangedFlyer(z, dt)
            return
        }
        val dx = z.targetX - z.x
        if ((isBat || z.isBoss) && z.retarget <= 0f) {
            z.retarget = (if (z.isBoss) 3.5f else 1.8f) + rng.nextFloat() * 1.5f
            z.targetX = trainLeft + rng.nextFloat() * (trainLength + shape.nose)
        }
        if (abs(dx) > 14f) {
            z.facing = if (dx > 0f) 1 else -1
            z.x += (if (dx > 0f) 1f else -1f) * min(abs(dx), z.type.speed * z.speedMul * dt)
        }
        if (isBat) z.x += sin(z.anim * 0.7f) * 40f * dt
        val hoverY = if (isBat) 418f else if (z.isBoss) 350f else 402f
        val closeIn = abs(dx) < 320f
        val wantY = (if (closeIn) hoverY else z.cruiseY) + sin(z.anim * 0.5f) * (if (isBat) 10f else 6f)
        val dy = wantY - z.y
        z.y += (if (dy > 0f) 1f else -1f) * min(abs(dy), 110f * dt)
        z.attacking = abs(dx) < 26f && abs(z.y - hoverY) < 24f
        if (z.attacking) hurtCar(slotAt(z.x), z.type.damage * z.dmgScale * dt)
    }

    /** Bosses use their type's special: summon helpers, or smash the car they are clawing (see ZombieType). */
    private fun updateBossSpecial(z: Zombie, dt: Float) {
        val type = z.type
        if (type.specialEvery <= 0f) return
        z.special -= dt
        if (z.special > 0f) return
        val summon = type.summon
        if (summon != null) {
            z.special = type.specialEvery
            if (zombies.size < zombieCap - type.summonCount) {
                repeat(type.summonCount) { n ->
                    val spread = (n - (type.summonCount - 1) / 2f) * 30f
                    addZombie(summon, z.x + spread, if (summon.flying) z.y else RAIL_Y + 30f, z.side)
                }
            }
            if (type.callout.isNotEmpty()) floatText(z.x, z.y - 90f, type.callout, 0.9f, 0xFFFF4081.toInt(), 26f)
        } else if (type.smash > 0f && z.attacking) {
            z.special = type.specialEvery
            hurtCar(if (z.jumpState == 2) z.roofSlot else if (z.side > 0) 0 else rearSlot(), type.smash * z.dmgScale)
            explosion(z.x - z.side * 30f, 470f, 40f)
            shake = max(shake, 0.7f)
            if (type.callout.isNotEmpty()) floatText(z.x, z.y - 150f, type.callout, 0.9f, 0xFFFF7043.toInt(), 28f)
        }
    }

    internal fun floatText(x: Float, y: Float, text: String, life: Float, color: Int, size: Float) {
        if (texts.size < MAX_TEXTS) addText(FloatingText(x, y, text, life, color, size))
    }

    /** A hopper leaps to the other train's track: sparks mark where it left and where it lands. */
    private fun hopTrack(z: Zombie) {
        val other = buddy ?: return
        spark(z.x, z.y - 30f)
        z.lane = other.lane
        val previous = fxLane
        fxLane = z.lane
        spark(z.x, z.y - 30f)
        fxLane = previous
    }

    /** Swaps which track each of the two trains runs on (the zombies stay on their tracks, so each train faces the other's crowd). */
    fun swapTracks() {
        val other = ally ?: return
        val t = lane
        lane = other.lane
        other.lane = t
        fxLane = lane
        other.fxLane = other.lane
        switchWarning = 0f
        other.switchWarning = 0f
        shake = max(shake, 0.8f)
        sound(TrainSoundCue.HORDE)
        floatText(worldW / 2f, 330f, "TRACKS SWITCHED!", 1.8f, 0xFFFFC107.toInt(), 28f)
    }

    private fun attack(z: Zombie, side: Int, dt: Float) {
        if (z.type.switcher) { // the Signalman throws the switch and is gone
            z.dead = true
            explosion(z.x, z.y - 20f, 30f)
            (lead ?: this).swapTracks()
            return
        }
        val slot = if (side > 0) 0 else rearSlot()
        if (z.type == ZombieType.BLOATER) {
            // Bloaters burst on contact instead of clawing.
            z.dead = true
            hurtCar(slot, 30f * z.dmgScale)
            explosion(z.x, z.y - 20f, 34f)
            greenBurst(z.x, z.y - 24f, 22)
            return
        }
        hurtCar(slot, z.type.damage * z.dmgScale * dt)
        if (thornsPerSecond > 0f) {
            shooter = -1
            damageZombie(z, thornsPerSecond * dt, silent = true)
            if (rng.nextFloat() < dt * 8f) spark(z.x, z.y - 24f)
        }
    }

    internal fun damageZombie(z: Zombie, amount: Float, silent: Boolean = false, crit: Boolean = false) {
        if (z.dead) return
        var dealt = if (silent) amount else armored(amount, z)
        if (!silent && z.elite != null && z.has(EliteTrait.BULLETPROOF) && shooter >= 0) dealt *= 0.3f
        val shown = dealt
        if (!silent && z.shield > 0f) {
            val absorbed = min(z.shield, dealt)
            z.shield -= absorbed
            dealt -= absorbed
        }
        if (z.isBoss && !applyingRemote) {
            bossDamageOut += dealt / z.maxHp
            lastOwnBossHit = time
        }
        // Co-op bosses shrug off your own guns; the damage your partner forwards lands in full.
        if (z.type.partnerOnly && !applyingRemote && partnerPresent && z.lane == lane) dealt *= 0.1f
        z.hp -= dealt
        if (z.isBoss && !applyingRemote && partnerPresent) tryCombo(z)
        if (!silent) {
            z.flash = 1f
            z.sinceHit = 0f
            z.pendingDamage += shown
            if (crit) z.pendingCrit = true
        }
        if (z.hp <= 0f) killZombie(z)
    }

    private fun killZombie(z: Zombie) {
        val previous = fxLane
        fxLane = z.lane
        killZombieFx(z)
        fxLane = previous
    }

    private fun killZombieFx(z: Zombie) {
        z.dead = true
        kills++
        if (z.flying) flyerKills++
        var mult = coinMultiplier * biome.coinMult * danger.coinMult
        if (z.elite != null) mult *= 2.5f
        if (hordeTime > 0f) mult *= 1.5f
        if (shooter >= 0 && shooter <= slotCount) mult *= coinPerk[shooter]
        val coins = max(1, (z.type.coins * mult).roundToInt())
        runCoins += coins
        if (texts.size < MAX_TEXTS) addText(FloatingText(z.x, z.y - 60f * z.type.size, "+$coins", 1.0f, 0xFFFFD54F.toInt(), 22f))
        greenBurst(z.x, z.y - 24f * z.type.size, if (z.isBoss) 60 else 12)
        if (fx.recording) fx.add(FxLog.COINS, fxLane, z.x, z.y - 30f, count0 = if (z.isBoss) 18 else min(4, 1 + coins / 3))
        repeat(if (z.isBoss) 18 else min(4, 1 + coins / 3)) {
            addParticle(Particle(z.x, z.y - 30f, (rng.nextFloat() - 0.5f) * 160f, -150f - rng.nextFloat() * 120f, 0.9f, 0.9f, 5f, 0xFFFFC107.toInt(), ParticleKind.COIN))
        }
        sound(TrainSoundCue.ZOMBIE_DIE)
        sound(TrainSoundCue.COIN)
        if (z.isBoss) {
            bossKills++
            shake = max(shake, 0.7f)
            explosion(z.x, z.y - 60f, 60f)
        }
        if (z.type == ZombieType.CLOWN || z.type == ZombieType.CLOWN_PRINCE) {
            sound(TrainSoundCue.HONK)
            if (texts.size < MAX_TEXTS) addText(FloatingText(z.x, z.y - 90f, "HONK!", 1.0f, 0xFFFF5252.toInt(), 22f))
            confetti(z.x, z.y - 40f, 26)
        }
        if (z.type == ZombieType.BALLOON) confetti(z.x, z.y - 90f, 22)
        if (biome == Biome.CARNIVAL && (z.type == ZombieType.CLOWN || z.type == ZombieType.JUGGLER) && zombies.size < zombieCap - 2) {
            // The funhouse: clowns pop into two mini clowns.
            repeat(2) { addZombie(ZombieType.MINI_CLOWN, z.x + (it * 2 - 1) * 16f, z.y, z.side) }
        }
        if (z.elite != null && z.has(EliteTrait.VOLATILE)) {
            // Volatile elites blow up: the train takes a hit if it was still close.
            explosion(z.x, z.y - 20f, 40f)
            val owner = rigOf(z)
            if (!z.flying && abs(z.x - owner.endX(z.side)) < 140f) owner.hurtCar(if (z.side > 0) 0 else owner.rearSlot(), 18f * z.dmgScale)
        }
        if (z.type == ZombieType.BLOATER) {
            // A shot bloater pops and hurts the zombies around it.
            explosion(z.x, z.y - 20f, 30f)
            for (i in zombies.indices) {
                val other = zombies[i]
                if (!other.dead && other.lane == z.lane && abs(other.x - z.x) < 100f) damageZombie(other, 35f * z.dmgScale)
            }
        }
    }

    private fun compactZombies() {
        var write = 0
        for (read in zombies.indices) {
            val z = zombies[read]
            if (!z.dead) zombies[write++] = z
        }
        while (zombies.size > write) zombies.removeAt(zombies.size - 1)
        if (boss?.dead == true) boss = null
    }

    internal fun rebuildDrawOrder() {
        drawOrder.clear()
        drawOrder.addAll(zombies)
        for (i in 1 until drawOrder.size) {
            val item = drawOrder[i]
            var j = i - 1
            while (j >= 0 && drawOrder[j].y > item.y) {
                drawOrder[j + 1] = drawOrder[j]
                j--
            }
            drawOrder[j + 1] = item
        }
    }

    // ---------------------------------------------------------------- gunners

    private fun updateGunners(dt: Float) {
        val boost = if (overdriveTime > 0f) 1.6f else 1f
        for (i in 0..slotCount) {
            val g = gunners[i]
            if (i == slotCount) {
                g.alive = guestPresent
                if (!g.alive) continue
            } else {
                if (!carAlive(i) || seatAwayTime[i] > 0f) {
                    g.alive = false
                    continue
                }
                g.alive = true
            }
            for (k in g.heat.indices) {
                if (g.heat[k] > 0f) g.heat[k] = max(0f, g.heat[k] - dt * HEAT_COOL_RATE)
                if (g.overheated[k] && g.heat[k] <= HEAT_RECOVER) g.overheated[k] = false
            }
            updateLoadout(i, dt)
            val weapon = g.weapon
            g.cooldown -= dt * boost * ratePerk[i]
            g.recoil = max(0f, g.recoil - dt * 9f)
            g.flash = max(0f, g.flash - dt)
            val gx = gunX(i)
            val range = weapon.range * rangePerk[i] * biome.rangeMult
            var target: Zombie? = null
            var best = range
            for (k in zombies.indices) {
                val z = zombies[k]
                if (z.dead || z.x < -10f || z.x > worldW + 10f) continue
                if (weapon.groundOnly && z.flying) continue
                val d = reachDistance(i, z)
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
            if (g.cooldown <= 0f && !g.overheated[weapon.ordinal] && !jammed) {
                g.cooldown = weapon.interval
                fire(i, target)
            }
        }
    }

    /** Where [z] is in OUR lane's frame (the other lane is drawn LANE_DY above or below ours). */
    private fun laneDy(z: Zombie): Float = if (z.lane == lane) 0f else if (z.lane == 1) -LANE_DY else LANE_DY

    private fun targetY(z: Zombie) = z.y + laneDy(z) - z.type.aimHeight * z.type.size
    /** Damage left after the target's flat armor (never below a quarter of the hit). */
    private fun armored(damage: Float, z: Zombie): Float {
        val armor = z.type.armor + (if (z.elite != null && z.has(EliteTrait.ARMORED)) 8f else 0f)
        return if (armor <= 0f) damage else max(damage * 0.25f, damage - armor)
    }

    /** Bulletproof elites shrug off gun damage; train weapons and abilities are unaffected. */
    private fun gunFactor(z: Zombie): Float = if (z.elite != null && z.has(EliteTrait.BULLETPROOF)) 0.3f else 1f

    private fun rollElite(z: Zombie) {
        val start = max(10f, 30f - 5f * danger.ordinal)
        val chance = min(0.7f, ((time - start) / 350f).coerceIn(0f, 0.3f) * danger.eliteMult)
        if (chance <= 0f || rng.nextFloat() >= chance) return
        val traits = EliteTrait.entries
        val first = traits[rng.nextInt(traits.size)]
        // Inferno elites sometimes carry a second trait.
        val second = if (danger == DangerLevel.INFERNO && rng.nextFloat() < 0.35f) traits[rng.nextInt(traits.size)].takeIf { it != first } else null
        applyElite(z, first, second)
    }

    private fun applyElite(z: Zombie, first: EliteTrait, second: EliteTrait?) {
        z.elite = first
        z.elite2 = second
        z.maxHp *= 1.6f
        z.hp = z.maxHp
        if (z.has(EliteTrait.SHIELDED)) z.shield = z.maxHp * 0.6f
        if (z.has(EliteTrait.SWIFT)) z.speedMul = 1.6f
    }

    /**
     * How far [z] is from [slot]'s gun for range purposes. Ground zombies mass at the two ends of the train, so
     * range is measured mostly from the hull edge: a gunner deep inside the train pays only 40% of the distance
     * to that edge, which lets middle wagons cover the nearest zombies instead of being out of reach.
     * Targets above the train are measured straight from the gunner.
     */
    private fun reachDistance(slot: Int, z: Zombie): Float {
        val gx = gunX(slot)
        val left = rearEnd
        val right = frontEnd
        return when {
            z.x > right -> (z.x - right) + 0.4f * max(0f, right - gx)
            z.x < left -> (left - z.x) + 0.4f * max(0f, gx - left)
            else -> abs(z.x - gx)
        }
    }

    private fun damageFor(slot: Int, weapon: WeaponType): Float =
        weapon.damage * crew[slot].powerMultiplier * damagePerk[slot] * paint.damageMult * (if (partnerPresent) 1f + 0.05f * linkLevel else 1f) * (if (buffTime > 0f) 1.3f else 1f)

    /** Damage of one shot from [slot]'s current weapon, with power upgrades, perk and any damage boost applied. */
    fun shotDamage(slot: Int): Float = damageFor(slot, gunners[slot].weapon)

    /** Total damage bonus in percent over the weapon's base damage (power level x perk x crate boost). */
    fun damageBonusPercent(slot: Int): Int =
        ((crew[slot].powerMultiplier * damagePerk[slot] * (if (buffTime > 0f) 1.3f else 1f) - 1f) * 100f).roundToInt()

    private fun fire(slot: Int, target: Zombie) {
        shotMask = shotMask or (1 shl slot)
        val g = gunners[slot]
        val weapon = g.weapon
        val crit = critChance[slot] > 0f && rng.nextFloat() < critChance[slot]
        val damage = damageFor(slot, weapon) * (if (crit) 2.5f else 1f)
        val gx = gunX(slot)
        val len = weaponLength(weapon)
        val mx = gx + g.facing * cos(g.aim) * len
        val my = shoulderY(slot) + sin(g.aim) * len
        val tx = target.x
        val ty = targetY(target)
        g.recoil = 1f
        g.flash = 0.07f
        if (weapon.heatPerShot > 0f) {
            val h = g.heat[weapon.ordinal] + weapon.heatPerShot
            g.heat[weapon.ordinal] = h
            if (h >= 1f) {
                g.overheated[weapon.ordinal] = true
                if (texts.size < MAX_TEXTS) addText(FloatingText(gx, gunY(slot) - 66f, "OVERHEAT!", 1.0f, 0xFFFF5252.toInt(), 18f))
            }
        }

        when (weapon) {
            WeaponType.SHOTGUN -> repeat(3) { n ->
                addTracer(Tracer(mx, my, tx, ty + (n - 1) * 14f, 0.07f, 1.5f, 0xFFFFE082.toInt()))
            }
            WeaponType.CANNON -> {
                addTracer(Tracer(mx, my, tx, ty, 0.12f, 5f, 0xFFFF9800.toInt()))
                explosion(tx, ty, 26f)
            }
            WeaponType.RIFLE -> addTracer(Tracer(mx, my, tx + g.facing * 160f, ty, 0.1f, 2.5f, 0xFFB3E5FC.toInt()))
            WeaponType.ROCKET -> {
                addTracer(Tracer(mx, my, tx, ty, 0.14f, 4f, 0xFFFFAB91.toInt()))
                explosion(tx, ty, 34f)
            }
            WeaponType.LASER -> addTracer(Tracer(mx, my, tx + g.facing * 240f, ty, 0.12f, 3.5f, 0xFF40E0FF.toInt()))
            WeaponType.MINIGUN -> addTracer(Tracer(mx, my, tx, ty, 0.05f, 1.8f, 0xFFFFCC80.toInt()))
            else -> addTracer(Tracer(mx, my, tx, ty, 0.06f, 1.6f, 0xFFFFF59D.toInt()))
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

        shooter = slot
        // Piercing: the next zombies further along the line of fire take most of the damage too.
        struck.clear()
        if (weapon.pierce > 1) {
            val reach = if (weapon == WeaponType.LASER) 320f else 220f
            repeat(weapon.pierce - 1) {
                var next: Zombie? = null
                var nextDist = reach
                for (k in zombies.indices) {
                    val z = zombies[k]
                    if (z.dead || z === target || z.lane != target.lane || struck.contains(z)) continue
                    if (weapon.groundOnly && z.flying) continue
                    val d = (z.x - tx) * g.facing
                    if (d > 0f && d < nextDist) { nextDist = d; next = z }
                }
                if (next != null) struck.add(next)
            }
        }
        damageZombie(target, damage, crit = crit)
        for (k in struck.indices) damageZombie(struck[k], damage * 0.8f)
        if (weapon.splashRadius > 0f) {
            for (k in zombies.indices) {
                val z = zombies[k]
                if (z.dead || z === target || z.lane != target.lane) continue
                if (weapon.groundOnly && z.flying) continue
                if (abs(z.x - tx) <= weapon.splashRadius) damageZombie(z, damage * 0.6f)
            }
        }
        shooter = -1
    }

    // ---------------------------------------------------------------- weapon switching

    /** Tap-to-cycle used by the in-run switcher: AUTO, then each owned weapon, then AUTO again. */
    fun cycleWeapon(slot: Int) {
        remote?.let { if (!over) it.act(GuestAction.CYCLE_WEAPON, slot); return }
        if (over || slot !in gunners.indices) return
        val g = gunners[slot]
        val owned = crew[slot].ownedWeapons
        val next = if (g.mode == AUTO_WEAPON) owned.first().ordinal else owned.firstOrNull { it.ordinal > g.mode }?.ordinal ?: AUTO_WEAPON
        g.mode = next
        g.autoCheck = 0f
        if (next != AUTO_WEAPON) equip(slot, WeaponType.entries[next])
    }

    private fun equip(slot: Int, weapon: WeaponType) {
        val g = gunners[slot]
        if (g.weapon == weapon) return
        g.weapon = weapon
        g.cooldown = max(g.cooldown, 0.35f) // swapping takes a moment, so flip-flopping has a cost
        if (time > 0.5f && texts.size < MAX_TEXTS) addText(FloatingText(gunX(slot), gunY(slot) - 66f, weapon.displayName, 0.9f, 0xFFFFFFFF.toInt(), 18f))
    }

    private fun updateLoadout(slot: Int, dt: Float) {
        val g = gunners[slot]
        if (g.mode != AUTO_WEAPON) {
            equip(slot, WeaponType.entries[g.mode])
            return
        }
        g.autoCheck -= dt
        if (g.autoCheck > 0f) return
        g.autoCheck = 0.3f
        val owned = crew[slot].ownedWeapons
        if (owned.size < 2) return
        var best = g.weapon
        var bestScore = 0f
        var currentScore = 0f
        for (k in owned.indices) {
            val w = owned[k]
            val score = weaponScore(slot, w)
            if (w == g.weapon) currentScore = score
            if (score > bestScore) { bestScore = score; best = w }
        }
        // Stick with the current weapon unless another is clearly better here.
        if (best != g.weapon && bestScore > currentScore * 1.2f) equip(slot, best)
    }

    /**
     * Estimated damage per second [weapon] would deal from [slot] right now: nearest target in range
     * (shots are capped at the target's remaining health, so big guns are wasted on weak zombies), plus
     * what its splash or piercing would add on neighbouring zombies.
     */
    private fun weaponScore(slot: Int, weapon: WeaponType): Float {
        if (gunners[slot].overheated[weapon.ordinal]) return 0f
        val gx = gunX(slot)
        var target: Zombie? = null
        var nearest = weapon.range * rangePerk[slot] * biome.rangeMult
        for (k in zombies.indices) {
            val z = zombies[k]
            if (z.dead || z.x < -10f || z.x > worldW + 10f) continue
            if (weapon.groundOnly && z.flying) continue
            val d = reachDistance(slot, z)
            if (d < nearest) { nearest = d; target = z }
        }
        if (target == null) return 0f
        val damage = damageFor(slot, weapon)
        var total = min(armored(damage, target) * gunFactor(target), target.hp + target.shield)
        if (weapon.splashRadius > 0f || weapon.pierce > 1) {
            val dir = if (target.x >= gx) 1 else -1
            var pierced = 0
            for (k in zombies.indices) {
                val o = zombies[k]
                if (o === target || o.dead || o.x < -10f || o.x > worldW + 10f) continue
                if (weapon.groundOnly && o.flying) continue
                val dx = o.x - target.x
                if (weapon.splashRadius > 0f && abs(dx) <= weapon.splashRadius) {
                    total += 0.6f * min(armored(damage, o) * gunFactor(o), o.hp + o.shield)
                } else if (weapon.pierce > 1 && pierced < weapon.pierce - 1 && dx * dir > 0f && abs(dx) < 260f) {
                    total += 0.8f * min(armored(damage, o) * gunFactor(o), o.hp + o.shield)
                    pierced++
                }
            }
        }
        // Prefer to rotate away from a weapon that is about to overheat.
        val hotFactor = if (gunners[slot].heat[weapon.ordinal] > 0.8f) 0.6f else 1f
        return total / weapon.interval * hotFactor
    }

    // ---------------------------------------------------------------- train-mounted weapons

    private fun updateTrainWeapons(dt: Float) {
        val boost = if (overdriveTime > 0f) 1.3f else 1f
        flakFlash = max(0f, flakFlash - dt)
        teslaFlash = max(0f, teslaFlash - dt)
        flameFlash = max(0f, flameFlash - dt)
        if (flakLevel > 0) {
            flakCooldown -= dt * boost
            if (flakCooldown <= 0f) fireFlak()
        }
        if (teslaLevel > 0) {
            teslaCooldown -= dt * boost
            if (teslaCooldown <= 0f) zapChain()
        }
        if (flameLevel > 0) {
            flameCooldown -= dt * boost
            if (flameCooldown <= 0f) burnFront()
        }
        if (mortarLevel > 0) {
            mortarCooldown -= dt * boost
            if (mortarCooldown <= 0f) fireMortar()
        }
    }

    private fun fireFlak() {
        var target: Zombie? = null
        var best = 560f
        for (k in zombies.indices) {
            val z = zombies[k]
            if (z.dead || !z.flying || z.x < -10f || z.x > worldW + 10f) continue
            val d = abs(z.x - flakX)
            if (d < best) { best = d; target = z }
        }
        if (target == null) {
            if (shootHazard()) {
                flakCooldown = max(0.5f, 1.2f - 0.1f * flakLevel)
                return
            }
            flakCooldown = 0.15f
            return
        }
        flakCooldown = max(0.5f, 1.2f - 0.1f * flakLevel)
        flakFlash = 0.1f
        val tx = target.x
        val ty = targetY(target)
        val dmg = (40f + 25f * flakLevel) * (if (buffTime > 0f) 1.3f else 1f)
        addTracer(Tracer(flakX, flakY - 26f, tx, ty, 0.12f, 3.5f, 0xFFFFD54F.toInt()))
        explosion(tx, ty, 30f)
        sound(TrainSoundCue.FLAK)
        shooter = -1
        damageZombie(target, dmg)
        for (k in zombies.indices) {
            val z = zombies[k]
            if (z !== target && !z.dead && z.flying && abs(z.x - tx) < 90f) damageZombie(z, dmg * 0.6f)
        }
    }

    /** Tesla coil: lightning leaps from the coil to the nearest zombie, then chains to neighbours. */
    private fun zapChain() {
        var from: Zombie? = null
        var best = 520f
        for (k in zombies.indices) {
            val z = zombies[k]
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
        val damage = (25f + 18f * teslaLevel) * (if (buffTime > 0f) 1.3f else 1f)
        scratch.clear()
        var px = teslaX
        var py = teslaY - 34f
        var current: Zombie? = from
        while (current != null && scratch.size < jumps) {
            val ty = targetY(current)
            addTracer(Tracer(px, py, current.x, ty, 0.2f, 2.6f, 0xFF9FE8FF.toInt(), jagged = true))
            scratch.add(current)
            px = current.x
            py = ty
            val here = current
            var next: Zombie? = null
            var nextDist = 170f
            for (k in zombies.indices) {
                val z = zombies[k]
                if (z.dead || scratch.contains(z) || abs(z.y - here.y) > 320f) continue
                val d = abs(z.x - here.x)
                if (d < nextDist) { nextDist = d; next = z }
            }
            current = next
        }
        shooter = -1
        for (k in scratch.indices) {
            val z = scratch[k]
            spark(z.x, targetY(z))
            damageZombie(z, damage)
        }
    }

    /** Flamethrower: scorches ground zombies in front of the cowcatcher a few times a second. */
    private fun burnFront() {
        var any = false
        val dmg = (5f + 3.5f * flameLevel) * (if (buffTime > 0f) 1.3f else 1f)
        shooter = -1
        for (k in zombies.indices) {
            val z = zombies[k]
            if (z.dead || z.lane != lane || z.flying || z.type.fireImmune || z.x < flameX - 12f || z.x > flameX + flameReach) continue
            any = true
            damageZombie(z, dmg)
            if (rng.nextFloat() < 0.3f) spark(z.x, z.y - 20f)
        }
        if (any) {
            flameFlash = 0.2f
            flameCooldown = 0.12f
        } else {
            flameCooldown = 0.15f
        }
    }

    private fun fireMortar() {
        var target: Zombie? = null
        var bestCount = 0
        for (k in zombies.indices) {
            val z = zombies[k]
            if (z.dead || z.lane != lane || z.flying || z.x < 20f || z.x > worldW - 20f || abs(z.x - mortarX) > 650f || abs(z.x - mortarX) < 70f) continue
            var count = 0
            for (m in zombies.indices) {
                val o = zombies[m]
                if (!o.dead && o.lane == lane && !o.flying && abs(o.x - z.x) < 110f) count++
            }
            if (count > bestCount) { bestCount = count; target = z }
        }
        if (target == null) {
            mortarCooldown = 0.3f
            return
        }
        mortarCooldown = max(1.8f, 4.2f - 0.35f * mortarLevel)
        val sx = mortarX
        val sy = mortarY - 18f
        val gravity = SHELL_GRAVITY * biome.gravityMult
        val flight = 0.9f / kotlin.math.sqrt(biome.gravityMult.coerceAtLeast(0.2f))
        shells.add(Shell(sx, sy, (target.x - sx) / flight, (target.y - 10f - sy) / flight - 0.5f * gravity * flight, target.x, target.y - 10f, flight, gravity))
        sound(TrainSoundCue.MORTAR)
        spark(sx, sy)
    }

    private fun updateShells(dt: Float) {
        var i = 0
        while (i < shells.size) {
            val s = shells[i]
            s.age += dt
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.vy += s.gravity * dt
            if (s.age >= s.total) {
                detonateShell(s)
                shells[i] = shells[shells.size - 1]
                shells.removeAt(shells.size - 1)
            } else {
                i++
            }
        }
    }

    private fun detonateShell(s: Shell) {
        explosion(s.tx, s.ty, 50f)
        shake = max(shake, 0.3f)
        val dmg = (70f + 45f * mortarLevel) * (if (buffTime > 0f) 1.3f else 1f)
        shooter = -1
        for (k in zombies.indices) {
            val z = zombies[k]
            if (!z.dead && z.lane == lane && !z.flying && abs(z.x - s.tx) <= 130f) damageZombie(z, dmg)
        }
    }

    // ---------------------------------------------------------------- repair crew

    internal fun updateRepairers(dt: Float) {
        if (repairers.isEmpty()) return
        var healing = hp < maxHp - 0.5f
        for (i in 1 until slotCount) if (carHp[i] > 0.01f && carHp[i] < carMaxHp[i] - 0.5f) healing = true
        val left = trainLeft + 12f
        val right = frontEnd - 36f
        healTextGap -= dt
        for (i in repairers.indices) {
            val r = repairers[i]
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
                if (healing && healTextGap <= 0f && texts.size < MAX_TEXTS) {
                    healTextGap = 1.1f
                    addText(FloatingText(r.x, repairBoardY - 44f, "+" + "%.1f".format(regenPerSecond), 0.9f, 0xFF66FF8A.toInt(), 18f))
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

    // ---------------------------------------------------------------- effects

    /** Ends the run immediately (the pause menu's "End run"); healing can't undo it. */
    fun forceEnd() {
        if (over) return
        hp = 0f
        endRun()
    }

    private fun endRun() {
        over = true
        onCoop?.invoke(CoopSignal.TrainDestroyed)
        hp = 0f
        shake = 1f
        sound(TrainSoundCue.GAME_OVER)
        var x = trainLeft
        while (x < trainLeft + trainLength) {
            explosion(x, 440f, 40f)
            x += 55f
        }
    }

    internal fun emitSmoke(dt: Float) {
        smokeAccumulator += dt
        if (smokeAccumulator < 0.1f) return
        smokeAccumulator = 0f
        addParticle(Particle(locoLeft + shape.stackX, shape.locoRoofY - 8f, -50f - rng.nextFloat() * 20f, -45f, 1.8f, 1.8f, 9f, (if (biome == Biome.MOON || biome == Biome.ORBITAL) 0xFF80D8FF else 0xFFB0B0B0).toInt(), ParticleKind.SMOKE))
    }

    internal fun updateEffects(dt: Float) {
        var i = 0
        while (i < particles.size) {
            val p = particles[i]
            p.life -= dt
            if (p.life <= 0f) {
                particles[i] = particles[particles.size - 1]
                particles.removeAt(particles.size - 1)
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            when (p.kind) {
                ParticleKind.BLOOD, ParticleKind.DEBRIS, ParticleKind.COIN -> p.vy += 520f * biome.gravityMult * dt
                ParticleKind.SMOKE -> { p.vx -= 6f * dt; p.vy -= 4f * dt }
                else -> {}
            }
            i++
        }
        i = 0
        while (i < tracers.size) {
            val t = tracers[i]
            t.life -= dt
            if (t.life <= 0f) {
                tracers[i] = tracers[tracers.size - 1]
                tracers.removeAt(tracers.size - 1)
            } else i++
        }
        i = 0
        while (i < texts.size) {
            val t = texts[i]
            t.life -= dt
            t.y -= 36f * dt
            if (t.life <= 0f) {
                texts[i] = texts[texts.size - 1]
                texts.removeAt(texts.size - 1)
            } else i++
        }
    }

    internal fun addParticle(p: Particle) {
        p.lane = fxLane
        if (particles.size < MAX_PARTICLES) particles.add(p)
    }

    internal fun addTracer(t: Tracer) {
        t.lane = fxLane
        tracers.add(t)
        if (fx.recording) fx.add(FxLog.TRACER, fxLane, t.x1, t.y1, t.x2, t.y2, t.width, t.color, (t.life * 100f).toInt() * 2 + (if (t.jagged) 1 else 0))
    }

    internal fun addText(t: FloatingText) {
        t.lane = fxLane
        texts.add(t)
        if (fx.recording) fx.add(FxLog.TEXT, fxLane, t.x, t.y, t.life, 0f, t.size, t.color, 0, t.text)
    }

    internal fun confetti(x: Float, y: Float, count: Int) {
        if (fx.recording) fx.add(FxLog.CONFETTI, fxLane, x, y, count0 = count)
        val colors = intArrayOf(0xFFFF4081.toInt(), 0xFFFFEB3B.toInt(), 0xFF40C4FF.toInt(), 0xFF69F0AE.toInt(), 0xFFFF9100.toInt())
        repeat(count) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            val s = 70f + rng.nextFloat() * 200f
            addParticle(Particle(x, y, cos(a) * s, sin(a) * s - 120f, 1.1f, 1.1f, 2.5f + rng.nextFloat() * 2f, colors[rng.nextInt(colors.size)], ParticleKind.DEBRIS))
        }
    }

    internal fun spark(x: Float, y: Float) {
        if (fx.recording) fx.add(FxLog.SPARK, fxLane, x, y)
        repeat(2) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            addParticle(Particle(x, y, cos(a) * 120f, sin(a) * 120f, 0.18f, 0.18f, 2.5f, 0xFFFFE082.toInt(), ParticleKind.SPARK))
        }
    }

    internal fun greenBurst(x: Float, y: Float, count: Int) {
        if (fx.recording) fx.add(FxLog.GREEN_BURST, fxLane, x, y, count0 = count)
        repeat(count) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            val s = 60f + rng.nextFloat() * 180f
            val color = if (rng.nextBoolean()) 0xFF6BA832.toInt() else 0xFF8E2A2A.toInt()
            addParticle(Particle(x, y, cos(a) * s, sin(a) * s - 90f, 0.7f, 0.7f, 2f + rng.nextFloat() * 3f, color, ParticleKind.BLOOD))
        }
    }

    internal fun explosion(x: Float, y: Float, radius: Float) {
        if (fx.recording) fx.add(FxLog.EXPLOSION, fxLane, x, y, width = radius)
        addParticle(Particle(x, y, 0f, 0f, 0.35f, 0.35f, radius, 0xFFFF8F00.toInt(), ParticleKind.FIRE))
        repeat(7) {
            val a = rng.nextFloat() * 2f * PI.toFloat()
            val s = 80f + rng.nextFloat() * 200f
            addParticle(Particle(x, y, cos(a) * s, sin(a) * s - 60f, 0.6f, 0.6f, 3f + rng.nextFloat() * 3f, 0xFF4E342E.toInt(), ParticleKind.DEBRIS))
        }
        if (shakeEnabled) shake = max(shake, 0.25f)
    }

    internal fun sound(cue: TrainSoundCue) {
        if (fx.recording) fx.add(FxLog.SOUND, fxLane, 0f, 0f, count0 = cue.ordinal)
        onSound?.invoke(cue)
    }

    // ---------------------------------------------------------------- debug hooks (used by the developer tools)

    fun debugSpawnElite(trait: EliteTrait) {
        val side = if (rng.nextBoolean()) 1 else -1
        val z = addZombie(ZombieType.BRUTE, if (side > 0) worldW + 40f else -40f, RAIL_Y + 30f, side)
        applyElite(z, trait, null)
    }

    fun debugSpawn(type: ZombieType, count: Int) {
        repeat(count) { if (zombies.size < zombieCap) spawnZombie(type) }
    }

    fun debugKillAll() {
        shooter = -1
        for (i in zombies.indices) {
            val z = zombies[i]
            if (!z.dead) damageZombie(z, z.hp + 1f)
        }
    }

    fun debugHeal() {
        hp = maxHp
        for (i in 1 until slotCount) {
            carHp[i] = carMaxHp[i]
            gunners[i].alive = true
        }
    }

    fun debugAddCoins(amount: Int) {
        runCoins += amount
    }

    fun debugSkipTime(seconds: Float) {
        time += seconds
        distance += 14f * seconds
    }

    fun debugCrate() {
        crate = Crate(worldW * 0.5f, -50f)
    }

    fun debugHorde() {
        if (hordeTime <= 0f && !hordePending) nextHordeAt = 0f
    }

    fun debugResetCooldowns() {
        dynamiteCooldown = 0f
        overdriveCooldown = 0f
        fieldRepairCooldown = 0f
    }
}
