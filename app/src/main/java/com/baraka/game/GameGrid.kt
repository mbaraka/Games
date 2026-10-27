package com.baraka.game

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.theme.GameColors

private const val GRID_COLUMNS = 2

/**
 * The hub's game launcher: [entries] laid out as a [GRID_COLUMNS]-wide grid of big, vividly
 * colored icon tiles (like a casual game portal, not a settings list). Rows are built by
 * hand (not a lazy grid) because the hub screen already scrolls vertically and a lazy grid
 * can't nest inside that.
 */
@Composable
fun GameGrid(
    entries: List<GameCatalogEntry>,
    enabled: Boolean,
    onGameSelected: (GameCatalogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        entries.chunked(GRID_COLUMNS).forEach { rowEntries ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowEntries.forEach { entry ->
                    GameTile(
                        entry = entry,
                        enabled = enabled,
                        onClick = { onGameSelected(entry) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(GRID_COLUMNS - rowEntries.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * One launcher tile: a square filled edge-to-edge with the game's own color, a big icon,
 * and just the title — no description on the grid, matching a casual-game-portal look
 * (color + icon identify the game at a glance; the full description is one long-press
 * away). Tap to play; long-press for the description.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameTile(
    entry: GameCatalogEntry,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInfo by remember { mutableStateOf(false) }
    val baseColor = if (enabled) entry.color else GameColors.TextDisabled
    val iconTint = lerp(baseColor, Color.White, 0.55f)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .alpha(if (enabled) 1f else 0.5f)
            .background(
                Brush.verticalGradient(listOf(lerp(baseColor, Color.Black, 0.12f), lerp(baseColor, Color.Black, 0.32f))),
                RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = { showInfo = true }
            )
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 16.dp)
                .fillMaxSize(0.38f)
        )

        if (entry.multiplayer) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Multiplayer",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        Text(
            text = entry.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(entry.title, color = GameColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(entry.description, color = GameColors.TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showInfo = false; onClick() }) {
                    Text("Play", color = entry.color, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Close", color = GameColors.TextSecondary)
                }
            }
        )
    }
}
