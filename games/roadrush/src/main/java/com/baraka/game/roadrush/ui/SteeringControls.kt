package com.baraka.game.roadrush.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Two huge, full-width-split press-and-hold zones for steering — sized for small fingers
 * rather than precision, since a drag/swipe gesture turned out to be error-prone for young
 * players. Kept as big colored slabs, not small circular buttons, so there's no way to miss
 * the target. [onLeftHeldChanged]/[onRightHeldChanged] fire on press-down and release, which
 * ui/RoadRushScreen.kt samples every game tick to move the car continuously for as long as a
 * side is held — see systems/RoadGameLoop.kt's `steerDirection` — rather than stepping once
 * per tap, since the car is no longer confined to fixed lanes.
 */
@Composable
fun SteeringControls(
    onLeftHeldChanged: (Boolean) -> Unit,
    onRightHeldChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        SteeringButton(label = "◀", color = Color(0xFF388E3C), onHeldChanged = onLeftHeldChanged, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(10.dp))
        SteeringButton(label = "▶", color = Color(0xFF1976D2), onHeldChanged = onRightHeldChanged, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SteeringButton(label: String, color: Color, onHeldChanged: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(color, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHeldChanged(true)
                        tryAwaitRelease()
                        onHeldChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
}
