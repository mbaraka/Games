package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baraka.game.coop.CoopLobbyScreen
import com.baraka.game.coop.CoopSide
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.coop.KNIGHT_COOP
import com.baraka.game.knightsurvivor.coop.KnightCoopGuestSession
import com.baraka.game.knightsurvivor.coop.KnightCoopHostSession
import com.baraka.game.knightsurvivor.coop.SimulatedKnightPartner
import com.baraka.game.knightsurvivor.coop.UpgradeOptionDto
import com.baraka.game.knightsurvivor.data.AchievementRepository
import com.baraka.game.knightsurvivor.data.SettingsRepository
import com.baraka.game.knightsurvivor.data.SurvivorSoundEngine
import com.baraka.game.knightsurvivor.data.SurvivorStatsRepository
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.Difficulty
import com.baraka.game.knightsurvivor.model.GamePhase
import com.baraka.game.knightsurvivor.model.GameState
import com.baraka.game.knightsurvivor.model.KnightDesign
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Vec2
import com.baraka.game.knightsurvivor.model.WeaponInstance
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.render.GameCanvas
import com.baraka.game.knightsurvivor.systems.AchievementSystem
import com.baraka.game.knightsurvivor.systems.GameLoopOrchestrator
import kotlinx.coroutines.delay

/** Which role, if any, this device is currently playing in a co-op run — see knightsurvivor/coop/KnightCoopSession.kt. */
private enum class CoopMode { NONE, HOST, GUEST }

/**
 * Owns the run's [GameState] and the ~60fps game loop, and lays out canvas + joystick +
 * HUD + phase-conditional overlays. Loading/saving best-run stats is the one Android side
 * effect this file performs (via [SurvivorStatsRepository]) — every gameplay decision is
 * delegated to systems/GameLoopOrchestrator.kt.
 *
 * Co-op (see AGENTS.md, "Co-op for any game", and knightsurvivor/coop/): the HOST is the
 * only device that ever calls [GameLoopOrchestrator.tick] — it folds the partner in as a
 * second [PlayerState] on the shared [GameState] and streams snapshots out. The GUEST never
 * simulates anything; it just renders the most recent snapshot (reusing this exact same
 * screen/renderer) and sends its joystick input back. Known v1 simplification: the guest
 * doesn't evaluate its own achievements locally, and "Play Again" after a co-op run ends
 * the shared session for both devices rather than offering a synced rematch.
 */
@Composable
fun KnightSurvivorScreen() {
    val context = LocalContext.current
    val statsRepository = remember { SurvivorStatsRepository.getInstance(context) }
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val achievementRepository = remember { AchievementRepository.getInstance(context) }
    val soundEngine = remember { SurvivorSoundEngine() }
    DisposableEffect(Unit) {
        onDispose { soundEngine.release() }
    }

    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    var difficulty by remember { mutableStateOf(settingsRepository.difficulty()) }
    var weaponDeck by remember { mutableStateOf(settingsRepository.weaponDeck()) }
    var knightDesign by remember { mutableStateOf(settingsRepository.knightDesign()) }
    var showDeckPicker by remember { mutableStateOf(true) }
    var gameState by remember { mutableStateOf(createInitialGameState(statsRepository.bestSurvivalTimeMs(), difficulty = difficulty, weaponDeck = weaponDeck)) }
    var hasRecordedThisRun by remember { mutableStateOf(false) }
    var unlockedAchievements by remember { mutableStateOf(achievementRepository.unlockedAchievements()) }
    var showSettings by remember { mutableStateOf(false) }

    var coopMode by remember { mutableStateOf(CoopMode.NONE) }
    var showCoopLobby by remember { mutableStateOf(false) }
    var hostSession by remember { mutableStateOf<KnightCoopHostSession?>(null) }
    var guestSession by remember { mutableStateOf<KnightCoopGuestSession?>(null) }
    var guestRenderState by remember { mutableStateOf<GameState?>(null) }
    var guestArenaSize by remember { mutableStateOf(ArenaSize(GameTunables.DEFAULT_ARENA_WIDTH, GameTunables.DEFAULT_ARENA_HEIGHT)) }
    var guestChoices by remember { mutableStateOf<List<UpgradeOptionDto>>(emptyList()) }
    val isGuest = coopMode == CoopMode.GUEST

    fun leaveCoop() {
        hostSession?.close(); hostSession = null
        guestSession?.close(); guestSession = null
        guestRenderState = null
        guestChoices = emptyList()
        coopMode = CoopMode.NONE
    }

    // The game loop below is a plain coroutine LaunchedEffect, which keeps ticking through
    // ON_STOP (e.g. the screen locking, or switching apps) unless it's told not to — Compose
    // only tears it down on actual disposal. Track foreground/background explicitly so the
    // run truly pauses instead of continuing to take damage/spawn enemies while unseen.
    var isAppInForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppInForeground = true
                Lifecycle.Event.ON_PAUSE -> isAppInForeground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isRunning = !isGuest && gameState.phase == GamePhase.RUNNING && !showSettings && !showDeckPicker && !showCoopLobby && isAppInForeground

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(GameTunables.TICK_MS)
            val received = hostSession?.receive(gameState) ?: gameState
            val ticked = GameLoopOrchestrator.tick(received)
            if (soundEnabled) {
                ticked.pendingSoundCues.forEach(soundEngine::play)
                ticked.pendingWeaponFireSounds.forEach(soundEngine::playWeaponFire)
            }
            gameState = if (ticked.pendingSoundCues.isEmpty() && ticked.pendingWeaponFireSounds.isEmpty()) {
                ticked
            } else {
                ticked.copy(pendingSoundCues = emptyList(), pendingWeaponFireSounds = emptyList())
            }
            hostSession?.send(gameState, GameTunables.TICK_MS / 1000f)

            val newlyUnlocked = AchievementSystem.evaluate(gameState) - unlockedAchievements
            if (newlyUnlocked.isNotEmpty()) {
                unlockedAchievements = unlockedAchievements + newlyUnlocked
                achievementRepository.unlock(newlyUnlocked)
            }
        }
    }

    // Guest render loop: never calls GameLoopOrchestrator.tick — just polls the session for
    // whatever the host last sent and pushes it into Compose state so the UI recomposes.
    LaunchedEffect(guestSession) {
        val session = guestSession ?: return@LaunchedEffect
        while (guestSession === session) {
            delay(GameTunables.TICK_MS)
            session.poll(guestArenaSize)
            guestRenderState = session.latestRenderState
            guestChoices = session.pendingChoices
        }
    }

    // Persist the result exactly once per concluded run (host/solo only — the guest doesn't own save data for a run it isn't simulating).
    LaunchedEffect(gameState.phase) {
        val runConcluded = gameState.phase == GamePhase.GAME_OVER || gameState.phase == GamePhase.VICTORY
        if (!isGuest && runConcluded && !hasRecordedThisRun) {
            statsRepository.recordRunResult(
                survivalTimeMs = gameState.survivalTimeMs,
                levelReached = gameState.player.level,
                killsThisRun = gameState.killCount
            )
            hasRecordedThisRun = true
        }
    }

    if (showDeckPicker) {
        WeaponDeckPickerScreen(
            initialSelection = weaponDeck,
            onConfirm = { chosen ->
                weaponDeck = chosen
                settingsRepository.setWeaponDeck(chosen)
                gameState = createInitialGameState(statsRepository.bestSurvivalTimeMs(), difficulty = difficulty, weaponDeck = chosen)
                hasRecordedThisRun = false
                showDeckPicker = false
            }
        )
        return
    }

    if (showCoopLobby) {
        CoopLobbyScreen(
            spec = KNIGHT_COOP,
            heading = "Knight Survivor Co-op",
            intro = "Team up with another phone in the same arena. You each keep your own weapons and level — enemies, XP, and pickups are shared.",
            practiceNote = "A bot partner charges the nearest enemy and auto-picks its own upgrades, so you can try co-op alone.",
            simulate = { SimulatedKnightPartner(weaponDeck) },
            onReady = { launch ->
                showCoopLobby = false
                when (launch.side) {
                    CoopSide.HOST -> {
                        val arenaSize = gameState.arenaSize
                        hostSession = KnightCoopHostSession(launch.link, knightDesign) { deck ->
                            createPlayer(arenaSize, difficulty, deck)
                        }
                        coopMode = CoopMode.HOST
                    }
                    CoopSide.GUEST -> {
                        guestSession = KnightCoopGuestSession(launch.link, knightDesign, weaponDeck)
                        coopMode = CoopMode.GUEST
                    }
                }
            },
            onClose = { showCoopLobby = false }
        )
        return
    }

    val displayState = if (isGuest) guestRenderState else gameState

    if (displayState == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Connecting to host…", color = Color.White, fontSize = 16.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Hud(
            player = displayState.player,
            survivalTimeMs = displayState.survivalTimeMs,
            killCount = displayState.killCount,
            bestSurvivalTimeMs = displayState.bestSurvivalTimeMsAtStart,
            modifier = Modifier.padding(12.dp)
        )

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            GameCanvas(
                state = displayState,
                knightDesign = knightDesign,
                partnerKnightDesign = if (isGuest) guestSession?.hostKnightDesign ?: KnightDesign.SWORN_BLADE else hostSession?.partnerKnightDesign ?: KnightDesign.SWORN_BLADE,
                onSizeMeasured = { width, height ->
                    if (isGuest) {
                        if (guestArenaSize.width != width || guestArenaSize.height != height) {
                            guestArenaSize = ArenaSize(width, height)
                        }
                    } else {
                        if (gameState.arenaSize.width != width || gameState.arenaSize.height != height) {
                            gameState = gameState.copy(arenaSize = ArenaSize(width, height))
                        }
                    }
                }
            )

            VirtualJoystick(
                onDirectionChanged = { direction ->
                    if (isGuest) guestSession?.sendInput(direction) else gameState = gameState.copy(joystickInput = direction)
                },
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            )

            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (coopMode == CoopMode.NONE) {
                    TopBarChip(text = "Co-op", onClick = { showCoopLobby = true })
                }
                TopBarChip(text = "Settings", onClick = { showSettings = true })
            }

            if (showSettings) {
                SettingsScreen(
                    soundEnabled = soundEnabled,
                    onSoundEnabledChanged = { enabled ->
                        soundEnabled = enabled
                        settingsRepository.setSoundEnabled(enabled)
                    },
                    difficulty = difficulty,
                    onDifficultyChanged = { chosen ->
                        difficulty = chosen
                        settingsRepository.setDifficulty(chosen)
                    },
                    player = displayState.player,
                    unlockedAchievements = unlockedAchievements,
                    knightDesign = knightDesign,
                    onKnightDesignChanged = { chosen ->
                        knightDesign = chosen
                        settingsRepository.setKnightDesign(chosen)
                    },
                    onClose = { showSettings = false },
                    onWeaponCardClick = { kind -> if (soundEnabled) soundEngine.playWeaponFire(kind) }
                )
            }

            if (!isGuest && displayState.phase == GamePhase.LEVEL_UP_PAUSE) {
                LevelUpOverlay(
                    choices = displayState.pendingUpgradeChoices,
                    onChoiceSelected = { choice ->
                        gameState = GameLoopOrchestrator.applyUpgradeChoiceAndResume(gameState, choice)
                    }
                )
            }

            if (isGuest && guestChoices.isNotEmpty()) {
                LevelUpOverlay(
                    choices = guestChoices,
                    onChoiceSelected = { choice ->
                        guestSession?.selectChoice(guestChoices.indexOf(choice))
                        guestChoices = emptyList()
                    }
                )
            }

            if (displayState.phase == GamePhase.GAME_OVER || displayState.phase == GamePhase.VICTORY) {
                EndScreenOverlay(
                    phase = displayState.phase,
                    survivalTimeMs = displayState.survivalTimeMs,
                    levelReached = displayState.player.level,
                    killCount = displayState.killCount,
                    isNewBestTime = displayState.isNewBestTime,
                    onPlayAgain = {
                        if (coopMode != CoopMode.NONE) leaveCoop()
                        showDeckPicker = true
                    }
                )
            }
        }
    }
}

/** Opens an overlay (Co-op/Settings). A plain labeled chip rather than a glyph — a Unicode arrow already read as invisible/ambiguous once in this app's hub screen; a short word is unambiguous at any size. */
@Composable
private fun TopBarChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun createPlayer(arenaSize: ArenaSize, difficulty: Difficulty, weaponDeck: Set<WeaponKind>): PlayerState {
    val maxHp = (GameTunables.PLAYER_BASE_MAX_HP * difficulty.playerMaxHpMultiplier).toInt()
    val startingWeapon = if (WeaponKind.SWORD_SLASH in weaponDeck) WeaponKind.SWORD_SLASH else weaponDeck.first()
    return PlayerState(
        position = Vec2(arenaSize.width / 2f, arenaSize.height / 2f),
        hp = maxHp,
        baseMaxHp = maxHp,
        xpToNextLevel = GameTunables.xpRequiredForLevel(1),
        weapons = listOf(WeaponInstance(kind = startingWeapon, level = 1))
    )
}

private fun createInitialGameState(
    bestSurvivalTimeMs: Long,
    arenaSize: ArenaSize = ArenaSize(GameTunables.DEFAULT_ARENA_WIDTH, GameTunables.DEFAULT_ARENA_HEIGHT),
    difficulty: Difficulty = Difficulty.NORMAL,
    weaponDeck: Set<WeaponKind> = WeaponKind.entries.toSet()
): GameState = GameState(
    player = createPlayer(arenaSize, difficulty, weaponDeck),
    arenaSize = arenaSize,
    bestSurvivalTimeMsAtStart = bestSurvivalTimeMs,
    difficulty = difficulty,
    weaponDeck = weaponDeck
)
