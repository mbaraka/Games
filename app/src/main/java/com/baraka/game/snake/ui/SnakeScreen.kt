package com.baraka.game.snake.ui

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baraka.game.roadrush.ui.RoadRushSettingsOverlay
import com.baraka.game.snake.data.SnakeSettingsRepository
import com.baraka.game.snake.data.SnakeSoundCue
import com.baraka.game.snake.data.SnakeSoundEngine
import com.baraka.game.snake.model.Direction
import com.baraka.game.snake.model.GamePhase
import com.baraka.game.snake.render.SnakeRenderer
import com.baraka.game.snake.systems.SnakeGameLoop
import kotlinx.coroutines.delay
import kotlin.random.Random

private val BACKGROUND_COLOR = Color(0xFF0C0D12)
private const val TICK_MS = 16L

/**
 * Owns the run's [com.baraka.game.snake.model.SnakeState] and its ~60fps loop, same shape as
 * the other Atari-style games' screens: this file wires input and persistence,
 * systems/SnakeGameLoop.kt makes every gameplay decision (including the actual grid-step
 * cadence). Swiping in any of the four cardinal directions buffers a turn; the drag
 * accumulator resets on whichever axis fires so a long diagonal-ish swipe can't double-queue.
 */
@Composable
fun SnakeScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SnakeSettingsRepository.getInstance(context) }
    val rng = remember { Random(System.nanoTime()) }

    var difficulty by remember { mutableStateOf(settingsRepository.difficulty()) }
    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    var showStartOverlay by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var gameState by remember {
        mutableStateOf(SnakeGameLoop.newGame(difficulty, settingsRepository.bestScore(), rng))
    }
    var hasRecordedThisRun by remember { mutableStateOf(false) }

    val soundEngine = remember { SnakeSoundEngine() }
    DisposableEffect(Unit) { onDispose { soundEngine.release() } }
    fun playSound(cue: SnakeSoundCue) {
        if (soundEnabled) soundEngine.play(cue)
    }

    val looping = !showStartOverlay && !showSettings && gameState.phase == GamePhase.RUNNING

    LaunchedEffect(looping) {
        while (looping) {
            delay(TICK_MS)
            val next = SnakeGameLoop.tick(gameState, TICK_MS / 1000f, rng)
            if (next.foodEatenThisStep) playSound(SnakeSoundCue.EAT)
            gameState = next
        }
    }

    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.GAME_OVER && !hasRecordedThisRun) {
            playSound(SnakeSoundCue.GAME_OVER)
            if (gameState.score > settingsRepository.bestScore()) {
                settingsRepository.setBestScore(gameState.score)
            }
            hasRecordedThisRun = true
        }
    }

    fun startNewRun() {
        gameState = SnakeGameLoop.newGame(difficulty, settingsRepository.bestScore(), rng)
        hasRecordedThisRun = false
        showStartOverlay = false
    }

    if (showStartOverlay) {
        SnakeStartOverlay(
            difficulty = difficulty,
            onDifficultyChanged = { chosen ->
                difficulty = chosen
                settingsRepository.setDifficulty(chosen)
            },
            soundEnabled = soundEnabled,
            onSoundEnabledChanged = { enabled ->
                soundEnabled = enabled
                settingsRepository.setSoundEnabled(enabled)
            },
            bestScore = settingsRepository.bestScore(),
            onStart = { startNewRun() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        SnakeHud(
            score = gameState.score,
            bestScore = settingsRepository.bestScore(),
            onSettingsClick = { showSettings = true },
            modifier = Modifier.padding(12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    var dx = 0f
                    var dy = 0f
                    val thresholdPx = 24.dp.toPx()
                    detectDragGestures(
                        onDragEnd = { dx = 0f; dy = 0f },
                        onDragCancel = { dx = 0f; dy = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        dx += dragAmount.x
                        dy += dragAmount.y
                        if (kotlin.math.abs(dx) > thresholdPx || kotlin.math.abs(dy) > thresholdPx) {
                            val direction = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                if (dx > 0) Direction.RIGHT else Direction.LEFT
                            } else {
                                if (dy > 0) Direction.DOWN else Direction.UP
                            }
                            gameState = SnakeGameLoop.setDirection(gameState, direction)
                            dx = 0f
                            dy = 0f
                        }
                    }
                }
        ) {
            SnakeRenderer(state = gameState)

            if (gameState.phase == GamePhase.GAME_OVER) {
                SnakeEndScreen(
                    score = gameState.score,
                    isNewBest = gameState.isNewBest,
                    onPlayAgain = { showStartOverlay = true }
                )
            }

            if (showSettings) {
                RoadRushSettingsOverlay(
                    soundEnabled = soundEnabled,
                    onSoundEnabledChanged = { enabled ->
                        soundEnabled = enabled
                        settingsRepository.setSoundEnabled(enabled)
                    },
                    onClose = { showSettings = false }
                )
            }
        }
    }
}
