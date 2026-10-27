package com.baraka.game.roadrush.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.baraka.game.roadrush.model.CarDesign
import com.baraka.game.roadrush.model.Difficulty
import com.baraka.game.ui.components.HubBackButton
import com.baraka.game.ui.components.SoundToggleRow
import com.baraka.game.ui.theme.GameColors

/**
 * Shown before a run starts so the player can pick which [CarDesign] their car wears, plus
 * the run's [Difficulty] — purely cosmetic and pre-run settings, same role as numbercorridor's
 * ui/AvatarPickerOverlay.kt. Scrollable since the difficulty picker made this content taller
 * than some phone screens.
 */
@Composable
fun CarPickerOverlay(
    selected: CarDesign,
    onSelected: (CarDesign) -> Unit,
    difficulty: Difficulty,
    onDifficultyChanged: (Difficulty) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
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
            Text("Road Rush", color = GameColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "Dodge oncoming traffic by shifting lanes. The road only gets faster.",
                color = GameColors.TextSecondary,
                fontSize = 14.sp
            )

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
                Text(difficulty.description, color = GameColors.TextMuted, fontSize = 12.sp)
            }

            Text("Choose your car", color = GameColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CarDesign.entries.forEach { design ->
                    CarOptionCard(design = design, isSelected = design == selected, onClick = { onSelected(design) })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sound", color = GameColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                SoundToggleRow(enabled = soundEnabled, onEnabledChanged = onSoundEnabledChanged)
            }

            Button(onClick = onStart) {
                Text("Start Driving")
            }
        }
        HubBackButton(modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
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

@Composable
private fun CarOptionCard(design: CarDesign, isSelected: Boolean, onClick: () -> Unit) {
    val carColor = Color(design.colorArgb)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) carColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(28.dp)
                        .background(carColor, CircleShape)
                )
                Text(design.displayName, color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(design.description, color = GameColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
