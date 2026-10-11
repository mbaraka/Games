package com.baraka.game.clashdecks.coop

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.baraka.game.clashdecks.ai.CpuStrategy
import com.baraka.game.clashdecks.ai.Difficulty
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.systems.DuelEngine
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.random.Random

const val CLASH_COOP_PROTOCOL = 1

/** Everything two phones say to each other over a duel — small and infrequent (once per card play or turn), so a whole [DuelState] snapshot per sync is simplest and never a bandwidth concern. */
sealed class ClashCoopMsg {
    class Hello(val version: Int) : ClashCoopMsg()
    /** Host → guest, after every rules change: the authoritative state to mirror and render. */
    class DuelSync(val state: DuelState) : ClashCoopMsg()
    /** Guest → host: "play this card of mine". */
    class PlayCard(val cardId: String) : ClashCoopMsg()
    /** Guest → host. */
    object EndTurn : ClashCoopMsg()
    object RequestRematch : ClashCoopMsg()
    object Bye : ClashCoopMsg()

    fun write(o: DataOutputStream) {
        when (this) {
            is Hello -> { o.writeByte(1); o.writeInt(version) }
            is DuelSync -> { o.writeByte(2); ClashDuelCodec.write(o, state) }
            is PlayCard -> { o.writeByte(3); o.writeUTF(cardId) }
            EndTurn -> o.writeByte(4)
            RequestRematch -> o.writeByte(5)
            Bye -> o.writeByte(6)
        }
    }

    companion object {
        fun read(i: DataInputStream): ClashCoopMsg? = when (i.readUnsignedByte()) {
            1 -> Hello(i.readInt())
            2 -> DuelSync(ClashDuelCodec.read(i))
            3 -> PlayCard(i.readUTF())
            4 -> EndTurn
            5 -> RequestRematch
            6 -> Bye
            else -> null
        }
    }
}

/** A two-way channel to the partner's phone. Everything is non-blocking so the duel loop never waits on it. */
interface ClashCoopLink {
    val connected: Boolean
    fun send(msg: ClashCoopMsg)
    /** The next message from the partner, or null when there is none right now. */
    fun poll(): ClashCoopMsg?
    /** Called periodically; lets a simulated partner act. */
    fun tick(dt: Float) {}
    fun close()
}

private val SERVICE_UUID: UUID = UUID.fromString("5e2a7c34-1f9b-4d6e-8a3c-9b4f2e7d6a10")

/** A classic Bluetooth (RFCOMM) connection between two paired phones — same shape as trainsurvivor's BluetoothLink. */
class ClashBluetoothLink internal constructor(private val socket: BluetoothSocket) : ClashCoopLink {
    private val incoming = ConcurrentLinkedQueue<ClashCoopMsg>()
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
                    if (length !in 1..65536) throw IOException("bad frame")
                    val bytes = ByteArray(length)
                    input.readFully(bytes)
                    val msg = ClashCoopMsg.read(DataInputStream(bytes.inputStream()))
                    if (msg != null) incoming.add(msg)
                }
            } catch (_: Exception) {
                connected = false
            }
        }, "clash-coop-read").apply { isDaemon = true }.start()
    }

    override fun send(msg: ClashCoopMsg) {
        if (!connected) return
        val buffer = ByteArrayOutputStream(64)
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

    override fun poll(): ClashCoopMsg? = incoming.poll()

    override fun close() {
        if (connected) send(ClashCoopMsg.Bye)
        writer.execute { runCatching { socket.close() }; connected = false }
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
        fun connectTo(device: BluetoothDevice, done: (ClashBluetoothLink?) -> Unit) {
            Thread({
                val link = try {
                    BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                    val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                    socket.connect()
                    ClashBluetoothLink(socket)
                } catch (_: Exception) {
                    null
                }
                done(link)
            }, "clash-coop-connect").apply { isDaemon = true }.start()
        }
    }
}

/** Waits for a partner to connect. Call [cancel] to stop waiting. */
class ClashBluetoothHost {
    @Volatile private var server: BluetoothServerSocket? = null
    @Volatile private var cancelled = false

    /** Calls [done] from a background thread with the link, or null if cancelled or it failed. */
    @SuppressLint("MissingPermission")
    fun start(done: (ClashBluetoothLink?) -> Unit) {
        Thread({
            val link = try {
                val s = BluetoothAdapter.getDefaultAdapter()?.listenUsingRfcommWithServiceRecord("ClashDecksCoop", SERVICE_UUID)
                server = s
                val socket = s?.accept()
                runCatching { s?.close() }
                if (socket != null && !cancelled) ClashBluetoothLink(socket) else null
            } catch (_: Exception) {
                null
            }
            done(link)
        }, "clash-coop-host").apply { isDaemon = true }.start()
    }

    fun cancel() {
        cancelled = true
        runCatching { server?.close() }
    }
}

/**
 * A pretend partner for trying two-phone duels on one phone: it plays its "guest" turns
 * using the same [CpuStrategy] the single-player CPU uses, reacting to whatever [DuelSync]
 * the host sends it, so hosting against this link feels like hosting against a real (if not
 * especially cunning) remote player.
 */
class SimulatedClashLink(private val difficulty: Difficulty = Difficulty.NORMAL) : ClashCoopLink {
    private val incoming = ConcurrentLinkedQueue<ClashCoopMsg>()
    private val rng = Random(System.nanoTime())
    override var connected = true
        private set

    private var latestState: DuelState? = null
    private var actedOnTurn = -1
    private var thinkDelay = 0f

    override fun send(msg: ClashCoopMsg) {
        if (msg is ClashCoopMsg.DuelSync) {
            latestState = msg.state
            if (msg.state.activeIsPlayer || msg.state.winner != null) {
                actedOnTurn = -1 // it's the host's turn (or the duel ended); reset so the next cpu turn is acted on fresh
            }
        }
    }

    override fun poll(): ClashCoopMsg? = incoming.poll()

    override fun tick(dt: Float) {
        if (!connected) return
        val state = latestState ?: return
        if (state.winner != null || state.activeIsPlayer) return
        if (state.turnNumber == actedOnTurn) return // already queued this turn's plan

        thinkDelay -= dt
        if (thinkDelay > 0f) return
        thinkDelay = 0.5f
        actedOnTurn = state.turnNumber

        val plan = CpuStrategy.chooseTurn(state, difficulty, rng)
        for (cardId in plan) incoming.add(ClashCoopMsg.PlayCard(cardId))
        incoming.add(ClashCoopMsg.EndTurn)
    }

    override fun close() {
        connected = false
    }
}
