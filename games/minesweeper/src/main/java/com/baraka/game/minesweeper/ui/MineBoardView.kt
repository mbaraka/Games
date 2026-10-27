package com.baraka.game.minesweeper.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.baraka.game.minesweeper.model.Cell
import com.baraka.game.minesweeper.model.MineSnap

private val DIGITS = Array(9) { it.toString() }

/**
 * Draws the board and turns taps into cell indices. It only redraws when the snapshot changes (there is no per-frame
 * animation), and reuses one [Paint] and one [Path], so a tap costs a single pass over at most ~200 cells.
 * [me] is 0 or 1 and decides which owner colour counts as "mine".
 */
@Composable
fun MineBoardView(
    snap: MineSnap,
    me: Int,
    showOwners: Boolean,
    flagMode: Boolean,
    longPressFlags: Boolean,
    onTap: (index: Int, flag: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD } }
    val path = remember { Path() }
    val flagModeState = rememberUpdatedState(flagMode)
    val longState = rememberUpdatedState(longPressFlags)
    val snapState = rememberUpdatedState(snap)

    Canvas(
        modifier = modifier
            .aspectRatio(snap.w.toFloat() / snap.h)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { p ->
                        val s = snapState.value
                        val idx = cellAt(p, size.width.toFloat(), size.height.toFloat(), s)
                        if (idx >= 0) onTap(idx, flagModeState.value)
                    },
                    onLongPress = { p ->
                        if (!longState.value) return@detectTapGestures
                        val s = snapState.value
                        val idx = cellAt(p, size.width.toFloat(), size.height.toFloat(), s)
                        if (idx >= 0) onTap(idx, !flagModeState.value)
                    }
                )
            }
    ) {
        val cell = size.width / snap.w
        val gap = cell * 0.05f
        paint.textSize = cell * 0.62f
        for (i in snap.cells.indices) {
            val c = snap.cells[i]
            val x = (i % snap.w) * cell
            val y = (i / snap.w) * cell
            drawCell(c, x, y, cell, gap, me, showOwners, paint, path)
        }
    }
}

private fun cellAt(p: Offset, width: Float, height: Float, snap: MineSnap): Int {
    val cx = (p.x / width * snap.w).toInt()
    val cy = (p.y / height * snap.h).toInt()
    return if (cx in 0 until snap.w && cy in 0 until snap.h) cy * snap.w + cx else -1
}

private fun DrawScope.drawCell(
    c: Byte, x: Float, y: Float, cell: Float, gap: Float, me: Int, showOwners: Boolean, paint: Paint, path: Path
) {
    val state = Cell.state(c)
    val topLeft = Offset(x + gap, y + gap)
    val box = Size(cell - gap * 2, cell - gap * 2)
    val ownerColor = if (Cell.owner(c) == me) MineColors.Me else MineColors.Partner
    val over = Cell.isMine(c) && state != Cell.EXPLODED
    when {
        state == Cell.EXPLODED -> {
            drawRect(MineColors.Boom, topLeft, box)
            drawMine(x + cell / 2, y + cell / 2, cell * 0.26f, MineColors.Mine)
            if (showOwners) drawRect(ownerColor, Offset(x + gap, y + cell - gap * 4), Size(cell - gap * 2, gap * 2))
        }
        state == Cell.REVEALED -> {
            drawRect(MineColors.Open, topLeft, box)
            if (showOwners) drawRect(ownerColor.copy(alpha = 0.22f), topLeft, box)
            val n = Cell.adjacent(c)
            if (n in 1..8) text(DIGITS[n], x + cell / 2, y + cell / 2, MineColors.Digits[n], paint)
        }
        over -> {
            // Game over: show the mines nobody hit (a flagged one stays marked so wrong flags are visible).
            drawRect(MineColors.Open, topLeft, box)
            drawMine(x + cell / 2, y + cell / 2, cell * 0.22f, if (state == Cell.FLAGGED) MineColors.Good else MineColors.Muted)
        }
        else -> {
            drawRect(MineColors.Hidden, topLeft, box)
            drawRect(MineColors.HiddenEdge, topLeft, Size(box.width, gap * 2))
            if (state == Cell.FLAGGED) drawFlag(x, y, cell, ownerColor, path)
        }
    }
}

private fun DrawScope.drawMine(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color, r, Offset(cx, cy))
    val spike = r * 1.55f
    drawLine(color, Offset(cx - spike, cy), Offset(cx + spike, cy), r * 0.3f)
    drawLine(color, Offset(cx, cy - spike), Offset(cx, cy + spike), r * 0.3f)
}

private fun DrawScope.drawFlag(x: Float, y: Float, cell: Float, color: Color, path: Path) {
    drawLine(MineColors.Text, Offset(x + cell * 0.42f, y + cell * 0.2f), Offset(x + cell * 0.42f, y + cell * 0.8f), cell * 0.07f)
    path.reset()
    path.moveTo(x + cell * 0.42f, y + cell * 0.2f)
    path.lineTo(x + cell * 0.78f, y + cell * 0.37f)
    path.lineTo(x + cell * 0.42f, y + cell * 0.54f)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.text(s: String, cx: Float, cy: Float, color: Color, paint: Paint) {
    paint.color = color.toArgb()
    val offset = (paint.descent() + paint.ascent()) / 2
    drawContext.canvas.nativeCanvas.drawText(s, cx, cy - offset, paint)
}
