package com.baraka.game.clashdecks.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.clashdecks.model.Card
import com.baraka.game.clashdecks.model.CardLibrary
import com.baraka.game.clashdecks.model.Rarity
import com.baraka.game.ui.theme.GameColors

/**
 * Pick which unlocked cards go in the player's deck for single-player duels (the CPU always
 * plays the full [CardLibrary]). Reachable from the main menu only — co-op doesn't use a
 * built deck. Locked cards (anything past [unlockedNonCommonCount] in
 * [CardLibrary.unlockOrder]) are shown but not selectable, so the deck a win unlocks next is
 * always visible as something to look forward to, not a mystery.
 */
@Composable
fun ClashDeckBuilderScreen(
    unlockedNonCommonCount: Int,
    selectedDeckIds: Set<String>,
    onSave: (Set<String>) -> Unit,
    onClose: () -> Unit
) {
    val unlockedIds = remember(unlockedNonCommonCount) { CardLibrary.unlockedIds(unlockedNonCommonCount) }
    var selection by remember {
        val initial = selectedDeckIds.filter { it in unlockedIds }.toSet()
        val valid = initial.size in CardLibrary.MIN_DECK_SIZE..CardLibrary.MAX_DECK_SIZE
        mutableStateOf(if (valid) initial else unlockedIds.toList().take(CardLibrary.MAX_DECK_SIZE).toSet())
    }

    fun toggle(card: Card) {
        if (card.id !in unlockedIds) return
        selection = if (card.id in selection) {
            selection - card.id
        } else if (selection.size < CardLibrary.MAX_DECK_SIZE) {
            selection + card.id
        } else {
            selection // already at the cap — ignored rather than silently bumping another card out
        }
    }

    val countColor = if (selection.size in CardLibrary.MIN_DECK_SIZE..CardLibrary.MAX_DECK_SIZE) GameColors.Success else GameColors.Danger

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Build Your Deck", color = GameColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            PillButton("Close", onClose, primary = false)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick ${CardLibrary.MIN_DECK_SIZE}–${CardLibrary.MAX_DECK_SIZE} unlocked cards. The opponent always plays the full set — win duels to unlock Uncommon and Rare cards one at a time.",
            color = GameColors.TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "${selection.size} / ${CardLibrary.MAX_DECK_SIZE} selected",
                color = countColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            PillButton(
                "Save",
                onClick = { onSave(selection) },
                enabled = selection.size in CardLibrary.MIN_DECK_SIZE..CardLibrary.MAX_DECK_SIZE
            )
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Rarity.entries.forEach { rarity ->
                val cards = CardLibrary.all.filter { it.rarity == rarity }
                if (cards.isEmpty()) return@forEach
                item {
                    Text(
                        rarity.displayName,
                        color = Color(rarity.colorArgb),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(cards) { card ->
                    DeckCardRow(
                        card = card,
                        unlocked = card.id in unlockedIds,
                        selected = card.id in selection,
                        onToggle = { toggle(card) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckCardRow(card: Card, unlocked: Boolean, selected: Boolean, onToggle: () -> Unit) {
    val stripe = Color(card.type.colorArgb)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) stripe.copy(alpha = 0.18f) else PanelBg)
            .clickable(enabled = unlocked, onClick = onToggle)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (unlocked) {
            IconSwatch(card.icon, stripe, size = 26.dp)
        } else {
            Text("🔒", fontSize = 18.sp, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                card.name,
                color = if (unlocked) GameColors.TextPrimary else GameColors.TextDisabled,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (unlocked) "Cost ${card.cost}  ·  ${card.summary}" else "Locked — unlock by winning duels",
                color = if (unlocked) GameColors.TextSecondary else GameColors.TextDisabled,
                fontSize = 11.sp,
                maxLines = 2
            )
        }
        if (unlocked) {
            Text(
                if (selected) "✓" else "",
                color = GameColors.Success,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
