package com.baraka.game.dreadnought.model

/** Muzzle speeds in metres per second. Main shells are slow enough that you have to lead a moving target. */
const val MAIN_SHELL_SPEED = 340f
const val SEC_SHELL_SPEED = 520f
const val TORPEDO_SPEED = 62f

enum class MountKind { MAIN, SECONDARY }

/**
 * One gun mount on a hull. Ship space is metres: +x is the bow, +y the starboard side, origin amidships.
 * A turret can only swing inside its firing arc ([arcCenterDeg] +- [arcHalfDeg], measured from the bow), so a
 * forward turret cannot fire straight over the stern. The mount drives both the simulation and the art.
 */
class MountSpec(
    val x: Float,
    val y: Float,
    val kind: MountKind,
    val barrels: Int,
    /** Turret radius in metres (also scales the barrels). */
    val radius: Float,
    arcCenterDeg: Float,
    arcHalfDeg: Float
) {
    val arcCenter: Float = Math.toRadians(arcCenterDeg.toDouble()).toFloat()
    val arcHalf: Float = Math.toRadians(arcHalfDeg.toDouble()).toFloat()
}

private fun main(x: Float, y: Float = 0f, barrels: Int, r: Float, center: Float = 0f, half: Float = 150f) =
    MountSpec(x, y, MountKind.MAIN, barrels, r, center, half)

private fun sec(x: Float, y: Float, r: Float = 2.6f, barrels: Int = 2, center: Float = 0f, half: Float = 165f) =
    MountSpec(x, y, MountKind.SECONDARY, barrels, r, center, half)

/** Hull plan as (station, half-width) pairs: station 0 = stern, 1 = bow; width 1 = full beam. */
private fun hull(vararg v: Float): FloatArray = v

/**
 * Every ship in the game, the player's included. Behaviour comes from the numbers and flags below
 * (standoff range, tubes, diver, escorts...), the drawing from `render/ShipArts.kt`, so a new ship is one
 * entry in the companion plus one art object. Ships the player designs ([ShipDesign]) are built at run time.
 */
class ShipClass(
    val label: String,
    val role: String,
    /** Metres, bow to stern. */
    val length: Float,
    val beam: Float,
    val maxHp: Float,
    val maxSpeed: Float,
    /** Radians per second at full speed. */
    val turnRate: Float,
    val accel: Float,
    val profile: FloatArray,
    val mounts: List<MountSpec>,
    val mainDamage: Float = 0f,
    val mainReload: Float = 5f,
    val mainRange: Float = 600f,
    val secDamage: Float = 0f,
    val secReload: Float = 1f,
    val secRange: Float = 380f,
    val tubes: Int = 0,
    val torpDamage: Float = 0f,
    val torpReload: Float = 9f,
    /** Distance an enemy tries to keep from the player. */
    val standoff: Float = 400f,
    val reward: Int = 0,
    /** Cost against a wave's budget; 0 means it never spawns from the random table. */
    val cost: Int = 0,
    val minWave: Int = 1,
    /** Dives out of reach of shells and surfaces to shoot. */
    val diver: Boolean = false,
    /** Launches patrol boats now and then. */
    val escorts: Boolean = false,
    val boss: Boolean = false,
    val player: Boolean = false,
    /** Can be chosen as the player's ship in the harbour; the hero* values replace the enemy numbers when you sail it. */
    val playable: Boolean = false,
    val unlockCost: Int = 0,
    val heroHp: Float = maxHp,
    val heroMain: Float = 1f,
    val heroSec: Float = 1f,
    val heroTorp: Float = 1f,
    val heroTubes: Int = tubes,
    /** Aircraft carried (enemy) and when you sail it, and the bomb each plane drops for you. */
    val planes: Int = 0,
    val heroPlanes: Int = planes,
    val heroBomb: Float = 0f,
    val heroBlurb: String = "",
    /** Custom ships: which paint (see [Paints]) and the design they were built from. */
    val paintIndex: Int = 0,
    val design: ShipDesign? = null
) {
    val mainCount: Int = mounts.count { it.kind == MountKind.MAIN }

    companion object {
        val DREADNOUGHT = ShipClass(
        label = "Dreadnought", role = "Fast battleship", length = 270f, beam = 33f, maxHp = 1300f,
        maxSpeed = 52f, turnRate = 0.2f, accel = 9f,
        profile = hull(0f, .58f, .04f, .76f, .10f, .92f, .18f, 1f, .70f, 1f, .78f, .95f, .86f, .80f, .92f, .60f, .965f, .36f, .99f, .14f, 1f, 0f),
        mounts = listOf(
            main(82f, barrels = 3, r = 8.5f), main(48f, barrels = 3, r = 8.5f),
            main(-92f, barrels = 3, r = 8.5f, center = 180f),
            sec(34f, 12.5f, center = 90f, half = 80f), sec(2f, 13f, center = 90f, half = 80f), sec(-60f, 12f, center = 90f, half = 80f),
            sec(34f, -12.5f, center = -90f, half = 80f), sec(2f, -13f, center = -90f, half = 80f), sec(-60f, -12f, center = -90f, half = 80f)
        ),
        mainDamage = 70f, mainReload = 4.2f, mainRange = 760f, secDamage = 12f, secReload = 1.1f, secRange = 420f,
        player = true, playable = true,
        heroBlurb = "The heavy hitter. Nine big guns in three turrets and the toughest hull; slow to turn. Best for learning."
        )
        val PATROL_BOAT = ShipClass(
        label = "Patrol boat", role = "Coastal gunboat", length = 70f, beam = 10f, maxHp = 70f,
        maxSpeed = 95f, turnRate = 0.75f, accel = 22f,
        profile = hull(0f, .7f, .1f, .9f, .25f, 1f, .6f, 1f, .78f, .8f, .9f, .5f, .97f, .2f, 1f, 0f),
        mounts = listOf(sec(22f, 0f, r = 2.4f, barrels = 1, half = 170f)),
        secDamage = 4f, secReload = 1.0f, secRange = 330f,
        standoff = 260f, reward = 10, cost = 1
        )
        val TORPEDO_BOAT = ShipClass(
        label = "Torpedo boat", role = "Fast attack craft", length = 78f, beam = 9f, maxHp = 85f,
        maxSpeed = 125f, turnRate = 0.8f, accel = 28f,
        profile = hull(0f, .85f, .08f, 1f, .55f, 1f, .75f, .72f, .9f, .4f, 1f, 0f),
        mounts = listOf(sec(26f, 0f, r = 2f, barrels = 1, half = 170f), sec(-24f, 0f, r = 2f, barrels = 1, center = 180f, half = 170f)),
        secDamage = 4f, secReload = 0.8f, secRange = 300f,
        tubes = 2, torpDamage = 110f, torpReload = 8f, standoff = 340f, reward = 25, cost = 2, minWave = 3
        )
        val DESTROYER = ShipClass(
        label = "Destroyer", role = "Escort destroyer", length = 115f, beam = 12.5f, maxHp = 230f,
        maxSpeed = 85f, turnRate = 0.45f, accel = 16f,
        profile = hull(0f, .6f, .06f, .8f, .14f, .95f, .22f, 1f, .7f, 1f, .8f, .88f, .9f, .55f, .96f, .28f, 1f, 0f),
        mounts = listOf(
            main(40f, barrels = 1, r = 3.2f, half = 160f), main(27f, barrels = 1, r = 3.2f, half = 160f),
            main(-34f, barrels = 1, r = 3.2f, center = 180f, half = 160f), main(-48f, barrels = 1, r = 3.2f, center = 180f, half = 160f)
        ),
        mainDamage = 9f, mainReload = 2.8f, mainRange = 520f,
        tubes = 2, torpDamage = 110f, torpReload = 12f, standoff = 430f, reward = 45, cost = 3, minWave = 3,
        playable = true, unlockCost = 150, heroHp = 650f, heroMain = 3f, heroTorp = 2.2f,
        heroBlurb = "Fast and nimble. Four small guns, plus a TORP button for two powerful torpedoes. Dodge, then strike."
        )
        val SUBMARINE = ShipClass(
        label = "Submarine", role = "Fleet submarine", length = 95f, beam = 9f, maxHp = 160f,
        maxSpeed = 55f, turnRate = 0.35f, accel = 8f,
        profile = hull(0f, 0f, .04f, .4f, .12f, .8f, .25f, .97f, .45f, 1f, .7f, 1f, .85f, .9f, .94f, .62f, .985f, .28f, 1f, 0f),
        mounts = listOf(main(27f, barrels = 1, r = 2.3f, half = 170f)),
        mainDamage = 8f, mainReload = 3f, mainRange = 380f,
        tubes = 2, torpDamage = 150f, torpReload = 10f, standoff = 380f, reward = 60, cost = 3, minWave = 6, diver = true,
        playable = true, unlockCost = 400, heroHp = 520f, heroMain = 3f, heroTorp = 2.0f, heroTubes = 4,
        heroBlurb = "The hunter. DIVE and enemy shells cannot touch you (the battery runs down, so surface to recharge). TORP fires a fan of four."
        )
        val CRUISER = ShipClass(
        label = "Cruiser", role = "Heavy cruiser", length = 190f, beam = 20f, maxHp = 520f,
        maxSpeed = 62f, turnRate = 0.3f, accel = 11f,
        profile = hull(0f, .62f, .05f, .8f, .12f, .95f, .2f, 1f, .7f, 1f, .8f, .92f, .88f, .7f, .94f, .45f, .98f, .2f, 1f, 0f),
        mounts = listOf(
            main(58f, barrels = 3, r = 6.2f), main(33f, barrels = 3, r = 6.2f), main(-66f, barrels = 3, r = 6.2f, center = 180f),
            sec(10f, 8f, r = 2.2f, center = 90f, half = 80f), sec(-30f, 8f, r = 2.2f, center = 90f, half = 80f),
            sec(10f, -8f, r = 2.2f, center = -90f, half = 80f), sec(-30f, -8f, r = 2.2f, center = -90f, half = 80f)
        ),
        mainDamage = 26f, mainReload = 5f, mainRange = 640f, secDamage = 6f, secReload = 1.2f, secRange = 380f,
        standoff = 520f, reward = 110, cost = 6, minWave = 7,
        playable = true, unlockCost = 300, heroHp = 950f, heroMain = 1.8f, heroSec = 2f,
        heroBlurb = "The all-rounder. Nine medium guns, quicker and more agile than the battleship, with a decent hull."
        )
        val BATTLESHIP = ShipClass(
        label = "Battleship", role = "Enemy battleship", length = 250f, beam = 36f, maxHp = 820f,
        maxSpeed = 50f, turnRate = 0.2f, accel = 8f,
        profile = hull(0f, .6f, .05f, .78f, .12f, .94f, .2f, 1f, .68f, 1f, .78f, .94f, .87f, .76f, .93f, .52f, .975f, .28f, 1f, 0f),
        mounts = listOf(
            main(78f, barrels = 2, r = 8f), main(46f, barrels = 2, r = 8f),
            main(-56f, barrels = 2, r = 8f, center = 180f), main(-86f, barrels = 2, r = 8f, center = 180f),
            sec(20f, 13f, center = 90f, half = 80f), sec(-24f, 13.5f, center = 90f, half = 80f),
            sec(20f, -13f, center = -90f, half = 80f), sec(-24f, -13.5f, center = -90f, half = 80f)
        ),
        mainDamage = 48f, mainReload = 5.5f, mainRange = 700f, secDamage = 7f, secReload = 1.2f, secRange = 400f,
        standoff = 580f, reward = 180, cost = 8, minWave = 9
        )
        val CARRIER = ShipClass(
        label = "Carrier", role = "Escort carrier", length = 260f, beam = 38f, maxHp = 900f,
        maxSpeed = 42f, turnRate = 0.16f, accel = 6f,
        profile = hull(0f, .72f, .05f, .9f, .12f, 1f, .2f, 1f, .8f, 1f, .88f, .92f, .94f, .72f, .98f, .42f, 1f, .24f),
        mounts = listOf(
            sec(110f, 17f, r = 2.6f, center = 90f, half = 90f), sec(40f, -19f, r = 2.6f, center = -90f, half = 90f),
            sec(-20f, 18f, r = 2.6f, center = 90f, half = 90f), sec(-70f, -18f, r = 2.6f, center = -90f, half = 90f),
            sec(-115f, 12f, r = 2.6f, center = 180f, half = 90f), sec(-115f, -12f, r = 2.6f, center = 180f, half = 90f)
        ),
        mainRange = 1000f, secDamage = 5f, secReload = 0.8f, secRange = 380f,
        standoff = 650f, reward = 200, cost = 9, minWave = 11, escorts = true,
        planes = 2, playable = true, unlockCost = 700, heroHp = 1200f, heroSec = 2f, heroPlanes = 4, heroBomb = 55f,
        heroBlurb = "The air power. No big guns: press LAUNCH and your planes bomb the target, then land to rearm. Flak can shoot them down."
        )
        val FLAGSHIP = ShipClass(
        label = "Flagship", role = "Super-battleship", length = 300f, beam = 42f, maxHp = 2200f,
        maxSpeed = 44f, turnRate = 0.16f, accel = 7f,
        profile = hull(0f, .6f, .04f, .78f, .1f, .94f, .18f, 1f, .7f, 1f, .8f, .95f, .88f, .8f, .94f, .56f, .98f, .3f, 1f, 0f),
        mounts = listOf(
            main(92f, barrels = 3, r = 10.5f), main(52f, barrels = 3, r = 10.5f), main(-100f, barrels = 3, r = 10.5f, center = 180f),
            sec(20f, 15f, r = 3.2f, barrels = 3, center = 90f, half = 80f), sec(-30f, 15.5f, r = 3.2f, barrels = 3, center = 90f, half = 80f),
            sec(20f, -15f, r = 3.2f, barrels = 3, center = -90f, half = 80f), sec(-30f, -15.5f, r = 3.2f, barrels = 3, center = -90f, half = 80f)
        ),
        mainDamage = 60f, mainReload = 5f, mainRange = 760f, secDamage = 8f, secReload = 1f, secRange = 420f,
        standoff = 600f, reward = 800, boss = true
        )

        val entries: List<ShipClass> = listOf(DREADNOUGHT, PATROL_BOAT, TORPEDO_BOAT, DESTROYER, SUBMARINE, CRUISER, BATTLESHIP, CARRIER, FLAGSHIP)

        /** Ships the wave builder may draw from. */
        val SPAWNABLE: List<ShipClass> = entries.filter { it.cost > 0 && !it.boss && !it.player }
        val ENEMIES: List<ShipClass> = entries.filter { !it.player }

        private val builtinPlayable: List<ShipClass> = entries.filter { it.playable }
        val BUILTIN_PLAYABLE_COUNT: Int = builtinPlayable.size
        private var playableCache: List<ShipClass> = builtinPlayable

        /** The ships the player can choose, in the order the harbour lists them: the built-in ones, then their own designs. */
        val PLAYABLE: List<ShipClass> get() = playableCache

        /** Sets the ships built from the player's designs (call whenever the designs change). */
        fun setCustom(list: List<ShipClass>) {
            playableCache = builtinPlayable + list
        }
    }
}
