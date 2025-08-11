package com.baraka.game

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
    var showUpgradeShop by remember { mutableStateOf(false) }
    
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
            
            // Wave announcement overlay
            if (gameState.waveAnnouncement.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gameState.waveAnnouncement,
                        color = Color.Yellow,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                onClick = { showUpgradeShop = true },
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
        
        // Upgrade Shop Overlay
        if (showUpgradeShop) {
            UpgradeShopOverlay(
                gameState = gameState,
                onUpgrade = { upgradeType ->
                    gameState = gameState.applyUpgrade(upgradeType)
                    showUpgradeShop = false
                },
                onClose = { showUpgradeShop = false }
            )
        }
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
    val gameTime: Long = 0,
    val damageLevel: Int = 1,
    val fireRateLevel: Int = 1,
    val projectileSpeedLevel: Int = 1,
    val waveAnnouncement: String = "",
    val announcementTimer: Int = 0
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
        
        // Handle wave announcements
        val (newAnnouncement, newAnnouncementTimer) = when {
            newWave > currentWave -> {
                val announcement = when {
                    newWave >= 10 -> "BOSS WAVE INCOMING!"
                    newWave >= 5 -> "ARMORED ENEMIES APPEAR!"
                    newWave >= 3 -> "FAST ENEMIES APPROACH!"
                    else -> "Wave ${newWave} Complete!"
                }
                Pair(announcement, 120) // Show for 2 seconds at 60 FPS
            }
            announcementTimer > 0 -> {
                Pair(waveAnnouncement, announcementTimer - 1)
            }
            else -> {
                Pair("", 0)
            }
        }
        
        return copy(
            enemies = finalEnemiesAfterBase,
            projectiles = finalProjectiles,
            score = newScore,
            money = newMoney,
            lives = finalLives,
            currentWave = newWave,
            gameTime = newGameTime,
            waveAnnouncement = newAnnouncement,
            announcementTimer = newAnnouncementTimer
        )
    }
    
    fun handleTap(offset: Offset): BaseDefenderGameState {
        if (lives <= 0) return this
        
        // Create projectile from player position
        val newProjectile = player.shoot(offset, damageLevel, projectileSpeedLevel)
        return copy(projectiles = projectiles + newProjectile)
    }
    
    fun draw(drawScope: DrawScope) {
        player.draw(drawScope)
        enemies.forEach { it.draw(drawScope) }
        projectiles.forEach { it.draw(drawScope) }
    }
    
    fun applyUpgrade(upgradeType: UpgradeType): BaseDefenderGameState {
        return when (upgradeType) {
            UpgradeType.DAMAGE -> {
                if (money >= 100) copy(
                    damageLevel = damageLevel + 1,
                    money = money - 100
                ) else this
            }
            UpgradeType.FIRE_RATE -> {
                if (money >= 150) copy(
                    fireRateLevel = fireRateLevel + 1,
                    money = money - 150
                ) else this
            }
            UpgradeType.PROJECTILE_SPEED -> {
                if (money >= 120) copy(
                    projectileSpeedLevel = projectileSpeedLevel + 1,
                    money = money - 120
                ) else this
            }
        }
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
    val position: Offset = Offset(200f, 380f), // Moved lower for better aiming
    val size: Float = 40f
) {
    fun shoot(target: Offset, damageLevel: Int, projectileSpeedLevel: Int): Projectile {
        val direction = (target - position).normalized()
        val baseDamage = 25
        val baseSpeed = 8f
        val damage = baseDamage + ((damageLevel - 1) * 10)
        val speed = baseSpeed + ((projectileSpeedLevel - 1) * 2f)
        
        return Projectile(
            position = position.copy(y = position.y - 20f),
            direction = direction,
            damage = damage,
            speed = speed
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

enum class EnemyType {
    BASIC, FAST, ARMORED, BOSS
}

data class Enemy(
    val position: Offset,
    val health: Int,
    val maxHealth: Int,
    val speed: Float,
    val moneyReward: Int,
    val enemyType: EnemyType = EnemyType.BASIC,
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
        
        // Different colors and sizes based on enemy type
        val (color, drawSize) = when (enemyType) {
            EnemyType.BASIC -> {
                val baseColor = when {
                    healthPercentage > 0.6f -> Color.Red
                    healthPercentage > 0.3f -> Color(0xFFFF9800) // Orange
                    else -> Color(0xFF8B0000) // Dark Red
                }
                Pair(baseColor, size)
            }
            EnemyType.FAST -> {
                val baseColor = when {
                    healthPercentage > 0.6f -> Color.Cyan
                    healthPercentage > 0.3f -> Color(0xFF00BCD4) // Dark Cyan
                    else -> Color(0xFF006064) // Darker Cyan
                }
                Pair(baseColor, size * 0.8f) // Smaller size
            }
            EnemyType.ARMORED -> {
                val baseColor = when {
                    healthPercentage > 0.6f -> Color(0xFF9C27B0) // Purple
                    healthPercentage > 0.3f -> Color(0xFF7B1FA2) // Dark Purple
                    else -> Color(0xFF4A148C) // Darker Purple
                }
                Pair(baseColor, size * 1.2f) // Larger size
            }
            EnemyType.BOSS -> {
                val baseColor = when {
                    healthPercentage > 0.6f -> Color(0xFFFF5722) // Deep Orange
                    healthPercentage > 0.3f -> Color(0xFFE64A19) // Dark Orange
                    else -> Color(0xFFBF360C) // Darker Orange
                }
                Pair(baseColor, size * 1.5f) // Much larger size
            }
        }
        
        drawScope.drawRect(
            color = color,
            topLeft = position,
            size = Size(drawSize, drawSize)
        )
        
        // Health bar
        drawScope.drawRect(
            color = Color.Green,
            topLeft = position.copy(y = position.y - 10f),
            size = Size(drawSize * healthPercentage, 5f)
        )
        
        // Enemy type indicator
        val typeText = when (enemyType) {
            EnemyType.BASIC -> ""
            EnemyType.FAST -> "F"
            EnemyType.ARMORED -> "A"
            EnemyType.BOSS -> "B"
        }
        
        if (typeText.isNotEmpty()) {
            // Draw a small indicator for special enemy types
            drawScope.drawRect(
                color = Color.White,
                topLeft = position.copy(x = position.x + drawSize - 8f, y = position.y + drawSize - 8f),
                size = Size(8f, 8f)
            )
        }
    }
    
    companion object {
        fun spawnRandom(wave: Int): Enemy {
            val baseHealth = 20 + (wave * 10)
            val baseSpeed = 1f + (wave * 0.2f)
            val baseReward = 5 + (wave * 2)
            
            // Determine enemy type based on wave and random chance
            val enemyType = when {
                wave >= 10 && Random.nextFloat() < 0.1f -> EnemyType.BOSS
                wave >= 5 && Random.nextFloat() < 0.3f -> EnemyType.ARMORED
                wave >= 3 && Random.nextFloat() < 0.4f -> EnemyType.FAST
                else -> EnemyType.BASIC
            }
            
            // Adjust stats based on enemy type
            val finalHealth = when (enemyType) {
                EnemyType.BASIC -> baseHealth
                EnemyType.FAST -> (baseHealth * 0.7f).toInt()
                EnemyType.ARMORED -> (baseHealth * 2.5f).toInt()
                EnemyType.BOSS -> (baseHealth * 4f).toInt()
            }
            
            val finalSpeed = when (enemyType) {
                EnemyType.BASIC -> baseSpeed
                EnemyType.FAST -> baseSpeed * 1.8f
                EnemyType.ARMORED -> baseSpeed * 0.6f
                EnemyType.BOSS -> baseSpeed * 0.4f
            }
            
            val finalReward = when (enemyType) {
                EnemyType.BASIC -> baseReward
                EnemyType.FAST -> (baseReward * 1.2f).toInt()
                EnemyType.ARMORED -> (baseReward * 1.8f).toInt()
                EnemyType.BOSS -> (baseReward * 3f).toInt()
            }
            
            val finalSize = when (enemyType) {
                EnemyType.BASIC -> 30f
                EnemyType.FAST -> 24f
                EnemyType.ARMORED -> 36f
                EnemyType.BOSS -> 45f
            }
            
            return Enemy(
                position = Offset(
                    x = (Random.nextFloat() * 300f + 50f).toFloat(),
                    y = -50f
                ),
                health = finalHealth,
                maxHealth = finalHealth,
                speed = finalSpeed,
                moneyReward = finalReward,
                enemyType = enemyType,
                size = finalSize
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

@Composable
fun UpgradeShopOverlay(
    gameState: BaseDefenderGameState,
    onUpgrade: (UpgradeType) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color(0xFF2D2D2D), RoundedCornerShape(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upgrade Shop",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "Money: $${gameState.money}",
                color = Color.Yellow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Upgrade options
            UpgradeOption(
                title = "Damage +10",
                description = "Increase projectile damage",
                cost = 100,
                currentLevel = gameState.damageLevel,
                onUpgrade = { onUpgrade(UpgradeType.DAMAGE) },
                enabled = gameState.money >= 100
            )
            
            UpgradeOption(
                title = "Fire Rate +20%",
                description = "Shoot faster",
                cost = 150,
                currentLevel = gameState.fireRateLevel,
                onUpgrade = { onUpgrade(UpgradeType.FIRE_RATE) },
                enabled = gameState.money >= 150
            )
            
            UpgradeOption(
                title = "Projectile Speed +25%",
                description = "Faster projectiles",
                cost = 120,
                currentLevel = gameState.projectileSpeedLevel,
                onUpgrade = { onUpgrade(UpgradeType.PROJECTILE_SPEED) },
                enabled = gameState.money >= 120
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Close")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun UpgradeOption(
    title: String,
    description: String,
    cost: Int,
    currentLevel: Int,
    onUpgrade: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF3D3D3D) else Color(0xFF2A2A2A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = if (enabled) Color.Gray else Color.DarkGray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Level: $currentLevel",
                    color = Color.Cyan,
                    fontSize = 12.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$$cost",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onUpgrade,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabled) Color.Green else Color.Gray
                    )
                ) {
                    Text("Upgrade")
                }
            }
        }
    }
}

enum class UpgradeType {
    DAMAGE, FIRE_RATE, PROJECTILE_SPEED
}

// Note: Rect class is defined in PixelCollectorGame.kt

fun Offset.normalized(): Offset {
    val length = kotlin.math.sqrt(x * x + y * y)
    return if (length > 0f) Offset(x / length, y / length) else Offset(0f, 0f)
}

operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
operator fun Offset.times(scalar: Float): Offset = Offset(x * scalar, y * scalar)
