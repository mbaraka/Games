package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.baraka.game.knightsurvivor.model.Vec2

private val OUTER_DIAMETER = 120.dp
private val KNOB_RADIUS = 24.dp

/**
 * A fixed-position virtual joystick. Purely an input widget — it knows nothing about the
 * game, it just reports a direction via [onDirectionChanged] as a [Vec2] whose length is
 * always <= 1, and re-centers itself when released.
 */
@Composable
fun VirtualJoystick(onDirectionChanged: (Vec2) -> Unit, modifier: Modifier = Modifier) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(OUTER_DIAMETER)
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
            .pointerInput(Unit) {
                val maxRadiusPx = minOf(size.width, size.height) / 2f
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val proposed = knobOffset + dragAmount
                        val distance = proposed.getDistance()
                        knobOffset = if (distance > maxRadiusPx) proposed * (maxRadiusPx / distance) else proposed
                        onDirectionChanged(Vec2(knobOffset.x / maxRadiusPx, knobOffset.y / maxRadiusPx))
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onDirectionChanged(Vec2.ZERO)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onDirectionChanged(Vec2.ZERO)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(OUTER_DIAMETER)) {
            drawCircle(color = Color.White.copy(alpha = 0.25f), radius = size.minDimension / 2f, style = Stroke(width = 2f))
            drawCircle(color = Color.White.copy(alpha = 0.55f), radius = KNOB_RADIUS.toPx(), center = center + knobOffset)
        }
    }
}
