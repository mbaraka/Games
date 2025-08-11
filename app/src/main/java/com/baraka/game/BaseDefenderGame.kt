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
import com.baraka.game.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class BaseDefenderGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                BaseDefenderGameScreen()
            }
        }
    }
}

@Composable
fun BaseDefenderGameScreen() {
    var gameState by remember { mutableStateOf(BaseDefenderGameState()) }
    var isGameRunning by remember { mutableStateOf(false) }
    
    LaunchedEffect(isGameRunning) {
        while (isGameRunning) {
            delay(16) // ~60 FPS
            gameState = gameState.update()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Game title
        Text(
            text = "Base Defender",
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
                text = "Wave: ${gameState.currentWave}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Money: $${gameState.money}",
                color = Color.Yellow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lives: ${gameState.lives}",
                color = Color.Red,
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
                        Text(
                            text = "Waves Survived: ${gameState.currentWave}",
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
                        gameState = BaseDefenderGameState()
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
            
            Button(
                onClick = { /* TODO: Open upgrade shop */ },
                enabled = gameState.money >= 100
            ) {
                Text("Upgrade Shop")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Instructions
        Text(
            text = "Tap to aim and shoot! Defend your base from incoming enemies!",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

data class BaseDefenderGameState(
    val player: Defender = Defender(),
    val enemies: List<Enemy> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val score: Int = 0,
    val money: Int = 100,
    val lives: Int = 3,
    val currentWave: Int = 1,
    val gameTime: Long = 0
) {
    fun update(): BaseDefenderGameState {
        if (lives <= 0) return this
        
        val newGameTime = gameTime + 1
        val newEnemies = enemies.map { it.update() }.filter { it.position.y < 400f }
        val newProjectiles = projectiles.map { it.update() }.filter { it.position.y > 0f }
        
        // Spawn new enemies
        val updatedEnemies = if (newGameTime % 120L == 0L) {
            newEnemies + Enemy.spawnRandom(currentWave)
        } else {
            newEnemies
        }
        
        // Check collisions
        val (newScore, newMoney, newLives, finalEnemies, finalProjectiles) = checkCollisions(
            updatedEnemies, newProjectiles, score, money, lives
        )
        
        // Check if enemies reached base
        val enemiesAtBase = finalEnemies.count { it.position.y >= 380f }
        val finalLives = newLives - enemiesAtBase
        val finalEnemiesAfterBase = finalEnemies.filter { it.position.y < 380f }
        
        // Check wave completion
        val newWave = if (finalEnemiesAfterBase.isEmpty() && updatedEnemies.isNotEmpty()) {
            currentWave + 1
        } else {
            currentWave
        }
        
        return copy(
            enemies = finalEnemiesAfterBase,
            projectiles = finalProjectiles,
            score = newScore,
            money = newMoney,
            lives = finalLives,
            currentWave = newWave,
            gameTime = newGameTime
        )
    }
    
    fun handleTap(offset: Offset): BaseDefenderGameState {
        if (lives <= 0) return this
        
        // Create projectile from player position
        val newProjectile = player.shoot(offset)
        return copy(projectiles = projectiles + newProjectile)
    }
    
    fun draw(drawScope: DrawScope) {
        player.draw(drawScope)
        enemies.forEach { it.draw(drawScope) }
        projectiles.forEach { it.draw(drawScope) }
    }
    
    private fun checkCollisions(
        enemies: List<Enemy>,
        projectiles: List<Projectile>,
        currentScore: Int,
        currentMoney: Int,
        currentLives: Int
    ): CollisionResult {
        var newScore = currentScore
        var newMoney = currentMoney
        var newLives = currentLives
        val remainingEnemies = mutableListOf<Enemy>()
        val remainingProjectiles = mutableListOf<Projectile>()
        
        // Check projectile-enemy collisions
        projectiles.forEach { projectile ->
            var projectileHit = false
            enemies.forEach { enemy ->
                if (!projectileHit && projectile.bounds.overlaps(enemy.bounds)) {
                    projectileHit = true
                    val damagedEnemy = enemy.takeDamage(projectile.damage)
                    if (damagedEnemy.health <= 0) {
                        newScore += 10
                        newMoney += enemy.moneyReward
                    } else {
                        remainingEnemies.add(damagedEnemy)
                    }
                }
            }
            if (!projectileHit) {
                remainingProjectiles.add(projectile)
            }
        }
        
        // Add enemies that weren't hit
        enemies.forEach { enemy ->
            if (!projectiles.any { it.bounds.overlaps(enemy.bounds) }) {
                remainingEnemies.add(enemy)
            }
        }
        
        return CollisionResult(newScore, newMoney, newLives, remainingEnemies, remainingProjectiles)
    }
}

data class CollisionResult(
    val score: Int,
    val money: Int,
    val lives: Int,
    val enemies: List<Enemy>,
    val projectiles: List<Projectile>
)

data class Defender(
    val position: Offset = Offset(200f, 350f),
    val size: Float = 40f
) {
    fun shoot(target: Offset): Projectile {
        val direction = (target - position).normalized()
        return Projectile(
            position = position.copy(y = position.y - 20f),
            direction = direction,
            damage = 25
        )
    }
    
    fun draw(drawScope: DrawScope) {
        drawScope.drawRect(
            color = Color.Blue,
            topLeft = position,
            size = Size(size, size)
        )
    }
}

data class Enemy(
    val position: Offset,
    val health: Int,
    val maxHealth: Int,
    val speed: Float,
    val moneyReward: Int,
    val size: Float = 30f
) {
    val bounds: Rect get() = Rect(position, Size(size, size))
    
    fun update(): Enemy {
        return copy(position = position.copy(y = position.y + speed))
    }
    
    fun takeDamage(damage: Int): Enemy {
        return copy(health = health - damage)
    }
    
    fun draw(drawScope: DrawScope) {
        val healthPercentage = health.toFloat() / maxHealth.toFloat()
        val color = when {
            healthPercentage > 0.6f -> Color.Red
            healthPercentage > 0.3f -> Color(0xFFFF9800) // Orange
            else -> Color(0xFF8B0000) // Dark Red
        }
        
        drawScope.drawRect(
            color = color,
            topLeft = position,
            size = Size(size, size)
        )
        
        // Health bar
        drawScope.drawRect(
            color = Color.Green,
            topLeft = position.copy(y = position.y - 10f),
            size = Size(size * healthPercentage, 5f)
        )
    }
    
    companion object {
        fun spawnRandom(wave: Int): Enemy {
            val baseHealth = 20 + (wave * 10)
            val baseSpeed = 1f + (wave * 0.2f)
            val baseReward = 5 + (wave * 2)
            
            return Enemy(
                position = Offset(
                    x = (Random.nextFloat() * 300f + 50f).toFloat(),
                    y = -50f
                ),
                health = baseHealth,
                maxHealth = baseHealth,
                speed = baseSpeed,
                moneyReward = baseReward
            )
        }
    }
}

data class Projectile(
    val position: Offset,
    val direction: Offset,
    val damage: Int,
    val speed: Float = 8f,
    val size: Float = 8f
) {
    val bounds: Rect get() = Rect(position, Size(size, size))
    
    fun update(): Projectile {
        return copy(
            position = position + (direction * speed)
        )
    }
    
    fun draw(drawScope: DrawScope) {
        drawScope.drawRect(
            color = Color.Yellow,
            topLeft = position,
            size = Size(size, size)
        )
    }
}

// Note: Rect class is defined in PixelCollectorGame.kt

fun Offset.normalized(): Offset {
    val length = kotlin.math.sqrt(x * x + y * y)
    return if (length > 0f) Offset(x / length, y / length) else Offset(0f, 0f)
}

operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
operator fun Offset.times(scalar: Float): Offset = Offset(x * scalar, y * scalar)
