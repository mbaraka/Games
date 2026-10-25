package com.baraka.game.dreadnought.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * The hull a custom ship is built on: its outline, how wide it is for its length, how long it may be, and which weapons
 * fit. The ship's drawing follows the form (see `render/ShipArts.kt`), so every design looks like a real ship of its kind.
 */
enum class HullForm(
    val label: String,
    val blurb: String,
    val profile: FloatArray,
    val beamRatio: Float,
    val minLength: Int,
    val maxLength: Int,
    val defaultLength: Int,
    val maxMains: Int,
    val maxSecondaries: Int,
    val maxTubes: Int,
    val maxPlanes: Int,
    val canDive: Boolean,
    val hasFunnels: Boolean
) {
    BATTLESHIP("Battleship", "Big, heavy hull for the biggest guns.", ShipClass.DREADNOUGHT.profile, 0.122f, 180, 320, 250, 4, 8, 2, 2, false, true),
    CRUISER("Cruiser", "Sleek and balanced; room for guns and torpedoes.", ShipClass.CRUISER.profile, 0.105f, 130, 230, 170, 4, 8, 4, 2, false, true),
    DESTROYER("Destroyer", "Small and fast; made for torpedoes.", ShipClass.DESTROYER.profile, 0.109f, 80, 150, 115, 3, 4, 6, 0, false, true),
    CARRIER("Carrier", "Flat deck for aircraft; no heavy guns.", ShipClass.CARRIER.profile, 0.146f, 200, 300, 260, 0, 6, 0, 6, false, false),
    SUBMARINE("Submarine", "Can dive out of reach; torpedoes and one deck gun.", ShipClass.SUBMARINE.profile, 0.095f, 60, 120, 95, 1, 0, 6, 0, true, false)
}

/** A hull colour and its trim, chosen in the shipyard. Index 0 is the standard grey. */
object Paints {
    val names = arrayOf("Haze grey", "Navy blue", "Forest", "Desert", "Crimson", "Arctic white")
    val hull = longArrayOf(0xFF66798A, 0xFF2F4F78, 0xFF4B6B3A, 0xFF9C8A5E, 0xFF8E3B3B, 0xFFC9D4DC)
    val accent = longArrayOf(0xFF4FC3F7, 0xFFFFC107, 0xFFFFEB3B, 0xFF5D4037, 0xFFFFC107, 0xFF1565C0)
}

/**
 * A ship the player has designed. Everything about the finished ship (hull points, speed, guns, reloads, plane count) is
 * worked out from these few choices by [toShipClass], and the points [cost] keeps a design within the [BUDGET].
 */
data class ShipDesign(
    val id: Int,
    val name: String,
    val form: HullForm = HullForm.CRUISER,
    val length: Int = form.defaultLength,
    /** 0 slim, 1 normal, 2 wide: wide is tougher and slower. */
    val width: Int = 1,
    val armor: Int = 3,
    val engine: Int = 3,
    val mains: Int = 2,
    val barrels: Int = 2,
    val caliber: Int = 3,
    val secondaries: Int = 4,
    val tubes: Int = 0,
    val planes: Int = 0,
    val dive: Boolean = false,
    val funnels: Int = 2,
    val paint: Int = 0
) {
    /** The design with every number pulled back inside what its hull form allows. */
    fun fixed(): ShipDesign {
        val f = form
        val m = mains.coerceIn(0, f.maxMains)
        return copy(
            length = length.coerceIn(f.minLength, f.maxLength),
            width = width.coerceIn(0, 2),
            armor = armor.coerceIn(1, 5),
            engine = engine.coerceIn(1, 5),
            mains = m,
            barrels = barrels.coerceIn(1, 4),
            caliber = caliber.coerceIn(1, 5),
            secondaries = (secondaries.coerceIn(0, f.maxSecondaries) / 2) * 2,
            tubes = tubes.coerceIn(0, f.maxTubes),
            planes = planes.coerceIn(0, f.maxPlanes),
            dive = dive && f.canDive,
            funnels = if (f.hasFunnels) funnels.coerceIn(0, 3) else 0,
            paint = paint.coerceIn(0, Paints.hull.lastIndex)
        )
    }

    val beam: Float get() = length * form.beamRatio * (0.9f + 0.1f * width + if (width == 2) 0.05f else 0f)
    val hullPoints: Float get() = length * (2.2f + 0.9f * armor) * (0.9f + 0.11f * width)
    val speed: Float get() = ((140f - 0.35f * length) * (0.7f + 0.15f * engine) * (1.05f - 0.05f * width)).coerceIn(30f, 125f)
    val turnRate: Float get() = (0.95f - 0.0027f * length).coerceIn(0.12f, 0.9f)
    val gunDamage: Float get() = GUN_DAMAGE[caliber - 1]
    val gunReload: Float get() = GUN_RELOAD[caliber - 1]
    val gunRange: Float get() = if (mains == 0 && planes > 0) 1000f else max(GUN_RANGE[caliber - 1], if (planes > 0) 900f else 0f)

    /** How many points this design uses; it must not go over [BUDGET]. */
    fun cost(): Int {
        val big = mains * barrels * GUN_COST[caliber - 1]
        return length / 8 + armor * 5 + engine * 4 + big + secondaries * 2 + tubes * 6 + planes * 12 + (if (dive) 20 else 0)
    }

    /** Builds the in-game ship for this design: the same kind of object as the built-in ships. */
    fun toShipClass(): ShipClass {
        val d = fixed()
        val b = d.beam
        val l = d.length.toFloat()
        val list = ArrayList<MountSpec>()
        val radius = min(GUN_RADIUS[d.caliber - 1], b * 0.27f)
        val fwd = (d.mains + 1) / 2
        val aft = d.mains / 2
        for (i in 0 until fwd) list.add(MountSpec(l * (0.38f - 0.12f * i), 0f, MountKind.MAIN, d.barrels, radius, 0f, 150f))
        for (j in 0 until aft) list.add(MountSpec(-l * (0.32f + 0.11f * j), 0f, MountKind.MAIN, d.barrels, radius, 180f, 150f))
        val sideX = floatArrayOf(0.24f, 0.08f, -0.08f, -0.22f)
        for (k in 0 until d.secondaries / 2) {
            list.add(MountSpec(l * sideX[k], b * 0.37f, MountKind.SECONDARY, 2, 2.6f, 90f, 80f))
            list.add(MountSpec(l * sideX[k], -b * 0.37f, MountKind.SECONDARY, 2, 2.6f, -90f, 80f))
        }
        val blurb = buildString {
            append("Your own design. ")
            if (d.mains > 0) append("${d.mains} turrets of ${d.barrels} guns. ")
            if (d.tubes > 0) append("${d.tubes} torpedo tubes. ")
            if (d.planes > 0) append("${d.planes} aircraft. ")
            if (d.dive) append("Can dive. ")
        }.trim()
        return ShipClass(
            label = d.name, role = "Custom " + d.form.label.lowercase(), length = l, beam = b, maxHp = d.hullPoints,
            maxSpeed = d.speed, turnRate = d.turnRate, accel = 5f + d.engine * 1.6f,
            profile = d.form.profile, mounts = list,
            mainDamage = d.gunDamage, mainReload = d.gunReload, mainRange = d.gunRange,
            secDamage = 10f, secReload = 1.1f, secRange = 400f,
            tubes = d.tubes, torpDamage = 130f, torpReload = 9f,
            diver = d.dive, playable = true,
            planes = d.planes, heroPlanes = d.planes, heroBomb = 55f,
            heroBlurb = blurb, paintIndex = d.paint, design = d
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("form", form.name); put("length", length); put("width", width)
        put("armor", armor); put("engine", engine); put("mains", mains); put("barrels", barrels); put("caliber", caliber)
        put("secondaries", secondaries); put("tubes", tubes); put("planes", planes); put("dive", dive)
        put("funnels", funnels); put("paint", paint)
    }

    companion object {
        const val BUDGET = 180
        const val MAX_DESIGNS = 5
        private val GUN_DAMAGE = floatArrayOf(9f, 18f, 30f, 48f, 70f)
        private val GUN_RELOAD = floatArrayOf(2.4f, 3.0f, 3.6f, 3.9f, 4.2f)
        private val GUN_RANGE = floatArrayOf(450f, 520f, 600f, 680f, 760f)
        private val GUN_RADIUS = floatArrayOf(3.2f, 4.8f, 6.2f, 8f, 9.5f)
        private val GUN_COST = intArrayOf(1, 2, 4, 7, 11)
        val GUN_NAMES = arrayOf("Light", "Medium", "Heavy", "Large", "Giant")

        fun fromJson(o: JSONObject): ShipDesign {
            val form = runCatching { HullForm.valueOf(o.optString("form", "CRUISER")) }.getOrDefault(HullForm.CRUISER)
            return ShipDesign(
                id = o.optInt("id", 1), name = o.optString("name", "My ship").ifBlank { "My ship" }.take(14), form = form,
                length = o.optInt("length", form.defaultLength), width = o.optInt("width", 1), armor = o.optInt("armor", 3),
                engine = o.optInt("engine", 3), mains = o.optInt("mains", 2), barrels = o.optInt("barrels", 2),
                caliber = o.optInt("caliber", 3), secondaries = o.optInt("secondaries", 4), tubes = o.optInt("tubes", 0),
                planes = o.optInt("planes", 0), dive = o.optBoolean("dive", false), funnels = o.optInt("funnels", 2),
                paint = o.optInt("paint", 0)
            ).fixed()
        }

        fun listToJson(list: List<ShipDesign>): String = JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(text: String): List<ShipDesign> = runCatching {
            val a = JSONArray(text)
            List(a.length()) { fromJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }
}
