package com.baraka.game.clashdecks

import com.baraka.game.clashdecks.ai.CpuStrategy
import com.baraka.game.clashdecks.ai.Difficulty
import com.baraka.game.clashdecks.model.Card
import com.baraka.game.clashdecks.model.CardType
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.Duelist
import com.baraka.game.clashdecks.model.Effect
import com.baraka.game.clashdecks.model.Rarity
import com.baraka.game.knightsurvivor.model.VectorIcon
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CpuStrategyTest {

    private val rng = Random(4)

    private fun card(id: String, cost: Int, type: CardType, vararg effects: Effect) =
        Card(id, id, type, cost, Rarity.COMMON, effects.toList(), VectorIcon.Shield, 400.0, "")

    private fun cpuState(cpuHand: List<Card>, cpuHp: Int = 20, playerHp: Int = 20, cpuEnergy: Int = 5): DuelState {
        val cpu = Duelist("C", cpuHp, 20, cpuEnergy, cpuEnergy, 0, emptyList(), 0, emptyList(), cpuHand, emptyList(), true)
        val player = Duelist("P", playerHp, 20, 0, 0, 0, emptyList(), 0, emptyList(), emptyList(), emptyList(), false)
        return DuelState(
            player, cpu, DuelPhase.CPU_TURN,
            activeIsPlayer = false, turnNumber = 3,
            activeStunned = false, activeRooted = false,
            log = emptyList(), pendingSounds = emptyList(), winner = null
        )
    }

    @Test
    fun normal_takesLethalWhenAvailable() {
        val big = card("big", 3, CardType.ATTACK, Effect.Damage(6))
        val small = card("small", 1, CardType.ATTACK, Effect.Damage(1))
        val plan = CpuStrategy.chooseTurn(cpuState(listOf(small, big), playerHp = 5), Difficulty.NORMAL, rng)
        assertTrue("expected the lethal card to be played", plan.contains("big"))
    }

    @Test
    fun normal_prefersDefenseWhenLow() {
        val wall = card("wall", 3, CardType.DEFENSE, Effect.Block(8))
        val poke = card("poke", 3, CardType.ATTACK, Effect.Damage(2))
        val plan = CpuStrategy.chooseTurn(cpuState(listOf(poke, wall), cpuHp = 4), Difficulty.NORMAL, rng)
        assertTrue("expected block first when near death", plan.firstOrNull() == "wall")
    }

    @Test
    fun easy_playsUntilOutOfEnergy() {
        val a = card("a", 2, CardType.ATTACK, Effect.Damage(2))
        val b = card("b", 2, CardType.ATTACK, Effect.Damage(2))
        val c = card("c", 2, CardType.SUPPORT, Effect.Heal(2))
        val plan = CpuStrategy.chooseTurn(cpuState(listOf(a, b, c), cpuEnergy = 4), Difficulty.EASY, rng)
        assertTrue(plan.size == 2)
    }

    @Test
    fun cpuTurn_neverThrows_onFullLibraryHand() {
        val hand = com.baraka.game.clashdecks.model.CardLibrary.all.take(6)
        val plan = CpuStrategy.chooseTurn(cpuState(hand, cpuEnergy = 8), Difficulty.HARD, rng)
        assertTrue(plan.size in 0..16)
    }
}
