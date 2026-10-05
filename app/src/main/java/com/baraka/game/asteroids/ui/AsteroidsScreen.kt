package com.baraka.game.asteroids.ui

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
import com.baraka.game.asteroids.data.AsteroidsSettingsRepository
import com.baraka.game.asteroids.data.AsteroidsSoundCue
import com.baraka.game.asteroids.data.AsteroidsSoundEngine
import com.baraka.game.asteroids.model.GamePhase
import com.baraka.game.asteroids.render.AsteroidsRenderer
import com.baraka.game.asteroids.systems.AsteroidsLoop
import com.baraka.game.roadrush.ui.RoadRushSettingsOverlay
import kotlinx.coroutines.delay
import kotlin.random.Random

private val BACKGROUND_COLOR = Color(0xFF05060A)
private const val TICK_MS = 16L

/**
 * Owns the run's [com.baraka.game.asteroids.model.AsteroidsState] and its ~60fps loop, same
 * shape as the other Atari-style games' screens: this file wires input and persistence,
 * systems/AsteroidsLoop.kt makes every gameplay decision. Rotate/thrust are continuous
 * button-held flags fed into every tick; fire is a one-shot action — see
 * ui/AsteroidsControls.kt.
 */
@Composable
fun AsteroidsScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { AsteroidsSettingsRepository.getInstance(context) }
    val rng = remember { Random(System.nanoTime()) }

    var difficulty by remember { mutableStateOf(settingsRepository.difficulty()) }
    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    var showStartOverlay by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var gameState by remember {
        mutableStateOf(AsteroidsLoop.newGame(difficulty, settingsRepository.bestScore(), rng))
    }
    var hasRecordedThisRun by remember { mutableStateOf(false) }

    var rotatingLeft by remember { mutableStateOf(false) }
    var rotatingRight by remember { mutableStateOf(false) }
    var thrusting by remember { mutableStateOf(false) }

    val soundEngine = remember { AsteroidsSoundEngine() }
    DisposableEffect(Unit) { onDispose { soundEngine.release() } }
    fun playSound(cue: AsteroidsSoundCue) {
        if (soundEnabled) soundEngine.play(cue)
    }

    val looping = !showStartOverlay && !showSettings && gameState.phase == GamePhase.RUNNING

    LaunchedEffect(looping) {
        while (looping) {
            delay(TICK_MS)
            val next = AsteroidsLoop.tick(gameState, TICK_MS / 1000f, rotatingLeft, rotatingRight, thrusting, rng)
            if (next.asteroidsDestroyedThisTick > 0) playSound(AsteroidsSoundCue.ASTEROID_HIT)
            if (next.shipDestroyedThisTick) playSound(AsteroidsSoundCue.SHIP_DESTROYED)
            if (next.phase == GamePhase.WAVE_CLEARED && gameState.phase != GamePhase.WAVE_CLEARED) {
                playSound(AsteroidsSoundCue.WAVE_CLEAR)
            }
            gameState = next
        }
    }

    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.GAME_OVER && !hasRecordedThisRun) {
            playSound(AsteroidsSoundCue.GAME_OVER)
            if (gameState.score > settingsRepository.bestScore()) {
                settingsRepository.setBestScore(gameState.score)
            }
            hasRecordedThisRun = true
        }
    }

    fun startNewRun() {
        gameState = AsteroidsLoop.newGame(difficulty, settingsRepository.bestScore(), rng)
        hasRecordedThisRun = false
        showStartOverlay = false
    }

    if (showStartOverlay) {
        AsteroidsStartOverlay(
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
        AsteroidsHud(
            lives = gameState.lives,
            score = gameState.score,
            wave = gameState.wave,
            bestScore = settingsRepository.bestScore(),
            onSettingsClick = { showSettings = true },
            modifier = Modifier.padding(12.dp)
        )

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            AsteroidsRenderer(state = gameState, thrusting = thrusting && gameState.phase == GamePhase.RUNNING)

            if (gameState.phase == GamePhase.WAVE_CLEARED) {
                AsteroidsWaveClearedOverlay(
                    wave = gameState.wave,
                    score = gameState.score,
                    onNextWave = { gameState = AsteroidsLoop.startWave(gameState, gameState.wave + 1, rng) }
                )
            }

            if (gameState.phase == GamePhase.GAME_OVER) {
                AsteroidsEndScreen(
                    score = gameState.score,
                    wave = gameState.wave,
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

        AsteroidsControls(
            onRotateLeftChanged = { rotatingLeft = it },
            onRotateRightChanged = { rotatingRight = it },
            onThrustChanged = { thrusting = it },
            onFire = {
                val fired = AsteroidsLoop.fire(gameState)
                gameState = fired
                if (fired.firedThisTick) playSound(AsteroidsSoundCue.FIRE)
            }
        )
    }
}
