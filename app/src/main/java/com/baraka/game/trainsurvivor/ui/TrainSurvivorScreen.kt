package com.baraka.game.trainsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import com.baraka.game.trainsurvivor.model.AUTO_WEAPON
import com.baraka.game.trainsurvivor.model.Biome
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baraka.game.trainsurvivor.data.TrainSaveRepository
import com.baraka.game.trainsurvivor.data.TrainSoundCue
import com.baraka.game.trainsurvivor.data.TrainSoundEngine
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.render.drawTrainScene
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.max

private val PANEL = Color(0xFF1B1F29)
private val AMBER = Color(0xFFFFB300)

private class RunResult(val distance: Int, val kills: Int, val bossKills: Int, val coins: Int, val killCoins: Int, val isBest: Boolean)

/**
 * Two phases: the garage (spend coins on the train and its passengers) and the run
 * (survive as long as possible). Coins are banked the moment a run ends, win or lose.
 */
@Composable
fun TrainSurvivorScreen() {
    val context = LocalContext.current
    val repository = remember { TrainSaveRepository.getInstance(context) }
    var save by remember { mutableStateOf(repository.load()) }
    var soundEnabled by remember { mutableStateOf(repository.isSoundEnabled()) }
    var inRun by remember { mutableStateOf(false) }

    val soundEngine = remember { TrainSoundEngine() }
    DisposableEffect(Unit) { onDispose { soundEngine.release() } }
    val soundOn by rememberUpdatedState(soundEnabled)
    val playSound: (TrainSoundCue) -> Unit = remember { { cue -> if (soundOn) soundEngine.play(cue) } }

    if (inRun) {
        RunScreen(
            startSave = save,
            repository = repository,
            playSound = playSound,
            onSaved = { save = it },
            onExit = { inRun = false }
        )
    } else {
        GarageScreen(
            save = save,
            soundEnabled = soundEnabled,
            onSoundToggle = {
                soundEnabled = it
                repository.setSoundEnabled(it)
            },
            onPurchase = {
                save = it
                repository.save(it)
                playSound(TrainSoundCue.UPGRADE)
            },
            onDepart = { inRun = true }
        )
    }
}

@Composable
private fun RunScreen(
    startSave: SaveData,
    repository: TrainSaveRepository,
    playSound: (TrainSoundCue) -> Unit,
    onSaved: (SaveData) -> Unit,
    onExit: () -> Unit
) {
    var runId by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RunResult?>(null) }
    val tick = remember { mutableLongStateOf(0L) }
    val game = remember(runId) { TrainGame(repository.load()).also { it.onSound = playSound } }
    var banked by remember(runId) { mutableStateOf(false) }
    val bestAtStart = remember(runId) { repository.load().bestDistance }
    // What this run has already written to disk, so every checkpoint only adds the difference.
    val written = remember(runId) { intArrayOf(0, 0, 0) }

    /** Writes this run's progress so far (coins, kills, bosses, best distance) to disk. Safe to call repeatedly. */
    fun checkpoint() {
        val current = repository.load()
        val updated = current.copy(
            coins = current.coins + game.finalCoins - written[0],
            totalKills = current.totalKills + game.kills - written[1],
            bossKills = current.bossKills + game.bossKills - written[2],
            bestDistance = max(current.bestDistance, game.distanceMeters)
        )
        written[0] = game.finalCoins
        written[1] = game.kills
        written[2] = game.bossKills
        repository.save(updated)
        onSaved(updated)
    }

    fun bank() {
        if (banked) return
        banked = true
        checkpoint()
        result = RunResult(
            distance = game.distanceMeters,
            kills = game.kills,
            bossKills = game.bossKills,
            coins = game.finalCoins,
            killCoins = game.runCoins,
            isBest = game.distanceMeters > bestAtStart
        )
    }

    // Leaving mid-run (home button, activity closed) still keeps everything earned so far.
    DisposableEffect(game) {
        onDispose { if (!banked) checkpoint() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, game) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { paused = true; if (!banked) checkpoint() } }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = !game.over) { paused = !paused }

    LaunchedEffect(game, paused) {
        if (paused) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) game.update(((now - last) / 1_000_000_000f).coerceAtMost(0.05f))
                last = now
                tick.longValue++
                if (game.over && !banked) bank()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { game.worldW = 720f * it.width / it.height.coerceAtLeast(1) }
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
                DynamiteButton(game, tick, Modifier.align(Alignment.BottomEnd))
                JunctionPanel(game, tick, Modifier.align(Alignment.BottomCenter))
                WeaponSwitcher(game, tick, Modifier.align(Alignment.CenterStart).padding(top = 44.dp))
            }
        }

        if (paused && !game.over) {
            Overlay {
                Text("PAUSED", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { paused = false }) { Text("Resume") }
                Button(
                    onClick = { game.hp = 0f; paused = false },
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = AMBER, contentColor = Color.Black)
                    ) { Text("Upgrade train", fontWeight = FontWeight.Bold) }
                    Button(onClick = { runId++; result = null; paused = false }) { Text("Retry") }
                }
            }
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
private fun WeaponSwitcher(game: TrainGame, tick: androidx.compose.runtime.MutableLongState, modifier: Modifier) {
    tick.longValue
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until game.slotCount) {
            val g = game.gunners[i]
            val auto = g.mode == AUTO_WEAPON
            Column(
                modifier = Modifier
                    .width(78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.5.dp, if (auto) Color(0xFF66BB6A) else Color(0xFFFFB300), RoundedCornerShape(10.dp))
                    .clickable { game.cycleWeapon(i) }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    (if (i == 0) "Driver" else "Pass. ${i + 1}") + if (auto) "  AUTO" else "  MANUAL",
                    color = if (auto) Color(0xFF66BB6A) else Color(0xFFFFB300), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
                Text(g.weapon.displayName.substringBefore(' '), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/** Shown while a fork is ahead and no route has been picked yet: two route cards plus a countdown. */
@Composable
private fun JunctionPanel(game: TrainGame, tick: androidx.compose.runtime.MutableLongState, modifier: Modifier) {
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
        Text(biome.tagline, color = Color(0xFFB0B8C6), fontSize = 10.sp)
        val coins = "%.2f".format(biome.coinMult).trimEnd('0').trimEnd('.')
        Text("Coins x$coins", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DynamiteButton(game: TrainGame, tick: androidx.compose.runtime.MutableLongState, modifier: Modifier) {
    tick.longValue
    val ready = game.dynamiteReady
    val progress = if (ready) 1f else 1f - game.dynamiteCooldown / game.dynamiteCooldownMax
    Box(
        modifier = modifier.size(76.dp).clip(CircleShape)
            .background(if (ready) Color(0xFFB71C1C) else Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = ready) { game.useDynamite() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(3.dp)) {
            drawArc(
                color = if (ready) Color(0xFFFFD54F) else Color(0xFF9E9E9E),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                style = Stroke(5f)
            )
        }
        Text(if (ready) "BLAST" else "${game.dynamiteCooldown.toInt() + 1}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
