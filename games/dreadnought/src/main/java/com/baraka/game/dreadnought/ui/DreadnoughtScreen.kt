package com.baraka.game.dreadnought.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baraka.game.dreadnought.BuildConfig
import com.baraka.game.dreadnought.data.DreadAudio
import com.baraka.game.dreadnought.data.DreadRepository
import com.baraka.game.dreadnought.data.DreadSave
import com.baraka.game.dreadnought.data.DreadSettings
import com.baraka.game.dreadnought.model.Refit
import com.baraka.game.dreadnought.model.ShipClass
import com.baraka.game.dreadnought.model.ShipDesign
import com.baraka.game.dreadnought.model.Theatre
import com.baraka.game.dreadnought.render.NavalRenderer
import com.baraka.game.dreadnought.systems.DreadCue
import com.baraka.game.dreadnought.systems.NavalGame
import com.baraka.game.dreadnought.systems.PHASE_BRIEF
import com.baraka.game.dreadnought.systems.PHASE_OVER
import com.baraka.game.dreadnought.systems.PHASE_REFIT
import com.baraka.game.ui.debug.FpsOverlay
import com.baraka.game.ui.debug.FrameStats
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

private enum class Page { HARBOR, RUN, SETTINGS, YARD }

/** What a finished (or abandoned) sortie hands back to the save. */
private class RunResult(val coins: Int, val wave: Int, val score: Int, val kills: Int, val flagships: Int)

/** Everything the Compose overlays need from the simulation, updated from the frame loop only when it changes. */
private class HudState {
    var phase by mutableIntStateOf(PHASE_BRIEF)
    var fireReady by mutableFloatStateOf(1f)
    var repair by mutableFloatStateOf(1f)
    var torp by mutableFloatStateOf(1f)
    var planes by mutableFloatStateOf(1f)
    var dive by mutableFloatStateOf(1f)
    var diving by mutableStateOf(false)
}

@Composable
fun DreadnoughtScreen() {
    val context = LocalContext.current
    val repo = remember { DreadRepository.get(context) }
    // the player's own designs must be registered before the save is read, because the saved ship is an index into the list
    var designs by remember {
        val loaded = repo.loadDesigns()
        ShipClass.setCustom(loaded.map { it.toShipClass() })
        mutableStateOf(loaded)
    }
    var editing by remember { mutableStateOf<ShipDesign?>(null) }
    var harborTab by remember { mutableIntStateOf(0) }
    var save by remember { mutableStateOf(repo.loadSave()) }
    var settings by remember { mutableStateOf(repo.loadSettings()) }
    var page by remember { mutableStateOf(Page.HARBOR) }
    var runId by remember { mutableIntStateOf(0) }

    val commit: (DreadSave) -> Unit = { s ->
        save = s
        repo.save(s)
    }
    val applyDesigns: (List<ShipDesign>) -> Unit = { list ->
        designs = list
        repo.saveDesigns(list)
        ShipClass.setCustom(list.map { it.toShipClass() })
    }
    val changeSettings: (DreadSettings) -> Unit = { s ->
        settings = s
        repo.saveSettings(s)
    }

    when (page) {
        Page.HARBOR -> HarborScreen(
            save = save,
            onSortie = {
                runId++
                page = Page.RUN
            },
            onBuy = { r ->
                val level = save.level(r)
                val price = r.cost(level)
                if (level < r.maxLevel && save.coins >= price) {
                    commit(save.copy(coins = save.coins - price, levels = save.levels.mapIndexed { i, v -> if (i == r.ordinal) v + 1 else v }))
                }
            },
            onTheatre = { i -> if (save.theatreUnlocked(Theatre.entries[i])) commit(save.copy(theatre = i)) },
            onShip = { i -> commit(save.copy(ship = i)) },
            onBuyShip = { i ->
                val c = ShipClass.PLAYABLE[i]
                if (!save.owns(i) && save.coins >= c.unlockCost) {
                    commit(save.copy(coins = save.coins - c.unlockCost, shipsOwned = save.shipsOwned or (1 shl i), ship = i))
                }
            },
            designs = designs,
            onNewDesign = {
                editing = ShipDesign(id = (designs.maxOfOrNull { it.id } ?: 0) + 1, name = "My ship ${designs.size + 1}")
                page = Page.YARD
            },
            onEditDesign = { i ->
                editing = designs.getOrNull(i)
                if (editing != null) page = Page.YARD
            },
            tab = harborTab,
            onTab = { harborTab = it },
            onSettings = { page = Page.SETTINGS },
            onExit = { (context as? android.app.Activity)?.finish() }
        )
        Page.YARD -> {
            val d = editing
            if (d == null) {
                page = Page.HARBOR
            } else {
                ShipyardScreen(
                    initial = d,
                    isNew = designs.none { it.id == d.id },
                    onSave = { nd ->
                        val list = if (designs.any { it.id == nd.id }) designs.map { if (it.id == nd.id) nd else it } else designs + nd
                        applyDesigns(list)
                        commit(save.copy(ship = ShipClass.BUILTIN_PLAYABLE_COUNT + list.indexOfFirst { it.id == nd.id }))
                        harborTab = 1
                        page = Page.HARBOR
                    },
                    onDelete = {
                        val at = designs.indexOfFirst { it.id == d.id }
                        val removedIndex = ShipClass.BUILTIN_PLAYABLE_COUNT + at
                        applyDesigns(designs.filter { it.id != d.id })
                        val keep = when {
                            save.ship == removedIndex -> 0
                            save.ship > removedIndex -> save.ship - 1
                            else -> save.ship
                        }
                        commit(save.copy(ship = keep))
                        harborTab = 1
                        page = Page.HARBOR
                    },
                    onClose = {
                        harborTab = 1
                        page = Page.HARBOR
                    }
                )
            }
        }
        Page.SETTINGS -> DreadSettingsScreen(
            settings = settings,
            onChange = changeSettings,
            onClose = { page = Page.HARBOR },
            versionName = BuildConfig.DREADNOUGHT_VERSION,
            debugSections = harborDebug(save, commit)
        )
        Page.RUN -> androidx.compose.runtime.key(runId) {
            RunScreen(
                save = save,
                settings = settings,
                onSettings = changeSettings,
                onBank = { r ->
                    commit(
                        save.copy(
                            coins = save.coins + r.coins,
                            bestWave = maxOf(save.bestWave, r.wave),
                            bestScore = maxOf(save.bestScore, r.score),
                            kills = save.kills + r.kills,
                            flagships = save.flagships + r.flagships
                        )
                    )
                },
                onRetry = {
                    runId++
                },
                onHarbor = { page = Page.HARBOR }
            )
        }
    }
}

@Composable
private fun RunScreen(
    save: DreadSave,
    settings: DreadSettings,
    onSettings: (DreadSettings) -> Unit,
    onBank: (RunResult) -> Unit,
    onRetry: () -> Unit,
    onHarbor: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val game = remember { NavalGame(save.levels, 1, save.shipClass()) }
    val renderer = remember { NavalRenderer() }
    val audio = remember { DreadAudio(context) }
    val stats = remember { FrameStats() }
    val hud = remember { HudState() }
    val stick = remember { StickState() }
    val theatre = Theatre.entries[save.theatre.coerceIn(0, Theatre.entries.lastIndex)]

    var paused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    var godOn by remember { mutableStateOf(false) }
    var instantOn by remember { mutableStateOf(false) }
    var timeScale by remember { mutableFloatStateOf(1f) }
    var banked by remember { mutableStateOf(false) }
    val bank by rememberUpdatedState(onBank)

    SideEffect {
        game.shakeOn = settings.screenShake
        game.numbersOn = settings.damageNumbers
        game.assistOn = settings.aimAssist
        game.autoFire = settings.autoFire
        audio.sfxVolume = settings.sfxVolume
        audio.ambienceVolume = settings.ambienceVolume
        audio.vibration = settings.vibration
    }

    fun result() = RunResult(game.runCoins, game.wave, game.score, game.kills, game.flagships)
    fun bankOnce() {
        if (!banked) {
            banked = true
            bank(result())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    paused = true
                    audio.pause()
                }
                Lifecycle.Event.ON_RESUME -> audio.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audio.release()
        }
    }

    LaunchedEffect(Unit) {
        audio.startAmbience()
        game.setHelm(0f, 0.4f)
    }

    // the frame loop: simulate, play the sounds the simulation asked for, publish the few values the overlays show
    LaunchedEffect(game) {
        var last = 0L
        var lastEngine = -1
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val ms = (now - last) / 1_000_000f
                    stats.record(ms)
                    if (!paused) {
                        game.update(ms / 1000f * timeScale)
                        for (i in 0 until game.cueCount) audio.play(DreadCue.entries[game.cueIds[i]], game.cueVol[i])
                        val eng = (abs(game.player.speed) / game.player.cls.maxSpeed * 20f).toInt()
                        if (eng != lastEngine) {
                            lastEngine = eng
                            audio.setEngine(eng / 20f)
                        }
                        hud.phase = game.phase
                        hud.fireReady = (game.mainReady * 20f).toInt() / 20f
                        hud.torp = (game.torpFrac() * 20f).toInt() / 20f
                        hud.planes = (game.planesFrac() * 20f).toInt() / 20f
                        hud.dive = (game.diveBattery * 20f).toInt() / 20f
                        hud.diving = game.diving
                        hud.repair = if (game.repairCd <= 0f) 1f else ((1f - game.repairCd / game.repairCdMax) * 20f).toInt() / 20f
                    }
                }
                last = now
                frame = now
            }
        }
    }

    LaunchedEffect(hud.phase) {
        if (hud.phase == PHASE_OVER) bankOnce()
    }

    BackHandler {
        if (showSettings) showSettings = false else paused = !paused
    }

    val insets = WindowInsets.safeDrawing
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val insetLeft = insets.getLeft(density, layoutDirection).toFloat()
    val insetTop = insets.getTop(density).toFloat()

    Box(Modifier.fillMaxSize().background(NavyColors.Water)) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(game) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    game.aimAt(renderer.screenToWorldX(down.position.x), renderer.screenToWorldY(down.position.y))
                    do {
                        val event = awaitPointerEvent()
                        for (ch in event.changes) {
                            if (ch.id == down.id && ch.pressed) {
                                game.aimAt(renderer.screenToWorldX(ch.position.x), renderer.screenToWorldY(ch.position.y))
                                ch.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
        ) {
            if (frame < 0L) return@Canvas
            renderer.safeLeft = insetLeft
            renderer.safeTop = insetTop
            renderer.draw(this, game, theatre, settings, density.density)
        }

        // controls
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(14.dp)) {
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NavyButton(
                    "ALL STOP",
                    {
                        stick.x = 0f
                        stick.y = 0f
                        game.setThrottle(0f)
                    },
                    Modifier.width(150.dp),
                    fontSize = 12.sp
                )
                CourseStick(
                    stick,
                    onOrder = { vx, vy ->
                        val m = hypot(vx, vy)
                        if (m > 0.15f) game.setHelm(atan2(vy, vx), (m - 0.15f) / 0.85f) else game.setThrottle(0f)
                    }
                )
            }
            Row(
                Modifier.align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if (game.canDive) {
                    RingButton(if (hud.diving) "SURFACE" else "DIVE", hud.dive, onPress = { game.setDive(!game.diving) }, diameter = 76.dp)
                }
                if (game.hasPlanes && game.player.cls.mainCount > 0) {
                    RingButton("LAUNCH", hud.planes, onPress = { game.launchPlanes() }, diameter = 76.dp)
                }
                if (game.hasTubes) {
                    RingButton("TORP", hud.torp, onPress = { game.fireTorpedoes() }, diameter = 76.dp)
                }
                RingButton("REPAIR", hud.repair, onPress = { game.requestRepair() }, diameter = 76.dp)
                if (game.hasPlanes && game.player.cls.mainCount == 0) {
                    RingButton("LAUNCH", hud.planes, onPress = { game.launchPlanes() }, diameter = 104.dp)
                } else {
                    RingButton("FIRE", hud.fireReady, onPress = { game.firing = true }, onRelease = { game.firing = false }, diameter = 104.dp)
                }
            }
            Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NavyButton("II", { paused = true }, Modifier.width(60.dp), fontSize = 16.sp)
                if (settings.showFps) FpsOverlay(stats)
            }
        }

        if (hud.phase == PHASE_REFIT) RefitOverlay(game)
        if (hud.phase == PHASE_OVER) {
            OverResult(
                game = game,
                bestWave = save.bestWave,
                onRetry = onRetry,
                onHarbor = onHarbor
            )
        }
        if (paused && hud.phase != PHASE_OVER) {
            PauseOverlay(
                onResume = { paused = false },
                onSettings = { showSettings = true },
                onQuit = {
                    bankOnce()
                    onHarbor()
                }
            )
        }
        if (showSettings) {
            DreadSettingsScreen(
                settings = settings,
                onChange = onSettings,
                onClose = { showSettings = false },
                versionName = BuildConfig.DREADNOUGHT_VERSION,
                debugSections = runDebug(
                    game = game,
                    god = godOn,
                    onGod = { godOn = it },
                    instantReload = instantOn,
                    onInstantReload = { instantOn = it },
                    timeScale = timeScale,
                    onTimeScale = { timeScale = it }
                )
            )
        }
    }
}

@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(NavyColors.Scrim).clickable(enabled = false) {}
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSettings: () -> Unit, onQuit: () -> Unit) {
    Scrim {
        Column(
            Modifier.widthIn(max = 320.dp).background(NavyColors.Card, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NavyLabel("PAUSED", size = 22.sp, bold = true, color = NavyColors.Accent)
            NavyButton("Resume", onResume, Modifier.fillMaxWidth(), accent = true)
            NavyButton("Settings", onSettings, Modifier.fillMaxWidth())
            NavyButton("Return to harbour", onQuit, Modifier.fillMaxWidth())
            NavyLabel("Coins earned this sortie are kept.", size = 11.sp, color = NavyColors.Muted)
        }
    }
}

@Composable
private fun RefitOverlay(game: NavalGame) {
    Scrim {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NavyLabel("WAVE ${game.wave} CLEARED - CHOOSE A REFIT", size = 18.sp, bold = true, color = NavyColors.Accent)
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0 until 3) {
                    val r = Refit.entries[game.refitOffer[i]]
                    Column(
                        Modifier.width(190.dp)
                            .fillMaxHeight()
                            .background(NavyColors.Card, RoundedCornerShape(14.dp))
                            .clickable { game.pickRefit(i) }
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(r.icon(), contentDescription = null, tint = NavyColors.Accent, modifier = Modifier.size(28.dp))
                        NavyLabel(r.label, size = 15.sp, bold = true)
                        NavyLabel(
                            r.blurb,
                            size = 11.sp,
                            color = NavyColors.Muted,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        val now = game.level(r)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NavyLabel(r.stat(game.player.cls, now), size = 12.sp, color = NavyColors.Muted)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NavyColors.Accent, modifier = Modifier.size(12.dp))
                            NavyLabel(r.stat(game.player.cls, now + 1), size = 13.sp, color = NavyColors.Accent, bold = true)
                        }
                    }
                }
            }
            NavyLabel("Refits last until this sortie ends", size = 11.sp, color = NavyColors.Muted)
        }
    }
}

@Composable
private fun OverResult(game: NavalGame, bestWave: Int, onRetry: () -> Unit, onHarbor: () -> Unit) {
    Scrim {
        Column(
            Modifier.widthIn(max = 360.dp).background(NavyColors.Card, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavyLabel("SHIP LOST", size = 24.sp, bold = true, color = NavyColors.Bad)
            NavyLabel("Sunk on wave ${game.wave}", size = 14.sp)
            NavyLabel("Score ${game.score}   |   Ships sunk ${game.kills}", size = 13.sp, color = NavyColors.Muted)
            NavyLabel("+${game.runCoins} coins", size = 18.sp, bold = true, color = NavyColors.Accent)
            if (game.wave > bestWave) NavyLabel("New best wave!", size = 13.sp, bold = true, color = NavyColors.Good)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NavyButton("Harbour", onHarbor, Modifier.weight(1f))
                NavyButton("Sortie again", onRetry, Modifier.weight(1f), accent = true)
            }
        }
    }
}
