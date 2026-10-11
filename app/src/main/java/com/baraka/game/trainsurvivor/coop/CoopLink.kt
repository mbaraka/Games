package com.baraka.game.trainsurvivor.coop

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.baraka.game.trainsurvivor.model.CrewSlot
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

const val COOP_PROTOCOL = 1

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
            else -> null
        }
    }
}

/** A two-way channel to the partner's phone. Everything is non-blocking so the game loop never waits on it. */
interface CoopLink {
    val connected: Boolean
    fun send(msg: CoopMsg)
    /** The next message from the partner, or null when there is none right now. */
    fun poll(): CoopMsg?
    /** Called every frame; lets a simulated partner act. */
    fun tick(dt: Float) {}
    fun close()
}

private val SERVICE_UUID: UUID = UUID.fromString("7c3e5b1a-4f0d-4a8e-9a55-0b7a6a2d9c11")

/** A classic Bluetooth (RFCOMM) connection between two paired phones. */
class BluetoothLink internal constructor(private val socket: BluetoothSocket) : CoopLink {
    private val incoming = ConcurrentLinkedQueue<CoopMsg>()
    private val writer = Executors.newSingleThreadExecutor()
    private val input = DataInputStream(socket.inputStream.buffered())
    private val output = DataOutputStream(socket.outputStream.buffered())
    @Volatile override var connected = true
        private set

    init {
        Thread({
            try {
                while (connected) {
                    val length = input.readInt()
                    if (length !in 1..4096) throw IOException("bad frame")
                    val bytes = ByteArray(length)
                    input.readFully(bytes)
                    val msg = CoopMsg.read(DataInputStream(bytes.inputStream()))
                    if (msg != null) incoming.add(msg)
                }
            } catch (_: Exception) {
                connected = false
            }
        }, "coop-read").apply { isDaemon = true }.start()
    }

    override fun send(msg: CoopMsg) {
        if (!connected) return
        val buffer = ByteArrayOutputStream(32)
        msg.write(DataOutputStream(buffer))
        val bytes = buffer.toByteArray()
        writer.execute {
            try {
                output.writeInt(bytes.size)
                output.write(bytes)
                output.flush()
            } catch (_: Exception) {
                connected = false
            }
        }
    }

    override fun poll(): CoopMsg? = incoming.poll()

    override fun close() {
        if (connected) send(CoopMsg.Bye)
        writer.execute { runCatching { socket.close() } ; connected = false }
        writer.shutdown()
    }

    companion object {
        /** Phones the player already paired in the system Bluetooth settings. Empty without permission. */
        @SuppressLint("MissingPermission")
        fun pairedDevices(): List<BluetoothDevice> = try {
            BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList().orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }

        fun isAvailable(): Boolean = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

        /** Connects to a host phone. Calls [done] from a background thread with the link, or null on failure. */
        @SuppressLint("MissingPermission")
        fun connectTo(device: BluetoothDevice, done: (BluetoothLink?) -> Unit) {
            Thread({
                val link = try {
                    BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                    val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                    socket.connect()
                    BluetoothLink(socket)
                } catch (_: Exception) {
                    null
                }
                done(link)
            }, "coop-connect").apply { isDaemon = true }.start()
        }
    }
}

/** Waits for a partner to connect. Call [cancel] to stop waiting. */
class BluetoothHost {
    @Volatile private var server: BluetoothServerSocket? = null
    @Volatile private var cancelled = false

    /** Calls [done] from a background thread with the link, or null if cancelled or it failed. */
    @SuppressLint("MissingPermission")
    fun start(done: (BluetoothLink?) -> Unit) {
        Thread({
            val link = try {
                val s = BluetoothAdapter.getDefaultAdapter()?.listenUsingRfcommWithServiceRecord("TrainSurvivorCoop", SERVICE_UUID)
                server = s
                val socket = s?.accept()
                runCatching { s?.close() }
                if (socket != null && !cancelled) BluetoothLink(socket) else null
            } catch (_: Exception) {
                null
            }
            done(link)
        }, "coop-host").apply { isDaemon = true }.start()
    }

    fun cancel() {
        cancelled = true
        runCatching { server?.close() }
    }
}

/** A pretend partner for trying co-op on one phone (and for the debug tools): it drifts along, lends a passenger now and then. */
class SimulatedLink : CoopLink {
    private val incoming = ConcurrentLinkedQueue<CoopMsg>()
    override var connected = true
        private set
    private var clock = 0f
    private var stateTimer = 0f
    private var lendTimer = 12f
    private var hp = 1f
    private var junctionPending = -1f
    private var junctionA = 0

    override fun send(msg: CoopMsg) {
        if (msg is CoopMsg.Junction) {
            junctionPending = 3f // the pretend partner takes the first route a moment later
            junctionA = msg.a
        }
    }

    override fun poll(): CoopMsg? = incoming.poll()

    override fun tick(dt: Float) {
        if (!connected) return
        clock += dt
        if (junctionPending > 0f) {
            junctionPending -= dt
            if (junctionPending <= 0f) incoming.add(CoopMsg.RouteReq(junctionA))
        }
        lendTimer -= dt
        if (lendTimer <= 0f) {
            lendTimer = 50f
            incoming.add(CoopMsg.Passenger(1, CrewSlot(owned = 0b111, selected = -1, power = 3, armor = 0), 18f))
        }
        stateTimer += dt
        if (stateTimer >= 0.25f) {
            stateTimer = 0f
            hp = (0.6f + 0.4f * kotlin.math.sin(clock * 0.2f)).coerceIn(0.05f, 1f)
            incoming.add(CoopMsg.State(hp, (clock * 14f).toInt(), 0, (clock * 0.7f).toInt(), 0f, 0f))
        }
    }

    override fun close() {
        connected = false
    }
}
