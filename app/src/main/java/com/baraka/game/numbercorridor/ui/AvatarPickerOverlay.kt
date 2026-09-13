package com.baraka.game.numbercorridor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.baraka.game.numbercorridor.model.AvatarDesign
import com.baraka.game.ui.theme.GameColors

/**
 * Shown before a run starts so the player can pick which of up to [AvatarDesign.entries]
 * looks their squad wears — purely cosmetic, same role as knightsurvivor's
 * ui/SettingsOverlay.kt knight-design panel. Squad *size* is a gameplay stat set by
 * shooting SQUAD buff walls mid-run (see model/PlayerState.kt), not chosen here.
 */
@Composable
fun AvatarPickerOverlay(
    selected: AvatarDesign,
    onSelected: (AvatarDesign) -> Unit,
    onStart: () -> Unit,
    onOpenGuide: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Number Corridor", color = GameColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "3 lanes, 3 jobs: left grows or shrinks your squad with math, middle is enemies to shoot down, right upgrades your weapon.",
                color = GameColors.TextSecondary,
                fontSize = 14.sp
            )
            Text("Choose your squad", color = GameColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AvatarDesign.entries.forEach { design ->
                    AvatarOptionCard(design = design, isSelected = design == selected, onClick = { onSelected(design) })
                }
            }

            Button(onClick = onStart) {
                Text("Start Run")
            }

            Text(
                text = "How do the lanes work?",
                color = GameColors.Accent,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onOpenGuide)
            )
        }
    }
}

@Composable
private fun AvatarOptionCard(design: AvatarDesign, isSelected: Boolean, onClick: () -> Unit) {
    val avatarColor = Color(design.colorArgb)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) avatarColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(28.dp)
                        .background(avatarColor, CircleShape)
                )
                Text(design.displayName, color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(design.description, color = GameColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
