package com.baraka.game.tunnelcrew.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.tunnelcrew.data.Cue
import com.baraka.game.tunnelcrew.data.CrewAudio
import com.baraka.game.tunnelcrew.data.CrewHaptics
import com.baraka.game.tunnelcrew.data.CrewRepository
import com.baraka.game.tunnelcrew.data.CrewSettings
import com.baraka.game.tunnelcrew.model.LEVELS
import com.baraka.game.tunnelcrew.model.Skill
import com.baraka.game.tunnelcrew.render.SkillIcon
import com.baraka.game.tunnelcrew.render.WorldFit
import com.baraka.game.tunnelcrew.render.drawCrewScene
import com.baraka.game.tunnelcrew.render.skyBrush
import com.baraka.game.tunnelcrew.systems.CrewSim
import com.baraka.game.tunnelcrew.systems.Ev
import com.baraka.game.tunnelcrew.systems.RESULT_RUNNING
import com.baraka.game.tunnelcrew.systems.RESULT_WON
import com.baraka.game.tunnelcrew.systems.TICKS_PER_SEC
import com.baraka.game.ui.debug.FpsOverlay
import com.baraka.game.ui.debug.FrameStats
import java.util.Arrays

private const val TICK_SEC = 1f / TICKS_PER_SEC
private val SPEEDS = floatArrayOf(1f, 2f, 4f)

/**
 * One level in play: owns the [CrewSim], runs its fixed-step loop off the display's frame clock,
 * turns taps into skill assignments and shows the HUD, skill bar and result. All gameplay rules
 * live in [CrewSim]; the drawing lives in `render/CrewRenderer.kt`.
 */
@Composable
fun CrewPlayScreen(
    levelIndex: Int,
    repo: CrewRepository,
    settings: CrewSettings,
    onSettingsChange: (CrewSettings) -> Unit,
    audio: CrewAudio,
    haptics: CrewHaptics,
    versionName: String,
    onOpenLevel: (Int) -> Unit,
    onExit: () -> Unit
) {
    val level = LEVELS[levelIndex]
    var runId by remember(levelIndex) { mutableIntStateOf(0) }
    val sim = remember(levelIndex, runId) { CrewSim(level) }
    val image = remember(sim) { sim.terrain.bitmap.asImageBitmap() }
    val fit = remember { WorldFit() }
    val stats = remember { FrameStats() }
    val frame = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current.density

    var started by remember(sim) { mutableStateOf(false) }
    var paused by remember(sim) { mutableStateOf(false) }
    var showMenu by remember(sim) { mutableStateOf(false) }
    var showSettings by remember(sim) { mutableStateOf(false) }
    var speedIdx by remember(sim) { mutableIntStateOf(0) }
    var slowMo by remember(sim) { mutableStateOf(false) }
    var selected by remember(sim) { mutableIntStateOf(level.skills.indexOfFirst { it > 0 }.coerceAtLeast(0)) }
    var nukeArmed by remember(sim) { mutableStateOf(false) }
    var debugRev by remember(sim) { mutableIntStateOf(0) }
    var skillRev by remember(sim) { mutableIntStateOf(0) }
    var hudAlive by remember(sim) { mutableIntStateOf(0) }
    var hudSaved by remember(sim) { mutableIntStateOf(0) }
    var hudTime by remember(sim) { mutableIntStateOf(level.timeSec) }
    var hudRate by remember(sim) { mutableIntStateOf(sim.rate) }
    var result by remember(sim) { mutableIntStateOf(RESULT_RUNNING) }
    var recorded by remember(sim) { mutableStateOf(false) }

    var canvasW by remember { mutableIntStateOf(0) }
    var canvasH by remember { mutableIntStateOf(0) }
    val sky = remember(level, canvasW, canvasH) { fit.update(canvasW.toFloat(), canvasH.toFloat()); skyBrush(level.theme, fit) }
    val stepping by rememberUpdatedState(started && !paused && !showMenu && !showSettings && result == RESULT_RUNNING)
    val speedFactor by rememberUpdatedState(SPEEDS[speedIdx] * (if (slowMo) 0.25f else 1f))
    val showFps by rememberUpdatedState(settings.showFps)

    LaunchedEffect(nukeArmed) {
        if (nukeArmed) {
            kotlinx.coroutines.delay(3000)
            nukeArmed = false
        }
    }

    LaunchedEffect(sim) {
        var last = 0L
        var acc = 0f
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    if (showFps) stats.record(dt * 1000f)
                    if (stepping) {
                        acc += dt * speedFactor
                        var steps = 0
                        while (acc >= TICK_SEC && steps < 12) {
                            sim.step()
                            acc -= TICK_SEC
                            steps++
                        }
                        if (steps == 12) acc = 0f
                    }
                    sim.terrain.flush()
                    drainEvents(sim, audio, haptics)
                    val alive = sim.alive
                    if (hudAlive != alive) hudAlive = alive
                    if (hudSaved != sim.saved) hudSaved = sim.saved
                    if (hudRate != sim.rate) hudRate = sim.rate
                    val secs = (sim.timeLeft.coerceAtLeast(0) + TICKS_PER_SEC - 1) / TICKS_PER_SEC
                    if (hudTime != secs) hudTime = secs
                    if (sim.result != result) {
                        result = sim.result
                        if (!recorded) {
                            recorded = true
                            repo.recordResult(levelIndex, sim.saved)
                            val cue = if (result == RESULT_WON) Cue.WIN else Cue.LOSE
                            audio.play(cue)
                            haptics.cue(cue)
                        }
                    }
                }
                last = now
                frame.intValue++
            }
        }
    }

    fun tap(px: Float, py: Float) {
        if (result != RESULT_RUNNING || !started || showMenu || showSettings) return
        val skill = Skill.entries[selected]
        val wx = (px - fit.ox) / fit.scale
        val wy = (py - fit.oy) / fit.scale
        val radius = maxOf(9f, 30f * density / fit.scale)
        val idx = sim.pick(wx, wy, radius, skill)
        if (idx >= 0 && sim.assign(idx, skill)) {
            audio.play(Cue.ASSIGN)
            skillRev++
        } else {
            audio.play(Cue.REFUSE)
        }
    }

    BackHandler(enabled = !showSettings) { showMenu = !showMenu }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0F18))) {
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ControlPanel(
                alive = hudAlive,
                spawnedAll = sim.totalCrew,
                saved = hudSaved,
                needed = level.needed,
                seconds = hudTime,
                paused = paused,
                speedLabel = if (speedIdx == 0) "x1" else "x${SPEEDS[speedIdx].toInt()}",
                nukeArmed = nukeArmed,
                rate = hudRate,
                minRate = sim.minRate,
                onRateDown = { sim.slower(); hudRate = sim.rate },
                onRateUp = { sim.faster(); hudRate = sim.rate },
                onPause = { paused = !paused },
                onSpeed = { speedIdx = (speedIdx + 1) % SPEEDS.size },
                onNuke = { if (nukeArmed) { sim.nuke(); nukeArmed = false } else nukeArmed = true },
                onMenu = { showMenu = true }
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasW = it.width; canvasH = it.height; fit.update(it.width.toFloat(), it.height.toFloat()) }
                        .pointerInput(sim) { detectTapGestures { tap(it.x, it.y) } }
                ) {
                    frame.intValue
                    fit.update(size.width, size.height)
                    drawCrewScene(sim, image, sky, fit)
                }
                if (paused && result == RESULT_RUNNING) {
                    Text("PAUSED - you can still give orders", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp).background(Color(0x99000000), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                }
                if (showFps) FpsOverlay(stats, Modifier.align(Alignment.BottomStart).padding(4.dp))
            }
            SkillPanel(sim, selected, skillRev) { selected = it }
        }

        if (!started) {
            Overlay {
                Text("LEVEL ${levelIndex + 1}", color = CrewColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(level.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(level.hint, color = CrewColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("Save ${level.needed} of ${level.total}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                CrewButton("Start", primary = true) { started = true }
                CrewButton("Levels", onClick = onExit)
            }
        }

        if (result != RESULT_RUNNING) {
            val won = result == RESULT_WON
            Overlay {
                Text(if (won) "LEVEL COMPLETE" else "LEVEL FAILED", color = if (won) CrewColors.Good else CrewColors.Bad, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Saved ${sim.saved} of ${level.total}  (needed ${level.needed})", color = Color.White, fontSize = 16.sp)
                if (!won && sim.timeLeft <= 0) Text("Time ran out.", color = CrewColors.Muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (won && levelIndex + 1 < LEVELS.size) CrewButton("Next level", primary = true) { onOpenLevel(levelIndex + 1) }
                    CrewButton("Retry", primary = !won) { runId++ }
                    CrewButton("Levels", onClick = onExit)
                }
            }
        }

        if (showMenu && !showSettings && result == RESULT_RUNNING) {
            Overlay {
                Text("PAUSED", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                CrewButton("Resume", primary = true) { showMenu = false }
                CrewButton("Restart level") { runId++ }
                CrewButton("Settings") { showSettings = true }
                CrewButton("Level select", onClick = onExit)
            }
        }

        if (showSettings) {
            debugRev
            CrewSettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onClose = { showSettings = false },
                versionName = versionName,
                debugSections = runDebugSections(
                    sim, repo,
                    onChanged = { debugRev++; skillRev++ },
                    slowMo = slowMo,
                    onSlowMo = { slowMo = it; debugRev++ },
                    onJump = { showSettings = false; onOpenLevel(it) }
                )
            )
        }
    }
}

private fun drainEvents(sim: CrewSim, audio: CrewAudio, haptics: CrewHaptics) {
    val ev = sim.events
    if (ev[Ev.BOOM] > 0) { audio.play(Cue.BOOM); haptics.cue(Cue.BOOM) }
    else if (ev[Ev.SPLAT] > 0) { audio.play(Cue.SPLAT); haptics.cue(Cue.SPLAT) }
    if (ev[Ev.SAVE] > 0) audio.play(Cue.SAVE)
    if (ev[Ev.CLINK] > 0) audio.play(Cue.CLINK)
    if (ev[Ev.HATCH] > 0) audio.play(Cue.HATCH)
    if (ev[Ev.FLAME] > 0) audio.play(Cue.FLAME)
    if (ev[Ev.WARN] > 0) audio.play(Cue.WARN)
    Arrays.fill(ev, 0)
}

@Composable
private fun Overlay(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xB3000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 460.dp).background(CrewColors.Card, RoundedCornerShape(20.dp)).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun Chip(text: String, modifier: Modifier = Modifier, selected: Boolean = false, color: Color = Color(0xFF2E3647), onClick: () -> Unit) {
    Box(
        modifier
            .height(36.dp)
            .background(if (selected) CrewColors.Accent else color, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) CrewColors.OnAccent else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Left column: crew counts, timer and the run controls. Lives beside the map so the map can use the full screen height. */
@Composable
private fun ControlPanel(
    alive: Int,
    spawnedAll: Int,
    saved: Int,
    needed: Int,
    seconds: Int,
    paused: Boolean,
    speedLabel: String,
    nukeArmed: Boolean,
    rate: Int,
    minRate: Int,
    onRateDown: () -> Unit,
    onRateUp: () -> Unit,
    onPause: () -> Unit,
    onSpeed: () -> Unit,
    onNuke: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        Modifier.width(92.dp).fillMaxHeight().background(CrewColors.Bar).verticalScroll(rememberScrollState()).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Chip("Menu", Modifier.fillMaxWidth(), onClick = onMenu)
        Text("%d:%02d".format(seconds / 60, seconds % 60), color = if (seconds < 30) CrewColors.Bad else Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("Out $alive/$spawnedAll", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Saved", color = CrewColors.Muted, fontSize = 11.sp)
        Text("$saved / $needed", color = CrewColors.Good, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text("Release rate", color = CrewColors.Muted, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip("-", Modifier.weight(1f), onClick = onRateDown)
            Text("$rate", color = if (rate > minRate) CrewColors.Accent else Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Chip("+", Modifier.weight(1f), onClick = onRateUp)
        }
        Chip(if (paused) "Resume" else "Pause", Modifier.fillMaxWidth(), selected = paused, onClick = onPause)
        Chip(speedLabel, Modifier.fillMaxWidth(), onClick = onSpeed)
        Chip(if (nukeArmed) "Sure?" else "Nuke", Modifier.fillMaxWidth(), color = CrewColors.Danger, onClick = onNuke)
    }
}

/** Right panel: one icon button per skill the level offers. One column up to 5 skills, two columns beyond that; scrolls only if it still does not fit. */
@Composable
private fun SkillPanel(sim: CrewSim, selected: Int, rev: Int, onSelect: (Int) -> Unit) {
    rev
    var offered = 0
    for (skill in Skill.entries) if (isOffered(sim, skill)) offered++
    val columns = if (offered > 5) 2 else 1
    Column(
        Modifier.width((84 * columns + 8).dp).fillMaxHeight().background(CrewColors.Bar).verticalScroll(rememberScrollState()).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val list = Skill.entries.filter { isOffered(sim, it) }
        for (row in list.chunked(columns)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (skill in row) SkillButton(sim, skill, selected == skill.ordinal, Modifier.weight(1f)) { onSelect(skill.ordinal) }
                if (row.size < columns) Box(Modifier.weight(1f))
            }
        }
    }
}

private fun isOffered(sim: CrewSim, skill: Skill): Boolean =
    sim.level.skills[skill.ordinal] > 0 || sim.skills[skill.ordinal] > 0 || sim.infiniteSkills

@Composable
private fun SkillButton(sim: CrewSim, skill: Skill, isSel: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val count = sim.skills[skill.ordinal]
    val usable = count > 0 || sim.infiniteSkills
    val fg = if (isSel) Color(0xFF10131A) else if (usable) Color.White else Color(0xFF7C8596)
    Column(
        modifier
            .background(if (isSel) skill.color else Color(0xFF262D3D), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SkillIcon(skill, tint = if (isSel || !usable) fg else skill.color, size = 28.dp)
        Text(skill.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(if (sim.infiniteSkills) "inf" else "$count", color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}
