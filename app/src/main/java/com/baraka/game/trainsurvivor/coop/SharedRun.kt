package com.baraka.game.trainsurvivor.coop

import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.systems.CoopRole
import com.baraka.game.trainsurvivor.systems.CoopSignal
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.abs

/**
 * A SHARED WORLD run: two trains in ONE simulation. [lead] is the host's train and owns the clock, the route and the
 * events; [ally] is the second train. They share one zombie population, one set of effects and one scene, but each
 * train spawns its own zombies in its own lane, so the world holds double the zombies and every gun can reach both lanes.
 *
 * The engine does the heavy lifting (see TrainGame: `lead`, `lane`, `buddy`). This class only
 *   - steps both trains in the right order,
 *   - turns each train's co-op signals into direct calls on the other (give coins, repair, tow, passenger loans,
 *     jam/gate help, rescues), exactly what [CoopSession] does over Bluetooth in a linked run, and
 *   - ends the run for both when either train falls.
 * The second train is driven by [allyBrain] (practice mode) or, later, by a remote player's input.
 *
 * Any map works in a shared world: nothing here knows about routes, bosses or hazards.
 */
class SharedRun(val lead: TrainGame, val ally: TrainGame) {
    /** What the HUD shows about the second train (the same shape as a linked partner). */
    val partner = PartnerState()
    private var brainTimer = 0f
    private var helpDelay = 0f

    init {
        lead.attachAlly(ally)
        for (rig in arrayOf(lead, ally)) {
            rig.coopRole = CoopRole.HOST
            rig.partnerPresent = true
            rig.coopSpawnMult = 1f // each train already spawns its own zombies, so the world is doubled by construction
        }
        lead.onCoop = { signal -> bridge(signal, ally) }
        ally.onCoop = { signal -> bridge(signal, lead) }
    }

    /** One frame: the lead moves the world on, then the second train acts in it. */
    fun update(dt: Float) {
        lead.update(dt)
        ally.update(dt)
        if (lead.over != ally.over) {
            if (lead.over) ally.forceEnd() else lead.forceEnd()
        }
        refreshPartner()
        if (allyBrain && !ally.over) think(dt)
    }

    /** True while the second train is played by the practice bot. */
    var allyBrain = true

    private fun refreshPartner() {
        partner.connected = true
        partner.silence = 0f
        partner.down = ally.over
        partner.hpFrac = (ally.hp / ally.maxHp).coerceIn(0f, 1f)
        partner.distance = ally.distanceMeters
        partner.biome = ally.biome
        partner.kills = ally.kills
        partner.ourPassengerAway = ally.guestTime
    }

    /** What one train tells the other, applied straight away because both live in this process. */
    private fun bridge(signal: CoopSignal, to: TrainGame) {
        when (signal) {
            is CoopSignal.PassengerSent -> to.receivePassenger(signal.survivor, signal.slot, signal.seconds)
            is CoopSignal.CoinsSent -> to.receiveCoins(signal.amount)
            is CoopSignal.RepairSent -> to.receiveRepair(signal.fraction)
            CoopSignal.TowSent -> to.receiveTow()
            is CoopSignal.HelpNeeded -> to.partnerNeedsHelp(signal.kind)
            CoopSignal.HelpGiven -> to.unlockTrain(true)
            CoopSignal.RescueCall -> to.partnerRescueCall()
            is CoopSignal.RescueDone -> to.partnerRescueDone(signal.bonus)
            CoopSignal.TrainDestroyed -> to.forceEnd()
            else -> {} // routes, bosses and Blast need no message: one world, one route, and Blast already hits both lanes
        }
    }

    /** A simple practice bot for the second train: uses its abilities sensibly and helps the player when asked. */
    private fun think(dt: Float) {
        brainTimer -= dt
        if (brainTimer > 0f) return
        brainTimer = 0.4f
        val a = ally
        var near = 0
        for (i in a.zombies.indices) {
            val z = a.zombies[i]
            if (!z.dead && z.lane == 1 && abs(z.x - a.trainLeft - a.trainLength / 2f) < 360f) near++
        }
        if (a.dynamiteReady && near >= 5) a.useDynamite()
        if (a.overdriveReady && near >= 3) a.useOverdrive()
        if (a.fieldRepairReady && a.hp < a.maxHp * 0.5f) a.useFieldRepair()
        // The bot frees the player's train a moment after being asked, and lends a passenger for a rescue.
        if (a.partnerHelp != null) {
            helpDelay += 0.4f
            if (helpDelay >= 2f) { a.helpPartner(); helpDelay = 0f }
        } else {
            helpDelay = 0f
        }
        if (a.partnerRescueTime > 0f) {
            for (seat in 0 until a.slotCount) if (a.sendPassenger(seat)) break
        }
    }

    companion object {
        /** A shared run for practice: the second train is a copy of the player's own, played by the bot. */
        fun practice(save: SaveData): SharedRun {
            val lead = TrainGame(save)
            val ally = TrainGame(save, lead = lead)
            return SharedRun(lead, ally)
        }
    }
}
