package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.render.drawVectorIcon
import com.baraka.game.ui.theme.GameColors

private val BACKGROUND_COLOR = Color(0xFF12141A)
private val CARD_COLOR = Color(0xFF1C2029)
private val CARD_SELECTED_COLOR = Color(0xFF2A3040)
private val CHIP_COLOR = Color(0xFF2D2D2D)

/**
 * Shown before every run starts: which weapons are eligible to be offered as "New Weapon"
 * level-up picks (see systems/UpgradeChoiceSystem.kt). Fixes two real complaints — a
 * weapon buried in a large random pool might never come up in a given run, and the
 * [GameTunables.MAX_ACTIVE_WEAPONS] equip cap was invisible until you hit it. Picking
 * fewer weapons here raises how often each one shows up; the cap is stated explicitly.
 */
@Composable
fun WeaponDeckPickerScreen(initialSelection: Set<WeaponKind>, onConfirm: (Set<WeaponKind>) -> Unit) {
    var selected by remember { mutableStateOf(initialSelection) }
    val canStart = selected.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Text("Choose Your Weapons", color = GameColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pick which weapons can appear as level-up offers this run. Up to ${GameTunables.MAX_ACTIVE_WEAPONS} can be equipped at once — picking fewer here means you'll see each one more often.",
            color = GameColors.TextSecondary,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionChip(text = "Select All") { selected = WeaponKind.entries.toSet() }
            QuickActionChip(text = "Clear All") { selected = emptySet() }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(WeaponKind.entries) { kind ->
                val isSelected = kind in selected
                WeaponDeckCard(
                    kind = kind,
                    selected = isSelected,
                    onClick = { selected = if (isSelected) selected - kind else selected + kind }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (canStart) "Start Run" else "Pick at least one weapon",
            color = if (canStart) Color.Black else GameColors.TextDisabled,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (canStart) GameColors.Accent else CHIP_COLOR, RoundedCornerShape(12.dp))
                .let { if (canStart) it.clickable { onConfirm(selected) } else it }
                .padding(vertical = 14.dp)
        )
    }
}

@Composable
private fun QuickActionChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = GameColors.TextPrimary,
        fontSize = 13.sp,
        modifier = Modifier
            .background(CHIP_COLOR, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun WeaponDeckCard(kind: WeaponKind, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) CARD_SELECTED_COLOR else CARD_COLOR, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(44.dp)) {
            drawVectorIcon(icon = kind.icon, center = center, radius = size.minDimension * 0.4f, color = Color(kind.colorArgb))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(kind.displayName, color = GameColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(kind.description, color = GameColors.TextSecondary, fontSize = 12.sp)
        }
        Text(
            text = if (selected) "✓" else "",
            color = GameColors.Accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
    }
}
