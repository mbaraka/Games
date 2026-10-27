package com.baraka.game.knightsurvivor.model

import com.baraka.game.ui.icons.VectorIcon

/**
 * Stat table for every enemy archetype in the game.
 *
 * A new "more of the same" enemy (tankier, faster, whatever) is just one new entry here
 * — systems/EnemySpawnSystem.kt, systems/EnemyMovementSystem.kt and render/EnemyRenderer.kt
 * only need edits if the new kind needs genuinely different spawn weighting, movement AI,
 * or a distinct silhouette.
 *
 * Time-based scaling (enemies getting tougher the longer a run goes) is NOT baked in here —
 * it lives in balance/GameTunables.kt and is applied by EnemySpawnSystem, so these base
 * stats stay simple "level 0" numbers.
 */
enum class EnemyKind(
    val displayName: String,
    val baseHp: Int,
    val baseSpeed: Float, // px/s
    val baseDamage: Int,
    val xpValue: Int,
    val radius: Float,
    val colorArgb: Long,
    val icon: VectorIcon,
    val isBoss: Boolean = false,
    /** Chance this kind drops a heal pickup alongside its XP gem on death. Defaults to none — set explicitly per kind. */
    val healthDropChance: Float = 0f,
    /** False for a kind that should never be picked by EnemySpawnSystem's regular roll directly — e.g. Slime's split result, which only ever appears via [splitsOnDeathInto]. */
    val directlySpawnable: Boolean = true,
    /** Cycles vulnerable/invulnerable on a fixed period (see EnemyInstance.isPhaseInvulnerableAt) — Ghost only. */
    val phasesInvulnerable: Boolean = false,
    /** Briefly roots the player on contact, on top of normal contact damage — Golem only. */
    val rootsPlayerOnHit: Boolean = false,
    val rootDurationMs: Long = 0L,
    /** Spawns disguised as a health pickup, immobile and harmless, until the player wanders close — Mimic only. See systems/EnemyMovementSystem.kt. */
    val disguisesAsPickup: Boolean = false,
    /** Periodically fires an [EnemyProjectile] at the player instead of relying purely on contact damage — Imp only. See systems/EnemyRangedAttackSystem.kt. */
    val hasRangedAttack: Boolean = false,
    val rangedAttackDamage: Int = 0,
    val rangedAttackCooldownMs: Long = 0L,
    val rangedAttackRangePx: Float = 0f,
    val rangedProjectileSpeed: Float = 0f
) {
    SKELETON(
        displayName = "Skeleton",
        baseHp = 18,
        baseSpeed = 70f,
        baseDamage = 8,
        xpValue = 3,
        radius = 14f,
        colorArgb = 0xFFE0E0E0,
        icon = VectorIcon.Face(eyeStyle = VectorIcon.EyeStyle.HOLLOW_SOCKET),
        healthDropChance = 0.05f
    ),
    GOBLIN(
        displayName = "Goblin",
        baseHp = 10,
        baseSpeed = 120f,
        baseDamage = 5,
        xpValue = 4,
        radius = 11f,
        colorArgb = 0xFF66BB6A,
        icon = VectorIcon.Face(earStyle = VectorIcon.EarStyle.POINTY)
    ),
    ORC(
        displayName = "Orc",
        baseHp = 55,
        baseSpeed = 45f,
        baseDamage = 14,
        xpValue = 10,
        radius = 20f,
        colorArgb = 0xFF8D6E63,
        icon = VectorIcon.Face(earStyle = VectorIcon.EarStyle.ROUND, hasTusks = true),
        healthDropChance = 0.12f
    ),
    BONE_KING(
        displayName = "Bone King",
        baseHp = 400,
        baseSpeed = 40f,
        baseDamage = 22,
        xpValue = 80,
        radius = 32f,
        colorArgb = 0xFFD7CCC8, // pale bone tan — a purple head read oddly on a "skull" face
        icon = VectorIcon.Face(eyeStyle = VectorIcon.EyeStyle.HOLLOW_SOCKET, hasCrown = true),
        isBoss = true,
        healthDropChance = 1f
    ),
    BAT(
        displayName = "Bat",
        baseHp = 7,
        baseSpeed = 150f,
        baseDamage = 4,
        xpValue = 3,
        radius = 10f,
        colorArgb = 0xFF4A148C,
        icon = VectorIcon.Face(earStyle = VectorIcon.EarStyle.POINTY)
    ),
    OGRE(
        displayName = "Ogre",
        baseHp = 90,
        baseSpeed = 35f,
        baseDamage = 20,
        xpValue = 16,
        radius = 26f,
        colorArgb = 0xFF5D4037,
        icon = VectorIcon.Face(earStyle = VectorIcon.EarStyle.ROUND, hasTusks = true),
        healthDropChance = 0.18f
    ),
    SLIME(
        displayName = "Slime",
        baseHp = 16,
        baseSpeed = 55f,
        baseDamage = 6,
        xpValue = 4,
        radius = 15f,
        colorArgb = 0xFF69F0AE,
        icon = VectorIcon.Slime
    ),
    SLIME_MINI(
        displayName = "Slime (Split)",
        baseHp = 6,
        baseSpeed = 65f,
        baseDamage = 4,
        xpValue = 2,
        radius = 9f,
        colorArgb = 0xFF69F0AE,
        icon = VectorIcon.Slime,
        directlySpawnable = false
    ),
    GHOST(
        displayName = "Ghost",
        baseHp = 22,
        baseSpeed = 85f,
        baseDamage = 9,
        xpValue = 6,
        radius = 15f,
        colorArgb = 0xFFE1F5FE,
        icon = VectorIcon.Ghost,
        phasesInvulnerable = true
    ),
    IMP(
        displayName = "Imp",
        baseHp = 12,
        baseSpeed = 130f,
        baseDamage = 5,
        xpValue = 6,
        radius = 12f,
        colorArgb = 0xFFD84315,
        icon = VectorIcon.Imp,
        hasRangedAttack = true,
        rangedAttackDamage = 6,
        rangedAttackCooldownMs = 1800L,
        rangedAttackRangePx = 260f,
        rangedProjectileSpeed = 220f
    ),
    GOLEM(
        displayName = "Golem",
        baseHp = 130,
        baseSpeed = 30f,
        baseDamage = 18,
        xpValue = 18,
        radius = 27f,
        colorArgb = 0xFF78909C,
        icon = VectorIcon.Golem,
        healthDropChance = 0.15f,
        rootsPlayerOnHit = true,
        rootDurationMs = 500L
    ),
    MIMIC(
        displayName = "Mimic",
        baseHp = 45,
        baseSpeed = 95f,
        baseDamage = 16,
        xpValue = 14,
        radius = 16f,
        colorArgb = 0xFF8D6E63,
        icon = VectorIcon.Mimic,
        healthDropChance = 0.3f,
        disguisesAsPickup = true
    );

    /**
     * On death, spawns two of this kind at half HP nearby (Slime's gimmick). A `when`
     * rather than a constructor field: Kotlin enum constants can't forward-reference a
     * later constant, and SLIME_MINI is declared after SLIME.
     */
    val splitsOnDeathInto: EnemyKind?
        get() = when (this) {
            SLIME -> SLIME_MINI
            else -> null
        }

    companion object {
        /** Every non-boss, directly-spawnable kind, used by EnemySpawnSystem's regular (non-boss) spawn roll. */
        val REGULAR: List<EnemyKind> = entries.filterNot { it.isBoss }.filter { it.directlySpawnable }

        /** Every boss kind, used by EnemySpawnSystem's periodic boss spawn. */
        val BOSSES: List<EnemyKind> = entries.filter { it.isBoss }
    }
}
