package com.baraka.game.clashdecks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.baraka.game.clashdecks.model.CardType
import com.baraka.game.clashdecks.model.StatusKind
import com.baraka.game.clashdecks.systems.DuelEngine
import com.baraka.game.ui.theme.GameColors

private enum class CodexTab(val label: String) { CARDS("Cards"), EFFECTS("Effects"), HOW_TO_PLAY("How to Play") }

/** The reference screen — reachable from the menu and from a button during a duel. Reads
 *  entirely off [CardLibrary] and [StatusKind]; no state of its own beyond the tab. */
@Composable
fun CodexScreen(onClose: () -> Unit) {
    var tab by remember { mutableStateOf(CodexTab.CARDS) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Codex", color = GameColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            PillButton("Close", onClose, primary = false)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CodexTab.entries.forEach { t -> TabChip(t.label, t == tab) { tab = t } }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            CodexTab.CARDS -> CardsTab()
            CodexTab.EFFECTS -> EffectsTab()
            CodexTab.HOW_TO_PLAY -> HowToPlayTab()
        }
    }
}

@Composable
private fun CardsTab() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardType.entries.forEach { type ->
            item {
                Text(
                    type.displayName + " cards",
                    color = Color(type.colorArgb),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            items(CardLibrary.all.filter { it.type == type }) { card -> CodexCardRow(card) }
        }
    }
}

@Composable
private fun CodexCardRow(card: Card) {
    CodexCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSwatch(card.icon, Color(card.type.colorArgb), size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(card.name, color = GameColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Cost ${card.cost}  ·  ${card.rarity.displayName}",
                    color = Color(card.rarity.colorArgb),
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(card.summary, color = GameColors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(card.flavor, color = GameColors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun EffectsTab() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(StatusKind.entries) { kind ->
            CodexCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconSwatch(kind.icon, Color(kind.colorArgb), size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(kind.displayName, color = GameColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (kind.isDebuff) "Debuff" else "Buff",
                            color = if (kind.isDebuff) GameColors.Danger else GameColors.Success,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(kind.description, color = GameColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HowToPlayTab() {
    val sections = listOf(
        "Goal" to "Reduce your opponent from ${DuelEngine.START_HP} HP to 0. You take the first turn.",
        "Energy" to "You start a turn with energy equal to 2 + the round number, capped at ${DuelEngine.MAX_ENERGY}. " +
            "Unspent energy does not carry over. Play as many cards as you can afford, in any order.",
        "Card types" to "Attack deals damage now. Defense adds Block. Support heals, draws, ramps energy, or applies statuses.",
        "Block" to "Block absorbs incoming damage. It lasts through the opponent's turn and is cleared at the start of your next turn.",
        "Draw" to "You draw 1 card at the start of every turn (after the first). Hand cap is ${DuelEngine.HAND_CAP}.",
        "Status effects" to "Stun skips your card plays for a turn. Freeze skips your whole turn. Poison ignores Block. " +
            "See the Effects tab for the full list. Cleanse removes all debuffs from you.",
        "Turn order each turn" to "Freeze check → Poison tick → Regen tick → status timers count down → Block clears → energy refills → draw."
    )
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sections) { (title, body) ->
            CodexCard {
                Text(title, color = GameColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(body, color = GameColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CodexCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBg)
            .padding(14.dp),
        content = content
    )
}
