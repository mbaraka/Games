package com.baraka.game.numbercorridor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.numbercorridor.model.PlayerState

@Composable
fun CorridorHud(player: PlayerState, score: Int, bestScore: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Score: $score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Best: $bestScore", color = Color.Gray, fontSize = 16.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("HP ${player.health}/${player.maxHealth}", color = Color(0xFFEF5350), fontSize = 14.sp)
            Text("DMG ${player.damage.toInt()}", color = Color(0xFFFF7043), fontSize = 14.sp)
            Text("RATE x${"%.1f".format(player.fireRateMultiplier)}", color = Color(0xFF29B6F6), fontSize = 14.sp)
            Text("SQUAD ${player.squadCount}", color = Color(0xFF66BB6A), fontSize = 14.sp)
        }
    }
}
