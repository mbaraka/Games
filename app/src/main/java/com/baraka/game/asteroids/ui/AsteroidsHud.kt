package com.baraka.game.asteroids.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Overlaid on AsteroidsScreen's fixed-dark play-area backdrop, so it intentionally stays
// light-on-dark in both Light and Dark theme — same call as the other Atari-style games.
@Composable
fun AsteroidsHud(lives: Int, score: Int, wave: Int, bestScore: Int, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("🚀".repeat(lives.coerceAtLeast(0)), fontSize = 16.sp)
        Text("Wave $wave  ·  $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Best: $bestScore", color = Color.Gray, fontSize = 14.sp)
            Text("⚙", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onSettingsClick))
        }
    }
}
