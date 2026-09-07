package com.baraka.game.knightsurvivor.model

/**
 * Stat table for every passive stat boost offered by the level-up upgrade screen.
 *
 * Adding a new passive is one entry here plus one `when` branch wherever that stat is
 * actually consumed (move speed -> PlayerMovementSystem, cooldown reduction ->
 * WeaponFiringSystem, pickup radius -> XpAndLevelingSystem) — it never touches the
 * upgrade-choice UI, which only ever renders [displayName]/[description].
 */
enum class PassiveKind(
    val displayName: String,
    val description: String,
    val maxStacks: Int
) {
    MOVE_SPEED(
        displayName = "Swift Boots",
        description = "Move speed +8% per stack.",
        maxStacks = 5
    ),
    MAX_HP(
        displayName = "Vitality",
        description = "Max HP +20 per stack (also heals for the amount gained).",
        maxStacks = 5
    ),
    PICKUP_RADIUS(
        displayName = "Magnet Charm",
        description = "XP pickup radius +15% per stack.",
        maxStacks = 5
    ),
    COOLDOWN_REDUCTION(
        displayName = "Battle Focus",
        description = "All weapon cooldowns -5% per stack.",
        maxStacks = 5
    );

    /**
     * The effective value contributed by owning [stacks] copies of this passive.
     * Semantics depend on the kind: a speed/cooldown *multiplier* for MOVE_SPEED,
     * PICKUP_RADIUS and COOLDOWN_REDUCTION, or a flat bonus for MAX_HP.
     */
    fun magnitudeAtStacks(stacks: Int): Float = when (this) {
        MOVE_SPEED -> 1f + stacks * 0.08f
        MAX_HP -> stacks * 20f
        PICKUP_RADIUS -> 1f + stacks * 0.15f
        COOLDOWN_REDUCTION -> (1f - stacks * 0.05f).coerceAtLeast(0.6f)
    }
}
