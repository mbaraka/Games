package com.baraka.game.cratecrew.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.cratecrew.data.CrateRepository
import com.baraka.game.cratecrew.model.CRATE_LEVELS
import com.baraka.game.cratecrew.model.CrateLevel

/** A tiny top-down minimap of the level, drawn straight from its layout string - no bitmaps needed. */
@Composable
private fun LevelPreview(level: CrateLevel, modifier: Modifier = Modifier) {
    val rows = level.layout
    val w = rows.maxOf { it.length }
    val h = rows.size
    Canvas(modifier.aspectRatio(w.toFloat() / h).clip(RoundedCornerShape(8.dp)).background(CrateColors.Floor)) {
        val cw = size.width / w
        val ch = size.height / h
        for (y in rows.indices) {
            val row = rows[y]
            for (x in 0 until w) {
                val ch2 = if (x < row.length) row[x] else ' '
                val color = when (ch2) {
                    '#', ' ' -> CrateColors.Wall
                    'T' -> CrateColors.Target
                    'C' -> CrateColors.Crate
                    'B' -> CrateColors.CrateOnTarget
                    'P', 'S' -> CrateColors.Player
                    else -> null
                } ?: continue
                drawRect(color, Offset(x * cw, y * ch), Size(cw, ch))
            }
        }
    }
}

/** Level select: a grid of every level with a minimap and the best move count so far; locked levels open as earlier ones are beaten. */
@Composable
fun CrateMenuScreen(
    repo: CrateRepository,
    progressRev: Int,
    gridState: LazyGridState,
    onPlay: (Int) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    progressRev
    val unlocked = repo.unlockedCount()
    Column(
        Modifier.fillMaxSize().background(CrateColors.Bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CrateButton("< Hub", onClick = onExit)
            Text("CRATE CREW", color = CrateColors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            CrateButton("Settings", onClick = onSettings)
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(170.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Push every crate onto a target. You can't pull, so plan your route - and you can always undo.",
                    color = CrateColors.Muted,
                    fontSize = 13.sp
                )
            }
            itemsIndexed(CRATE_LEVELS) { i, level ->
                val open = i < unlocked
                val best = repo.bestMoves(i)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(CrateColors.Card, RoundedCornerShape(16.dp))
                        .clickable(enabled = open) { onPlay(i) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LevelPreview(level, Modifier.fillMaxWidth().alpha(if (open) 1f else 0.35f))
                    Text(
                        if (open) "LEVEL ${i + 1}" else "LEVEL ${i + 1}  -  LOCKED",
                        color = if (open) CrateColors.Accent else CrateColors.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(level.name, color = CrateColors.Text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (best > 0) "Best $best moves  (par ${level.par})" else "Par ${level.par} moves",
                        color = if (best > 0) CrateColors.Good else CrateColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
