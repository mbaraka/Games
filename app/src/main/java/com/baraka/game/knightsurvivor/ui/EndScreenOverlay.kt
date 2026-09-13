package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.model.GamePhase
import com.baraka.game.ui.theme.GameColors

/** Shown on GAME_OVER or VICTORY: a short run recap plus a Play Again button. */
@Composable
fun EndScreenOverlay(
    phase: GamePhase,
    survivalTimeMs: Long,
    levelReached: Int,
    killCount: Int,
    isNewBestTime: Boolean,
    onPlayAgain: () -> Unit
) {
    val isVictory = phase == GamePhase.VICTORY

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isVictory) "VICTORY!" else "YOU DIED",
                color = if (isVictory) GameColors.Success else GameColors.Danger,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            EndScreenStat(title = "Time Survived", value = formatSurvivalDuration(survivalTimeMs))
            EndScreenStat(title = "Level Reached", value = "$levelReached")
            EndScreenStat(title = "Enemies Killed", value = "$killCount")

            if (isNewBestTime) {
                Text(text = "New Best Time!", color = GameColors.Warning, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.Success, contentColor = Color.White),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Play Again")
            }
        }
    }
}

@Composable
private fun EndScreenStat(title: String, value: String) {
    Text(text = "$title: $value", color = GameColors.TextPrimary, fontSize = 16.sp)
}
