package com.baraka.game.cratecrew.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.cratecrew.data.CrateSettings
import com.baraka.game.ui.components.SettingsSection
import com.baraka.game.ui.components.SettingsSliderRow
import com.baraka.game.ui.components.SettingsToggleRow
import com.baraka.game.ui.debug.DebugKit
import com.baraka.game.ui.debug.DebugPanel
import com.baraka.game.ui.debug.DebugSection

/**
 * Crate Crew settings, shown from the level select and from the in-level pause menu. Changes apply
 * immediately. [debugSections] only appear once developer mode is on (debug build, or tapping the
 * version label seven times).
 */
@Composable
fun CrateSettingsScreen(
    settings: CrateSettings,
    onChange: (CrateSettings) -> Unit,
    onClose: () -> Unit,
    versionName: String,
    debugSections: List<DebugSection>
) {
    val context = LocalContext.current
    var devUnlocked by remember { mutableStateOf(DebugKit.isUnlocked(context)) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }
    BackHandler(onBack = { if (showDebug) showDebug = false else onClose() })

    Box(Modifier.fillMaxSize().background(CrateColors.Bg)) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrateButton("< Back", onClick = onClose)
                Text("SETTINGS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SettingsSection("Audio", background = CrateColors.Card, titleColor = CrateColors.Accent) {
                        SettingsSliderRow("Sound effects", settings.sfxVolume, { onChange(settings.copy(sfxVolume = it)) }, Color.White)
                    }
                }
                item {
                    SettingsSection("Feedback", background = CrateColors.Card, titleColor = CrateColors.Accent) {
                        SettingsToggleRow("Vibration", settings.vibration, { onChange(settings.copy(vibration = it)) }, "Pulses on pushes, blocked moves and wins", Color.White, CrateColors.Muted)
                    }
                }
                item {
                    SettingsSection("Performance", background = CrateColors.Card, titleColor = CrateColors.Accent) {
                        SettingsToggleRow("Show FPS counter", settings.showFps, { onChange(settings.copy(showFps = it)) }, "Frame rate overlay while playing", Color.White, CrateColors.Muted)
                    }
                }
                item {
                    SettingsSection("About", background = CrateColors.Card, titleColor = CrateColors.Accent) {
                        Text(
                            "Crate Crew  -  version $versionName",
                            color = CrateColors.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!devUnlocked) {
                                    versionTaps++
                                    if (versionTaps >= 7) {
                                        DebugKit.unlock(context)
                                        devUnlocked = true
                                    }
                                }
                            }
                        )
                        if (devUnlocked) Text("Developer mode is on.", color = CrateColors.Accent, fontSize = 12.sp)
                    }
                }
                if (devUnlocked) {
                    item {
                        SettingsSection("Developer", background = CrateColors.Card, titleColor = CrateColors.Accent) {
                            CrateButton("Open debug tools", Modifier.fillMaxWidth()) { showDebug = true }
                        }
                    }
                }
            }
        }
        if (showDebug) DebugPanel("Crate Crew", debugSections) { showDebug = false }
    }
}
