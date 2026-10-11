package com.baraka.game.trainsurvivor.coop

import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.CoopRole
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
class CoopSession(val game: TrainGame, val link: CoopLink, val role: CoopRole) {
    val partner = PartnerState()
    private var sendTimer = 0f

    init {
        game.coopRole = role
        game.coopSpawnMult = 2f // two trains, double the zombies
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
            game.coopRole = CoopRole.HOST // nobody left to wait for
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
            CoopMsg.Down -> partner.down = true
            CoopMsg.Bye -> partner.connected = false
            is CoopMsg.Hello -> {}
        }
    }
}
