package com.baraka.game.zombie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.theme.GameTheme
import com.baraka.game.zombie.engine.ZombieGLSurfaceView
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView

class ZombieGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                ZombieGameScreen(
                    onExit = { finish() }
                )
            }
        }
    }
}

@Composable
fun ZombieGameScreen(
    onExit: () -> Unit
) {
    var gameState by remember { mutableStateOf(GameState.MAIN_MENU) }
    
    when (gameState) {
        GameState.MAIN_MENU -> {
            ZombieMainMenu(
                onStartGame = { gameState = GameState.PLAYING },
                onSettings = { gameState = GameState.SETTINGS },
                onExit = onExit
            )
        }
        GameState.PLAYING -> {
            ZombieGameplayScreen(
                onGameOver = { gameState = GameState.GAME_OVER },
                onPause = { gameState = GameState.PAUSED }
            )
        }
        GameState.PAUSED -> {
            ZombiePauseMenu(
                onResume = { gameState = GameState.PLAYING },
                onMainMenu = { gameState = GameState.MAIN_MENU }
            )
        }
        GameState.GAME_OVER -> {
            ZombieGameOverScreen(
                onRestart = { gameState = GameState.PLAYING },
                onMainMenu = { gameState = GameState.MAIN_MENU }
            )
        }
        GameState.SETTINGS -> {
            ZombieSettingsScreen(
                onBack = { gameState = GameState.MAIN_MENU }
            )
        }
    }
}

enum class GameState {
    MAIN_MENU,
    PLAYING,
    PAUSED,
    GAME_OVER,
    SETTINGS
}

@Composable
fun ZombieMainMenu(
    onStartGame: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ZOMBIE SURVIVAL",
            color = Color(0xFFFF4444),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF4444)
            )
        ) {
            Text(
                text = "START GAME",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Button(
            onClick = onSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF666666)
            )
        ) {
            Text(
                text = "SETTINGS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF444444)
            )
        ) {
            Text(
                text = "EXIT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ZombieGameplayScreen(
    onGameOver: () -> Unit,
    onPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // 3D Game View
        AndroidView(
            factory = { context ->
                ZombieGLSurfaceView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Game UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with pause button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ZOMBIE SURVIVAL",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x88FF4444)
                    )
                ) {
                    Text("PAUSE")
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Movement controls
                Button(
                    onClick = { /* TODO: Move forward */ },
                    modifier = Modifier.size(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x88FFFFFF)
                    )
                ) {
                    Text("↑", color = Color.Black)
                }
                
                Button(
                    onClick = { /* TODO: Move left */ },
                    modifier = Modifier.size(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x88FFFFFF)
                    )
                ) {
                    Text("←", color = Color.Black)
                }
                
                Button(
                    onClick = { /* TODO: Move right */ },
                    modifier = Modifier.size(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x88FFFFFF)
                    )
                ) {
                    Text("→", color = Color.Black)
                }
                
                Button(
                    onClick = { /* TODO: Move backward */ },
                    modifier = Modifier.size(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x88FFFFFF)
                    )
                ) {
                    Text("↓", color = Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ZombiePauseMenu(
    onResume: () -> Unit,
    onMainMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME PAUSED",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        Button(
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("RESUME", fontSize = 18.sp)
        }
        
        Button(
            onClick = onMainMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("MAIN MENU", fontSize = 18.sp)
        }
    }
}

@Composable
fun ZombieGameOverScreen(
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            color = Color(0xFFFF4444),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("RESTART", fontSize = 18.sp)
        }
        
        Button(
            onClick = onMainMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("MAIN MENU", fontSize = 18.sp)
        }
    }
}

@Composable
fun ZombieSettingsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(32.dp)
    ) {
        Text(
            text = "SETTINGS",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // TODO: Add settings controls here
        
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 32.dp)
        ) {
            Text("BACK", fontSize = 18.sp)
        }
    }
}
