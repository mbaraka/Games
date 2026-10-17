package com.baraka.game.dreadnought.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.dreadnought.data.DreadSettings
import com.baraka.game.ui.components.SettingsSection
import com.baraka.game.ui.components.SettingsSliderRow
import com.baraka.game.ui.components.SettingsToggleRow
import com.baraka.game.ui.debug.DebugKit
import com.baraka.game.ui.debug.DebugPanel
import com.baraka.game.ui.debug.DebugSection
import kotlin.math.roundToInt

/**
 * Dreadnought settings: a volume for each sound channel, vibration, display and aiming options.
 * Reached from the harbour and from the in-sortie pause menu; changes apply at once.
 * [debugSections] are this context's developer tools; they only show once developer mode is on.
 */
@Composable
fun DreadSettingsScreen(
    settings: DreadSettings,
    onChange: (DreadSettings) -> Unit,
    onClose: () -> Unit,
    versionName: String,
    debugSections: List<DebugSection>
) {
    val context = LocalContext.current
    var devUnlocked by remember { mutableStateOf(DebugKit.isUnlocked(context)) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }
    BackHandler(onBack = { if (showDebug) showDebug = false else onClose() })

    val bg = NavyColors.Card
    val title = NavyColors.Accent
    val text = NavyColors.Text
    val muted = NavyColors.Muted

    Box(Modifier.fillMaxSize().background(NavyColors.Bg)) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavyButton("< Back", onClose, fontSize = 12.sp)
                NavyLabel("SETTINGS", size = 20.sp, bold = true)
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SettingsSection("Audio", background = bg, titleColor = title) {
                        SettingsSliderRow("Sound effects", settings.sfxVolume, { onChange(settings.copy(sfxVolume = it)) }, text)
                        SettingsSliderRow("Sea and engines", settings.ambienceVolume, { onChange(settings.copy(ambienceVolume = it)) }, text)
                    }
                }
                item {
                    SettingsSection("Feedback", background = bg, titleColor = title) {
                        SettingsToggleRow("Vibration", settings.vibration, { onChange(settings.copy(vibration = it)) }, "Pulses on broadsides, hits and sinkings", text, muted)
                        SettingsToggleRow("Screen shake", settings.screenShake, { onChange(settings.copy(screenShake = it)) }, "Camera shake on gunfire and explosions", text, muted)
                        SettingsToggleRow("Damage numbers", settings.damageNumbers, { onChange(settings.copy(damageNumbers = it)) }, "Floating numbers on every hit", text, muted)
                    }
                }
                item {
                    SettingsSection("Gunnery", background = bg, titleColor = title) {
                        SettingsToggleRow("Aim assist", settings.aimAssist, { onChange(settings.copy(aimAssist = it)) }, "Tap a ship to lock on; shells are aimed ahead of it", text, muted)
                        SettingsToggleRow("Auto fire", settings.autoFire, { onChange(settings.copy(autoFire = it)) }, "Fire as soon as turrets are loaded and on target", text, muted)
                        SettingsToggleRow("Range ring", settings.rangeRing, { onChange(settings.copy(rangeRing = it)) }, "Dotted circle at the main battery's reach", text, muted)
                    }
                }
                item {
                    SettingsSection("Display", background = bg, titleColor = title) {
                        SettingsSliderRow(
                            "Camera zoom", settings.zoom, { onChange(settings.copy(zoom = it)) }, text,
                            valueRange = 0.7f..1.5f, valueLabel = { "${(it * 100).roundToInt()}%" }
                        )
                        SettingsToggleRow("Show FPS counter", settings.showFps, { onChange(settings.copy(showFps = it)) }, "Frame rate overlay while playing", text, muted)
                    }
                }
                item {
                    SettingsSection("About", background = bg, titleColor = title) {
                        androidx.compose.material3.Text(
                            "Dreadnought  -  version $versionName",
                            color = muted,
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
                        if (devUnlocked) NavyLabel("Developer mode is on.", size = 12.sp, color = title, bold = true)
                    }
                }
                if (devUnlocked) {
                    item {
                        SettingsSection("Developer", background = bg, titleColor = title) {
                            NavyButton("Open debug tools", { showDebug = true }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        if (showDebug) DebugPanel("Dreadnought", debugSections) { showDebug = false }
    }
}
