package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.ui.theme.GameColors

/** Top-of-screen HUD: survival timer, level, kill count, best time, HP bar, XP bar. Pure display — no game logic. */
@Composable
fun Hud(
    player: PlayerState,
    survivalTimeMs: Long,
    killCount: Int,
    bestSurvivalTimeMs: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatSurvivalDuration(survivalTimeMs), color = GameColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "Lv.${player.level}", color = GameColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Kills: $killCount", color = GameColors.Warning, fontSize = 16.sp)
        }

        Text(text = "Best: ${formatSurvivalDuration(bestSurvivalTimeMs)}", color = GameColors.TextMuted, fontSize = 12.sp)

        val hpFraction = (player.hp.toFloat() / player.effectiveMaxHp.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { hpFraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = GameColors.Success,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        val xpFraction = (player.xp.toFloat() / player.xpToNextLevel.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { xpFraction },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = GameColors.Accent,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}
