package com.baraka.game.dreadnought.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

/** The stick's current deflection (-1..1 on each axis). Hoisted so an "all stop" button can recentre it. */
class StickState(x: Float = 0.55f, y: Float = 0f) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
}

/**
 * Course and speed in one thumb: push towards where the ship should head, further for more speed. Letting go
 * keeps the last order (like a helm), so the thumb is free to fire; the centre dead-zone orders all stop.
 */
@Composable
fun CourseStick(state: StickState, onOrder: (vx: Float, vy: Float) -> Unit, modifier: Modifier = Modifier, diameter: Dp = 150.dp) {
    Box(
        modifier = modifier
            .size(diameter)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val r = size.width / 2f
                    fun apply(px: Float, py: Float) {
                        var vx = (px - r) / r
                        var vy = (py - r) / r
                        val m = hypot(vx, vy)
                        if (m > 1f) {
                            vx /= m
                            vy /= m
                        }
                        state.x = vx
                        state.y = vy
                        onOrder(vx, vy)
                    }
                    apply(down.position.x, down.position.y)
                    do {
                        val event = awaitPointerEvent()
                        for (ch in event.changes) {
                            if (ch.id == down.id && ch.pressed) {
                                apply(ch.position.x, ch.position.y)
                                ch.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.width / 2f
            val c = Offset(r, r)
            drawCircle(Color(0x990B1722), r, c)
            drawCircle(Color(0x663E6A8C), r - 1.5f, c, style = Stroke(3f))
            drawCircle(Color(0x443E6A8C), r * 0.15f, c)
            // the knob sits where the order was last set, so it doubles as a readout
            val k = Offset(r + state.x * r * 0.72f, r + state.y * r * 0.72f)
            drawLine(Color(0x88FFB74D), c, k, 4f, StrokeCap.Round)
            drawCircle(Color(0xFFFFB74D), r * 0.27f, k)
            drawCircle(Color(0xFF1B1200), r * 0.1f, k)
        }
    }
}

/**
 * A round button with a progress ring. [fill] 0..1 is how ready it is; [onPress] fires on touch-down and
 * [onRelease] on lift, so the same widget does a tap (repair) and a hold (fire).
 */
@Composable
fun RingButton(
    label: String,
    fill: Float,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    onRelease: () -> Unit = {},
    diameter: Dp = 92.dp
) {
    val ready = fill >= 0.999f
    Box(
        modifier = modifier
            .size(diameter)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPress()
                    waitForUpOrCancellation()
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * 0.075f
            val inset = w / 2f + 1f
            drawCircle(Color(0xCC0B1722), size.width / 2f, center)
            drawCircle(Color(0xFF1F3C55), size.width / 2f - inset, center, style = Stroke(w))
            drawArc(
                color = if (ready) Color(0xFFFFB74D) else Color(0xFF8D6E3F),
                startAngle = -90f,
                sweepAngle = 360f * fill.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2f, size.height - inset * 2f),
                style = Stroke(w, cap = StrokeCap.Butt)
            )
        }
        Text(
            label,
            color = if (ready) Color(0xFFFFB74D) else Color(0xFF93A9BA),
            fontSize = (diameter.value * 0.19f).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
