package com.baraka.game.missilecommand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.missilecommand.model.Difficulty
import com.baraka.game.roadrush.ui.SoundToggleRow
import com.baraka.game.ui.theme.GameColors

/** Shown before a run starts — difficulty and sound, same role as breakout's/spaceinvaders' start overlays. */
@Composable
fun MissileCommandStartOverlay(
    difficulty: Difficulty,
    onDifficultyChanged: (Difficulty) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
    bestScore: Int,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Missile Command", color = GameColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Tap anywhere to detonate an interceptor there. Protect your cities — ammo is limited each wave.",
                color = GameColors.TextSecondary,
                fontSize = 14.sp
            )
            if (bestScore > 0) {
                Text("Best score: $bestScore", color = GameColors.Warning, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Difficulty", color = GameColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { candidate ->
                        DifficultyChip(
                            text = candidate.displayName,
                            selected = candidate == difficulty,
                            onClick = { onDifficultyChanged(candidate) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sound", color = GameColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                SoundToggleRow(enabled = soundEnabled, onEnabledChanged = onSoundEnabledChanged)
            }

            Button(onClick = onStart) {
                Text("Start")
            }
        }
    }
}

@Composable
private fun DifficultyChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.Black else GameColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(if (selected) GameColors.Accent else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
