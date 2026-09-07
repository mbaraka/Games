package com.baraka.game.knightsurvivor.model

/**
 * Stat table for every weapon archetype in the game.
 *
 * To add a new weapon that reuses an existing [WeaponTargeting] pattern (e.g. another
 * homing projectile with different numbers), add ONE enum entry here and extend the
 * `when` in each stat function below — no other file needs to change. Only a weapon with
 * a genuinely new *firing shape* needs a new [WeaponTargeting] case, which in turn needs
 * one new `when` branch in systems/WeaponFiringSystem.kt.
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
    val maxLevel: Int = 5
) {
    SWORD_SLASH(
        displayName = "Sword Slash",
        description = "Swings a blade around the knight, hitting every enemy in range.",
        targeting = WeaponTargeting.AREA_AROUND_PLAYER,
        colorArgb = 0xFFCFD8DC,
        icon = VectorIcon.Blade(lengthRatio = 2.2f),
        fireSoundHz = 320.0, // low, broad — a heavy swing
        fireSoundWaveform = Waveform.SAWTOOTH
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
        colorArgb = 0xFF29B6F6,
        icon = VectorIcon.Shield,
        fireSoundHz = 540.0, // soft, sustained — a magical hum (not currently triggered; ORBIT ticks too often to sound a fire cue without spamming, see WeaponFiringSystem)
        fireSoundWaveform = Waveform.SINE
    );

    /** Damage dealt per hit at [level] (1..[maxLevel]). */
    fun damageAtLevel(level: Int): Int = when (this) {
        SWORD_SLASH -> 16 + (level - 1) * 7
        DAGGER_THROW -> 10 + (level - 1) * 5
        ORBIT_SHIELD -> 8 + (level - 1) * 4
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
    }

    /** Hit radius in px. Used as the AoE radius for AREA_AROUND_PLAYER and the orbit radius for ORBIT. */
    fun radiusAtLevel(level: Int): Float = when (this) {
        SWORD_SLASH -> 70f + (level - 1) * 10f
        ORBIT_SHIELD -> 60f + (level - 1) * 4f
        DAGGER_THROW -> 0f
    }

    /** Number of simultaneous orbiting instances (ORBIT_SHIELD only, 0 for every other kind). */
    fun orbCountAtLevel(level: Int): Int = when (this) {
        ORBIT_SHIELD -> 1 + (level - 1) / 2 // L1-2: 1 orb, L3-4: 2 orbs, L5: 3 orbs
        else -> 0
    }

    /** Enemies a single projectile can pierce through before it's consumed (DAGGER_THROW only). */
    fun pierceAtLevel(level: Int): Int = when (this) {
        DAGGER_THROW -> if (level >= 4) 1 else 0
        else -> 0
    }

    /** Projectile travel speed in px/s (only meaningful for NEAREST_ENEMY weapons). */
    val projectileSpeed: Float
        get() = when (this) {
            DAGGER_THROW -> 260f
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
