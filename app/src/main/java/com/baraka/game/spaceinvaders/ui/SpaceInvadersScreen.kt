package com.baraka.game.spaceinvaders.ui

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
import com.baraka.game.roadrush.ui.RoadRushSettingsOverlay
import com.baraka.game.spaceinvaders.data.SpaceInvadersSettingsRepository
import com.baraka.game.spaceinvaders.data.SpaceInvadersSoundCue
import com.baraka.game.spaceinvaders.data.SpaceInvadersSoundEngine
import com.baraka.game.spaceinvaders.model.GamePhase
import com.baraka.game.spaceinvaders.render.SpaceInvadersRenderer
import com.baraka.game.spaceinvaders.systems.AlienWaveLoop
import kotlinx.coroutines.delay
import kotlin.random.Random

private val BACKGROUND_COLOR = Color(0xFF07080C)
private const val TICK_MS = 16L

/**
 * Owns the run's [com.baraka.game.spaceinvaders.model.SpaceInvadersState] and its ~60fps
 * loop, same shape as breakout's BreakoutScreen: this file wires input and persistence,
 * systems/AlienWaveLoop.kt makes every gameplay decision. Dragging moves the player cannon;
 * tapping fires (subject to its cooldown).
 */
@Composable
fun SpaceInvadersScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SpaceInvadersSettingsRepository.getInstance(context) }
    val rng = remember { Random(System.nanoTime()) }

    var difficulty by remember { mutableStateOf(settingsRepository.difficulty()) }
    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    var showStartOverlay by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var gameState by remember {
        mutableStateOf(AlienWaveLoop.newGame(difficulty, settingsRepository.bestScore()))
    }
    var hasRecordedThisRun by remember { mutableStateOf(false) }

    val soundEngine = remember { SpaceInvadersSoundEngine() }
    DisposableEffect(Unit) { onDispose { soundEngine.release() } }
    fun playSound(cue: SpaceInvadersSoundCue) {
        if (soundEnabled) soundEngine.play(cue)
    }

    val looping = !showStartOverlay && !showSettings && gameState.phase == GamePhase.RUNNING

    LaunchedEffect(looping) {
        while (looping) {
            delay(TICK_MS)
            val next = AlienWaveLoop.tick(gameState, TICK_MS / 1000f, rng)
            if (next.alienKilledThisTick > 0) playSound(SpaceInvadersSoundCue.ALIEN_KILLED)
            if (next.shieldHitThisTick) playSound(SpaceInvadersSoundCue.SHIELD_HIT)
            if (next.playerHitThisTick) playSound(SpaceInvadersSoundCue.PLAYER_HIT)
            if (next.alienMarchStepThisTick) playSound(SpaceInvadersSoundCue.MARCH_STEP)
            if (next.phase == GamePhase.WAVE_CLEARED && gameState.phase != GamePhase.WAVE_CLEARED) {
                playSound(SpaceInvadersSoundCue.WAVE_CLEAR)
            }
            gameState = next
        }
    }

    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.GAME_OVER && !hasRecordedThisRun) {
            playSound(SpaceInvadersSoundCue.GAME_OVER)
            if (gameState.score > settingsRepository.bestScore()) {
                settingsRepository.setBestScore(gameState.score)
            }
            hasRecordedThisRun = true
        }
    }

    fun startNewRun() {
        gameState = AlienWaveLoop.newGame(difficulty, settingsRepository.bestScore())
        hasRecordedThisRun = false
        showStartOverlay = false
    }

    if (showStartOverlay) {
        SpaceInvadersStartOverlay(
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
        SpaceInvadersHud(
            lives = gameState.lives,
            score = gameState.score,
            wave = gameState.wave,
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
                        val fired = AlienWaveLoop.firePlayerBullet(gameState)
                        gameState = fired
                        if (fired.playerShotThisTick) playSound(SpaceInvadersSoundCue.PLAYER_SHOT)
                    })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            gameState = AlienWaveLoop.movePlayer(gameState, offset.x / size.width)
                        }
                    ) { change, _ ->
                        change.consume()
                        gameState = AlienWaveLoop.movePlayer(gameState, change.position.x / size.width)
                    }
                }
        ) {
            SpaceInvadersRenderer(state = gameState)

            if (gameState.phase == GamePhase.WAVE_CLEARED) {
                SpaceInvadersWaveClearedOverlay(
                    wave = gameState.wave,
                    score = gameState.score,
                    onNextWave = { gameState = AlienWaveLoop.startWave(gameState, gameState.wave + 1) }
                )
            }

            if (gameState.phase == GamePhase.GAME_OVER) {
                SpaceInvadersEndScreen(
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
    }
}
