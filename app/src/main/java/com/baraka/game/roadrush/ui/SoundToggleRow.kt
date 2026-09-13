package com.baraka.game.roadrush.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.theme.GameColors

/** Shared On/Off sound toggle, used both in [CarPickerOverlay] and [RoadRushSettingsOverlay] so there's one place to flip it. */
@Composable
fun SoundToggleRow(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SoundToggleChip(text = "🔊 On", selected = enabled, onClick = { onEnabledChanged(true) })
        SoundToggleChip(text = "🔇 Off", selected = !enabled, onClick = { onEnabledChanged(false) })
    }
}

@Composable
private fun SoundToggleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.Black else GameColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(if (selected) GameColors.Warning else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
