package com.baraka.game.tunnelcrew.ui

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.tunnelcrew.data.CrewRepository
import com.baraka.game.tunnelcrew.model.LEVELS
import com.baraka.game.tunnelcrew.model.Level
import com.baraka.game.tunnelcrew.model.Skill
import com.baraka.game.tunnelcrew.render.SkillIcon
import com.baraka.game.tunnelcrew.systems.Terrain
import com.baraka.game.tunnelcrew.systems.WORLD_H
import com.baraka.game.tunnelcrew.systems.WORLD_W

/** A small picture of the level: its sky and terrain, drawn from the same data the game uses. */
@Composable
private fun LevelPreview(level: Level, modifier: Modifier = Modifier) {
    val terrain = remember(level) { Terrain(level.theme).also { level.build(it); it.flush() } }
    DisposableEffect(terrain) { onDispose { terrain.bitmap.recycle() } }
    val image = remember(terrain) { terrain.bitmap.asImageBitmap() }
    Canvas(modifier.aspectRatio(WORLD_W.toFloat() / WORLD_H).clip(RoundedCornerShape(10.dp))) {
        drawRect(
            Brush.verticalGradient(listOf(Color(level.theme.skyTop), Color(level.theme.skyBottom))),
            Offset.Zero, size
        )
        drawImage(
            image, IntOffset.Zero, IntSize(WORLD_W, WORLD_H),
            IntOffset.Zero, IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.Low
        )
        val sx = size.width / WORLD_W
        val sy = size.height / WORLD_H
        val e = level.exit
        drawCircle(Color(0xFFFFE8A3), 3.2f * sx * 2f, Offset((e[0] + e[2] / 2f) * sx, (e[1] + e[3] / 2f) * sy))
    }
}

/** Level select: a grid of every level with a preview and the best result so far; locked levels open as earlier ones are beaten. */
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
        Modifier.fillMaxSize().background(CrewColors.Bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CrewButton("< Hub", onClick = onExit)
            Text("TUNNEL CREW", color = CrewColors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            CrewButton("Settings", onClick = onSettings)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(190.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Your tiny crew walks on its own and never stops. Pick a skill below the map, tap a crew member to give it to them, " +
                        "and steer enough of them to the exit door. Pause to take your time - orders still work while paused.",
                    color = CrewColors.Muted,
                    fontSize = 13.sp
                )
            }
            itemsIndexed(LEVELS) { i, level ->
                val open = i < unlocked
                val best = repo.best(i)
                val done = best >= level.needed
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(CrewColors.Card, RoundedCornerShape(16.dp))
                        .clickable(enabled = open) { onPlay(i) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LevelPreview(level, Modifier.fillMaxWidth().alpha(if (open) 1f else 0.35f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (open) "LEVEL ${i + 1}" else "LEVEL ${i + 1}  -  LOCKED",
                            color = if (open) CrewColors.Accent else CrewColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            level.rating.label,
                            color = Color(0xFF10131A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color(level.rating.color), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                    Text(level.name, color = CrewColors.Text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        when {
                            done -> "Best $best/${level.total}  -  done"
                            best > 0 -> "Best $best/${level.total}  (need ${level.needed})"
                            else -> "Save ${level.needed} of ${level.total}"
                        },
                        color = if (done) CrewColors.Good else CrewColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (skill in Skill.entries) {
                            val n = level.skills[skill.ordinal]
                            if (n <= 0) continue
                            SkillIcon(skill, tint = skill.color, size = 18.dp)
                            Text("$n", color = CrewColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
