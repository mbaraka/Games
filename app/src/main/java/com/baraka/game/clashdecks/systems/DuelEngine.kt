package com.baraka.game.clashdecks.systems

import com.baraka.game.clashdecks.model.Card
import com.baraka.game.clashdecks.model.CardLibrary
import com.baraka.game.clashdecks.model.CardType
import com.baraka.game.clashdecks.model.DuelLogEntry
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelSound
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.DuelVfx
import com.baraka.game.clashdecks.model.DuelWinner
import com.baraka.game.clashdecks.model.Duelist
import com.baraka.game.clashdecks.model.Effect
import com.baraka.game.clashdecks.model.EffectTarget
import com.baraka.game.clashdecks.model.LogKind
import com.baraka.game.clashdecks.model.StatusEffect
import com.baraka.game.clashdecks.model.StatusKind
import com.baraka.game.clashdecks.model.merged
import com.baraka.game.clashdecks.model.tickedDown
import kotlin.random.Random

/**
 * The pure, testable rules of a duel. Every function takes a [DuelState] and returns a new
 * one; nothing here touches Android, coroutines, or the clock. The ViewModel drives the
 * flow: [startDuel] once, then per turn a series of [playCard] calls followed by [endTurn].
 *
 * Turn structure (`beginTurn`, run for whoever is about to act):
 *  1. Freeze  → consume it, skip the whole turn.
 *  2. Poison  → lose HP (ignores block); can be lethal.
 *  3. Regen   → heal HP.
 *  4. Snapshot Stun/Root for this turn, then tick every status timer down.
 *  5. Clear own block from last turn (it protected through the opponent's turn).
 *  6. Refill energy from the round-based ramp.
 *  7. Draw one card.
 */
object DuelEngine {

    const val START_HP = 20
    const val MAX_ENERGY = 8
    const val HAND_CAP = 8

    private fun energyForRound(turnNumber: Int): Int = (2 + turnNumber).coerceAtMost(MAX_ENERGY)

    // --- Log grammar helpers: names are always "You" / "Opponent", so conjugate for the player. ---
    /** "You play Strike" / "Opponent plays Strike" — regular +s on the CPU side. */
    private fun Duelist.does(verb: String): String = if (isCpu) "$name ${verb}s" else "$name $verb"
    /** "You are" / "Opponent is". */
    private fun Duelist.beVerb(): String = if (isCpu) "$name is" else "$name are"
    /** "Your" / "Opponent's". */
    private fun Duelist.possessive(): String = if (isCpu) "$name's" else "Your"

    /**
     * [playerName]/[cpuName] default to the single-player "You"/"Opponent" labels, which also
     * drive the log's second-/third-person grammar (see the `does`/`beVerb`/`possessive`
     * helpers above, keyed off `isCpu`, not the name). Two-phone co-op
     * (clashdecks/coop/ClashCoopSession.kt) passes neutral names instead since "You" would be
     * wrong on the other phone — the grammar reads slightly stiffer there ("Player 2 play
     * Strike") but needs no further engine change.
     */
    fun startDuel(rng: Random, playerName: String = "You", cpuName: String = "Opponent"): DuelState {
        val player = Duelist.fresh(playerName, isCpu = false, startHp = START_HP, deck = CardLibrary.buildDeck(rng))
            .drawCards(3, rng)
        val cpu = Duelist.fresh(cpuName, isCpu = true, startHp = START_HP, deck = CardLibrary.buildDeck(rng))
            .drawCards(3, rng)

        val firstEnergy = energyForRound(1)
        return DuelState(
            player = player.copy(energy = firstEnergy, maxEnergyThisTurn = firstEnergy),
            cpu = cpu,
            phase = DuelPhase.PLAYER_TURN,
            activeIsPlayer = true,
            turnNumber = 1,
            activeStunned = false,
            activeRooted = false,
            log = listOf(DuelLogEntry("Duel start — $playerName goes first.", LogKind.SYSTEM)),
            pendingSounds = listOf(DuelSound.TURN_START),
            pendingVfx = emptyList(),
            winner = null
        )
    }

    // ---------------------------------------------------------------- playing a card

    fun playCard(state: DuelState, cardId: String, rng: Random): DuelState {
        if (state.winner != null || state.phase == DuelPhase.GAME_OVER) return state
        if (state.activeStunned) return state
        val me = state.active
        val card = me.hand.firstOrNull { it.id == cardId } ?: return state
        if (me.energy < card.cost) return state
        if (state.activeRooted && card.type == CardType.DEFENSE) return state

        val spent = me.copy(
            energy = me.energy - card.cost,
            hand = me.hand.toMutableList().also { it.remove(card) },
            discard = me.discard + card
        )
        var s = state.withActive(spent)
            .log("${me.does("play")} ${card.name}.", LogKind.NEUTRAL)
            .sound(card.type.playSound())
            .vfx(DuelVfx.CardPlayed(state.activeSide, card.type))

        for (effect in card.effects) {
            s = applyEffect(s, effect, rng)
            if (s.winner != null) break
        }
        return checkWinner(s)
    }

    private fun CardType.playSound(): DuelSound = when (this) {
        CardType.ATTACK -> DuelSound.CARD_ATTACK
        CardType.DEFENSE -> DuelSound.CARD_DEFENSE
        CardType.SUPPORT -> DuelSound.CARD_SUPPORT
    }

    private fun applyEffect(s: DuelState, effect: Effect, rng: Random): DuelState {
        val me = s.active
        return when (effect) {
            is Effect.Damage -> dealDamage(s, effect.amount, fromActive = true)

            is Effect.Block ->
                s.withActive(me.copy(block = me.block + effect.amount))
                    .log("${me.does("gain")} ${effect.amount} Block.", LogKind.NEUTRAL)
                    .vfx(DuelVfx.BlockGain(s.activeSide, effect.amount))

            is Effect.Heal -> {
                val healed = (me.hp + effect.amount).coerceAtMost(me.maxHp)
                s.withActive(me.copy(hp = healed))
                    .log("${me.does("heal")} ${healed - me.hp}.", LogKind.HEAL)
                    .sound(DuelSound.HEAL)
                    .vfx(DuelVfx.Heal(s.activeSide, healed - me.hp))
            }

            is Effect.DrawCards ->
                s.withActive(me.drawCards(effect.count, rng))
                    .log("${me.does("draw")} ${effect.count}.", LogKind.NEUTRAL)
                    .sound(DuelSound.DRAW)

            is Effect.GainEnergy ->
                s.withActive(me.copy(energy = (me.energy + effect.amount).coerceAtMost(MAX_ENERGY)))
                    .log("${me.does("gain")} ${effect.amount} energy.", LogKind.NEUTRAL)

            is Effect.ApplyStatus -> {
                val toSelf = effect.target == EffectTarget.SELF
                val target = if (toSelf) me else s.inactive
                val updated = target.copy(
                    statuses = target.statuses.merged(
                        StatusEffect(effect.kind, effect.amount, effect.turns)
                    )
                )
                val next = if (toSelf) s.withActive(updated) else s.withInactive(updated)
                val verb = if (effect.kind.isDebuff) "suffer" else "gain"
                val targetSide = if (toSelf) s.activeSide else s.inactiveSide
                next.log("${target.does(verb)} ${effect.kind.displayName} (${effect.turns}).", LogKind.STATUS)
                    .sound(if (effect.kind == StatusKind.STUN) DuelSound.STUN
                           else if (effect.kind == StatusKind.FREEZE) DuelSound.FREEZE
                           else if (effect.kind.isDebuff) DuelSound.DEBUFF
                           else DuelSound.BUFF)
                    .vfx(DuelVfx.StatusApplied(targetSide, effect.kind))
            }

            is Effect.Cleanse -> {
                val toSelf = effect.target == EffectTarget.SELF
                val target = if (toSelf) me else s.inactive
                val cleared = target.copy(statuses = target.statuses.filterNot { it.kind.isDebuff })
                val next = if (toSelf) s.withActive(cleared) else s.withInactive(cleared)
                val targetSide = if (toSelf) s.activeSide else s.inactiveSide
                next.log("${target.beVerb()} cleansed of debuffs.", LogKind.STATUS)
                    .sound(DuelSound.BUFF)
                    .vfx(DuelVfx.Cleansed(targetSide))
            }

            is Effect.Shatter -> {
                val opp = s.inactive
                val broken = opp.copy(block = 0)
                var s2 = s.withInactive(broken).sound(DuelSound.SHATTER)
                if (opp.block > 0) s2 = s2
                    .log("${opp.possessive()} ${opp.block} Block is shattered.", LogKind.DAMAGE)
                    .vfx(DuelVfx.BlockShattered(s.inactiveSide, opp.block))
                dealDamage(s2, effect.bonusDamage, fromActive = true)
            }

            is Effect.ReflectNextHit ->
                s.withActive(me.copy(reflectPending = me.reflectPending + effect.amount))
                    .log("${me.does("ready")} a riposte.", LogKind.NEUTRAL)
                    .vfx(DuelVfx.ReflectReady(s.activeSide))
        }
    }

    /**
     * Applies [raw] damage from one side to the other, folding in the attacker's
     * Weaken/Empower, the defender's Vulnerable, then Block, then any pending reflect.
     */
    private fun dealDamage(s: DuelState, raw: Int, fromActive: Boolean): DuelState {
        val attacker = if (fromActive) s.active else s.inactive
        val defender = if (fromActive) s.inactive else s.active

        var dmg = raw
        dmg -= attacker.amountOf(StatusKind.WEAKEN)
        dmg += attacker.amountOf(StatusKind.EMPOWER)
        dmg += defender.amountOf(StatusKind.VULNERABLE)
        dmg = dmg.coerceAtLeast(0)

        val absorbed = minOf(defender.block, dmg)
        val toHp = dmg - absorbed
        var newDefender = defender.copy(block = defender.block - absorbed, hp = defender.hp - toHp)

        var newAttacker = attacker
        var reflected = 0
        if (toHp > 0 && newDefender.reflectPending > 0) {
            reflected = newDefender.reflectPending
            newDefender = newDefender.copy(reflectPending = 0)
            newAttacker = newAttacker.copy(hp = newAttacker.hp - reflected)
        }

        var s2 = putSides(s, fromActive, newAttacker, newDefender)
        val defenderSide = if (fromActive) s.inactiveSide else s.activeSide
        val attackerSide = if (fromActive) s.activeSide else s.inactiveSide
        s2 = s2.log(
            "${attacker.does("hit")} ${defender.name} for $toHp" +
                (if (absorbed > 0) " ($absorbed blocked)" else "") + ".",
            LogKind.DAMAGE
        ).vfx(DuelVfx.Hit(defenderSide, toHp, absorbed))
        if (reflected > 0) s2 = s2
            .log("${defender.possessive()} riposte deals $reflected.", LogKind.DAMAGE)
            .vfx(DuelVfx.Hit(attackerSide, reflected, 0))
        return s2
    }

    private fun putSides(s: DuelState, fromActive: Boolean, attacker: Duelist, defender: Duelist): DuelState =
        if (fromActive) s.withActive(attacker).withInactive(defender)
        else s.withActive(defender).withInactive(attacker)

    // ---------------------------------------------------------------- turn transitions

    fun endTurn(state: DuelState, rng: Random): DuelState {
        if (state.winner != null) return state
        val flipped = state.copy(activeIsPlayer = !state.activeIsPlayer)
        val nextRound = if (flipped.activeIsPlayer) flipped.turnNumber + 1 else flipped.turnNumber
        return beginTurn(flipped.copy(turnNumber = nextRound), rng)
    }

    private fun beginTurn(state: DuelState, rng: Random): DuelState {
        var s = state.sound(DuelSound.TURN_START)
        var me = s.active

        // 1. Freeze: lose the whole turn.
        if (me.hasStatus(StatusKind.FREEZE)) {
            me = me.copy(statuses = me.statuses.filterNot { it.kind == StatusKind.FREEZE })
            val frozenMsg = if (me.isCpu) "${me.name} is frozen and skips the turn!"
                            else "${me.name} are frozen and skip the turn!"
            s = s.withActive(me).log(frozenMsg, LogKind.STATUS).sound(DuelSound.FREEZE)
                .vfx(DuelVfx.Frozen(s.activeSide))
            return endTurn(s, rng)
        }

        // 2. Poison tick (ignores block).
        val poison = me.amountOf(StatusKind.POISON)
        if (poison > 0) {
            me = me.copy(hp = me.hp - poison)
            s = s.withActive(me)
                .log("${me.does("take")} $poison poison damage.", LogKind.DAMAGE)
                .sound(DuelSound.POISON_TICK)
                .vfx(DuelVfx.Hit(s.activeSide, poison, 0))
            val dead = checkWinner(s)
            if (dead.winner != null) return dead
        }

        // 3. Regen tick.
        val regen = me.amountOf(StatusKind.REGEN)
        if (regen > 0) {
            val before = me.hp
            me = me.copy(hp = (me.hp + regen).coerceAtMost(me.maxHp))
            s = s.withActive(me).log("${me.does("regenerate")} ${me.hp - before}.", LogKind.HEAL)
        }

        // 4. Snapshot gating statuses for this turn, then age everything.
        val stunned = me.hasStatus(StatusKind.STUN)
        val rooted = me.hasStatus(StatusKind.ROOT)
        me = me.copy(statuses = me.statuses.tickedDown())

        // 5. Clear own block from last turn.
        me = me.copy(block = 0)

        // 6. Refill energy from the ramp.
        val energy = energyForRound(s.turnNumber)
        me = me.copy(energy = energy, maxEnergyThisTurn = energy)

        // 7. Draw.
        me = me.drawCards(1, rng)
        if (me.hand.size > HAND_CAP) {
            val overflow = me.hand.size - HAND_CAP
            me = me.copy(hand = me.hand.drop(overflow), discard = me.discard + me.hand.take(overflow))
        }

        s = s.withActive(me).copy(
            phase = if (s.activeIsPlayer) DuelPhase.PLAYER_TURN else DuelPhase.CPU_TURN,
            activeStunned = stunned,
            activeRooted = rooted
        )
        if (stunned) s = s.log("${me.beVerb()} stunned — no cards this turn.", LogKind.STATUS).sound(DuelSound.STUN)
        if (rooted && !stunned) s = s.log("${me.beVerb()} rooted — no Defense this turn.", LogKind.STATUS)
        return s
    }

    // ---------------------------------------------------------------- win check

    fun checkWinner(state: DuelState): DuelState {
        if (state.winner != null) return state
        val playerDead = state.player.hp <= 0
        val cpuDead = state.cpu.hp <= 0
        if (!playerDead && !cpuDead) return state

        val winner = if (cpuDead && !playerDead) DuelWinner.PLAYER
        else if (playerDead && !cpuDead) DuelWinner.CPU
        else DuelWinner.CPU // simultaneous KO: defender's side loses — the active attacker survives ties
        val playerWon = winner == DuelWinner.PLAYER
        return state.copy(
            phase = DuelPhase.GAME_OVER,
            winner = winner,
            log = state.log + DuelLogEntry(
                if (playerWon) "Opponent is defeated. You win!" else "You have been defeated.",
                LogKind.SYSTEM
            ),
            pendingSounds = state.pendingSounds + (if (playerWon) DuelSound.WIN else DuelSound.LOSE)
        )
    }

    /** Convenience for tests/AI: the card object for an id in the active hand. */
    fun activeCard(state: DuelState, cardId: String): Card? =
        state.active.hand.firstOrNull { it.id == cardId }
}
