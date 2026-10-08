package com.baraka.game.trainsurvivor.model

enum class Perk { DAMAGE, FIRE_RATE, RANGE, CRIT, SCAVENGER }

/** Headwear a survivor can wear; drawn in render/TrainRenderer.kt (add a branch there for a new style). */
enum class HatStyle { NONE, CAP, COWBOY, HELMET, BANDANA, BEANIE }

/** How a survivor looks: all colours are ARGB. */
class AvatarLook(
    val shirt: Long,
    val pants: Long = 0xFF37474F,
    val skin: Long = 0xFFE0AC69,
    val hair: Long = 0xFF3E2723,
    val hat: HatStyle = HatStyle.NONE,
    val hatColor: Long = 0xFF455A64
)

/** A named survivor: one per seat, with a passive perk and a look. */
class Survivor(val name: String, val role: String, val perk: Perk, val perkText: String, val look: AvatarLook)

/**
 * The crew, in seat order. To add a survivor, add an entry: the train gets one more seat (seat prices,
 * garage cards, the in-run switcher, saving and the debug tools all follow `SURVIVORS.size`).
 */
val SURVIVORS = listOf(
    Survivor("Cole", "Driver", Perk.DAMAGE, "Steady Hands: +15% damage", AvatarLook(shirt = 0xFFD84315, hair = 0xFF3E2723, hat = HatStyle.CAP, hatColor = 0xFF263238)),
    Survivor("Mara", "Gunner", Perk.FIRE_RATE, "Quick Draw: +20% fire rate", AvatarLook(shirt = 0xFF1E88E5, hair = 0xFF212121, hat = HatStyle.BANDANA, hatColor = 0xFFE53935)),
    Survivor("Hawk", "Scout", Perk.RANGE, "Eagle Eye: +25% weapon range", AvatarLook(shirt = 0xFF43A047, hair = 0xFF6D4C41, hat = HatStyle.COWBOY, hatColor = 0xFF5D4037)),
    Survivor("Doc", "Medic", Perk.CRIT, "Lucky Shots: 12% chance for x2.5 damage", AvatarLook(shirt = 0xFF8E24AA, hair = 0xFFB0BEC5, hat = HatStyle.BEANIE, hatColor = 0xFFFFFFFF)),
    Survivor("Rocco", "Scrapper", Perk.SCAVENGER, "Scavenger: +25% coins from his kills", AvatarLook(shirt = 0xFFFDD835, hair = 0xFF212121, hat = HatStyle.HELMET, hatColor = 0xFFFF8F00))
)

/** Seats on the train: one per survivor. */
val MAX_SLOTS: Int = SURVIVORS.size
