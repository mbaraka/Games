package com.baraka.game.basedefender.model

import androidx.compose.ui.graphics.Color

/**
 * Base Defender content is DATA. The engine and renderer read these flags and never switch on a specific
 * entry, so a new weapon, enemy, power-up, upgrade or turret role is one enum entry (plus art for a new
 * [EnemyShape]). See `EnemyArts.kt` for drawing and `Waves.kt` for when things appear.
 */
object Tuning {
    /** Logical world width. The world is always 400 wide; the screen scales it, so nothing depends on pixels. */
    const val WORLD_W = 400f
    const val GROUND_H = 64f
    const val START_MONEY = 100
    const val START_LIVES = 3
    const val MAX_LIVES = 5
    const val TOTAL_WAVES = 20
    const val MAX_BASES = 5
    const val TICKS_PER_SECOND = 60f

    // Hard caps so a late wave can never grow a collection without bound.
    const val MAX_ENEMIES = 80
    const val MAX_BULLETS = 220
    const val MAX_PICKUPS = 6
    const val MAX_PARTICLES = 320
    const val MAX_TEXTS = 40

    const val BASE_DAMAGE = 25f
    const val DAMAGE_PER_LEVEL = 10f
    const val BASE_BULLET_SPEED = 8f
    const val SPEED_PER_LEVEL = 2f
    const val BASE_FIRE_INTERVAL = 28f
    const val FIRE_RATE_FACTOR = 0.85f
    const val MIN_FIRE_INTERVAL = 5f
}

enum class GameStatus { PLAYING, GAME_OVER, VICTORY }

/** What a shot does. [burnTicks]/[freezeTicks] are status durations; [splash] is an explosion radius (0 = none). */
enum class WeaponType(
    val label: String,
    val color: Color,
    val damageMult: Float,
    val speedMult: Float,
    val burnTicks: Int = 0,
    val freezeTicks: Int = 0,
    val splash: Float = 0f
) {
    BASIC("Basic", Color(0xFFFFD54F), 1f, 1f),
    FIRE("Fire", Color(0xFFFF7043), 0.8f, 1.2f, burnTicks = 120),
    ICE("Ice", Color(0xFF4DD0E1), 0.6f, 1.5f, freezeTicks = 90),
    EXPLOSIVE("Blast", Color(0xFFFFA726), 1.5f, 0.7f, splash = 46f)
}

/**
 * Each extra base gets the next role, so more turrets means different turrets, not copies.
 * [fireMult] scales the reload time, [damageMult] each pellet, [pierce] extra enemies a shot passes through.
 */
enum class TurretRole(
    val label: String,
    val color: Color,
    val fireMult: Float,
    val damageMult: Float,
    val pellets: Int = 1,
    val spread: Float = 0f,
    val jitter: Float = 0f,
    val pierce: Int = 0,
    val barrel: Float = 26f,
    val barrelWidth: Float = 7f
) {
    CANNON("Cannon", Color(0xFFFFC107), 1f, 1f),
    GATLING("Gatling", Color(0xFF66BB6A), 0.4f, 0.45f, jitter = 0.07f, barrel = 24f, barrelWidth = 6f),
    SCATTER("Scatter", Color(0xFFFF8A65), 1.3f, 0.55f, pellets = 3, spread = 0.2f, barrel = 20f, barrelWidth = 10f),
    SNIPER("Sniper", Color(0xFF64B5F6), 2.2f, 2.4f, pierce = 2, barrel = 34f, barrelWidth = 5f);

    companion object {
        /** The role of the n-th base bought (0 = the starting base). */
        fun forSlot(slot: Int): TurretRole = entries[slot % entries.size]
    }
}

/** Art families. A new family needs one entry in `EnemyArts.artFor` (the `when` there is exhaustive on purpose). */
enum class EnemyShape { BLOB, DART, TANK, HULL }

/**
 * @property minWave first wave it can spawn in; @property weight relative spawn chance (0 = scripted, e.g. boss).
 * @property sway sideways wobble; @property leakDamage lives lost if it reaches the ground.
 */
enum class EnemyType(
    val label: String,
    val shape: EnemyShape,
    val color: Color,
    val minWave: Int,
    val weight: Int,
    val hpMult: Float,
    val speedMult: Float,
    val rewardMult: Float,
    val size: Float,
    val sway: Float = 0.2f,
    val leakDamage: Int = 1
) {
    BASIC("Grunt", EnemyShape.BLOB, Color(0xFFEF5350), 1, 10, 1f, 1f, 1f, 28f),
    FAST("Raider", EnemyShape.DART, Color(0xFF26C6DA), 3, 4, 0.7f, 1.8f, 1.2f, 22f, sway = 0.7f),
    ARMORED("Tank", EnemyShape.TANK, Color(0xFFAB47BC), 5, 3, 2.5f, 0.6f, 1.8f, 34f, sway = 0.05f),
    BOSS("Overlord", EnemyShape.HULL, Color(0xFFFF7043), 99, 0, 14f, 0.35f, 12f, 56f, sway = 0.05f, leakDamage = 3)
}

enum class PowerUpType(val label: String, val letter: String, val color: Color, val duration: Int) {
    RAPID_FIRE("Rapid", "R", Color(0xFF42A5F5), 600),
    SHIELD("Shield", "S", Color(0xFF66BB6A), 480),
    TIME_SLOW("Slow", "T", Color(0xFFBA68C8), 720),
    /** Duration 0 = instant: grants one big piercing shot per turret. */
    MEGA_SHOT("Mega", "M", Color(0xFFEF5350), 0)
}

/** Shop items. Cost of the next level = baseCost * growth^level. The engine applies the effect in `buy`. */
enum class UpgradeType(
    val title: String,
    val description: String,
    val baseCost: Int,
    val growth: Float,
    val maxLevel: Int
) {
    DAMAGE("Damage", "+10 damage on every shot", 100, 1.35f, 15),
    FIRE_RATE("Fire rate", "Every turret reloads 15% faster", 150, 1.4f, 12),
    BULLET_SPEED("Bullet speed", "Shots fly faster and miss less", 120, 1.3f, 10),
    EXTRA_BASE("New turret", "Adds a turret with a new role", 120, 1.7f, Tuning.MAX_BASES - 1),
    REPAIR("Repair", "Restore one life", 80, 1.2f, 20)
}
