package com.baraka.game.clashdecks.model

import kotlin.random.Random

/**
 * One side of a duel. Deck/hand/discard are immutable lists rebuilt on every change so a
 * whole [DuelState] can be `copy()`d cheaply and compared in tests.
 *
 * [block] is spent absorbing damage and is cleared at the start of this duelist's own next
 * turn (so it protects through the opponent's turn — see `DuelEngine.beginTurn`).
 * [reflectPending] is charged by `Riposte` and discharged by the next hit that reaches HP.
 */
data class Duelist(
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val energy: Int,
    val maxEnergyThisTurn: Int,
    val block: Int,
    val statuses: List<StatusEffect>,
    val reflectPending: Int,
    val deck: List<Card>,
    val hand: List<Card>,
    val discard: List<Card>,
    val isCpu: Boolean
) {
    val isAlive: Boolean get() = hp > 0

    fun canAfford(card: Card): Boolean = energy >= card.cost

    fun amountOf(kind: StatusKind): Int = statuses.amountOf(kind)
    fun hasStatus(kind: StatusKind): Boolean = statuses.has(kind)

    /** Effective HP an attacker must chew through right now (block counts, poison-on-self does not). */
    val effectiveHp: Int get() = hp + block

    /**
     * Draws [n] cards, reshuffling the discard pile into the deck when it runs dry. If both
     * are empty the draw simply yields fewer cards (no deck-out loss in v1).
     */
    fun drawCards(n: Int, rng: Random): Duelist {
        var d = deck
        var h = hand
        var disc = discard
        repeat(n) {
            if (d.isEmpty()) {
                if (disc.isEmpty()) return@repeat
                d = disc.shuffled(rng)
                disc = emptyList()
            }
            h = h + d.first()
            d = d.drop(1)
        }
        return copy(deck = d, hand = h, discard = disc)
    }

    companion object {
        fun fresh(name: String, isCpu: Boolean, startHp: Int, deck: List<Card>): Duelist = Duelist(
            name = name,
            hp = startHp,
            maxHp = startHp,
            energy = 0,
            maxEnergyThisTurn = 0,
            block = 0,
            statuses = emptyList(),
            reflectPending = 0,
            deck = deck,
            hand = emptyList(),
            discard = emptyList(),
            isCpu = isCpu
        )
    }
}
