package com.baraka.game.trainsurvivor.model

import kotlin.math.pow
import kotlin.math.roundToInt

const val MAX_SLOTS = 5
const val MAX_POWER = 5
const val MAX_CAR_ARMOR = 5

/** Everything a passenger can carry, weakest to strongest. Damage is per shot (per pellet-group for the shotgun). */
enum class WeaponType(
    val displayName: String,
    val damage: Float,
    val interval: Float,
    val range: Float,
    val splashRadius: Float,
    val pierce: Int,
    val unlockCost: Int,
    val blurb: String,
    val groundOnly: Boolean = false,
    /** Heat added per shot (0..1 is full). Rapid-fire weapons overheat and lock out until they cool. */
    val heatPerShot: Float = 0f
) {
    PISTOL("Pistol", 10f, 0.55f, 330f, 0f, 1, 0, "Reliable sidearm"),
    SMG("SMG", 7f, 0.16f, 340f, 0f, 1, 150, "Rapid fire, great against weak swarms", heatPerShot = 0.05f),
    SHOTGUN("Shotgun", 45f, 0.9f, 230f, 70f, 1, 400, "Hits groups up close"),
    RIFLE("Sniper Rifle", 60f, 0.85f, 560f, 0f, 3, 900, "Long range, pierces 3"),
    MINIGUN("Minigun", 13f, 0.07f, 400f, 0f, 1, 2000, "Bullet hose that overheats", heatPerShot = 0.03f),
    CANNON("Cannon", 180f, 1.6f, 480f, 120f, 1, 4500, "Explosive shells (ground only)", groundOnly = true),
    ROCKET("Rocket Launcher", 190f, 1.9f, 540f, 150f, 1, 8000, "Big blasts, hits flyers too"),
    LASER("Laser Rifle", 42f, 0.22f, 620f, 0f, 5, 14000, "Piercing beam, very long range, overheats", heatPerShot = 0.1f)
}

enum class TrainUpgrade(
    val displayName: String,
    val blurb: String,
    val maxLevel: Int,
    val baseCost: Int,
    val growth: Double
) {
    ARMOR("Armor Plating", "+40 train health and +25 per wagon, per level", 10, 80, 1.5),
    REPAIR("Repair Crew", "Mechanics heal the train and its wagons over time", 8, 100, 1.55),
    SPIKES("Spiked Hull", "Zombies clinging to the train take damage", 8, 90, 1.55),
    BOUNTY("Bounty Hunter", "+12% coins from kills per level", 8, 120, 1.6),
    DYNAMITE("Dynamite", "Active: bigger blast, faster cooldown", 6, 150, 1.7),
    FLAK("Flak Turret", "Roof turret that shoots down flying enemies", 8, 200, 1.6),
    TESLA("Tesla Coil", "Chain lightning zaps groups of zombies", 8, 250, 1.65),
    FLAME("Flamethrower", "Cowcatcher flames scorch zombies at the front", 8, 220, 1.6),
    MORTAR("Mortar", "Lobs shells at the biggest ground crowd", 8, 300, 1.65),
    OVERDRIVE("Overdrive", "Active: every gun fires much faster for a few seconds", 6, 350, 1.7),
    FIELD_REPAIR("Field Repair", "Active: heals every car and revives wrecks", 6, 300, 1.7)
}

enum class ZombieType(
    val displayName: String,
    val hp: Float,
    val speed: Float,
    val damage: Float,
    val coins: Int,
    val size: Float,
    val flying: Boolean = false,
    val boss: Boolean = false,
    /** Flat damage removed from every hit (never below 25% of it), so small bullets are weak against armored targets. */
    val armor: Float = 0f
) {
    WALKER("Walker", 30f, 38f, 5f, 2, 1.0f),
    RUNNER("Runner", 18f, 88f, 4f, 3, 0.9f),
    BRUTE("Brute", 170f, 28f, 14f, 9, 1.45f, armor = 5f),
    BLOATER("Bloater", 55f, 36f, 0f, 5, 1.2f),
    TITAN("Titan", 1000f, 22f, 32f, 150, 2.4f, boss = true, armor = 12f),
    BAT("Zombie Bat", 14f, 125f, 3f, 2, 0.8f, flying = true),
    BALLOON("Balloon Zombie", 45f, 42f, 5f, 6, 1.0f, flying = true),
    CLOWN("Clown", 70f, 62f, 7f, 6, 1.1f),
    GARGOYLE("Gargoyle King", 900f, 55f, 26f, 180, 2.0f, flying = true, boss = true, armor = 9f),
    CLOWN_PRINCE("Clown Prince", 1200f, 34f, 22f, 200, 2.1f, boss = true, armor = 8f)
}

/** A stretch of track the player can route onto at a junction: new scenery plus a risk/reward trade. */
enum class Biome(
    val displayName: String,
    val tagline: String,
    val coinMult: Float,
    val hpMult: Float,
    val spawnMult: Float,
    val dmgMult: Float,
    val accent: Long,
    /** Multiplies the Repair Crew's healing while on this route. */
    val repairMult: Float = 1f,
    /** Free healing per second on this route, even without a Repair Crew. */
    val flatRegen: Float = 0f
) {
    PLAINS("Open Plains", "Calm and safe: fewer, weaker zombies and the train repairs itself", 1.0f, 0.85f, 0.7f, 0.9f, 0xFF8BC34A, repairMult = 2f, flatRegen = 2.5f),
    MOUNTAIN("Mountain Pass", "Frozen, much tougher zombies", 1.4f, 1.5f, 1.0f, 1.0f, 0xFF81D4FA),
    TUNNEL("Dark Tunnel", "Dense hordes in the dark", 1.35f, 1.0f, 1.5f, 1.1f, 0xFFFFB74D),
    DESERT("Scorched Desert", "Fast swarming runners", 1.3f, 0.9f, 1.4f, 1.0f, 0xFFFFD54F),
    GRAVEYARD("Haunted Graveyard", "Brutes and bloaters that hit very hard", 1.7f, 1.25f, 1.0f, 1.35f, 0xFFB39DDB)
}

enum class Perk { DAMAGE, FIRE_RATE, RANGE, CRIT, SCAVENGER }

/** The five named survivors, one per seat. Each brings a small passive perk to whatever weapon they hold. */
class Survivor(val name: String, val role: String, val perk: Perk, val perkText: String)

val SURVIVORS = listOf(
    Survivor("Cole", "Driver", Perk.DAMAGE, "Steady Hands: +15% damage"),
    Survivor("Mara", "Gunner", Perk.FIRE_RATE, "Quick Draw: +20% fire rate"),
    Survivor("Hawk", "Scout", Perk.RANGE, "Eagle Eye: +25% weapon range"),
    Survivor("Doc", "Medic", Perk.CRIT, "Lucky Shots: 12% chance for x2.5 damage"),
    Survivor("Rocco", "Scrapper", Perk.SCAVENGER, "Scavenger: +25% coins from his kills")
)

const val AUTO_WEAPON = -1

/**
 * One passenger's loadout: every weapon they own (bitmask over [WeaponType.ordinal]), which one they
 * prefer ([selected], or [AUTO_WEAPON] to let them pick per situation), their shared [power] level
 * and the armor on the car they ride in.
 */
data class CrewSlot(val owned: Int = 1, val selected: Int = AUTO_WEAPON, val power: Int = 0, val armor: Int = 0) {
    fun owns(weapon: WeaponType): Boolean = owned and (1 shl weapon.ordinal) != 0
    val ownedWeapons: List<WeaponType> get() = WeaponType.entries.filter { owns(it) }
    val bestWeapon: WeaponType get() = ownedWeapons.maxByOrNull { it.ordinal } ?: WeaponType.PISTOL
    val powerMultiplier: Float get() = 1f + 0.3f * power
}

/** One finished run on the local leaderboard. */
data class RunRecord(val distance: Int, val kills: Int, val coins: Int, val day: Long)

/** Everything persisted between runs. Immutable; [Shop] and [Progression] return a new copy on every change. */
data class SaveData(
    val coins: Int = 0,
    val bestDistance: Int = 0,
    val totalKills: Int = 0,
    val bossKills: Int = 0,
    val flyerKills: Int = 0,
    val routesTaken: Int = 0,
    val prestige: Int = 0,
    val upgrades: Map<TrainUpgrade, Int> = emptyMap(),
    val slots: List<CrewSlot> = List(MAX_SLOTS) { CrewSlot() },
    val unlockedSlots: Int = 2,
    val goalDay: Long = 0L,
    val goalProgress: List<Int> = listOf(0, 0, 0),
    val goalClaimed: Int = 0,
    val milestonesClaimed: Int = 0,
    val records: List<RunRecord> = emptyList()
) {
    fun level(upgrade: TrainUpgrade): Int = upgrades[upgrade] ?: 0

    /** Permanent coin bonus earned by prestiging. */
    val prestigeMultiplier: Float get() = 1f + 0.1f * prestige
}

/** Pure shop rules; every buy function returns the updated [SaveData] or null when it can't be bought. */
object Shop {
    private val slotCosts = intArrayOf(0, 0, 300, 800, 2000)

    fun upgradeCost(save: SaveData, upgrade: TrainUpgrade): Int? {
        val level = save.level(upgrade)
        if (level >= upgrade.maxLevel) return null
        return (upgrade.baseCost * upgrade.growth.pow(level)).roundToInt()
    }

    fun buyUpgrade(save: SaveData, upgrade: TrainUpgrade): SaveData? {
        val cost = upgradeCost(save, upgrade) ?: return null
        if (save.coins < cost) return null
        return save.copy(coins = save.coins - cost, upgrades = save.upgrades + (upgrade to save.level(upgrade) + 1))
    }

    fun slotCost(save: SaveData): Int? = if (save.unlockedSlots >= MAX_SLOTS) null else slotCosts[save.unlockedSlots]

    fun buySlot(save: SaveData): SaveData? {
        val cost = slotCost(save) ?: return null
        if (save.coins < cost) return null
        return save.copy(coins = save.coins - cost, unlockedSlots = save.unlockedSlots + 1)
    }

    /** Buys [weapon] for the passenger at [index]; any weapon can be bought in any order. */
    fun buyWeapon(save: SaveData, index: Int, weapon: WeaponType): SaveData? {
        val slot = save.slots[index]
        if (slot.owns(weapon) || save.coins < weapon.unlockCost) return null
        return save.copy(
            coins = save.coins - weapon.unlockCost,
            slots = save.slots.replaced(index, slot.copy(owned = slot.owned or (1 shl weapon.ordinal)))
        )
    }

    /** Sets which weapon the passenger prefers: a weapon ordinal they own, or [AUTO_WEAPON]. Free. */
    fun selectWeapon(save: SaveData, index: Int, selected: Int): SaveData {
        val slot = save.slots[index]
        val valid = selected == AUTO_WEAPON || WeaponType.entries.getOrNull(selected)?.let { slot.owns(it) } == true
        return if (valid) save.copy(slots = save.slots.replaced(index, slot.copy(selected = selected))) else save
    }

    fun powerCost(slot: CrewSlot): Int? {
        if (slot.power >= MAX_POWER) return null
        return ((40 + slot.bestWeapon.ordinal * 45) * 1.6.pow(slot.power)).roundToInt()
    }

    fun buyPower(save: SaveData, index: Int): SaveData? {
        val slot = save.slots[index]
        val cost = powerCost(slot) ?: return null
        if (save.coins < cost) return null
        return save.copy(coins = save.coins - cost, slots = save.slots.replaced(index, slot.copy(power = slot.power + 1)))
    }

    /** Per-car armor: +40% health for the car this passenger rides in (the locomotive for the driver). */
    fun carArmorCost(index: Int, slot: CrewSlot): Int? {
        if (slot.armor >= MAX_CAR_ARMOR) return null
        return ((60 + index * 20) * 1.7.pow(slot.armor)).roundToInt()
    }

    fun buyCarArmor(save: SaveData, index: Int): SaveData? {
        val slot = save.slots[index]
        val cost = carArmorCost(index, slot) ?: return null
        if (save.coins < cost) return null
        return save.copy(coins = save.coins - cost, slots = save.slots.replaced(index, slot.copy(armor = slot.armor + 1)))
    }

    private fun List<CrewSlot>.replaced(index: Int, value: CrewSlot) = mapIndexed { i, s -> if (i == index) value else s }
}
