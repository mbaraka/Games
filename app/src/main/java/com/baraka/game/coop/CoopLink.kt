package com.baraka.game.coop

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/*
 * Shared two-phone co-op layer for every game in the hub. It moves messages between two paired phones over
 * classic Bluetooth and knows nothing about any game: a game supplies its own message type [M] and a
 * [CoopCodec] for it (see CoopSpec), plus its own session class that turns messages into game actions.
 * The lobby UI is in CoopLobbyScreen.kt. See AGENTS.md ("Co-op for any game").
 */

/** Which phone is which in a connection: the host accepted the connection, the guest dialled it. */
enum class CoopSide { HOST, GUEST }

/** Turns a game's messages into bytes and back. [read] returns null for an unknown message so old and new versions can coexist. */
interface CoopCodec<M> {
    fun write(msg: M, out: DataOutputStream)
    fun read(input: DataInputStream): M?
}

/**
 * Describes one game's co-op channel. [serviceId] must be unique per game (generate a fresh UUID): it is what stops
 * two different games from connecting to each other. [bye] is sent when the link is closed on purpose.
 */
class CoopSpec<M>(
    val serviceId: UUID,
    val serviceName: String,
    val codec: CoopCodec<M>,
    val bye: M? = null,
    val maxFrameBytes: Int = 65536
)

/** A two-way channel to the partner's phone. Everything is non-blocking so a game loop never waits on it. */
interface CoopLink<M> {
    val connected: Boolean
    fun send(msg: M)
    /** The next message from the partner, or null when there is none right now. */
    fun poll(): M?
    /** Called every frame (or every ~100 ms); lets a simulated partner act. */
    fun tick(dt: Float) {}
    fun close()
}

/** A connected partner, ready to play together. */
class CoopLaunch<M>(val link: CoopLink<M>, val side: CoopSide)

/** Base for a pretend partner used to try co-op on one phone: override [send] (what the partner hears) and [tick] (what it does), and use [deliver] to "send" a message back. */
abstract class SimulatedPartner<M> : CoopLink<M> {
    private val incoming = ConcurrentLinkedQueue<M>()
    override var connected = true
        protected set

    protected fun deliver(msg: M) {
        incoming.add(msg)
    }

    override fun poll(): M? = incoming.poll()

    override fun close() {
        connected = false
    }
}

/** A classic Bluetooth (RFCOMM) connection between two paired phones. */
class BluetoothLink<M> internal constructor(private val socket: BluetoothSocket, private val spec: CoopSpec<M>) : CoopLink<M> {
    private val incoming = ConcurrentLinkedQueue<M>()
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
                    if (length !in 1..spec.maxFrameBytes) throw IOException("bad frame")
                    val bytes = ByteArray(length)
                    input.readFully(bytes)
                    val msg = spec.codec.read(DataInputStream(bytes.inputStream()))
                    if (msg != null) incoming.add(msg)
                }
            } catch (_: Exception) {
                connected = false
            }
        }, "coop-read").apply { isDaemon = true }.start()
    }

    override fun send(msg: M) {
        if (!connected) return
        val buffer = ByteArrayOutputStream(64)
        spec.codec.write(msg, DataOutputStream(buffer))
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

    override fun poll(): M? = incoming.poll()

    override fun close() {
        if (connected) spec.bye?.let { send(it) }
        writer.execute { runCatching { socket.close() }; connected = false }
        writer.shutdown()
    }
}

/** Bluetooth helpers shared by every game's lobby. */
object CoopBluetooth {
    /** Phones the player already paired in the system Bluetooth settings. Empty without permission. */
    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BluetoothDevice> = try {
        BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList().orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }

    /** Starts looking for nearby phones (results arrive as broadcasts). False if it could not start. */
    @SuppressLint("MissingPermission")
    fun startScan(): Boolean = try {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter != null && (adapter.isDiscovering || adapter.startDiscovery())
    } catch (_: SecurityException) {
        false
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (_: SecurityException) {}
    }

    fun hasAdapter(): Boolean = BluetoothAdapter.getDefaultAdapter() != null

    fun isEnabled(): Boolean = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

    /** Connects to a host phone. Calls [done] from a background thread with the link, or null on failure. */
    @SuppressLint("MissingPermission")
    fun <M> connectTo(spec: CoopSpec<M>, device: BluetoothDevice, done: (BluetoothLink<M>?) -> Unit) {
        Thread({
            val link = try {
                BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                val socket = device.createRfcommSocketToServiceRecord(spec.serviceId)
                socket.connect()
                BluetoothLink(socket, spec)
            } catch (_: Exception) {
                null
            }
            done(link)
        }, "coop-connect").apply { isDaemon = true }.start()
    }
}

/** Waits for a partner to connect. Call [cancel] to stop waiting. */
class BluetoothHost<M>(private val spec: CoopSpec<M>) {
    @Volatile private var server: BluetoothServerSocket? = null
    @Volatile private var cancelled = false

    /** Calls [done] from a background thread with the link, or null if cancelled or it failed. */
    @SuppressLint("MissingPermission")
    fun start(done: (BluetoothLink<M>?) -> Unit) {
        Thread({
            val link = try {
                val s = BluetoothAdapter.getDefaultAdapter()?.listenUsingRfcommWithServiceRecord(spec.serviceName, spec.serviceId)
                server = s
                val socket = s?.accept()
                runCatching { s?.close() }
                if (socket != null && !cancelled) BluetoothLink(socket, spec) else null
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
