package com.baraka.game.knightsurvivor.model

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
    val isBoss: Boolean = false
) {
    SKELETON(
        displayName = "Skeleton",
        baseHp = 18,
        baseSpeed = 70f,
        baseDamage = 8,
        xpValue = 3,
        radius = 14f,
        colorArgb = 0xFFE0E0E0,
        icon = VectorIcon.Face(eyeStyle = VectorIcon.EyeStyle.HOLLOW_SOCKET)
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
        icon = VectorIcon.Face(earStyle = VectorIcon.EarStyle.ROUND, hasTusks = true)
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
        isBoss = true
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
        icon = VectorIcon.Face(earStyle = VectorIcon.EarStyle.ROUND, hasTusks = true)
    );

    companion object {
        /** Every non-boss kind, used by EnemySpawnSystem's regular (non-boss) spawn roll. */
        val REGULAR: List<EnemyKind> = entries.filterNot { it.isBoss }

        /** Every boss kind, used by EnemySpawnSystem's periodic boss spawn. */
        val BOSSES: List<EnemyKind> = entries.filter { it.isBoss }
    }
}
