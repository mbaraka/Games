package com.baraka.game.trainsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baraka.game.trainsurvivor.data.GameSettings
import com.baraka.game.trainsurvivor.data.Haptics
import com.baraka.game.trainsurvivor.data.MusicTrack
import com.baraka.game.trainsurvivor.data.TrainAudio
import com.baraka.game.trainsurvivor.data.TrainSaveRepository
import com.baraka.game.trainsurvivor.data.TrainSoundCue
import com.baraka.game.trainsurvivor.model.AUTO_WEAPON
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.Progression
import com.baraka.game.trainsurvivor.model.RunDelta
import com.baraka.game.trainsurvivor.model.RunRecord
import com.baraka.game.trainsurvivor.model.SURVIVORS
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.render.drawTrainScene
import com.baraka.game.trainsurvivor.systems.TrainGame
import com.baraka.game.ui.debug.FpsOverlay
import com.baraka.game.ui.debug.FrameStats
import kotlin.math.abs
import kotlin.math.max

private val PANEL = Color(0xFF1B1F29)
private val AMBER = Color(0xFFFFB300)

private class RunResult(
    val distance: Int,
    val kills: Int,
    val bossKills: Int,
    val coins: Int,
    val killCoins: Int,
    val isBest: Boolean,
    val goalsReady: Boolean
)

/**
 * Two phases: the garage (spend coins on the train and its passengers) and the run
 * (survive as long as possible). Coins are banked the moment a run ends, win or lose.
 * This composable also owns audio, haptics and settings so both phases share them.
 */
@Composable
fun TrainSurvivorScreen() {
    val context = LocalContext.current
    val repository = remember { TrainSaveRepository.getInstance(context) }
    var save by remember { mutableStateOf(Progression.sync(repository.load())) }
    var settings by remember { mutableStateOf(repository.loadSettings()) }
    var inRun by remember { mutableStateOf(false) }

    val audio = remember { TrainAudio() }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    SideEffect {
        audio.musicVolume = settings.musicVolume
        audio.sfxVolume = settings.sfxVolume
        audio.ambienceVolume = settings.ambienceVolume
    }
    val haptics = remember { Haptics(context) }
    SideEffect { haptics.enabled = settings.vibration }
    val playCue: (TrainSoundCue) -> Unit = remember { { cue -> audio.play(cue); haptics.cue(cue) } }

    // Music and ambience stop while the app is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) audio.pauseAll()
            if (event == Lifecycle.Event.ON_START) audio.resume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(inRun) {
        if (inRun) {
            audio.setMusic(MusicTrack.RUN)
            audio.setAmbience(true, 1f)
        } else {
            audio.setAmbience(false)
            audio.setMusic(MusicTrack.GARAGE)
        }
    }

    val onSettingsChange: (GameSettings) -> Unit = { settings = it; repository.saveSettings(it) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    if (inRun) {
        RunScreen(
            repository = repository,
            settings = settings,
            onSettingsChange = onSettingsChange,
            versionName = versionName,
            audio = audio,
            playCue = playCue,
            onSaved = { save = it },
            onExit = { inRun = false }
        )
    } else {
        GarageScreen(
            save = save,
            settings = settings,
            onSettingsChange = onSettingsChange,
            versionName = versionName,
            onPurchase = {
                save = it
                repository.save(it)
                playCue(TrainSoundCue.UPGRADE)
            },
            onDepart = { inRun = true },
            debugSections = garageDebugSections(
                save = save,
                onSave = { save = it; repository.save(it) },
                onReset = { repository.resetProgress(); save = Progression.sync(repository.load()) }
            )
        )
    }
}

@Composable
private fun RunScreen(
    repository: TrainSaveRepository,
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    versionName: String,
    audio: TrainAudio,
    playCue: (TrainSoundCue) -> Unit,
    onSaved: (SaveData) -> Unit,
    onExit: () -> Unit
) {
    var runId by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RunResult?>(null) }
    var godMode by remember { mutableStateOf(false) }
    var timeScale by remember { mutableFloatStateOf(1f) }
    val tick = remember { mutableLongStateOf(0L) }
    val stats = remember { FrameStats() }
    val game = remember(runId) { TrainGame(repository.load()).also { it.onSound = playCue; it.godMode = godMode } }
    var banked by remember(runId) { mutableStateOf(false) }
    val bestAtStart = remember(runId) { repository.load().bestDistance }
    // What this run has already written to disk, so every checkpoint only adds the difference:
    // coins, kills, bosses, flyers, routes.
    val written = remember(runId) { IntArray(5) }

    /** Writes this run's progress so far to disk and returns the new save. Safe to call repeatedly. */
    fun checkpoint(final: Boolean): SaveData {
        val current = repository.load()
        val dKills = game.kills - written[1]
        val dBosses = game.bossKills - written[2]
        val dFlyers = game.flyerKills - written[3]
        val dRoutes = game.routesTaken - written[4]
        var updated = current.copy(
            coins = current.coins + game.finalCoins - written[0],
            totalKills = current.totalKills + dKills,
            bossKills = current.bossKills + dBosses,
            flyerKills = current.flyerKills + dFlyers,
            routesTaken = current.routesTaken + dRoutes,
            bestDistance = max(current.bestDistance, game.distanceMeters)
        )
        updated = Progression.applyProgress(updated, RunDelta(dKills, dFlyers, dBosses, dRoutes, game.finalCoins, game.distanceMeters))
        if (final) updated = Progression.addRecord(updated, RunRecord(game.distanceMeters, game.kills, game.finalCoins, Progression.today()))
        written[0] = game.finalCoins
        written[1] = game.kills
        written[2] = game.bossKills
        written[3] = game.flyerKills
        written[4] = game.routesTaken
        repository.save(updated)
        onSaved(updated)
        return updated
    }

    fun bank() {
        if (banked) return
        banked = true
        val updated = checkpoint(final = true)
        result = RunResult(
            distance = game.distanceMeters,
            kills = game.kills,
            bossKills = game.bossKills,
            coins = game.finalCoins,
            killCoins = game.runCoins,
            isBest = game.distanceMeters > bestAtStart,
            goalsReady = Progression.hasClaimable(updated)
        )
    }

    // Leaving mid-run (home button, activity closed) still keeps everything earned so far.
    DisposableEffect(game) {
        onDispose { if (!banked && game.time > 2f) checkpoint(final = true) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, game) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                paused = true
                if (!banked) checkpoint(final = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = !game.over && !showSettings) { paused = !paused }

    val currentSettings by rememberUpdatedState(settings)
    val currentTimeScale by rememberUpdatedState(timeScale)

    LaunchedEffect(runId) {
        audio.setMusic(MusicTrack.RUN)
        audio.setAmbience(true, 1f)
    }
    LaunchedEffect(paused) { if (paused) audio.setAmbience(false) else if (!game.over) audio.setAmbience(true, game.speedFactor) }

    LaunchedEffect(game, paused) {
        if (paused) return@LaunchedEffect
        var last = 0L
        var bossMusic = false
        var lastSpeed = 1f
        var endedAudio = false
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val real = (now - last) / 1_000_000_000f
                    stats.record(real * 1000f)
                    game.damageNumbers = currentSettings.damageNumbers
                    game.shakeEnabled = currentSettings.screenShake
                    game.update(real.coerceAtMost(0.05f) * currentTimeScale)
                }
                last = now
                tick.longValue++
                if (game.over && !banked) bank()
                if (game.over) {
                    if (!endedAudio) {
                        endedAudio = true
                        audio.setAmbience(false)
                        audio.setMusic(null)
                    }
                } else {
                    val wantBoss = game.boss != null
                    if (wantBoss != bossMusic) {
                        bossMusic = wantBoss
                        audio.setMusic(if (wantBoss) MusicTrack.BOSS else MusicTrack.RUN)
                    }
                    if (abs(game.speedFactor - lastSpeed) > 0.03f) {
                        lastSpeed = game.speedFactor
                        audio.setAmbience(true, lastSpeed)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { game.worldW = 720f * it.width / it.height.coerceAtLeast(1) }
                .pointerInput(game) {
                    detectTapGestures { offset ->
                        val scale = size.width / game.worldW
                        game.tapAt(offset.x / scale, offset.y / scale)
                    }
                }
        ) {
            tick.longValue
            drawTrainScene(game)
        }

        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)) {
            if (!game.over) {
                RoundButton(
                    label = "II",
                    modifier = Modifier.align(Alignment.TopEnd),
                    size = 40,
                    onClick = { paused = true }
                )
                Row(modifier = Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (game.fieldRepairLevel > 0) {
                        AbilityButton(tick, "REPAIR", Color(0xFF2E7D32), { game.fieldRepairReady }, { 1f - game.fieldRepairCooldown / game.fieldRepairCooldownMax }, { game.fieldRepairCooldown }) { game.useFieldRepair() }
                    }
                    if (game.overdriveLevel > 0) {
                        AbilityButton(tick, "BOOST", Color(0xFFEF6C00), { game.overdriveReady }, { if (game.overdriveTime > 0f) game.overdriveTime / game.overdriveDuration else 1f - game.overdriveCooldown / (game.overdriveCooldownMax + game.overdriveDuration) }, { if (game.overdriveTime > 0f) game.overdriveTime else game.overdriveCooldown }, activeNow = { game.overdriveTime > 0f }) { game.useOverdrive() }
                    }
                    AbilityButton(tick, "BLAST", Color(0xFFB71C1C), { game.dynamiteReady }, { 1f - game.dynamiteCooldown / game.dynamiteCooldownMax }, { game.dynamiteCooldown }) { game.useDynamite() }
                }
                JunctionPanel(game, tick, Modifier.align(Alignment.BottomCenter))
                WeaponSwitcher(game, tick, Modifier.align(Alignment.CenterStart).padding(top = 44.dp))
                if (settings.showFps) FpsOverlay(stats, Modifier.align(Alignment.BottomStart))
            }
        }

        if (paused && !game.over && !showSettings) {
            Overlay {
                Text("PAUSED", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { paused = false }) { Text("Resume") }
                Button(
                    onClick = { showSettings = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3342), contentColor = Color.White)
                ) { Text("Settings") }
                Button(
                    onClick = { game.forceEnd(); paused = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E2A2A))
                ) { Text("End run & bank coins") }
            }
        }

        val finished = result
        if (game.over && finished != null) {
            Overlay {
                Text("TRAIN DESTROYED", color = Color(0xFFFF5252), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (finished.isBest) Text("New best distance!", color = AMBER, fontWeight = FontWeight.Bold)
                Text("${finished.distance} m   •   ${finished.kills} zombies   •   ${finished.bossKills} bosses", color = Color.White, fontSize = 15.sp)
                Text("+${finished.coins} coins banked (${finished.killCoins} from kills)", color = AMBER, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (finished.goalsReady) Text("A goal or achievement is ready to claim in the Garage!", color = Color(0xFF66BB6A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = AMBER, contentColor = Color.Black)
                    ) { Text("Upgrade train", fontWeight = FontWeight.Bold) }
                    Button(onClick = { runId++; result = null; paused = false }) { Text("Retry") }
                }
            }
        }

        if (showSettings) {
            TrainSettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onClose = { showSettings = false },
                versionName = versionName,
                debugSections = runDebugSections(
                    game = game,
                    godMode = godMode,
                    onGodMode = { godMode = it; game.godMode = it },
                    timeScale = timeScale,
                    onTimeScale = { timeScale = it }
                )
            )
        }
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.background(PANEL, RoundedCornerShape(18.dp)).padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { content() }
    }
}

@Composable
private fun RoundButton(label: String, modifier: Modifier, size: Int, onClick: () -> Unit) {
    Box(
        modifier = modifier.size(size.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

/**
 * One chip per passenger. Tap to cycle that passenger between AUTO (they pick the best weapon for the
 * enemies in front of them) and each weapon they own, so you can take over when you know better.
 */
@Composable
private fun WeaponSwitcher(game: TrainGame, tick: MutableLongState, modifier: Modifier) {
    tick.longValue
    // Compact two-line chips, scrollable in case a small screen can't show all five.
    Column(
        modifier = modifier.heightIn(max = 250.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (i in 0 until game.slotCount) {
            val g = game.gunners[i]
            val auto = g.mode == AUTO_WEAPON
            val accent = if (!g.alive) Color(0xFF616161) else if (auto) Color(0xFF66BB6A) else Color(0xFFFFB300)
            Column(
                modifier = Modifier
                    .width(108.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.5.dp, accent, RoundedCornerShape(9.dp))
                    .clickable(enabled = g.alive) { game.cycleWeapon(i) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    SURVIVORS[i].name + if (!g.alive) "  LOST" else if (auto) "  AUTO" else "  MANUAL",
                    color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (!g.alive) "-" else if (g.isHot) g.weapon.displayName.substringBefore(' ') + " HOT" else g.weapon.displayName.substringBefore(' '),
                        color = if (g.isHot) Color(0xFFFF5252) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                    if (g.alive) {
                        val dmg = game.shotDamage(i).toInt()
                        val bonus = game.damageBonusPercent(i)
                        val line = remember(dmg, bonus) { if (bonus != 0) "$dmg +$bonus%" else "$dmg" }
                        Text(line, color = if (bonus > 0) Color(0xFFFFD54F) else Color(0xFFB0BEC5), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Shown while a fork is ahead and no route has been picked yet: two route cards plus a countdown. */
@Composable
private fun JunctionPanel(game: TrainGame, tick: MutableLongState, modifier: Modifier) {
    tick.longValue
    val junction = game.junction ?: return
    if (junction.chosen != null) return
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "JUNCTION AHEAD - choose your route (${kotlin.math.ceil(junction.timeLeft).toInt()}s)",
            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RouteCard(junction.optionA) { game.chooseRoute(junction.optionA) }
            RouteCard(junction.optionB) { game.chooseRoute(junction.optionB) }
        }
    }
}

@Composable
private fun RouteCard(biome: Biome, onClick: () -> Unit) {
    val accent = Color(biome.accent)
    Column(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1F29))
            .border(2.dp, accent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(biome.displayName, color = accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Text(biome.tagline, color = Color(0xFFB0B8C6), fontSize = 10.sp, maxLines = 2)
        Text("Enemy: " + biome.signature.displayName, color = Color(0xFF81C784), fontSize = 10.sp)
        val coins = "%.2f".format(biome.coinMult).trimEnd('0').trimEnd('.')
        Text("Coins x$coins", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** A round active-ability button with a cooldown ring; [activeNow] highlights an ability that is currently running. */
@Composable
private fun AbilityButton(
    tick: MutableLongState,
    label: String,
    color: Color,
    ready: () -> Boolean,
    progress: () -> Float,
    secondsLeft: () -> Float,
    activeNow: () -> Boolean = { false },
    onClick: () -> Unit
) {
    tick.longValue
    val isReady = ready()
    val active = activeNow()
    val fraction = if (isReady) 1f else progress().coerceIn(0f, 1f)
    Box(
        modifier = Modifier.size(72.dp).clip(CircleShape)
            .background(if (isReady) color else if (active) Color(0xFF8D5A00) else Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = isReady, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(3.dp)) {
            drawArc(
                color = if (isReady || active) Color(0xFFFFD54F) else Color(0xFF9E9E9E),
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                style = Stroke(5f)
            )
        }
        Text(if (isReady) label else "${secondsLeft().toInt() + 1}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
