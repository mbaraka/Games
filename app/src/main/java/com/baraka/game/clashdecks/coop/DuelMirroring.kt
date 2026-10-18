package com.baraka.game.clashdecks.coop

import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.DuelVfx
import com.baraka.game.clashdecks.model.DuelWinner
import com.baraka.game.clashdecks.model.VfxSide

/**
 * The host always plays as `state.player`; the guest is `state.cpu` from the host's point of
 * view. Rather than teach every screen/component a "which side am I" concept, the guest just
 * swaps `player`/`cpu` (and everything side-tagged) on every [ClashCoopMsg.DuelSync] it
 * receives before handing it to the exact same `ui/ClashDecksScreen.kt` composables
 * single-player uses — from then on "`.player`/`VfxSide.PLAYER`/`DuelWinner.PLAYER` = me" is
 * true again, same as it always was.
 */
fun DuelState.mirroredForGuest(): DuelState {
    val swappedWinner = when (winner) {
        DuelWinner.PLAYER -> DuelWinner.CPU
        DuelWinner.CPU -> DuelWinner.PLAYER
        else -> winner
    }
    val flippedActive = !activeIsPlayer
    // `phase` mirrors `activeIsPlayer` (PLAYER_TURN/CPU_TURN) and is what the UI actually
    // gates card-playability on, so it has to flip in lockstep or the guest's hand would
    // read as permanently (un)playable.
    val flippedPhase = if (phase == DuelPhase.GAME_OVER) phase
        else if (flippedActive) DuelPhase.PLAYER_TURN else DuelPhase.CPU_TURN
    return copy(
        player = cpu,
        cpu = player,
        phase = flippedPhase,
        activeIsPlayer = flippedActive,
        winner = swappedWinner,
        pendingVfx = pendingVfx.map { it.flipped() }
        // log/pendingSounds/turnNumber/activeStunned/activeRooted are side-agnostic already.
    )
}

private fun DuelVfx.flipped(): DuelVfx {
    val flippedSide = if (side == VfxSide.PLAYER) VfxSide.CPU else VfxSide.PLAYER
    return when (this) {
        is DuelVfx.Hit -> copy(side = flippedSide)
        is DuelVfx.Heal -> copy(side = flippedSide)
        is DuelVfx.BlockGain -> copy(side = flippedSide)
        is DuelVfx.StatusApplied -> copy(side = flippedSide)
        is DuelVfx.BlockShattered -> copy(side = flippedSide)
        is DuelVfx.Frozen -> copy(side = flippedSide)
        is DuelVfx.Cleansed -> copy(side = flippedSide)
        is DuelVfx.ReflectReady -> copy(side = flippedSide)
        is DuelVfx.Warded -> copy(side = flippedSide)
        is DuelVfx.CardPlayed -> copy(side = flippedSide)
    }
}
