package com.baraka.game.basedefender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
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

        // Game stats - horizontally scrollable
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            item {
                Spacer(modifier =Modifier.height(10.dp))
            }

            item {
                Text(
                    text = "Wave: ${gameState.currentWave}/20",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    text = "Money: $${gameState.money}",
                    color = Color.Yellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    text = "Bases: ${gameState.multiBaseLevel}",
                    color = Color.Cyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    text = "Lives: ${gameState.lives}",
                    color = Color.Red,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Wave progress bar
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { gameState.currentWave.toFloat() / 20f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color.Cyan,
            trackColor = Color.Gray.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Game canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)  // Reduced from 500dp to 400dp for better proportions
                .background(Color(0xFF2D2D2D))
        ) {
            // Background pattern
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBackgroundPattern(this)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Pass the actual canvas height to handleTap
                            gameState = gameState.handleTap(offset, size.height.toFloat())
                        }
                    }
            ) {
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
            if (gameState.gameStatus == GameStatus.GAME_OVER) {
                GameOverOverlay(gameState = gameState)
            }

            // Victory overlay
            if (gameState.gameStatus == GameStatus.VICTORY) {
                VictoryOverlay(gameState = gameState)
            }
        }

        // Scrollable UI elements below the grid
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Weapon Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeaponButton(
                        weaponType = WeaponType.BASIC,
                        isSelected = gameState.currentWeapon == WeaponType.BASIC,
                        onClick = { gameState = gameState.switchWeapon(WeaponType.BASIC) }
                    )
                    WeaponButton(
                        weaponType = WeaponType.FIRE,
                        isSelected = gameState.currentWeapon == WeaponType.FIRE,
                        onClick = { gameState = gameState.switchWeapon(WeaponType.FIRE) }
                    )
                    WeaponButton(
                        weaponType = WeaponType.ICE,
                        isSelected = gameState.currentWeapon == WeaponType.ICE,
                        onClick = { gameState = gameState.switchWeapon(WeaponType.ICE) }
                    )
                    WeaponButton(
                        weaponType = WeaponType.EXPLOSIVE,
                        isSelected = gameState.currentWeapon == WeaponType.EXPLOSIVE,
                        onClick = { gameState = gameState.switchWeapon(WeaponType.EXPLOSIVE) }
                    )
                }
            }

            item {
                // Power-up Status
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item {
                        PowerUpStatusButton(
                            text = "Rapid Fire",
                            isActive = gameState.rapidFireActive,
                            color = Color.Blue,
                            onClick = { /* Can't manually activate */ }
                        )
                    }
                    item {
                        PowerUpStatusButton(
                            text = "Shield",
                            isActive = gameState.shieldActive,
                            color = Color.Green,
                            onClick = { /* Can't manually activate */ }
                        )
                    }
                    item {
                        PowerUpStatusButton(
                            text = "Time Slow",
                            isActive = gameState.timeSlowActive,
                            color = Color(0xFF9C27B0),
                            onClick = { /* Can't manually activate */ }
                        )
                    }
                    item {
                        PowerUpStatusButton(
                            text = "Mega Shot",
                            isActive = gameState.megaShotAvailable,
                            color = Color.Red,
                            onClick = { /* Can't manually activate */ }
                        )
                    }
                }
            }

            item {
                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            when (gameState.gameStatus) {
                                GameStatus.GAME_OVER, GameStatus.VICTORY -> {
                                    gameState = gameState.startNewGame()
                                    isGameRunning = true
                                }

                                else -> {
                                    isGameRunning = !isGameRunning
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (gameState.gameStatus) {
                                GameStatus.GAME_OVER, GameStatus.VICTORY -> Color.Green
                                else -> if (isGameRunning) Color.Red else Color.Green
                            }
                        )
                    ) {
                        Text(
                            text = when (gameState.gameStatus) {
                                GameStatus.GAME_OVER, GameStatus.VICTORY -> "New Game"
                                else -> if (isGameRunning) "Pause" else "Start"
                            }
                        )
                    }

                    Button(
                        onClick = { showUpgradeShop = true },
                        enabled = gameState.money >= 100
                    ) {
                        Text("Upgrade Shop")
                    }
                }
            }

            item {
                // Instructions
                Text(
                    text = "Tap to aim and shoot! Defend your base from incoming enemies!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Debug collision status
            item {
                val collisionStatus = if (gameState.enemies.any { enemy ->
                    val basePositions = gameState.getBasePositions(400f)
                    basePositions.any { basePos ->
                        val baseBounds = Rect(basePos, Size(40f, 40f))
                        enemy.bounds.overlaps(baseBounds)
                    }
                }) "⚠️ COLLISION DETECTED! ⚠️" else "✅ No Collision"
                
                Text(
                    text = collisionStatus,
                    color = if (collisionStatus.contains("COLLISION")) Color.Red else Color.Green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }


            // Upgrade Shop Overlay
            if (showUpgradeShop) {
                upgradeShopOverlay(
                    gameState = gameState,
                    onUpgrade = { upgradeType ->
                        gameState = gameState.applyUpgrade(upgradeType)
                        showUpgradeShop = false
                    },
                    onClose = { showUpgradeShop = false }
                )
            }

            item {
                Spacer(modifier =Modifier.height(20.dp))
            }
        }
    }
}

data class BaseDefenderGameState(
    val player: Defender = Defender(),
    val enemies: List<Enemy> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val powerUps: List<PowerUp> = emptyList(),
    val score: Int = 0,
    val money: Int = 100,
    val lives: Int = 3,
    val currentWave: Int = 1,
    val gameTime: Long = 0,
    val damageLevel: Int = 1,
    val fireRateLevel: Int = 1,
    val projectileSpeedLevel: Int = 1,
    val multiBaseLevel: Int = 1,
    val currentWeapon: WeaponType = WeaponType.BASIC,
    val waveAnnouncement: String = "",
    val announcementTimer: Int = 0,
    // Power-up effects
    val rapidFireActive: Boolean = false,
    val rapidFireTimer: Int = 0,
    val shieldActive: Boolean = false,
    val shieldTimer: Int = 0,
    val timeSlowActive: Boolean = false,
    val timeSlowTimer: Int = 0,
    val megaShotAvailable: Boolean = false,
    // Game state
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val highScore: Int = 0,
    val totalEnemiesKilled: Int = 0,
    val totalMoneyEarned: Int = 0,
    val totalProjectilesFired: Int = 0,
    val gameStartTime: Long = 0L,
    // Visual effects
    val screenShake: Float = 0f,
    val particles: List<Particle> = emptyList(),
    val hitEffects: List<HitEffect> = emptyList(),
    // Canvas dimensions
    val canvasHeight: Float = 400f
) {
    fun update(): BaseDefenderGameState {
        if (gameStatus != GameStatus.PLAYING) return this

        val newGameTime = gameTime + 1
        // Use the actual canvas height from the UI (400dp converted to pixels)
        val actualCanvasHeight = 400f
        val newEnemies =
            enemies.map { it.update(timeSlowActive) }.filter { it.position.y < actualCanvasHeight }
        val newProjectiles = projectiles.map { it.update() }.filter { it.position.y > 0f }

        // Update power-ups (remove ones that are off-screen)
        val newPowerUps = powerUps.filter { it.position.y < actualCanvasHeight }

        // Update particles and effects
        val updatedParticles = particles.mapNotNull { it.update() }
        val updatedHitEffects = hitEffects.mapNotNull { it.update() }

        // Spawn new enemies
        val updatedEnemies = if (newGameTime % 120L == 0L) {
            newEnemies + Enemy.spawnRandom(currentWave)
        } else {
            newEnemies
        }

        // Spawn power-ups randomly (every 10 seconds)
        val updatedPowerUps = if (newGameTime % 600L == 0L && Random.nextFloat() < 0.3f) {
            newPowerUps + PowerUp.spawnRandom()
        } else {
            newPowerUps
        }

        // Check collisions (projectile-enemy and power-up collection)
        val (newScore, newMoney, _, finalEnemies, finalProjectiles, finalPowerUps, powerUpEffects, collisionParticles, collisionHitEffects) = checkCollisions(
            updatedEnemies, newProjectiles, updatedPowerUps, score, money, lives
        )

        // Check if enemies reached any base using proper collision detection
        val basePositions = getBasePositions(actualCanvasHeight)
        val enemiesAtBase = finalEnemies.count { enemy ->
            basePositions.any { basePos ->
                // Simplified collision detection: enemy must be near the base area
                val enemyBottomY = enemy.position.y + enemy.size
                val baseTopY = basePos.y
                
                // Enemy is considered "at base" only when it reaches the base area
                // Use a small tolerance (10 pixels) to make it feel natural
                val isAtBase = enemyBottomY >= baseTopY - 10f
                
                // Debug: Log collision detection
                if (isAtBase) {
                    println("DEBUG: Enemy at ${enemy.position} reached base at $basePos")
                    println("DEBUG: Enemy bottom Y: $enemyBottomY, Base top Y: $baseTopY")
                    println("DEBUG: Canvas height: $actualCanvasHeight, Base Y: ${basePos.y}")
                    println("DEBUG: Is at base: $isAtBase")
                }
                
                isAtBase
            }
        }

        // Apply shield protection if active
        val finalLives = if (shieldActive) {
            lives // Shield protects against damage
        } else {
            lives - enemiesAtBase
        }
        
        // Debug: Log lives calculation
        if (enemiesAtBase > 0) {
            println("DEBUG: $enemiesAtBase enemies hit base. Lives: $lives -> $finalLives")
        }

        // Remove enemies that hit any base
        val finalEnemiesAfterBase = finalEnemies.filter { enemy ->
            !basePositions.any { basePos ->
                // Use the same simplified collision detection logic
                val enemyBottomY = enemy.position.y + enemy.size
                val baseTopY = basePos.y
                val isAtBase = enemyBottomY >= baseTopY - 10f
                
                isAtBase
            }
        }

        // Add screen shake when enemies reach base (only if shield is not active)
        val baseShake = if (enemiesAtBase > 0 && !shieldActive) 3f * enemiesAtBase else 0f

        // Update screen shake
        val newScreenShake = ((screenShake + baseShake) * 0.9f).coerceAtLeast(0f)

        // Check wave completion
        val newWave = if (finalEnemiesAfterBase.isEmpty() && updatedEnemies.isNotEmpty()) {
            currentWave + 1
        } else {
            currentWave
        }

        // Check victory condition (complete 20 waves)
        val newGameStatus = when {
            finalLives <= 0 -> GameStatus.GAME_OVER
            newWave > 20 -> GameStatus.VICTORY
            else -> GameStatus.PLAYING
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

        // Update power-up timers and effects
        val newRapidFireTimer =
            if (powerUpEffects.rapidFireActive) 600 else if (rapidFireTimer > 0) rapidFireTimer - 1 else 0
        val newShieldTimer =
            if (powerUpEffects.shieldActive) 480 else if (shieldTimer > 0) shieldTimer - 1 else 0
        val newTimeSlowTimer =
            if (powerUpEffects.timeSlowActive) 720 else if (timeSlowTimer > 0) timeSlowTimer - 1 else 0

        return copy(
            enemies = finalEnemiesAfterBase,
            projectiles = finalProjectiles,
            powerUps = finalPowerUps,
            score = newScore,
            money = newMoney,
            lives = finalLives,
            currentWave = newWave,
            gameTime = newGameTime,
            waveAnnouncement = newAnnouncement,
            announcementTimer = newAnnouncementTimer,
            rapidFireActive = newRapidFireTimer > 0,
            rapidFireTimer = newRapidFireTimer,
            shieldActive = newShieldTimer > 0,
            shieldTimer = newShieldTimer,
            timeSlowActive = newTimeSlowTimer > 0,
            timeSlowTimer = newTimeSlowTimer,
            megaShotAvailable = powerUpEffects.megaShotAvailable,
            gameStatus = newGameStatus,
            highScore = maxOf(highScore, newScore),
            totalEnemiesKilled = totalEnemiesKilled + (if (newScore > score) (newScore - score) / 10 else 0),
            totalMoneyEarned = totalMoneyEarned + (if (newMoney > money) newMoney - money else 0),
            screenShake = newScreenShake,
            particles = updatedParticles + collisionParticles,
            hitEffects = updatedHitEffects + collisionHitEffects
        )
    }

    fun handleTap(offset: Offset, canvasHeight: Float): BaseDefenderGameState {
        if (gameStatus != GameStatus.PLAYING) return this

        val newProjectiles = mutableListOf<Projectile>()

        // Calculate positions for all active bases
        val basePositions = getBasePositions(canvasHeight)

        if (megaShotAvailable) {
            // Mega shot: one powerful projectile from each base
            basePositions.forEach { basePosition ->
                val megaProjectile =
                    player.copy(position = basePosition)
                        .shoot(offset, damageLevel * 3, projectileSpeedLevel, currentWeapon)
                newProjectiles.add(megaProjectile)
            }
            return copy(
                projectiles = projectiles + newProjectiles,
                megaShotAvailable = false,
                totalProjectilesFired = totalProjectilesFired + basePositions.size
            )
        } else if (rapidFireActive) {
            // Rapid fire: shoot 3 projectiles from each base
            basePositions.forEach { basePosition ->
                repeat(3) { index ->
                    val spread = (index - 1) * 0.1f // Small spread
                    val spreadOffset = offset.copy(x = offset.x + (spread * 50f))
                    val projectile =
                        player.copy(position = basePosition)
                            .shoot(spreadOffset, damageLevel, projectileSpeedLevel, currentWeapon)
                    newProjectiles.add(projectile)
                }
            }
            return copy(
                projectiles = projectiles + newProjectiles,
                totalProjectilesFired = totalProjectilesFired + (basePositions.size * 3)
            )
        } else {
            // Normal shot: one projectile from each base
            basePositions.forEach { basePosition ->
                val newProjectile =
                    player.copy(position = basePosition)
                        .shoot(offset, damageLevel, projectileSpeedLevel, currentWeapon)
                newProjectiles.add(newProjectile)
            }
            return copy(
                projectiles = projectiles + newProjectiles,
                totalProjectilesFired = totalProjectilesFired + basePositions.size
            )
        }
    }

    fun getBasePositions(canvasHeight: Float): List<Offset> {
        val positions = mutableListOf<Offset>()

        // Calculate base positions dynamically based on canvas width
        // We need to pass canvas width to this function, but for now we'll use a reasonable default
        // The actual canvas width will be available in the draw function
        val canvasWidth = 400f // Default width, will be overridden in draw function

        when (multiBaseLevel) {
            1 -> {
                // Single base at center
                positions.add(Offset(canvasWidth / 2f, canvasHeight - player.size - 10f))
            }

            2 -> {
                // Two bases: left and right
                val baseY = canvasHeight - player.size - 10f
                val leftX = canvasWidth * 0.25f
                val rightX = canvasWidth * 0.75f
                positions.add(Offset(leftX, baseY))  // Left base
                positions.add(Offset(rightX, baseY))  // Right base
            }

            3 -> {
                // Three bases: left, center, right
                val baseY = canvasHeight - player.size - 10f
                val leftX = canvasWidth * 0.2f
                val centerX = canvasWidth * 0.5f
                val rightX = canvasWidth * 0.8f
                positions.add(Offset(leftX, baseY))  // Left base
                positions.add(Offset(centerX, baseY))  // Center base
                positions.add(Offset(rightX, baseY))  // Right base
            }
        }

        return positions
    }

    fun switchWeapon(weaponType: WeaponType): BaseDefenderGameState {
        return copy(currentWeapon = weaponType)
    }

    fun startNewGame(): BaseDefenderGameState {
        return copy(
            player = Defender(),
            enemies = emptyList(),
            projectiles = emptyList(),
            powerUps = emptyList(),
            score = 0,
            money = 100,
            lives = 3,
            currentWave = 1,
            gameTime = 0,
            damageLevel = 1,
            fireRateLevel = 1,
            projectileSpeedLevel = 1,
            multiBaseLevel = 1,
            currentWeapon = WeaponType.BASIC,
            waveAnnouncement = "",
            announcementTimer = 0,
            rapidFireActive = false,
            rapidFireTimer = 0,
            shieldActive = false,
            shieldTimer = 0,
            timeSlowActive = false,
            timeSlowTimer = 0,
            megaShotAvailable = false,
            gameStatus = GameStatus.PLAYING,
            totalEnemiesKilled = 0,
            totalMoneyEarned = 0,
            totalProjectilesFired = 0,
            gameStartTime = System.currentTimeMillis(),
            screenShake = 0f,
            particles = emptyList(),
            hitEffects = emptyList()
        )
    }

    fun addScreenShake(intensity: Float): BaseDefenderGameState {
        return copy(screenShake = (screenShake + intensity).coerceAtMost(10f))
    }

    fun addParticles(newParticles: List<Particle>): BaseDefenderGameState {
        return copy(particles = particles + newParticles)
    }

    fun addHitEffect(position: Offset, color: Color): BaseDefenderGameState {
        val hitEffect = HitEffect(
            position = position,
            color = color,
            size = 15f,
            life = 30,
            maxLife = 30
        )
        return copy(hitEffects = hitEffects + hitEffect)
    }

    fun createExplosionParticles(position: Offset, color: Color): List<Particle> {
        return List(8) { index ->
            val angle = (index * 45f) * (Math.PI / 180f)
            val speed = 2f + Random.nextFloat() * 3f
            val velocity = Offset(
                x = (kotlin.math.cos(angle) * speed).toFloat(),
                y = (kotlin.math.sin(angle) * speed).toFloat()
            )

            Particle(
                position = position,
                velocity = velocity,
                color = color,
                size = 3f + Random.nextFloat() * 4f,
                life = 60 + Random.nextInt(30),
                maxLife = 90
            )
        }
    }

    fun draw(drawScope: DrawScope) {
        // Get dynamic base positions based on canvas height
        val canvasHeight = drawScope.size.height
        val basePositions = getBasePositions(canvasHeight)

        // Debug: Draw collision status indicator
        val collisionStatus = if (enemies.any { enemy ->
            basePositions.any { basePos ->
                val baseBounds = Rect(basePos, Size(player.size, player.size))
                enemy.bounds.overlaps(baseBounds)
            }
        }) "COLLISION DETECTED!" else "No Collision"
        
        // Draw debug background
        drawScope.drawRect(
            color = if (collisionStatus == "COLLISION DETECTED!") Color.Red.copy(alpha = 0.7f) else Color.Green.copy(alpha = 0.7f),
            topLeft = Offset(10f, 10f),
            size = Size(150f, 30f)
        )

        // Apply screen shake offset
        val shakeOffset = Offset(
            x = (Random.nextFloat() - 0.5f) * screenShake,
            y = (Random.nextFloat() - 0.5f) * screenShake
        )

        // Draw game elements with shake offset
        drawScope.withTransform({
            translate(shakeOffset.x, shakeOffset.y)
        }) {
            // Draw all bases
            basePositions.forEach { position ->
                val dynamicPlayer = player.copy(position = position)
                dynamicPlayer.draw(drawScope)

                // Draw shield effect if active
                if (shieldActive) {
                    drawShieldEffect(drawScope, position)
                }
            }

            enemies.forEach { it.draw(drawScope) }
            projectiles.forEach { it.draw(drawScope) }
            powerUps.forEach { it.draw(drawScope) }
        }

        // Draw particles and effects (not affected by screen shake)
        particles.forEach { it.draw(drawScope) }
        hitEffects.forEach { it.draw(drawScope) }

        // Draw time slow effect indicator
        if (timeSlowActive) {
            drawTimeSlowEffect(drawScope)
        }
    }

    private fun drawTimeSlowEffect(drawScope: DrawScope) {
        // Draw time slow visual effect across the screen
        val alpha = 0.1f + (0.05f * kotlin.math.sin(gameTime * 0.15f)).toFloat()

        // Draw subtle purple overlay
        drawScope.drawRect(
            color = Color(0xFF9C27B0).copy(alpha = alpha),
            topLeft = Offset(0f, 0f),
            size = Size(drawScope.size.width, drawScope.size.height)
        )

        // Draw time slow particles
        repeat(5) { index ->
            val x =
                (index * drawScope.size.width / 5f) + (gameTime * 0.5f) % (drawScope.size.width / 5f)
            val y = (gameTime * 0.3f + index * 50f) % drawScope.size.height

            drawScope.drawCircle(
                color = Color(0xFF9C27B0).copy(alpha = 0.3f),
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }

    private fun drawShieldEffect(drawScope: DrawScope, basePosition: Offset) {
        // Draw shield bubble around base
        val shieldRadius = player.size * 1.5f
        val shieldAlpha = 0.3f + (0.2f * kotlin.math.sin(gameTime * 0.1f)).toFloat()

        // Outer glow
        drawScope.drawCircle(
            color = Color.Cyan.copy(alpha = shieldAlpha * 0.5f),
            radius = shieldRadius + 5f,
            center = basePosition.copy(
                x = basePosition.x + player.size / 2f,
                y = basePosition.y + player.size / 2f
            )
        )

        // Main shield
        drawScope.drawCircle(
            color = Color.Cyan.copy(alpha = shieldAlpha),
            radius = shieldRadius,
            center = basePosition.copy(
                x = basePosition.x + player.size / 2f,
                y = basePosition.y + player.size / 2f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )

        // Inner shield
        drawScope.drawCircle(
            color = Color.Cyan.copy(alpha = shieldAlpha * 0.7f),
            radius = shieldRadius * 0.7f,
            center = basePosition.copy(
                x = basePosition.x + player.size / 2f,
                y = basePosition.y + player.size / 2f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }

    private fun drawPowerUpStatus(drawScope: DrawScope) {
        var yOffset = 50f

        if (rapidFireActive) {
            drawScope.drawRect(
                color = Color.Blue.copy(alpha = 0.7f),
                topLeft = Offset(10f, yOffset),
                size = Size(100f, 20f)
            )
            yOffset += 25f
        }

        if (shieldActive) {
            drawScope.drawRect(
                color = Color.Green.copy(alpha = 0.7f),
                topLeft = Offset(10f, yOffset),
                size = Size(100f, 20f)
            )
            yOffset += 25f
        }

        if (timeSlowActive) {
            drawScope.drawRect(
                color = Color(0xFF9C27B0).copy(alpha = 0.7f),
                topLeft = Offset(10f, yOffset),
                size = Size(100f, 20f)
            )
        }
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

            UpgradeType.MULTI_BASE -> {
                if (money >= 50 && multiBaseLevel < 3) copy(
                    multiBaseLevel = multiBaseLevel + 1,
                    money = money - 50
                ) else this
            }
        }
    }

    private fun checkCollisions(
        enemies: List<Enemy>,
        projectiles: List<Projectile>,
        powerUps: List<PowerUp>,
        currentScore: Int,
        currentMoney: Int,
        currentLives: Int
    ): CollisionResult {
        var newParticles = emptyList<Particle>()
        var newHitEffects = emptyList<HitEffect>()
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
                    val damagedEnemy = enemy.takeDamage(projectile.damage, projectile.weaponType)

                    // Add hit effect
                    newHitEffects = newHitEffects + HitEffect(
                        position = enemy.position,
                        color = when (projectile.weaponType) {
                            WeaponType.FIRE -> Color.Red
                            WeaponType.ICE -> Color.Cyan
                            WeaponType.EXPLOSIVE -> Color(0xFFFF9800)
                            else -> Color.Yellow
                        },
                        size = 15f,
                        life = 30,
                        maxLife = 30
                    )

                    if (damagedEnemy.health <= 0) {
                        newScore += 10
                        newMoney += enemy.moneyReward

                        // Add explosion particles
                        val explosionColor = when (enemy.enemyType) {
                            EnemyType.BOSS -> Color(0xFFFF5722)
                            EnemyType.ARMORED -> Color(0xFF9C27B0)
                            EnemyType.FAST -> Color.Cyan
                            else -> Color.Red
                        }
                        newParticles =
                            newParticles + createExplosionParticles(enemy.position, explosionColor)
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

        // Check power-up collection (simplified - player always collects power-ups)
        val remainingPowerUps = mutableListOf<PowerUp>()
        var powerUpEffects = PowerUpEffects()

        powerUps.forEach { powerUp ->
            // Check if power-up is near any of the player bases (simplified collision)
            var powerUpCollected = false
            val basePositions =
                getBasePositions(400f) // Use actual canvas height for collision detection

            for (basePosition in basePositions) {
                val playerBounds = Rect(basePosition, Size(40f, 40f))
                if (powerUp.bounds.overlaps(playerBounds)) {
                    powerUpCollected = true
                    // Apply power-up effect
                    powerUpEffects = when (powerUp.powerUpType) {
                        PowerUpType.RAPID_FIRE -> PowerUpEffects(
                            rapidFireActive = true,
                            shieldActive = false,
                            timeSlowActive = false,
                            megaShotAvailable = false
                        )

                        PowerUpType.SHIELD -> PowerUpEffects(
                            rapidFireActive = false,
                            shieldActive = true,
                            timeSlowActive = false,
                            megaShotAvailable = false
                        )

                        PowerUpType.TIME_SLOW -> PowerUpEffects(
                            rapidFireActive = false,
                            shieldActive = false,
                            timeSlowActive = true,
                            megaShotAvailable = false
                        )

                        PowerUpType.MEGA_SHOT -> PowerUpEffects(
                            rapidFireActive = false,
                            shieldActive = false,
                            timeSlowActive = false,
                            megaShotAvailable = true
                        )
                    }
                    break
                }
            }

            if (!powerUpCollected) {
                remainingPowerUps.add(powerUp)
            }
        }

        return CollisionResult(
            newScore,
            newMoney,
            newLives,
            remainingEnemies,
            remainingProjectiles,
            remainingPowerUps,
            powerUpEffects,
            newParticles,
            newHitEffects
        )
    }
}

data class CollisionResult(
    val score: Int,
    val money: Int,
    val lives: Int,
    val enemies: List<Enemy>,
    val projectiles: List<Projectile>,
    val powerUps: List<PowerUp>,
    val powerUpEffects: PowerUpEffects,
    val particles: List<Particle>,
    val hitEffects: List<HitEffect>
)

data class PowerUpEffects(
    val rapidFireActive: Boolean = false,
    val shieldActive: Boolean = false,
    val timeSlowActive: Boolean = false,
    val megaShotAvailable: Boolean = false
)

data class Defender(
    val position: Offset = Offset(
        200f,
        0f
    ), // Will be calculated dynamically based on canvas height
    val size: Float = 40f
) {
    fun getPosition(canvasHeight: Float): Offset {
        // Position base at bottom of canvas with small margin
        return Offset(200f, canvasHeight - size - 10f)
    }

    fun shoot(
        target: Offset,
        damageLevel: Int,
        projectileSpeedLevel: Int,
        weaponType: WeaponType
    ): Projectile {
        val direction = (target - position).normalized()
        val baseDamage = 25
        val baseSpeed = 8f

        // Adjust damage and speed based on weapon type
        val (weaponDamage, weaponSpeed) = when (weaponType) {
            WeaponType.BASIC -> Pair(1.0f, 1.0f)
            WeaponType.FIRE -> Pair(0.8f, 1.2f) // Lower damage but faster
            WeaponType.ICE -> Pair(0.6f, 1.5f) // Lower damage but much faster
            WeaponType.EXPLOSIVE -> Pair(1.5f, 0.7f) // Higher damage but slower
        }

        val damage = ((baseDamage + ((damageLevel - 1) * 10)) * weaponDamage).toInt()
        val speed = (baseSpeed + ((projectileSpeedLevel - 1) * 2f)) * weaponSpeed

        return Projectile(
            position = position.copy(y = position.y - 20f),
            direction = direction,
            damage = damage,
            speed = speed,
            weaponType = weaponType
        )
    }

    fun draw(drawScope: DrawScope) {
        // Draw base/body
        drawScope.drawRect(
            color = Color.Blue,
            topLeft = position,
            size = Size(size, size)
        )

        // Draw border
        drawScope.drawRect(
            color = Color.White,
            topLeft = position,
            size = Size(size, size),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Draw turret (smaller rectangle on top)
        drawScope.drawRect(
            color = Color(0xFF2196F3), // Darker blue
            topLeft = position.copy(x = position.x + size * 0.25f, y = position.y - size * 0.25f),
            size = Size(size * 0.5f, size * 0.5f)
        )

        // Draw turret border
        drawScope.drawRect(
            color = Color.White,
            topLeft = position.copy(x = position.x + size * 0.25f, y = position.y - size * 0.25f),
            size = Size(size * 0.5f, size * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
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
    val size: Float = 30f,
    val isOnFire: Boolean = false,
    val isFrozen: Boolean = false,
    val fireTimer: Int = 0,
    val freezeTimer: Int = 0
) {
    val bounds: Rect get() = Rect(position, Size(size, size))

    fun update(timeSlowActive: Boolean = false): Enemy {
        val effectiveSpeed = when {
            timeSlowActive -> speed * 0.3f
            isFrozen -> speed * 0.3f
            else -> speed
        }

        val newPosition = position.copy(y = position.y + effectiveSpeed)

        // Handle status effects
        val newFireTimer = if (fireTimer > 0) fireTimer - 1 else 0
        val newFreezeTimer = if (freezeTimer > 0) freezeTimer - 1 else 0

        // Fire damage over time
        val fireDamage = if (isOnFire && newFireTimer > 0 && newFireTimer % 30 == 0) 5 else 0
        val newHealth = health - fireDamage

        return copy(
            position = newPosition,
            health = newHealth,
            isOnFire = newFireTimer > 0,
            isFrozen = newFreezeTimer > 0,
            fireTimer = newFireTimer,
            freezeTimer = newFreezeTimer
        )
    }

    fun takeDamage(damage: Int, weaponType: WeaponType = WeaponType.BASIC): Enemy {
        val newHealth = health - damage

        // Apply weapon-specific effects
        val (newIsOnFire, newFireTimer) = when (weaponType) {
            WeaponType.FIRE -> Pair(true, 120) // 2 seconds of fire
            else -> Pair(isOnFire, fireTimer)
        }

        val (newIsFrozen, newFreezeTimer) = when (weaponType) {
            WeaponType.ICE -> Pair(true, 90) // 1.5 seconds of freeze
            else -> Pair(isFrozen, freezeTimer)
        }

        return copy(
            health = newHealth,
            isOnFire = newIsOnFire,
            isFrozen = newIsFrozen,
            fireTimer = newFireTimer,
            freezeTimer = newFreezeTimer
        )
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
                topLeft = position.copy(
                    x = position.x + drawSize - 8f,
                    y = position.y + drawSize - 8f
                ),
                size = Size(8f, 8f)
            )
        }

        // Status effect indicators
        if (isOnFire) {
            // Fire effect
            drawScope.drawRect(
                color = Color.Red.copy(alpha = 0.7f),
                topLeft = position.copy(x = position.x - 2f, y = position.y - 2f),
                size = Size(drawSize + 4f, drawSize + 4f)
            )
        }

        if (isFrozen) {
            // Ice effect
            drawScope.drawRect(
                color = Color.Cyan.copy(alpha = 0.5f),
                topLeft = position.copy(x = position.x - 1f, y = position.y - 1f),
                size = Size(drawSize + 2f, drawSize + 2f)
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
                    x = (Random.nextFloat() * 350f + 25f).toFloat(),  // Updated to use full canvas width
                    y = -50f  // Keep spawning above screen
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
    val size: Float = 8f,
    val weaponType: WeaponType = WeaponType.BASIC,
    val effectTimer: Int = 0
) {
    val bounds: Rect get() = Rect(position, Size(size, size))

    fun update(): Projectile {
        return copy(
            position = position + (direction * speed),
            effectTimer = if (effectTimer > 0) effectTimer - 1 else 0
        )
    }

    fun draw(drawScope: DrawScope) {
        val color = when (weaponType) {
            WeaponType.BASIC -> Color.Yellow
            WeaponType.FIRE -> Color.Red
            WeaponType.ICE -> Color.Cyan
            WeaponType.EXPLOSIVE -> Color(0xFFFF9800)
        }

        // Draw projectile with glow effect
        drawScope.drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = size * 1.5f,
            center = position
        )

        drawScope.drawCircle(
            color = color,
            radius = size,
            center = position
        )

        // Draw center core
        drawScope.drawCircle(
            color = Color.White,
            radius = size * 0.5f,
            center = position
        )

        // Add weapon-specific effects
        when (weaponType) {
            WeaponType.BASIC -> {

            }

            WeaponType.FIRE -> {
                // Fire trail effect
                drawScope.drawCircle(
                    color = Color(0xFFFF5722).copy(alpha = 0.6f),
                    radius = size * 0.8f,
                    center = position.copy(x = position.x - 3f, y = position.y - 3f)
                )
            }

            WeaponType.ICE -> {
                // Ice sparkle effect
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 2f,
                    center = position.copy(x = position.x + 3f, y = position.y + 3f)
                )
            }

            WeaponType.EXPLOSIVE -> {
                // Explosive glow effect
                drawScope.drawCircle(
                    color = Color(0xFFFF9800).copy(alpha = 0.4f),
                    radius = size * 1.2f,
                    center = position
                )
            }
        }
    }
}


fun LazyListScope.upgradeShopOverlay(
    gameState: BaseDefenderGameState,
    onUpgrade: (UpgradeType) -> Unit,
    onClose: () -> Unit
) {

    item {
        Text(
            text = "Upgrade Shop",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
    }

    item {
        Text(
            text = "Money: $${gameState.money}",
            color = Color.Yellow,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    item {
        UpgradeOption(
            title = "Damage +10",
            description = "Increase projectile damage",
            cost = 100,
            currentLevel = gameState.damageLevel,
            onUpgrade = { onUpgrade(UpgradeType.DAMAGE) },
            enabled = gameState.money >= 100
        )
    }

    item {
        UpgradeOption(
            title = "Fire Rate +20%",
            description = "Shoot faster",
            cost = 150,
            currentLevel = gameState.fireRateLevel,
            onUpgrade = { onUpgrade(UpgradeType.FIRE_RATE) },
            enabled = gameState.money >= 150
        )
    }

    item {
        UpgradeOption(
            title = "Projectile Speed +25%",
            description = "Faster projectiles",
            cost = 120,
            currentLevel = gameState.projectileSpeedLevel,
            onUpgrade = { onUpgrade(UpgradeType.PROJECTILE_SPEED) },
            enabled = gameState.money >= 120
        )
    }

    item {
        UpgradeOption(
            title = "Multi Base",
            description = "Add another base (${gameState.multiBaseLevel}/3)",
            cost = 50,
            currentLevel = gameState.multiBaseLevel,
            onUpgrade = { onUpgrade(UpgradeType.MULTI_BASE) },
            enabled = gameState.money >= 50 && gameState.multiBaseLevel < 3
        )
    }

    item {
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Close")
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
    DAMAGE, FIRE_RATE, PROJECTILE_SPEED, MULTI_BASE
}

enum class WeaponType {
    BASIC, FIRE, ICE, EXPLOSIVE
}

enum class PowerUpType {
    RAPID_FIRE, SHIELD, TIME_SLOW, MEGA_SHOT
}

enum class GameStatus {
    PLAYING, GAME_OVER, VICTORY, PAUSED
}

data class Particle(
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val life: Int,
    val maxLife: Int
) {
    fun update(): Particle? {
        val newPosition = position + velocity
        val newLife = life - 1
        return if (newLife > 0) {
            copy(
                position = newPosition,
                life = newLife
            )
        } else null
    }

    fun draw(drawScope: DrawScope) {
        val alpha = (life.toFloat() / maxLife.toFloat()).coerceIn(0f, 1f)
        drawScope.drawCircle(
            color = color.copy(alpha = alpha),
            radius = size * alpha,
            center = position
        )
    }
}

data class HitEffect(
    val position: Offset,
    val color: Color,
    val size: Float,
    val life: Int,
    val maxLife: Int
) {
    fun update(): HitEffect? {
        val newLife = life - 1
        return if (newLife > 0) {
            copy(life = newLife)
        } else null
    }

    fun draw(drawScope: DrawScope) {
        val alpha = (life.toFloat() / maxLife.toFloat()).coerceIn(0f, 1f)
        val currentSize = size * (1f + (1f - alpha) * 2f)

        drawScope.drawCircle(
            color = color.copy(alpha = alpha * 0.7f),
            radius = currentSize,
            center = position
        )

        drawScope.drawCircle(
            color = color.copy(alpha = alpha * 0.3f),
            radius = currentSize * 0.5f,
            center = position
        )
    }
}

data class PowerUp(
    val position: Offset,
    val powerUpType: PowerUpType,
    val size: Float = 20f
) {
    val bounds: Rect get() = Rect(position, Size(size, size))

    fun draw(drawScope: DrawScope) {
        val color = when (powerUpType) {
            PowerUpType.RAPID_FIRE -> Color.Blue
            PowerUpType.SHIELD -> Color.Green
            PowerUpType.TIME_SLOW -> Color(0xFF9C27B0) // Purple
            PowerUpType.MEGA_SHOT -> Color.Red
        }

        // Draw power-up with pulsing effect
        drawScope.drawRect(
            color = color,
            topLeft = position,
            size = Size(size, size)
        )

        // Add a border to make it stand out
        drawScope.drawRect(
            color = Color.White,
            topLeft = position.copy(x = position.x - 1f, y = position.y - 1f),
            size = Size(size + 2f, size + 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Add power-up type indicator
        val symbol = when (powerUpType) {
            PowerUpType.RAPID_FIRE -> "R"
            PowerUpType.SHIELD -> "S"
            PowerUpType.TIME_SLOW -> "T"
            PowerUpType.MEGA_SHOT -> "M"
        }

        // Draw symbol in center (simplified as a small rectangle)
        drawScope.drawRect(
            color = Color.White,
            topLeft = position.copy(x = position.x + size * 0.3f, y = position.y + size * 0.3f),
            size = Size(size * 0.4f, size * 0.4f)
        )
    }

    companion object {
        fun spawnRandom(): PowerUp {
            val powerUpType = PowerUpType.values().random()
            return PowerUp(
                position = Offset(
                    x = (Random.nextFloat() * 350f + 25f).toFloat(),  // Updated to use full canvas width
                    y = -30f  // Keep spawning above screen
                ),
                powerUpType = powerUpType
            )
        }
    }
}


@Composable
fun PowerUpStatusButton(
    text: String,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = false, // Power-ups can't be manually activated
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) color else Color.Gray.copy(alpha = 0.3f),
            disabledContainerColor = if (isActive) color else Color.Gray.copy(alpha = 0.3f)
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isActive) Color.White else Color.Gray,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Rect class for collision detection
data class Rect(val topLeft: Offset, val size: Size) {
    fun overlaps(other: Rect): Boolean {
        return !(topLeft.x + size.width < other.topLeft.x ||
                other.topLeft.x + other.size.width < topLeft.x ||
                topLeft.y + size.height < other.topLeft.y ||
                other.topLeft.y + other.size.height < topLeft.y)
    }
}

fun Offset.normalized(): Offset {
    val length = kotlin.math.sqrt(x * x + y * y)
    return if (length > 0f) Offset(x / length, y / length) else Offset(0f, 0f)
}

operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
operator fun Offset.times(scalar: Float): Offset = Offset(x * scalar, y * scalar)

@Composable
fun GameOverOverlay(gameState: BaseDefenderGameState) {
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
                text = "GAME OVER",
                color = Color.Red,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            // Game statistics
            GameStatsCard(
                title = "Final Score",
                value = "${gameState.score}",
                color = Color.Yellow
            )

            GameStatsCard(
                title = "Waves Survived",
                value = "${gameState.currentWave}",
                color = Color.Cyan
            )

            GameStatsCard(
                title = "Enemies Killed",
                value = "${gameState.totalEnemiesKilled}",
                color = Color.Green
            )

            GameStatsCard(
                title = "Money Earned",
                value = "$${gameState.totalMoneyEarned}",
                color = Color(0xFFFF9800)
            )

            GameStatsCard(
                title = "Projectiles Fired",
                value = "${gameState.totalProjectilesFired}",
                color = Color.Magenta
            )

            if (gameState.score > gameState.highScore) {
                Text(
                    text = "🎉 NEW HIGH SCORE! 🎉",
                    color = Color.Yellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VictoryOverlay(gameState: BaseDefenderGameState) {
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
                text = "🎉 VICTORY! 🎉",
                color = Color.Green,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Text(
                text = "You've defended your base through all 20 waves!",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )

            // Victory statistics
            GameStatsCard(
                title = "Final Score",
                value = "${gameState.score}",
                color = Color.Yellow
            )

            GameStatsCard(
                title = "Waves Completed",
                value = "20",
                color = Color.Cyan
            )

            GameStatsCard(
                title = "Enemies Defeated",
                value = "${gameState.totalEnemiesKilled}",
                color = Color.Green
            )

            GameStatsCard(
                title = "Total Money",
                value = "$${gameState.totalMoneyEarned}",
                color = Color(0xFFFF9800)
            )

            if (gameState.score > gameState.highScore) {
                Text(
                    text = "🏆 NEW HIGH SCORE! 🏆",
                    color = Color.Yellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun drawBackgroundPattern(drawScope: DrawScope) {
    val gridSize = 40f
    val alpha = 0.1f

    // Get the actual canvas dimensions from the drawScope
    val canvasWidth = drawScope.size.width
    val canvasHeight = drawScope.size.height  // Remove 0.8f multiplier to cover full canvas height

    // Draw vertical grid lines
    for (x in 0..(canvasWidth / gridSize).toInt()) {
        drawScope.drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(x * gridSize, 0f),
            end = Offset(x * gridSize, canvasHeight),
            strokeWidth = 1f
        )
    }

    // Draw horizontal grid lines
    for (y in 0..(canvasHeight / gridSize).toInt()) {
        drawScope.drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(0f, y * gridSize),
            end = Offset(canvasWidth, y * gridSize),
            strokeWidth = 1f
        )
    }

    // Draw some subtle dots at intersections
    for (x in 0..(canvasWidth / gridSize).toInt()) {
        for (y in 0..(canvasHeight / gridSize).toInt()) {
            drawScope.drawCircle(
                color = Color.White.copy(alpha = alpha * 0.5f),
                radius = 1f,
                center = Offset(x * gridSize, y * gridSize)
            )
        }
    }
}

@Composable
fun GameStatsCard(
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D3D3D)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 16.sp
            )

            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
