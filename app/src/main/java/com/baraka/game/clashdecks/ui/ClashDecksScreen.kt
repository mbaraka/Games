package com.baraka.game.clashdecks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.baraka.game.clashdecks.ClashScreen
import com.baraka.game.clashdecks.ClashUiModel
import com.baraka.game.clashdecks.ai.Difficulty
import com.baraka.game.clashdecks.model.DuelLogEntry
import com.baraka.game.clashdecks.model.DuelPhase
import com.baraka.game.clashdecks.model.DuelState
import com.baraka.game.clashdecks.model.DuelWinner
import com.baraka.game.clashdecks.model.LogKind
import com.baraka.game.clashdecks.model.VfxSide
import com.baraka.game.ui.debug.DebugSection
import com.baraka.game.ui.theme.GameColors

@Composable
fun ClashDecksScreen(
    model: ClashUiModel,
    onBack: () -> Unit,
    onSetDifficulty: (Difficulty) -> Unit,
    onStartDuel: () -> Unit,
    onPlayCard: (String) -> Unit,
    onEndTurn: () -> Unit,
    onOpenCodex: () -> Unit,
    onCloseCodex: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onRematch: () -> Unit,
    onBackToMenu: () -> Unit,
    onPlayVsFriend: () -> Unit = {},
    debugSections: List<DebugSection> = emptyList()
) {
    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        when (model.screen) {
            ClashScreen.MENU -> MenuScreen(model, onBack, onSetDifficulty, onStartDuel, onOpenCodex, onOpenSettings, onPlayVsFriend)
            ClashScreen.CODEX -> CodexScreen(onClose = onCloseCodex)
            ClashScreen.SETTINGS -> ClashSettingsScreen(
                soundEnabled = model.soundEnabled,
                onSoundEnabledChanged = onSetSoundEnabled,
                onClose = onCloseSettings,
                debugSections = debugSections
            )
            ClashScreen.DUEL -> model.duel?.let {
                DuelScreen(it, model.cpuThinking, model.effects, onPlayCard, onEndTurn, onOpenCodex, onOpenSettings, "Opponent")
            }
            ClashScreen.RESULT -> model.duel?.let {
                ResultScreen(it, onRematch, onBackToMenu, onOpenCodex, model.bestWinRounds)
            }
        }
    }
}

@Composable
private fun MenuScreen(
    model: ClashUiModel,
    onBack: () -> Unit,
    onSetDifficulty: (Difficulty) -> Unit,
    onStartDuel: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayVsFriend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Clash Decks", color = GameColors.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(
            "A turn-based card duel vs the computer. Attack, Defense and Support — plus stun, freeze, poison and more.",
            color = GameColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text("Difficulty", color = GameColors.TextSecondary, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Difficulty.entries.forEach { d ->
                TabChip(d.displayName, d == model.difficulty) { onSetDifficulty(d) }
            }
        }
        Spacer(Modifier.height(8.dp))
        PillButton("Start Duel", onStartDuel)
        PillButton("Play vs Friend (Bluetooth)", onPlayVsFriend, primary = false)
        PillButton("Codex", onOpenCodex, primary = false)
        PillButton("Settings", onOpenSettings, primary = false)
        PillButton("Back", onBack, primary = false)
    }
}

@Composable
internal fun DuelScreen(
    duel: DuelState,
    cpuThinking: Boolean,
    effects: List<com.baraka.game.clashdecks.VfxEmission>,
    onPlayCard: (String) -> Unit,
    onEndTurn: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenSettings: () -> Unit,
    opponentLabel: String = "Opponent"
) {
    val playerTurn = duel.phase == DuelPhase.PLAYER_TURN && duel.winner == null && !cpuThinking
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Round ${duel.turnNumber}", color = GameColors.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("⚙", color = GameColors.Accent, fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = 6.dp))
            Spacer(Modifier.width(8.dp))
            Text("Codex", color = GameColors.Accent, fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenCodex() }
                    .padding(horizontal = 12.dp, vertical = 6.dp))
        }

        DuelistPanel(duel.cpu, opponentLabel, VfxSide.CPU, effects)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val recent = duel.log.takeLast(6)
                recent.forEach { entry -> LogLine(entry) }
                if (cpuThinking) {
                    Text("Opponent is thinking…", color = GameColors.TextMuted, fontSize = 12.sp)
                }
            }
        }

        DuelistPanel(duel.player, "You", VfxSide.PLAYER, effects)
        EnergyPips(duel.player.energy, duel.player.maxEnergyThisTurn)

        if (duel.phase == DuelPhase.PLAYER_TURN && duel.activeStunned && duel.winner == null) {
            Text("You are stunned — end your turn.", color = GameColors.Danger, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            duel.player.hand.forEach { card ->
                val canPlay = playerTurn &&
                    !duel.activeStunned &&
                    duel.player.energy >= card.cost &&
                    !(duel.activeRooted && card.type == com.baraka.game.clashdecks.model.CardType.DEFENSE)
                CardView(card, enabled = canPlay, onClick = { onPlayCard(card.id) })
            }
        }

        PillButton(
            text = if (duel.activeStunned) "End Turn (stunned)" else "End Turn",
            onClick = onEndTurn,
            enabled = playerTurn
        )
    }

        DuelVfxLayer(effects)
    }
}

@Composable
private fun LogLine(entry: DuelLogEntry) {
    val color = when (entry.kind) {
        LogKind.DAMAGE -> GameColors.Danger
        LogKind.HEAL -> GameColors.Success
        LogKind.STATUS -> GameColors.Accent
        LogKind.SYSTEM -> GameColors.TextPrimary
        LogKind.NEUTRAL -> GameColors.TextSecondary
    }
    Text(entry.text, color = color, fontSize = 12.sp)
}

@Composable
internal fun ResultScreen(
    duel: DuelState,
    onRematch: () -> Unit,
    onBackToMenu: () -> Unit,
    onOpenCodex: () -> Unit,
    /** Fewest rounds a win has ever taken (0 = none recorded — always 0 in co-op, which doesn't track this). */
    bestWinRounds: Int = 0
) {
    val won = duel.winner == DuelWinner.PLAYER
    val isNewBest = won && duel.turnNumber == bestWinRounds
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            if (won) "Victory" else "Defeat",
            color = if (won) GameColors.Success else GameColors.Danger,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (won) "The opponent's deck is spent." else "Your knight falls. Try a different line.",
            color = GameColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "Finished in round ${duel.turnNumber}",
            color = GameColors.TextMuted,
            fontSize = 13.sp
        )
        if (bestWinRounds > 0) {
            Text(
                "Best win: $bestWinRounds round${if (bestWinRounds == 1) "" else "s"}",
                color = GameColors.TextMuted,
                fontSize = 13.sp
            )
        }
        if (isNewBest) {
            Text("New fastest win!", color = GameColors.Warning, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        PillButton("Rematch", onRematch)
        PillButton("Menu", onBackToMenu, primary = false)
        PillButton("Codex", onOpenCodex, primary = false)
    }
}
