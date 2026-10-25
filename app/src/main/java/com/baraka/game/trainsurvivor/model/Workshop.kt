package com.baraka.game.trainsurvivor.model

import java.util.Random
import kotlin.math.roundToInt

/** The special trait a crafted weapon rolled (model/Workshop.kt combines this with an archetype and an element). */
enum class WeaponEffect(val label: String) {
    NONE("Standard"),
    OVERCHARGED("Overcharged: +2 pierce"),
    VOLATILE("Volatile: adds splash"),
    PRECISE("Precise: +40% range")
}

/** The weapon's firing shape: how its dps is split between hit size and rate - one of the parts shown in the Workshop screen. */
enum class WeaponArchetype(val displayName: String, val blurb: String) {
    SIDEARM("Sidearm", "Balanced damage and rate"),
    RAPID("Rapid", "Many weak, fast shots"),
    HEAVY("Heavy", "Few, hard-hitting shots"),
    MARKSMAN("Marksman", "Moderate rate, long reach")
}

/** One procedurally-crafted weapon (a standalone turret - see TrainGame.workshop - not a passenger's gun),
 * plus the parts that were combined to make it: an [archetype], an element ([kind]), an [effect], and
 * optionally [inspiredBy] a random passenger, whose own perk (model/Survivors.kt) becomes a bonus here. */
class CraftedWeapon(
    val name: String,
    val damage: Float,
    val interval: Float,
    val range: Float,
    val pierce: Int,
    val splashRadius: Float,
    val kind: DamageKind,
    val groundOnly: Boolean,
    val effect: WeaponEffect,
    val archetype: WeaponArchetype,
    val inspiredBy: Survivor?,
    /** Chance (0 = never) for a shot to deal 2.5x damage - only set when inspired by a CRIT passenger. */
    val critChance: Float = 0f,
    /** Coin multiplier on its own kills (1 = none) - only set when inspired by a SCAVENGER passenger. */
    val coinBonus: Float = 1f
)

private val WORKSHOP_PREFIXES = arrayOf(
    "Salvaged", "Tinkered", "Jury-rigged", "Reforged", "Scavenged", "Homebrew", "Custom", "Patchwork",
    "Overclocked", "Improvised", "Rebuilt", "Surplus", "Experimental", "Prototype", "Backlot", "Contraband"
)
private val WORKSHOP_NOUNS = arrayOf(
    "Blaster", "Repeater", "Cannon", "Carbine", "Striker", "Flinger", "Ripper", "Howitzer",
    "Peacemaker", "Thumper", "Needler", "Disruptor", "Smasher", "Lancer", "Hailer", "Render"
)

/**
 * The weapon crafted on [day] (see Progression.today): combines a firing archetype, an element, an
 * effect and - about half the time - the inspiration of a random passenger's perk, all resolved purely
 * from the day number, so the same day always gives the same weapon (stable to save and compare), yet a
 * brand new combination exists every day forever - nothing here needs the app to ever be updated. Every
 * stat is clamped between Pistol-grade and Laser-grade (see [WeaponType]), so a roll can never be
 * absurdly over- or under-powered: a bad roll is just not worth keeping (scrap it).
 */
fun craftedWeaponForDay(day: Long): CraftedWeapon {
    val random = Random(day * 48271L + 101L)
    val archetype = WeaponArchetype.entries[random.nextInt(WeaponArchetype.entries.size)]
    val kind = DamageKind.entries[random.nextInt(DamageKind.entries.size)]
    val effect = WeaponEffect.entries[random.nextInt(WeaponEffect.entries.size)]
    val inspiredBy = if (random.nextFloat() < 0.5f) null else SURVIVORS[random.nextInt(SURVIVORS.size)]

    // tier 0 lands around a Pistol's dps, tier 1 around a Laser's - every roll sits somewhere on that curve.
    val tier = random.nextFloat()
    val baseDps = 18f + tier * 150f
    val (dpsFactor, rateFactor) = when (archetype) {
        WeaponArchetype.SIDEARM -> 1f to 1f
        WeaponArchetype.RAPID -> 0.6f to 2.6f
        WeaponArchetype.HEAVY -> 2.2f to 0.4f
        WeaponArchetype.MARKSMAN -> 1.4f to 0.7f
    }
    var interval = (0.5f / rateFactor).coerceIn(0.05f, 1.6f)
    var damage = baseDps * dpsFactor * interval
    val baseRange = 260f + tier * 320f
    var range = if (effect == WeaponEffect.PRECISE) baseRange * 1.4f else baseRange
    val pierce = 1 + (if (effect == WeaponEffect.OVERCHARGED) 2 else 0) + (tier * 3f).roundToInt().coerceIn(0, 4)
    val splash = if (effect == WeaponEffect.VOLATILE) 50f + tier * 60f else 0f

    // The inspiration layers one more bonus on top, the same size as that passenger's own perk.
    var critChance = 0f
    var coinBonus = 1f
    when (inspiredBy?.perk) {
        Perk.DAMAGE -> damage *= 1.15f
        Perk.FIRE_RATE -> interval = (interval / 1.2f).coerceAtLeast(0.05f)
        Perk.RANGE -> range *= 1.25f
        Perk.CRIT -> critChance = 0.12f
        Perk.SCAVENGER -> coinBonus = 1.25f
        null -> {}
    }

    val name = "${WORKSHOP_PREFIXES[random.nextInt(WORKSHOP_PREFIXES.size)]} ${WORKSHOP_NOUNS[random.nextInt(WORKSHOP_NOUNS.size)]}"
    return CraftedWeapon(
        name, damage, interval, range, pierce, splash, kind, groundOnly = false,
        effect = effect, archetype = archetype, inspiredBy = inspiredBy, critChance = critChance, coinBonus = coinBonus
    )
}
