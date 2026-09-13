package com.baraka.game.numbercorridor.model

/**
 * The squad's weapon, upgraded (or occasionally downgraded) by shooting down right-lane
 * [WallKind.WeaponCrate] tiles — see systems/CollisionSystem.kt. Unlike squad size or
 * enemy hp, weapon tier isn't taught via +/-/x/÷: it's a straightforward "stronger gun"
 * progression, since the right lane is about gearing up rather than arithmetic.
 */
enum class WeaponTier(val displayName: String, val damage: Float, val fireRateMultiplier: Float, val colorArgb: Long) {
    RECRUIT("Recruit Pistol", damage = 8f, fireRateMultiplier = 1.0f, colorArgb = 0xFF9E9E9E),
    TROOPER("Trooper SMG", damage = 13f, fireRateMultiplier = 1.2f, colorArgb = 0xFF42A5F5),
    GUNNER("Gunner Rifle", damage = 20f, fireRateMultiplier = 1.35f, colorArgb = 0xFFFFA726),
    HEAVY("Heavy Cannon", damage = 30f, fireRateMultiplier = 1.5f, colorArgb = 0xFFEF5350),
    JUGGERNAUT("Juggernaut Blaster", damage = 45f, fireRateMultiplier = 1.7f, colorArgb = 0xFFAB47BC);

    fun shifted(delta: Int): WeaponTier {
        val target = (ordinal + delta).coerceIn(0, entries.lastIndex)
        return entries[target]
    }
}
