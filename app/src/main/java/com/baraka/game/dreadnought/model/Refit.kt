package com.baraka.game.dreadnought.model

/**
 * A ship upgrade. The same list is used twice: bought for coins in the harbour (permanent levels) and offered
 * three at a time between waves (one extra level for the rest of the sortie). Add an entry, then read
 * `game.level(Refit.X)` where its effect belongs.
 */
enum class Refit(val label: String, val blurb: String, val baseCost: Int, val maxLevel: Int = 6) {
    MAIN_DAMAGE("Heavy shells", "+12% main battery damage per level", 120),
    RELOAD("Autoloaders", "-8% main battery reload per level", 140),
    FIRE_CONTROL("Fire control", "Tighter shell spread, -14% per level", 130),
    RANGE("Rangefinders", "+7% main battery range per level", 90),
    ARMOR("Belt armour", "+14% hull strength per level", 150),
    ENGINES("Engines", "+7% speed and turning per level", 110),
    SECONDARIES("Secondary guns", "+18% secondary gun damage per level", 100),
    DAMAGE_CONTROL("Damage control", "Stronger repair that recharges sooner", 130);

    /** Price of the next level when [level] have been bought. */
    fun cost(level: Int): Int = baseCost * (level + 1)
}

/** A sea to fight on. Palettes are ARGB; later theatres unlock by reaching a wave in any sortie. */
enum class Theatre(
    val label: String,
    val blurb: String,
    val unlockWave: Int,
    val deep: Long,
    val shallow: Long,
    val swell: Long,
    val ice: Boolean = false
) {
    ATLANTIC("North Atlantic", "Grey swell and long shadows.", 0, 0xFF1D3B52, 0xFF27506C, 0xFF6F93AB),
    PACIFIC("Pacific", "Deep blue, bright water.", 5, 0xFF0D4A78, 0xFF1768A3, 0xFF8EC8EC),
    ARCTIC("Arctic Convoy", "Ice floes drifting in black water.", 10, 0xFF132F44, 0xFF1D4560, 0xFFB6D4E6, ice = true)
}
