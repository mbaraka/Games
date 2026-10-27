package com.baraka.game.cratecrew.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.cratecrew.data.CrateAudio
import com.baraka.game.cratecrew.data.CrateCue
import com.baraka.game.cratecrew.data.CrateHaptics
import com.baraka.game.cratecrew.data.CrateRepository
import com.baraka.game.cratecrew.data.CrateSettings
import com.baraka.game.cratecrew.model.CRATE_LEVELS
import com.baraka.game.cratecrew.systems.CrateGame
import com.baraka.game.ui.debug.FpsOverlay
import com.baraka.game.ui.debug.FrameStats
import kotlin.math.abs

/**
 * One level in play: owns the [CrateGame], turns D-pad taps and swipes into moves, and shows the
 * HUD and result overlay. All puzzle rules live in [CrateGame]; this screen only draws and reads input.
 */
@Composable
fun CratePlayScreen(
    levelIndex: Int,
    repo: CrateRepository,
    settings: CrateSettings,
    onSettingsChange: (CrateSettings) -> Unit,
    audio: CrateAudio,
    haptics: CrateHaptics,
    versionName: String,
    onOpenLevel: (Int) -> Unit,
    onExit: () -> Unit
) {
    val level = CRATE_LEVELS[levelIndex]
    var runId by remember(levelIndex) { mutableIntStateOf(0) }
    val game = remember(levelIndex, runId) { CrateGame(level) }
    val stats = remember { FrameStats() }
    var frame by remember { mutableIntStateOf(0) }
    var showMenu by remember(game) { mutableStateOf(false) }
    var showSettings by remember(game) { mutableStateOf(false) }
    var debugRev by remember(game) { mutableIntStateOf(0) }
    var recorded by remember(game) { mutableStateOf(false) }

    LaunchedEffect(settings) {
        audio.volume = settings.sfxVolume
        haptics.enabled = settings.vibration
    }
    LaunchedEffect(game.isSolved) {
        if (game.isSolved && !recorded) {
            recorded = true
            repo.recordResult(levelIndex, game.moves)
            audio.play(CrateCue.WIN)
            haptics.cue(CrateCue.WIN)
        }
    }

    fun act(dx: Int, dy: Int) {
        if (game.isSolved || showMenu || showSettings) return
        val before = game.pushes
        if (game.move(dx, dy)) {
            if (game.pushes != before) {
                audio.play(CrateCue.PUSH)
                haptics.cue(CrateCue.PUSH)
            } else {
                audio.play(CrateCue.STEP)
            }
        } else {
            audio.play(CrateCue.BLOCKED)
            haptics.cue(CrateCue.BLOCKED)
        }
        frame++
    }

    fun undo() {
        if (game.undo()) {
            audio.play(CrateCue.UNDO)
            frame++
        }
    }

    BackHandler(enabled = !showSettings) { showMenu = !showMenu }

    Box(Modifier.fillMaxSize().background(CrateColors.Bg)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrateButton("Menu", onClick = { showMenu = true })
                Text("LEVEL ${levelIndex + 1}  -  ${level.name}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Moves ${game.moves}", color = CrateColors.Muted, fontSize = 13.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                frame
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .aspectRatio(game.width.toFloat() / game.height)
                        .pointerInput(game) {
                            var dragX = 0f
                            var dragY = 0f
                            detectDragGestures(
                                onDragStart = { dragX = 0f; dragY = 0f },
                                onDragEnd = {
                                    if (abs(dragX) > abs(dragY)) {
                                        if (abs(dragX) > 20f) act(if (dragX > 0) 1 else -1, 0)
                                    } else {
                                        if (abs(dragY) > 20f) act(0, if (dragY > 0) 1 else -1)
                                    }
                                }
                            ) { change, drag ->
                                change.consume()
                                dragX += drag.x
                                dragY += drag.y
                            }
                        }
                ) {
                    val cw = this.size.width / game.width
                    val ch = this.size.height / game.height
                    for (y in 0 until game.height) {
                        for (x in 0 until game.width) {
                            val i = y * game.width + x
                            val base = if (game.isWall(i)) CrateColors.Wall else if (game.isTarget(i)) CrateColors.Target else CrateColors.Floor
                            drawRect(base, Offset(x * cw, y * ch), Size(cw - 1f, ch - 1f))
                        }
                    }
                    for (c in game.crates) {
                        val x = c % game.width
                        val y = c / game.width
                        val onTarget = game.isTarget(c)
                        drawRoundRect(
                            color = if (onTarget) CrateColors.CrateOnTarget else CrateColors.Crate,
                            topLeft = Offset(x * cw + cw * 0.08f, y * ch + ch * 0.08f),
                            size = Size(cw * 0.84f, ch * 0.84f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cw * 0.15f, ch * 0.15f)
                        )
                    }
                    val px = game.playerPos % game.width
                    val py = game.playerPos / game.width
                    drawCircle(
                        color = CrateColors.Player,
                        radius = minOf(cw, ch) * 0.32f,
                        center = Offset(px * cw + cw / 2f, py * ch + ch / 2f)
                    )
                }
                if (settings.showFps) FpsOverlay(stats, Modifier.align(Alignment.TopStart))
            }
            DPad(
                onMove = ::act,
                onUndo = ::undo,
                canUndo = game.canUndo(),
                onRestart = { runId++ }
            )
        }

        if (game.isSolved) {
            Overlay {
                Text("SOLVED!", color = CrateColors.Good, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("${game.moves} moves  (${game.pushes} pushes)  -  par ${level.par}", color = Color.White, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (levelIndex + 1 < CRATE_LEVELS.size) CrateButton("Next level", primary = true) { onOpenLevel(levelIndex + 1) }
                    CrateButton("Retry") { runId++ }
                    CrateButton("Levels", onClick = onExit)
                }
            }
        }

        if (showMenu && !showSettings && !game.isSolved) {
            Overlay {
                Text("PAUSED", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                CrateButton("Resume", primary = true) { showMenu = false }
                CrateButton("Restart level") { runId++ }
                CrateButton("Settings") { showSettings = true }
                CrateButton("Level select", onClick = onExit)
            }
        }

        if (showSettings) {
            debugRev
            CrateSettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onClose = { showSettings = false },
                versionName = versionName,
                debugSections = runDebugSections(
                    game, repo, levelIndex,
                    onChanged = { debugRev++; frame++ },
                    onJump = { showSettings = false; onOpenLevel(it) }
                )
            )
        }
    }
}

@Composable
private fun DPad(onMove: (Int, Int) -> Unit, onUndo: () -> Unit, canUndo: Boolean, onRestart: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PadButton("▲") { onMove(0, -1) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PadButton("◀") { onMove(-1, 0) }
                PadButton("▶") { onMove(1, 0) }
            }
            PadButton("▼") { onMove(0, 1) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CrateButton("Undo", Modifier.fillMaxWidth(), enabled = canUndo, onClick = onUndo)
            CrateButton("Restart level", Modifier.fillMaxWidth(), onClick = onRestart)
        }
    }
}

@Composable
private fun PadButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).background(Color(0xFF3C3424), RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Overlay(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xB3000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 460.dp).background(CrateColors.Card, RoundedCornerShape(20.dp)).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
