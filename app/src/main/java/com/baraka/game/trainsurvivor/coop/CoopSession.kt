package com.baraka.game.trainsurvivor.coop

import com.baraka.game.coop.CoopLink
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.CoopRole
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.SHARED_MAX
import com.baraka.game.trainsurvivor.systems.CoopSignal
import com.baraka.game.trainsurvivor.systems.TrainGame

/** What we last heard about the partner's train (read by the HUD). */
class PartnerState {
    var connected = true
    var down = false
    var hpFrac = 1f
    var distance = 0
    var biome: Biome = Biome.PLAINS
    var kills = 0
    /** Seconds our lent passenger is still on their train, and seconds their passenger is still on ours. */
    var ourPassengerAway = 0f
    var silence = 0f
    val away get() = silence > 4f
}

/** Which phone does what: the host runs shared timers; the engine emits signals and this class carries them over a [CoopLink]. */
class CoopSession(val game: TrainGame, val link: CoopLink<CoopMsg>, val role: CoopRole) {
    val partner = PartnerState()
    private var sendTimer = 0f
    private var ghostTimer = 0f
    private var sharedTimer = 0f
    private val snapIds = IntArray(SHARED_MAX)
    private val snapTypes = IntArray(SHARED_MAX)
    private val snapXs = IntArray(SHARED_MAX)
    private val snapYs = IntArray(SHARED_MAX)
    private val snapHp = IntArray(SHARED_MAX)
    private val snapFlags = IntArray(SHARED_MAX)

    init {
        game.coopRole = role
        game.coopSpawnMult = 2f // two trains, double the zombies
        game.partnerPresent = true
        game.onCoop = { signal -> onSignal(signal) }
        link.send(CoopMsg.Hello(COOP_PROTOCOL))
    }

    fun update(dt: Float) {
        link.tick(dt)
        var m = link.poll()
        while (m != null) {
            partner.silence = 0f
            handle(m)
            m = link.poll()
        }
        partner.silence += dt
        if (partner.connected && !link.connected) {
            partner.connected = false
            game.coopSpawnMult = 1f // alone again
            game.partnerPresent = false
            game.coopRole = CoopRole.HOST // nobody left to wait for
        }
        ghostTimer += dt
        if (ghostTimer >= 0.125f && !game.over) {
            ghostTimer = 0f
            val n = game.slotCount
            val hp = IntArray(n) { (game.carFraction(it) * 100f).toInt() }
            link.send(CoopMsg.Ghost(game.shape.ordinal, game.livery.ordinal, hp, game.takeShotMask()))
        }
        if (game.sharedActive && !game.over) {
            sharedTimer += dt
            if (sharedTimer >= 0.1f) {
                sharedTimer = 0f
                val n = game.fillSnapshot(snapIds, snapTypes, snapXs, snapYs, snapHp, snapFlags)
                link.send(CoopMsg.Zombies(snapIds.copyOf(n), snapTypes.copyOf(n), snapXs.copyOf(n), snapYs.copyOf(n), snapHp.copyOf(n), snapFlags.copyOf(n)))
                if (game.shared.hasHits) {
                    val (ids, damage) = game.shared.takeHits()
                    link.send(CoopMsg.Hits(ids, damage))
                }
            }
        }
        sendTimer += dt
        if (sendTimer >= 0.25f) {
            sendTimer = 0f
            if (!game.over) {
                link.send(
                    CoopMsg.State(
                        (game.hp / game.maxHp).coerceIn(0f, 1f), game.distanceMeters, game.biome.ordinal, game.kills,
                        game.guestTime, game.seatAwayTime.maxOrNull() ?: 0f
                    )
                )
            }
            val fraction = game.takeBossDamage()
            if (fraction > 0f) link.send(CoopMsg.BossDamage(fraction))
        }
    }

    fun close() {
        game.onCoop = null
        link.close()
    }

    private fun onSignal(s: CoopSignal) {
        when (s) {
            is CoopSignal.JunctionOpened -> link.send(CoopMsg.Junction(s.a.ordinal, s.b.ordinal))
            is CoopSignal.RouteRequested -> link.send(CoopMsg.RouteReq(s.choice.ordinal))
            is CoopSignal.RouteChosen -> link.send(CoopMsg.RouteChosen(s.choice.ordinal))
            is CoopSignal.BossSpawned -> link.send(CoopMsg.Boss(s.type.ordinal))
            is CoopSignal.PassengerSent -> link.send(CoopMsg.Passenger(s.survivor, s.slot, s.seconds))
            CoopSignal.Blast -> link.send(CoopMsg.Blast)
            is CoopSignal.CoinsSent -> link.send(CoopMsg.Coins(s.amount))
            is CoopSignal.HelpNeeded -> link.send(CoopMsg.HelpNeeded(s.kind))
            CoopSignal.HelpGiven -> link.send(CoopMsg.HelpGiven)
            CoopSignal.TowSent -> link.send(CoopMsg.Tow)
            CoopSignal.RescueCall -> link.send(CoopMsg.RescueCall)
            is CoopSignal.RescueDone -> link.send(CoopMsg.RescueDone(s.bonus))
            is CoopSignal.RepairSent -> link.send(CoopMsg.Repair(s.fraction))
            CoopSignal.TrainDestroyed -> link.send(CoopMsg.Down)
        }
    }

    private fun handle(m: CoopMsg) {
        when (m) {
            is CoopMsg.State -> {
                partner.hpFrac = m.hpFrac
                partner.distance = m.distance
                partner.biome = Biome.entries.getOrElse(m.biome) { Biome.PLAINS }
                partner.kills = m.kills
                partner.ourPassengerAway = m.guestLeft
            }
            is CoopMsg.Junction -> {
                val a = Biome.entries.getOrNull(m.a)
                val b = Biome.entries.getOrNull(m.b)
                if (a != null && b != null && role == CoopRole.GUEST) game.openJunctionWith(a, b)
            }
            is CoopMsg.RouteReq -> {
                val c = Biome.entries.getOrNull(m.choice)
                if (c != null && role == CoopRole.HOST) game.chooseRoute(c)
            }
            is CoopMsg.RouteChosen -> {
                val c = Biome.entries.getOrNull(m.choice)
                if (c != null && role == CoopRole.GUEST) game.applyRemoteRoute(c)
            }
            is CoopMsg.Boss -> {
                val t = ZombieType.entries.getOrNull(m.type)
                if (t != null && role == CoopRole.GUEST) game.spawnBoss(t)
            }
            is CoopMsg.BossDamage -> game.applyBossDamage(m.fraction)
            is CoopMsg.Passenger -> game.receivePassenger(m.survivor, m.slot, m.seconds)
            CoopMsg.Blast -> game.remoteBlast()
            is CoopMsg.Repair -> game.receiveRepair(m.fraction)
            is CoopMsg.Coins -> game.receiveCoins(m.amount)
            is CoopMsg.HelpNeeded -> game.partnerNeedsHelp(m.kind)
            CoopMsg.HelpGiven -> game.unlockTrain(true)
            CoopMsg.Tow -> game.receiveTow()
            is CoopMsg.Zombies -> if (game.sharedActive) game.shared.apply(m.ids, m.types, m.xs, m.ys, m.hp, m.flags, game.farCenterX, RAIL_Y)
            is CoopMsg.Hits -> game.applyRemoteHits(m.ids, m.damage)
            is CoopMsg.Ghost -> game.ghost.apply(m.shape, m.livery, m.hp, m.shotMask)
            CoopMsg.RescueCall -> game.partnerRescueCall()
            is CoopMsg.RescueDone -> game.partnerRescueDone(m.bonus)
            CoopMsg.Down -> {
                partner.down = true
                game.forceEnd() // two trains, one fate: the run ends for both
            }
            CoopMsg.Bye -> partner.connected = false
            is CoopMsg.Hello -> {}
        }
    }
}
