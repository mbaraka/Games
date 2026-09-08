package com.baraka.game.knightsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.model.Achievement
import com.baraka.game.knightsurvivor.model.Difficulty
import com.baraka.game.knightsurvivor.model.KnightDesign
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.render.drawKnightIcon
import com.baraka.game.ui.theme.GameColors

private enum class SettingsTab(val label: String) {
    GENERAL("General"), KNIGHT("Knight"), GUIDE("Guide"), ACHIEVEMENTS("Achievements")
}

/**
 * A dedicated screen rather than an overlay Box — a settings panel that shares no layout
 * with the running game and owns its own back-press handling ([BackHandler]) so pressing
 * back closes Settings, not the whole Activity (an overlay-only "close" button left that
 * gap). Panels are picked from a left tab rail, matching the room to add more tabs later
 * without restructuring anything.
 */
@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
    difficulty: Difficulty,
    onDifficultyChanged: (Difficulty) -> Unit,
    player: PlayerState,
    unlockedAchievements: Set<Achievement>,
    knightDesign: KnightDesign,
    onKnightDesignChanged: (KnightDesign) -> Unit,
    onClose: () -> Unit,
    onWeaponCardClick: (WeaponKind) -> Unit = {}
) {
    BackHandler(onBack = onClose)

    var tab by remember { mutableStateOf(SettingsTab.GENERAL) }

    Scaffold(containerColor = Color(0xFF12141A)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Settings",
                    color = GameColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Close ✕",
                    color = GameColors.TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(96.dp)
                        .padding(start = 16.dp)
                ) {
                    SettingsTab.entries.forEach { candidate ->
                        SettingsRailTab(text = candidate.label, selected = candidate == tab, onClick = { tab = candidate })
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (tab) {
                        SettingsTab.GENERAL -> GeneralSettingsPanel(
                            soundEnabled = soundEnabled,
                            onSoundEnabledChanged = onSoundEnabledChanged,
                            difficulty = difficulty,
                            onDifficultyChanged = onDifficultyChanged
                        )
                        SettingsTab.KNIGHT -> KnightDesignPanel(
                            selected = knightDesign,
                            onSelected = onKnightDesignChanged
                        )
                        SettingsTab.GUIDE -> CompendiumContent(
                            player = player,
                            difficulty = difficulty,
                            knightDesign = knightDesign,
                            onWeaponCardClick = onWeaponCardClick
                        )
                        SettingsTab.ACHIEVEMENTS -> AchievementsContent(unlockedAchievements = unlockedAchievements)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsPanel(
    soundEnabled: Boolean,
    onSoundEnabledChanged: (Boolean) -> Unit,
    difficulty: Difficulty,
    onDifficultyChanged: (Difficulty) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Sound", color = GameColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingChip(text = "On", selected = soundEnabled, onClick = { onSoundEnabledChanged(true) })
            SettingChip(text = "Off", selected = !soundEnabled, onClick = { onSoundEnabledChanged(false) })
        }

        Spacer(Modifier.height(24.dp))

        Text(text = "Difficulty", color = GameColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { candidate ->
                SettingChip(
                    text = candidate.displayName,
                    selected = candidate == difficulty,
                    onClick = { onDifficultyChanged(candidate) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Takes effect on your next run — this one keeps its current difficulty.",
            color = GameColors.TextDisabled,
            fontSize = 11.sp
        )
    }
}

/** Lets the player pick between the 4 purely-cosmetic [KnightDesign]s — applies immediately, live, since there's no balance reason to gate a look change to the next run. */
@Composable
private fun KnightDesignPanel(selected: KnightDesign, onSelected: (KnightDesign) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(KnightDesign.entries) { design ->
            KnightDesignCard(design = design, isSelected = design == selected, onClick = { onSelected(design) })
        }
    }
}

@Composable
private fun KnightDesignCard(design: KnightDesign, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFF2A3040) else Color(0xFF1C2029), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            drawKnightIcon(center = center, radius = size.minDimension * 0.4f, design = design)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(design.displayName, color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(design.description, color = GameColors.TextSecondary, fontSize = 12.sp)
        }
        Text(
            text = if (isSelected) "✓" else "",
            color = GameColors.Accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun SettingsRailTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.Black else GameColors.TextPrimary,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) GameColors.Accent else Color(0xFF2D2D2D), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun SettingChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.Black else GameColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(if (selected) GameColors.Accent else Color(0xFF2D2D2D), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
