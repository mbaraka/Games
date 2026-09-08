package com.baraka.game.clashdecks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baraka.game.clashdecks.ai.CpuStrategy
import com.baraka.game.clashdecks.ai.Difficulty
import com.baraka.game.clashdecks.data.DuelSoundEngine
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.systems.DuelEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ClashScreen { MENU, DUEL, CODEX, RESULT }

data class ClashUiModel(
    val screen: ClashScreen = ClashScreen.MENU,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val duel: DuelState? = null,
    val cpuThinking: Boolean = false
)

/**
 * Owns the duel loop and the procedural sound engine. All rules live in [DuelEngine];
 * this class only sequences player input, the animated CPU turn, and sound draining.
 */
class ClashDecksViewModel : ViewModel() {

    private val rng = Random(System.nanoTime())
    private val sound = DuelSoundEngine()

    private val _ui = MutableStateFlow(ClashUiModel())
    val ui: StateFlow<ClashUiModel> = _ui.asStateFlow()

    private var screenBeforeCodex: ClashScreen = ClashScreen.MENU

    fun setDifficulty(difficulty: Difficulty) = _ui.update { it.copy(difficulty = difficulty) }

    fun startDuel() {
        val state = DuelEngine.startDuel(rng)
        _ui.update { it.copy(screen = ClashScreen.DUEL, cpuThinking = false, duel = null) }
        applyState(state)
    }

    fun rematch() = startDuel()

    fun backToMenu() {
        _ui.update { ClashUiModel(difficulty = it.difficulty) }
    }

    fun openCodex() {
        screenBeforeCodex = _ui.value.screen
        _ui.update { it.copy(screen = ClashScreen.CODEX) }
    }

    fun closeCodex() {
        _ui.update { it.copy(screen = screenBeforeCodex) }
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

                delay(650)
                state = _ui.value.duel ?: break
                val plan = CpuStrategy.chooseTurn(state, _ui.value.difficulty, rng)
                for (cardId in plan) {
                    state = DuelEngine.playCard(state, cardId, rng)
                    applyState(state)
                    if (state.winner != null) break
                    delay(700)
                }
                if (state.winner != null) break

                state = DuelEngine.endTurn(state, rng)
                applyState(state)
            }
            _ui.update { it.copy(cpuThinking = false) }
        }
    }

    /** Drains the state's pending sounds, then stores it (clearing the sound queue). */
    private fun applyState(state: DuelState) {
        state.pendingSounds.forEach(sound::play)
        val cleared = state.copy(pendingSounds = emptyList())
        _ui.update {
            it.copy(
                duel = cleared,
                screen = if (cleared.winner != null) ClashScreen.RESULT else it.screen
            )
        }
    }

    override fun onCleared() {
        sound.release()
        super.onCleared()
    }
}
