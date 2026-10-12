package com.baraka.game.minesweeper.coop

import com.baraka.game.coop.CoopCodec
import com.baraka.game.coop.CoopLink
import com.baraka.game.coop.CoopSpec
import com.baraka.game.coop.SimulatedPartner
import com.baraka.game.minesweeper.model.Cell
import com.baraka.game.minesweeper.model.MineSnap
import com.baraka.game.minesweeper.model.MineStatus
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
import kotlin.random.Random

/** Bump whenever a [MineMsg] is added or changed; phones on different versions warn instead of silently misbehaving. */
const val MINE_PROTOCOL = 1

/** Everything two phones say to each other. The host owns the board; the guest only asks and watches. */
sealed class MineMsg {
    class Hello(val version: Int) : MineMsg()
    /** Host -> guest after every change. */
    class Snap(val snap: MineSnap) : MineMsg()
    /** Guest -> host: "uncover this cell". */
    class Reveal(val index: Int) : MineMsg()
    /** Guest -> host: "toggle a flag here". */
    class Flag(val index: Int) : MineMsg()
    object RequestRematch : MineMsg()
    object Bye : MineMsg()

    fun write(o: DataOutputStream) {
        when (this) {
            is Hello -> { o.writeByte(1); o.writeInt(version) }
            is Snap -> {
                o.writeByte(2)
                o.writeByte(snap.mode); o.writeByte(snap.w); o.writeByte(snap.h); o.writeByte(snap.mines)
                o.writeByte(snap.lives); o.writeShort(snap.scoreHost); o.writeShort(snap.scoreGuest)
                o.writeByte(snap.status); o.writeInt(snap.boomSeq); o.writeByte(snap.boomBy)
                o.writeBoolean(snap.started)
                o.write(snap.cells)
            }
            is Reveal -> { o.writeByte(3); o.writeShort(index) }
            is Flag -> { o.writeByte(4); o.writeShort(index) }
            RequestRematch -> o.writeByte(5)
            Bye -> o.writeByte(6)
        }
    }

    companion object {
        fun read(i: DataInputStream): MineMsg? = when (i.readUnsignedByte()) {
            1 -> Hello(i.readInt())
            2 -> {
                val mode = i.readUnsignedByte(); val w = i.readUnsignedByte(); val h = i.readUnsignedByte(); val mines = i.readUnsignedByte()
                val lives = i.readUnsignedByte(); val sh = i.readShort().toInt(); val sg = i.readShort().toInt()
                val status = i.readUnsignedByte(); val boomSeq = i.readInt(); val boomBy = i.readUnsignedByte()
                val started = i.readBoolean()
                if (w * h > 1024) null else {
                    val cells = ByteArray(w * h)
                    i.readFully(cells)
                    Snap(MineSnap(mode, w, h, mines, lives, sh, sg, status, boomSeq, boomBy, started, cells))
                }
            }
            3 -> Reveal(i.readUnsignedShort())
            4 -> Flag(i.readUnsignedShort())
            5 -> RequestRematch
            6 -> Bye
            else -> null
        }
    }
}

typealias MineLink = CoopLink<MineMsg>

/** Minesweeper's co-op channel on the shared layer (its own service id, so other games cannot connect to it). */
val MINE_COOP: CoopSpec<MineMsg> = CoopSpec(
    serviceId = UUID.fromString("c3b9a1d2-6e47-4f08-9a5d-2b7e81f04c36"),
    serviceName = "MinesweeperCoop",
    codec = object : CoopCodec<MineMsg> {
        override fun write(msg: MineMsg, out: DataOutputStream) = msg.write(out)
        override fun read(input: DataInputStream): MineMsg? = MineMsg.read(input)
    },
    bye = MineMsg.Bye
)

/**
 * A pretend guest for trying the game on one phone. It sees only what a real guest sees (the snapshots), applies the
 * two classic deductions (flag when covered + flags == number, uncover when flags == number), and otherwise guesses.
 */
class SimulatedMineLink : SimulatedPartner<MineMsg>() {
    private val rng = Random(System.nanoTime())
    private var snap: MineSnap? = null
    private var wait = 1.2f

    override fun send(msg: MineMsg) {
        if (msg is MineMsg.Snap) snap = msg.snap
    }

    override fun tick(dt: Float) {
        if (!connected) return
        val s = snap ?: return
        if (s.status != MineStatus.PLAYING) return
        wait -= dt
        if (wait > 0f) return
        wait = 0.7f + rng.nextFloat() * 0.8f
        val n = s.w * s.h
        if (!s.started) { deliver(MineMsg.Reveal(n / 2 + s.w / 2)); return }
        val start = rng.nextInt(n)
        var guess = -1
        for (k in 0 until n) {
            val i = (start + k) % n
            val c = s.cells[i]
            val st = Cell.state(c)
            if (st == Cell.HIDDEN && guess < 0) guess = i
            if (st != Cell.REVEALED || Cell.adjacent(c) == 0) continue
            val x = i % s.w
            val y = i / s.w
            var hidden = 0; var flags = 0; var firstHidden = -1
            for (dy in -1..1) for (dx in -1..1) {
                val nx = x + dx; val ny = y + dy
                if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= s.w || ny >= s.h) continue
                val j = ny * s.w + nx
                when (Cell.state(s.cells[j])) {
                    Cell.HIDDEN -> { hidden++; if (firstHidden < 0) firstHidden = j }
                    Cell.FLAGGED -> flags++
                }
            }
            val num = Cell.adjacent(c)
            if (hidden > 0 && flags == num) { deliver(MineMsg.Reveal(firstHidden)); return }
            if (hidden > 0 && hidden + flags == num) { deliver(MineMsg.Flag(firstHidden)); return }
        }
        if (guess >= 0) deliver(MineMsg.Reveal(guess))
    }
}
