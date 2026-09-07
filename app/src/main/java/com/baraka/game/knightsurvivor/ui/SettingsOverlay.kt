package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.model.Difficulty

/**
 * Sound + difficulty preferences. Sound takes effect immediately (it's just gating
 * playback each time); difficulty is fixed for the current run already in progress, so a
 * change here only applies starting the next run — the note under the picker says so.
 */
@Composable
fun SettingsOverlay(
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
    difficulty: Difficulty,
    onDifficultyChanged: (Difficulty) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f))) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Close ✕",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(text = "Sound", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingChip(text = "On", selected = soundEnabled, onClick = { onSoundEnabledChanged(true) })
                SettingChip(text = "Off", selected = !soundEnabled, onClick = { onSoundEnabledChanged(false) })
            }

            Spacer(Modifier.height(24.dp))

            Text(text = "Difficulty", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { candidate ->
                    SettingChip(
                        text = candidate.displayName,
                        selected = candidate == difficulty,
                        onClick = { onDifficultyChanged(candidate) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Takes effect on your next run — this one keeps its current difficulty.",
                color = Color.DarkGray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.Black else Color.White,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(if (selected) Color(0xFFFFD54F) else Color(0xFF2D2D2D), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
