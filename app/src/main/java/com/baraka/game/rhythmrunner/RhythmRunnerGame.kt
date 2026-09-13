package com.baraka.game.rhythmrunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.components.GameScreenContainer
import com.baraka.game.ui.theme.GameColors
import com.baraka.game.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class RhythmRunnerGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                RhythmRunnerGameScreen()
            }
        }
    }
}

@Composable
fun RhythmRunnerGameScreen() {
    var gameState by remember { mutableStateOf(RhythmGameState()) }
    var isGameRunning by remember { mutableStateOf(false) }
    var showGameOver by remember { mutableStateOf(false) }

    LaunchedEffect(isGameRunning) {
        while (isGameRunning && !showGameOver) {
            delay(16) // ~60 FPS
            gameState = gameState.update()
            
            if (gameState.lives <= 0) {
                showGameOver = true
                isGameRunning = false
            }
        }
    }
    
    // Separate beat counter that updates every 1000ms (60 BPM - much slower and clearer)
    LaunchedEffect(isGameRunning) {
        while (isGameRunning && !showGameOver) {
            delay(1000) // Update beat every 1000ms for 60 BPM
            gameState = gameState.copy(beatCount = gameState.beatCount + 1)
        }
    }

    GameScreenContainer(horizontalAlignment = Alignment.CenterHorizontally) {
        // Game title
        Text(
            text = "Rhythm Runner",
            color = GameColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Game stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Score: ${gameState.score}",
                color = GameColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lives: ${gameState.lives}",
                color = GameColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Combo: ${gameState.combo}x",
            color = GameColors.Accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Tap the game area to jump over red obstacles!",
            color = GameColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = "Collect gold circles for points!",
            color = GameColors.Accent,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = "Player Y: ${gameState.playerY.toInt()}, Jumping: ${gameState.isJumping}",
            color = GameColors.TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (isGameRunning && gameState.playerY <= 120f) {
            Button(
                onClick = {
                    gameState = gameState.resetPlayer()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.Danger, contentColor = Color.White),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Reset Stuck Player", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color(0xFF2D2D2D))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (isGameRunning) {
                            gameState = gameState.handleTap(offset)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGame(gameState, size)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Beat indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(4) { beatIndex ->
                val isActiveBeat = (gameState.beatCount / 4) % 4 == beatIndex
                val isCurrentBeat = gameState.beatCount % 4 == beatIndex
                
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = when {
                                isCurrentBeat -> GameColors.Accent
                                isActiveBeat -> GameColors.Success
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game controls
        if (!isGameRunning && !showGameOver) {
            Button(
                onClick = { 
                    isGameRunning = true
                    gameState = RhythmGameState().resetPlayer()  // Reset player to safe position
                    showGameOver = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.Success, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Game", fontSize = 18.sp)
            }
        }

        if (showGameOver) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Game Over!",
                        color = GameColors.Danger,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Final Score: ${gameState.score}",
                        color = GameColors.TextPrimary,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Best Combo: ${gameState.bestCombo}x",
                        color = GameColors.Accent,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showGameOver = false
                            gameState = RhythmGameState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.Success, contentColor = Color.White)
                    ) {
                        Text("Play Again")
                    }
                }
            }
        }
    }
}

fun DrawScope.drawGame(gameState: RhythmGameState, size: Size) {
    val playerSize = 30f
    
    // Draw player using actual game state position
    drawRect(
        color = Color(0xFF4CAF50),
        topLeft = Offset(size.width * 0.2f - playerSize/2, gameState.playerY - playerSize/2),
        size = Size(playerSize, playerSize)
    )
    
    // Draw obstacles
    gameState.obstacles.forEach { obstacle ->
        drawRect(
            color = Color(0xFFFF5722),
            topLeft = Offset(obstacle.x, obstacle.y),
            size = Size(obstacle.width, obstacle.height)
        )
    }
    
    // Draw collectibles
    gameState.collectibles.forEach { collectible ->
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 15f,
            center = Offset(collectible.x + collectible.width/2, collectible.y + collectible.height/2)
        )
    }
    
    // Draw power-ups
    gameState.powerUps.forEach { powerUp ->
        drawCircle(
            color = Color(0xFF9C27B0),
            radius = 12f,
            center = Offset(powerUp.x + powerUp.width/2, powerUp.y + powerUp.height/2)
        )
    }
    
    // Draw ground
    drawRect(
        color = Color(0xFF666666),
        topLeft = Offset(0f, size.height - 2f),
        size = Size(size.width, 2f)
    )
}

data class GameObject(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val speed: Float
)

data class RhythmGameState(
    val playerY: Float = 250f,  // Start higher so jumping is more visible
    val playerVelocity: Float = 0f,
    val isJumping: Boolean = false,
    val obstacles: List<GameObject> = emptyList(),
    val collectibles: List<GameObject> = emptyList(),
    val powerUps: List<GameObject> = emptyList(),
    val score: Int = 0,
    val lives: Int = 3,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val beatCount: Int = 0,
    val gameSpeed: Float = 2f,
    val lastObstacleSpawn: Int = 0,
    val lastCollectibleSpawn: Int = 0,
    val lastPowerUpSpawn: Int = 0
) {
    fun update(): RhythmGameState {
        val newGameSpeed = gameSpeed + 0.001f
        
        // Update player physics with bounds checking
        val newPlayerVelocity = if (isJumping) playerVelocity - 1.2f else 0f
        val newPlayerY = (playerY + newPlayerVelocity).coerceIn(100f, 300f)  // Keep player on screen
        
        // Better jumping state logic
        val newIsJumping = when {
            newPlayerY >= 300f -> false  // Hit ground, stop jumping
            newPlayerY <= 100f -> false  // Hit ceiling, stop jumping
            isJumping -> true             // Still in air
            else -> false                 // On ground
        }
        
        // If player is stuck at ceiling, force them down
        val finalPlayerY = if (newPlayerY <= 100f) {
            250f  // Force to ground position
        } else {
            newPlayerY
        }
        
        // Spawn obstacles every 2 beats with random height
        val newObstacles = if (beatCount - lastObstacleSpawn >= 2) {
            obstacles + GameObject(
                x = 400f,
                y = 250f + Random.nextFloat() * 100f, // Random height between 250-350
                width = 30f,
                height = 40f,
                speed = newGameSpeed
            )
        } else {
            obstacles
        }
        
        // Spawn collectibles every 3 beats with random height
        val newCollectibles = if (beatCount - lastCollectibleSpawn >= 3) {
            collectibles + GameObject(
                x = 400f,
                y = 150f + Random.nextFloat() * 150f, // Random height between 150-300
                width = 30f,
                height = 30f,
                speed = newGameSpeed
            )
        } else {
            collectibles
        }
        
        // Spawn power-ups every 5 beats with random height
        val newPowerUps = if (beatCount - lastPowerUpSpawn >= 5) {
            powerUps + GameObject(
                x = 400f,
                y = 100f + Random.nextFloat() * 200f, // Random height between 100-300
                width = 25f,
                height = 25f,
                speed = newGameSpeed
            )
        } else {
            powerUps
        }
        
        // Update object positions
        val updatedObstacles = newObstacles.map { it.copy(x = it.x - it.speed) }.filter { it.x > -50f }
        val updatedCollectibles = newCollectibles.map { it.copy(x = it.x - it.speed) }.filter { it.x > -50f }
        val updatedPowerUps = newPowerUps.map { it.copy(x = it.x - it.speed) }.filter { it.x > -50f }
        
        // Check collisions
        val playerBounds = GameObject(150f, finalPlayerY, 30f, 30f, 0f)
        val collisionWithObstacle = updatedObstacles.any { it.intersects(playerBounds) }
        val collisionWithCollectible = updatedCollectibles.any { it.intersects(playerBounds) }
        val collisionWithPowerUp = updatedPowerUps.any { it.intersects(playerBounds) }
        
        val newLives = if (collisionWithObstacle) lives - 1 else lives
        val newScore = if (collisionWithCollectible) score + 10 else score
        val newCombo = if (collisionWithCollectible) combo + 1 else 0
        val newBestCombo = maxOf(bestCombo, newCombo)
        
        return copy(
            playerY = finalPlayerY,
            playerVelocity = newPlayerVelocity,
            isJumping = newIsJumping,
            obstacles = updatedObstacles,
            collectibles = if (collisionWithCollectible) updatedCollectibles.filter { !it.intersects(playerBounds) } else updatedCollectibles,
            powerUps = if (collisionWithPowerUp) updatedPowerUps.filter { !it.intersects(playerBounds) } else updatedPowerUps,
            score = newScore,
            lives = newLives,
            combo = newCombo,
            bestCombo = newBestCombo,
            gameSpeed = newGameSpeed,
            lastObstacleSpawn = if (beatCount - lastObstacleSpawn >= 2) beatCount else lastObstacleSpawn,
            lastCollectibleSpawn = if (beatCount - lastCollectibleSpawn >= 3) beatCount else lastCollectibleSpawn,
            lastPowerUpSpawn = if (beatCount - lastPowerUpSpawn >= 5) beatCount else lastPowerUpSpawn
        )
    }
    
    fun handleTap(offset: Offset): RhythmGameState {
        // Always allow jumping if not already jumping
        return if (!isJumping) {
            copy(
                isJumping = true,
                playerVelocity = -20f  // Stronger jump
            )
        } else {
            this
        }
    }
    
    fun resetPlayer(): RhythmGameState {
        return copy(
            playerY = 250f,
            playerVelocity = 0f,
            isJumping = false
        )
    }
}

fun GameObject.intersects(other: GameObject): Boolean {
    return x < other.x + other.width &&
           x + width > other.x &&
           y < other.y + other.height &&
           y + height > other.y
}
