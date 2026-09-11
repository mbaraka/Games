package com.baraka.game.roadrush.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import com.baraka.game.roadrush.model.CarDesign
import com.baraka.game.roadrush.model.ObstacleCar
import com.baraka.game.roadrush.model.PlayerCarState
import com.baraka.game.roadrush.model.RIVAL_CAR_COLORS
import com.baraka.game.roadrush.model.RoadState

private val SKY_COLOR = Color(0xFF90CAF9)
private val GRASS_COLOR = Color(0xFF66BB6A)
private val ROAD_COLOR = Color(0xFF37474F)
private val LANE_LINE_COLOR = Color(0xFFFFF9C4)

/** The near (player-line) edge sits at this fraction of the canvas height; the horizon sits near the top. */
private const val HORIZON_Y_FRACTION = 0.1f
private const val PLAYER_LINE_Y_FRACTION = 0.9f
private const val TOP_WIDTH_FRACTION = 0.32f
private const val BOTTOM_WIDTH_FRACTION = 0.96f

/**
 * Draws the pseudo-3D road scene: a cheerful sky-and-grass backdrop, a road that converges
 * toward a horizon near the top of the screen and widens toward the player's line near the
 * bottom (so an [ObstacleCar] scrolling from progress 0 to 1 reads as "coming toward the
 * camera" — same depth trick as numbercorridor's CorridorRenderer), and cartoon-style cars
 * instead of plain rectangles. Lane markings and roadside trees scroll with
 * [RoadState.elapsedSeconds] purely for motion feel; they carry no gameplay meaning.
 */
@Composable
fun RoadRenderer(state: RoadState, carDesign: CarDesign, onSizeMeasured: (widthPx: Float, heightPx: Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> onSizeMeasured(size.width.toFloat(), size.height.toFloat()) }
    ) {
        drawBackdrop()
        drawRoad()
        drawRoadsideTrees(state.elapsedSeconds, state.speed)
        drawLaneDashes(state.elapsedSeconds, state.speed)
        state.cars.forEach { car -> drawRivalCar(car) }
        drawPlayerCar(state.player, carDesign)
    }
}

private data class DepthGeometry(val screenY: Float, val laneWidth: Float, val roadLeft: Float)

private fun DrawScope.depthGeometry(depth: Float): DepthGeometry {
    val horizonY = size.height * HORIZON_Y_FRACTION
    val playerLineY = size.height * PLAYER_LINE_Y_FRACTION
    val screenY = horizonY + depth * (playerLineY - horizonY)

    val topWidth = size.width * TOP_WIDTH_FRACTION
    val bottomWidth = size.width * BOTTOM_WIDTH_FRACTION
    val roadWidth = topWidth + depth * (bottomWidth - topWidth)
    val roadLeft = (size.width - roadWidth) / 2f

    return DepthGeometry(screenY, roadWidth / PlayerCarState.LANE_COUNT, roadLeft)
}

private fun DrawScope.drawBackdrop() {
    val horizonY = size.height * HORIZON_Y_FRACTION
    drawRect(SKY_COLOR, size = Size(size.width, horizonY))
    drawCircle(Color(0xFFFFF59D), radius = size.width * 0.09f, center = Offset(size.width * 0.8f, horizonY * 0.45f))
    drawCloud(size.width * 0.22f, horizonY * 0.35f, size.width * 0.09f)
    drawCloud(size.width * 0.5f, horizonY * 0.6f, size.width * 0.06f)
    drawRect(GRASS_COLOR, topLeft = Offset(0f, horizonY), size = Size(size.width, size.height - horizonY))
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, radius: Float) {
    val puff = Color.White.copy(alpha = 0.9f)
    drawCircle(puff, radius = radius, center = Offset(cx - radius * 0.7f, cy))
    drawCircle(puff, radius = radius * 0.85f, center = Offset(cx + radius * 0.6f, cy))
    drawCircle(puff, radius = radius * 0.9f, center = Offset(cx, cy - radius * 0.3f))
}

private fun DrawScope.drawRoad() {
    val far = depthGeometry(0f)
    val near = depthGeometry(1f)
    val path = Path().apply {
        moveTo(far.roadLeft, far.screenY)
        lineTo(far.roadLeft + far.laneWidth * PlayerCarState.LANE_COUNT, far.screenY)
        lineTo(near.roadLeft + near.laneWidth * PlayerCarState.LANE_COUNT, near.screenY)
        lineTo(near.roadLeft, near.screenY)
        close()
    }
    drawPath(path, ROAD_COLOR)

    for (lane in 0..PlayerCarState.LANE_COUNT) {
        drawLine(
            Color(0xFFECEFF1),
            Offset(far.roadLeft + far.laneWidth * lane, far.screenY),
            Offset(near.roadLeft + near.laneWidth * lane, near.screenY),
            strokeWidth = 3f
        )
    }
}

/** Dashed center-of-lane markers that scroll toward the player, purely for a sense of speed. */
private fun DrawScope.drawLaneDashes(elapsedSeconds: Float, speed: Float) {
    val scrollOffset = (elapsedSeconds * speed) % 0.2f
    var depth = -scrollOffset
    while (depth < 1f) {
        if (depth > 0f) {
            val geometry = depthGeometry(depth)
            for (lane in 1 until PlayerCarState.LANE_COUNT) {
                val x = geometry.roadLeft + geometry.laneWidth * lane
                drawLine(
                    LANE_LINE_COLOR,
                    Offset(x, geometry.screenY),
                    Offset(x, geometry.screenY + geometry.laneWidth * 0.08f),
                    strokeWidth = 4f
                )
            }
        }
        depth += 0.1f
    }
}

/** Simple round-top pine trees dotted along both shoulders, scrolling toward the player for atmosphere. */
private fun DrawScope.drawRoadsideTrees(elapsedSeconds: Float, speed: Float) {
    val scrollOffset = (elapsedSeconds * speed * 0.6f) % 0.3f
    var depth = -scrollOffset
    while (depth < 1f) {
        if (depth > 0.05f) {
            val geometry = depthGeometry(depth)
            val treeSize = (geometry.laneWidth * 0.55f).coerceIn(8f, size.width * 0.14f)
            val roadRight = geometry.roadLeft + geometry.laneWidth * PlayerCarState.LANE_COUNT
            drawTree(geometry.roadLeft - treeSize * 0.9f, geometry.screenY, treeSize)
            drawTree(roadRight + treeSize * 0.9f, geometry.screenY, treeSize)
        }
        depth += 0.3f
    }
}

private fun DrawScope.drawTree(x: Float, y: Float, treeSize: Float) {
    drawRoundRect(
        color = Color(0xFF6D4C41),
        topLeft = Offset(x - treeSize * 0.08f, y - treeSize * 0.15f),
        size = Size(treeSize * 0.16f, treeSize * 0.4f),
        cornerRadius = CornerRadius(treeSize * 0.05f)
    )
    drawCircle(Color(0xFF2E7D32), radius = treeSize * 0.36f, center = Offset(x, y - treeSize * 0.35f))
    drawCircle(Color(0xFF43A047), radius = treeSize * 0.24f, center = Offset(x - treeSize * 0.1f, y - treeSize * 0.48f))
}

private fun DrawScope.drawRivalCar(car: ObstacleCar) {
    if (car.progress <= 0f || car.progress >= 1.15f) return
    val geometry = depthGeometry(car.progress)
    val cx = geometry.roadLeft + geometry.laneWidth * (car.lane + 0.5f)
    val cy = geometry.screenY - geometry.laneWidth * 0.35f
    val color = Color(RIVAL_CAR_COLORS[car.colorSeed % RIVAL_CAR_COLORS.size])
    drawCartoonCar(cx, cy, geometry.laneWidth * 0.33f, color, facingUp = false)
}

private fun DrawScope.drawPlayerCar(player: PlayerCarState, carDesign: CarDesign) {
    val geometry = depthGeometry(1f)
    val cx = geometry.roadLeft + geometry.laneWidth * (player.lane + 0.5f)
    val cy = geometry.screenY - geometry.laneWidth * 0.4f
    val flashing = player.invincibleSeconds > 0f && (player.invincibleSeconds * 10).toInt() % 2 == 0
    val color = if (flashing) Color.White else Color(carDesign.colorArgb)
    drawCartoonCar(cx, cy, geometry.laneWidth * 0.37f, color, facingUp = true)
}

/**
 * A friendly, cartoon-style top-down car: a chunky rounded body with a thick dark outline
 * (comic-strip style so it reads clearly at small sizes), round bubble wheels with hubcaps, a
 * big glassy windshield bubble offset toward the direction of travel, a shiny highlight, and
 * round headlight/taillight dots — built to look like a toy car, not a plain rectangle.
 * [facingUp] mirrors the cabin so the player's car (driving up the screen) and rival cars
 * (driving down, toward the player) both read as facing the right way.
 */
private fun DrawScope.drawCartoonCar(cx: Float, cy: Float, halfWidth: Float, color: Color, facingUp: Boolean) {
    val dir = if (facingUp) -1f else 1f
    val bodyWidth = halfWidth * 1.9f
    val bodyHeight = halfWidth * 2.5f
    val outline = (halfWidth * 0.09f).coerceAtLeast(1.5f)

    // Round bubble wheels with hubcaps, peeking out past the body sides.
    val wheelRadius = halfWidth * 0.34f
    val wheelOffsetX = bodyWidth / 2f * 0.95f
    val wheelOffsetY = bodyHeight * 0.32f
    listOf(-1f, 1f).forEach { side ->
        listOf(-1f, 1f).forEach { end ->
            val center = Offset(cx + side * wheelOffsetX, cy + end * wheelOffsetY)
            drawCircle(Color.Black, radius = wheelRadius, center = center)
            drawCircle(Color(0xFFBDBDBD), radius = wheelRadius * 0.5f, center = center)
        }
    }

    // Chassis with a thick cartoon outline.
    drawRoundRect(
        color = Color.Black,
        topLeft = Offset(cx - bodyWidth / 2f - outline, cy - bodyHeight / 2f - outline),
        size = Size(bodyWidth + outline * 2f, bodyHeight + outline * 2f),
        cornerRadius = CornerRadius(bodyWidth * 0.42f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - bodyWidth / 2f, cy - bodyHeight / 2f),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(bodyWidth * 0.4f)
    )
    // Darker two-tone skirt along the bottom edge (relative to travel direction) for a hint of depth.
    val skirtHeight = bodyHeight * 0.16f
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(cx - bodyWidth / 2f, cy + dir * (bodyHeight / 2f - skirtHeight)),
        size = Size(bodyWidth, skirtHeight),
        cornerRadius = CornerRadius(bodyWidth * 0.2f)
    )
    // Glossy highlight stripe for a toy-like shine.
    drawRoundRect(
        color = Color.White.copy(alpha = 0.3f),
        topLeft = Offset(cx - bodyWidth * 0.34f, cy - bodyHeight * 0.42f),
        size = Size(bodyWidth * 0.14f, bodyHeight * 0.5f),
        cornerRadius = CornerRadius(bodyWidth * 0.07f)
    )

    // Side mirrors, poking out near the front windshield.
    val mirrorY = cy + dir * bodyHeight * 0.02f
    listOf(-1f, 1f).forEach { side ->
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(cx + side * (bodyWidth / 2f - outline * 0.3f), mirrorY - halfWidth * 0.07f),
            size = Size(halfWidth * 0.16f, halfWidth * 0.14f),
            cornerRadius = CornerRadius(halfWidth * 0.05f)
        )
    }

    // Front windshield bubble, offset toward the direction of travel.
    val frontCenterY = cy + dir * bodyHeight * 0.18f
    val frontWidth = bodyWidth * 0.62f
    val frontHeight = bodyHeight * 0.32f
    drawRoundedGlass(cx, frontCenterY, frontWidth, frontHeight, outline)
    drawCircle(
        Color.White.copy(alpha = 0.7f),
        radius = frontWidth * 0.14f,
        center = Offset(cx - frontWidth * 0.15f, frontCenterY - dir * frontHeight * 0.1f)
    )

    // Smaller rear window at the back, so the cabin reads as a full bubble, not just a windshield.
    val rearCenterY = cy - dir * bodyHeight * 0.26f
    val rearWidth = bodyWidth * 0.46f
    val rearHeight = bodyHeight * 0.2f
    drawRoundedGlass(cx, rearCenterY, rearWidth, rearHeight, outline * 0.8f)

    // Round headlight/taillight dots at the leading and trailing edges.
    val lightY = cy - dir * bodyHeight / 2f + dir * halfWidth * 0.22f
    val tailY = cy + dir * bodyHeight / 2f - dir * halfWidth * 0.22f
    val lightRadius = halfWidth * 0.16f
    drawCircle(Color(0xFFFFF59D), radius = lightRadius, center = Offset(cx - bodyWidth * 0.3f, lightY))
    drawCircle(Color(0xFFFFF59D), radius = lightRadius, center = Offset(cx + bodyWidth * 0.3f, lightY))
    drawCircle(Color(0xFFEF5350), radius = lightRadius * 0.8f, center = Offset(cx - bodyWidth * 0.3f, tailY))
    drawCircle(Color(0xFFEF5350), radius = lightRadius * 0.8f, center = Offset(cx + bodyWidth * 0.3f, tailY))
}

/** A rounded glass panel (windshield or rear window) with a thin black outline. */
private fun DrawScope.drawRoundedGlass(cx: Float, centerY: Float, width: Float, height: Float, outline: Float) {
    drawRoundRect(
        color = Color.Black,
        topLeft = Offset(cx - width / 2f - outline * 0.7f, centerY - height / 2f - outline * 0.7f),
        size = Size(width + outline * 1.4f, height + outline * 1.4f),
        cornerRadius = CornerRadius(width * 0.45f)
    )
    drawRoundRect(
        color = Color(0xFF81D4FA),
        topLeft = Offset(cx - width / 2f, centerY - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(width * 0.45f)
    )
}
