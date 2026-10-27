package com.baraka.game.knightsurvivor.balance

import com.baraka.game.knightsurvivor.model.EnemyKind

/**
 * Every cross-cutting balance number for Knight Survivor lives here.
 *
 * Per-archetype stat tables (EnemyKind/WeaponKind/PassiveKind) stay in their own model
 * files since they're structurally tied to adding/removing a kind — but any plain number
 * a designer would iterate on WITHOUT adding a new kind (curves, thresholds, timing
 * constants) belongs here, so a balance pass never requires opening a systems/ file.
 */
object GameTunables {

    // --- Arena / fallback layout -------------------------------------------------------
    /** Used only until the Composable reports its real measured size on first layout. */
    const val DEFAULT_ARENA_WIDTH = 400f
    const val DEFAULT_ARENA_HEIGHT = 640f

    // --- Simulation step -----------------------------------------------------------------
    /** Fixed tick length, matching the ~60fps `delay(16)` loop every other game in this app uses. */
    const val TICK_MS = 16L

    // --- Visuals --------------------------------------------------------------------------
    /** Draw everything (knight, enemies, projectiles, pickups, gems) this much bigger than
     *  their actual collision radius - visibility only, never read by systems/, so hitboxes
     *  and balance stay exactly as tuned. Applied at the DrawScope boundary in render/. */
    const val VISUAL_SCALE = 2f

    // --- Player ---------------------------------------------------------------------------
    const val PLAYER_BASE_MAX_HP = 100
    const val PLAYER_BASE_SPEED_PX_PER_SEC = 150f
    const val PLAYER_BASE_PICKUP_RADIUS = 55f
    const val PLAYER_INVULNERABILITY_MS = 500L

    // --- Health pickups ---------------------------------------------------------------------
    const val HEALTH_PICKUP_HEAL_AMOUNT = 15

    // --- XP / leveling ----------------------------------------------------------------------
    /** XP required to go from [level] to level+1. */
    fun xpRequiredForLevel(level: Int): Int = 10 + (level - 1) * 8

    const val XP_GEM_MAGNET_SPEED_PX_PER_SEC = 260f
    const val XP_GEM_COLLECT_DISTANCE = 14f

    // --- Upgrade choices --------------------------------------------------------------------
    const val MAX_ACTIVE_WEAPONS = 4
    const val UPGRADE_CHOICES_PER_LEVEL_UP = 3

    // --- Weapon visual effects ------------------------------------------------------------
    /** How long an AREA_AROUND_PLAYER weapon's fading ring stays on screen after activating. */
    const val WEAPON_EFFECT_VISIBLE_MS = 250L
    /** How far a NEAREST_ENEMY weapon will look for a target before giving up on firing this cycle. */
    const val WEAPON_TARGETING_RANGE = 500f

    // --- Projectiles ------------------------------------------------------------------------
    /** A projectile drifting this far outside the arena bounds is removed. */
    const val PROJECTILE_DESPAWN_MARGIN = 60f

    // --- Enemy spawning & difficulty curve --------------------------------------------------
    const val SPAWN_EDGE_MARGIN = 40f
    const val MAX_LIVE_ENEMIES = 90

    /** Milliseconds between regular (non-boss) enemy spawns at [elapsedSeconds] into the run. */
    fun spawnIntervalMsAt(elapsedSeconds: Float): Long =
        (1400L - (elapsedSeconds * 9L).toLong()).coerceAtLeast(280L)

    /** HP/damage multiplier applied to EnemyKind's base stats at [elapsedSeconds] into the run. */
    fun enemyPowerMultiplierAt(elapsedSeconds: Float): Float = 1f + elapsedSeconds / 40f

    /** Speed multiplier applied to EnemyKind's base speed at [elapsedSeconds] into the run (capped so enemies never outrun the player too badly). */
    fun enemySpeedMultiplierAt(elapsedSeconds: Float): Float = 1f + (elapsedSeconds / 150f).coerceAtMost(0.6f)

    /** How long a regular enemy has been eligible to spawn, and its relative spawn weight once it is. Skeleton is always available. */
    private val enemyUnlockSeconds: Map<EnemyKind, Float> = mapOf(
        EnemyKind.SKELETON to 0f,
        EnemyKind.SLIME to 10f,
        EnemyKind.GOBLIN to 20f,
        EnemyKind.IMP to 30f,
        EnemyKind.BAT to 35f,
        EnemyKind.MIMIC to 45f,
        EnemyKind.ORC to 50f,
        EnemyKind.GHOST to 60f,
        EnemyKind.GOLEM to 65f,
        EnemyKind.OGRE to 75f
    )
    private val enemySpawnWeights: Map<EnemyKind, Float> = mapOf(
        EnemyKind.SKELETON to 5f,
        EnemyKind.SLIME to 3f,
        EnemyKind.GOBLIN to 3f,
        EnemyKind.IMP to 2.5f,
        EnemyKind.BAT to 3f,
        EnemyKind.MIMIC to 1f,
        EnemyKind.ORC to 2f,
        EnemyKind.GHOST to 2f,
        EnemyKind.GOLEM to 1f,
        EnemyKind.OGRE to 1.5f
    )

    /** The [EnemyKind, weight] pairs currently eligible to spawn at [elapsedSeconds]. Always non-empty (skeleton unlocks at 0s). */
    fun availableRegularEnemyWeights(elapsedSeconds: Float): List<Pair<EnemyKind, Float>> =
        EnemyKind.REGULAR
            .filter { kind -> elapsedSeconds >= (enemyUnlockSeconds[kind] ?: 0f) }
            .map { kind -> kind to (enemySpawnWeights[kind] ?: 1f) }

    // --- Boss spawns --------------------------------------------------------------------------
    const val BOSS_SPAWN_INTERVAL_MS = 60_000L

    // --- Victory ------------------------------------------------------------------------------
    const val VICTORY_SURVIVAL_TIME_MS = 10 * 60 * 1000L
}
