package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.model.UpgradeDisplay
import com.baraka.game.ui.icons.VectorIcon
import com.baraka.game.ui.icons.drawVectorIcon
import com.baraka.game.ui.theme.GameColors

/** One already-owned weapon or passive, shown in the loadout strip above the upgrade cards. */
data class LoadoutEntry(val icon: VectorIcon, val colorArgb: Long, val levelLabel: String)

/**
 * Pauses the run on level-up: pick one of the offered [choices]. Purely presentational —
 * see systems/UpgradeChoiceSystem.kt for how real choices are rolled/applied. Generic over
 * [UpgradeDisplay] rather than the real `UpgradeOption` sealed type so a co-op guest (which
 * only ever receives title/description/color over the wire, see
 * knightsurvivor/coop/KnightCoopMsg.kt's UpgradeOptionDto) can reuse this exact composable.
 * [currentWeapons]/[currentPassives] are what the player owns *before* this choice, split
 * the same way the weapon-deck and settings screens already split them, so the player can
 * see what they already have while picking - both empty for a co-op guest since the data
 * isn't sent over the wire (see ui/KnightSurvivorScreen.kt's call site).
 */
@Composable
fun <T : UpgradeDisplay> LevelUpOverlay(
    choices: List<T>,
    currentWeapons: List<LoadoutEntry> = emptyList(),
    currentPassives: List<LoadoutEntry> = emptyList(),
    onChoiceSelected: (T) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "LEVEL UP!", color = GameColors.Warning, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(text = "Choose an upgrade", color = GameColors.TextPrimary, fontSize = 16.sp)

            LoadoutRow(label = "Weapons", entries = currentWeapons)
            LoadoutRow(label = "Passives", entries = currentPassives)

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                choices.forEach { choice ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onChoiceSelected(choice) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val color = Color(choice.colorArgb)
                            choice.icon?.let { icon ->
                                Canvas(modifier = Modifier.size(32.dp)) {
                                    drawVectorIcon(icon, center = Offset(size.width / 2f, size.height / 2f), radius = size.minDimension / 2f, color = color)
                                }
                            }
                            Text(
                                text = choice.title,
                                color = color,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = choice.description,
                                color = GameColors.TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadoutRow(label: String, entries: List<LoadoutEntry>) {
    if (entries.isEmpty()) return
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = GameColors.TextMuted, fontSize = 11.sp)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            entries.forEach { entry -> LoadoutChip(entry) }
        }
    }
}

@Composable
private fun LoadoutChip(entry: LoadoutEntry) {
    val color = Color(entry.colorArgb)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawVectorIcon(entry.icon, center = Offset(size.width / 2f, size.height / 2f), radius = size.minDimension / 2f, color = color)
            }
        }
        Text(text = entry.levelLabel, color = GameColors.TextMuted, fontSize = 9.sp)
    }
}
