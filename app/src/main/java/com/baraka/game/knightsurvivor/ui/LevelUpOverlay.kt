package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.model.UpgradeOption

/** Pauses the run on level-up: pick one of the offered [choices]. Purely presentational — see systems/UpgradeChoiceSystem.kt for how choices are rolled/applied. */
@Composable
fun LevelUpOverlay(choices: List<UpgradeOption>, onChoiceSelected: (UpgradeOption) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "LEVEL UP!", color = Color.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(text = "Choose an upgrade", color = Color.White, fontSize = 16.sp)

            choices.forEach { choice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable { onChoiceSelected(choice) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = choice.title, color = Color(choice.colorArgb), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = choice.description, color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
