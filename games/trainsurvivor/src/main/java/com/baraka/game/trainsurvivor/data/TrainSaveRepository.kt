package com.baraka.game.trainsurvivor.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.trainsurvivor.model.AUTO_WEAPON
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.DangerLevel
import com.baraka.game.trainsurvivor.model.MAX_CAR_ARMOR
import com.baraka.game.trainsurvivor.model.MAX_POWER
import com.baraka.game.trainsurvivor.model.MAX_SLOTS
import com.baraka.game.trainsurvivor.model.RunRecord
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.TrainUpgrade
import com.baraka.game.trainsurvivor.model.WeaponType

/** SharedPreferences-backed persistence for progress and settings — same pattern as the other games' repositories. */
class TrainSaveRepository private constructor(private val prefs: SharedPreferences) {

    fun load(): SaveData = SaveData(
        coins = prefs.getInt("coins", 0),
        bestDistance = prefs.getInt("best_distance", 0),
        totalKills = prefs.getInt("total_kills", 0),
        bossKills = prefs.getInt("boss_kills", 0),
        flyerKills = prefs.getInt("flyer_kills", 0),
        routesTaken = prefs.getInt("routes_taken", 0),
        prestige = prefs.getInt("prestige", 0),
        upgrades = TrainUpgrade.entries.associateWith { prefs.getInt("up_${it.name}", 0).coerceIn(0, it.maxLevel) },
        slots = List(MAX_SLOTS) { i ->
            // Older saves stored a single weapon tier; treat everything up to it as owned.
            val legacyTier = prefs.getInt("slot_${i}_tier", 0).coerceIn(0, WeaponType.entries.lastIndex)
            val mask = (1 shl WeaponType.entries.size) - 1
            val owned = (prefs.getInt("slot_${i}_owned", (1 shl (legacyTier + 1)) - 1) and mask).let { if (it == 0) 1 else it }
            val selected = prefs.getInt("slot_${i}_sel", AUTO_WEAPON)
            CrewSlot(
                owned = owned,
                selected = if (selected in WeaponType.entries.indices && owned and (1 shl selected) != 0) selected else AUTO_WEAPON,
                power = prefs.getInt("slot_${i}_power", 0).coerceIn(0, MAX_POWER),
                armor = prefs.getInt("slot_${i}_armor", 0).coerceIn(0, MAX_CAR_ARMOR)
            )
        },
        unlockedSlots = prefs.getInt("unlocked_slots", 2).coerceIn(1, MAX_SLOTS),
        goalDay = prefs.getLong("goal_day", 0L),
        goalProgress = List(3) { prefs.getInt("goal_progress_$it", 0) },
        goalClaimed = prefs.getInt("goal_claimed", 0),
        milestonesClaimed = prefs.getInt("milestones_claimed", 0),
        records = decodeRecords(prefs.getString("records", "") ?: ""),
        dangerLevel = prefs.getInt("danger_level", 0),
        dangerBest = List(DangerLevel.entries.size) { prefs.getInt("danger_best_$it", 0) },
        livery = prefs.getInt("livery", 0),
        liveriesOwned = prefs.getInt("liveries_owned", 1) or 1,
        shape = prefs.getInt("shape", 0),
        shapesOwned = prefs.getInt("shapes_owned", 1) or 1,
        coopBest = prefs.getInt("coop_best", 0),
        coopRescues = prefs.getInt("coop_rescues", 0),
        trailsOwned = decodeLongs(prefs.getString("trails_owned", "") ?: ""),
        selectedTrail = prefs.getLong("selected_trail", -1L),
        workshopOwned = decodeLongs(prefs.getString("workshop_owned", "") ?: ""),
        workshopActive = prefs.getLong("workshop_active", -1L)
    )

    fun save(data: SaveData) {
        prefs.edit().apply {
            putInt("coins", data.coins)
            putInt("best_distance", data.bestDistance)
            putInt("total_kills", data.totalKills)
            putInt("boss_kills", data.bossKills)
            putInt("flyer_kills", data.flyerKills)
            putInt("routes_taken", data.routesTaken)
            putInt("prestige", data.prestige)
            TrainUpgrade.entries.forEach { putInt("up_${it.name}", data.level(it)) }
            data.slots.forEachIndexed { i, s ->
                putInt("slot_${i}_owned", s.owned)
                putInt("slot_${i}_sel", s.selected)
                putInt("slot_${i}_power", s.power)
                putInt("slot_${i}_armor", s.armor)
            }
            putInt("unlocked_slots", data.unlockedSlots)
            putLong("goal_day", data.goalDay)
            data.goalProgress.forEachIndexed { i, p -> putInt("goal_progress_$i", p) }
            putInt("goal_claimed", data.goalClaimed)
            putInt("milestones_claimed", data.milestonesClaimed)
            putString("records", encodeRecords(data.records))
            putInt("danger_level", data.dangerLevel)
            data.dangerBest.forEachIndexed { i, v -> putInt("danger_best_$i", v) }
            putInt("livery", data.livery)
            putInt("liveries_owned", data.liveriesOwned)
            putInt("shape", data.shape)
            putInt("shapes_owned", data.shapesOwned)
            putInt("coop_best", data.coopBest)
            putInt("coop_rescues", data.coopRescues)
            putString("trails_owned", data.trailsOwned.joinToString(","))
            putLong("selected_trail", data.selectedTrail)
            putString("workshop_owned", data.workshopOwned.joinToString(","))
            putLong("workshop_active", data.workshopActive)
        }.apply()
    }

    /** Wipes all progress (settings are kept). Used by the debug tools. */
    fun resetProgress() {
        val settings = loadSettings()
        prefs.edit().clear().apply()
        saveSettings(settings)
    }

    fun loadSettings(): GameSettings {
        val d = GameSettings()
        return GameSettings(
            musicVolume = prefs.getFloat("set_music", d.musicVolume).coerceIn(0f, 1f),
            sfxVolume = prefs.getFloat("set_sfx", d.sfxVolume).coerceIn(0f, 1f),
            ambienceVolume = prefs.getFloat("set_ambience", d.ambienceVolume).coerceIn(0f, 1f),
            vibration = prefs.getBoolean("set_vibration", d.vibration),
            damageNumbers = prefs.getBoolean("set_damage_numbers", d.damageNumbers),
            screenShake = prefs.getBoolean("set_screen_shake", d.screenShake),
            showFps = prefs.getBoolean("set_show_fps", d.showFps)
        )
    }

    fun saveSettings(s: GameSettings) {
        prefs.edit()
            .putFloat("set_music", s.musicVolume)
            .putFloat("set_sfx", s.sfxVolume)
            .putFloat("set_ambience", s.ambienceVolume)
            .putBoolean("set_vibration", s.vibration)
            .putBoolean("set_damage_numbers", s.damageNumbers)
            .putBoolean("set_screen_shake", s.screenShake)
            .putBoolean("set_show_fps", s.showFps)
            .apply()
    }

    private fun encodeRecords(records: List<RunRecord>): String =
        records.joinToString(";") { "${it.distance},${it.kills},${it.coins},${it.day},${it.level}" }

    private fun decodeRecords(raw: String): List<RunRecord> = raw.split(';').mapNotNull { part ->
        val f = part.split(',')
        if (f.size != 4 && f.size != 5) return@mapNotNull null
        runCatching { RunRecord(f[0].toInt(), f[1].toInt(), f[2].toInt(), f[3].toLong(), f.getOrNull(4)?.toInt() ?: 0) }.getOrNull()
    }

    private fun decodeLongs(raw: String): List<Long> = if (raw.isEmpty()) emptyList() else raw.split(',').mapNotNull { it.toLongOrNull() }

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
