package com.baraka.game.minesweeper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.baraka.game.minesweeper.data.MineSettings
import com.baraka.game.ui.components.GameScreenContainer
import com.baraka.game.ui.components.SettingsSection
import com.baraka.game.ui.components.SettingsToggleRow
import com.baraka.game.ui.debug.DebugKit

/** Settings, reachable from the menu and from the in-game pause menu. Changes apply and save immediately. */
@Composable
fun MineSettingsScreen(settings: MineSettings, versionName: String, onChange: (MineSettings) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var taps by remember { mutableIntStateOf(0) }
    var devUnlocked by remember { mutableStateOf(DebugKit.isUnlocked(context)) }
    BackHandler(onBack = onClose)

    GameScreenContainer(backgroundColor = MineColors.Background) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SETTINGS", color = MineColors.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            MineButton("Back", onClose)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSection("Controls", background = MineColors.Card, titleColor = MineColors.Accent) {
                SettingsToggleRow(
                    "Long-press to flag", settings.longPressFlags, { onChange(settings.copy(longPressFlags = it)) },
                    "Hold a cell to flag it (or to uncover it while the flag button is on)", MineColors.Text, MineColors.Muted
                )
                SettingsToggleRow(
                    "Vibration", settings.vibration, { onChange(settings.copy(vibration = it)) },
                    "A buzz whenever someone steps on a mine", MineColors.Text, MineColors.Muted
                )
            }
            SettingsSection("Performance", background = MineColors.Card, titleColor = MineColors.Accent) {
                SettingsToggleRow(
                    "Show FPS counter", settings.showFps, { onChange(settings.copy(showFps = it)) },
                    "Frame rate overlay while playing", MineColors.Text, MineColors.Muted
                )
            }
            SettingsSection("About", background = MineColors.Card, titleColor = MineColors.Accent) {
                Text(
                    "Minesweeper  -  version $versionName",
                    color = MineColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (!devUnlocked && ++taps >= 7) {
                            DebugKit.unlock(context)
                            devUnlocked = true
                        }
                    }
                )
                if (devUnlocked) Text("Developer mode is on. Open the pause menu during a game for the debug tools.", color = MineColors.Accent, fontSize = 12.sp)
            }
        }
    }
}
