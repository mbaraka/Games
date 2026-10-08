package com.baraka.game.trainsurvivor.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.baraka.game.trainsurvivor.data.GameSettings
import com.baraka.game.ui.components.SettingsSection
import com.baraka.game.ui.components.SettingsSliderRow
import com.baraka.game.ui.components.SettingsToggleRow
import com.baraka.game.ui.debug.DebugKit
import com.baraka.game.ui.debug.DebugPanel
import com.baraka.game.ui.debug.DebugSection

private val BG = Color(0xFF10131A)
private val CARD = Color(0xFF1D222D)
private val AMBER = Color(0xFFFFB300)
private val MUTED = Color(0xFF9AA3B2)

/**
 * Train Survivor settings: every sound channel has its own volume, plus vibration and display options.
 * Shown from the Garage and from the in-run pause menu; changes apply immediately.
 *
 * [debugSections] describe this context's developer tools (garage cheats or in-run cheats); they only
 * appear once developer mode is on (debug build, or tapping the version label seven times).
 */
@Composable
fun TrainSettingsScreen(
    settings: GameSettings,
    onChange: (GameSettings) -> Unit,
    onClose: () -> Unit,
    versionName: String,
    debugSections: List<DebugSection>
) {
    val context = LocalContext.current
    var devUnlocked by remember { mutableStateOf(DebugKit.isUnlocked(context)) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }
    BackHandler(onBack = { if (showDebug) showDebug = false else onClose() })

    Box(Modifier.fillMaxSize().background(BG)) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = CARD, contentColor = Color.White)) {
                    Text("< Back", fontSize = 12.sp)
                }
                Text("SETTINGS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SettingsSection("Audio", background = CARD, titleColor = AMBER) {
                        SettingsSliderRow("Music", settings.musicVolume, { onChange(settings.copy(musicVolume = it)) }, Color.White)
                        SettingsSliderRow("Sound effects", settings.sfxVolume, { onChange(settings.copy(sfxVolume = it)) }, Color.White)
                        SettingsSliderRow("Train ambience", settings.ambienceVolume, { onChange(settings.copy(ambienceVolume = it)) }, Color.White)
                    }
                }
                item {
                    SettingsSection("Feedback", background = CARD, titleColor = AMBER) {
                        SettingsToggleRow("Vibration", settings.vibration, { onChange(settings.copy(vibration = it)) }, "Pulses on big hits, blasts and bosses", Color.White, MUTED)
                        SettingsToggleRow("Damage numbers", settings.damageNumbers, { onChange(settings.copy(damageNumbers = it)) }, "Floating numbers when you hit zombies", Color.White, MUTED)
                        SettingsToggleRow("Screen shake", settings.screenShake, { onChange(settings.copy(screenShake = it)) }, "Camera shake on explosions and smashes", Color.White, MUTED)
                    }
                }
                item {
                    SettingsSection("Performance", background = CARD, titleColor = AMBER) {
                        SettingsToggleRow("Show FPS counter", settings.showFps, { onChange(settings.copy(showFps = it)) }, "Frame rate overlay while playing", Color.White, MUTED)
                    }
                }
                item {
                    SettingsSection("About", background = CARD, titleColor = AMBER) {
                        Text(
                            "Train Survivor  -  version $versionName",
                            color = MUTED,
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
                        if (devUnlocked) Text("Developer mode is on.", color = AMBER, fontSize = 12.sp)
                    }
                }
                if (devUnlocked) {
                    item {
                        SettingsSection("Developer", background = CARD, titleColor = AMBER) {
                            Button(
                                onClick = { showDebug = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3342), contentColor = Color.White)
                            ) { Text("Open debug tools") }
                        }
                    }
                }
            }
        }
        if (showDebug) DebugPanel("Train Survivor", debugSections) { showDebug = false }
    }
}
