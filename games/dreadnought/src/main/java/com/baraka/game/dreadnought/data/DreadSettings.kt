package com.baraka.game.dreadnought.data

import android.content.Context
import android.content.SharedPreferences
import com.baraka.game.dreadnought.model.Refit
import com.baraka.game.dreadnought.model.ShipClass
import com.baraka.game.dreadnought.model.ShipDesign
import com.baraka.game.dreadnought.model.Theatre

/** Every player-facing option of Dreadnought. Volumes are 0..1. */
data class DreadSettings(
    val sfxVolume: Float = 0.9f,
    val ambienceVolume: Float = 0.5f,
    val vibration: Boolean = true,
    val screenShake: Boolean = true,
    val damageNumbers: Boolean = true,
    val aimAssist: Boolean = true,
    val autoFire: Boolean = false,
    val rangeRing: Boolean = true,
    val zoom: Float = 1f,
    val showFps: Boolean = false
)

/** Progress that survives between sorties. New fields get a default so old saves keep loading. */
data class DreadSave(
    val coins: Int = 0,
    val bestWave: Int = 0,
    val bestScore: Int = 0,
    val kills: Int = 0,
    val flagships: Int = 0,
    val levels: List<Int> = List(Refit.entries.size) { 0 },
    val theatre: Int = 0,
    /** Chosen ship (index in [ShipClass.PLAYABLE]) and the ships bought (bit per index; the first is always owned). */
    val ship: Int = 0,
    val shipsOwned: Int = 1
) {
    fun shipClass(): ShipClass = ShipClass.PLAYABLE[ship.coerceIn(0, ShipClass.PLAYABLE.lastIndex)]
    /** Built-in ships are bought with coins; your own designs are always yours. */
    fun owns(index: Int): Boolean = index >= ShipClass.BUILTIN_PLAYABLE_COUNT || (shipsOwned and (1 shl index)) != 0
    fun level(r: Refit): Int = levels.getOrElse(r.ordinal) { 0 }
    fun theatreUnlocked(t: Theatre): Boolean = bestWave >= t.unlockWave
}

/** SharedPreferences-backed persistence, same pattern as the other games' repositories. */
class DreadRepository private constructor(private val prefs: SharedPreferences) {

    fun loadSave(): DreadSave = DreadSave(
        coins = prefs.getInt("coins", 0),
        bestWave = prefs.getInt("best_wave", 0),
        bestScore = prefs.getInt("best_score", 0),
        kills = prefs.getInt("kills", 0),
        flagships = prefs.getInt("flagships", 0),
        levels = Refit.entries.map { prefs.getInt("lvl_${it.name}", 0).coerceIn(0, it.maxLevel) },
        theatre = prefs.getInt("theatre", 0).coerceIn(0, Theatre.entries.lastIndex),
        ship = prefs.getInt("ship", 0).coerceIn(0, ShipClass.PLAYABLE.lastIndex),
        shipsOwned = prefs.getInt("ships_owned", 1) or 1
    )

    fun save(data: DreadSave) {
        prefs.edit().apply {
            putInt("coins", data.coins)
            putInt("best_wave", data.bestWave)
            putInt("best_score", data.bestScore)
            putInt("kills", data.kills)
            putInt("flagships", data.flagships)
            putInt("theatre", data.theatre)
            putInt("ship", data.ship)
            putInt("ships_owned", data.shipsOwned)
            for (r in Refit.entries) putInt("lvl_${r.name}", data.level(r))
        }.apply()
    }

    fun loadSettings(): DreadSettings {
        val d = DreadSettings()
        return DreadSettings(
            sfxVolume = prefs.getFloat("s_sfx", d.sfxVolume),
            ambienceVolume = prefs.getFloat("s_amb", d.ambienceVolume),
            vibration = prefs.getBoolean("s_vib", d.vibration),
            screenShake = prefs.getBoolean("s_shake", d.screenShake),
            damageNumbers = prefs.getBoolean("s_dmgnum", d.damageNumbers),
            aimAssist = prefs.getBoolean("s_assist", d.aimAssist),
            autoFire = prefs.getBoolean("s_autofire", d.autoFire),
            rangeRing = prefs.getBoolean("s_ring", d.rangeRing),
            zoom = prefs.getFloat("s_zoom", d.zoom).coerceIn(0.7f, 1.5f),
            showFps = prefs.getBoolean("s_fps", d.showFps)
        )
    }

    fun loadDesigns(): List<ShipDesign> = ShipDesign.listFromJson(prefs.getString("designs", "") ?: "")

    fun saveDesigns(list: List<ShipDesign>) {
        prefs.edit().putString("designs", ShipDesign.listToJson(list)).apply()
    }

    fun saveSettings(s: DreadSettings) {
        prefs.edit().apply {
            putFloat("s_sfx", s.sfxVolume)
            putFloat("s_amb", s.ambienceVolume)
            putBoolean("s_vib", s.vibration)
            putBoolean("s_shake", s.screenShake)
            putBoolean("s_dmgnum", s.damageNumbers)
            putBoolean("s_assist", s.aimAssist)
            putBoolean("s_autofire", s.autoFire)
            putBoolean("s_ring", s.rangeRing)
            putFloat("s_zoom", s.zoom)
            putBoolean("s_fps", s.showFps)
        }.apply()
    }

    companion object {
        fun get(context: Context) = DreadRepository(context.applicationContext.getSharedPreferences("dreadnought", Context.MODE_PRIVATE))
    }
}
