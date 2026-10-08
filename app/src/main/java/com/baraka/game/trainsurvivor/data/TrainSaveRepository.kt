package com.baraka.game.trainsurvivor.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.trainsurvivor.model.AUTO_WEAPON
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.MAX_POWER
import com.baraka.game.trainsurvivor.model.MAX_SLOTS
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.TrainUpgrade
import com.baraka.game.trainsurvivor.model.WeaponType

/** SharedPreferences-backed persistence for coins, upgrades and crew — same pattern as the other games' repositories. */
class TrainSaveRepository private constructor(private val prefs: SharedPreferences) {

    fun load(): SaveData = SaveData(
        coins = prefs.getInt("coins", 0),
        bestDistance = prefs.getInt("best_distance", 0),
        totalKills = prefs.getInt("total_kills", 0),
        bossKills = prefs.getInt("boss_kills", 0),
        upgrades = TrainUpgrade.entries.associateWith { prefs.getInt("up_${it.name}", 0) },
        slots = List(MAX_SLOTS) { i ->
            // Older saves stored a single weapon tier; treat everything up to it as owned.
            val legacyTier = prefs.getInt("slot_${i}_tier", 0).coerceIn(0, WeaponType.entries.lastIndex)
            val mask = (1 shl WeaponType.entries.size) - 1
            val owned = (prefs.getInt("slot_${i}_owned", (1 shl (legacyTier + 1)) - 1) and mask).let { if (it == 0) 1 else it }
            val selected = prefs.getInt("slot_${i}_sel", AUTO_WEAPON)
            CrewSlot(
                owned = owned,
                selected = if (selected in WeaponType.entries.indices && owned and (1 shl selected) != 0) selected else AUTO_WEAPON,
                power = prefs.getInt("slot_${i}_power", 0).coerceIn(0, MAX_POWER)
            )
        },
        unlockedSlots = prefs.getInt("unlocked_slots", 2).coerceIn(1, MAX_SLOTS)
    )

    fun save(data: SaveData) {
        prefs.edit().apply {
            putInt("coins", data.coins)
            putInt("best_distance", data.bestDistance)
            putInt("total_kills", data.totalKills)
            putInt("boss_kills", data.bossKills)
            TrainUpgrade.entries.forEach { putInt("up_${it.name}", data.level(it)) }
            data.slots.forEachIndexed { i, s ->
                putInt("slot_${i}_owned", s.owned)
                putInt("slot_${i}_sel", s.selected)
                putInt("slot_${i}_power", s.power)
            }
            putInt("unlocked_slots", data.unlockedSlots)
        }.apply()
    }

    fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    companion object {
        @Volatile
        private var instance: TrainSaveRepository? = null

        fun getInstance(context: Context): TrainSaveRepository = instance ?: synchronized(this) {
            instance ?: TrainSaveRepository(
                context.applicationContext.getSharedPreferences("train_survivor", Context.MODE_PRIVATE)
            ).also { instance = it }
        }
    }
}
