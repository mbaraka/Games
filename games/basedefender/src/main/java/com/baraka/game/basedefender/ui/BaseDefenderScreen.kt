package com.baraka.game.basedefender.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.basedefender.BuildConfig
import com.baraka.game.basedefender.data.BdRepository
import com.baraka.game.basedefender.data.BdSettings
import com.baraka.game.basedefender.model.GameStatus
import com.baraka.game.basedefender.model.PowerUpType
import com.baraka.game.basedefender.model.Tuning
import com.baraka.game.basedefender.model.WeaponType
import com.baraka.game.basedefender.render.BaseDefenderRenderer
import com.baraka.game.basedefender.systems.BaseDefenderEngine
import com.baraka.game.ui.debug.FpsOverlay
import com.baraka.game.ui.debug.FrameStats

private enum class Phase { MENU, PLAYING }

/**
 * The Base Defender screen: owns the engine, the game loop and which overlay is up.
 * Layout: HUD on top, the battlefield filling the middle, weapon and power-up controls below.
 */
@Composable
fun BaseDefenderScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val repo = remember { BdRepository.get(context) }
    var settings by remember { mutableStateOf(repo.settings()) }
    val engine = remember { BaseDefenderEngine(settings) }
    val renderer = remember { BaseDefenderRenderer() }
    val stats = remember { FrameStats() }

    var phase by remember { mutableStateOf(Phase.MENU) }
    var paused by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var hud by remember { mutableStateOf(engine.makeHud()) }
    var frame by remember { mutableIntStateOf(0) }
    var shopTick by remember { mutableIntStateOf(0) }
    var debugTick by remember { mutableIntStateOf(0) }
    var newBest by remember { mutableStateOf(false) }
    var recorded by remember { mutableStateOf(false) }
    var bestScore by remember { mutableIntStateOf(repo.bestScore()) }
    var bestWave by remember { mutableIntStateOf(repo.bestWave()) }

    fun applySettings(s: BdSettings) {
        settings = s
        engine.settings = s
        repo.save(s)
    }

    fun restart() {
        engine.newGame()
        hud = engine.makeHud()
        paused = false; showShop = false
        newBest = false; recorded = false
        phase = Phase.PLAYING
    }

    val running = phase == Phase.PLAYING && !paused && !showShop && !showSettings && hud.status == GameStatus.PLAYING
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    stats.record(dt * 1000f)
                    engine.advance(dt)
                }
                last = now
                frame++
                if (engine.hudDirty) hud = engine.makeHud()
                if (engine.hapticPulses > 0) {
                    if (settings.vibration) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    engine.hapticPulses = 0
                }
            }
        }
    }

    // Record the result once when a run ends. The engine keeps stepping its effects, so give it a final HUD too.
    LaunchedEffect(hud.status) {
        if (hud.status != GameStatus.PLAYING && !recorded && phase == Phase.PLAYING) {
            recorded = true
            newBest = engine.score > repo.bestScore()
            repo.recordRun(engine.score, engine.wave)
            bestScore = repo.bestScore(); bestWave = repo.bestWave()
        }
    }

    // Back while playing pauses; back on the pause menu (or start/end screens) falls through and leaves the game.
    BackHandler(enabled = phase == Phase.PLAYING && hud.status == GameStatus.PLAYING && !paused && !showSettings && !showShop) { paused = true }
    val exitGame = { (context as? android.app.Activity)?.finish(); Unit }
    BackHandler(enabled = showShop) { showShop = false }

    val debugSections = remember(debugTick, hud) { baseDefenderDebugSections(engine, repo) { debugTick++ } }

    Box(Modifier.fillMaxSize().background(BdColors.Bg)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            HudBar(hud, onPause = { if (phase == Phase.PLAYING) paused = true })
            LinearProgressIndicator(
                progress = { ((hud.wave - 1).coerceAtLeast(0).toFloat() / Tuning.TOTAL_WAVES).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = BdColors.Info,
                trackColor = BdColors.Chip
            )
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val s = engine.viewScale
                            engine.pointerDown(down.position.x / s, down.position.y / s)
                            do {
                                val event = awaitPointerEvent()
                                val c = event.changes.firstOrNull()
                                if (c != null) {
                                    engine.pointerMove(c.position.x / s, c.position.y / s)
                                    c.consume()
                                }
                            } while (event.changes.any { it.pressed })
                            engine.pointerUp()
                        }
                    }
                ) {
                    frame // reading this state redraws the canvas every frame
                    engine.resize(size.width, size.height)
                    renderer.draw(this, engine, showCrosshair = true, shakeOn = settings.screenShake)
                }
                if (settings.showFps && running) FpsOverlay(stats, Modifier.align(Alignment.TopStart).padding(6.dp))
            }
            ControlBar(hud, onWeapon = { engine.weapon = it })
        }

        if (phase == Phase.MENU && !showSettings) {
            BdStartOverlay(bestScore, bestWave, onStart = { restart() }, onSettings = { showSettings = true }, onExit = exitGame)
        }
        if (phase == Phase.PLAYING && paused && !showShop && !showSettings && hud.status == GameStatus.PLAYING) {
            BdPauseOverlay(
                onResume = { paused = false },
                onShop = { showShop = true },
                onSettings = { showSettings = true },
                onRestart = { restart() },
                onExit = exitGame
            )
        }
        if (phase == Phase.PLAYING && hud.status != GameStatus.PLAYING && !showSettings) {
            BdEndOverlay(
                victory = hud.status == GameStatus.VICTORY,
                engine = engine,
                newBest = newBest,
                onAgain = { restart() },
                onSettings = { showSettings = true },
                onExit = exitGame
            )
        }
        if (showShop) {
            BdShopOverlay(engine, hud, shopTick, onBuy = {
                engine.buy(it)
                hud = engine.makeHud()
                shopTick++
            }, onClose = { showShop = false })
        }
        if (showSettings) {
            BdSettingsScreen(
                settings = settings,
                onChange = { applySettings(it) },
                onClose = { showSettings = false },
                versionName = BuildConfig.BASE_DEFENDER_VERSION,
                debugSections = debugSections
            )
        }
    }
}

@Composable
private fun HudBar(hud: com.baraka.game.basedefender.systems.BdHud, onPause: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(BdColors.Bar).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Stat("WAVE", "${hud.wave}/${Tuning.TOTAL_WAVES}", BdColors.Text)
            Stat("MONEY", "$${hud.money}", BdColors.Money)
            Stat("LIVES", hearts(hud.lives), BdColors.Danger)
            Stat("SCORE", "${hud.score}", BdColors.Info)
            Stat("BASES", "${hud.bases}", BdColors.Good)
        }
        BdGhostButton("II", onPause)
    }
}

private fun hearts(n: Int): String {
    val sb = StringBuilder()
    for (i in 0 until n) sb.append('♥')
    return if (sb.isEmpty()) "0" else sb.toString()
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, color = BdColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ControlBar(hud: com.baraka.game.basedefender.systems.BdHud, onWeapon: (WeaponType) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(BdColors.Bar).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (w in WeaponType.entries) {
                BdChip(w.label, hud.weapon == w, w.color, { onWeapon(w) }, Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in PowerUpType.entries) {
                val active = (hud.powerMask shr p.ordinal) and 1 == 1
                val label = if (p == PowerUpType.MEGA_SHOT && active) "${p.label} x${hud.megaCharges}" else p.label
                BdPill(label, active, p.color, Modifier.weight(1f))
            }
        }
    }
}
