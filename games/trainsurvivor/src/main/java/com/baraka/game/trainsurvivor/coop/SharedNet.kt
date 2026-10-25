package com.baraka.game.trainsurvivor.coop

import com.baraka.game.coop.CoopLink
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.TrainUpgrade
import com.baraka.game.trainsurvivor.systems.CoopRole
import com.baraka.game.trainsurvivor.systems.GuestAction
import com.baraka.game.trainsurvivor.systems.RemoteControl
import com.baraka.game.trainsurvivor.systems.TrainGame
import com.baraka.game.trainsurvivor.systems.Zombie
import com.baraka.game.trainsurvivor.systems.readRig
import com.baraka.game.trainsurvivor.systems.readWorld
import com.baraka.game.trainsurvivor.systems.readZombies
import com.baraka.game.trainsurvivor.systems.replayFx
import com.baraka.game.trainsurvivor.systems.visualTick
import com.baraka.game.trainsurvivor.systems.writeFx
import com.baraka.game.trainsurvivor.systems.writeRig
import com.baraka.game.trainsurvivor.systems.writeWorld
import com.baraka.game.trainsurvivor.systems.writeZombies
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/*
 * SHARED WORLD OVER BLUETOOTH.
 *
 *   host phone:  SharedRun (the real simulation of both trains)  --Snap-->  guest phone: SharedMirror (two trains that only draw)
 *                applies the guest's Act messages to the 2nd train  <--Act--
 *
 * Before the run both phones swap their saves (SaveBlob) so each knows the other's train. The guest's buttons call the same
 * TrainGame methods as ever; on the mirror they forward through [RemoteControl] instead of acting. See systems/RigSync.kt for
 * what a snapshot holds and how to extend it.
 */

/** Just the parts of a save that decide how a train looks and fights, so a partner can build a copy of it. */
object SaveCodec {
    fun encode(save: SaveData): ByteArray {
        val bytes = ByteArrayOutputStream(160)
        val o = DataOutputStream(bytes)
        o.writeByte(save.unlockedSlots)
        o.writeByte(save.slots.size)
        for (s in save.slots) { o.writeInt(s.owned); o.writeInt(s.selected); o.writeByte(s.power); o.writeByte(s.armor) }
        for (u in TrainUpgrade.entries) o.writeByte(save.level(u))
        o.writeByte(save.shape)
        o.writeByte(save.livery)
        o.writeByte(save.dangerLevel)
        o.writeInt(save.prestige)
        o.writeByte(save.dangerBest.size)
        for (d in save.dangerBest) o.writeInt(d)
        return bytes.toByteArray()
    }

    fun decode(data: ByteArray): SaveData {
        val i = DataInputStream(ByteArrayInputStream(data))
        val unlocked = i.readUnsignedByte()
        val slots = List(i.readUnsignedByte()) { CrewSlot(i.readInt(), i.readInt(), i.readUnsignedByte(), i.readUnsignedByte()) }
        val upgrades = HashMap<TrainUpgrade, Int>()
        for (u in TrainUpgrade.entries) upgrades[u] = i.readUnsignedByte()
        val shape = i.readUnsignedByte()
        val livery = i.readUnsignedByte()
        val danger = i.readUnsignedByte()
        val prestige = i.readInt()
        val best = List(i.readUnsignedByte()) { i.readInt() }
        return SaveData(
            prestige = prestige, upgrades = upgrades, slots = slots, unlockedSlots = unlocked,
            dangerLevel = danger, dangerBest = best, livery = livery, liveriesOwned = (1 shl livery) or 1, shape = shape, shapesOwned = (1 shl shape) or 1
        )
    }
}

/** Swaps saves with the partner before a shared run. Poll [poll] until it returns true (or [failed] is set). */
class SharedHandshake(private val link: CoopLink<CoopMsg>, mine: SaveData) {
    var failed: String? = null
    var partnerSave: SaveData? = null
        private set

    init {
        link.send(CoopMsg.Hello(COOP_PROTOCOL))
        link.send(CoopMsg.SaveBlob(SaveCodec.encode(mine)))
    }

    fun poll(): Boolean {
        while (partnerSave == null && failed == null) {
            val m = link.poll() ?: break
            when (m) {
                is CoopMsg.Hello -> if (m.version != COOP_PROTOCOL) failed = "Your partner has a different version of the game: update both phones."
                is CoopMsg.SaveBlob -> partnerSave = runCatching { SaveCodec.decode(m.data) }.getOrElse { failed = "Could not read your partner's train."; null }
                CoopMsg.Bye -> failed = "Your partner left."
                else -> {}
            }
        }
        if (!link.connected && partnerSave == null) failed = "The connection was lost."
        return partnerSave != null
    }
}

/** The two trains of a shared world as the GUEST sees them: never simulated, only overwritten by snapshots and drawn. */
class SharedMirror(hostSave: SaveData, guestSave: SaveData) {
    val lead = TrainGame(hostSave, spawnEnabled = false)
    val ally = TrainGame(guestSave, spawnEnabled = false, lead = lead)
    private val index = HashMap<Int, Zombie>()
    private var stamp = 0
    private var lastSeq = -1
    /** Seconds since the last snapshot arrived. */
    var silence = 0f
    var received = false
        private set

    init {
        lead.attachAlly(ally)
        for (rig in arrayOf(lead, ally)) {
            rig.coopRole = CoopRole.HOST
            rig.partnerPresent = true
        }
    }

    fun apply(data: ByteArray) {
        val input = DataInputStream(ByteArrayInputStream(data))
        val seq = input.readInt()
        if (seq <= lastSeq) return
        lastSeq = seq
        stamp++
        lead.readWorld(input)
        lead.readZombies(input, index, stamp)
        lead.readRig(input, index)
        ally.readRig(input, index)
        lead.replayFx(input)
        silence = 0f
        received = true
    }

    fun tick(dt: Float) {
        silence += dt
        lead.visualTick(dt)
        ally.visualTick(dt)
    }
}

/** What the run screen needs from a shared-world run, whichever phone simulates it. */
interface SharedDriver {
    /** The player's own train (the one the buttons and HUD describe). */
    val me: TrainGame
    /** The train on the lower track and the one on the upper track, as drawn. */
    val front: TrainGame
    val back: TrainGame
    /** How the other train is doing, for the partner strip. */
    val partner: PartnerState
    fun setWorldWidth(width: Float)
    fun update(dt: Float)
    fun close()
}

/** Practice: both trains simulated here, the second played by a bot. */
class PracticeDriver(val run: SharedRun) : SharedDriver {
    override val me get() = run.lead
    override val front get() = run.lead
    override val back get() = run.ally
    override val partner get() = run.partner
    override fun setWorldWidth(width: Float) { run.lead.worldW = width }
    override fun update(dt: Float) = run.update(dt)
    override fun close() {}
}

/** The host: simulates both trains and streams the world to the guest. */
class HostDriver(val run: SharedRun, link: CoopLink<CoopMsg>) : SharedDriver {
    private val session = SharedHostSession(run, link)
    override val me get() = run.lead
    override val front get() = run.lead
    override val back get() = run.ally
    override val partner get() = run.partner
    override fun setWorldWidth(width: Float) { run.lead.worldW = width }
    override fun update(dt: Float) {
        session.pump()
        run.update(dt)
        session.afterUpdate(dt)
    }
    override fun close() = session.close()
}

/** The guest: draws the host's world and sends its button presses. */
class GuestDriver(link: CoopLink<CoopMsg>, hostSave: SaveData, guestSave: SaveData) : SharedDriver {
    internal val session = SharedGuestSession(link, hostSave, guestSave)
    override val me get() = session.mirror.ally
    override val front get() = session.mirror.lead
    override val back get() = session.mirror.ally
    override val partner get() = session.partner
    override fun setWorldWidth(width: Float) {} // the host's world width rules; the guest just scales it to fit
    override fun update(dt: Float) = session.update(dt)
    override fun close() = session.close()
}

/** Both ends in one process, joined by an in-memory link: you play the guest. It exercises the whole network path without a second phone. */
class LoopbackDriver(hostSave: SaveData, guestSave: SaveData) : SharedDriver {
    private val pair = LoopbackPair()
    private val host = HostDriver(SharedRun.network(hostSave, guestSave), pair.a)
    private val guest = GuestDriver(pair.b, hostSave, guestSave)
    override val me get() = guest.me
    override val front get() = guest.front
    override val back get() = guest.back
    override val partner get() = guest.partner
    override fun setWorldWidth(width: Float) = host.setWorldWidth(width)
    override fun update(dt: Float) {
        host.update(dt)
        guest.update(dt)
    }
    override fun close() {
        guest.close()
        host.close()
    }
}

/** The host's end of a link: applies the guest's actions to the second train and sends snapshots about 15 times a second. */
class SharedHostSession(private val run: SharedRun, private val link: CoopLink<CoopMsg>) {
    private var seq = 0
    private var timer = 0f
    private val buffer = ByteArrayOutputStream(4096)

    init {
        run.lead.fx.recording = true
        run.allyBrain = false
    }

    /** Applies what the guest asked for since the last frame. */
    fun pump() {
        var m = link.poll()
        while (m != null) {
            when (m) {
                is CoopMsg.Act -> applyAction(m)
                CoopMsg.Bye -> partnerLeft()
                else -> {}
            }
            m = link.poll()
        }
        if (!link.connected) partnerLeft()
    }

    private fun partnerLeft() {
        if (run.partnerLost) return
        run.partnerLost = true
        run.allyBrain = true // a bot takes the second train over so the run can continue
    }

    private fun applyAction(m: CoopMsg.Act) {
        val ally = run.ally
        when (m.code) {
            GuestAction.BLAST -> ally.useDynamite()
            GuestAction.BOOST -> ally.useOverdrive()
            GuestAction.FIELD_REPAIR -> ally.useFieldRepair()
            GuestAction.CYCLE_WEAPON -> ally.cycleWeapon(m.arg)
            GuestAction.GIVE_COINS -> ally.sendCoins()
            GuestAction.SEND_REPAIR -> ally.sendRepair()
            GuestAction.SEND_TOW -> ally.sendTow()
            GuestAction.HELP_PARTNER -> ally.helpPartner()
            GuestAction.SEND_PASSENGER -> ally.sendPassenger(m.arg)
            GuestAction.CHOOSE_ROUTE -> Biome.entries.getOrNull(m.arg)?.let { run.lead.chooseRoute(it) }
            GuestAction.TAP -> ally.tapFromGuest(m.x, m.y)
        }
    }

    fun afterUpdate(dt: Float) {
        timer += dt
        if (timer < 1f / 15f) return
        timer = 0f
        if (!link.connected || link.backlog > 2) return
        buffer.reset()
        val out = DataOutputStream(buffer)
        out.writeInt(++seq)
        run.lead.writeWorld(out)
        run.lead.writeZombies(out)
        run.lead.writeRig(out)
        run.ally.writeRig(out)
        run.lead.writeFx(out)
        link.send(CoopMsg.Snap(buffer.toByteArray()))
    }

    fun close() {
        run.lead.fx.recording = false
        link.close()
    }
}

/** The guest's end of a link: feeds snapshots to the mirror and sends button presses to the host. */
class SharedGuestSession(private val link: CoopLink<CoopMsg>, hostSave: SaveData, guestSave: SaveData) {
    val mirror = SharedMirror(hostSave, guestSave)
    val partner = PartnerState()

    init {
        mirror.ally.remote = object : RemoteControl {
            override fun act(code: Int, arg: Int, x: Float, y: Float) {
                link.send(CoopMsg.Act(code, arg, x, y))
            }
        }
    }

    fun update(dt: Float) {
        var m = link.poll()
        while (m != null) {
            when (m) {
                is CoopMsg.Snap -> runCatching { mirror.apply(m.data) }
                CoopMsg.Bye -> partner.connected = false
                else -> {}
            }
            m = link.poll()
        }
        if (!link.connected) partner.connected = false
        mirror.tick(dt)
        val host = mirror.lead
        partner.silence = mirror.silence
        partner.down = host.over
        partner.hpFrac = (host.hp / host.maxHp).coerceIn(0f, 1f)
        partner.distance = host.distanceMeters
        partner.biome = host.biome
        partner.kills = host.kills
        partner.ourPassengerAway = host.guestTime
        // If the host is gone, end the run here so the coins earned so far are banked.
        if (!partner.connected && !mirror.ally.over) mirror.ally.over = true
    }

    fun close() {
        mirror.ally.remote = null
        link.close()
    }
}

/** A pair of in-memory links: what one sends, the other polls. */
class LoopbackPair {
    val a: CoopLink<CoopMsg> = Side()
    val b: CoopLink<CoopMsg> = Side()

    init {
        (a as Side).peer = b as Side
        b.peer = a
    }

    private class Side : CoopLink<CoopMsg> {
        lateinit var peer: Side
        private val queue = java.util.concurrent.ConcurrentLinkedQueue<CoopMsg>()
        @Volatile override var connected = true
            private set

        override fun send(msg: CoopMsg) {
            if (connected) peer.queue.add(msg)
        }

        override fun poll(): CoopMsg? = queue.poll()

        override fun close() {
            connected = false
            peer.connected = false
        }
    }
}
