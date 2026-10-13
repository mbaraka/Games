package com.baraka.game.clashdecks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.clashdecks.ClashScreen
import com.baraka.game.clashdecks.ClashUiModel
import com.baraka.game.ui.theme.GameColors

/**
 * The co-op counterpart to `ClashDecksScreen` — same `when` over [ClashUiModel.screen], minus
 * the single-player-only MENU (the lobby that leads here is its own screen, shown before this
 * composable exists) and difficulty picker. Everything else — [DuelScreen], [CodexScreen],
 * [ClashSettingsScreen], [ResultScreen] — is the exact same composable single-player uses;
 * only the opponent's label changes, so cards/log/effects/animations all look identical to a
 * local duel.
 */
@Composable
fun ClashCoopScreen(
    model: ClashUiModel,
    opponentLabel: String,
    onPlayCard: (String) -> Unit,
    onEndTurn: () -> Unit,
    onOpenCodex: () -> Unit,
    onCloseCodex: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onRematch: () -> Unit,
    onLeave: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        when (model.screen) {
            ClashScreen.CODEX -> CodexScreen(onClose = onCloseCodex)
            ClashScreen.SETTINGS -> ClashSettingsScreen(
                soundEnabled = model.soundEnabled,
                onSoundEnabledChanged = onSetSoundEnabled,
                onClose = onCloseSettings
            )
            ClashScreen.RESULT -> model.duel?.let {
                ResultScreen(it, onRematch, onLeave, onOpenCodex)
            }
            else -> model.duel?.let {
                DuelScreen(it, model.cpuThinking, model.effects, onPlayCard, onEndTurn, onOpenCodex, onOpenSettings, opponentLabel)
            }
        }
        if (model.partnerVersionMismatch) {
            Text(
                "Your partner is on a different version of Clash Decks — update both phones for a reliable duel.",
                color = GameColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(GameColors.Danger)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
