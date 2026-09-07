package com.baraka.game.battleship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val GRID = 10

/** Standard fleet: carrier, battleship, cruiser, submarine, destroyer. */
internal val BattleShipFleetLengths = listOf(5, 4, 3, 3, 2)

enum class BattleShipGameMode {
    LocalTwoPlayer,
    VsComputer
}

enum class BattlePhase {
    Menu,
    SetupP1,
    HandoffAfterP1,
    SetupP2,
    HandoffAfterP2,
    Battle,
    GameOver
}

enum class ShotKind {
    MISS,
    HIT
}

data class BattleShipUiState(
    val phase: BattlePhase = BattlePhase.Menu,
    /** Set when a game mode is chosen; null on main menu. */
    val gameMode: BattleShipGameMode? = null,
    val player1Ships: List<Boolean> = List(GRID * GRID) { false },
    val player2Ships: List<Boolean> = List(GRID * GRID) { false },
    /** Player 1’s shots onto player 2’s grid. */
    val player1Shots: List<ShotKind?> = List(GRID * GRID) { null },
    /** Player 2’s shots onto player 1’s grid. */
    val player2Shots: List<ShotKind?> = List(GRID * GRID) { null },
    val setupShipIndex: Int = 0,
    val setupHorizontal: Boolean = true,
    /** In battle: 1 or 2 — who fires next. */
    val currentTurn: Int = 1,
    val lastMessage: String? = null,
    val winner: Int? = null,
    /** Vs CPU: true while the AI delay is running (UI can show “thinking…”). */
    val cpuThinking: Boolean = false
) {
    val currentSetupPlayer: Int
        get() = when (phase) {
            BattlePhase.SetupP1 -> 1
            BattlePhase.SetupP2 -> 2
            else -> 0
        }

    fun shipLengthToPlace(): Int? =
        if (phase != BattlePhase.SetupP1 && phase != BattlePhase.SetupP2) null
        else BattleShipFleetLengths.getOrNull(setupShipIndex)

    val isVsComputer: Boolean get() = gameMode == BattleShipGameMode.VsComputer

    /** Human is always player 1 in vs computer mode. */
    fun isHumanTurnInBattle(): Boolean =
        phase == BattlePhase.Battle && (gameMode != BattleShipGameMode.VsComputer || currentTurn == 1)
}

class BattleShipViewModel : ViewModel() {

    private val random = Random.Default
    private val cpuTargetQueue = ArrayDeque<Int>()

    private val _uiState = MutableStateFlow(BattleShipUiState())
    val uiState: StateFlow<BattleShipUiState> = _uiState.asStateFlow()

    fun startGame(mode: BattleShipGameMode) {
        resetCpuAi()
        _uiState.value = BattleShipUiState(
            phase = BattlePhase.SetupP1,
            gameMode = mode,
            lastMessage = "Place your fleet. Tap a cell for the ship’s start. Use Rotate if needed."
        )
    }

    fun backToMenu() {
        resetCpuAi()
        _uiState.value = BattleShipUiState()
    }

    fun playAgain() {
        val mode = _uiState.value.gameMode ?: BattleShipGameMode.LocalTwoPlayer
        resetCpuAi()
        _uiState.value = BattleShipUiState(
            phase = BattlePhase.SetupP1,
            gameMode = mode,
            lastMessage = "Place your fleet. Tap a cell for the ship’s start. Use Rotate if needed."
        )
    }

    private fun resetCpuAi() {
        cpuTargetQueue.clear()
    }

    fun toggleSetupOrientation() {
        _uiState.update { s ->
            if (s.phase != BattlePhase.SetupP1 && s.phase != BattlePhase.SetupP2) return@update s
            s.copy(setupHorizontal = !s.setupHorizontal)
        }
    }

    fun confirmHandoffAfterP1() {
        _uiState.update { s ->
            if (s.phase != BattlePhase.HandoffAfterP1) return@update s
            s.copy(
                phase = BattlePhase.SetupP2,
                setupShipIndex = 0,
                setupHorizontal = true,
                lastMessage = "Player 2: place your fleet (don’t peek at Player 1’s layout)."
            )
        }
    }

    fun confirmHandoffAfterP2() {
        _uiState.update { s ->
            if (s.phase != BattlePhase.HandoffAfterP2) return@update s
            s.copy(
                phase = BattlePhase.Battle,
                currentTurn = 1,
                cpuThinking = false,
                lastMessage = battleOpeningMessage(s)
            )
        }
    }

    private fun battleOpeningMessage(s: BattleShipUiState): String =
        when (s.gameMode) {
            BattleShipGameMode.VsComputer -> "Your turn — fire at the computer’s grid."
            BattleShipGameMode.LocalTwoPlayer, null -> "Battle! Player 1 — fire at the enemy grid below."
        }

    private fun finishP1Setup() {
        _uiState.update { s ->
            when (s.gameMode) {
                BattleShipGameMode.VsComputer -> {
                    val cpuBoard = randomFleetBoard()
                    s.copy(
                        player2Ships = cpuBoard,
                        phase = BattlePhase.HandoffAfterP2,
                        setupShipIndex = 0,
                        setupHorizontal = true,
                        lastMessage = "The computer placed its fleet. Tap Begin battle when you’re ready."
                    )
                }
                BattleShipGameMode.LocalTwoPlayer, null -> {
                    s.copy(
                        phase = BattlePhase.HandoffAfterP1,
                        setupShipIndex = 0,
                        setupHorizontal = true,
                        lastMessage = "Player 1 is ready. Pass the device to Player 2 and tap Continue."
                    )
                }
            }
        }
    }

    private fun finishP2Setup() {
        _uiState.update { s ->
            s.copy(
                phase = BattlePhase.HandoffAfterP2,
                setupHorizontal = true,
                lastMessage = "Both fleets ready. Pass the device so Player 1 starts. Tap Begin battle."
            )
        }
    }

    private fun randomFleetBoard(): List<Boolean> {
        repeat(800) {
            val board = MutableList(GRID * GRID) { false }
            var ok = true
            for (len in BattleShipFleetLengths) {
                var placed = false
                repeat(300) {
                    val horizontal = random.nextBoolean()
                    val row = random.nextInt(GRID)
                    val col = random.nextInt(GRID)
                    if (canPlace(board, row, col, len, horizontal)) {
                        placeShip(board, row, col, len, horizontal)
                        placed = true
                        return@repeat
                    }
                }
                if (!placed) {
                    ok = false
                    break
                }
            }
            if (ok) return board
        }
        return List(GRID * GRID) { false }
    }

    fun tryPlaceShip(cellIndex: Int) {
        _uiState.update { s ->
            if (s.phase != BattlePhase.SetupP1 && s.phase != BattlePhase.SetupP2) return@update s
            val len = BattleShipFleetLengths.getOrNull(s.setupShipIndex) ?: return@update s
            val board = when (s.phase) {
                BattlePhase.SetupP1 -> s.player1Ships.toMutableList()
                BattlePhase.SetupP2 -> s.player2Ships.toMutableList()
                else -> return@update s
            }
            val row = cellIndex / GRID
            val col = cellIndex % GRID
            if (!canPlace(board, row, col, len, s.setupHorizontal)) {
                return@update s.copy(lastMessage = "Can’t place there — overlap or out of bounds.")
            }
            placeShip(board, row, col, len, s.setupHorizontal)
            val nextIndex = s.setupShipIndex + 1
            when (s.phase) {
                BattlePhase.SetupP1 -> s.copy(
                    player1Ships = board,
                    setupShipIndex = nextIndex,
                    lastMessage = if (nextIndex >= BattleShipFleetLengths.size) null else "Place next ship (${BattleShipFleetLengths[nextIndex]} cells)."
                )
                BattlePhase.SetupP2 -> s.copy(
                    player2Ships = board,
                    setupShipIndex = nextIndex,
                    lastMessage = if (nextIndex >= BattleShipFleetLengths.size) null else "Place next ship (${BattleShipFleetLengths[nextIndex]} cells)."
                )
                else -> s
            }
        }
        val st = _uiState.value
        if (st.phase == BattlePhase.SetupP1 && st.setupShipIndex >= BattleShipFleetLengths.size) {
            finishP1Setup()
        } else if (st.phase == BattlePhase.SetupP2 && st.setupShipIndex >= BattleShipFleetLengths.size) {
            finishP2Setup()
        }
    }

    fun fire(cellIndex: Int) {
        val before = _uiState.value
        if (before.phase != BattlePhase.Battle || before.winner != null) return
        if (before.gameMode == BattleShipGameMode.VsComputer && before.currentTurn != 1) return
        if (before.cpuThinking) return

        val after = applyShooterTurn(before, 1, cellIndex)
        _uiState.value = after
        if (after.phase == BattlePhase.GameOver || after.currentTurn != 2) return
        if (after.gameMode == BattleShipGameMode.VsComputer) {
            scheduleCpuTurn()
        }
    }

    private fun scheduleCpuTurn() {
        viewModelScope.launch {
            _uiState.update { it.copy(cpuThinking = true, lastMessage = "Computer is thinking…") }
            delay(550)
            val s = _uiState.value
            if (s.phase != BattlePhase.Battle || s.currentTurn != 2 || s.gameMode != BattleShipGameMode.VsComputer) {
                _uiState.update { it.copy(cpuThinking = false) }
                return@launch
            }
            val cell = pickCpuCell(s)
            val hit = s.player1Ships[cell]
            val next = applyShooterTurn(s, 2, cell)
            if (hit) {
                enqueueCpuNeighbors(cell)
            }
            _uiState.value = next.copy(
                cpuThinking = false,
                lastMessage = cpuShotMessage(hit, next.phase == BattlePhase.GameOver, next.winner)
            )
        }
    }

    private fun cpuShotMessage(hit: Boolean, gameOver: Boolean, winner: Int?): String {
        if (gameOver && winner == 2) return "Computer wins — your fleet is destroyed!"
        return if (hit) "Computer hit your ship! Your turn." else "Computer missed. Your turn."
    }

    private fun pickCpuCell(s: BattleShipUiState): Int {
        while (cpuTargetQueue.isNotEmpty()) {
            val c = cpuTargetQueue.removeFirst()
            if (c in 0 until GRID * GRID && s.player2Shots[c] == null) return c
        }
        val pool = buildList {
            for (i in 0 until GRID * GRID) {
                if (s.player2Shots[i] == null) add(i)
            }
        }
        return if (pool.isEmpty()) 0 else pool.random(random)
    }

    private fun enqueueCpuNeighbors(index: Int) {
        val row = index / GRID
        val col = index % GRID
        val deltas = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in deltas) {
            val r = row + dr
            val c = col + dc
            if (r in 0 until GRID && c in 0 until GRID) {
                cpuTargetQueue.addLast(r * GRID + c)
            }
        }
    }

    private fun applyShooterTurn(s: BattleShipUiState, shooter: Int, cellIndex: Int): BattleShipUiState {
        return when (shooter) {
            1 -> {
                if (s.player1Shots[cellIndex] != null) {
                    return s.copy(lastMessage = "Already fired there.")
                }
                val hit = s.player2Ships[cellIndex]
                val shot = if (hit) ShotKind.HIT else ShotKind.MISS
                val newShots = s.player1Shots.toMutableList().also { it[cellIndex] = shot }
                if (allShipsDestroyed(s.player2Ships, newShots)) {
                    return s.copy(
                        player1Shots = newShots,
                        phase = BattlePhase.GameOver,
                        winner = 1,
                        cpuThinking = false,
                        lastMessage = winMessage(s, winnerPlayer = 1)
                    )
                }
                val msg = when (s.gameMode) {
                    BattleShipGameMode.VsComputer ->
                        if (hit) "Hit! Computer’s turn." else "Miss. Computer’s turn."
                    BattleShipGameMode.LocalTwoPlayer, null ->
                        if (hit) "Hit! Pass to Player 2." else "Miss. Pass to Player 2."
                }
                s.copy(
                    player1Shots = newShots,
                    currentTurn = 2,
                    lastMessage = msg
                )
            }
            2 -> {
                if (s.player2Shots[cellIndex] != null) {
                    return s.copy(lastMessage = "Already fired there.")
                }
                val hit = s.player1Ships[cellIndex]
                val shot = if (hit) ShotKind.HIT else ShotKind.MISS
                val newShots = s.player2Shots.toMutableList().also { it[cellIndex] = shot }
                if (allShipsDestroyed(s.player1Ships, newShots)) {
                    return s.copy(
                        player2Shots = newShots,
                        phase = BattlePhase.GameOver,
                        winner = 2,
                        cpuThinking = false,
                        lastMessage = winMessage(s, winnerPlayer = 2)
                    )
                }
                val msg = when (s.gameMode) {
                    BattleShipGameMode.VsComputer -> ""
                    BattleShipGameMode.LocalTwoPlayer, null ->
                        if (hit) "Hit! Pass to Player 1." else "Miss. Pass to Player 1."
                }
                s.copy(
                    player2Shots = newShots,
                    currentTurn = 1,
                    lastMessage = msg
                )
            }
            else -> s
        }
    }

    private fun winMessage(s: BattleShipUiState, winnerPlayer: Int): String {
        if (s.gameMode == BattleShipGameMode.VsComputer) {
            return if (winnerPlayer == 1) "You win — all computer ships destroyed!"
            else "Computer wins — your fleet is destroyed!"
        }
        return "Player $winnerPlayer wins — all enemy ships destroyed!"
    }

    companion object {
        fun canPlace(board: List<Boolean>, row: Int, col: Int, len: Int, horizontal: Boolean): Boolean {
            if (horizontal) {
                if (col + len > GRID) return false
                for (c in col until col + len) {
                    if (board[row * GRID + c]) return false
                }
            } else {
                if (row + len > GRID) return false
                for (r in row until row + len) {
                    if (board[r * GRID + col]) return false
                }
            }
            return true
        }

        fun placeShip(board: MutableList<Boolean>, row: Int, col: Int, len: Int, horizontal: Boolean) {
            if (horizontal) {
                for (c in col until col + len) board[row * GRID + c] = true
            } else {
                for (r in row until row + len) board[r * GRID + col] = true
            }
        }

        fun allShipsDestroyed(ships: List<Boolean>, shots: List<ShotKind?>): Boolean {
            return ships.indices.all { i -> !ships[i] || shots[i] == ShotKind.HIT }
        }
    }
}
