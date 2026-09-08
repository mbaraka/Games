package com.baraka.game.clashdecks.model

/**
 * The three families a [Card] belongs to. Drives the card's color stripe in the UI, its
 * play sound, and some CPU heuristics. Any actual behavior lives in the card's [Effect]
 * list, not here — this is purely categorical.
 */
enum class CardType(val displayName: String, val colorArgb: Long) {
    ATTACK("Attack", 0xFFE53935),
    DEFENSE("Defense", 0xFF42A5F5),
    SUPPORT("Support", 0xFF66BB6A)
}
