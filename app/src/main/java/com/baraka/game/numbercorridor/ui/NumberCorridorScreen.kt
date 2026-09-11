package com.baraka.game.numbercorridor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baraka.game.numbercorridor.data.CorridorSettingsRepository
import com.baraka.game.numbercorridor.model.AvatarDesign
import com.baraka.game.numbercorridor.model.GamePhase
import com.baraka.game.numbercorridor.model.GameState
import com.baraka.game.numbercorridor.model.WallRow
import com.baraka.game.numbercorridor.render.CorridorRenderer
import com.baraka.game.numbercorridor.systems.GameLoopOrchestrator
import kotlinx.coroutines.delay

private val BACKGROUND_COLOR = Color(0xFF0C0D12)
private const val TICK_MS = 16L

/**
 * Owns the run's [GameState] and its ~60fps loop, same shape as knightsurvivor's
 * KnightSurvivorScreen: this file wires input and persistence, systems/GameLoopOrchestrator.kt
 * makes every gameplay decision. The only input is horizontal drag, converted to a target
 * lane and handed to [GameLoopOrchestrator.moveSquad].
 */
@Composable
fun NumberCorridorScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { CorridorSettingsRepository.getInstance(context) }

    var avatarDesign by remember { mutableStateOf(settingsRepository.avatarDesign()) }
    var showAvatarPicker by remember { mutableStateOf(true) }
    var gameState by remember { mutableStateOf(GameState(avatarDesign = avatarDesign, bestScoreAtStart = settingsRepository.bestScore())) }
    var hasRecordedThisRun by remember { mutableStateOf(false) }
    var canvasWidthPx by remember { mutableStateOf(0f) }

    val isRunning = gameState.phase == GamePhase.RUNNING && !showAvatarPicker

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(TICK_MS)
            gameState = GameLoopOrchestrator.tick(gameState, TICK_MS / 1000f)
        }
    }

    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.GAME_OVER && !hasRecordedThisRun) {
            if (gameState.score > settingsRepository.bestScore()) {
                settingsRepository.setBestScore(gameState.score)
            }
            hasRecordedThisRun = true
        }
    }

    if (showAvatarPicker) {
        AvatarPickerOverlay(
            selected = avatarDesign,
            onSelected = { design ->
                avatarDesign = design
                settingsRepository.setAvatarDesign(design)
            },
            onStart = {
                gameState = GameState(avatarDesign = avatarDesign, bestScoreAtStart = settingsRepository.bestScore())
                hasRecordedThisRun = false
                showAvatarPicker = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        CorridorHud(
            player = gameState.player,
            score = gameState.score,
            bestScore = settingsRepository.bestScore(),
            modifier = Modifier.padding(12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(canvasWidthPx) {
                    if (canvasWidthPx <= 0f) return@pointerInput
                    val corridorWidth = canvasWidthPx * 0.94f
                    val laneWidth = corridorWidth / WallRow.LANE_COUNT
                    // Accumulates fractional lanes dragged since the last whole-lane move — a single
                    // onDrag callback's delta is usually much smaller than one lane, so without this
                    // running total every event gets truncated to zero and the squad never moves.
                    var draggedLanes = 0f
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        draggedLanes += dragAmount.x / laneWidth
                        val wholeLanes = draggedLanes.toInt()
                        if (wholeLanes != 0) {
                            draggedLanes -= wholeLanes
                            gameState = GameLoopOrchestrator.moveSquad(gameState, gameState.player.laneStart + wholeLanes)
                        }
                    }
                }
        ) {
            CorridorRenderer(
                state = gameState,
                onSizeMeasured = { width, _ -> canvasWidthPx = width }
            )

            if (gameState.phase == GamePhase.GAME_OVER) {
                CorridorEndScreen(
                    score = gameState.score,
                    isNewBest = gameState.isNewBest,
                    onPlayAgain = { showAvatarPicker = true }
                )
            }
        }
    }
}
