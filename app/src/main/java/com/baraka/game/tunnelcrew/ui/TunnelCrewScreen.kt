package com.baraka.game.tunnelcrew.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.baraka.game.tunnelcrew.data.CrewAudio
import com.baraka.game.tunnelcrew.data.CrewHaptics
import com.baraka.game.tunnelcrew.data.CrewRepository

private const val NO_LEVEL = -1

/** Root of Tunnel Crew: switches between level select, a level in play and settings. */
@Composable
fun TunnelCrewScreen() {
    val context = LocalContext.current
    val repo = remember { CrewRepository(context) }
    val audio = remember { CrewAudio() }
    val haptics = remember { CrewHaptics(context) }
    var settings by remember { mutableStateOf(repo.loadSettings()) }
    var level by remember { mutableIntStateOf(NO_LEVEL) }
    var showSettings by remember { mutableStateOf(false) }
    var progressRev by remember { mutableIntStateOf(0) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(settings) {
        audio.volume = settings.sfxVolume
        haptics.enabled = settings.vibration
    }

    fun changeSettings(s: com.baraka.game.tunnelcrew.data.CrewSettings) {
        settings = s
        repo.saveSettings(s)
    }

    when {
        level != NO_LEVEL -> CrewPlayScreen(
            levelIndex = level,
            repo = repo,
            settings = settings,
            onSettingsChange = ::changeSettings,
            audio = audio,
            haptics = haptics,
            versionName = versionName,
            onOpenLevel = { level = it },
            onExit = { level = NO_LEVEL; progressRev++ }
        )
        showSettings -> CrewSettingsScreen(
            settings = settings,
            onChange = ::changeSettings,
            onClose = { showSettings = false },
            versionName = versionName,
            debugSections = menuDebugSections(repo, onChanged = { progressRev++ }, onJump = { showSettings = false; level = it })
        )
        else -> CrewMenuScreen(
            repo = repo,
            progressRev = progressRev,
            onPlay = { level = it },
            onSettings = { showSettings = true },
            onExit = { (context as? Activity)?.finish() }
        )
    }
}
