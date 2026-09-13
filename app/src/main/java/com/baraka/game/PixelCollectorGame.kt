package com.baraka.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import com.baraka.game.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class PixelCollectorGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                PixelCollectorGameScreen()
            }
        }
    }
}

@Composable
fun PixelCollectorGameScreen() {
    var gameState by remember { mutableStateOf(GameState()) }
    var isGameRunning by remember { mutableStateOf(false) }
    
    LaunchedEffect(isGameRunning) {
        while (isGameRunning) {
            delay(16) // ~60 FPS
            gameState = gameState.update()
        }
    }
    
    GameScreenContainer(horizontalAlignment = Alignment.CenterHorizontally) {
        // Game title
        Text(
            text = "Pixel Collector",
            color = Color.White,
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
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lives: ${gameState.lives}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
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
                        gameState = gameState.handleTap(offset)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                gameState.draw(this)
            }
            
            // Game over overlay
            if (gameState.lives <= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "GAME OVER",
                            color = Color.Red,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Final Score: ${gameState.score}",
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { 
                    if (gameState.lives <= 0) {
                        gameState = GameState()
                        isGameRunning = true
                    } else {
                        isGameRunning = !isGameRunning
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gameState.lives <= 0) Color.Green else if (isGameRunning) Color.Red else Color.Green
                )
            ) {
                Text(
                    text = if (gameState.lives <= 0) "Restart" else if (isGameRunning) "Pause" else "Start"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Instructions
        Text(
            text = "Tap to move the player. Collect green items, avoid red obstacles!",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

data class GameState(
    val player: Player = Player(),
    val collectibles: List<Collectible> = emptyList(),
    val obstacles: List<Obstacle> = emptyList(),
    val score: Int = 0,
    val lives: Int = 3,
    val gameTime: Long = 0
) {
    fun update(): GameState {
        if (lives <= 0) return this
        
        val newGameTime = gameTime + 1
        val newCollectibles = collectibles.map { it.update() }
        val newObstacles = obstacles.map { it.update() }
        
        // Spawn new collectibles and obstacles
        val updatedCollectibles = if (newGameTime % 60L == 0L) {
            newCollectibles + Collectible.spawnRandom()
        } else {
            newCollectibles
        }
        
        val updatedObstacles = if (newGameTime % 120L == 0L) {
            newObstacles + Obstacle.spawnRandom()
        } else {
            newObstacles
        }
        
        // Check collisions
        val (newScore, newLives, finalCollectibles, finalObstacles) = checkCollisions(
            player, updatedCollectibles, updatedObstacles, score, lives
        )
        
        return copy(
            collectibles = finalCollectibles,
            obstacles = finalObstacles,
            score = newScore,
            lives = newLives,
            gameTime = newGameTime
        )
    }
    
    fun handleTap(offset: Offset): GameState {
        if (lives <= 0) return this
        return copy(player = player.moveTo(offset))
    }
    
    fun draw(drawScope: DrawScope) {
        player.draw(drawScope)
        collectibles.forEach { it.draw(drawScope) }
        obstacles.forEach { it.draw(drawScope) }
    }
    
    private fun checkCollisions(
        player: Player,
        collectibles: List<Collectible>,
        obstacles: List<Obstacle>,
        currentScore: Int,
        currentLives: Int
    ): Quadruple<Int, Int, List<Collectible>, List<Obstacle>> {
        var newScore = currentScore
        var newLives = currentLives
        val remainingCollectibles = mutableListOf<Collectible>()
        val remainingObstacles = mutableListOf<Obstacle>()
        
        // Check collectible collisions
        collectibles.forEach { collectible ->
            if (player.bounds.overlaps(collectible.bounds)) {
                newScore += 10
            } else {
                remainingCollectibles.add(collectible)
            }
        }
        
        // Check obstacle collisions
        obstacles.forEach { obstacle ->
            if (player.bounds.overlaps(obstacle.bounds)) {
                newLives -= 1
            } else {
                remainingObstacles.add(obstacle)
            }
        }
        
        return Quadruple(newScore, newLives, remainingCollectibles, remainingObstacles)
    }
}

data class Player(
    val position: Offset = Offset(200f, 200f),
    val size: Float = 30f
) {
    val bounds: Rect get() = Rect(position, Size(size, size))
    
    fun moveTo(target: Offset): Player {
        return copy(position = target)
    }
    
    fun draw(drawScope: DrawScope) {
        drawScope.drawRect(
            color = Color.Blue,
            topLeft = position,
            size = Size(size, size)
        )
    }
}

data class Collectible(
    val position: Offset,
    val size: Float = 20f
) {
    val bounds: Rect get() = Rect(position, Size(size, size))
    
    fun update(): Collectible {
        return copy(position = position.copy(y = position.y + 2f))
    }
    
    fun draw(drawScope: DrawScope) {
        drawScope.drawRect(
            color = Color.Green,
            topLeft = position,
            size = Size(size, size)
        )
    }
    
    companion object {
        fun spawnRandom(): Collectible {
            return Collectible(
                position = Offset(
                    x = (Random.nextFloat() * 300f + 50f).toFloat(),
                    y = -50f
                )
            )
        }
    }
}

data class Obstacle(
    val position: Offset,
    val size: Float = 25f
) {
    val bounds: Rect get() = Rect(position, Size(size, size))
    
    fun update(): Obstacle {
        return copy(position = position.copy(y = position.y + 3f))
    }
    
    fun draw(drawScope: DrawScope) {
        drawScope.drawRect(
            color = Color.Red,
            topLeft = position,
            size = Size(size, size)
        )
    }
    
    companion object {
        fun spawnRandom(): Obstacle {
            return Obstacle(
                position = Offset(
                    x = (Random.nextFloat() * 300f + 50f).toFloat(),
                    y = -50f
                )
            )
        }
    }
}

data class Rect(val topLeft: Offset, val size: Size) {
    fun overlaps(other: Rect): Boolean {
        return !(topLeft.x + size.width < other.topLeft.x ||
                other.topLeft.x + other.size.width < topLeft.x ||
                topLeft.y + size.height < other.topLeft.y ||
                other.topLeft.y + other.size.height < topLeft.y)
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
