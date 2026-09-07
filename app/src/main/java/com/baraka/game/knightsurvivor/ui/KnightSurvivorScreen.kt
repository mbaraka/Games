package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.data.SurvivorSoundEngine
import com.baraka.game.knightsurvivor.data.SurvivorStatsRepository
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.GamePhase
import com.baraka.game.knightsurvivor.model.GameState
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Vec2
import com.baraka.game.knightsurvivor.model.WeaponInstance
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.render.GameCanvas
import com.baraka.game.knightsurvivor.systems.GameLoopOrchestrator
import kotlinx.coroutines.delay

private val BACKGROUND_COLOR = Color(0xFF12141A)

/**
 * Owns the run's [GameState] and the ~60fps game loop, and lays out canvas + joystick +
 * HUD + phase-conditional overlays. Loading/saving best-run stats is the one Android side
 * effect this file performs (via [SurvivorStatsRepository]) — every gameplay decision is
 * delegated to systems/GameLoopOrchestrator.kt.
 */
@Composable
fun KnightSurvivorScreen() {
    val context = LocalContext.current
    val statsRepository = remember { SurvivorStatsRepository.getInstance(context) }
    val soundEngine = remember { SurvivorSoundEngine() }
    DisposableEffect(Unit) {
        onDispose { soundEngine.release() }
    }

    var gameState by remember { mutableStateOf(createInitialGameState(statsRepository.bestSurvivalTimeMs())) }
    var hasRecordedThisRun by remember { mutableStateOf(false) }
    var showCompendium by remember { mutableStateOf(false) }
    val isRunning = gameState.phase == GamePhase.RUNNING && !showCompendium

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(GameTunables.TICK_MS)
            val ticked = GameLoopOrchestrator.tick(gameState)
            ticked.pendingSoundCues.forEach(soundEngine::play)
            gameState = if (ticked.pendingSoundCues.isEmpty()) ticked else ticked.copy(pendingSoundCues = emptyList())
        }
    }

    // Persist the result exactly once per concluded run.
    LaunchedEffect(gameState.phase) {
        val runConcluded = gameState.phase == GamePhase.GAME_OVER || gameState.phase == GamePhase.VICTORY
        if (runConcluded && !hasRecordedThisRun) {
            statsRepository.recordRunResult(
                survivalTimeMs = gameState.survivalTimeMs,
                levelReached = gameState.player.level,
                killsThisRun = gameState.killCount
            )
            hasRecordedThisRun = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BACKGROUND_COLOR)) {
        Hud(
            player = gameState.player,
            survivalTimeMs = gameState.survivalTimeMs,
            killCount = gameState.killCount,
            bestSurvivalTimeMs = gameState.bestSurvivalTimeMsAtStart,
            modifier = Modifier.padding(12.dp)
        )

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            GameCanvas(
                state = gameState,
                onSizeMeasured = { width, height ->
                    if (gameState.arenaSize.width != width || gameState.arenaSize.height != height) {
                        gameState = gameState.copy(arenaSize = ArenaSize(width, height))
                    }
                }
            )

            VirtualJoystick(
                onDirectionChanged = { direction -> gameState = gameState.copy(joystickInput = direction) },
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            )

            CompendiumButton(
                onClick = { showCompendium = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )

            if (showCompendium) {
                CompendiumOverlay(player = gameState.player, onClose = { showCompendium = false })
            }

            if (gameState.phase == GamePhase.LEVEL_UP_PAUSE) {
                LevelUpOverlay(
                    choices = gameState.pendingUpgradeChoices,
                    onChoiceSelected = { choice ->
                        gameState = GameLoopOrchestrator.applyUpgradeChoiceAndResume(gameState, choice)
                    }
                )
            }

            if (gameState.phase == GamePhase.GAME_OVER || gameState.phase == GamePhase.VICTORY) {
                EndScreenOverlay(
                    phase = gameState.phase,
                    survivalTimeMs = gameState.survivalTimeMs,
                    levelReached = gameState.player.level,
                    killCount = gameState.killCount,
                    isNewBestTime = gameState.isNewBestTime,
                    onPlayAgain = {
                        gameState = createInitialGameState(statsRepository.bestSurvivalTimeMs(), gameState.arenaSize)
                        hasRecordedThisRun = false
                    }
                )
            }
        }
    }
}

/** Opens [CompendiumOverlay]. A plain labeled chip rather than a glyph — a Unicode arrow already read as invisible/ambiguous once in this app's hub screen; a short word is unambiguous at any size. */
@Composable
private fun CompendiumButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = "Guide",
        color = Color.White,
        fontSize = 13.sp,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun createInitialGameState(
    bestSurvivalTimeMs: Long,
    arenaSize: ArenaSize = ArenaSize(GameTunables.DEFAULT_ARENA_WIDTH, GameTunables.DEFAULT_ARENA_HEIGHT)
): GameState {
    val player = PlayerState(
        position = Vec2(arenaSize.width / 2f, arenaSize.height / 2f),
        hp = GameTunables.PLAYER_BASE_MAX_HP,
        baseMaxHp = GameTunables.PLAYER_BASE_MAX_HP,
        xpToNextLevel = GameTunables.xpRequiredForLevel(1),
        weapons = listOf(WeaponInstance(kind = WeaponKind.SWORD_SLASH, level = 1))
    )
    return GameState(player = player, arenaSize = arenaSize, bestSurvivalTimeMsAtStart = bestSurvivalTimeMs)
}
