package com.baraka.game.asteroids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.theme.GameColors

@Composable
fun AsteroidsEndScreen(score: Int, wave: Int, isNewBest: Boolean, onPlayAgain: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Game Over", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Score: $score  ·  Reached wave $wave", color = Color.White, fontSize = 18.sp)
            if (isNewBest) {
                Text("New Best!", color = GameColors.Warning, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onPlayAgain) {
                Text("Play Again")
            }
        }
    }
}

@Composable
fun AsteroidsWaveClearedOverlay(wave: Int, score: Int, onNextWave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wave $wave Cleared!", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Score: $score", color = Color.White, fontSize = 18.sp)
            Button(onClick = onNextWave) {
                Text("Next Wave")
            }
        }
    }
}
