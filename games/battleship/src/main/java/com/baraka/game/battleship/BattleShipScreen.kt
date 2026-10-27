package com.baraka.game.battleship

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.components.GameScreenContainer
import com.baraka.game.ui.theme.GameColors

private val OceanDark = Color(0xFF0D47A1)

@Composable
fun BattleShipScreen(
    uiState: BattleShipUiState,
    onBack: () -> Unit,
    onChooseMode: (BattleShipGameMode) -> Unit,
    onBackToMenu: () -> Unit,
    onToggleOrientation: () -> Unit,
    onPlaceCell: (Int) -> Unit,
    onConfirmHandoffP1: () -> Unit,
    onConfirmHandoffP2: () -> Unit,
    onFire: (Int) -> Unit,
    onPlayAgain: () -> Unit
) {
    GameScreenContainer(
        backgroundColor = MaterialTheme.colorScheme.background,
        scrollable = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Fleet Duel",
                    color = GameColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "version ${com.baraka.game.battleship.BuildConfig.BATTLESHIP_VERSION}",
                    color = GameColors.TextMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.size(72.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (uiState.phase) {
            BattlePhase.Menu -> MenuContent(onChooseMode = onChooseMode)
            BattlePhase.SetupP1, BattlePhase.SetupP2 -> SetupContent(
                uiState = uiState,
                onToggleOrientation = onToggleOrientation,
                onPlaceCell = onPlaceCell
            )
            BattlePhase.HandoffAfterP1 -> HandoffContent(
                title = "Player 1 done",
                body = "Pass the device to Player 2 so they can place their fleet (without seeing yours).",
                buttonLabel = "Continue",
                onContinue = onConfirmHandoffP1
            )
            BattlePhase.HandoffAfterP2 -> HandoffContent(
                title = "Ready to fight",
                body = if (uiState.isVsComputer) {
                    "You’ll shoot first. Good luck!"
                } else {
                    "Pass the device to Player 1. They take the first shot."
                },
                buttonLabel = "Begin battle",
                onContinue = onConfirmHandoffP2
            )
            BattlePhase.Battle -> BattleContent(
                uiState = uiState,
                onFire = onFire
            )
            BattlePhase.GameOver -> GameOverContent(
                uiState = uiState,
                onPlayAgain = onPlayAgain,
                onBackToMenu = onBackToMenu
            )
        }
    }
}

@Composable
private fun MenuContent(onChooseMode: (BattleShipGameMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose how you want to play.",
            color = GameColors.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        ModeCard(
            icon = Icons.Default.Group,
            accent = OceanDark,
            title = "Local 2 players",
            body = "Pass the device between players. Each places ships without seeing the other's grid.",
            onClick = { onChooseMode(BattleShipGameMode.LocalTwoPlayer) }
        )
        Spacer(modifier = Modifier.height(14.dp))
        ModeCard(
            icon = Icons.Default.SmartToy,
            accent = Color(0xFF00695C),
            title = "Vs computer",
            body = "You place your fleet; the computer places randomly. You shoot first.",
            onClick = { onChooseMode(BattleShipGameMode.VsComputer) }
        )
    }
}

/** One big, icon-led mode choice - bigger tap target and fewer words than a plain text button. */
@Composable
private fun ModeCard(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, title: String, body: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SetupContent(
    uiState: BattleShipUiState,
    onToggleOrientation: () -> Unit,
    onPlaceCell: (Int) -> Unit
) {
    val ids = when (uiState.phase) {
        BattlePhase.SetupP2 -> uiState.player2Ids
        else -> uiState.player1Ids
    }
    val len = uiState.shipLengthToPlace()
    val playerLabel = when {
        uiState.isVsComputer && uiState.phase == BattlePhase.SetupP1 -> "You"
        else -> "Player ${uiState.currentSetupPlayer}"
    }

    Text(
        text = "$playerLabel — place ships (${BattleShipFleetLengths.joinToString(", ")} cells)",
        color = GameColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = if (len != null) {
            "Current ship: $len cells — ${if (uiState.setupHorizontal) "horizontal" else "vertical"}"
        } else "",
        color = GameColors.TextSecondary,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
    uiState.lastMessage?.let {
        Text(text = it, color = GameColors.Warning, fontSize = 13.sp)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onToggleOrientation,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F), contentColor = Color.White)
    ) {
        Text("Rotate ship")
    }
    Spacer(modifier = Modifier.height(12.dp))
    Board(
        ids = ids,
        shots = null,
        visible = AllVisible,
        sunk = NoneSunk,
        modifier = Modifier.fillMaxWidth(),
        onCellClick = onPlaceCell
    )
}

@Composable
private fun HandoffContent(
    title: String,
    body: String,
    buttonLabel: String,
    onContinue: () -> Unit
) {
    Text(text = title, color = GameColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = body, color = GameColors.TextSecondary, fontSize = 15.sp)
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = OceanDark, contentColor = Color.White)
    ) {
        Text(buttonLabel)
    }
}

@Composable
private fun BattleContent(
    uiState: BattleShipUiState,
    onFire: (Int) -> Unit
) {
    val turn = uiState.currentTurn
    val canFire = uiState.isHumanTurnInBattle() && !uiState.cpuThinking
    val turnLabel = when {
        uiState.isVsComputer && uiState.cpuThinking -> "Computer is shooting…"
        uiState.isVsComputer && turn == 1 -> "Your turn — fire at the computer’s grid."
        uiState.isVsComputer -> "Computer’s turn — watch your fleet above."
        else -> "Player $turn’s turn — fire at the enemy ocean below."
    }
    Text(
        text = turnLabel,
        color = GameColors.TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    uiState.lastMessage?.let {
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = it, color = GameColors.Warning, fontSize = 14.sp)
    }
    Spacer(modifier = Modifier.height(12.dp))

    val viewAsPlayer = if (uiState.isVsComputer) 1 else turn
    // One board at a time instead of both stacked, so the screen fits without scrolling.
    // Defaults to whichever board the player can act on right now.
    var showEnemyBoard by remember { mutableStateOf(canFire) }
    LaunchedEffect(canFire) { showEnemyBoard = canFire }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BoardTab("Your fleet", selected = !showEnemyBoard, modifier = Modifier.weight(1f)) { showEnemyBoard = false }
        BoardTab("Enemy waters", selected = showEnemyBoard, modifier = Modifier.weight(1f)) { showEnemyBoard = true }
    }
    Spacer(modifier = Modifier.height(10.dp))

    if (showEnemyBoard) {
        val myShots = if (viewAsPlayer == 1) uiState.player1Shots else uiState.player2Shots
        val enemyIds = if (viewAsPlayer == 1) uiState.player2Ids else uiState.player1Ids
        val enemySunk = sunkFlags(enemyIds, myShots)
        Board(
            ids = enemyIds,
            shots = myShots,
            visible = enemySunk, // enemy ships stay hidden until they are sunk
            sunk = enemySunk,
            modifier = Modifier.fillMaxWidth(),
            fog = true,
            onCellClick = if (canFire) {
                { idx -> if (myShots[idx] == null) onFire(idx) }
            } else {
                null
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        FleetStatus(sunk = enemySunk)
    } else {
        val myIds = if (viewAsPlayer == 1) uiState.player1Ids else uiState.player2Ids
        val shotsAtMe = if (viewAsPlayer == 1) uiState.player2Shots else uiState.player1Shots
        val mySunk = sunkFlags(myIds, shotsAtMe)
        Board(
            ids = myIds,
            shots = shotsAtMe,
            visible = AllVisible,
            sunk = mySunk,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        FleetStatus(sunk = mySunk)
    }

    Spacer(modifier = Modifier.height(12.dp))
    Legend()
}

@Composable
private fun BoardTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OceanDark else Color(0xFF263238))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun GameOverContent(
    uiState: BattleShipUiState,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val w = uiState.winner
    val headline = when {
        w == null -> "Game over"
        uiState.isVsComputer && w == 1 -> "You win!"
        uiState.isVsComputer && w == 2 -> "Computer wins!"
        else -> "Player $w wins!"
    }
    Text(
        text = headline,
        color = GameColors.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    uiState.lastMessage?.let {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = it,
            color = GameColors.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onPlayAgain,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = OceanDark, contentColor = Color.White)
    ) {
        Text("Play again")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
        Text("Main menu")
    }
}

private fun sunkFlags(ids: List<Int>, shots: List<ShotKind?>): BooleanArray =
    BooleanArray(BattleShipFleetLengths.size) { id -> BattleShipViewModel.shipSunk(ids, shots, id) }

private val AllVisible = BooleanArray(BattleShipFleetLengths.size) { true }
private val NoneSunk = BooleanArray(BattleShipFleetLengths.size)

/** A boat icon plus a pip per cell of length, instead of a name/number text label - readable at
 *  a glance without needing to read "Battleship 4" (see AGENTS.md UI-consistency rules). */
@Composable
private fun FleetStatus(sunk: BooleanArray) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (id in BattleShipFleetLengths.indices) {
            val gone = sunk[id]
            val color = if (gone) Color(0xFFFFCDD2) else Color(0xFFECEFF1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (gone) Color(0xFF7F1D1D) else Color(0xFF263238))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBoat,
                        contentDescription = "${BattleShipNames[id]}, ${BattleShipFleetLengths[id]} cells" + if (gone) ", sunk" else "",
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(BattleShipFleetLengths[id]) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(color))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(shot = ShotKind.MISS, sunk = false, label = "Miss")
        LegendItem(shot = ShotKind.HIT, sunk = false, label = "Hit")
        LegendItem(shot = ShotKind.HIT, sunk = true, label = "Sunk")
    }
}

@Composable
private fun LegendItem(shot: ShotKind, sunk: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))) {
            drawLegendIcon(shot, sunk)
        }
        Spacer(modifier = Modifier.size(5.dp))
        Text(text = label, color = GameColors.TextSecondary, fontSize = 11.sp)
    }
}
