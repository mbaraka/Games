package com.baraka.game.trainsurvivor.model

enum class Perk { DAMAGE, FIRE_RATE, RANGE, CRIT, SCAVENGER }

/** Headwear a survivor can wear; drawn in render/TrainRenderer.kt (add a branch there for a new style). */
enum class HatStyle { NONE, CAP, COWBOY, HELMET, BANDANA, BEANIE }

/** The kind of body a survivor has; drawn in render/TrainRenderer.kt (add a branch in drawAvatarBody for a new kind). */
enum class AvatarBody { HUMAN, ROBOT }

/** Extra gear drawn on a human body. */
enum class AvatarExtra { NONE, GOGGLES, PONCHO, WELDER }

/**
 * How a survivor looks: all colours are ARGB. [width] and [height] scale the body (1 = standard), so the crew
 * has different silhouettes; [accent] colours the extra gear (or a robot's chest cross).
 */
class AvatarLook(
    val shirt: Long,
    val pants: Long = 0xFF37474F,
    val skin: Long = 0xFFE0AC69,
    val hair: Long = 0xFF3E2723,
    val hat: HatStyle = HatStyle.NONE,
    val hatColor: Long = 0xFF455A64,
    val width: Float = 1f,
    val height: Float = 1f,
    val body: AvatarBody = AvatarBody.HUMAN,
    val extra: AvatarExtra = AvatarExtra.NONE,
    val accent: Long = 0xFFFFFFFF
)

/** A named survivor: one per seat, with a passive perk and a look. */
class Survivor(val name: String, val role: String, val perk: Perk, val perkText: String, val look: AvatarLook)

/**
 * The crew, in seat order. To add a survivor, add an entry: the train gets one more seat (seat prices,
 * garage cards, the in-run switcher, saving and the debug tools all follow `SURVIVORS.size`).
 */
val SURVIVORS = listOf(
    Survivor("Cole", "Driver", Perk.DAMAGE, "Steady Hands: +15% damage", AvatarLook(shirt = 0xFFD84315, hair = 0xFF3E2723, hat = HatStyle.CAP, hatColor = 0xFF263238)),
    Survivor("Mara", "Gunner", Perk.FIRE_RATE, "Quick Draw: +20% fire rate", AvatarLook(shirt = 0xFF1E88E5, pants = 0xFF4E342E, skin = 0xFFC68642, hair = 0xFF212121, hat = HatStyle.BANDANA, hatColor = 0xFFE53935, width = 0.9f, height = 0.94f, extra = AvatarExtra.GOGGLES)),
    Survivor("Hawk", "Scout", Perk.RANGE, "Eagle Eye: +25% weapon range", AvatarLook(shirt = 0xFF43A047, pants = 0xFF5D4037, skin = 0xFF8D5524, hair = 0xFF6D4C41, hat = HatStyle.COWBOY, hatColor = 0xFF5D4037, width = 0.95f, height = 1.08f, extra = AvatarExtra.PONCHO, accent = 0xFF8D6E63)),
    Survivor("Doc", "Medic", Perk.CRIT, "Lucky Shots: 12% chance for x2.5 damage", AvatarLook(shirt = 0xFFECEFF1, pants = 0xFF455A64, skin = 0xFFCFD8DC, width = 1.1f, body = AvatarBody.ROBOT, accent = 0xFFE53935)),
    Survivor("Rocco", "Scrapper", Perk.SCAVENGER, "Scavenger: +25% coins from his kills", AvatarLook(shirt = 0xFFFDD835, pants = 0xFF33691E, hair = 0xFF212121, hat = HatStyle.HELMET, hatColor = 0xFFFF8F00, width = 1.3f, extra = AvatarExtra.WELDER))
)

/** Seats on the train: one per survivor. */
val MAX_SLOTS: Int = SURVIVORS.size
