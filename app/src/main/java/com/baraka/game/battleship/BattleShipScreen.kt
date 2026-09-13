package com.baraka.game.battleship

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
private val OceanCell = Color(0xFF1565C0)
private val ShipColor = Color(0xFF546E7A)
private val HitColor = Color(0xFFC62828)
private val MissColor = Color(0xFFB0BEC5)
private val FogColor = Color(0xFF1A237E)

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
            Text(
                text = "Battleship",
                color = GameColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
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
        Button(
            onClick = { onChooseMode(BattleShipGameMode.LocalTwoPlayer) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OceanDark)
        ) {
            Text("Local 2 players")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Pass the device between players. Each places ships without seeing the other’s grid.",
            color = GameColors.TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onChooseMode(BattleShipGameMode.VsComputer) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
        ) {
            Text("Vs computer")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "You place your fleet; the computer places randomly. You shoot first.",
            color = GameColors.TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SetupContent(
    uiState: BattleShipUiState,
    onToggleOrientation: () -> Unit,
    onPlaceCell: (Int) -> Unit
) {
    val board = when (uiState.phase) {
        BattlePhase.SetupP1 -> uiState.player1Ships
        BattlePhase.SetupP2 -> uiState.player2Ships
        else -> uiState.player1Ships
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
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
    ) {
        Text("Rotate ship")
    }
    Spacer(modifier = Modifier.height(12.dp))
    BoardGrid(
        modifier = Modifier.fillMaxWidth(),
        onCellClick = onPlaceCell
    ) { index ->
        SetupCell(hasShip = board[index])
    }
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
        colors = ButtonDefaults.buttonColors(containerColor = OceanDark)
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
    Spacer(modifier = Modifier.height(16.dp))

    val viewAsPlayer = if (uiState.isVsComputer) 1 else turn

    Text(text = "Your fleet (enemy shots)", color = GameColors.TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(6.dp))
    val myShips = if (viewAsPlayer == 1) uiState.player1Ships else uiState.player2Ships
    val shotsAtMe = if (viewAsPlayer == 1) uiState.player2Shots else uiState.player1Shots
    BoardGrid(modifier = Modifier.fillMaxWidth(), onCellClick = null) { index ->
        OwnFleetCell(hasShip = myShips[index], shot = shotsAtMe[index])
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = if (canFire) "Enemy waters (tap to fire)" else "Enemy waters",
        color = GameColors.TextSecondary,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    val myShots = if (viewAsPlayer == 1) uiState.player1Shots else uiState.player2Shots
    BoardGrid(
        modifier = Modifier.fillMaxWidth(),
        onCellClick = if (canFire) {
            { idx -> if (myShots[idx] == null) onFire(idx) }
        } else {
            null
        }
    ) { index ->
        EnemyCell(shot = myShots[index])
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
        colors = ButtonDefaults.buttonColors(containerColor = OceanDark)
    ) {
        Text("Play again")
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
        Text("Main menu")
    }
}

@Composable
private fun BoardGrid(
    modifier: Modifier = Modifier,
    onCellClick: ((Int) -> Unit)? = null,
    cell: @Composable (index: Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        modifier = modifier
            .height(360.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp)),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(100) { idx ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .then(
                        if (onCellClick != null) Modifier.clickable { onCellClick(idx) }
                        else Modifier
                    )
            ) {
                cell(idx)
            }
        }
    }
}

@Composable
private fun SetupCell(hasShip: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (hasShip) ShipColor else OceanCell)
    )
}

@Composable
private fun OwnFleetCell(hasShip: Boolean, shot: ShotKind?) {
    val bg = when {
        shot == ShotKind.HIT -> HitColor
        shot == ShotKind.MISS -> MissColor
        hasShip -> ShipColor
        else -> OceanCell
    }
    Box(modifier = Modifier.fillMaxSize().background(bg))
}

@Composable
private fun EnemyCell(shot: ShotKind?) {
    val bg = when {
        shot == ShotKind.HIT -> HitColor
        shot == ShotKind.MISS -> MissColor
        else -> FogColor
    }
    Box(modifier = Modifier.fillMaxSize().background(bg))
}
