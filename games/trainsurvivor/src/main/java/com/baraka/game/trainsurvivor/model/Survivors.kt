package com.baraka.game.trainsurvivor.model

enum class Perk { DAMAGE, FIRE_RATE, RANGE, CRIT, SCAVENGER }

/** Headwear a survivor can wear; drawn in render/TrainRenderer.kt (add a branch there for a new style). */
enum class HatStyle { NONE, CAP, COWBOY, HELMET, BANDANA, BEANIE, PHARAOH, NINJA }

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

/**
 * A named survivor: one per seat, with a passive perk and a look. [coopHeal] is the share of the host train's health
 * (per second) this survivor repairs while lent to a partner's train.
 */
class Survivor(
    val name: String, val role: String, val perk: Perk, val perkText: String, val look: AvatarLook, val coopHeal: Float = 0.006f,
    /** A weapon only this survivor's seat can buy or use; null if they use the shared pool like everyone else. */
    val signatureWeapon: WeaponType? = null
)

/**
 * The crew, in seat order. To add a survivor, add an entry: the train gets one more seat (seat prices,
 * garage cards, the in-run switcher, saving and the debug tools all follow `SURVIVORS.size`).
 */
val SURVIVORS = listOf(
    Survivor("Cole", "Driver", Perk.DAMAGE, "Steady Hands: +15% damage", AvatarLook(shirt = 0xFFD84315, hair = 0xFF3E2723, hat = HatStyle.CAP, hatColor = 0xFF263238), signatureWeapon = WeaponType.CONDUCTORS_REVOLVER),
    Survivor("Mara", "Ninja", Perk.FIRE_RATE, "Quick Draw: +20% fire rate", AvatarLook(shirt = 0xFF1A1A2E, pants = 0xFF101418, skin = 0xFFC68642, hair = 0xFF000000, hat = HatStyle.NINJA, hatColor = 0xFF1A1A1A, width = 0.9f, height = 0.94f, accent = 0xFFD32F2F), signatureWeapon = WeaponType.TWIN_KUNAI),
    Survivor("Khufu", "Pharaoh", Perk.RANGE, "Eye of Horus: +25% weapon range", AvatarLook(shirt = 0xFFF5F0E6, pants = 0xFFD4AF37, skin = 0xFFB5793B, hair = 0xFF1A1A1A, hat = HatStyle.PHARAOH, hatColor = 0xFFD4AF37, width = 0.95f, height = 1.08f, accent = 0xFF1565C0), signatureWeapon = WeaponType.ANUBIS_CURSE),
    Survivor("Doc", "Medic", Perk.CRIT, "Lucky Shots: 12% chance for x2.5 damage", AvatarLook(shirt = 0xFFECEFF1, pants = 0xFF455A64, skin = 0xFFCFD8DC, width = 1.1f, body = AvatarBody.ROBOT, accent = 0xFFE53935), coopHeal = 0.025f, signatureWeapon = WeaponType.TRAUMA_BEAM),
    Survivor("Rocco", "Scrapper", Perk.SCAVENGER, "Scavenger: +25% coins from his kills", AvatarLook(shirt = 0xFFFDD835, pants = 0xFF33691E, hair = 0xFF212121, hat = HatStyle.HELMET, hatColor = 0xFFFF8F00, width = 1.3f, extra = AvatarExtra.WELDER), signatureWeapon = WeaponType.SCRAP_CANNON)
)

/** Seats on the train: one per survivor. */
val MAX_SLOTS: Int = SURVIVORS.size

/** The seat [weapon] is exclusive to, or null if any seat can buy and use it. */
fun signatureOwnerOf(weapon: WeaponType): Int? = SURVIVORS.indexOfFirst { it.signatureWeapon == weapon }.takeIf { it >= 0 }
