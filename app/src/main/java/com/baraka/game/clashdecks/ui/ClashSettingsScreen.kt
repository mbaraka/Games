package com.baraka.game.clashdecks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.components.SoundToggleRow
import com.baraka.game.ui.components.SettingsSection
import com.baraka.game.ui.debug.DebugKit
import com.baraka.game.ui.debug.DebugPanel
import com.baraka.game.ui.debug.DebugSection
import com.baraka.game.ui.theme.GameColors

/**
 * Clash Decks' own settings — sound, the game's own version (see AGENTS.md's "App version vs
 * game version"), and developer tools once unlocked. Reachable from the menu and from a gear
 * button mid-duel, same shape as [CodexScreen].
 *
 * [debugSections] is empty by default: single-player ([com.baraka.game.clashdecks.ClashDecksViewModel])
 * passes its real cheat list, but a co-op duel never does — mutating the host's [com.baraka.game.clashdecks.model.DuelState]
 * out from under its own broadcast would desync the guest's mirrored copy (see
 * com.baraka.game.clashdecks.coop).
 */
@Composable
fun ClashSettingsScreen(
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
    debugSections: List<DebugSection> = emptyList()
) {
    val context = LocalContext.current
    var devUnlocked by remember { mutableStateOf(DebugKit.isUnlocked(context)) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Settings", color = GameColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            PillButton("Close", onClose, primary = false)
        }
        Spacer(Modifier.height(16.dp))

        SettingsSection("Sound", background = PanelBg, titleColor = GameColors.TextSecondary) {
            Text(
                "Card plays, hits, heals and status effects.",
                color = GameColors.TextMuted,
                fontSize = 12.sp
            )
            SoundToggleRow(enabled = soundEnabled, onEnabledChanged = onSoundEnabledChanged)
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection("About", background = PanelBg, titleColor = GameColors.TextSecondary) {
            Text(
                // Clash Decks' own version — one per update to this game's own code (see
                // app/build.gradle.kts' gameBuild), not the whole app's version. Also what
                // ClashCoopLink.CLASH_COOP_PROTOCOL sends so two phones on different builds
                // can tell each other apart.
                "Clash Decks — version ${com.baraka.game.BuildConfig.CLASH_DECKS_VERSION}",
                color = GameColors.TextMuted,
                fontSize = 12.sp,
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
            if (devUnlocked) Text("Developer mode is on.", color = GameColors.Accent, fontSize = 12.sp)
        }

        if (devUnlocked && debugSections.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SettingsSection("Developer", background = PanelBg, titleColor = GameColors.TextSecondary) {
                Button(
                    onClick = { showDebug = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.Accent, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) { Text("Open debug tools") }
            }
        }
    }

    if (showDebug) DebugPanel("Clash Decks", debugSections) { showDebug = false }
}
