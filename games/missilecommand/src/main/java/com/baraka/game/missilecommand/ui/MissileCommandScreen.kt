package com.baraka.game.missilecommand.ui

import androidx.compose.foundation.background
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
import com.baraka.game.missilecommand.data.MissileCommandSettingsRepository
import com.baraka.game.missilecommand.data.MissileCommandSoundCue
import com.baraka.game.missilecommand.data.MissileCommandSoundEngine
import com.baraka.game.missilecommand.model.GamePhase
import com.baraka.game.missilecommand.render.MissileCommandRenderer
import com.baraka.game.missilecommand.systems.MissileCommandLoop
import com.baraka.game.ui.components.SoundSettingsOverlay
import kotlinx.coroutines.delay
import kotlin.random.Random

private val BACKGROUND_COLOR = Color(0xFF07080C)
private const val TICK_MS = 16L

/**
 * Owns the run's [com.baraka.game.missilecommand.model.MissileCommandState] and its ~60fps
 * loop, same shape as breakout's/spaceinvaders' screens: this file wires input and
 * persistence, systems/MissileCommandLoop.kt makes every gameplay decision. A single tap
 * anywhere in the play area fires an interceptor at that point.
 */
@Composable
fun MissileCommandScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { MissileCommandSettingsRepository.getInstance(context) }
    val rng = remember { Random(System.nanoTime()) }

    var difficulty by remember { mutableStateOf(settingsRepository.difficulty()) }
    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    var showStartOverlay by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var gameState by remember {
        mutableStateOf(MissileCommandLoop.newGame(difficulty, settingsRepository.bestScore()))
    }
    var hasRecordedThisRun by remember { mutableStateOf(false) }

    val soundEngine = remember { MissileCommandSoundEngine() }
    DisposableEffect(Unit) { onDispose { soundEngine.release() } }
    fun playSound(cue: MissileCommandSoundCue) {
        if (soundEnabled) soundEngine.play(cue)
    }

    val looping = !showStartOverlay && !showSettings && gameState.phase == GamePhase.RUNNING

    LaunchedEffect(looping) {
        while (looping) {
            delay(TICK_MS)
            val next = MissileCommandLoop.tick(gameState, TICK_MS / 1000f, rng)
            if (next.detonatedThisTick > 0) playSound(MissileCommandSoundCue.DETONATE)
            if (next.missilesDestroyedThisTick > 0) playSound(MissileCommandSoundCue.ENEMY_DESTROYED)
            if (next.cityLostThisTick) playSound(MissileCommandSoundCue.CITY_LOST)
            if (next.siloLostThisTick) playSound(MissileCommandSoundCue.SILO_LOST)
            if (next.phase == GamePhase.WAVE_CLEARED && gameState.phase != GamePhase.WAVE_CLEARED) {
                playSound(MissileCommandSoundCue.WAVE_CLEAR)
            }
            gameState = next
        }
    }

    LaunchedEffect(gameState.phase) {
        if (gameState.phase == GamePhase.GAME_OVER && !hasRecordedThisRun) {
            playSound(MissileCommandSoundCue.GAME_OVER)
            if (gameState.score > settingsRepository.bestScore()) {
                settingsRepository.setBestScore(gameState.score)
            }
            hasRecordedThisRun = true
        }
    }

    fun startNewRun() {
        gameState = MissileCommandLoop.newGame(difficulty, settingsRepository.bestScore())
        hasRecordedThisRun = false
        showStartOverlay = false
    }

    if (showStartOverlay) {
        MissileCommandStartOverlay(
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
        MissileCommandHud(
            citiesRemaining = gameState.citiesRemaining,
            ammo = gameState.ammo,
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
                    detectTapGestures(onTap = { offset ->
                        val fired = MissileCommandLoop.fire(gameState, offset.x / size.width, offset.y / size.height)
                        gameState = fired
                        if (fired.launchedThisTick) playSound(MissileCommandSoundCue.LAUNCH)
                    })
                }
        ) {
            MissileCommandRenderer(state = gameState)

            if (gameState.phase == GamePhase.WAVE_CLEARED) {
                MissileCommandWaveClearedOverlay(
                    wave = gameState.wave,
                    score = gameState.score,
                    citiesRemaining = gameState.citiesRemaining,
                    onNextWave = { gameState = MissileCommandLoop.startWave(gameState, gameState.wave + 1) }
                )
            }

            if (gameState.phase == GamePhase.GAME_OVER) {
                MissileCommandEndScreen(
                    score = gameState.score,
                    wave = gameState.wave,
                    isNewBest = gameState.isNewBest,
                    onPlayAgain = { showStartOverlay = true }
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
    }
}
