package com.baraka.game.clashdecks.ai

import com.baraka.game.clashdecks.model.Card
import com.baraka.game.clashdecks.model.CardType
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.Effect
import com.baraka.game.clashdecks.model.StatusKind
import com.baraka.game.clashdecks.systems.DuelEngine
import kotlin.random.Random

enum class Difficulty(val displayName: String) { EASY("Easy"), NORMAL("Normal"), HARD("Hard") }

/**
 * Decides the CPU's whole turn as an ordered list of card ids. It plans by actually
 * running [DuelEngine.playCard] on a copy of the state, so a plan that spends energy it
 * gains mid-turn (Focus → a big card) stays legal, and it stops the moment it would win.
 *
 * The CPU never sees the player's hand — threat assessment is a rough guess from the
 * player's next-turn energy.
 */
object CpuStrategy {

    fun chooseTurn(state: DuelState, difficulty: Difficulty, rng: Random): List<String> {
        var s = state
        val plays = mutableListOf<String>()
        var guard = 0
        while (guard++ < 16) {
            val me = s.cpu
            val playable = me.hand.filter { card ->
                me.energy >= card.cost && !(s.activeRooted && card.type == CardType.DEFENSE)
            }
            if (playable.isEmpty()) break

            val pick = when (difficulty) {
                Difficulty.EASY -> playable.random(rng)
                Difficulty.NORMAL, Difficulty.HARD -> bestCard(s, playable, difficulty) ?: break
            }
            plays += pick.id
            s = DuelEngine.playCard(s, pick.id, rng)
            if (s.winner != null) break
        }
        return plays
    }

    private fun bestCard(s: DuelState, options: List<Card>, difficulty: Difficulty): Card? {
        val me = s.cpu
        val opp = s.player
        val lowHp = me.hp <= 7
        val threatened = difficulty == Difficulty.HARD && me.hp <= 10

        var best: Card? = null
        var bestScore = Int.MIN_VALUE
        for (card in options) {
            var score = -card.cost // slight nudge toward efficiency

            val dmg = projectedDamage(card, me, opp)
            if (dmg >= opp.effectiveHp && dmg > 0) score += 1000 // lethal
            score += dmg * 3

            if (card.nominalBlock > 0) score += card.nominalBlock * (if (lowHp || threatened) 5 else 2)
            if (card.nominalHeal > 0) score += card.nominalHeal * (if (lowHp) 6 else 1)

            for (effect in card.effects) {
                when (effect) {
                    is Effect.ApplyStatus -> score += when (effect.kind) {
                        StatusKind.FREEZE -> 14
                        StatusKind.STUN -> 9
                        StatusKind.EMPOWER -> 7
                        StatusKind.VULNERABLE, StatusKind.WEAKEN -> 5
                        StatusKind.POISON -> 6 + effect.amount * effect.turns
                        StatusKind.ROOT -> 4
                        StatusKind.REGEN -> if (lowHp) 8 else 3
                        StatusKind.WARDED -> if (threatened) 6 else 3
                    }
                    is Effect.DrawCards -> score += 3 * effect.count
                    is Effect.GainEnergy -> score += 2 * effect.amount
                    is Effect.Cleanse -> score += if (me.statuses.any { it.kind.isDebuff }) 9 else -6
                    is Effect.ReflectNextHit -> score += if (threatened) 6 else 2
                    is Effect.Shatter -> score += if (opp.block > 0) opp.block else -2
                    // ScalingDamage's guaranteed base is already folded into `dmg` above via
                    // Card.nominalDamage — no extra scoring needed here, just don't fall
                    // through to a compile error on this now-non-exhaustive `when`.
                    is Effect.ScalingDamage -> {}
                    else -> {}
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = card
            }
        }
        // Don't play purely negative-value cards (e.g. Cleanse with nothing to clean).
        return if (bestScore < 0) null else best
    }

    private fun projectedDamage(card: Card, me: com.baraka.game.clashdecks.model.Duelist, opp: com.baraka.game.clashdecks.model.Duelist): Int {
        if (card.nominalDamage == 0) return 0
        var d = card.nominalDamage
        d -= me.amountOf(StatusKind.WEAKEN)
        d += me.amountOf(StatusKind.EMPOWER)
        d += opp.amountOf(StatusKind.VULNERABLE)
        return d.coerceAtLeast(0)
    }
}
