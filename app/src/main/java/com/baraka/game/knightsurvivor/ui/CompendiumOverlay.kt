package com.baraka.game.knightsurvivor.ui

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.model.Achievement
import com.baraka.game.knightsurvivor.model.Difficulty
import com.baraka.game.knightsurvivor.model.EnemyKind
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.WeaponInstance
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.render.drawKnightIcon
import com.baraka.game.knightsurvivor.render.drawVectorIcon
import com.baraka.game.ui.theme.GameColors

private enum class CompendiumTab(val label: String) {
    KNIGHT("Knight"), ENEMIES("Enemies"), WEAPONS("Weapons"), ACHIEVEMENTS("Achievements")
}

/**
 * The in-game reference: the current knight build, every enemy's base stats, every
 * weapon's stats/unlock status, and earned achievements — so a player can check "how
 * strong is this thing" or "what am I working toward" without leaving the run. Reads
 * entirely from the static [EnemyKind]/[WeaponKind]/[Achievement] enums plus the current
 * [PlayerState] and unlocked set; needs no new systems/model state of its own.
 *
 * Embedded as the "Guide" tab of [SettingsScreen] rather than its own full-screen overlay —
 * seen once and then rarely revisited, so it doesn't need a spot of its own on the main
 * screen.
 */
@Composable
fun CompendiumContent(player: PlayerState, difficulty: Difficulty, unlockedAchievements: Set<Achievement>) {
    var tab by remember { mutableStateOf(CompendiumTab.KNIGHT) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompendiumTab.entries.forEach { candidate ->
                TabChip(text = candidate.label, selected = candidate == tab, onClick = { tab = candidate })
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (tab) {
                CompendiumTab.KNIGHT -> item { KnightSummaryCard(player, difficulty) }
                CompendiumTab.ENEMIES -> items(EnemyKind.entries) { kind -> EnemyEntryCard(kind) }
                CompendiumTab.WEAPONS -> items(WeaponKind.entries) { kind -> WeaponEntryCard(kind, player.weaponOf(kind)) }
                CompendiumTab.ACHIEVEMENTS -> items(Achievement.entries) { achievement ->
                    AchievementEntryCard(achievement, unlocked = achievement in unlockedAchievements)
                }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
private fun IconSwatch(iconSize: Dp = 48.dp, draw: DrawScope.() -> Unit) {
    Canvas(modifier = Modifier.size(iconSize), onDraw = draw)
}

@Composable
private fun KnightSummaryCard(player: PlayerState, difficulty: Difficulty) {
    CompendiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSwatch(iconSize = 56.dp) {
                drawKnightIcon(center = center, radius = size.minDimension * 0.42f)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Your Knight", color = GameColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Level ${player.level} · ${difficulty.displayName}", color = Color.Cyan, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        StatLine("HP", "${player.hp} / ${player.effectiveMaxHp}")
        StatLine("XP", "${player.xp} / ${player.xpToNextLevel}")
        StatLine("Move Speed", "×${"%.2f".format(player.moveSpeedMultiplier)}")
        StatLine("Pickup Radius", "×${"%.2f".format(player.pickupRadiusMultiplier)}")
        StatLine("Cooldown", "×${"%.2f".format(player.cooldownMultiplier)} (lower = faster)")

        Spacer(Modifier.height(12.dp))
        Text("Equipped Weapons", color = GameColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (player.weapons.isEmpty()) {
            Text("None yet", color = GameColors.TextMuted, fontSize = 13.sp)
        } else {
            player.weapons.forEach { weapon ->
                Text("• ${weapon.kind.displayName} — Lv.${weapon.level}", color = GameColors.TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EnemyEntryCard(kind: EnemyKind) {
    CompendiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSwatch {
                drawVectorIcon(icon = kind.icon, center = center, radius = size.minDimension * 0.4f, color = Color(kind.colorArgb))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(kind.displayName, color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (kind.isBoss) {
                    Text("BOSS", color = GameColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        StatLine("HP", "${kind.baseHp}")
        StatLine("Speed", "${kind.baseSpeed.toInt()} px/s")
        StatLine("Damage", "${kind.baseDamage}")
        StatLine("XP reward", "${kind.xpValue}")
        StatLine("Heal drop chance", "${(kind.healthDropChance * 100).toInt()}%")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Base stats at run start — every enemy grows tougher the longer you survive.",
            color = GameColors.TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun WeaponEntryCard(kind: WeaponKind, owned: WeaponInstance?) {
    CompendiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSwatch {
                drawVectorIcon(icon = kind.icon, center = center, radius = size.minDimension * 0.4f, color = Color(kind.colorArgb))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(kind.displayName, color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (owned != null) "Equipped — Lv.${owned.level}" else "Not yet unlocked",
                    color = if (owned != null) GameColors.Success else GameColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(kind.description, color = GameColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        val level = owned?.level ?: 1
        val levelSuffix = if (owned == null) " (Lv.1)" else ""
        StatLine("Damage$levelSuffix", "${kind.damageAtLevel(level)}")
        StatLine("Cooldown$levelSuffix", "${kind.cooldownMsAtLevel(level)} ms")
        val shots = kind.projectileCountAtLevel(level)
        if (shots > 1) StatLine("Shots$levelSuffix", "$shots")
    }
}

@Composable
private fun AchievementEntryCard(achievement: Achievement, unlocked: Boolean) {
    CompendiumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (unlocked) "✓" else "🔒",
                fontSize = 20.sp,
                modifier = Modifier.width(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.displayName,
                    color = if (unlocked) GameColors.Accent else GameColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = achievement.description,
                    color = if (unlocked) GameColors.TextSecondary else GameColors.TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = GameColors.TextSecondary, fontSize = 13.sp)
        Text(value, color = GameColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompendiumCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C2029), RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}
