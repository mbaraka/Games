package com.baraka.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.basedefender.BaseDefenderGame
import com.baraka.game.ui.theme.GameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                GameSelectionScreen(
                    onGameSelected = { gameType ->
                        when (gameType) {
                            GameType.PIXEL_COLLECTOR -> {
                                val intent = Intent(this, PixelCollectorGame::class.java)
                                startActivity(intent)
                            }
                            GameType.BASE_DEFENDER -> {
                                val intent = Intent(this, BaseDefenderGame::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                )
            }
        }
    }
}

enum class GameType {
    PIXEL_COLLECTOR,
    BASE_DEFENDER
}

@Composable
fun GameSelectionScreen(onGameSelected: (GameType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App title
        Text(
            text = "Game Hub",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Choose Your Adventure",
            color = Color.Gray,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Game selection cards
        GameCard(
            title = "Pixel Collector",
            description = "Collect green pixels while avoiding red obstacles!",
            color = Color(0xFF4CAF50),
            onClick = { onGameSelected(GameType.PIXEL_COLLECTOR) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        GameCard(
            title = "Base Defender",
            description = "Defend your base! Aim and shoot at incoming enemies!",
            color = Color(0xFFFF5722),
            onClick = { onGameSelected(GameType.BASE_DEFENDER) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Placeholder for future games
        GameCard(
            title = "Coming Soon...",
            description = "More exciting games are in development!",
            color = Color(0xFF9E9E9E),
            onClick = { /* Future game */ },
            enabled = false
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // App info
        Text(
            text = "Built with Jetpack Compose",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
fun GameCard(
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF2D2D2D) else Color(0xFF1A1A1A)
        ),
        onClick = if (enabled) onClick else { {} }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game icon/color indicator
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (enabled) color else Color.Gray,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Game info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = description,
                    color = if (enabled) Color.Gray else Color.DarkGray,
                    fontSize = 14.sp
                )
            }
            
            // Arrow indicator
            if (enabled) {
                Text(
                    text = "→",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}