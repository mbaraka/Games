package com.baraka.game.trainsurvivor.model

import kotlin.math.pow
import kotlin.math.roundToInt

// Content tables live in their own files: Enemies.kt, Routes.kt, Survivors.kt, TrainStyle.kt, Upgrades.kt.
// This file holds the player's save data and the shop rules that change it.

const val MAX_POWER = 5
const val MAX_CAR_ARMOR = 5
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
data class RunRecord(val distance: Int, val kills: Int, val coins: Int, val day: Long, val level: Int = 0)

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
    val records: List<RunRecord> = emptyList(),
    /** The danger level chosen for the next run, and the furthest distance reached on each level. */
    val dangerLevel: Int = 0,
    val dangerBest: List<Int> = List(DangerLevel.entries.size) { 0 },
    /** Train look: selected livery/shape and bitmasks of the ones owned (the first of each is always owned). */
    val livery: Int = 0,
    val liveriesOwned: Int = 1,
    val shape: Int = 0,
    val shapesOwned: Int = 1,
    /** Furthest distance reached in a two-phone co-op run, and survivors rescued there. Unlocks co-op paints. */
    val coopBest: Int = 0,
    val coopRescues: Int = 0
) {
    /** A title for how far the player's co-op runs have gone. */
    val coopRank: String get() = when {
        coopBest >= 6000 -> "Twin Legends"
        coopBest >= 3000 -> "Rail Partners"
        coopBest >= 1000 -> "Track Mates"
        coopBest > 0 -> "Rookie Pair"
        else -> "No co-op runs yet"
    }

    fun level(upgrade: TrainUpgrade): Int = upgrades[upgrade] ?: 0

    fun dangerUnlocked(index: Int): Boolean =
        index <= 0 || dangerBest.getOrElse(index - 1) { 0 } >= DangerLevel.entries[index.coerceAtMost(DangerLevel.entries.lastIndex)].requirementMeters

    /** The chosen danger level, falling back to Normal if it isn't unlocked. */
    val selectedDanger: DangerLevel
        get() {
            val i = dangerLevel.coerceIn(0, DangerLevel.entries.lastIndex)
            return DangerLevel.entries[if (dangerUnlocked(i)) i else 0]
        }

    fun ownsLivery(index: Int): Boolean = liveriesOwned and (1 shl index) != 0
    fun ownsShape(index: Int): Boolean = shapesOwned and (1 shl index) != 0

    val selectedLivery: Livery
        get() {
            val i = livery.coerceIn(0, Livery.entries.lastIndex)
            return Livery.entries[if (ownsLivery(i)) i else 0]
        }

    val selectedShape: TrainShape
        get() {
            val i = shape.coerceIn(0, TrainShape.entries.lastIndex)
            return TrainShape.entries[if (ownsShape(i)) i else 0]
        }

    /** Permanent coin bonus earned by prestiging. */
    val prestigeMultiplier: Float get() = 1f + 0.1f * prestige
}

/** Pure shop rules; every buy function returns the updated [SaveData] or null when it can't be bought. */
object Shop {
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

    /** Price of unlocking the next seat; the first two seats are free, then each costs 2.6x the previous. */
    fun slotCost(save: SaveData): Int? =
        if (save.unlockedSlots >= MAX_SLOTS) null else (300.0 * 2.6.pow(save.unlockedSlots - 2)).roundToInt()

    fun buySlot(save: SaveData): SaveData? {
        val cost = slotCost(save) ?: return null
        if (save.coins < cost) return null
        return save.copy(coins = save.coins - cost, unlockedSlots = save.unlockedSlots + 1)
    }

    /** Buys [weapon] for the passenger at [index]; any weapon can be bought in any order, except a
     * signature weapon, which only the survivor it belongs to can buy. */
    fun buyWeapon(save: SaveData, index: Int, weapon: WeaponType): SaveData? {
        val slot = save.slots[index]
        val owner = signatureOwnerOf(weapon)
        if (slot.owns(weapon) || save.coins < weapon.unlockCost || (owner != null && owner != index)) return null
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

    fun buyLivery(save: SaveData, index: Int): SaveData? {
        val livery = Livery.entries.getOrNull(index) ?: return null
        if (save.ownsLivery(index) || save.coins < livery.cost || save.coopBest < livery.coopDistance) return null
        return save.copy(coins = save.coins - livery.cost, liveriesOwned = save.liveriesOwned or (1 shl index), livery = index)
    }

    fun selectLivery(save: SaveData, index: Int): SaveData = if (save.ownsLivery(index)) save.copy(livery = index) else save

    fun buyShape(save: SaveData, index: Int): SaveData? {
        val shape = TrainShape.entries.getOrNull(index) ?: return null
        if (save.ownsShape(index) || save.coins < shape.cost) return null
        return save.copy(coins = save.coins - shape.cost, shapesOwned = save.shapesOwned or (1 shl index), shape = index)
    }

    fun selectShape(save: SaveData, index: Int): SaveData = if (save.ownsShape(index)) save.copy(shape = index) else save

    private fun List<CrewSlot>.replaced(index: Int, value: CrewSlot) = mapIndexed { i, s -> if (i == index) value else s }
}
