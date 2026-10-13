package com.baraka.game.clashdecks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baraka.game.clashdecks.coop.ClashCoopLink
import com.baraka.game.clashdecks.coop.ClashCoopSession
import com.baraka.game.coop.CoopSide as CoopRole
import com.baraka.game.clashdecks.coop.mirroredForGuest
import com.baraka.game.clashdecks.data.ClashDecksSettingsRepository
import com.baraka.game.clashdecks.data.DuelSoundEngine
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.systems.DuelEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Two-phone duel counterpart to [ClashDecksViewModel]. The host is the only side that ever
 * calls [DuelEngine] — it reacts to its own taps exactly like single-player (gated on
 * `activeIsPlayer`) *and* to [ClashCoopMsg.PlayCard]/[ClashCoopMsg.EndTurn] from the guest
 * (gated on *not* `activeIsPlayer`, replacing where single-player would hand the turn to
 * [com.baraka.game.clashdecks.ai.CpuStrategy]), then broadcasts the result. The guest never
 * touches [DuelEngine] — it mirrors whatever the host sends
 * ([com.baraka.game.clashdecks.coop.mirroredForGuest]) and sends its taps as messages instead
 * of mutating state locally, so the two phones can never disagree about the rules.
 *
 * Reuses [ClashUiModel]/[ClashScreen]/[VfxEmission] from the single-player file and the exact
 * same `ui/ClashDecksScreen.kt` composables — from the UI's point of view this is just another
 * source of [ClashUiModel] updates.
 */
class ClashCoopViewModel(application: Application) : AndroidViewModel(application) {

    private val rng = Random(System.nanoTime())
    private val sound = DuelSoundEngine()
    private var vfxSeq = 0L
    private val settingsRepository = ClashDecksSettingsRepository.getInstance(application)

    private val _ui = MutableStateFlow(
        ClashUiModel(screen = ClashScreen.DUEL, soundEnabled = settingsRepository.isSoundEnabled())
    )
    val ui: StateFlow<ClashUiModel> = _ui.asStateFlow()

    private var session: ClashCoopSession? = null
    private var role: CoopRole = CoopRole.HOST
    private var localName: String = ""
    private var screenBeforeOverlay: ClashScreen = ClashScreen.DUEL
    private var pollJob: Job? = null

    val isHost: Boolean get() = role == CoopRole.HOST
    val partnerConnected: Boolean get() = session?.connected ?: false

    fun playerName(): String = settingsRepository.playerName()

    fun setPlayerName(name: String) {
        settingsRepository.setPlayerName(name)
    }

    fun setSoundEnabled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
        _ui.update { it.copy(soundEnabled = enabled) }
    }

    /** Starts carrying a just-connected [link] as [role], introducing us as [localName] (blank falls back to "You"/"Opponent"). The host deals immediately; the guest waits for the first sync. */
    fun begin(link: ClashCoopLink, role: CoopRole, localName: String) {
        this.role = role
        this.localName = localName
        val newSession = ClashCoopSession(link, role, localName)
        session = newSession
        newSession.onVersionMismatch = { _ui.update { it.copy(partnerVersionMismatch = true) } }
        when (role) {
            CoopRole.HOST -> {
                newSession.onRemotePlayCard = { cardId -> hostApplyRemoteCard(cardId) }
                newSession.onRemoteEndTurn = { hostApplyRemoteEndTurn() }
                newSession.onRematchRequested = { hostStartDuel() }
                newSession.onPartnerHello = { partnerName -> hostRenamePartner(partnerName) }
                hostStartDuel()
            }
            CoopRole.GUEST -> {
                newSession.onStateReceived = { remote -> guestReceive(remote) }
            }
        }
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(100)
                newSession.update()
            }
        }
    }

    fun openCodex() {
        screenBeforeOverlay = _ui.value.screen
        _ui.update { it.copy(screen = ClashScreen.CODEX) }
    }

    fun closeCodex() {
        _ui.update { it.copy(screen = screenBeforeOverlay) }
    }

    fun openSettings() {
        screenBeforeOverlay = _ui.value.screen
        _ui.update { it.copy(screen = ClashScreen.SETTINGS) }
    }

    fun closeSettings() {
        _ui.update { it.copy(screen = screenBeforeOverlay) }
    }

    fun playCard(cardId: String) {
        val duel = _ui.value.duel ?: return
        if (!duel.activeIsPlayer || duel.winner != null) return
        when (role) {
            CoopRole.HOST -> applyHostState(DuelEngine.playCard(duel, cardId, rng))
            CoopRole.GUEST -> session?.sendPlayCard(cardId)
        }
    }

    fun endTurn() {
        val duel = _ui.value.duel ?: return
        if (!duel.activeIsPlayer || duel.winner != null) return
        when (role) {
            CoopRole.HOST -> applyHostState(DuelEngine.endTurn(duel, rng))
            CoopRole.GUEST -> session?.sendEndTurn()
        }
    }

    fun rematch() {
        when (role) {
            CoopRole.HOST -> hostStartDuel()
            CoopRole.GUEST -> session?.sendRematchRequest()
        }
    }

    private fun hostStartDuel() {
        val state = DuelEngine.startDuel(rng, playerName = localName.ifBlank { "You" }, cpuName = "Opponent")
        applyHostState(state, forceDuelScreen = true)
    }

    /** The guest's Hello told us their chosen name — rename them in the live duel (if one's running) and let the guest know via the next broadcast. */
    private fun hostRenamePartner(partnerName: String) {
        val resolved = partnerName.ifBlank { "Opponent" }
        val duel = _ui.value.duel ?: return
        if (duel.cpu.name == resolved) return
        applyHostState(duel.copy(cpu = duel.cpu.copy(name = resolved)))
    }

    private fun hostApplyRemoteCard(cardId: String) {
        val duel = _ui.value.duel ?: return
        if (duel.activeIsPlayer || duel.winner != null) return // not the guest's turn
        applyHostState(DuelEngine.playCard(duel, cardId, rng))
    }

    private fun hostApplyRemoteEndTurn() {
        val duel = _ui.value.duel ?: return
        if (duel.activeIsPlayer || duel.winner != null) return
        applyHostState(DuelEngine.endTurn(duel, rng))
    }

    /** Host path: play sounds/vfx locally, store the cleared state, and broadcast the pre-clear state (still carrying this change's events) to the guest. */
    private fun applyHostState(state: DuelState, forceDuelScreen: Boolean = false) {
        if (_ui.value.soundEnabled) state.pendingSounds.forEach(sound::play)
        val emissions = state.pendingVfx.map { VfxEmission(vfxSeq++, it) }
        val cleared = state.copy(pendingSounds = emptyList(), pendingVfx = emptyList())
        _ui.update {
            it.copy(
                duel = cleared,
                effects = emissions,
                screen = if (forceDuelScreen) ClashScreen.DUEL else if (cleared.winner != null) ClashScreen.RESULT else it.screen
            )
        }
        session?.broadcast(state)
    }

    /** Guest path: mirror the host's state onto "me" (see [mirroredForGuest]), play sounds/vfx locally, store it. */
    private fun guestReceive(remote: DuelState) {
        val mirrored = remote.mirroredForGuest()
        if (_ui.value.soundEnabled) mirrored.pendingSounds.forEach(sound::play)
        val emissions = mirrored.pendingVfx.map { VfxEmission(vfxSeq++, it) }
        val cleared = mirrored.copy(pendingSounds = emptyList(), pendingVfx = emptyList())
        _ui.update {
            it.copy(
                duel = cleared,
                effects = emissions,
                screen = if (cleared.winner != null) ClashScreen.RESULT else ClashScreen.DUEL
            )
        }
    }

    /** Leaving the coop screen without destroying the Activity — close the link so the partner's phone notices, and reset so a new [begin] can start fresh. */
    fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        session?.close()
        session = null
        _ui.update { ClashUiModel(screen = ClashScreen.DUEL, soundEnabled = it.soundEnabled) }
    }

    override fun onCleared() {
        disconnect()
        sound.release()
        super.onCleared()
    }
}
