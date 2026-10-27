package com.baraka.game.dreadnought.model

/**
 * A ship upgrade. The same list is used twice: bought for coins in the harbour (permanent levels) and offered
 * three at a time between waves (one extra level for the rest of the sortie). [blurb] says in plain words what it
 * does, [stat] turns a level into the real number it gives (so the screens can show "now -> next"). Add an entry,
 * then read `game.level(Refit.X)` where its effect belongs, keeping [stat] in step with that formula.
 */
enum class Refit(
    val label: String,
    val blurb: String,
    val baseCost: Int,
    /** The number a level gives for a ship of the given class, as text for the screens. */
    val stat: (ShipClass, Int) -> String,
    val maxLevel: Int = 5
) {
    MAIN_DAMAGE("Heavy shells", "Your big shells hit harder (carrier: plane bombs).", 60, { c, l -> if (c.heroPlanes > 0 && c.mainCount == 0) "${(c.heroBomb * (1f + 0.12f * l)).toInt()} dmg/bomb" else "${(c.mainDamage * c.heroMain * (1f + 0.12f * l)).toInt()} dmg/shell" }),
    RELOAD("Autoloaders", "Big guns reload faster (carrier: planes rearm faster).", 70, { c, l -> if (c.heroPlanes > 0 && c.mainCount == 0) "%.1fs rearm".format(3.5f * (1f - 0.08f * l)) else "%.1fs reload".format(c.mainReload * (1f - 0.08f * l)) }),
    FIRE_CONTROL("Fire control", "Shells land closer to where you aim (carrier: bombs land closer).", 60, { c, l -> if (c.heroPlanes > 0 && c.mainCount == 0) "%.0fm scatter".format(14f * (1f - 0.14f * l)) else "%.1f%% scatter".format(3.5f * (1f - 0.14f * l)) }),
    RANGE("Rangefinders", "Big guns reach further (carrier: planes strike from further).", 45, { c, l -> "${(c.mainRange * (1f + 0.07f * l)).toInt()}m range" }),
    ARMOR("Belt armour", "More hull points: the ship survives more hits.", 75, { c, l -> "${(c.heroHp * (1f + 0.14f * l)).toInt()} HP" }),
    ENGINES("Engines", "A faster ship that also turns quicker; good for dodging torpedoes.", 55, { c, l -> "${(c.maxSpeed * (1f + 0.07f * l)).toInt()} m/s" }),
    SECONDARIES("Secondary guns", "The small side guns shoot nearby ships by themselves, and hit harder.", 50, { c, l -> "${(c.secDamage * c.heroSec * (1f + 0.18f * l)).toInt()} dmg/shell" }),
    DAMAGE_CONTROL("Damage control", "The REPAIR button heals more and is ready again sooner.", 65, { _, l -> "${28 + 4 * l}% / ${(REPAIR_COOLDOWN - 4f * l).toInt()}s" });

    /** Price of the next level when [level] have been bought. */
    fun cost(level: Int): Int = baseCost * (level + 1)

    companion object {
        const val REPAIR_COOLDOWN = 35f
    }
}

/**
 * A sea to fight on. Palettes are ARGB; later seas unlock by reaching a wave in any sortie. Each sea looks and feels
 * different, all from data: how big and fast the swell is, foam crests, sun glitter, coral shallows, ice floes and
 * icebergs, and the weather laid over the whole scene (rain, snow, fog, lightning).
 */
enum class Theatre(
    val label: String,
    val blurb: String,
    val unlockWave: Int,
    val deep: Long,
    val shallow: Long,
    val swell: Long,
    val ice: Boolean = false,
    val bergs: Boolean = false,
    /** Swell size and speed (1 = normal), foam crests and sun glitter (0..1), coral shallows. */
    val swellScale: Float = 1f,
    val swellSpeed: Float = 1f,
    val whitecaps: Float = 0f,
    val glitter: Float = 0f,
    val coral: Boolean = false,
    /** Weather: rain and snow density (0..1), fog colour (ARGB, 0 = clear), lightning flashes. */
    val rain: Float = 0f,
    val snow: Float = 0f,
    val fog: Long = 0L,
    val lightning: Boolean = false
) {
    ATLANTIC(
        "North Atlantic", "A storm: heavy grey swell, white-capped waves, driving rain, fog and lightning.", 0,
        0xFF182B3A, 0xFF2A4658, 0xFFA3B9C6,
        swellScale = 1.7f, swellSpeed = 1.9f, whitecaps = 0.9f, rain = 0.8f, fog = 0x4075909F, lightning = true
    ),
    PACIFIC(
        "Pacific", "Calm turquoise water, bright sun glitter and coral shallows.", 5,
        0xFF0A5C8C, 0xFF19A3C7, 0xFFCDF1FF,
        swellScale = 0.8f, swellSpeed = 0.7f, whitecaps = 0.12f, glitter = 1f, coral = true
    ),
    ARCTIC(
        "Arctic", "Black water, ice floes, towering icebergs and drifting snow.", 10,
        0xFF0E2231, 0xFF1B3B50, 0xFFB6D4E6,
        ice = true, bergs = true, swellScale = 1.2f, swellSpeed = 0.5f, snow = 0.8f, fog = 0x33BFDCEB
    )
}
