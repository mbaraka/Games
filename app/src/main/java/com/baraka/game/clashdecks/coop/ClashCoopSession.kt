package com.baraka.game.clashdecks.coop

import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.coop.CoopSide as CoopRole

/**
 * Carries a duel over a [ClashCoopLink]. The host is authoritative — it's the only side that
 * ever calls [com.baraka.game.clashdecks.systems.DuelEngine] — this class just ferries
 * [ClashCoopMsg]s between the link and whichever callbacks `ClashCoopViewModel` wired up:
 * [onRemotePlayCard]/[onRemoteEndTurn] fire on the host when the guest acts,
 * [onStateReceived] fires on the guest for every host update.
 */
class ClashCoopSession(private val link: ClashCoopLink, val role: CoopRole, localName: String) {

    var onStateReceived: ((DuelState) -> Unit)? = null
    var onRemotePlayCard: ((String) -> Unit)? = null
    var onRemoteEndTurn: (() -> Unit)? = null
    var onRematchRequested: (() -> Unit)? = null
    /** Fired once the partner's [ClashCoopMsg.Hello] arrives with their chosen name (blank if they never set one). */
    var onPartnerHello: ((String) -> Unit)? = null
    /** Fired once if the partner's [CLASH_COOP_PROTOCOL] doesn't match ours — they're on a different build of the game. */
    var onVersionMismatch: (() -> Unit)? = null

    val connected: Boolean get() = link.connected

    init {
        link.send(ClashCoopMsg.Hello(CLASH_COOP_PROTOCOL, localName))
    }

    /** Call regularly (e.g. every ~100ms) from the owning ViewModel's scope. */
    fun update() {
        link.tick(0.1f)
        var message = link.poll()
        while (message != null) {
            handle(message)
            message = link.poll()
        }
    }

    /** Host → guest: the new authoritative state. */
    fun broadcast(state: DuelState) = link.send(ClashCoopMsg.DuelSync(state))

    /** Guest → host. */
    fun sendPlayCard(cardId: String) = link.send(ClashCoopMsg.PlayCard(cardId))
    fun sendEndTurn() = link.send(ClashCoopMsg.EndTurn)
    fun sendRematchRequest() = link.send(ClashCoopMsg.RequestRematch)

    fun close() {
        link.close()
    }

    private fun handle(message: ClashCoopMsg) {
        when (message) {
            is ClashCoopMsg.Hello -> {
                if (message.version != CLASH_COOP_PROTOCOL) onVersionMismatch?.invoke()
                onPartnerHello?.invoke(message.name)
            }
            is ClashCoopMsg.DuelSync -> onStateReceived?.invoke(message.state)
            is ClashCoopMsg.PlayCard -> onRemotePlayCard?.invoke(message.cardId)
            ClashCoopMsg.EndTurn -> onRemoteEndTurn?.invoke()
            ClashCoopMsg.RequestRematch -> onRematchRequested?.invoke()
            ClashCoopMsg.Bye -> {}
        }
    }
}
