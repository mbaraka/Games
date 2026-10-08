package com.baraka.game.trainsurvivor.model

/**
 * The train's body. Geometry and small stat differences live here; the drawing for each shape is registered in
 * render/TrainShapes.kt. To add a shape: add an entry and register its art - the Garage's Look tab, saving,
 * the engine's car layout and the debug tools follow automatically.
 */
enum class TrainShape(
    val displayName: String,
    val blurb: String,
    val cost: Int,
    /** Locomotive and wagon sizes, the gap between cars and how far the nose sticks out past the locomotive (units). */
    val locoW: Float,
    val carW: Float,
    val carGap: Float,
    val nose: Float,
    /** Where the driver stands (from the locomotive's left edge), the roof height he stands on, and where exhaust comes out. */
    val locoGunX: Float,
    val locoRoofY: Float,
    val stackX: Float,
    /** Multiplies the locomotive's health, and the train's speed (distance and coins per second). */
    val hpMult: Float = 1f,
    val speedMult: Float = 1f
) {
    STEAM("Steam Locomotive", "Classic and sturdy", 0, 190f, 150f, 8f, 34f, 38f, 388f, 160f),
    DIESEL("Diesel Hauler", "Heavy: +20% locomotive health", 1500, 210f, 150f, 8f, 24f, 56f, 384f, 40f, hpMult = 1.2f),
    BULLET("Bullet Train", "Streamlined: 15% faster, so distance and coins come quicker", 4000, 230f, 150f, 8f, 46f, 74f, 390f, 24f, speedMult = 1.15f)
}

/** Paint schemes for the train, bought with coins in the Garage. [glow] liveries have lit trim that shines at night. */
enum class Livery(
    val displayName: String,
    val cost: Int,
    val boiler: Long,
    val boilerHi: Long,
    val cab: Long,
    val cabDark: Long,
    val trim: Long,
    val cars: List<Long>,
    val glow: Boolean = false
) {
    CLASSIC("Classic Red", 0, 0xFF9E2A24, 0xFFB8403A, 0xFF5E2A24, 0xFF3F1B17, 0xFFD1A63A, listOf(0xFF7A3E2B, 0xFF2F5D62, 0xFF5B4B8A, 0xFF4F6B3A, 0xFF8A5A2B)),
    MIDNIGHT("Midnight Express", 400, 0xFF263238, 0xFF37474F, 0xFF1C2328, 0xFF11161A, 0xFFB0BEC5, listOf(0xFF37474F, 0xFF2D3A41, 0xFF424F57, 0xFF263238, 0xFF33424B)),
    CRIMSON("Crimson Flyer", 600, 0xFFC62828, 0xFFE53935, 0xFF8E1B1B, 0xFF5E1212, 0xFFFFC107, listOf(0xFF8E1B1B, 0xFFD7CCC8, 0xFFB71C1C, 0xFFEFEBE9, 0xFF9C2A2A)),
    FOREST("Forest Line", 900, 0xFF2E7D32, 0xFF43A047, 0xFF1B5E20, 0xFF124016, 0xFFD7B35A, listOf(0xFF33691E, 0xFF5D4037, 0xFF2E7D32, 0xFF795548, 0xFF558B2F)),
    NEON("Neon Runner", 2500, 0xFF1A1033, 0xFF2A1A55, 0xFF130B26, 0xFF0B0617, 0xFF00E5FF, listOf(0xFF2A1A55, 0xFF1D2B55, 0xFF3A1A55, 0xFF1A3355, 0xFF2A1F44), glow = true),
    ROCKET("Rocket Express", 3000, 0xFFECEFF1, 0xFFFFFFFF, 0xFFB0BEC5, 0xFF78909C, 0xFFFF7043, listOf(0xFFECEFF1, 0xFFCFD8DC, 0xFFFFFFFF, 0xFFB0BEC5, 0xFFE0E6EA)),
    GOLD("Golden Edition", 5000, 0xFFC9A227, 0xFFFFE082, 0xFF8D6E1F, 0xFF5D470F, 0xFFFFFFFF, listOf(0xFFB8860B, 0xFFDAA520, 0xFFC9A227, 0xFFE6C453, 0xFFA67C00), glow = true)
}
