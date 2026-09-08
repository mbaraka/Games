package com.baraka.game.clashdecks.model

/** Flavor + Codex grouping only in v1 — every card is in the shared pool regardless of rarity. */
enum class Rarity(val displayName: String, val colorArgb: Long) {
    COMMON("Common", 0xFFB0BEC5),
    UNCOMMON("Uncommon", 0xFF4FC3F7),
    RARE("Rare", 0xFFFFD54F)
}
