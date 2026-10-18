package com.baraka.game.clashdecks.coop

import com.baraka.game.clashdecks.model.CardLibrary
import com.baraka.game.clashdecks.model.CardType
import com.baraka.game.clashdecks.model.DuelLogEntry
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelSound
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.DuelVfx
import com.baraka.game.clashdecks.model.Duelist
import com.baraka.game.clashdecks.model.LogKind
import com.baraka.game.clashdecks.model.StatusEffect
import com.baraka.game.clashdecks.model.StatusKind
import com.baraka.game.clashdecks.model.VfxSide
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Turns a [DuelState] into bytes and back for [ClashCoopMsg.DuelSync]. Only what the guest
 * actually needs to *render* is sent: a duelist's deck/discard piles are never shown in the
 * UI (only `hand.size`/`block`/etc are), and only the host ever calls [DuelEngine]'s draw
 * logic, so they round-trip as empty lists rather than being serialized — the guest's local
 * [DuelState] copy is a display mirror, never simulated locally.
 */
object ClashDuelCodec {

    fun write(o: DataOutputStream, state: DuelState) {
        writeDuelist(o, state.player)
        writeDuelist(o, state.cpu)
        o.writeByte(state.phase.ordinal)
        o.writeBoolean(state.activeIsPlayer)
        o.writeInt(state.turnNumber)
        o.writeBoolean(state.activeStunned)
        o.writeBoolean(state.activeRooted)
        o.writeUTF(state.winner ?: "")
        o.writeShort(state.log.size)
        for (entry in state.log) {
            o.writeUTF(entry.text)
            o.writeByte(entry.kind.ordinal)
        }
        o.writeByte(state.pendingSounds.size)
        for (sound in state.pendingSounds) o.writeByte(sound.ordinal)
        o.writeByte(state.pendingVfx.size)
        for (vfx in state.pendingVfx) writeVfx(o, vfx)
    }

    fun read(i: DataInputStream): DuelState {
        val player = readDuelist(i)
        val cpu = readDuelist(i)
        val phase = DuelPhase.entries[i.readUnsignedByte()]
        val activeIsPlayer = i.readBoolean()
        val turnNumber = i.readInt()
        val activeStunned = i.readBoolean()
        val activeRooted = i.readBoolean()
        val winnerRaw = i.readUTF()
        val logCount = i.readUnsignedShort()
        val log = (0 until logCount).map { DuelLogEntry(i.readUTF(), LogKind.entries[i.readUnsignedByte()]) }
        val soundCount = i.readUnsignedByte()
        val sounds = (0 until soundCount).map { DuelSound.entries[i.readUnsignedByte()] }
        val vfxCount = i.readUnsignedByte()
        val vfx = (0 until vfxCount).map { readVfx(i) }
        return DuelState(
            player = player,
            cpu = cpu,
            phase = phase,
            activeIsPlayer = activeIsPlayer,
            turnNumber = turnNumber,
            activeStunned = activeStunned,
            activeRooted = activeRooted,
            log = log,
            pendingSounds = sounds,
            pendingVfx = vfx,
            winner = winnerRaw.ifEmpty { null }
        )
    }

    private fun writeDuelist(o: DataOutputStream, duelist: Duelist) {
        o.writeUTF(duelist.name)
        o.writeInt(duelist.hp)
        o.writeInt(duelist.maxHp)
        o.writeInt(duelist.energy)
        o.writeInt(duelist.maxEnergyThisTurn)
        o.writeInt(duelist.block)
        o.writeInt(duelist.reflectPending)
        o.writeBoolean(duelist.isCpu)
        o.writeByte(duelist.statuses.size)
        for (status in duelist.statuses) {
            o.writeByte(status.kind.ordinal)
            o.writeInt(status.amount)
            o.writeInt(status.turnsRemaining)
        }
        o.writeByte(duelist.hand.size)
        for (card in duelist.hand) o.writeUTF(card.id)
    }

    private fun readDuelist(i: DataInputStream): Duelist {
        val name = i.readUTF()
        val hp = i.readInt()
        val maxHp = i.readInt()
        val energy = i.readInt()
        val maxEnergyThisTurn = i.readInt()
        val block = i.readInt()
        val reflectPending = i.readInt()
        val isCpu = i.readBoolean()
        val statusCount = i.readUnsignedByte()
        val statuses = (0 until statusCount).map {
            StatusEffect(StatusKind.entries[i.readUnsignedByte()], i.readInt(), i.readInt())
        }
        val handCount = i.readUnsignedByte()
        val hand = (0 until handCount).mapNotNull { CardLibrary.byId(i.readUTF()) }
        return Duelist(
            name = name, hp = hp, maxHp = maxHp, energy = energy, maxEnergyThisTurn = maxEnergyThisTurn,
            block = block, statuses = statuses, reflectPending = reflectPending,
            deck = emptyList(), hand = hand, discard = emptyList(), isCpu = isCpu
        )
    }

    private fun writeVfx(o: DataOutputStream, vfx: DuelVfx) {
        val sideByte = if (vfx.side == VfxSide.PLAYER) 0 else 1
        when (vfx) {
            is DuelVfx.Hit -> { o.writeByte(0); o.writeByte(sideByte); o.writeInt(vfx.toHp); o.writeInt(vfx.blocked) }
            is DuelVfx.Heal -> { o.writeByte(1); o.writeByte(sideByte); o.writeInt(vfx.amount) }
            is DuelVfx.BlockGain -> { o.writeByte(2); o.writeByte(sideByte); o.writeInt(vfx.amount) }
            is DuelVfx.StatusApplied -> { o.writeByte(3); o.writeByte(sideByte); o.writeByte(vfx.kind.ordinal) }
            is DuelVfx.BlockShattered -> { o.writeByte(4); o.writeByte(sideByte); o.writeInt(vfx.amount) }
            is DuelVfx.Frozen -> { o.writeByte(5); o.writeByte(sideByte) }
            is DuelVfx.CardPlayed -> { o.writeByte(6); o.writeByte(sideByte); o.writeByte(vfx.type.ordinal) }
            is DuelVfx.Cleansed -> { o.writeByte(7); o.writeByte(sideByte) }
            is DuelVfx.ReflectReady -> { o.writeByte(8); o.writeByte(sideByte) }
            is DuelVfx.Warded -> { o.writeByte(9); o.writeByte(sideByte) }
        }
    }

    private fun readVfx(i: DataInputStream): DuelVfx {
        val type = i.readUnsignedByte()
        val side = if (i.readUnsignedByte() == 0) VfxSide.PLAYER else VfxSide.CPU
        return when (type) {
            0 -> DuelVfx.Hit(side, i.readInt(), i.readInt())
            1 -> DuelVfx.Heal(side, i.readInt())
            2 -> DuelVfx.BlockGain(side, i.readInt())
            3 -> DuelVfx.StatusApplied(side, StatusKind.entries[i.readUnsignedByte()])
            4 -> DuelVfx.BlockShattered(side, i.readInt())
            5 -> DuelVfx.Frozen(side)
            6 -> DuelVfx.CardPlayed(side, CardType.entries[i.readUnsignedByte()])
            7 -> DuelVfx.Cleansed(side)
            8 -> DuelVfx.ReflectReady(side)
            9 -> DuelVfx.Warded(side)
            else -> DuelVfx.Frozen(side)
        }
    }
}
