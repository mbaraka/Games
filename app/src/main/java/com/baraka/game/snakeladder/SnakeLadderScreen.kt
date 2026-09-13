package com.baraka.game.snakeladder

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val TokenColors = listOf(
    Color(0xFF42A5F5),
    Color(0xFFEF5350),
    Color(0xFF66BB6A),
    Color(0xFFFFCA28),
    Color(0xFFAB47BC),
    Color(0xFFFF7043)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnakeLadderScreen(
    uiState: SnakeLadderUiState,
    onBack: () -> Unit,
    onStartGame: (List<Boolean>) -> Unit,
    onRoll: () -> Unit,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snakes & Ladders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        when (uiState.phase) {
            SnakeLadderPhase.Menu -> MenuContent(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                onStartGame = onStartGame
            )

            SnakeLadderPhase.Playing,
            SnakeLadderPhase.Finished -> GameContent(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                uiState = uiState,
                onRoll = onRoll,
                onPlayAgain = onPlayAgain,
                onMenu = onMenu
            )
        }
    }
}

@Composable
private fun MenuContent(
    modifier: Modifier = Modifier,
    onStartGame: (List<Boolean>) -> Unit
) {
    var totalPlayers by remember { mutableIntStateOf(2) }
    var playerIsCpu by remember { mutableStateOf(listOf(false, true)) }

    fun resizeSlots(newTotal: Int) {
        totalPlayers = newTotal
        playerIsCpu = List(newTotal) { i -> playerIsCpu.getOrElse(i) { false } }
    }

    val canStart = playerIsCpu.any { !it }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "2D board · Roll, climb ladders, slide down snakes. First to 100 wins.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Players (2–6)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Total seats at the board. Then set each as Human (pass the phone) or CPU.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (2..6).forEach { n ->
                FilterChip(
                    selected = totalPlayers == n,
                    onClick = { resizeSlots(n) },
                    label = { Text("$n") }
                )
            }
        }
        Text(
            text = "Who is each player?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
        (0 until totalPlayers).forEach { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "P${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(0.35f)
                )
                FilterChip(
                    selected = !playerIsCpu[index],
                    onClick = {
                        playerIsCpu =
                            playerIsCpu.mapIndexed { i, cpu -> if (i == index) false else cpu }
                    },
                    label = { Text("Human") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = playerIsCpu[index],
                    onClick = {
                        playerIsCpu =
                            playerIsCpu.mapIndexed { i, cpu -> if (i == index) true else cpu }
                    },
                    label = { Text("CPU") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { playerIsCpu = List(totalPlayers) { false } },
                modifier = Modifier.weight(1f)
            ) {
                Text("All human")
            }
            OutlinedButton(
                onClick = { playerIsCpu = List(totalPlayers) { it > 0 } },
                modifier = Modifier.weight(1f)
            ) {
                Text("P1 human, rest CPU")
            }
        }
        if (!canStart) {
            Text(
                text = "Pick at least one Human (someone has to roll on this device).",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = { onStartGame(playerIsCpu) },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            val humans = playerIsCpu.count { !it }
            val cpus = playerIsCpu.count { it }
            Text("Start game ($totalPlayers: $humans human, $cpus CPU)")
        }
    }
}

private fun winnerHeadline(uiState: SnakeLadderUiState): String {
    val w = uiState.winnerIndex ?: return "Game over"
    return when {
        uiState.humanCount == 1 && !uiState.isCpuPlayer(w) -> "You won!"
        uiState.isCpuPlayer(w) -> "Player ${w + 1} (CPU) wins!"
        else -> "Player ${w + 1} wins!"
    }
}

@Composable
private fun GameContent(
    modifier: Modifier = Modifier,
    uiState: SnakeLadderUiState,
    onRoll: () -> Unit,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    val sounds = rememberGameSounds()
    val haptics = LocalHapticFeedback.current

    var prevPositions by remember { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(uiState.positions) {
        val prev = prevPositions
        if (prev != null && prev != uiState.positions) {
            sounds.hop()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        prevPositions = uiState.positions
    }

    var prevDice by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(uiState.lastDice) {
        if (uiState.lastDice != null && uiState.lastDice != prevDice) {
            sounds.diceLand()
        }
        prevDice = uiState.lastDice
    }

    var prevSpecial by remember { mutableStateOf<SpecialMoveEvent?>(null) }
    LaunchedEffect(uiState.specialMove) {
        val sm = uiState.specialMove
        if (sm != null && sm != prevSpecial) {
            if (sm.type == SpecialMoveType.LADDER) sounds.ladder() else sounds.snake()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevSpecial = sm
    }

    var prevWinner by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(uiState.winnerIndex) {
        if (uiState.winnerIndex != null && uiState.winnerIndex != prevWinner) {
            sounds.win()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevWinner = uiState.winnerIndex
    }

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoardGrid(
            positions = uiState.positions,
            currentPlayerIndex = uiState.currentPlayerIndex,
            phase = uiState.phase,
            specialMove = uiState.specialMove,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        )

        Text(
            text = "Links: → next square after landing. ↑n = ladder top from foot n. ↓n = snake tail from head n.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        PlayerLegend(uiState = uiState)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val turnLabel = when {
                    uiState.phase == SnakeLadderPhase.Finished -> winnerHeadline(uiState)
                    uiState.phase == SnakeLadderPhase.Playing && uiState.currentIsCpu -> {
                        val n = uiState.currentPlayerIndex + 1
                        "Player $n (CPU)'s turn"
                    }
                    uiState.phase == SnakeLadderPhase.Playing -> {
                        val n = uiState.currentPlayerIndex + 1
                        val soleHumanYou = uiState.humanCount == 1 &&
                            !uiState.isCpuPlayer(uiState.currentPlayerIndex)
                        if (soleHumanYou) "Your turn"
                        else "Player $n's turn — pass the device"
                    }
                    else -> ""
                }
                Text(
                    text = turnLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DiceView(
                        value = uiState.lastDice,
                        isRolling = uiState.isRolling && uiState.lastDice == null
                    )
                    Text(
                        text = uiState.lastDice?.let { "Last roll: $it" } ?: "Roll the die",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                uiState.lastMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (uiState.phase == SnakeLadderPhase.Playing) {
                    val canRoll = !uiState.isRolling && !uiState.currentIsCpu
                    Button(
                        onClick = onRoll,
                        enabled = canRoll,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isRolling) "Rolling…" else "Roll dice")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPlayAgain,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play again")
                        }
                        OutlinedButton(
                            onClick = onMenu,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Menu")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerLegend(uiState: SnakeLadderUiState) {
    if (uiState.positions.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        uiState.positions.indices.forEach { i ->
            val label = when {
                uiState.isCpuPlayer(i) -> "P${i + 1} · CPU"
                uiState.humanCount == 1 -> "You"
                else -> "P${i + 1} · Human"
            }
            val isTurn = uiState.phase == SnakeLadderPhase.Playing && i == uiState.currentPlayerIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                PlayerPiece(
                    playerIndex = i,
                    diameter = 18.dp,
                    isCurrentTurn = isTurn,
                    showNumber = true
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Medium,
                    color = if (isTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PlayerPiece(
    playerIndex: Int,
    diameter: Dp,
    isCurrentTurn: Boolean,
    showNumber: Boolean,
    modifier: Modifier = Modifier
) {
    val fill = TokenColors[playerIndex % TokenColors.size]
    val ring = if (isCurrentTurn) Color(0xFFFFB300) else Color.White
    val numberSize = when {
        diameter >= 16.dp -> 9.sp
        diameter >= 12.dp -> 7.sp
        else -> 5.sp
    }
    val desc = "Player ${playerIndex + 1} piece"
    Box(
        modifier = modifier
            .size(diameter)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, ring, CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, Color(0xD8000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (showNumber) {
                Text(
                    text = "${playerIndex + 1}",
                    color = Color.White,
                    fontSize = numberSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BoardGrid(
    positions: List<Int>,
    currentPlayerIndex: Int,
    phase: SnakeLadderPhase,
    specialMove: SpecialMoveEvent?,
    modifier: Modifier = Modifier
) {
    val ladderStarts = SnakeLadderRules.ladders.keys
    val snakeHeads = SnakeLadderRules.snakes.keys
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(SnakeLadderRules.GRID),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        ) {
            items(SnakeLadderRules.BOARD_SIZE) { index ->
                val row = index / SnakeLadderRules.GRID
                val col = index % SnakeLadderRules.GRID
                val cell = SnakeLadderRules.cellAtDisplayGrid(row, col)
                BoardCell(
                    cellNumber = cell,
                    isLadderFoot = ladderStarts.contains(cell),
                    isSnakeHead = snakeHeads.contains(cell),
                    ladderDestination = SnakeLadderRules.ladders[cell],
                    snakeDestination = SnakeLadderRules.snakes[cell],
                    ladderLandingFromFoot = SnakeLadderRules.ladderTopToFoot[cell],
                    snakeLandingFromHead = SnakeLadderRules.snakeTailToHead[cell],
                    playersHere = positions.mapIndexedNotNull { playerIdx, pos ->
                        if (pos == cell) playerIdx else null
                    },
                    currentPlayerIndex = currentPlayerIndex,
                    phase = phase,
                    highlight = when (cell) {
                        specialMove?.from, specialMove?.to -> specialMove?.type
                        else -> null
                    },
                    modifier = Modifier
                        .padding(2.dp)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
private fun BoardCell(
    cellNumber: Int,
    isLadderFoot: Boolean,
    isSnakeHead: Boolean,
    ladderDestination: Int?,
    snakeDestination: Int?,
    ladderLandingFromFoot: Int?,
    snakeLandingFromHead: Int?,
    playersHere: List<Int>,
    currentPlayerIndex: Int,
    phase: SnakeLadderPhase,
    highlight: SpecialMoveType? = null,
    modifier: Modifier = Modifier
) {
    val ladderGreen = Color(0xFF2E7D32)
    val snakeRed = Color(0xFFC62828)
    val finishGold = Color(0xFFF9A825)

    val staticBase = when {
        cellNumber == SnakeLadderRules.BOARD_SIZE -> Color(0xFFFFF9C4)
        isLadderFoot -> Color(0xFFC8E6C9)
        isSnakeHead -> Color(0xFFFFCDD2)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val base by animateColorAsState(
        targetValue = when (highlight) {
            SpecialMoveType.LADDER -> Color(0xFFFFD54F)
            SpecialMoveType.SNAKE -> Color(0xFFEF9A9A)
            null -> staticBase
        },
        animationSpec = tween(180),
        label = "cellHighlight"
    )
    val borderColor = when {
        cellNumber == SnakeLadderRules.BOARD_SIZE -> finishGold
        isLadderFoot -> ladderGreen
        isSnakeHead -> snakeRed
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val borderWidth = when {
        isLadderFoot || isSnakeHead || cellNumber == SnakeLadderRules.BOARD_SIZE -> 2.dp
        else -> 1.dp
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .background(base)
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = cellNumber.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            ladderLandingFromFoot?.let { foot ->
                Text(
                    text = "↑$foot",
                    fontSize = 6.sp,
                    color = ladderGreen,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 7.sp,
                    maxLines = 1
                )
            }
            snakeLandingFromHead?.let { head ->
                Text(
                    text = "↓$head",
                    fontSize = 6.sp,
                    color = snakeRed,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 7.sp,
                    maxLines = 1
                )
            }
        }
        when {
            isLadderFoot -> {
                val dest = ladderDestination
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_snakeladder_ladder),
                        contentDescription = if (dest != null) {
                            "Ladder to square $dest"
                        } else {
                            "Ladder — climb up"
                        },
                        tint = ladderGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    if (dest != null) {
                        Text(
                            text = "→$dest",
                            fontSize = 7.sp,
                            color = ladderGreen,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 8.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            isSnakeHead -> {
                val dest = snakeDestination
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_snakeladder_snake),
                        contentDescription = if (dest != null) {
                            "Snake to square $dest"
                        } else {
                            "Snake — slide down"
                        },
                        tint = snakeRed,
                        modifier = Modifier.size(12.dp)
                    )
                    if (dest != null) {
                        Text(
                            text = "→$dest",
                            fontSize = 7.sp,
                            color = snakeRed,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 8.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            cellNumber == SnakeLadderRules.BOARD_SIZE -> {
                Text(
                    text = "★",
                    fontSize = 11.sp,
                    color = finishGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        val stack = playersHere.size
        val pieceDiameter = when {
            stack <= 1 -> 14.dp
            stack == 2 -> 11.dp
            else -> 9.dp
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .heightIn(min = pieceDiameter + 4.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterHorizontally)
        ) {
            playersHere.forEach { playerIdx ->
                PlayerPiece(
                    playerIndex = playerIdx,
                    diameter = pieceDiameter,
                    isCurrentTurn = phase == SnakeLadderPhase.Playing && playerIdx == currentPlayerIndex,
                    showNumber = true,
                    modifier = Modifier
                )
            }
        }
    }
}

private val DicePipLayout: Map<Int, Set<Pair<Int, Int>>> = mapOf(
    1 to setOf(1 to 1),
    2 to setOf(0 to 0, 2 to 2),
    3 to setOf(0 to 0, 1 to 1, 2 to 2),
    4 to setOf(0 to 0, 0 to 2, 2 to 0, 2 to 2),
    5 to setOf(0 to 0, 0 to 2, 1 to 1, 2 to 0, 2 to 2),
    6 to setOf(0 to 0, 0 to 2, 1 to 0, 1 to 2, 2 to 0, 2 to 2)
)

@Composable
private fun DiceView(
    value: Int?,
    isRolling: Boolean,
    modifier: Modifier = Modifier
) {
    var face by remember { mutableIntStateOf(value ?: 1) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            while (isActive) {
                face = (1..6).random()
                delay(80)
            }
        }
    }
    LaunchedEffect(value, isRolling) {
        if (!isRolling && value != null) face = value
    }

    val infiniteTransition = rememberInfiniteTransition(label = "diceSpin")
    val rotation by if (isRolling) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing)),
            label = "diceRotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val scale by animateFloatAsState(
        targetValue = if (isRolling) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "diceScale"
    )

    Box(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(7.dp)
            .semantics { contentDescription = value?.let { "Dice showing $it" } ?: "Dice rolling" }
    ) {
        val pips = DicePipLayout[face].orEmpty()
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            for (row in 0..2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0..2) {
                        val active = pips.contains(row to col)
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (active) Color(0xFF212121) else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

private class GameSounds(private val toneGen: ToneGenerator) {
    fun hop() = toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
    fun diceLand() = toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 90)
    fun ladder() = toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
    fun snake() = toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 220)
    fun win() = toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 450)
}

@Composable
private fun rememberGameSounds(): GameSounds {
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }
    DisposableEffect(Unit) {
        onDispose { toneGen.release() }
    }
    return remember { GameSounds(toneGen) }
}
