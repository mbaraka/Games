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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        Button(
            onClick = { onChooseMode(BattleShipGameMode.LocalTwoPlayer) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OceanDark, contentColor = Color.White)
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C), contentColor = Color.White)
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
    Spacer(modifier = Modifier.height(16.dp))

    val viewAsPlayer = if (uiState.isVsComputer) 1 else turn

    Text(text = "Your fleet (enemy shots)", color = GameColors.TextSecondary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(6.dp))
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

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = if (canFire) "Enemy waters (tap to fire)" else "Enemy waters",
        color = GameColors.TextSecondary,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
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
    Spacer(modifier = Modifier.height(12.dp))
    Legend()
    Spacer(modifier = Modifier.height(48.dp))
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

@Composable
private fun FleetStatus(sunk: BooleanArray) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (id in BattleShipFleetLengths.indices) {
            val gone = sunk[id]
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (gone) Color(0xFF7F1D1D) else Color(0xFF263238))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${BattleShipNames[id]} ${BattleShipFleetLengths[id]}",
                    color = if (gone) Color(0xFFFFCDD2) else Color(0xFFECEFF1),
                    fontSize = 9.sp,
                    maxLines = 1,
                    textDecoration = if (gone) TextDecoration.LineThrough else null
                )
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
