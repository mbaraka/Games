package com.baraka.game.numbercorridor.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
private val ENEMY_TILE_COLOR = Color(0xFF5B5F6B)
private val ENEMY_BODY_COLOR = Color(0xFF7C4DA8)
private val SQUAD_SHIFT_COLOR = Color(0xFF66BB6A)
private val WEAPON_UPGRADE_COLOR = Color(0xFFFFB300)
private val WEAPON_DOWNGRADE_COLOR = Color(0xFFB0413E)
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
        val tileLeft = laneLeft + geometry.laneWidth * 0.06f
        val tileWidth = geometry.laneWidth * 0.88f
        val rectTop = geometry.screenY - tileHeight
        val damagedAlpha = 0.55f + 0.45f * (segment.hp.toFloat() / segment.maxHp.toFloat())

        when (val kind = segment.kind) {
            is WallKind.Obstacle -> {
                drawRect(
                    color = ENEMY_TILE_COLOR.copy(alpha = ENEMY_TILE_COLOR.alpha * damagedAlpha),
                    topLeft = Offset(tileLeft, rectTop),
                    size = androidx.compose.ui.geometry.Size(tileWidth, tileHeight)
                )
                drawEnemyIcons(kind.enemyCount, tileLeft, rectTop, tileWidth, tileHeight)
                drawTileLabel(segment.hp.toString(), laneLeft + geometry.laneWidth / 2f, rectTop + tileHeight * 0.86f, geometry.laneWidth)
            }
            is WallKind.SquadShift -> {
                drawRect(
                    color = SQUAD_SHIFT_COLOR.copy(alpha = SQUAD_SHIFT_COLOR.alpha * damagedAlpha),
                    topLeft = Offset(tileLeft, rectTop),
                    size = androidx.compose.ui.geometry.Size(tileWidth, tileHeight)
                )
                drawTileLabel(segment.label(), laneLeft + geometry.laneWidth / 2f, rectTop + tileHeight / 2f, geometry.laneWidth)
            }
            is WallKind.WeaponCrate -> {
                val crateColor = if (kind.delta >= 0) WEAPON_UPGRADE_COLOR else WEAPON_DOWNGRADE_COLOR
                drawRect(
                    color = crateColor.copy(alpha = crateColor.alpha * damagedAlpha),
                    topLeft = Offset(tileLeft, rectTop),
                    size = androidx.compose.ui.geometry.Size(tileWidth, tileHeight)
                )
                drawTileLabel(segment.label(), laneLeft + geometry.laneWidth / 2f, rectTop + tileHeight / 2f, geometry.laneWidth)
            }
        }
    }
}

private fun DrawScope.drawTileLabel(text: String, centerX: Float, centerY: Float, laneWidth: Float) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = (laneWidth * 0.3f).coerceIn(12f, 36f)
            isAntiAlias = true
            isFakeBoldText = true
        }
        drawText(text, centerX, centerY + paint.textSize / 3f, paint)
    }
}

/** Small monster silhouettes standing for how many hits-worth of enemies this obstacle represents — see systems/WallSpawnSystem.kt. */
private fun DrawScope.drawEnemyIcons(count: Int, tileLeft: Float, tileTop: Float, tileWidth: Float, tileHeight: Float) {
    val iconRadius = (tileWidth / (count * 2.4f)).coerceAtMost(tileHeight * 0.32f)
    val rowY = tileTop + tileHeight * 0.36f
    val spacing = tileWidth / (count + 1)
    for (index in 0 until count) {
        val cx = tileLeft + spacing * (index + 1)
        drawCircle(ENEMY_BODY_COLOR, radius = iconRadius, center = Offset(cx, rowY))
        val eyeOffset = iconRadius * 0.4f
        drawCircle(Color.White, radius = iconRadius * 0.22f, center = Offset(cx - eyeOffset, rowY - eyeOffset * 0.3f))
        drawCircle(Color.White, radius = iconRadius * 0.22f, center = Offset(cx + eyeOffset, rowY - eyeOffset * 0.3f))
        drawCircle(Color.Black, radius = iconRadius * 0.1f, center = Offset(cx - eyeOffset, rowY - eyeOffset * 0.3f))
        drawCircle(Color.Black, radius = iconRadius * 0.1f, center = Offset(cx + eyeOffset, rowY - eyeOffset * 0.3f))
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

/** Offsets (in units of one chibi's radius) for clustering 1-4 squad members around a shared center, tightest first. */
private val SQUAD_CLUSTER_OFFSETS: Map<Int, List<Offset>> = mapOf(
    1 to listOf(Offset(0f, 0f)),
    2 to listOf(Offset(-0.65f, 0.1f), Offset(0.65f, -0.1f)),
    3 to listOf(Offset(-0.75f, 0.35f), Offset(0.75f, 0.35f), Offset(0f, -0.45f)),
    4 to listOf(Offset(-0.65f, -0.55f), Offset(0.65f, -0.55f), Offset(-0.65f, 0.55f), Offset(0.65f, 0.55f))
)

/**
 * Draws the squad as a cluster of chibi soldiers seen from behind — big rounded head with a
 * colored helmet, small torso, stub legs, and a gun barrel pointing up the corridor. All
 * members stay inside one lane's width regardless of [PlayerState.squadCount]: a bigger
 * squad packs in tighter and each member shrinks slightly, it never spreads across lanes
 * (see PlayerState's KDoc for why).
 */
private fun DrawScope.drawSquad(player: PlayerState, avatarDesign: AvatarDesign) {
    val geometry = depthGeometry(1f)
    val avatarColor = Color(avatarDesign.colorArgb)
    val cx = geometry.corridorLeft + geometry.laneWidth * (player.lane + 0.5f)
    val cy = geometry.screenY - geometry.laneWidth * 0.42f

    val offsets = SQUAD_CLUSTER_OFFSETS.getValue(player.squadCount.coerceIn(1, 4))
    val unitRadius = geometry.laneWidth * (0.30f - 0.035f * (player.squadCount - 1))
    val spread = geometry.laneWidth * 0.42f

    offsets.forEach { offset ->
        drawChibiUnit(
            center = Offset(cx + offset.x * spread, cy + offset.y * spread),
            radius = unitRadius,
            helmetColor = avatarColor
        )
    }
}

private val SKIN_COLOR = Color(0xFFF2C79A)
private val VEST_COLOR = Color(0xFF33363F)
private val GUN_COLOR = Color(0xFF212121)

private fun DrawScope.drawChibiUnit(center: Offset, radius: Float, helmetColor: Color) {
    // Contact shadow so the unit reads as standing on the floor rather than floating.
    drawOval(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(center.x - radius * 0.9f, center.y + radius * 1.55f),
        size = androidx.compose.ui.geometry.Size(radius * 1.8f, radius * 0.55f)
    )

    // Legs
    val legWidth = radius * 0.34f
    val legHeight = radius * 0.6f
    listOf(-1f, 1f).forEach { side ->
        drawRoundRect(
            color = VEST_COLOR,
            topLeft = Offset(center.x + side * radius * 0.32f - legWidth / 2f, center.y + radius * 0.85f),
            size = androidx.compose.ui.geometry.Size(legWidth, legHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(legWidth * 0.4f)
        )
    }

    // Gun, held forward and slightly to the side so it doesn't hide behind the head
    drawRoundRect(
        color = GUN_COLOR,
        topLeft = Offset(center.x + radius * 0.55f, center.y - radius * 1.55f),
        size = androidx.compose.ui.geometry.Size(radius * 0.26f, radius * 1.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.1f)
    )

    // Torso
    drawRoundRect(
        color = helmetColor.copy(alpha = 0.85f),
        topLeft = Offset(center.x - radius * 0.62f, center.y + radius * 0.05f),
        size = androidx.compose.ui.geometry.Size(radius * 1.24f, radius * 1.0f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.35f)
    )
    drawRoundRect(
        color = VEST_COLOR,
        topLeft = Offset(center.x - radius * 0.4f, center.y + radius * 0.2f),
        size = androidx.compose.ui.geometry.Size(radius * 0.8f, radius * 0.65f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.2f)
    )

    // Chibi head — big and round, this is most of what reads as "a character" at small sizes.
    val headCenter = Offset(center.x, center.y - radius * 0.55f)
    drawCircle(SKIN_COLOR, radius = radius * 0.78f, center = headCenter)

    // Helmet cap over the top half of the head, colored per avatar design.
    drawArc(
        color = helmetColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headCenter.x - radius * 0.78f, headCenter.y - radius * 0.78f),
        size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f)
    )
    drawLine(
        color = helmetColor.copy(alpha = 0.7f),
        start = Offset(headCenter.x - radius * 0.78f, headCenter.y),
        end = Offset(headCenter.x + radius * 0.78f, headCenter.y),
        strokeWidth = radius * 0.12f
    )

    // Ears, just enough silhouette to read as a head from behind rather than a helmet floating alone.
    listOf(-1f, 1f).forEach { side ->
        drawCircle(SKIN_COLOR, radius = radius * 0.14f, center = Offset(headCenter.x + side * radius * 0.72f, headCenter.y + radius * 0.05f))
    }
}
