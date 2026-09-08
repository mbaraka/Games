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
    /** If set, the fire sound glides from [fireSoundHz] down/up to this pitch instead of staying flat — e.g. a duck's quack. Only meaningful with [fireSoundStyle] = SWEEP. */
    val fireSoundEndHz: Double? = null,
    /** How the fire sound is synthesized — see data/SurvivorSoundEngine.kt. Defaults to a plain flat tone. */
    val fireSoundStyle: WeaponFireSoundStyle = WeaponFireSoundStyle.TONE,
    /** Overrides the default fire-sound duration (see data/SurvivorSoundEngine.kt#WEAPON_FIRE_DURATION_MS) — a swept sound usually needs more time to read as its shape than a flat blip does. */
    val fireSoundDurationMs: Int? = null,
    /** Hit radius in px at level 1 — the AoE radius for AREA_AROUND_PLAYER, the orbit radius for ORBIT. 0 for weapons where it's meaningless (e.g. a thrown projectile). */
    val baseRadius: Float = 0f,
    /** How much [baseRadius] grows per level above 1. */
    val radiusGrowthPerLevel: Float = 0f,
    /** Chance (0..1) an AREA_AROUND_PLAYER hit also stuns the enemy — see [WeaponFiringSystem]. 0 for every weapon that doesn't stun. */
    val stunChance: Float = 0f,
    /** How long a successful stun lasts. */
    val stunDurationMs: Long = 0L,
    /** How many px an AREA_AROUND_PLAYER hit yanks the enemy toward the player — Magnet Pulse only, 0 for everything else. */
    val pullStrengthPx: Float = 0f,
    /** If > 0, a NEAREST_ENEMY projectile's impact also deals splash damage to other enemies within this radius. */
    val splashRadius: Float = 0f,
    /** Fraction of the primary hit's damage dealt to each enemy caught in the splash. */
    val splashDamageFraction: Float = 0f,
    /** If > 0, a NEAREST_ENEMY projectile's impact also briefly slows every enemy within [splashRadius] (Egg Toss's "puddle"). */
    val splashSlowDurationMs: Long = 0L,
    val splashSlowMultiplier: Float = 1f,
    /** A NEAREST_ENEMY projectile that flies out, then homes back through the player's position — Boomerang only. */
    val returnsToPlayer: Boolean = false,
    /** NOISE_POP/WHOOSH only: how much a 1-pole low-pass filters the noise. Lower = duller/wetter, higher = crisper. */
    val noiseFilterAlpha: Double = 0.35,
    /** NOISE_POP only: how many discrete pops make up the burst — 1 for a single splat, more for an actual crackle (Firecracker String). */
    val noisePopCount: Int = 1,
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
        fireSoundStyle = WeaponFireSoundStyle.METALLIC,
        baseRadius = 70f,
        radiusGrowthPerLevel = 10f
    ),
    DAGGER_THROW(
        displayName = "Dagger Throw",
        description = "Hurls a dagger at the nearest enemy at regular intervals.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFFFFC107,
        icon = VectorIcon.Blade(lengthRatio = 1.5f),
        fireSoundHz = 1400.0, // high, crisp — a quick flick
        fireSoundWaveform = Waveform.SQUARE,
        fireSoundEndHz = 700.0,
        fireSoundStyle = WeaponFireSoundStyle.SWEEP,
        fireSoundDurationMs = 55
    ),
    ORBIT_SHIELD(
        displayName = "Orbit Shield",
        description = "Shields spin around the knight, damaging anything they touch.",
        targeting = WeaponTargeting.ORBIT,
        colorArgb = 0xFF66BB6A,
        icon = VectorIcon.TurtleShell,
        fireSoundHz = 540.0, // soft, sustained — a magical hum (not currently triggered; ORBIT ticks too often to sound a fire cue without spamming, see WeaponFiringSystem)
        fireSoundWaveform = Waveform.SINE,
        fireSoundStyle = WeaponFireSoundStyle.METALLIC,
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
        fireSoundWaveform = Waveform.SAWTOOTH,
        fireSoundStyle = WeaponFireSoundStyle.METALLIC
    ),
    SUNBURST(
        displayName = "Sunburst",
        description = "A blazing solar field that repeatedly scorches every enemy nearby.",
        targeting = WeaponTargeting.AREA_AROUND_PLAYER,
        colorArgb = 0xFFFFB300,
        icon = VectorIcon.Star(points = 8, innerRadiusRatio = 0.55f),
        fireSoundHz = 700.0,
        fireSoundWaveform = Waveform.SINE,
        fireSoundStyle = WeaponFireSoundStyle.CHIME,
        baseRadius = 50f,
        radiusGrowthPerLevel = 8f
    ),
    RUBBER_DUCK(
        displayName = "Rubber Duck Toss",
        description = "Bonks the nearest enemy with a squeaky rubber duck. Purely for morale.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFFFFEB3B,
        icon = VectorIcon.Duck,
        fireSoundHz = 780.0, // a quack: buzzy carrier gliding down with fast tremolo, not a smooth pitch slide
        fireSoundWaveform = Waveform.SAWTOOTH,
        fireSoundEndHz = 320.0,
        fireSoundStyle = WeaponFireSoundStyle.QUACK,
        fireSoundDurationMs = 150
    ),
    FRYING_PAN(
        displayName = "Frying Pan",
        description = "Bonks every enemy in range, with a chance to stun them silly.",
        targeting = WeaponTargeting.AREA_AROUND_PLAYER,
        colorArgb = 0xFF78909C,
        icon = VectorIcon.FryingPan,
        fireSoundHz = 190.0,
        fireSoundWaveform = Waveform.SQUARE,
        fireSoundStyle = WeaponFireSoundStyle.METALLIC_IMPACT,
        fireSoundDurationMs = 220,
        baseRadius = 55f,
        radiusGrowthPerLevel = 6f,
        stunChance = 0.25f,
        stunDurationMs = 700L
    ),
    BOOMERANG(
        displayName = "Boomerang",
        description = "Flies out through enemies, then homes back through them again.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFF8D6E63,
        icon = VectorIcon.Boomerang,
        fireSoundHz = 480.0,
        fireSoundWaveform = Waveform.SAWTOOTH,
        fireSoundStyle = WeaponFireSoundStyle.WHOOSH,
        fireSoundDurationMs = 180,
        returnsToPlayer = true
    ),
    MAGNET_PULSE(
        displayName = "Magnet Pulse",
        description = "A light jolt that yanks nearby enemies closer, setting them up for the rest of your kit.",
        targeting = WeaponTargeting.AREA_AROUND_PLAYER,
        colorArgb = 0xFFE53935,
        icon = VectorIcon.Magnet,
        fireSoundHz = 150.0,
        fireSoundWaveform = Waveform.SINE,
        fireSoundStyle = WeaponFireSoundStyle.PULSE,
        fireSoundDurationMs = 160,
        baseRadius = 90f,
        radiusGrowthPerLevel = 10f,
        pullStrengthPx = 26f
    ),
    FIRECRACKER_STRING(
        displayName = "Firecracker String",
        description = "Pops on impact into a shower of sparks that scorch nearby enemies too.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFFE53935,
        icon = VectorIcon.Firecracker,
        fireSoundHz = 700.0,
        fireSoundWaveform = Waveform.SQUARE,
        fireSoundStyle = WeaponFireSoundStyle.NOISE_POP,
        fireSoundDurationMs = 140,
        noiseFilterAlpha = 0.6, // crisp/sharp
        noisePopCount = 3, // an actual crackle, not one thud
        splashRadius = 45f,
        splashDamageFraction = 0.5f
    ),
    EGG_TOSS(
        displayName = "Egg Toss",
        description = "Splats on impact, leaving nearby enemies slipping and slowed.",
        targeting = WeaponTargeting.NEAREST_ENEMY,
        colorArgb = 0xFFFFF8E1,
        icon = VectorIcon.Egg,
        fireSoundHz = 380.0,
        fireSoundWaveform = Waveform.SINE,
        fireSoundStyle = WeaponFireSoundStyle.NOISE_POP,
        fireSoundDurationMs = 130,
        noiseFilterAlpha = 0.15, // dull/wet
        splashRadius = 50f,
        splashSlowDurationMs = 1500L,
        splashSlowMultiplier = 0.5f
    );

    /** Damage dealt per hit at [level] (1..[maxLevel]). */
    fun damageAtLevel(level: Int): Int = when (this) {
        SWORD_SLASH -> 16 + (level - 1) * 7
        DAGGER_THROW -> 10 + (level - 1) * 5
        ORBIT_SHIELD -> 8 + (level - 1) * 4
        THROWING_AXE -> 15 + (level - 1) * 7
        SUNBURST -> 4 + (level - 1) * 2
        RUBBER_DUCK -> 12 + (level - 1) * 5
        FRYING_PAN -> 14 + (level - 1) * 6
        BOOMERANG -> 11 + (level - 1) * 5
        MAGNET_PULSE -> 3 + (level - 1) * 2
        FIRECRACKER_STRING -> 12 + (level - 1) * 5
        EGG_TOSS -> 6 + (level - 1) * 3
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
        FRYING_PAN -> (1000 - (level - 1) * 80).coerceAtLeast(550).toLong()
        BOOMERANG -> (900 - (level - 1) * 70).coerceAtLeast(480).toLong()
        MAGNET_PULSE -> (1200 - (level - 1) * 90).coerceAtLeast(650).toLong()
        FIRECRACKER_STRING -> (750 - (level - 1) * 60).coerceAtLeast(380).toLong()
        EGG_TOSS -> (800 - (level - 1) * 60).coerceAtLeast(420).toLong()
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
        BOOMERANG -> 1 + (level - 1) / 2 // hits multiple enemies per pass, both out and back
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
            BOOMERANG -> 240f
            FIRECRACKER_STRING -> 300f
            EGG_TOSS -> 220f
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
