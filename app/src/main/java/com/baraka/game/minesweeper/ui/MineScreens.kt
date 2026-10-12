package com.baraka.game.minesweeper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.minesweeper.MineViewModel
import com.baraka.game.minesweeper.model.COOP_LIVES
import com.baraka.game.minesweeper.model.MineDifficulty
import com.baraka.game.minesweeper.model.MineMode
import com.baraka.game.minesweeper.model.MineSnap
import com.baraka.game.minesweeper.model.MineStatus
import com.baraka.game.ui.components.GameScreenContainer
import com.baraka.game.ui.debug.DebugKit
import com.baraka.game.ui.debug.DebugPanel
import com.baraka.game.ui.debug.FpsOverlay
import com.baraka.game.ui.debug.FrameStats

@Composable
fun MineMenuScreen(vm: MineViewModel, onPlayFriend: () -> Unit, onSettings: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    GameScreenContainer(backgroundColor = MineColors.Background, scrollable = true, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MINESWEEPER", color = MineColors.Accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
        Text("Two phones, one minefield", color = MineColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 18.dp))

        Card("Mode (host picks)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in MineMode.entries) MineButton(m.label, { vm.mode = m }, Modifier.weight(1f), selected = vm.mode == m)
            }
            Text(vm.mode.blurb, color = MineColors.Muted, fontSize = 13.sp)
        }
        Spacer12()
        Card("Board size (host picks)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (d in MineDifficulty.entries) MineButton(d.label, { vm.difficulty = d }, Modifier.weight(1f), selected = vm.difficulty == d)
            }
            Text("${vm.difficulty.w} x ${vm.difficulty.h} cells, ${vm.difficulty.mines} mines", color = MineColors.Muted, fontSize = 13.sp)
        }
        Spacer12()
        MineButton("Play with a friend (Bluetooth)", onPlayFriend, Modifier.fillMaxWidth(), primary = true)
        Spacer12()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MineButton("Settings", onSettings, Modifier.weight(1f))
            MineButton("Back to hub", onBack, Modifier.weight(1f))
        }
        Text(
            "The host (who taps Host in the lobby) picks the mode and size. Both phones tap at the same time. Tap to uncover, long-press or use the flag button to flag.",
            color = MineColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun Spacer12() = androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MineColors.Card, RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(), color = MineColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

/** The live game: header, board, flag button; pause menu, settings, debug panel and the result card are overlays on top. */
@Composable
fun MineGameScreen(vm: MineViewModel, versionName: String, onLeave: () -> Unit) {
    val context = LocalContext.current
    val snap = vm.snap
    var flagMode by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var debugOpen by remember { mutableStateOf(false) }
    var hideResult by remember { mutableStateOf(false) }
    val stats = remember { FrameStats() }
    LaunchedEffect(snap?.status) { hideResult = false }
    LaunchedEffect(vm.settings.showFps) {
        if (!vm.settings.showFps) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                if (last != 0L) stats.record((t - last) / 1_000_000f)
                last = t
            }
        }
    }

    if (settingsOpen) {
        MineSettingsScreen(vm.settings, versionName, vm::updateSettings) { settingsOpen = false }
        return
    }
    BackHandler { paused = !paused }

    Box(Modifier.fillMaxSize()) {
        GameScreenContainer(backgroundColor = MineColors.Background, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MineButton("Menu", { paused = true })
                Text(
                    snap?.modeEnum?.label?.uppercase() ?: "", color = MineColors.Accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                Text(if (vm.isHost) "Host" else "Guest", color = MineColors.Muted, fontSize = 12.sp)
            }
            if (!vm.connected) Banner("Partner disconnected.", MineColors.Bad)
            else if (vm.versionMismatch) Banner("Your partner has a different game version - update both phones.", MineColors.Danger)

            if (snap == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Waiting for the host to deal...", color = MineColors.Muted, fontSize = 16.sp)
                }
            } else {
                StatusBar(snap, vm.me)
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    MineBoardView(
                        snap = snap,
                        me = vm.me,
                        showOwners = snap.modeEnum == MineMode.VERSUS,
                        flagMode = flagMode,
                        longPressFlags = vm.settings.longPressFlags,
                        onTap = vm::tap,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                MineButton(
                    if (flagMode) "Flag mode: ON (tap to flag)" else "Flag mode: OFF (tap to uncover)",
                    { flagMode = !flagMode }, Modifier.fillMaxWidth(), selected = flagMode
                )
            }
        }

        if (vm.settings.showFps) FpsOverlay(stats, Modifier.align(Alignment.TopStart).padding(top = 4.dp, start = 4.dp))

        if (snap != null && snap.status != MineStatus.PLAYING && !hideResult && !paused) {
            ResultCard(snap, vm.me, vm.isHost, onRematch = vm::rematch, onView = { hideResult = true }, onLeave = onLeave)
        }
        if (paused && !debugOpen) {
            Overlay {
                Text("PAUSED", color = MineColors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("The board keeps running for your partner.", color = MineColors.Muted, fontSize = 12.sp)
                MineButton("Resume", { paused = false }, Modifier.fillMaxWidth(), primary = true)
                MineButton("Settings", { settingsOpen = true }, Modifier.fillMaxWidth())
                if (DebugKit.isUnlocked(context)) MineButton("Debug tools", { debugOpen = true }, Modifier.fillMaxWidth())
                MineButton("Leave game", onLeave, Modifier.fillMaxWidth())
            }
        }
        if (debugOpen) DebugPanel("Minesweeper", mineDebugSections(vm)) { debugOpen = false }
    }
}

@Composable
private fun Banner(text: String, color: Color) {
    Text(
        text, color = MineColors.OnLight, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).background(color, RoundedCornerShape(8.dp)).padding(8.dp)
    )
}

@Composable
private fun StatusBar(snap: MineSnap, me: Int) {
    val mine = if (me == 0) snap.scoreHost else snap.scoreGuest
    val other = if (me == 0) snap.scoreGuest else snap.scoreHost
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).background(MineColors.Card, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (snap.modeEnum == MineMode.VERSUS) {
            Text("You  $mine", color = MineColors.Me, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("Partner  $other", color = MineColors.Partner, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        } else {
            Text("Lives  ${snap.lives}/$COOP_LIVES", color = if (snap.lives > 1) MineColors.Good else MineColors.Bad, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("Cells  ${mine + other}", color = MineColors.Text, fontSize = 14.sp)
        }
        Text("   Mines left  ${(snap.mines - snap.flagCount()).coerceAtLeast(0)}", color = MineColors.Muted, fontSize = 14.sp)
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth().background(MineColors.Card, RoundedCornerShape(16.dp)).padding(18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

@Composable
private fun ResultCard(snap: MineSnap, me: Int, isHost: Boolean, onRematch: () -> Unit, onView: () -> Unit, onLeave: () -> Unit) {
    val mine = if (me == 0) snap.scoreHost else snap.scoreGuest
    val other = if (me == 0) snap.scoreGuest else snap.scoreHost
    val versus = snap.modeEnum == MineMode.VERSUS
    val (title, color) = when {
        snap.status == MineStatus.LOST -> "Out of lives" to MineColors.Bad
        !versus -> "Board cleared!" to MineColors.Good
        mine > other -> "You win!" to MineColors.Good
        mine < other -> "Partner wins" to MineColors.Partner
        else -> "Draw" to MineColors.Text
    }
    Overlay {
        Text(title, color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            if (versus) "You $mine  -  Partner $other" else "You uncovered $mine cells, your partner $other.",
            color = MineColors.Text, fontSize = 15.sp, textAlign = TextAlign.Center
        )
        MineButton(if (isHost) "Play again" else "Ask for a rematch", onRematch, Modifier.fillMaxWidth(), primary = true)
        MineButton("View board", onView, Modifier.fillMaxWidth())
        MineButton("Leave", onLeave, Modifier.fillMaxWidth())
    }
}
