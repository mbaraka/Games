package com.baraka.game.knightsurvivor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.baraka.game.knightsurvivor.model.Vec2

private val MAX_KNOB_OFFSET = 50.dp
private val DEAD_ZONE = 10.dp
private val KNOB_RADIUS = 24.dp
private val BASE_RADIUS = 46.dp

/**
 * A "floating" joystick: touch down anywhere on [modifier]'s area to make it appear right
 * there (no fixed corner to reach for, see AGENTS.md feedback F-whatever/the owner's touch-
 * anywhere request), drag to steer, release to hide it again. Past [DEAD_ZONE] it reports a
 * full-length direction immediately rather than scaling with how far the drag has gone - this
 * is a move-or-don't game (no reason to reward precise half-speed control), and scaling by
 * distance from a fixed, often awkwardly-placed center was exactly what read as "the knight
 * randomly slows down" to a player whose thumb drifts a few px while holding a direction.
 */
@Composable
fun VirtualJoystick(onDirectionChanged: (Vec2) -> Unit, modifier: Modifier = Modifier) {
    var origin by remember { mutableStateOf<Offset?>(null) }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { MAX_KNOB_OFFSET.toPx() }
    val deadZonePx = with(density) { DEAD_ZONE.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        origin = startOffset
                        knobOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val proposed = knobOffset + dragAmount
                        val distance = proposed.getDistance()
                        knobOffset = if (distance > maxOffsetPx) proposed * (maxOffsetPx / distance) else proposed
                        val direction = if (distance < deadZonePx) {
                            Vec2.ZERO
                        } else {
                            Vec2(knobOffset.x, knobOffset.y).normalized()
                        }
                        onDirectionChanged(direction)
                    },
                    onDragEnd = {
                        origin = null
                        knobOffset = Offset.Zero
                        onDirectionChanged(Vec2.ZERO)
                    },
                    onDragCancel = {
                        origin = null
                        knobOffset = Offset.Zero
                        onDirectionChanged(Vec2.ZERO)
                    }
                )
            }
    ) {
        val center = origin ?: return@Canvas
        drawCircle(color = Color.White.copy(alpha = 0.18f), radius = BASE_RADIUS.toPx(), center = center)
        drawCircle(color = Color.White.copy(alpha = 0.3f), radius = BASE_RADIUS.toPx(), center = center, style = Stroke(width = 2f))
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = KNOB_RADIUS.toPx(), center = center + knobOffset)
    }
}
