package com.baraka.game.trainsurvivor.model

/**
 * What kind of damage a hit is. Every weapon, train weapon and ability has one ([WeaponType.kind]); most guns are plain
 * [PHYSICAL]. Elemental enemies (see [Element]) take more or less depending on it.
 */
enum class DamageKind(val displayName: String) {
    PHYSICAL("Physical"),
    FIRE("Fire"),
    ELECTRIC("Electric"),
    EARTH("Earth")
}

/**
 * The element of an enemy from the Elemental Wilds. [weak] kinds hurt it 75% more, [resists] kinds 50% less, and
 * [physical] scales plain bullets (a spirit barely feels them). To add an element: add an entry here, give it a colour and a
 * symbol, and use it on a ZombieType (`element = ...`); the badge, damage numbers and the auto-weapon choice follow.
 */
enum class Element(
    val displayName: String,
    val symbol: String,
    val color: Long,
    val weak: Set<DamageKind>,
    val resists: Set<DamageKind>,
    val physical: Float = 1f
) {
    FIRE("Fire", "F", 0xFFFF7043, weak = setOf(DamageKind.EARTH), resists = setOf(DamageKind.FIRE)),
    WATER("Water", "W", 0xFF29B6F6, weak = setOf(DamageKind.ELECTRIC), resists = setOf(DamageKind.FIRE), physical = 0.8f),
    GRASS("Grass", "G", 0xFF66BB6A, weak = setOf(DamageKind.FIRE), resists = setOf(DamageKind.EARTH)),
    ELECTRIC("Electric", "Z", 0xFFFFEE58, weak = setOf(DamageKind.EARTH), resists = setOf(DamageKind.ELECTRIC)),
    EARTH("Earth", "R", 0xFF8D6E63, weak = setOf(DamageKind.FIRE), resists = setOf(DamageKind.EARTH), physical = 0.7f),
    SPIRIT("Spirit", "S", 0xFFB39DDB, weak = setOf(DamageKind.ELECTRIC, DamageKind.FIRE), resists = emptySet(), physical = 0.4f);

    /** How much a hit of [kind] is worth against this element (1 = normal). */
    fun multiplier(kind: DamageKind): Float = when {
        kind in weak -> 1.75f
        kind in resists -> 0.5f
        kind == DamageKind.PHYSICAL -> physical
        else -> 1f
    }

    /** One line for the Field Guide: what it is weak to and what it shrugs off. */
    val summary: String
        get() = "Weak to " + weak.joinToString(" and ") { it.displayName.lowercase() } +
            (if (resists.isNotEmpty()) "; resists " + resists.joinToString(" and ") { it.displayName.lowercase() } else "") +
            (if (physical < 1f) "; plain bullets do only ${(physical * 100).toInt()}%" else "")
}
