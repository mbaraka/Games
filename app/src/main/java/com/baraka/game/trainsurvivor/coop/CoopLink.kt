package com.baraka.game.trainsurvivor.coop

import com.baraka.game.coop.CoopCodec
import com.baraka.game.coop.CoopSpec
import com.baraka.game.coop.SimulatedPartner
import com.baraka.game.trainsurvivor.model.CrewSlot
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/** Bumped whenever messages are added, so two phones with different builds can warn each other instead of silently ignoring messages. */
const val COOP_PROTOCOL = 2

/** Everything two trains say to each other. Small, so a Bluetooth link carries it easily. */
sealed class CoopMsg {
    class Hello(val version: Int) : CoopMsg()
    /** Sent about 4 times a second: how the sender's train is doing. */
    class State(val hpFrac: Float, val distance: Int, val biome: Int, val kills: Int, val guestLeft: Float, val awayLeft: Float) : CoopMsg()
    class Junction(val a: Int, val b: Int) : CoopMsg()
    class RouteReq(val choice: Int) : CoopMsg()
    class RouteChosen(val choice: Int) : CoopMsg()
    class Boss(val type: Int) : CoopMsg()
    /** Boss damage as a fraction of the boss's health, so both trains' bosses stay level. */
    class BossDamage(val fraction: Float) : CoopMsg()
    class Passenger(val survivor: Int, val slot: CrewSlot, val seconds: Float) : CoopMsg()
    object Down : CoopMsg()
    object Bye : CoopMsg()
    object Blast : CoopMsg()
    class Repair(val fraction: Float) : CoopMsg()
    class Coins(val amount: Int) : CoopMsg()
    /** The sender's train is locked by a co-op hazard (kind = HazardKind ordinal). */
    class HelpNeeded(val kind: Int) : CoopMsg()
    /** The sender cleared the receiver's lock. */
    object HelpGiven : CoopMsg()
    object Tow : CoopMsg()
    /** The sender's train for the partner's second-track view: shape, paint, health percent per seat, guns that fired. */
    class Ghost(val shape: Int, val livery: Int, val hp: IntArray, val shotMask: Int) : CoopMsg()
    object RescueCall : CoopMsg()
    class RescueDone(val bonus: Int) : CoopMsg()

    fun write(o: DataOutputStream) {
        when (this) {
            is Hello -> { o.writeByte(1); o.writeInt(version) }
            is State -> { o.writeByte(2); o.writeFloat(hpFrac); o.writeInt(distance); o.writeByte(biome); o.writeInt(kills); o.writeFloat(guestLeft); o.writeFloat(awayLeft) }
            is Junction -> { o.writeByte(3); o.writeByte(a); o.writeByte(b) }
            is RouteReq -> { o.writeByte(4); o.writeByte(choice) }
            is RouteChosen -> { o.writeByte(5); o.writeByte(choice) }
            is Boss -> { o.writeByte(6); o.writeByte(type) }
            is BossDamage -> { o.writeByte(7); o.writeFloat(fraction) }
            is Passenger -> {
                o.writeByte(8); o.writeByte(survivor); o.writeInt(slot.owned); o.writeInt(slot.selected)
                o.writeByte(slot.power); o.writeByte(slot.armor); o.writeFloat(seconds)
            }
            Down -> o.writeByte(9)
            Bye -> o.writeByte(10)
            Blast -> o.writeByte(11)
            is Repair -> { o.writeByte(12); o.writeFloat(fraction) }
            is Coins -> { o.writeByte(13); o.writeInt(amount) }
            is HelpNeeded -> { o.writeByte(14); o.writeByte(kind) }
            HelpGiven -> o.writeByte(15)
            Tow -> o.writeByte(16)
            is Ghost -> {
                o.writeByte(19); o.writeByte(shape); o.writeByte(livery); o.writeByte(hp.size)
                for (v in hp) o.writeByte(v)
                o.writeByte(shotMask)
            }
            RescueCall -> o.writeByte(17)
            is RescueDone -> { o.writeByte(18); o.writeInt(bonus) }
        }
    }

    companion object {
        fun read(i: DataInputStream): CoopMsg? = when (i.readUnsignedByte()) {
            1 -> Hello(i.readInt())
            2 -> State(i.readFloat(), i.readInt(), i.readUnsignedByte(), i.readInt(), i.readFloat(), i.readFloat())
            3 -> Junction(i.readUnsignedByte(), i.readUnsignedByte())
            4 -> RouteReq(i.readUnsignedByte())
            5 -> RouteChosen(i.readUnsignedByte())
            6 -> Boss(i.readUnsignedByte())
            7 -> BossDamage(i.readFloat())
            8 -> Passenger(i.readUnsignedByte(), CrewSlot(i.readInt(), i.readInt(), i.readUnsignedByte(), i.readUnsignedByte()), i.readFloat())
            9 -> Down
            10 -> Bye
            11 -> Blast
            12 -> Repair(i.readFloat())
            13 -> Coins(i.readInt())
            14 -> HelpNeeded(i.readUnsignedByte())
            15 -> HelpGiven
            16 -> Tow
            19 -> {
                val shape = i.readUnsignedByte(); val livery = i.readUnsignedByte(); val n = i.readUnsignedByte()
                val hp = IntArray(n.coerceIn(0, 8)) { i.readUnsignedByte() }
                Ghost(shape, livery, hp, i.readUnsignedByte())
            }
            17 -> RescueCall
            18 -> RescueDone(i.readInt())
            else -> null
        }
    }
}

/** A pretend partner for trying co-op on one phone (and for the debug tools): it drifts along, lends a passenger now and then. */
class SimulatedLink : SimulatedPartner<CoopMsg>() {
    private var clock = 0f
    private var stateTimer = 0f
    private var lendTimer = 12f
    private var hp = 1f
    private var junctionPending = -1f
    private var junctionA = 0

    private var helpIn = -1f

    override fun send(msg: CoopMsg) {
        if (msg is CoopMsg.HelpNeeded) helpIn = 2.5f // the pretend partner clears your lock after a moment
        if (msg is CoopMsg.Junction) {
            junctionPending = 3f // the pretend partner takes the first route a moment later
            junctionA = msg.a
        }
    }

    override fun tick(dt: Float) {
        if (!connected) return
        clock += dt
        if (junctionPending > 0f) {
            junctionPending -= dt
            if (junctionPending <= 0f) deliver(CoopMsg.RouteReq(junctionA))
        }
        if (helpIn > 0f) {
            helpIn -= dt
            if (helpIn <= 0f) deliver(CoopMsg.HelpGiven)
        }
        lendTimer -= dt
        if (lendTimer <= 0f) {
            lendTimer = 50f
            deliver(CoopMsg.Passenger(1, CrewSlot(owned = 0b111, selected = -1, power = 3, armor = 0), 18f))
        }
        stateTimer += dt
        if (stateTimer >= 0.25f) {
            stateTimer = 0f
            hp = (0.6f + 0.4f * kotlin.math.sin(clock * 0.2f)).coerceIn(0.05f, 1f)
            deliver(CoopMsg.State(hp, (clock * 14f).toInt(), 0, (clock * 0.7f).toInt(), 0f, 0f))
            deliver(CoopMsg.Ghost(0, 0, IntArray(5) { (hp * 100f).toInt() }, if (clock.toInt() % 2 == 0) 0b00101 else 0b11010))
        }
    }

}

/** Train Survivor's co-op channel on the shared layer (its own service id, so other games cannot connect to it). */
val TRAIN_COOP: CoopSpec<CoopMsg> = CoopSpec(
    serviceId = UUID.fromString("7c3e5b1a-4f0d-4a8e-9a55-0b7a6a2d9c11"),
    serviceName = "TrainSurvivorCoop",
    codec = object : CoopCodec<CoopMsg> {
        override fun write(msg: CoopMsg, out: DataOutputStream) = msg.write(out)
        override fun read(input: DataInputStream): CoopMsg? = CoopMsg.read(input)
    },
    bye = CoopMsg.Bye
)
