package com.baraka.game.clashdecks.model

enum class DuelPhase { PLAYER_TURN, CPU_TURN, GAME_OVER }

/** Winner marker on [DuelState.winner]. */
object DuelWinner {
    const val PLAYER = "player"
    const val CPU = "cpu"
}

enum class LogKind { NEUTRAL, DAMAGE, HEAL, STATUS, SYSTEM }

data class DuelLogEntry(val text: String, val kind: LogKind)

/** A procedurally-synthesized sound cue (see `data/DuelSoundEngine.kt`). */
enum class DuelSound {
    CARD_ATTACK, CARD_DEFENSE, CARD_SUPPORT,
    STUN, FREEZE, BUFF, DEBUFF, POISON_TICK, HEAL, SHATTER, DRAW,
    TURN_START, WIN, LOSE
}

/**
 * The entire duel in one immutable value. [DuelEngine] only ever returns a new one.
 *
 * [activeIsPlayer] tracks whose turn it is independently of [phase] (phase also encodes
 * GAME_OVER). [turnNumber] is the round counter — it increments each time control returns
 * to the player, and drives the energy ramp. [activeStunned]/[activeRooted] are computed
 * once in `beginTurn` and read by `playCard`. [pendingSounds] is drained by the ViewModel
 * after every transition and then cleared.
 */
data class DuelState(
    val player: Duelist,
    val cpu: Duelist,
    val phase: DuelPhase,
    val activeIsPlayer: Boolean,
    val turnNumber: Int,
    val activeStunned: Boolean,
    val activeRooted: Boolean,
    val log: List<DuelLogEntry>,
    val pendingSounds: List<DuelSound>,
    val pendingVfx: List<DuelVfx>,
    val winner: String?
) {
    val active: Duelist get() = if (activeIsPlayer) player else cpu
    val inactive: Duelist get() = if (activeIsPlayer) cpu else player

    fun withActive(d: Duelist): DuelState =
        if (activeIsPlayer) copy(player = d) else copy(cpu = d)

    fun withInactive(d: Duelist): DuelState =
        if (activeIsPlayer) copy(cpu = d) else copy(player = d)

    fun log(text: String, kind: LogKind = LogKind.NEUTRAL): DuelState =
        copy(log = log + DuelLogEntry(text, kind))

    fun sound(cue: DuelSound): DuelState = copy(pendingSounds = pendingSounds + cue)

    fun vfx(effect: DuelVfx): DuelState = copy(pendingVfx = pendingVfx + effect)

    val activeSide: VfxSide get() = if (activeIsPlayer) VfxSide.PLAYER else VfxSide.CPU
    val inactiveSide: VfxSide get() = if (activeIsPlayer) VfxSide.CPU else VfxSide.PLAYER
}
