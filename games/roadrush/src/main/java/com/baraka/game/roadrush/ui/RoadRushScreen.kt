package com.baraka.game.roadrush.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baraka.game.roadrush.data.RoadRushSettingsRepository
import com.baraka.game.roadrush.data.RoadRushSoundEngine
import com.baraka.game.roadrush.data.RoadSoundCue
import com.baraka.game.roadrush.model.CarDesign
import com.baraka.game.roadrush.model.Difficulty
import com.baraka.game.roadrush.model.GamePhase
import com.baraka.game.roadrush.model.RoadState
import com.baraka.game.roadrush.render.RoadRenderer
import com.baraka.game.roadrush.systems.RoadGameLoop
import com.baraka.game.ui.components.SoundSettingsOverlay
import kotlinx.coroutines.delay

private val BACKGROUND_COLOR = Color(0xFF0C0D12)
private const val TICK_MS = 16L

/**
 * Owns the run's [RoadState] and its ~60fps loop, same shape as numbercorridor's
 * NumberCorridorScreen: this file wires input and persistence, systems/RoadGameLoop.kt makes
 * every gameplay decision. The only input is the two steering buttons; their held/released
 * state ([isLeftHeld]/[isRightHeld]) is sampled every tick into a -1/0/1 `steerDirection` so
 * the car moves continuously while a side is held, not lane-stepping per tap.
 */
@Composable
fun RoadRushScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { RoadRushSettingsRepository.getInstance(context) }

    var carDesign by remember { mutableStateOf(settingsRepository.carDesign()) }
    var difficulty by remember { mutableStateOf(settingsRepository.difficulty()) }
    var showCarPicker by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    var roadState by remember { mutableStateOf(RoadState(difficulty = difficulty, bestScoreAtStart = settingsRepository.bestScore())) }
    var hasRecordedThisRun by remember { mutableStateOf(false) }
    var isLeftHeld by remember { mutableStateOf(false) }
    var isRightHeld by remember { mutableStateOf(false) }
    val soundEngine = remember { RoadRushSoundEngine() }
    DisposableEffect(Unit) { onDispose { soundEngine.release() } }
    fun playSound(cue: RoadSoundCue) {
        if (soundEnabled) soundEngine.play(cue)
    }

    val isRunning = roadState.phase == GamePhase.RUNNING && !showCarPicker && !showSettings

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(TICK_MS)
            val steerDirection = (if (isRightHeld) 1f else 0f) - (if (isLeftHeld) 1f else 0f)
            val previousLives = roadState.lives
            val next = RoadGameLoop.tick(roadState, TICK_MS / 1000f, steerDirection)
            if (next.lives < previousLives) playSound(RoadSoundCue.CRASH)
            if (next.carsDodgedThisTick > 0) playSound(RoadSoundCue.DODGE)
            roadState = next
        }
    }

    LaunchedEffect(roadState.phase) {
        if (roadState.phase == GamePhase.GAME_OVER && !hasRecordedThisRun) {
            playSound(RoadSoundCue.GAME_OVER)
            if (roadState.score > settingsRepository.bestScore()) {
                settingsRepository.setBestScore(roadState.score)
            }
            hasRecordedThisRun = true
        }
    }

    if (showCarPicker) {
        CarPickerOverlay(
            selected = carDesign,
            onSelected = { design ->
                carDesign = design
                settingsRepository.setCarDesign(design)
            },
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
            onStart = {
                roadState = RoadState(difficulty = difficulty, bestScoreAtStart = settingsRepository.bestScore())
                hasRecordedThisRun = false
                showCarPicker = false
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
        RoadRushHud(
            lives = roadState.lives,
            score = roadState.score,
            bestScore = settingsRepository.bestScore(),
            onSettingsClick = { showSettings = true },
            modifier = Modifier.padding(12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            RoadRenderer(
                state = roadState,
                carDesign = carDesign,
                onSizeMeasured = { _, _ -> }
            )

            if (roadState.phase == GamePhase.GAME_OVER) {
                RoadRushEndScreen(
                    score = roadState.score,
                    isNewBest = roadState.isNewBest,
                    onPlayAgain = { showCarPicker = true }
                )
            }

            if (showSettings) {
                SoundSettingsOverlay(
                    soundEnabled = soundEnabled,
                    onSoundEnabledChanged = { enabled ->
                        soundEnabled = enabled
                        settingsRepository.setSoundEnabled(enabled)
                    },
                    onClose = { showSettings = false }
                )
            }
        }

        SteeringControls(
            onLeftHeldChanged = { held ->
                isLeftHeld = held
                if (held) playSound(RoadSoundCue.SWERVE)
            },
            onRightHeldChanged = { held ->
                isRightHeld = held
                if (held) playSound(RoadSoundCue.SWERVE)
            }
        )
    }
}
