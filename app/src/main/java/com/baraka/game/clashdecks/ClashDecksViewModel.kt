package com.baraka.game.clashdecks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baraka.game.clashdecks.ai.CpuStrategy
import com.baraka.game.clashdecks.ai.Difficulty
import com.baraka.game.clashdecks.data.ClashDecksSettingsRepository
import com.baraka.game.clashdecks.data.DuelSoundEngine
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.DuelVfx
import com.baraka.game.clashdecks.model.StatusKind
import com.baraka.game.clashdecks.systems.DuelDebugTools
import com.baraka.game.clashdecks.systems.DuelEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ClashScreen { MENU, DUEL, CODEX, SETTINGS, RESULT }

/** One visual effect plus a unique id so the UI's effect layer plays each batch once. */
data class VfxEmission(val id: Long, val vfx: DuelVfx)

data class ClashUiModel(
    val screen: ClashScreen = ClashScreen.MENU,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val duel: DuelState? = null,
    val cpuThinking: Boolean = false,
    /** The most recent batch of effects to animate; replaced on every state transition. */
    val effects: List<VfxEmission> = emptyList(),
    val soundEnabled: Boolean = true,
    /** Co-op only: the partner reported a different [com.baraka.game.clashdecks.coop.CLASH_COOP_PROTOCOL] — they're on a different build. Always false in single-player. */
    val partnerVersionMismatch: Boolean = false
)

/**
 * Owns the duel loop and the procedural sound engine. All rules live in [DuelEngine];
 * this class only sequences player input, the animated CPU turn, and sound draining.
 * [AndroidViewModel] (not plain [androidx.lifecycle.ViewModel]) so it can read/persist the
 * sound setting via [ClashDecksSettingsRepository] the same way the other games do.
 */
class ClashDecksViewModel(application: Application) : AndroidViewModel(application) {

    private val rng = Random(System.nanoTime())
    private val sound = DuelSoundEngine()
    private var vfxSeq = 0L
    private val settingsRepository = ClashDecksSettingsRepository.getInstance(application)

    private val _ui = MutableStateFlow(ClashUiModel(soundEnabled = settingsRepository.isSoundEnabled()))
    val ui: StateFlow<ClashUiModel> = _ui.asStateFlow()

    /** Where Codex/Settings were opened from, so closing either returns to the right screen. */
    private var screenBeforeOverlay: ClashScreen = ClashScreen.MENU

    fun setSoundEnabled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
        _ui.update { it.copy(soundEnabled = enabled) }
    }

    fun setDifficulty(difficulty: Difficulty) = _ui.update { it.copy(difficulty = difficulty) }

    fun startDuel() {
        val state = DuelEngine.startDuel(rng)
        _ui.update { it.copy(screen = ClashScreen.DUEL, cpuThinking = false, duel = null) }
        applyState(state)
    }

    fun rematch() = startDuel()

    fun backToMenu() {
        _ui.update { ClashUiModel(difficulty = it.difficulty, soundEnabled = it.soundEnabled) }
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
        val model = _ui.value
        val duel = model.duel ?: return
        if (model.cpuThinking || duel.phase != DuelPhase.PLAYER_TURN || duel.winner != null) return
        applyState(DuelEngine.playCard(duel, cardId, rng))
    }

    fun endTurn() {
        val model = _ui.value
        val duel = model.duel ?: return
        if (model.cpuThinking || duel.phase != DuelPhase.PLAYER_TURN || duel.winner != null) return

        val afterPlayer = DuelEngine.endTurn(duel, rng)
        applyState(afterPlayer)
        if (afterPlayer.winner == null && !afterPlayer.activeIsPlayer) {
            runCpuTurn()
        }
        // If afterPlayer.activeIsPlayer is true again, the CPU turn was skipped (frozen) —
        // control is already back with the player, nothing to schedule.
    }

    private fun runCpuTurn() {
        viewModelScope.launch {
            _ui.update { it.copy(cpuThinking = true) }

            // Loop so the CPU takes another turn if the player was skipped (Freeze):
            // DuelEngine.endTurn recursively hands a frozen player's turn straight back,
            // leaving it the CPU's turn again with nothing else to drive it.
            var safety = 0
            while (safety++ < 8) {
                var state = _ui.value.duel ?: break
                if (state.winner != null || state.activeIsPlayer) break

                // Plan first, *then* decide whether there's anything worth a "thinking"
                // pause for — an empty hand/no affordable card used to burn a fixed 650ms
                // doing nothing before immediately ending the turn anyway.
                val plan = CpuStrategy.chooseTurn(state, _ui.value.difficulty, rng)
                if (plan.isNotEmpty()) {
                    delay(650)
                    state = _ui.value.duel ?: break
                    for (cardId in plan) {
                        state = DuelEngine.playCard(state, cardId, rng)
                        applyState(state)
                        if (state.winner != null) break
                        delay(700)
                    }
                    if (state.winner != null) break
                }

                state = DuelEngine.endTurn(state, rng)
                applyState(state)
            }
            _ui.update { it.copy(cpuThinking = false) }
        }
    }

    // ---------------------------------------------------------------- debug tools
    // See systems/DuelDebugTools.kt. Gated behind DebugKit's unlock in ClashSettingsScreen;
    // every one of these is a no-op when there's no active duel to mutate.

    fun debugHealPlayer() = mutateDuel(DuelDebugTools::healPlayerFull)
    fun debugHealCpu() = mutateDuel(DuelDebugTools::healCpuFull)
    fun debugDamagePlayerToOne() = mutateDuel { DuelDebugTools.setPlayerHp(it, 1) }
    fun debugDamageCpuToOne() = mutateDuel { DuelDebugTools.setCpuHp(it, 1) }
    fun debugGrantPlayerEnergy() = mutateDuel { DuelDebugTools.grantPlayerEnergy(it, 5) }
    fun debugDrawPlayerCard() = mutateDuel { DuelDebugTools.drawPlayerCard(it, rng) }
    fun debugCleansePlayer() = mutateDuel(DuelDebugTools::cleansePlayer)
    fun debugPoisonCpu() = mutateDuel { DuelDebugTools.applyStatusToCpu(it, StatusKind.POISON) }
    fun debugFreezeCpu() = mutateDuel { DuelDebugTools.applyStatusToCpu(it, StatusKind.FREEZE) }
    fun debugForceWin() = mutateDuel(DuelDebugTools::forceWin)
    fun debugForceLose() = mutateDuel(DuelDebugTools::forceLose)

    private fun mutateDuel(transform: (DuelState) -> DuelState) {
        val current = _ui.value.duel ?: return
        applyState(transform(current))
    }

    /** Plays the state's pending sounds, forwards its VFX to the UI, then stores it cleared. */
    private fun applyState(state: DuelState) {
        if (_ui.value.soundEnabled) state.pendingSounds.forEach(sound::play)
        val emissions = state.pendingVfx.map { VfxEmission(vfxSeq++, it) }
        val cleared = state.copy(pendingSounds = emptyList(), pendingVfx = emptyList())
        _ui.update {
            it.copy(
                duel = cleared,
                effects = emissions,
                screen = if (cleared.winner != null) ClashScreen.RESULT else it.screen
            )
        }
    }

    override fun onCleared() {
        sound.release()
        super.onCleared()
    }
}
