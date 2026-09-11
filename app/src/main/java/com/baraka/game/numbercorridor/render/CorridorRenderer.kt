package com.baraka.game.numbercorridor.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import com.baraka.game.numbercorridor.model.AvatarDesign
import com.baraka.game.numbercorridor.model.Bullet
import com.baraka.game.numbercorridor.model.GameState
import com.baraka.game.numbercorridor.model.PlayerState
import com.baraka.game.numbercorridor.model.WallKind
import com.baraka.game.numbercorridor.model.WallRow
import com.baraka.game.numbercorridor.model.WallSegment
import kotlin.math.min

private val CORRIDOR_FLOOR_COLOR = Color(0xFF23242E)
private val CORRIDOR_WALL_COLOR = Color(0xFF14151B)
private val LANE_LINE_COLOR = Color(0xFF3A3C4A)
private val OBSTACLE_COLOR = Color(0xFF5B5F6B)
private val DAMAGE_BUFF_COLOR = Color(0xFFFF7043)
private val FIRE_RATE_BUFF_COLOR = Color(0xFF29B6F6)
private val SQUAD_BUFF_COLOR = Color(0xFF66BB6A)
private val BULLET_COLOR = Color(0xFFFFF176)

/** The near (player-line) edge sits at this fraction of the canvas height; the horizon sits near the top. */
private const val HORIZON_Y_FRACTION = 0.12f
private const val PLAYER_LINE_Y_FRACTION = 0.92f
private const val TOP_WIDTH_FRACTION = 0.22f
private const val BOTTOM_WIDTH_FRACTION = 0.94f

/**
 * Draws the pseudo-3D corridor: lanes converge toward a horizon near the top of the screen
 * and widen toward the player's line near the bottom, so a [WallRow] scrolling from
 * progress 0 to 1 reads as "coming toward the camera" rather than just sliding down a flat
 * grid. All depth -> screen math lives in [depthGeometry] so every draw call agrees on it.
 */
@Composable
fun CorridorRenderer(state: GameState, onSizeMeasured: (widthPx: Float, heightPx: Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> onSizeMeasured(size.width.toFloat(), size.height.toFloat()) }
    ) {
        drawCorridorFloor()
        state.rows.forEach { row -> drawWallRow(row) }
        drawBullets(state.bullets)
        drawSquad(state.player, state.avatarDesign)
    }
}

/** The trapezoid geometry for a given normalized depth (0 = horizon, 1 = player's line). */
private data class DepthGeometry(val screenY: Float, val laneWidth: Float, val corridorLeft: Float)

private fun DrawScope.depthGeometry(depth: Float): DepthGeometry {
    val horizonY = size.height * HORIZON_Y_FRACTION
    val playerLineY = size.height * PLAYER_LINE_Y_FRACTION
    val screenY = horizonY + depth * (playerLineY - horizonY)

    val topWidth = size.width * TOP_WIDTH_FRACTION
    val bottomWidth = size.width * BOTTOM_WIDTH_FRACTION
    val corridorWidth = topWidth + depth * (bottomWidth - topWidth)
    val corridorLeft = (size.width - corridorWidth) / 2f

    return DepthGeometry(screenY, corridorWidth / WallRow.LANE_COUNT, corridorLeft)
}

private fun DrawScope.drawCorridorFloor() {
    drawRect(CORRIDOR_WALL_COLOR)
    val far = depthGeometry(0f)
    val near = depthGeometry(1f)
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(far.corridorLeft, far.screenY)
        lineTo(far.corridorLeft + far.laneWidth * WallRow.LANE_COUNT, far.screenY)
        lineTo(near.corridorLeft + near.laneWidth * WallRow.LANE_COUNT, near.screenY)
        lineTo(near.corridorLeft, near.screenY)
        close()
    }
    drawPath(path, CORRIDOR_FLOOR_COLOR)

    for (lane in 0..WallRow.LANE_COUNT) {
        drawLine(
            LANE_LINE_COLOR,
            Offset(far.corridorLeft + far.laneWidth * lane, far.screenY),
            Offset(near.corridorLeft + near.laneWidth * lane, near.screenY),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawWallRow(row: WallRow) {
    if (row.progress <= 0f || row.progress >= 1f) return
    val geometry = depthGeometry(row.progress)
    val tileHeight = min(geometry.laneWidth * 0.9f, size.height * 0.1f)

    row.segments.forEachIndexed { lane, segment ->
        if (segment == null) return@forEachIndexed
        val laneLeft = geometry.corridorLeft + geometry.laneWidth * lane
        val color = when (val kind = segment.kind) {
            WallKind.Obstacle -> OBSTACLE_COLOR
            is WallKind.Buff -> when (kind.target) {
                com.baraka.game.numbercorridor.model.BuffTarget.DAMAGE -> DAMAGE_BUFF_COLOR
                com.baraka.game.numbercorridor.model.BuffTarget.FIRE_RATE -> FIRE_RATE_BUFF_COLOR
                com.baraka.game.numbercorridor.model.BuffTarget.SQUAD -> SQUAD_BUFF_COLOR
            }
        }
        val damagedAlpha = 0.55f + 0.45f * (segment.hp.toFloat() / segment.maxHp.toFloat())
        val rectTop = geometry.screenY - tileHeight
        drawRect(
            color = color.copy(alpha = color.alpha * damagedAlpha),
            topLeft = Offset(laneLeft + geometry.laneWidth * 0.06f, rectTop),
            size = androidx.compose.ui.geometry.Size(geometry.laneWidth * 0.88f, tileHeight)
        )
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = (geometry.laneWidth * 0.32f).coerceIn(14f, 40f)
                isAntiAlias = true
                isFakeBoldText = true
            }
            drawText(
                segment.label(),
                laneLeft + geometry.laneWidth / 2f,
                rectTop + tileHeight / 2f + paint.textSize / 3f,
                paint
            )
        }
    }
}

private fun DrawScope.drawBullets(bullets: List<Bullet>) {
    bullets.forEach { bullet ->
        val geometry = depthGeometry(bullet.progress)
        val cx = geometry.corridorLeft + geometry.laneWidth * (bullet.lane + 0.5f)
        val radius = (geometry.laneWidth * 0.08f).coerceIn(3f, 10f)
        drawCircle(BULLET_COLOR, radius = radius, center = Offset(cx, geometry.screenY))
    }
}

private fun DrawScope.drawSquad(player: PlayerState, avatarDesign: AvatarDesign) {
    val geometry = depthGeometry(1f)
    val avatarColor = Color(avatarDesign.colorArgb)
    for (lane in player.occupiedLanes) {
        val cx = geometry.corridorLeft + geometry.laneWidth * (lane + 0.5f)
        val cy = geometry.screenY - geometry.laneWidth * 0.35f
        val radius = geometry.laneWidth * 0.32f

        // Body
        drawCircle(avatarColor, radius = radius, center = Offset(cx, cy))
        // Weapon barrel pointing up the corridor
        drawLine(
            color = Color(0xFF212121),
            start = Offset(cx, cy - radius * 0.2f),
            end = Offset(cx, cy - radius * 1.6f),
            strokeWidth = radius * 0.28f
        )
        drawCircle(Color.White.copy(alpha = 0.85f), radius = radius * 0.28f, center = Offset(cx, cy - radius * 0.35f))
    }

    // Squad outline to make the occupied-lane window legible even at squadCount == 1.
    val left = geometry.corridorLeft + geometry.laneWidth * player.laneStart
    val width = geometry.laneWidth * player.squadCount
    drawRect(
        color = avatarColor.copy(alpha = 0.5f),
        topLeft = Offset(left, geometry.screenY - geometry.laneWidth * 0.75f),
        size = androidx.compose.ui.geometry.Size(width, geometry.laneWidth * 0.75f),
        style = Stroke(width = 3f)
    )
}
