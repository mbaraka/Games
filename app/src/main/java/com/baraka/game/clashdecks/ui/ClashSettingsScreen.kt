package com.baraka.game.clashdecks.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.roadrush.ui.SoundToggleRow
import com.baraka.game.ui.theme.GameColors

/**
 * Clash Decks' own settings — just the sound toggle for now, but its own screen (reachable
 * from the menu and from a gear button mid-duel) so more options have somewhere to go later,
 * same shape as [CodexScreen].
 */
@Composable
fun ClashSettingsScreen(
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
    onClose: () -> Unit
) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Sound", color = GameColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "Card plays, hits, heals and status effects.",
                color = GameColors.TextMuted,
                fontSize = 12.sp
            )
            SoundToggleRow(enabled = soundEnabled, onEnabledChanged = onSoundEnabledChanged)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            // Clash Decks' own version — one per update to this game's own code (see
            // app/build.gradle.kts' gameBuild), not the whole app's version. Also what
            // ClashCoopLink.CLASH_COOP_PROTOCOL sends so two phones on different builds
            // can tell each other apart.
            "Clash Decks — version ${com.baraka.game.BuildConfig.CLASH_DECKS_VERSION}",
            color = GameColors.TextMuted,
            fontSize = 12.sp
        )
    }
}
