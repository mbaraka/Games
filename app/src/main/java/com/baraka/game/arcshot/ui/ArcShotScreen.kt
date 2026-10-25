package com.baraka.game.arcshot.ui

import android.app.Activity
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.baraka.game.arcshot.data.ArcAudio
import com.baraka.game.arcshot.data.ArcHaptics
import com.baraka.game.arcshot.data.ArcRepository

private const val NO_LEVEL = -1

/** Root of Arc Shot: switches between level select, a level in play and settings. */
@Composable
fun ArcShotScreen() {
    val context = LocalContext.current
    val repo = remember { ArcRepository(context) }
    val audio = remember { ArcAudio() }
    val haptics = remember { ArcHaptics(context) }
    var settings by remember { mutableStateOf(repo.loadSettings()) }
    var level by remember { mutableIntStateOf(NO_LEVEL) }
    var showSettings by remember { mutableStateOf(false) }
    var progressRev by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    DisposableEffect(Unit) { onDispose { audio.release() } }

    fun changeSettings(s: com.baraka.game.arcshot.data.ArcSettings) {
        settings = s
        repo.saveSettings(s)
    }

    when {
        level != NO_LEVEL -> ArcPlayScreen(
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
        showSettings -> ArcSettingsScreen(
            settings = settings,
            onChange = ::changeSettings,
            onClose = { showSettings = false },
            versionName = versionName,
            debugSections = menuDebugSections(repo, onChanged = { progressRev++ }, onJump = { showSettings = false; level = it })
        )
        else -> ArcMenuScreen(
            repo = repo,
            progressRev = progressRev,
            gridState = gridState,
            onPlay = { level = it },
            onSettings = { showSettings = true },
            onExit = { (context as? Activity)?.finish() }
        )
    }
}
