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

enum class SpecialMoveType { LADDER, SNAKE }

/** Transient event describing an in-progress ladder climb or snake slide, for UI animation/sound. */
data class SpecialMoveEvent(
    val playerIndex: Int,
    val type: SpecialMoveType,
    val from: Int,
    val to: Int
)

data class SnakeLadderUiState(
    val phase: SnakeLadderPhase = SnakeLadderPhase.Menu,
    val positions: List<Int> = emptyList(),
    /** Same length as [positions] while playing: `true` = CPU, `false` = human. */
    val playerIsCpu: List<Boolean> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val lastDice: Int? = null,
    val lastMessage: String? = null,
    val isRolling: Boolean = false,
    val winnerIndex: Int? = null,
    val specialMove: SpecialMoveEvent? = null,
    /** Same length as [positions]: the emoji avatar each player picked in the menu. */
    val playerAvatars: List<String> = emptyList()
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

/** Fallback avatars if the menu doesn't supply one per player (kept in sync with the picker in SnakeLadderScreen). */
private val DefaultAvatars = listOf("🦁", "🐵", "🐸", "🐶", "🐱", "🦊", "🐼", "🦄", "🐷", "🐙", "🐰", "🦕")

class SnakeLadderViewModel : ViewModel() {

    private val random = Random.Default
    private val _uiState = MutableStateFlow(SnakeLadderUiState())
    val uiState: StateFlow<SnakeLadderUiState> = _uiState.asStateFlow()

    /**
     * @param playerIsCpu length 2–6; `true` = CPU, `false` = human. At least one human required.
     * @param playerAvatars emoji avatar per player, same length as [playerIsCpu].
     */
    fun startGame(playerIsCpu: List<Boolean>, playerAvatars: List<String> = emptyList()) {
        val n = playerIsCpu.size
        if (n !in 2..6 || playerIsCpu.none { !it }) return

        val avatars = List(n) { i -> playerAvatars.getOrElse(i) { DefaultAvatars[i % DefaultAvatars.size] } }

        _uiState.value = SnakeLadderUiState(
            phase = SnakeLadderPhase.Playing,
            positions = List(n) { 0 },
            playerIsCpu = playerIsCpu,
            currentPlayerIndex = 0,
            lastDice = null,
            lastMessage = openingMessage(playerIsCpu),
            isRolling = false,
            winnerIndex = null,
            playerAvatars = avatars
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
            startGame(s.playerIsCpu, s.playerAvatars)
        } else {
            backToMenu()
        }
    }

    fun rollDice() {
        val s = _uiState.value
        if (s.phase != SnakeLadderPhase.Playing || s.isRolling) return
        if (s.currentIsCpu) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRolling = true, lastDice = null, lastMessage = "Rolling…") }
            delay(500)
            val dice = random.nextInt(1, 7)
            animateAndResolve(dice)
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

            _uiState.update { it.copy(isRolling = true, lastDice = null, lastMessage = cpuRollingMessage(snapshot)) }
            delay(550)
            val dice = random.nextInt(1, 7)
            animateAndResolve(dice)
        }
    }

    private fun cpuRollingMessage(state: SnakeLadderUiState): String {
        val n = state.currentPlayerIndex + 1
        return "Player $n (CPU) is rolling…"
    }

    private suspend fun animateAndResolve(dice: Int) {
        val state0 = _uiState.value
        if (state0.phase != SnakeLadderPhase.Playing) return

        val idx = state0.currentPlayerIndex
        val from = state0.positions[idx]
        val moved = SnakeLadderRules.moveAfterRoll(from, dice)
        val name = playerLabel(state0, idx)

        _uiState.update { it.copy(lastDice = dice) }

        if (moved == null) {
            val msg = if (from == 0) {
                "$name rolled $dice."
            } else {
                "$name rolled $dice — need exact ${SnakeLadderRules.BOARD_SIZE - from} to finish, so no move."
            }
            delay(250)
            val next = advancePlayer(state0, idx)
            _uiState.update { it.copy(lastMessage = msg, isRolling = false, currentPlayerIndex = next) }
        } else {
            val rawLanding = if (from == 0) dice else from + dice
            val start = if (from == 0) 1 else from + 1
            for (step in start..rawLanding) {
                _uiState.update { st ->
                    val positions = st.positions.toMutableList()
                    positions[idx] = step
                    st.copy(positions = positions)
                }
                delay(90)
            }

            if (moved != rawLanding) {
                val type = if (SnakeLadderRules.ladders.containsKey(rawLanding)) {
                    SpecialMoveType.LADDER
                } else {
                    SpecialMoveType.SNAKE
                }
                delay(200)
                _uiState.update { it.copy(specialMove = SpecialMoveEvent(idx, type, rawLanding, moved)) }
                delay(350)
                _uiState.update { st ->
                    val positions = st.positions.toMutableList()
                    positions[idx] = moved
                    st.copy(positions = positions)
                }
                delay(250)
                _uiState.update { it.copy(specialMove = null) }
            }

            val msg = buildString {
                append("$name rolled $dice.")
                if (from == 0) append(" Entered on $rawLanding") else append(" Landed on $rawLanding")
                if (moved != rawLanding) append(" → slid or climbed to $moved")
                append(".")
            }

            if (moved == SnakeLadderRules.BOARD_SIZE) {
                _uiState.update {
                    it.copy(
                        lastMessage = "$name wins!",
                        isRolling = false,
                        phase = SnakeLadderPhase.Finished,
                        winnerIndex = idx,
                        currentPlayerIndex = idx
                    )
                }
            } else {
                val next = advancePlayer(state0, idx)
                _uiState.update { it.copy(lastMessage = msg, isRolling = false, currentPlayerIndex = next) }
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
