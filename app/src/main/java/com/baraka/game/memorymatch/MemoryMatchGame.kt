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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun MemoryMatchGameScreen() {
    var gameState by remember { mutableStateOf(MemoryGameState()) }
    var selectedCards by remember { mutableStateOf(listOf<Int>()) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameState.matchedPairs) {
        if (gameState.matchedPairs == gameState.cards.size / 2) {
            delay(500)
            // Game won!
        }
    }

              Column(
         modifier = Modifier
             .fillMaxSize()
             .background(Color(0xFF1E1E1E))
             .padding(16.dp),
         horizontalAlignment = Alignment.CenterHorizontally
     ) {
         // Top Bar
         Surface(
             modifier = Modifier.fillMaxWidth(),
             color = Color(0xFF2A2A2A)
         ) {
             Text(
                 text = "Memory Match",
                 color = Color.White,
                 fontSize = 24.sp,
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier.padding(16.dp),
                 textAlign = TextAlign.Center
             )
         }
         
         Spacer(modifier = Modifier.height(16.dp))
         
         // Game description
         Text(
             text = "Find matching pairs of gaming cards!",
             color = Color.Gray,
             fontSize = 16.sp,
             modifier = Modifier.padding(bottom = 16.dp)
         )
         
         // Game grid
         LazyVerticalGrid(
             columns = GridCells.Fixed(4),
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
                                         gameState = gameState.copy(
                                             matchedCards = gameState.matchedCards + newSelected,
                                             matchedPairs = gameState.matchedPairs + 1,
                                             moves = gameState.moves + 1
                                         )
                                     } else {
                                         // No match
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
         Button(
             onClick = {
                 gameState = MemoryGameState()
                 selectedCards = emptyList()
                 isProcessing = false
             },
             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
             modifier = Modifier.fillMaxWidth()
         ) {
             Text("New Game", fontSize = 18.sp)
         }
         
         Spacer(modifier = Modifier.height(16.dp))
         
         // Bottom Score Bar
         Surface(
             modifier = Modifier.fillMaxWidth(),
             color = Color(0xFF2A2A2A)
         ) {
             Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp),
                 horizontalArrangement = Arrangement.SpaceEvenly,
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 ScoreCard("Moves", gameState.moves.toString(), Color(0xFF4CAF50))
                 ScoreCard("Pairs", "${gameState.matchedPairs}/${gameState.cards.size / 2}", Color(0xFFFF5722))
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
                if (isMatched) Color(0xFF4CAF50)
                else if (isSelected) Color(0xFFFF5722)
                else Color(0xFF2A2A2A)
            )
            .border(
                width = 2.dp,
                color = if (isMatched) Color(0xFF4CAF50) else Color(0xFF444444),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected || isMatched) {
            Text(
                text = card.symbol,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "?",
                color = Color.Gray,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ScoreCard(label: String, value: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.Gray,
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
    val cards: List<MemoryCard> = createGameCards(),
    val selectedCards: List<Int> = emptyList(),
    val matchedCards: List<Int> = emptyList(),
    val matchedPairs: Int = 0,
    val moves: Int = 0
)

fun createGameCards(): List<MemoryCard> {
    val symbols = listOf("🎮", "🎯", "⚡", "🏆", "💎", "🔥", "🌟", "🎪")
    val cards = mutableListOf<MemoryCard>()
    
    symbols.forEachIndexed { index, symbol ->
        cards.add(MemoryCard(index * 2, symbol))
        cards.add(MemoryCard(index * 2 + 1, symbol))
    }
    
    return cards.shuffled()
}
