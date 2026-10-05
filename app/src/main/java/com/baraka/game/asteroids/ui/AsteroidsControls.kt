package com.baraka.game.asteroids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

/**
 * A four-button touch cluster — rotate left/right and thrust held continuously (reported via
 * [onRotateLeftChanged]/[onRotateRightChanged]/[onThrustChanged] every time the press state
 * flips), fire as a one-shot tap. Asteroids needs independent rotate+thrust+fire, which
 * doesn't map cleanly onto the drag-to-aim or tap-to-fire schemes the other Atari-style
 * games use, so this is its own control surface instead.
 */
@Composable
fun AsteroidsControls(
    onRotateLeftChanged: (Boolean) -> Unit,
    onRotateRightChanged: (Boolean) -> Unit,
    onThrustChanged: (Boolean) -> Unit,
    onFire: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HoldButton(label = "◀", onPressedChanged = onRotateLeftChanged)
            HoldButton(label = "▲", onPressedChanged = onThrustChanged)
            HoldButton(label = "▶", onPressedChanged = onRotateRightChanged)
        }
        TapButton(label = "🔥", onClick = onFire)
    }
}

@Composable
private fun HoldButton(label: String, onPressedChanged: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) { onPressedChanged(pressed) }
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (pressed) Color(0xFF3A3F4A) else Color(0xFF23262E))
            .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 22.sp)
    }
}

@Composable
private fun TapButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFFEF5350))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 24.sp)
    }
}
