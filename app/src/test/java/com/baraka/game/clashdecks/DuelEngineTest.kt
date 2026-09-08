package com.baraka.game.clashdecks

import com.baraka.game.clashdecks.model.Card
import com.baraka.game.clashdecks.model.CardType
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelSound
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.DuelWinner
import com.baraka.game.clashdecks.model.Duelist
import com.baraka.game.clashdecks.model.Effect
import com.baraka.game.clashdecks.model.EffectTarget
import com.baraka.game.clashdecks.model.StatusEffect
import com.baraka.game.clashdecks.model.StatusKind
import com.baraka.game.clashdecks.systems.DuelEngine
import com.baraka.game.knightsurvivor.model.VectorIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DuelEngineTest {

    private val rng = Random(1)

    private fun card(id: String, cost: Int, type: CardType, vararg effects: Effect) =
        Card(id, id, type, cost, com.baraka.game.clashdecks.model.Rarity.COMMON, effects.toList(), VectorIcon.Shield, 400.0, "")

    private fun duelist(name: String, isCpu: Boolean, hp: Int = 20, energy: Int = 5, hand: List<Card> = emptyList()) =
        Duelist(name, hp, 20, energy, energy, 0, emptyList(), 0, emptyList(), hand, emptyList(), isCpu)

    private fun state(
        player: Duelist,
        cpu: Duelist,
        activeIsPlayer: Boolean = true,
        stunned: Boolean = false,
        rooted: Boolean = false
    ) = DuelState(
        player, cpu,
        phase = if (activeIsPlayer) DuelPhase.PLAYER_TURN else DuelPhase.CPU_TURN,
        activeIsPlayer = activeIsPlayer,
        turnNumber = 1,
        activeStunned = stunned,
        activeRooted = rooted,
        log = emptyList(),
        pendingSounds = emptyList(),
        winner = null
    )

    @Test
    fun startDuel_dealsThreeCardsEachAndPlayerActs() {
        val s = DuelEngine.startDuel(rng)
        assertEquals(3, s.player.hand.size)
        assertEquals(3, s.cpu.hand.size)
        assertTrue(s.activeIsPlayer)
        assertEquals(3, s.player.energy)
        assertEquals(20, s.player.hp)
    }

    @Test
    fun damage_isAbsorbedByBlockThenHp() {
        val hit = card("hit", 1, CardType.ATTACK, Effect.Damage(5))
        val s = state(
            player = duelist("P", false, hand = listOf(hit)),
            cpu = duelist("C", true).copy(block = 2)
        )
        val out = DuelEngine.playCard(s, "hit", rng)
        assertEquals(0, out.cpu.block)
        assertEquals(17, out.cpu.hp) // 5 dmg - 2 block = 3 to hp
    }

    @Test
    fun weakenEmpowerVulnerable_stackOnOneHit() {
        val hit = card("hit", 1, CardType.ATTACK, Effect.Damage(5))
        val attacker = duelist("P", false, hand = listOf(hit))
            .copy(statuses = listOf(StatusEffect(StatusKind.EMPOWER, 3, 2), StatusEffect(StatusKind.WEAKEN, 1, 2)))
        val defender = duelist("C", true).copy(statuses = listOf(StatusEffect(StatusKind.VULNERABLE, 2, 2)))
        val out = DuelEngine.playCard(state(attacker, defender), "hit", rng)
        // 5 + 3 (empower) - 1 (weaken) + 2 (vulnerable) = 9
        assertEquals(11, out.cpu.hp)
    }

    @Test
    fun shatter_removesBlockThenDeals() {
        val sunder = card("sunder", 4, CardType.ATTACK, Effect.Shatter(4))
        val s = state(duelist("P", false, hand = listOf(sunder)), duelist("C", true).copy(block = 10))
        val out = DuelEngine.playCard(s, "sunder", rng)
        assertEquals(0, out.cpu.block)
        assertEquals(16, out.cpu.hp)
    }

    @Test
    fun riposte_reflectsToAttacker() {
        val defender = duelist("C", true).copy(reflectPending = 2)
        val hit = card("hit", 1, CardType.ATTACK, Effect.Damage(3))
        val out = DuelEngine.playCard(state(duelist("P", false, hand = listOf(hit)), defender), "hit", rng)
        assertEquals(17, out.cpu.hp)
        assertEquals(18, out.player.hp) // took 2 reflected
    }

    @Test
    fun stun_blocksPlaysButNotEndTurn() {
        val hit = card("hit", 1, CardType.ATTACK, Effect.Damage(3))
        val s = state(duelist("P", false, hand = listOf(hit)), duelist("C", true), stunned = true)
        val afterPlay = DuelEngine.playCard(s, "hit", rng)
        assertEquals(20, afterPlay.cpu.hp) // play ignored
    }

    @Test
    fun freeze_skipsWholeTurnAndReturnsControl() {
        val frozenCpu = duelist("C", true).copy(statuses = listOf(StatusEffect(StatusKind.FREEZE, 0, 1)))
        val s = state(duelist("P", false), frozenCpu, activeIsPlayer = true)
        val afterEnd = DuelEngine.endTurn(s, rng) // -> cpu begins, frozen, skips -> player again
        assertTrue(afterEnd.activeIsPlayer)
        assertFalse(afterEnd.cpu.hasStatus(StatusKind.FREEZE))
        assertTrue(afterEnd.pendingSounds.contains(DuelSound.FREEZE))
    }

    @Test
    fun poison_ticksAtTurnStartIgnoringBlockAndCanKill() {
        val poisoned = duelist("C", true, hp = 2).copy(
            block = 5,
            statuses = listOf(StatusEffect(StatusKind.POISON, 3, 3))
        )
        val s = state(duelist("P", false), poisoned, activeIsPlayer = true)
        val out = DuelEngine.endTurn(s, rng) // cpu turn begins -> poison tick
        assertEquals(DuelWinner.PLAYER, out.winner)
    }

    @Test
    fun energyRamps_andCapsAtEight() {
        var s = DuelEngine.startDuel(rng)
        repeat(20) { s = DuelEngine.endTurn(s, rng) }
        assertTrue(s.active.maxEnergyThisTurn <= DuelEngine.MAX_ENERGY)
        assertEquals(DuelEngine.MAX_ENERGY, s.active.maxEnergyThisTurn)
    }

    @Test
    fun blockClearsAtStartOfOwnNextTurn() {
        val guard = card("g", 1, CardType.DEFENSE, Effect.Block(6))
        var s = state(duelist("P", false, energy = 3, hand = listOf(guard)), duelist("C", true))
        s = DuelEngine.playCard(s, "g", rng)
        assertEquals(6, s.player.block)
        s = DuelEngine.endTurn(s, rng)   // cpu turn — player block still up
        assertEquals(6, s.player.block)
        s = DuelEngine.endTurn(s, rng)   // back to player — cleared
        assertEquals(0, s.player.block)
    }

    @Test
    fun cleanse_removesOnlyDebuffs() {
        val cleanse = card("c", 1, CardType.SUPPORT, Effect.Cleanse(EffectTarget.SELF))
        val me = duelist("P", false, hand = listOf(cleanse)).copy(
            statuses = listOf(
                StatusEffect(StatusKind.POISON, 2, 3),
                StatusEffect(StatusKind.EMPOWER, 3, 3)
            )
        )
        val out = DuelEngine.playCard(state(me, duelist("C", true)), "c", rng)
        assertFalse(out.player.hasStatus(StatusKind.POISON))
        assertTrue(out.player.hasStatus(StatusKind.EMPOWER))
    }

    @Test
    fun lethalDamage_endsDuelWithPlayerWin() {
        val finisher = card("f", 1, CardType.ATTACK, Effect.Damage(5))
        val out = DuelEngine.playCard(
            state(duelist("P", false, hand = listOf(finisher)), duelist("C", true, hp = 3)),
            "f", rng
        )
        assertEquals(DuelWinner.PLAYER, out.winner)
        assertEquals(DuelPhase.GAME_OVER, out.phase)
        assertNull(DuelEngine.playCard(out, "f", rng).takeIf { it != out }) // no further plays
    }

    @Test
    fun rootedActive_cannotPlayDefense() {
        val guard = card("g", 1, CardType.DEFENSE, Effect.Block(5))
        val s = state(duelist("P", false, hand = listOf(guard)), duelist("C", true), rooted = true)
        val out = DuelEngine.playCard(s, "g", rng)
        assertEquals(0, out.player.block)
    }
}
