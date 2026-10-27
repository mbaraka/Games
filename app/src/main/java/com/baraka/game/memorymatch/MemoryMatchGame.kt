package com.baraka.game.memorymatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.components.HubBackButton
import com.baraka.game.ui.components.Section
import com.baraka.game.ui.theme.GameColors
import com.baraka.game.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MemoryMatchGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                MemoryMatchGameScreen()
            }
        }
    }
}

enum class Difficulty(val label: String, val pairs: Int, val columns: Int) {
    EASY("Easy", 6, 3),
    MEDIUM("Medium", 8, 4),
    HARD("Hard", 12, 4),
    EXPERT("Expert", 18, 6)
}

@Composable
fun MemoryMatchGameScreen() {
    var difficulty by remember { mutableStateOf<Difficulty?>(null) }

    if (difficulty == null) {
        DifficultySelectScreen(onSelect = { difficulty = it })
    } else {
        MemoryGamePlayScreen(
            difficulty = difficulty!!,
            onChangeDifficulty = { difficulty = null }
        )
    }
}

@Composable
fun DifficultySelectScreen(onSelect: (Difficulty) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Memory Match",
            color = GameColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Choose a difficulty",
            color = GameColors.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Difficulty.entries.forEach { diff ->
            Button(
                onClick = { onSelect(diff) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text("${diff.label} (${diff.pairs} pairs)", fontSize = 18.sp, color = GameColors.TextPrimary)
            }
        }
    }
        HubBackButton(modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
    }
}

@Composable
fun MemoryGamePlayScreen(difficulty: Difficulty, onChangeDifficulty: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { MemoryMatchSettingsRepository.getInstance(context) }
    val soundEngine = remember { MemoryMatchSoundEngine() }
    DisposableEffect(Unit) {
        onDispose { soundEngine.release() }
    }

    var soundEnabled by remember { mutableStateOf(settingsRepository.isSoundEnabled()) }
    fun playSound(cue: MemorySoundCue) {
        if (soundEnabled) soundEngine.play(cue)
    }

    var gameState by remember(difficulty) { mutableStateOf(MemoryGameState(cards = createGameCards(difficulty.pairs))) }
    var selectedCards by remember { mutableStateOf(listOf<Int>()) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameState.matchedPairs) {
        if (gameState.matchedPairs == gameState.cards.size / 2) {
            delay(500)
            playSound(MemorySoundCue.WIN)
        }
    }

              Column(
         modifier = Modifier
             .fillMaxSize()
             .background(MaterialTheme.colorScheme.background)
             .windowInsetsPadding(WindowInsets.safeDrawing)
             .padding(16.dp),
         horizontalAlignment = Alignment.CenterHorizontally
     ) {
         // Top Bar
         Surface(
             modifier = Modifier.fillMaxWidth(),
             color = MaterialTheme.colorScheme.surface
         ) {
             Section(
                 text = "Memory Match",
                 trailingIcon = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                 onTrailingIconClick = {
                     soundEnabled = !soundEnabled
                     settingsRepository.setSoundEnabled(soundEnabled)
                 },
                 modifier = Modifier.padding(16.dp)
             )
         }
         
         Spacer(modifier = Modifier.height(16.dp))
         
         // Game description
         Text(
             text = "Find matching pairs of gaming cards!",
             color = GameColors.TextSecondary,
             fontSize = 16.sp,
             modifier = Modifier.padding(bottom = 16.dp)
         )
         
         // Game grid
         LazyVerticalGrid(
             columns = GridCells.Fixed(difficulty.columns),
             horizontalArrangement = Arrangement.spacedBy(8.dp),
             verticalArrangement = Arrangement.spacedBy(8.dp),
             modifier = Modifier.weight(1f)
         ) {
             items(gameState.cards) { card ->
                 MemoryCard(
                     card = card,
                     isSelected = selectedCards.contains(card.id),
                     isMatched = gameState.matchedCards.contains(card.id),
                     onClick = {
                         if (!isProcessing && !gameState.matchedCards.contains(card.id) && !selectedCards.contains(card.id)) {
                             playSound(MemorySoundCue.FLIP)
                             val newSelected = selectedCards + card.id
                             selectedCards = newSelected

                             if (newSelected.size == 2) {
                                 isProcessing = true
                                 scope.launch {
                                     delay(1000)
                                     val firstCard = gameState.cards.find { it.id == newSelected[0] }
                                     val secondCard = gameState.cards.find { it.id == newSelected[1] }

                                     if (firstCard?.symbol == secondCard?.symbol) {
                                         // Match found
                                         playSound(MemorySoundCue.MATCH)
                                         gameState = gameState.copy(
                                             matchedCards = gameState.matchedCards + newSelected,
                                             matchedPairs = gameState.matchedPairs + 1,
                                             moves = gameState.moves + 1
                                         )
                                     } else {
                                         // No match
                                         playSound(MemorySoundCue.MISMATCH)
                                         gameState = gameState.copy(moves = gameState.moves + 1)
                                     }

                                     selectedCards = emptyList()
                                     isProcessing = false
                                 }
                             }
                         }
                     }
                 )
             }
         }
         
         Spacer(modifier = Modifier.height(16.dp))
         
         // Reset button
         Row(modifier = Modifier.fillMaxWidth()) {
             Button(
                 onClick = {
                     gameState = MemoryGameState(cards = createGameCards(difficulty.pairs))
                     selectedCards = emptyList()
                     isProcessing = false
                 },
                 colors = ButtonDefaults.buttonColors(
                     containerColor = GameColors.Success,
                     contentColor = Color.White
                 ),
                 modifier = Modifier.weight(1f)
             ) {
                 Text("New Game", fontSize = 18.sp)
             }
             Spacer(modifier = Modifier.width(8.dp))
             Button(
                 onClick = onChangeDifficulty,
                 colors = ButtonDefaults.buttonColors(
                     containerColor = MaterialTheme.colorScheme.surfaceVariant,
                     contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                 ),
                 modifier = Modifier.weight(1f)
             ) {
                 Text("Difficulty", fontSize = 18.sp)
             }
         }

         Spacer(modifier = Modifier.height(16.dp))

         // Bottom Score Bar
         Surface(
             modifier = Modifier.fillMaxWidth(),
             color = MaterialTheme.colorScheme.surface
         ) {
             Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp),
                 horizontalArrangement = Arrangement.SpaceEvenly,
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 ScoreCard("Moves", gameState.moves.toString(), GameColors.Success)
                 ScoreCard("Pairs", "${gameState.matchedPairs}/${gameState.cards.size / 2}", GameColors.Accent)
             }
         }
     }
}

@Composable
fun MemoryCard(
    card: MemoryCard,
    isSelected: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit
) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isSelected || isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isMatched) GameColors.Success
                else if (isSelected) GameColors.Accent
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 2.dp,
                color = if (isMatched) GameColors.Success else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected || isMatched) {
            Text(
                text = card.symbol,
                color = GameColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "?",
                color = GameColors.TextMuted,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ScoreCard(label: String, value: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = GameColors.TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class MemoryCard(
    val id: Int,
    val symbol: String
)

data class MemoryGameState(
    val cards: List<MemoryCard>,
    val selectedCards: List<Int> = emptyList(),
    val matchedCards: List<Int> = emptyList(),
    val matchedPairs: Int = 0,
    val moves: Int = 0
)

private val ALL_SYMBOLS = listOf(
    "🎮", "🎯", "⚡", "🏆", "💎", "🔥", "🌟", "🎪",
    "🚀", "🎲", "🎨", "🎸", "🐉", "👾", "🍀", "⚔️",
    "🛡️", "🔮"
)

fun createGameCards(pairCount: Int): List<MemoryCard> {
    val symbols = ALL_SYMBOLS.take(pairCount)
    val cards = mutableListOf<MemoryCard>()

    symbols.forEachIndexed { index, symbol ->
        cards.add(MemoryCard(index * 2, symbol))
        cards.add(MemoryCard(index * 2 + 1, symbol))
    }

    return cards.shuffled()
}
