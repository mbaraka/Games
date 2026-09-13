package com.baraka.game.numbercorridor.model

/**
 * The squad's stats and position. [squadCount] only ever changes via a left-lane
 * [WallKind.SquadShift] and [weaponTier] only via a right-lane [WallKind.WeaponCrate] —
 * gear comes entirely from the corridor, never from leveling or shopping.
 *
 * [squadCount] never widens how many lanes the squad occupies — like Mob Control's crowd,
 * more units means a denser cluster and more simultaneous shots from the *same* lane, not
 * a wider hitbox. A wide hitbox would make a big squad easier to hit on an ADD/MULTIPLY
 * wall and impossible to dodge a SUBTRACT/DIVIDE wall with, which defeats the point of
 * rewarding growth.
 */
data class PlayerState(
    val lane: Int,
    val squadCount: Int,
    val weaponTier: WeaponTier,
    val health: Int,
    val maxHealth: Int,
    /** One cooldown per squad member, all sharing [lane] — see systems/GameLoopOrchestrator.kt.fireWeapons. */
    val shooterCooldowns: List<Float> = List(1) { 0f }
) {
    val damage: Float get() = weaponTier.damage
    val fireRateMultiplier: Float get() = weaponTier.fireRateMultiplier

    companion object {
        const val MAX_SQUAD = 4
        const val MIN_SQUAD = 1
        const val BASE_FIRE_INTERVAL_SECONDS = 0.5f

        fun initial(): PlayerState {
            return PlayerState(
                lane = WallRow.ENEMY_LANE,
                squadCount = 1,
                weaponTier = WeaponTier.RECRUIT,
                health = 5,
                maxHealth = 5
            )
        }
    }
}
