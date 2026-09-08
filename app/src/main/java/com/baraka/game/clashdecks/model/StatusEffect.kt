package com.baraka.game.clashdecks.model

/**
 * One live instance of a [StatusKind] on a duelist: [amount] is its magnitude (damage per
 * poison tick, bonus/malus damage for empower/weaken/vulnerable, heal per regen; ignored
 * for stun/freeze/root), [turnsRemaining] counts down at the start of the afflicted
 * duelist's turn.
 */
data class StatusEffect(
    val kind: StatusKind,
    val amount: Int,
    val turnsRemaining: Int
)

/** Sum of every live instance of [kind] (statuses of the same kind are merged, but be defensive). */
fun List<StatusEffect>.amountOf(kind: StatusKind): Int =
    filter { it.kind == kind }.sumOf { it.amount }

fun List<StatusEffect>.has(kind: StatusKind): Boolean = any { it.kind == kind }

/**
 * Adds [incoming], merging with an existing status of the same kind: magnitudes add,
 * duration refreshes to the longer of the two. Keeps the list to one entry per kind.
 */
fun List<StatusEffect>.merged(incoming: StatusEffect): List<StatusEffect> {
    val existing = firstOrNull { it.kind == incoming.kind }
        ?: return this + incoming
    return map {
        if (it.kind == incoming.kind) {
            it.copy(
                amount = it.amount + incoming.amount,
                turnsRemaining = maxOf(it.turnsRemaining, incoming.turnsRemaining)
            )
        } else it
    }
}

/** Decrements every timer by one turn and drops the ones that have run out. */
fun List<StatusEffect>.tickedDown(): List<StatusEffect> =
    mapNotNull { if (it.turnsRemaining <= 1) null else it.copy(turnsRemaining = it.turnsRemaining - 1) }
