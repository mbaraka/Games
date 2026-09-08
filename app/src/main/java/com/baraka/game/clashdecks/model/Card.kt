package com.baraka.game.clashdecks.model

import com.baraka.game.knightsurvivor.model.VectorIcon

/**
 * A card definition. Immutable data — the same [Card] object is shared between the deck,
 * hand and discard; identity in a hand is by list position, and two copies of "Strike"
 * are the same object. [soundHz] is the base pitch of its play blip (see
 * `data/DuelSoundEngine.kt`).
 */
data class Card(
    val id: String,
    val name: String,
    val type: CardType,
    val cost: Int,
    val rarity: Rarity,
    val effects: List<Effect>,
    val icon: VectorIcon,
    val soundHz: Double,
    val flavor: String
) {
    /** "Deal 2 · Stun (1) → enemy" — built from [effects], shown on the card face and in the Codex. */
    val summary: String get() = effects.joinToString("  ·  ") { it.describe() }

    /** Raw offensive output ignoring buffs/block — used by the CPU's lethal check. */
    val nominalDamage: Int
        get() = effects.sumOf {
            when (it) {
                is Effect.Damage -> it.amount
                is Effect.Shatter -> it.bonusDamage
                else -> 0
            }
        }

    val nominalBlock: Int get() = effects.filterIsInstance<Effect.Block>().sumOf { it.amount }
    val nominalHeal: Int get() = effects.filterIsInstance<Effect.Heal>().sumOf { it.amount }
}
