package com.baraka.game.trainsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF12151C)
private val CARD = Color(0xFF1B1F29)
private val TEXT = Color(0xFFFFFFFF)
private val MUTED = Color(0xFFB0B8C6)
private val CYAN = Color(0xFF80DEEA)
private val GOLD = Color(0xFFFFB300)

/** Picks how two trains play together: each on its own phone (linked), or both in one shared world. */
@Composable
fun CoopModeScreen(onLinked: () -> Unit, onShared: () -> Unit, onClose: () -> Unit) {
    BackHandler { onClose() }
    Column(
        modifier = Modifier.fillMaxSize().background(BG).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("CO-OP: PICK A MODE", color = TEXT, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3342), contentColor = TEXT)) { Text("Back", fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ModeCard(
                title = "SHARED WORLD",
                accent = GOLD,
                lines = listOf(
                    "Both trains in ONE world and one screen: yours on the lower track, your partner's above.",
                    "Double the zombies, and every gun reaches both tracks, so you really fight side by side.",
                    "Blast hits both tracks. Help each other with coins, repairs, tows and passengers."
                ),
                tag = "Practice now with a bot; Bluetooth next",
                onClick = onShared,
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                title = "LINKED TRAINS",
                accent = CYAN,
                lines = listOf(
                    "Two phones, each running its own train, linked over Bluetooth.",
                    "Routes and bosses are shared; the partner's train shows on the co-op routes.",
                    "Co-op-only maps with jams and gates, twin bosses and rescues."
                ),
                tag = "Bluetooth: host or join",
                onClick = onLinked,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeCard(title: String, accent: Color, lines: List<String>, tag: String, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(CARD).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        for (line in lines) Text(line, color = TEXT, fontSize = 12.sp)
        Text(tag, color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
