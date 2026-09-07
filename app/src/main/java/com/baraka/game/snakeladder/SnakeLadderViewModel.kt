package com.baraka.game.snakeladder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SnakeLadderUiState(
    val phase: SnakeLadderPhase = SnakeLadderPhase.Menu,
    val positions: List<Int> = emptyList(),
    /** Same length as [positions] while playing: `true` = CPU, `false` = human. */
    val playerIsCpu: List<Boolean> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val lastDice: Int? = null,
    val lastMessage: String? = null,
    val isRolling: Boolean = false,
    val winnerIndex: Int? = null
) {
    val playerCount: Int get() = positions.size

    fun isCpuPlayer(index: Int): Boolean =
        index in playerIsCpu.indices && playerIsCpu[index]

    val currentIsCpu: Boolean
        get() = isCpuPlayer(currentPlayerIndex)

    val humanCount: Int
        get() = playerIsCpu.count { !it }
}

enum class SnakeLadderPhase {
    Menu,
    Playing,
    Finished
}

class SnakeLadderViewModel : ViewModel() {

    private val random = Random.Default
    private val _uiState = MutableStateFlow(SnakeLadderUiState())
    val uiState: StateFlow<SnakeLadderUiState> = _uiState.asStateFlow()

    /**
     * @param playerIsCpu length 2–6; `true` = CPU, `false` = human. At least one human required.
     */
    fun startGame(playerIsCpu: List<Boolean>) {
        val n = playerIsCpu.size
        if (n !in 2..6 || playerIsCpu.none { !it }) return

        _uiState.value = SnakeLadderUiState(
            phase = SnakeLadderPhase.Playing,
            positions = List(n) { 0 },
            playerIsCpu = playerIsCpu,
            currentPlayerIndex = 0,
            lastDice = null,
            lastMessage = openingMessage(playerIsCpu),
            isRolling = false,
            winnerIndex = null
        )
        maybeScheduleCpuTurn()
    }

    private fun openingMessage(playerIsCpu: List<Boolean>): String {
        val soleHuman = playerIsCpu.withIndex().singleOrNull { !it.value }?.index
        return if (soleHuman != null) {
            "You are Player ${soleHuman + 1}. Roll to start."
        } else {
            "Player 1's turn. Humans take turns on this device when it's their go."
        }
    }

    fun backToMenu() {
        _uiState.value = SnakeLadderUiState()
    }

    fun playAgain() {
        val s = _uiState.value
        if (s.playerIsCpu.size >= 2) {
            startGame(s.playerIsCpu)
        } else {
            backToMenu()
        }
    }

    fun rollDice() {
        val s = _uiState.value
        if (s.phase != SnakeLadderPhase.Playing || s.isRolling) return
        if (s.currentIsCpu) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRolling = true, lastMessage = "Rolling…") }
            delay(420)
            val dice = random.nextInt(1, 7)
            applyDiceResult(dice)
        }
    }

    private fun maybeScheduleCpuTurn() {
        val s = _uiState.value
        if (s.phase != SnakeLadderPhase.Playing || s.winnerIndex != null) return
        if (!s.currentIsCpu || s.isRolling) return

        viewModelScope.launch {
            delay(650)
            val snapshot = _uiState.value
            if (snapshot.phase != SnakeLadderPhase.Playing || snapshot.winnerIndex != null) return@launch
            if (!snapshot.currentIsCpu || snapshot.isRolling) return@launch

            _uiState.update { it.copy(isRolling = true, lastMessage = cpuRollingMessage(snapshot)) }
            delay(480)
            val dice = random.nextInt(1, 7)
            applyDiceResult(dice)
        }
    }

    private fun cpuRollingMessage(state: SnakeLadderUiState): String {
        val n = state.currentPlayerIndex + 1
        return "Player $n (CPU) is rolling…"
    }

    private fun applyDiceResult(dice: Int) {
        _uiState.update { state ->
            if (state.phase != SnakeLadderPhase.Playing) return@update state

            val idx = state.currentPlayerIndex
            val from = state.positions[idx]
            val moved = SnakeLadderRules.moveAfterRoll(from, dice)

            val newPositions = state.positions.toMutableList()
            val name = playerLabel(state, idx)

            if (moved == null) {
                val msg = if (from == 0) {
                    "$name rolled $dice."
                } else {
                    "$name rolled $dice — need exact ${SnakeLadderRules.BOARD_SIZE - from} to finish, so no move."
                }
                val next = advancePlayer(state, idx)
                state.copy(
                    positions = newPositions,
                    lastDice = dice,
                    lastMessage = msg,
                    isRolling = false,
                    currentPlayerIndex = next
                )
            } else {
                newPositions[idx] = moved
                val reachedEnd = moved == SnakeLadderRules.BOARD_SIZE
                val rawLanding = if (from == 0) dice else from + dice
                val msg = buildString {
                    append("$name rolled $dice.")
                    if (from == 0) {
                        append(" Entered on $rawLanding")
                    } else {
                        append(" Landed on $rawLanding")
                    }
                    if (moved != rawLanding) {
                        append(" → slid or climbed to $moved")
                    }
                    append(".")
                }
                if (reachedEnd) {
                    state.copy(
                        positions = newPositions,
                        lastDice = dice,
                        lastMessage = "$name wins!",
                        isRolling = false,
                        phase = SnakeLadderPhase.Finished,
                        winnerIndex = idx,
                        currentPlayerIndex = idx
                    )
                } else {
                    val next = advancePlayer(state, idx)
                    state.copy(
                        positions = newPositions,
                        lastDice = dice,
                        lastMessage = msg,
                        isRolling = false,
                        currentPlayerIndex = next
                    )
                }
            }
        }

        val after = _uiState.value
        if (after.phase == SnakeLadderPhase.Playing && after.winnerIndex == null) {
            maybeScheduleCpuTurn()
        }
    }

    private fun playerLabel(state: SnakeLadderUiState, index: Int): String {
        val n = index + 1
        if (state.isCpuPlayer(index)) return "Player $n (CPU)"
        val soleHuman = state.playerIsCpu.withIndex().singleOrNull { !it.value }?.index
        if (soleHuman == index) return "You"
        return "Player $n"
    }

    private fun advancePlayer(state: SnakeLadderUiState, justPlayed: Int): Int {
        if (state.playerCount <= 1) return 0
        return (justPlayed + 1) % state.playerCount
    }
}
