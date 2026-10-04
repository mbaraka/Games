package com.baraka.game.breakout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.baraka.game.breakout.data.BreakoutSettingsRepository
import com.baraka.game.breakout.data.BreakoutSoundCue
import com.baraka.game.breakout.data.BreakoutSoundEngine
import com.baraka.game.breakout.model.BreakoutState
import com.baraka.game.breakout.model.GamePhase
import com.baraka.game.breakout.render.BreakoutRenderer
import com.baraka.game.breakout.systems.BreakoutGameLoop
import com.baraka.game.roadrush.ui.RoadRushSettingsOverlay
import kotlinx.coroutines.delay
import kotlin.random.Random

private val BACKGROUND_COLOR = Color(0xFF0C0D12)
private const val TICK_MS = 16L

/**
 * Owns the run's [BreakoutState] and its ~60fps loop, same shape as roadrush's RoadRushScreen:
 * this file wires input and persistence, systems/BreakoutGameLoop.kt makes every gameplay
 * decision. Dragging anywhere in the play area moves the paddle to that x position; tapping
 * launches the ball when it's resting on the paddle ([GamePhase.AIMING]).
 */
@Composable
fun BreakoutScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { BreakoutSettingsRepository.getInstance(context) }
    val rng = remember { Random(System.nanoTime()) }

    var difficulty by remember { mutableStateOf(settingsRepository.difficulty()) }
    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    var showStartOverlay by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var breakoutState by remember {
        mutableStateOf(BreakoutGameLoop.newGame(difficulty, settingsRepository.bestScore()))
    }
    var hasRecordedThisRun by remember { mutableStateOf(false) }

    val soundEngine = remember { BreakoutSoundEngine() }
    DisposableEffect(Unit) { onDispose { soundEngine.release() } }
    fun playSound(cue: BreakoutSoundCue) {
        if (soundEnabled) soundEngine.play(cue)
    }

    val looping = !showStartOverlay && !showSettings && breakoutState.phase != GamePhase.GAME_OVER

    LaunchedEffect(looping) {
        while (looping) {
            delay(TICK_MS)
            val next = BreakoutGameLoop.tick(breakoutState, TICK_MS / 1000f, rng)
            if (next.wallBounceThisTick) playSound(BreakoutSoundCue.WALL_BOUNCE)
            if (next.paddleBounceThisTick) playSound(BreakoutSoundCue.PADDLE_BOUNCE)
            if (next.bricksBrokenThisTick > 0) playSound(BreakoutSoundCue.BRICK_BREAK)
            if (next.powerUpCollectedThisTick) playSound(BreakoutSoundCue.POWER_UP)
            if (next.ballLostThisTick) playSound(BreakoutSoundCue.BALL_LOST)
            if (next.phase == GamePhase.LEVEL_CLEARED && breakoutState.phase != GamePhase.LEVEL_CLEARED) {
                playSound(BreakoutSoundCue.LEVEL_CLEAR)
            }
            breakoutState = next
        }
    }

    LaunchedEffect(breakoutState.phase) {
        if (breakoutState.phase == GamePhase.GAME_OVER && !hasRecordedThisRun) {
            playSound(BreakoutSoundCue.GAME_OVER)
            if (breakoutState.score > settingsRepository.bestScore()) {
                settingsRepository.setBestScore(breakoutState.score)
            }
            hasRecordedThisRun = true
        }
    }

    fun startNewRun() {
        breakoutState = BreakoutGameLoop.newGame(difficulty, settingsRepository.bestScore())
        hasRecordedThisRun = false
        showStartOverlay = false
    }

    if (showStartOverlay) {
        BreakoutStartOverlay(
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
        BreakoutHud(
            lives = breakoutState.lives,
            score = breakoutState.score,
            level = breakoutState.level,
            bestScore = settingsRepository.bestScore(),
            onSettingsClick = { showSettings = true },
            modifier = Modifier.padding(12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        if (breakoutState.phase == GamePhase.AIMING) {
                            breakoutState = BreakoutGameLoop.launch(breakoutState, rng)
                        }
                    })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            breakoutState = BreakoutGameLoop.setPaddleX(breakoutState, offset.x / size.width)
                        }
                    ) { change, _ ->
                        change.consume()
                        breakoutState = BreakoutGameLoop.setPaddleX(breakoutState, change.position.x / size.width)
                    }
                }
        ) {
            BreakoutRenderer(state = breakoutState)

            if (breakoutState.phase == GamePhase.LEVEL_CLEARED) {
                BreakoutLevelClearedOverlay(
                    level = breakoutState.level,
                    score = breakoutState.score,
                    onNextLevel = { breakoutState = BreakoutGameLoop.startLevel(breakoutState, breakoutState.level + 1) }
                )
            }

            if (breakoutState.phase == GamePhase.GAME_OVER) {
                BreakoutEndScreen(
                    score = breakoutState.score,
                    level = breakoutState.level,
                    isNewBest = breakoutState.isNewBest,
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
