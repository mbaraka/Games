package com.baraka.game.clashdecks.coop

import com.baraka.game.coop.CoopCodec
import com.baraka.game.coop.CoopLink
import com.baraka.game.coop.CoopSpec
import com.baraka.game.coop.SimulatedPartner
import com.baraka.game.clashdecks.ai.CpuStrategy
import com.baraka.game.clashdecks.ai.Difficulty
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.systems.DuelEngine
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
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

/**
 * A pretend partner for trying two-phone duels on one phone: it plays its "guest" turns
 * using the same [CpuStrategy] the single-player CPU uses, reacting to whatever [DuelSync]
 * the host sends it, so hosting against this link feels like hosting against a real (if not
 * especially cunning) remote player.
 */
class SimulatedClashLink(private val difficulty: Difficulty = Difficulty.NORMAL) : SimulatedPartner<ClashCoopMsg>() {
    private val rng = Random(System.nanoTime())

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
        for (cardId in plan) deliver(ClashCoopMsg.PlayCard(cardId))
        deliver(ClashCoopMsg.EndTurn)
    }

}

/** A link to the partner's phone carrying duel messages. */
typealias ClashCoopLink = CoopLink<ClashCoopMsg>

/** Clash Decks' co-op channel on the shared layer (its own service id, so other games cannot connect to it). */
val CLASH_COOP: CoopSpec<ClashCoopMsg> = CoopSpec(
    serviceId = UUID.fromString("5e2a7c34-1f9b-4d6e-8a3c-9b4f2e7d6a10"),
    serviceName = "ClashDecksCoop",
    codec = object : CoopCodec<ClashCoopMsg> {
        override fun write(msg: ClashCoopMsg, out: DataOutputStream) = msg.write(out)
        override fun read(input: DataInputStream): ClashCoopMsg? = ClashCoopMsg.read(input)
    },
    bye = ClashCoopMsg.Bye
)
