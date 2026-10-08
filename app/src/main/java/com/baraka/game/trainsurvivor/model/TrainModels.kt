package com.baraka.game.trainsurvivor.model

import kotlin.math.pow
import kotlin.math.roundToInt

const val MAX_SLOTS = 5
const val MAX_POWER = 5

/** Everything a passenger can carry, weakest to strongest. Damage is per shot (per pellet-group for the shotgun). */
enum class WeaponType(
    val displayName: String,
    val damage: Float,
    val interval: Float,
    val range: Float,
    val splashRadius: Float,
    val pierce: Int,
    val unlockCost: Int,
    val blurb: String
) {
    PISTOL("Pistol", 10f, 0.55f, 330f, 0f, 1, 0, "Reliable sidearm"),
    SMG("SMG", 7f, 0.16f, 340f, 0f, 1, 150, "Rapid fire"),
    SHOTGUN("Shotgun", 30f, 0.9f, 230f, 70f, 1, 400, "Hits groups up close"),
    RIFLE("Sniper Rifle", 60f, 0.85f, 560f, 0f, 3, 900, "Long range, pierces 3"),
    MINIGUN("Minigun", 13f, 0.07f, 400f, 0f, 1, 2000, "Bullet hose"),
    CANNON("Cannon", 130f, 1.6f, 480f, 120f, 1, 4500, "Explosive shells")
}

enum class TrainUpgrade(
    val displayName: String,
    val blurb: String,
    val maxLevel: Int,
    val baseCost: Int,
    val growth: Double
) {
    ARMOR("Armor Plating", "+40 max train health per level", 10, 80, 1.5),
    REPAIR("Repair Crew", "Heals the train over time", 8, 100, 1.55),
    SPIKES("Spiked Hull", "Zombies clinging to the train take damage", 8, 90, 1.55),
    BOUNTY("Bounty Hunter", "+12% coins from kills per level", 8, 120, 1.6),
    DYNAMITE("Dynamite", "Bigger blast, faster cooldown", 6, 150, 1.7)
}

enum class ZombieType(
    val displayName: String,
    val hp: Float,
    val speed: Float,
    val damage: Float,
    val coins: Int,
    val size: Float
) {
    WALKER("Walker", 30f, 38f, 5f, 2, 1.0f),
    RUNNER("Runner", 18f, 88f, 4f, 3, 0.9f),
    BRUTE("Brute", 170f, 28f, 14f, 9, 1.45f),
    BLOATER("Bloater", 55f, 36f, 0f, 5, 1.2f),
    TITAN("Titan", 1500f, 22f, 42f, 150, 2.4f)
}

/** A stretch of track the player can route onto at a junction: new scenery plus a risk/reward trade. */
enum class Biome(
    val displayName: String,
    val tagline: String,
    val coinMult: Float,
    val hpMult: Float,
    val spawnMult: Float,
    val dmgMult: Float,
    val accent: Long
) {
    PLAINS("Open Plains", "Steady and familiar", 1.0f, 1.0f, 1.0f, 1.0f, 0xFF8BC34A),
    MOUNTAIN("Mountain Pass", "Frozen, tougher zombies", 1.5f, 1.3f, 1.0f, 1.0f, 0xFF81D4FA),
    TUNNEL("Dark Tunnel", "Dense hordes in the dark", 1.35f, 1.0f, 1.4f, 1.0f, 0xFFFFB74D),
    DESERT("Scorched Desert", "Fast swarming runners", 1.25f, 0.9f, 1.25f, 1.0f, 0xFFFFD54F),
    GRAVEYARD("Haunted Graveyard", "Brutes and bloaters hit hard", 1.6f, 1.1f, 1.0f, 1.2f, 0xFFB39DDB)
}

data class CrewSlot(val weaponTier: Int = 0, val power: Int = 0) {
    val weapon: WeaponType get() = WeaponType.entries[weaponTier]
    val damage: Float get() = weapon.damage * (1f + 0.3f * power)
}

/** Everything persisted between runs. Immutable; [Shop] returns a new copy on every purchase. */
data class SaveData(
    val coins: Int = 0,
    val bestDistance: Int = 0,
    val totalKills: Int = 0,
    val bossKills: Int = 0,
    val upgrades: Map<TrainUpgrade, Int> = emptyMap(),
    val slots: List<CrewSlot> = List(MAX_SLOTS) { CrewSlot() },
    val unlockedSlots: Int = 2
) {
    fun level(upgrade: TrainUpgrade): Int = upgrades[upgrade] ?: 0
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

    fun nextWeapon(slot: CrewSlot): WeaponType? = WeaponType.entries.getOrNull(slot.weaponTier + 1)

    fun weaponCost(slot: CrewSlot): Int? = nextWeapon(slot)?.unlockCost

    fun buyWeapon(save: SaveData, index: Int): SaveData? {
        val slot = save.slots[index]
        val cost = weaponCost(slot) ?: return null
        if (save.coins < cost) return null
        return save.copy(coins = save.coins - cost, slots = save.slots.replaced(index, CrewSlot(slot.weaponTier + 1, slot.power)))
    }

    fun powerCost(slot: CrewSlot): Int? {
        if (slot.power >= MAX_POWER) return null
        return ((40 + slot.weaponTier * 45) * 1.6.pow(slot.power)).roundToInt()
    }

    fun buyPower(save: SaveData, index: Int): SaveData? {
        val slot = save.slots[index]
        val cost = powerCost(slot) ?: return null
        if (save.coins < cost) return null
        return save.copy(coins = save.coins - cost, slots = save.slots.replaced(index, slot.copy(power = slot.power + 1)))
    }

    private fun List<CrewSlot>.replaced(index: Int, value: CrewSlot) = mapIndexed { i, s -> if (i == index) value else s }
}
