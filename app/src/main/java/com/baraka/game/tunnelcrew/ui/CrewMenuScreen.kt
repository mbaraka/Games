package com.baraka.game.tunnelcrew.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.tunnelcrew.data.CrewRepository
import com.baraka.game.tunnelcrew.model.LEVELS
import com.baraka.game.tunnelcrew.model.Skill

/** Level select: a grid of every level with the best result so far; locked levels open as earlier ones are beaten. */
@Composable
fun CrewMenuScreen(
    repo: CrewRepository,
    progressRev: Int,
    onPlay: (Int) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    progressRev
    val unlocked = repo.unlockedCount()
    Column(
        Modifier.fillMaxSize().background(CREW_BG).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD, contentColor = Color.White)) {
                Text("< Hub", fontSize = 12.sp)
            }
            Text("TUNNEL CREW", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Button(onClick = onSettings, colors = ButtonDefaults.buttonColors(containerColor = CREW_CARD, contentColor = Color.White)) {
                Text("Settings", fontSize = 12.sp)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(170.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Your tiny crew walks on its own and never stops. Pick a skill below the map, tap a crew member to give it to them, " +
                        "and steer enough of them to the exit door. Pause to take your time - orders still work while paused.",
                    color = CREW_MUTED,
                    fontSize = 12.sp
                )
            }
            itemsIndexed(LEVELS) { i, level ->
                val open = i < unlocked
                val best = repo.best(i)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(if (open) CREW_CARD else Color(0xFF161A22), RoundedCornerShape(12.dp))
                        .clickable(enabled = open) { onPlay(i) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        if (open) "LEVEL ${i + 1}" else "LEVEL ${i + 1} - LOCKED",
                        color = if (open) CREW_ACCENT else CREW_MUTED,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(level.name, color = if (open) Color.White else CREW_MUTED, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Save ${level.needed} of ${level.total}", color = CREW_MUTED, fontSize = 12.sp)
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            if (best >= level.needed) "Best $best/${level.total}  - done" else if (best > 0) "Best $best/${level.total}" else if (open) "Not played" else "",
                            color = if (best >= level.needed) Color(0xFF69F0AE) else CREW_MUTED,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        Skill.entries.filter { level.skills[it.ordinal] > 0 }.joinToString(" ") { "${it.label} ${level.skills[it.ordinal]}" },
                        color = CREW_MUTED,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
