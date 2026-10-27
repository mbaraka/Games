package com.baraka.game.arcshot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.arcshot.data.ArcAudio
import com.baraka.game.arcshot.data.ArcCue
import com.baraka.game.arcshot.data.ArcHaptics
import com.baraka.game.arcshot.data.ArcRepository
import com.baraka.game.arcshot.data.ArcSettings
import com.baraka.game.arcshot.model.ARC_LEVELS
import com.baraka.game.arcshot.model.GROUND_Y
import com.baraka.game.arcshot.model.LAUNCHER_X
import com.baraka.game.arcshot.model.WORLD_H
import com.baraka.game.arcshot.model.WORLD_W
import com.baraka.game.arcshot.render.drawArcScene
import com.baraka.game.arcshot.systems.ArcGame
import com.baraka.game.arcshot.systems.BALL_RADIUS
import com.baraka.game.arcshot.systems.MAX_POWER
import com.baraka.game.arcshot.systems.MIN_POWER
import com.baraka.game.arcshot.systems.ShotResult
import com.baraka.game.arcshot.systems.starsFor
import com.baraka.game.ui.debug.FpsOverlay
import com.baraka.game.ui.debug.FrameStats
import kotlin.math.hypot
import kotlin.random.Random

private const val MAX_PULL_WORLD = 240f
private val SPEEDS = floatArrayOf(1f, 0.4f)

/**
 * One level in play: owns the [ArcGame], turns a slingshot-style drag into a launch, steps the
 * physics off the display's frame clock and draws the scene. All gameplay rules live in [ArcGame];
 * this screen only draws, reads input and plays feedback.
 */
@Composable
fun ArcPlayScreen(
    levelIndex: Int,
    repo: ArcRepository,
    settings: ArcSettings,
    onSettingsChange: (ArcSettings) -> Unit,
    audio: ArcAudio,
    haptics: ArcHaptics,
    versionName: String,
    onOpenLevel: (Int) -> Unit,
    onExit: () -> Unit
) {
    val level = ARC_LEVELS[levelIndex]
    var runId by remember(levelIndex) { mutableIntStateOf(0) }
    val game = remember(levelIndex, runId) { ArcGame(level) }
    val stats = remember { FrameStats() }
    var frame by remember { mutableIntStateOf(0) }
    var showMenu by remember(game) { mutableStateOf(false) }
    var showSettings by remember(game) { mutableStateOf(false) }
    var debugRev by remember(game) { mutableIntStateOf(0) }
    var recorded by remember(game) { mutableStateOf(false) }
    var slowMo by remember(game) { mutableStateOf(false) }
    var shake by remember { mutableFloatStateOf(0f) }

    var canvasW by remember { mutableIntStateOf(0) }
    var canvasH by remember { mutableIntStateOf(0) }
    var dragVx by remember { mutableFloatStateOf(0f) }
    var dragVy by remember { mutableFloatStateOf(0f) }
    var aiming by remember(game) { mutableStateOf(false) }

    LaunchedEffect(settings) {
        audio.volume = settings.sfxVolume
        haptics.enabled = settings.vibration
    }
    LaunchedEffect(game.won) {
        if (game.won && !recorded) {
            recorded = true
            val stars = starsFor(level, game.shots)
            repo.recordResult(levelIndex, game.shots, stars)
            audio.play(ArcCue.WIN)
            haptics.cue(ArcCue.WIN)
        }
    }

    fun fit(): Triple<Float, Float, Float> {
        val scale = minOf(canvasW / WORLD_W, canvasH / WORLD_H).coerceAtLeast(0.001f)
        val ox = (canvasW - WORLD_W * scale) / 2f
        val oy = (canvasH - WORLD_H * scale) / 2f
        return Triple(scale, ox, oy)
    }

    LaunchedEffect(game) {
        var last = 0L
        var acc = 0f
        val dtFixed = 1f / 120f
        var prevResult = ShotResult.NONE
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    if (settings.showFps) stats.record(dt * 1000f)
                    if (shake > 0f) shake = (shake - dt * 6f).coerceAtLeast(0f)
                    if (game.result == ShotResult.FLYING) {
                        acc += dt * (if (slowMo) SPEEDS[1] else SPEEDS[0])
                        var steps = 0
                        while (acc >= dtFixed && steps < 20) {
                            game.step(dtFixed)
                            acc -= dtFixed
                            steps++
                            if (game.result != ShotResult.FLYING) break
                        }
                    }
                    if (game.result != prevResult) {
                        if (game.result == ShotResult.MISS) {
                            audio.play(ArcCue.BOUNCE_OFF)
                            haptics.cue(ArcCue.BOUNCE_OFF)
                            if (settings.screenShake) shake = 1f
                            game.readyForNextShot()
                        }
                        prevResult = game.result
                    }
                }
                last = now
                frame++
            }
        }
    }

    fun previewPoints(): List<Offset> {
        val pts = ArrayList<Offset>(16)
        var px = LAUNCHER_X
        var py = GROUND_Y - BALL_RADIUS
        var vx = dragVx
        var vy = dragVy
        val dt = 0.07f
        for (i in 0 until 14) {
            vx += game.wind * dt
            vy += game.gravity * dt
            px += vx * dt
            py += vy * dt
            if (py > GROUND_Y + 40f) break
            pts.add(Offset(px, py))
        }
        return pts
    }

    BackHandler(enabled = !showSettings) { showMenu = !showMenu }

    Box(Modifier.fillMaxSize().background(ArcColors.Bg)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcButton("Menu", onClick = { showMenu = true })
                Text("LEVEL ${levelIndex + 1}  -  ${level.name}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Shots ${game.shots}", color = ArcColors.Muted, fontSize = 13.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                frame
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasW = it.width; canvasH = it.height }
                        .pointerInput(game) {
                            detectDragGestures(
                                onDragStart = {
                                    if (game.result != ShotResult.FLYING && !game.won) {
                                        aiming = true
                                        dragVx = 0f
                                        dragVy = 0f
                                    }
                                },
                                onDragEnd = {
                                    if (aiming) {
                                        aiming = false
                                        val len = hypot(dragVx, dragVy).coerceAtLeast(0.0001f)
                                        val clamped = len.coerceAtMost(MAX_PULL_WORLD)
                                        val power = (MIN_POWER + (clamped / MAX_PULL_WORLD) * (MAX_POWER - MIN_POWER))
                                        if (clamped > 12f) {
                                            val nx = dragVx / len
                                            val ny = dragVy / len
                                            game.launch(nx * power, ny * power)
                                            audio.play(ArcCue.LAUNCH)
                                            haptics.cue(ArcCue.LAUNCH)
                                        }
                                    }
                                },
                                onDragCancel = { aiming = false }
                            ) { change, drag ->
                                change.consume()
                                if (aiming) {
                                    val (scale, _, _) = fit()
                                    dragVx -= drag.x / scale
                                    dragVy -= drag.y / scale
                                }
                            }
                        }
                ) {
                    val (scale, ox, oy) = fit()
                    val shakeX = if (shake > 0f) (Random.nextFloat() - 0.5f) * shake * 14f else 0f
                    val shakeY = if (shake > 0f) (Random.nextFloat() - 0.5f) * shake * 14f else 0f
                    drawArcScene(
                        game = game,
                        level = level,
                        scale = scale,
                        ox = ox + shakeX,
                        oy = oy + shakeY,
                        canvasSize = size,
                        aimVx = dragVx,
                        aimVy = dragVy,
                        aiming = aiming,
                        previewPoints = if (aiming) previewPoints() else emptyList()
                    )
                }
                if (settings.showFps) FpsOverlay(stats, Modifier.align(Alignment.TopStart))
                if (!aiming && game.result != ShotResult.FLYING && !game.won) {
                    Text(
                        "Drag anywhere to aim, release to launch",
                        color = ArcColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    )
                }
            }
        }

        if (game.won) {
            val stars = starsFor(level, game.shots)
            Overlay {
                Text("TARGET HIT!", color = ArcColors.Good, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (s in 1..3) Text(if (s <= stars) "★" else "☆", color = if (s <= stars) ArcColors.Star else ArcColors.Muted, fontSize = 22.sp)
                }
                Text("${game.shots} shot(s)  -  par ${level.parShots}", color = Color.White, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (levelIndex + 1 < ARC_LEVELS.size) ArcButton("Next level", primary = true) { onOpenLevel(levelIndex + 1) }
                    ArcButton("Retry") { runId++ }
                    ArcButton("Levels", onClick = onExit)
                }
            }
        }

        if (showMenu && !showSettings && !game.won) {
            Overlay {
                Text("PAUSED", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                ArcButton("Resume", primary = true) { showMenu = false }
                ArcButton("Restart level") { runId++ }
                ArcButton("Settings") { showSettings = true }
                ArcButton("Level select", onClick = onExit)
            }
        }

        if (showSettings) {
            debugRev
            ArcSettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onClose = { showSettings = false },
                versionName = versionName,
                debugSections = runDebugSections(
                    game, repo, levelIndex,
                    slowMo = slowMo,
                    onSlowMo = { slowMo = it; debugRev++ },
                    onChanged = { debugRev++; frame++ },
                    onJump = { showSettings = false; onOpenLevel(it) }
                )
            )
        }
    }
}

@Composable
private fun Overlay(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xB3000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 460.dp).background(ArcColors.Card, RoundedCornerShape(20.dp)).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
