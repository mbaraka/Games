package com.baraka.game.numbercruncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class NumberCruncherGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                NumberCruncherGameScreen()
            }
        }
    }
}

@Composable
fun NumberCruncherGameScreen() {
    var gameState by remember { mutableStateOf(NumberCruncherGameState()) }
    var selectedTile by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showGameOver by remember { mutableStateOf(false) }
    var showVictory by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.targetValue) {
        if (gameState.targetValue == 0) {
            delay(500)
            showVictory = true
        }
    }

    LaunchedEffect(gameState.movesLeft) {
        if (gameState.movesLeft <= 0) {
            delay(500)
            showGameOver = true
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
            text = "Number Cruncher",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Game stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GameStat("Target", gameState.targetValue.toString(), Color(0xFF4CAF50))
            GameStat("Moves", gameState.movesLeft.toString(), Color(0xFFFF9800))
            GameStat("Score", gameState.score.toString(), Color(0xFF2196F3))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game grid
        GameGrid(
            grid = gameState.grid,
            selectedTile = selectedTile,
            onTileSelected = { row, col ->
                if (selectedTile == null) {
                    selectedTile = Pair(row, col)
                } else {
                    val (selectedRow, selectedCol) = selectedTile!!
                    if (canCombine(gameState.grid[selectedRow][selectedCol], gameState.grid[row][col])) {
                        gameState = gameState.combineTiles(selectedRow, selectedCol, row, col)
                        selectedTile = null
                    } else {
                        selectedTile = Pair(row, col)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Game controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    gameState = gameState.newGame()
                    selectedTile = null
                    showGameOver = false
                    showVictory = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("New Game", color = Color.White)
            }

            Button(
                onClick = {
                    gameState = gameState.hint()
                },
                enabled = gameState.movesLeft > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Hint", color = Color.White)
            }
        }

        // Instructions
        Text(
            text = "Combine same numbers to reach the target!",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    // Game Over Dialog
    if (showGameOver) {
        GameOverDialog(
            score = gameState.score,
            onNewGame = {
                gameState = gameState.newGame()
                selectedTile = null
                showGameOver = false
            },
            onDismiss = { showGameOver = false }
        )
    }

    // Victory Dialog
    if (showVictory) {
        VictoryDialog(
            score = gameState.score,
            movesUsed = gameState.totalMoves - gameState.movesLeft,
            onNewGame = {
                gameState = gameState.newGame()
                selectedTile = null
                showVictory = false
            },
            onDismiss = { showVictory = false }
        )
    }
}

@Composable
fun GameStat(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameGrid(
    grid: Array<Array<Int>>,
    selectedTile: Pair<Int, Int>?,
    onTileSelected: (Int, Int) -> Unit
) {
    val gridSize = 4
    val tileSize = 70.dp
    val spacing = 8.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (row in 0 until gridSize) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                for (col in 0 until gridSize) {
                    val isSelected = selectedTile == Pair(row, col)
                    val tileValue = grid[row][col]
                    
                    GameTile(
                        value = tileValue,
                        isSelected = isSelected,
                        onClick = { onTileSelected(row, col) },
                        modifier = Modifier.size(tileSize)
                    )
                }
            }
        }
    }
}

@Composable
fun GameTile(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> Color(0xFFFFD700) // Gold for selected
        value == 0 -> Color(0xFF2D2D2D) // Dark for empty
        else -> getTileColor(value)
    }
    
    val borderColor = if (isSelected) Color(0xFFFFA000) else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures {
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Game Over!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Final Score: $score\n\nBetter luck next time!",
                color = Color.Gray,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("New Game", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
            ) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF2D2D2D),
        titleContentColor = Color.White,
        textContentColor = Color.Gray
    )
}

@Composable
fun VictoryDialog(
    score: Int,
    movesUsed: Int,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Victory! 🎉",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Congratulations!\n\nFinal Score: $score\nMoves Used: $movesUsed",
                color = Color.Gray,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Play Again", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
            ) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF2D2D2D),
        titleContentColor = Color.White,
        textContentColor = Color.Gray
    )
}

fun getTileColor(value: Int): Color {
    return when (value) {
        2 -> Color(0xFFE3F2FD)
        4 -> Color(0xFFBBDEFB)
        8 -> Color(0xFF90CAF9)
        16 -> Color(0xFF64B5F6)
        32 -> Color(0xFF42A5F5)
        64 -> Color(0xFF2196F3)
        128 -> Color(0xFF1E88E5)
        256 -> Color(0xFF1976D2)
        512 -> Color(0xFF1565C0)
        1024 -> Color(0xFF0D47A1)
        else -> Color(0xFF0D47A1)
    }
}

fun canCombine(value1: Int, value2: Int): Boolean {
    return value1 > 0 && value2 > 0 && value1 == value2
}

data class NumberCruncherGameState(
    val grid: Array<Array<Int>> = Array(4) { Array(4) { 0 } },
    val targetValue: Int = 256,
    val movesLeft: Int = 20,
    val score: Int = 0,
    val totalMoves: Int = 20
) {
    fun newGame(): NumberCruncherGameState {
        val newGrid = Array(4) { Array(4) { 0 } }
        
        // Fill with random starting numbers (2, 4, 8)
        repeat(8) {
            var row: Int
            var col: Int
            do {
                row = Random.nextInt(4)
                col = Random.nextInt(4)
            } while (newGrid[row][col] != 0)
            
            newGrid[row][col] = listOf(2, 4, 8).random()
        }
        
        return copy(
            grid = newGrid,
            targetValue = 256,
            movesLeft = 20,
            score = 0,
            totalMoves = 20
        )
    }

    fun combineTiles(row1: Int, col1: Int, row2: Int, col2: Int): NumberCruncherGameState {
        if (!canCombine(grid[row1][col1], grid[row2][col2])) {
            return this
        }

        val newGrid = grid.map { it.clone() }.toTypedArray()
        val combinedValue = grid[row1][col1] + grid[row2][col2]
        
        newGrid[row1][col1] = combinedValue
        newGrid[row2][col2] = 0
        
        val newScore = score + combinedValue
        val newMovesLeft = movesLeft - 1
        
        // Check if target reached
        val newTargetValue = if (combinedValue >= targetValue) 0 else targetValue
        
        // Add new random tile
        addRandomTile(newGrid)
        
        return copy(
            grid = newGrid,
            targetValue = newTargetValue,
            movesLeft = newMovesLeft,
            score = newScore
        )
    }

    fun hint(): NumberCruncherGameState {
        // Find a valid move and highlight it
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                if (grid[row][col] > 0) {
                    // Check adjacent tiles
                    val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                    for ((dRow, dCol) in directions) {
                        val newRow = row + dRow
                        val newCol = col + dCol
                        if (newRow in 0..3 && newCol in 0..3 && 
                            canCombine(grid[row][col], grid[newRow][newCol])) {
                            return copy(movesLeft = movesLeft - 1)
                        }
                    }
                }
            }
        }
        
        // No valid moves found
        return copy(movesLeft = movesLeft - 1)
    }

    private fun addRandomTile(grid: Array<Array<Int>>) {
        val emptyPositions = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                if (grid[row][col] == 0) {
                    emptyPositions.add(Pair(row, col))
                }
            }
        }
        
        if (emptyPositions.isNotEmpty()) {
            val (row, col) = emptyPositions.random()
            grid[row][col] = listOf(2, 4).random()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NumberCruncherGameState

        if (!grid.contentDeepEquals(other.grid)) return false
        if (targetValue != other.targetValue) return false
        if (movesLeft != other.movesLeft) return false
        if (score != other.score) return false
        if (totalMoves != other.totalMoves) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + targetValue
        result = 31 * result + movesLeft
        result = 31 * result + score
        result = 31 * result + totalMoves
        return result
    }
}
