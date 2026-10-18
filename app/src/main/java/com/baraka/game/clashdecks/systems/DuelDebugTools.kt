package com.baraka.game.clashdecks.systems

import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.StatusEffect
import com.baraka.game.clashdecks.model.StatusKind
import com.baraka.game.clashdecks.model.merged
import kotlin.random.Random

/**
 * Debug-only shortcuts for testing a duel in minutes instead of playing it out — wired into
 * Settings behind [com.baraka.game.ui.debug.DebugKit]'s unlock (see AGENTS.md's "every game
 * ships a debug tool"). Every function here still goes through real [DuelState]/[Duelist]
 * values (clamped HP, [DuelEngine.checkWinner] for the win cheats, the real merge rules for
 * statuses) so a cheat can bend the normal card-cost rules but can never leave HP negative,
 * energy over the cap, or a "won and lost" duel.
 *
 * Single-player only: [com.baraka.game.clashdecks.ClashCoopViewModel] never wires these in,
 * since mutating the host's duel out from under [DuelEngine]'s own log/broadcast would desync
 * the guest's mirrored copy.
 */
object DuelDebugTools {

    fun healPlayerFull(state: DuelState): DuelState = state.copy(player = state.player.copy(hp = state.player.maxHp))
    fun healCpuFull(state: DuelState): DuelState = state.copy(cpu = state.cpu.copy(hp = state.cpu.maxHp))

    fun setPlayerHp(state: DuelState, hp: Int): DuelState =
        DuelEngine.checkWinner(state.copy(player = state.player.copy(hp = hp.coerceIn(0, state.player.maxHp))))

    fun setCpuHp(state: DuelState, hp: Int): DuelState =
        DuelEngine.checkWinner(state.copy(cpu = state.cpu.copy(hp = hp.coerceIn(0, state.cpu.maxHp))))

    fun grantPlayerEnergy(state: DuelState, amount: Int): DuelState =
        state.copy(player = state.player.copy(energy = (state.player.energy + amount).coerceIn(0, DuelEngine.MAX_ENERGY)))

    fun drawPlayerCard(state: DuelState, rng: Random): DuelState = state.copy(player = state.player.drawCards(1, rng))

    fun cleansePlayer(state: DuelState): DuelState =
        state.copy(player = state.player.copy(statuses = state.player.statuses.filterNot { it.kind.isDebuff }))

    /** Drops a 2-stack, 2-turn copy of [kind] on the CPU — handy for checking a status's icon/chip/log text without drawing into it. */
    fun applyStatusToCpu(state: DuelState, kind: StatusKind): DuelState =
        state.copy(cpu = state.cpu.copy(statuses = state.cpu.statuses.merged(StatusEffect(kind, amount = 2, turnsRemaining = 2))))

    /** Zeroes the CPU's HP and runs it through the real win check, same as any other lethal hit. */
    fun forceWin(state: DuelState): DuelState = DuelEngine.checkWinner(state.copy(cpu = state.cpu.copy(hp = 0)))

    /** Zeroes the player's HP and runs it through the real win check. */
    fun forceLose(state: DuelState): DuelState = DuelEngine.checkWinner(state.copy(player = state.player.copy(hp = 0)))
}
