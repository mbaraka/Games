package com.baraka.game.arcshot.ui

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
import com.baraka.game.arcshot.data.ArcRepository
import com.baraka.game.arcshot.model.ARC_LEVELS
import com.baraka.game.arcshot.model.ArcLevel
import com.baraka.game.arcshot.model.GROUND_Y
import com.baraka.game.arcshot.model.LAUNCHER_X
import com.baraka.game.arcshot.model.WORLD_H
import com.baraka.game.arcshot.model.WORLD_W

/** A tiny top-down minimap of the level's launcher, obstacles and target, drawn straight from its data. */
@Composable
private fun LevelPreview(level: ArcLevel, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(WORLD_W / WORLD_H).clip(RoundedCornerShape(8.dp)).background(ArcColors.SkyBottom)) {
        val sx = size.width / WORLD_W
        val sy = size.height / WORLD_H
        drawRect(ArcColors.Ground, Offset(0f, GROUND_Y * sy), Size(size.width, size.height - GROUND_Y * sy))
        for (r in level.obstacles) drawRect(ArcColors.Obstacle, Offset(r.x * sx, r.y * sy), Size(r.w * sx, r.h * sy))
        drawCircle(ArcColors.Target, level.targetRadius * sx, Offset(level.targetX * sx, level.targetY * sy))
        drawRect(ArcColors.Ball, Offset(LAUNCHER_X * sx - 4f, GROUND_Y * sy - 10f), Size(8f, 10f))
    }
}

/** Level select: a grid of every level with a minimap and the best star rating so far; locked levels open as earlier ones are beaten. */
@Composable
fun ArcMenuScreen(
    repo: ArcRepository,
    progressRev: Int,
    gridState: LazyGridState,
    onPlay: (Int) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    progressRev
    val unlocked = repo.unlockedCount()
    Column(
        Modifier.fillMaxSize().background(ArcColors.Bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArcButton("< Hub", onClick = onExit)
            Text("ARC SHOT", color = ArcColors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            ArcButton("Settings", onClick = onSettings)
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(190.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Drag back from the cannon like a slingshot, then let go to launch. Arc the ball over obstacles and land it on the target.",
                    color = ArcColors.Muted,
                    fontSize = 13.sp
                )
            }
            itemsIndexed(ARC_LEVELS) { i, level ->
                val open = i < unlocked
                val stars = repo.bestStars(i)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(ArcColors.Card, RoundedCornerShape(16.dp))
                        .clickable(enabled = open) { onPlay(i) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LevelPreview(level, Modifier.fillMaxWidth().alpha(if (open) 1f else 0.35f))
                    Text(
                        if (open) "LEVEL ${i + 1}" else "LEVEL ${i + 1}  -  LOCKED",
                        color = if (open) ArcColors.Accent else ArcColors.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(level.name, color = ArcColors.Text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (s in 1..3) {
                            Text(if (s <= stars) "★" else "☆", color = if (s <= stars) ArcColors.Star else ArcColors.Muted, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
