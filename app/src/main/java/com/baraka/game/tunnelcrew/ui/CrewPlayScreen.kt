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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    var result by remember(sim) { mutableIntStateOf(RESULT_RUNNING) }
    var recorded by remember(sim) { mutableStateOf(false) }

    val sky = remember(level) { skyBrush(level.skyTop, level.skyBottom, 1200f) }
    val stepping by rememberUpdatedState(started && !paused && !showMenu && !showSettings && result == RESULT_RUNNING)
    val speedFactor by rememberUpdatedState(SPEEDS[speedIdx] * (if (slowMo) 0.25f else 1f))
    val showFps by rememberUpdatedState(settings.showFps)
    val canvasSize = remember { IntArray(2) }

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
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopBar(
                alive = hudAlive,
                spawnedAll = sim.totalCrew,
                saved = hudSaved,
                needed = level.needed,
                seconds = hudTime,
                paused = paused,
                speedLabel = if (speedIdx == 0) "x1" else "x${SPEEDS[speedIdx].toInt()}",
                nukeArmed = nukeArmed,
                onPause = { paused = !paused },
                onSpeed = { speedIdx = (speedIdx + 1) % SPEEDS.size },
                onNuke = { if (nukeArmed) { sim.nuke(); nukeArmed = false } else nukeArmed = true },
                onMenu = { showMenu = true }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize[0] = it.width; canvasSize[1] = it.height; fit.update(it.width.toFloat(), it.height.toFloat()) }
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
            SkillBar(sim, selected, skillRev) { selected = it }
        }

        if (!started) {
            Overlay {
                Text("LEVEL ${levelIndex + 1}", color = CREW_ACCENT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(level.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(level.hint, color = CREW_MUTED, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("Save ${level.needed} of ${level.total}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Button(onClick = { started = true }) { Text("Start") }
                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD)) { Text("Levels") }
            }
        }

        if (result != RESULT_RUNNING) {
            val won = result == RESULT_WON
            Overlay {
                Text(if (won) "LEVEL COMPLETE" else "LEVEL FAILED", color = if (won) Color(0xFF69F0AE) else Color(0xFFEF5350), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Saved ${sim.saved} of ${level.total}  (needed ${level.needed})", color = Color.White, fontSize = 16.sp)
                if (!won && sim.timeLeft <= 0) Text("Time ran out.", color = CREW_MUTED, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (won && levelIndex + 1 < LEVELS.size) Button(onClick = { onOpenLevel(levelIndex + 1) }) { Text("Next level") }
                    Button(onClick = { runId++ }, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD)) { Text("Retry") }
                    Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD)) { Text("Levels") }
                }
            }
        }

        if (showMenu && !showSettings && result == RESULT_RUNNING) {
            Overlay {
                Text("PAUSED", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Button(onClick = { showMenu = false }) { Text("Resume") }
                Button(onClick = { runId++ }, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD)) { Text("Restart level") }
                Button(onClick = { showSettings = true }, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD)) { Text("Settings") }
                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD)) { Text("Level select") }
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
    Arrays.fill(ev, 0)
}

@Composable
private fun Overlay(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 460.dp).background(CREW_CARD, RoundedCornerShape(16.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun Chip(text: String, selected: Boolean = false, color: Color = CREW_CARD, onClick: () -> Unit) {
    Box(
        Modifier
            .height(34.dp)
            .background(if (selected) CREW_ACCENT else color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopBar(
    alive: Int,
    spawnedAll: Int,
    saved: Int,
    needed: Int,
    seconds: Int,
    paused: Boolean,
    speedLabel: String,
    nukeArmed: Boolean,
    onPause: () -> Unit,
    onSpeed: () -> Unit,
    onNuke: () -> Unit,
    onMenu: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF141922)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Chip("Menu", onClick = onMenu)
        Text("Out $alive/$spawnedAll", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Saved $saved/$needed", color = Color(0xFF69F0AE), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("%d:%02d".format(seconds / 60, seconds % 60), color = if (seconds < 30) Color(0xFFEF5350) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f))
        Chip(if (paused) "Resume" else "Pause", selected = paused, onClick = onPause)
        Chip(speedLabel, onClick = onSpeed)
        Chip(if (nukeArmed) "Sure?" else "Nuke", color = Color(0xFF7B1F1F), onClick = onNuke)
    }
}

@Composable
private fun SkillBar(sim: CrewSim, selected: Int, rev: Int, onSelect: (Int) -> Unit) {
    rev
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF141922)).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        for (skill in Skill.entries) {
            val offered = sim.level.skills[skill.ordinal] > 0 || sim.skills[skill.ordinal] > 0 || sim.infiniteSkills
            if (!offered) continue
            val count = sim.skills[skill.ordinal]
            val usable = count > 0 || sim.infiniteSkills
            val isSel = selected == skill.ordinal
            Column(
                Modifier
                    .widthIn(min = 58.dp)
                    .background(if (isSel) skill.color else Color(0xFF232A38), RoundedCornerShape(8.dp))
                    .clickable { onSelect(skill.ordinal) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val textColor = if (isSel) Color.Black else if (usable) Color.White else Color(0xFF626B7A)
                Text(skill.label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (sim.infiniteSkills) "inf" else "$count", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
