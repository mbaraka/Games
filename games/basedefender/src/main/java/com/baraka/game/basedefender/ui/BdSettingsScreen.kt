package com.baraka.game.basedefender.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.basedefender.data.BdSettings
import com.baraka.game.ui.components.SettingsSection
import com.baraka.game.ui.components.SettingsToggleRow
import com.baraka.game.ui.debug.DebugKit
import com.baraka.game.ui.debug.DebugPanel
import com.baraka.game.ui.debug.DebugSection

/**
 * Base Defender settings. The game has no audio yet, so there are no volume rows; the day it gains a sound
 * channel, add a `SettingsSliderRow` for it here (AGENTS.md: every sound must be controllable).
 * Reached from the start screen, the pause menu and the end screen. Changes apply immediately.
 */
@Composable
fun BdSettingsScreen(
    settings: BdSettings,
    onChange: (BdSettings) -> Unit,
    onClose: () -> Unit,
    versionName: String,
    debugSections: List<DebugSection>
) {
    val context = LocalContext.current
    var devUnlocked by remember { mutableStateOf(DebugKit.isUnlocked(context)) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }
    BackHandler(onBack = { if (showDebug) showDebug = false else onClose() })

    val text = BdColors.Text
    val muted = BdColors.Muted
    val card = BdColors.Card
    val title = BdColors.Accent

    Box(Modifier.fillMaxSize().background(BdColors.Bg).clickable(enabled = false) {}) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BdGhostButton("< Back", onClose)
                Text("SETTINGS", color = text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SettingsSection("Feedback", background = card, titleColor = title) {
                        SettingsToggleRow("Vibration", settings.vibration, { onChange(settings.copy(vibration = it)) }, "Pulse when a base is hit or a boss falls", text, muted)
                        SettingsToggleRow("Screen shake", settings.screenShake, { onChange(settings.copy(screenShake = it)) }, "Camera shake on hits and boss kills", text, muted)
                        SettingsToggleRow("Damage numbers", settings.damageNumbers, { onChange(settings.copy(damageNumbers = it)) }, "Floating numbers on every hit", text, muted)
                    }
                }
                item {
                    SettingsSection("Gameplay", background = card, titleColor = title) {
                        SettingsToggleRow("Auto aim", settings.autoAim, { onChange(settings.copy(autoAim = it)) }, "Turrets pick their own targets. Off = they only fire where you drag", text, muted)
                    }
                }
                item {
                    SettingsSection("Display", background = card, titleColor = title) {
                        SettingsToggleRow("Reduced effects", settings.reducedEffects, { onChange(settings.copy(reducedEffects = it)) }, "Fewer particles, for slower phones", text, muted)
                        SettingsToggleRow("Show FPS counter", settings.showFps, { onChange(settings.copy(showFps = it)) }, "Frame rate overlay while playing", text, muted)
                    }
                }
                item {
                    SettingsSection("About", background = card, titleColor = title) {
                        Text(
                            "Base Defender  -  version $versionName",
                            color = muted, fontSize = 13.sp,
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
                        if (devUnlocked) Text("Developer mode is on.", color = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (devUnlocked) {
                    item {
                        SettingsSection("Developer", background = card, titleColor = title) {
                            BdButton("Open debug tools", { showDebug = true }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        if (showDebug) DebugPanel("Base Defender", debugSections) { showDebug = false }
    }
}
