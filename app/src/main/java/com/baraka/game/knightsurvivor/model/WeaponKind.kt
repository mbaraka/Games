package com.baraka.game.knightsurvivor.model

/**
 * Stat table for every weapon archetype in the game.
 *
 * To add a new weapon that reuses an existing [WeaponTargeting] pattern (e.g. another
 * homing projectile with different numbers), add ONE enum entry here. Stats that are just
 * "a base value plus linear growth per level" (like [baseRadius]) are constructor fields
 * computed with no `when` at all; stats with per-weapon shaped curves (damage, cooldown,
 * pierce, projectile count) still need one more `when` branch each. Only a weapon with a
 * genuinely new *firing shape* needs a new [WeaponTargeting] case, which in turn needs one
 * new `when` branch in systems/WeaponFiringSystem.kt.
 */
enum class WeaponKind(
    val displayName: String,
    val description: String,
    val targeting: WeaponTargeting,
    val colorArgb: Long,
    val icon: VectorIcon,
    /** Pitch of this weapon's own fire sound — see data/SurvivorSoundEngine.kt#playWeaponFire. */
    val fireSoundHz: Double,
    /** Timbre of this weapon's own fire sound. */
    val fireSoundWaveform: Waveform,
    /** If set, the fire sound glides from [fireSoundHz] down/up to this pitch instead of staying flat — e.g. a duck's quack. Null means a plain flat tone. */
    val fireSoundEndHz: Double? = null,
    /** Overrides the default fire-sound duration (see data/SurvivorSoundEngine.kt#WEAPON_FIRE_DURATION_MS) — a swept sound usually needs more time to read as its shape than a flat blip does. */
    val fireSoundDurationMs: Int? = null,
    /** Hit radius in px at level 1 — the AoE radius for AREA_AROUND_PLAYER, the orbit radius for ORBIT. 0 for weapons where it's meaningless (e.g. a thrown projectile). */
    val baseRadius: Float = 0f,
    /** How much [baseRadius] grows per level above 1. */
    val radiusGrowthPerLevel: Float = 0f,
    val maxLevel: Int = 5
) {
    SWORD_SLASH(
        displayName = "Sword Slash",
        description = "Swings a blade around the knight, hitting every enemy in range.",
        targeting = WeaponTargeting.AREA_AROUND_PLAYER,
        colorArgb = 0xFFCFD8DC,
        icon = VectorIcon.Blade(lengthRatio = 2.2f),
        fireSoundHz = 320.0, // low, broad — a heavy swing
        fireSoundWaveform = Waveform.SAWTOOTH,
        baseRadius = 70f,
        radiusGrowthPerLevel = 10f
    ),
    DAGGER_THROW(
        displayName = "Dagger Throw",
        description = "Hurls a dagger at the nearest enemy at regular intervals.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFFFFC107,
        icon = VectorIcon.Blade(lengthRatio = 1.5f),
        fireSoundHz = 920.0, // high, crisp — a quick flick
        fireSoundWaveform = Waveform.SQUARE
    ),
    ORBIT_SHIELD(
        displayName = "Orbit Shield",
        description = "Shields spin around the knight, damaging anything they touch.",
        targeting = WeaponTargeting.ORBIT,
        colorArgb = 0xFF66BB6A,
        icon = VectorIcon.TurtleShell,
        fireSoundHz = 540.0, // soft, sustained — a magical hum (not currently triggered; ORBIT ticks too often to sound a fire cue without spamming, see WeaponFiringSystem)
        fireSoundWaveform = Waveform.SINE,
        baseRadius = 60f,
        radiusGrowthPerLevel = 4f
    ),
    THROWING_AXE(
        displayName = "Throwing Axe",
        description = "Hurls a cleaving axe that pierces through multiple enemies.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFFA1887F,
        icon = VectorIcon.Axe(lengthRatio = 1.6f),
        fireSoundHz = 240.0,
        fireSoundWaveform = Waveform.SAWTOOTH
    ),
    SUNBURST(
        displayName = "Sunburst",
        description = "A blazing solar field that repeatedly scorches every enemy nearby.",
        targeting = WeaponTargeting.AREA_AROUND_PLAYER,
        colorArgb = 0xFFFFB300,
        icon = VectorIcon.Star(points = 8, innerRadiusRatio = 0.55f),
        fireSoundHz = 1200.0,
        fireSoundWaveform = Waveform.SINE,
        baseRadius = 50f,
        radiusGrowthPerLevel = 8f
    ),
    RUBBER_DUCK(
        displayName = "Rubber Duck Toss",
        description = "Bonks the nearest enemy with a squeaky rubber duck. Purely for morale.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFFFFEB3B,
        icon = VectorIcon.Duck,
        fireSoundHz = 900.0, // a quack: starts high, glides down, over a slightly longer blip than other weapons
        fireSoundWaveform = Waveform.SQUARE,
        fireSoundEndHz = 380.0,
        fireSoundDurationMs = 130
    );

    /** Damage dealt per hit at [level] (1..[maxLevel]). */
    fun damageAtLevel(level: Int): Int = when (this) {
        SWORD_SLASH -> 16 + (level - 1) * 7
        DAGGER_THROW -> 10 + (level - 1) * 5
        ORBIT_SHIELD -> 8 + (level - 1) * 4
        THROWING_AXE -> 15 + (level - 1) * 7
        SUNBURST -> 4 + (level - 1) * 2
        RUBBER_DUCK -> 12 + (level - 1) * 5
    }

    /**
     * Milliseconds between activations at [level] (lower = attacks more often).
     * For [WeaponTargeting.ORBIT] this is instead the minimum interval between repeat
     * hits on the *same* enemy, so a shield doesn't melt one target across many ticks.
     */
    fun cooldownMsAtLevel(level: Int): Long = when (this) {
        SWORD_SLASH -> (900 - (level - 1) * 80).coerceAtLeast(450).toLong()
        DAGGER_THROW -> (650 - (level - 1) * 60).coerceAtLeast(300).toLong()
        ORBIT_SHIELD -> (350 - (level - 1) * 20).coerceAtLeast(220).toLong()
        THROWING_AXE -> (820 - (level - 1) * 70).coerceAtLeast(420).toLong()
        SUNBURST -> (420 - (level - 1) * 30).coerceAtLeast(220).toLong()
        RUBBER_DUCK -> (700 - (level - 1) * 60).coerceAtLeast(350).toLong()
    }

    /** Hit radius in px. Used as the AoE radius for AREA_AROUND_PLAYER and the orbit radius for ORBIT. Driven by [baseRadius]/[radiusGrowthPerLevel] so a new weapon needs no `when` branch here. */
    fun radiusAtLevel(level: Int): Float = baseRadius + (level - 1) * radiusGrowthPerLevel

    /** Number of simultaneous orbiting instances (ORBIT_SHIELD only, 0 for every other kind). */
    fun orbCountAtLevel(level: Int): Int = when (this) {
        ORBIT_SHIELD -> 1 + (level - 1) / 2 // L1-2: 1 orb, L3-4: 2 orbs, L5: 3 orbs
        else -> 0
    }

    /** Enemies a single projectile can pierce through before it's consumed. */
    fun pierceAtLevel(level: Int): Int = when (this) {
        DAGGER_THROW -> if (level >= 4) 1 else 0
        THROWING_AXE -> (level - 1) / 2 // L1-2: 0, L3-4: 1, L5: 2
        else -> 0
    }

    /** Projectiles fired per activation (NEAREST_ENEMY only), each aimed at a different nearby enemy. Defaults to 1 so this is opt-in per weapon. */
    fun projectileCountAtLevel(level: Int): Int = when (this) {
        DAGGER_THROW -> 1 + (level - 1) / 2 // L1-2: 1, L3-4: 2, L5: 3
        THROWING_AXE -> 1 + (level - 1) / 3 // L1-3: 1, L4-5: 2
        else -> 1
    }

    /** Projectile travel speed in px/s (only meaningful for NEAREST_ENEMY weapons). */
    val projectileSpeed: Float
        get() = when (this) {
            DAGGER_THROW -> 260f
            THROWING_AXE -> 190f
            RUBBER_DUCK -> 210f
            else -> 0f
        }
}

/** The shape a weapon's attack takes; drives how systems/WeaponFiringSystem.kt resolves it each tick. */
enum class WeaponTargeting {
    /** Fires a projectile at the nearest living enemy; does nothing if none are in range. */
    NEAREST_ENEMY,

    /** Deals damage directly to every enemy within radiusAtLevel() of the player; no projectile. */
    AREA_AROUND_PLAYER,

    /** Keeps orbCountAtLevel() instances continuously rotating around the player. */
    ORBIT
}
