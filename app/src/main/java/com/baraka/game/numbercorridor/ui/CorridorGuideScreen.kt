package com.baraka.game.numbercorridor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.baraka.game.numbercorridor.model.MathOp
import com.baraka.game.numbercorridor.model.WeaponTier
import com.baraka.game.ui.theme.GameColors

private enum class GuideTab(val label: String) {
    LANES("Lanes")
    // More tabs (e.g. "Avatars", "Scoring") can be added here as the game grows —
    // see SettingsTab in knightsurvivor's SettingsOverlay.kt for the same shape.
}

// Lane-identity colors: these mirror the corridor's actual lane wall colors (game art), not
// UI chrome, so they stay fixed rather than becoming theme-aware.
private val SQUAD_LANE_COLOR = Color(0xFF66BB6A)
private val ENEMY_LANE_COLOR = Color(0xFF7C4DA8)
private val WEAPON_LANE_COLOR = Color(0xFFFFB300)

/**
 * A dedicated screen (not an in-canvas overlay) explaining what each of the corridor's 3
 * fixed lanes does — same "own back-press handling + tab rail" shape as knightsurvivor's
 * SettingsScreen, so a later tab (avatars, scoring, ...) slots in without restructuring.
 */
@Composable
fun CorridorGuideScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)

    var tab by remember { mutableStateOf(GuideTab.LANES) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Guide",
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
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GuideTab.entries.forEach { candidate ->
                    GuideTabChip(text = candidate.label, selected = candidate == tab, onClick = { tab = candidate })
                }
            }

            Spacer(Modifier.height(12.dp))

            when (tab) {
                GuideTab.LANES -> LanesGuideContent()
            }
        }
    }
}

@Composable
private fun GuideTabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.Black else GameColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(if (selected) GameColors.Accent else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun LanesGuideContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "The corridor has 3 lanes and each one always does the same job, left to right. Drag your squad into the lane whose wall you want to hit.",
                color = GameColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        item {
            LaneCard(
                color = SQUAD_LANE_COLOR,
                title = "Left — Squad",
                description = "Green walls change your squad size (1-4 soldiers) using +, -, x, or ÷. More soldiers means more simultaneous shots from whichever lane you're standing in."
            )
        }
        item {
            LaneCard(
                color = ENEMY_LANE_COLOR,
                title = "Middle — Enemies",
                description = "Purple walls are enemies blocking the lane. Bigger groups take more hits to clear. Shoot them down before they reach you or it costs a heart."
            )
        }
        item {
            LaneCard(
                color = WEAPON_LANE_COLOR,
                title = "Right — Weapon Crates",
                description = "Gold crates upgrade your weapon to the next, stronger tier once destroyed. Occasional red crates knock you down a tier instead — worth watching for."
            )
        }

        item {
            Text(
                text = "Squad math symbols",
                color = GameColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(MathOp.entries) { op -> MathOpCard(op) }

        item {
            Text(
                text = "Weapon tiers, weakest to strongest",
                color = GameColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(WeaponTier.entries) { tier -> WeaponTierCard(tier) }
    }
}

@Composable
private fun LaneCard(color: Color, title: String, description: String) {
    GuideCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(28.dp)) {
                drawRoundRect(color = color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(description, color = GameColors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun MathOpCard(op: MathOp) {
    GuideCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(op.symbol, color = GameColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
            Text(opDescription(op), color = GameColors.TextSecondary, fontSize = 13.sp)
        }
    }
}

private fun opDescription(op: MathOp): String = when (op) {
    MathOp.ADD -> "Adds soldiers to your squad."
    MathOp.SUBTRACT -> "Removes soldiers — can hurt if your squad is small."
    MathOp.MULTIPLY -> "Multiplies your squad — big swings, good or bad."
    MathOp.DIVIDE -> "Divides your squad — the one to dodge unless it's already big."
}

@Composable
private fun WeaponTierCard(tier: WeaponTier) {
    GuideCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(24.dp)) {
                drawCircle(color = Color(tier.colorArgb))
            }
            Spacer(Modifier.width(12.dp))
            Text(tier.displayName, color = GameColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text("Damage ${tier.damage.toInt()} · Fire rate x${"%.2f".format(tier.fireRateMultiplier)}", color = GameColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun GuideCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}
