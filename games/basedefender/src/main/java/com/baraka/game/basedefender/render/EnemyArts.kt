package com.baraka.game.basedefender.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.baraka.game.basedefender.model.EnemyShape
import com.baraka.game.basedefender.systems.Enemy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * One way of drawing an enemy family. [body] is already tinted for damage / hit flash, so an art never
 * looks at HP itself. To add a family: add an [EnemyShape], write an art here, and list it in [artFor].
 */
fun interface EnemyArt {
    fun draw(scope: DrawScope, e: Enemy, body: Color, time: Int, path: Path)
}

private val DARK = Color(0xFF14121C)
private val EYE = Color(0xFFFFF59D)

private object BlobArt : EnemyArt {
    override fun draw(scope: DrawScope, e: Enemy, body: Color, time: Int, path: Path) = with(scope) {
        val r = e.size * 0.5f
        val c = Offset(e.x, e.y)
        val squash = 1f + 0.06f * sin((time + e.phase * 10f) * 0.2f)
        // Little feet that wiggle as it walks down.
        for (k in -1..1 step 2) {
            val foot = sin((time + e.phase * 10f) * 0.3f + k) * 2f
            drawCircle(DARK, r * 0.28f, Offset(e.x + k * r * 0.55f, e.y - r * 0.85f + foot))
        }
        drawOval(body, Offset(e.x - r * squash, e.y - r / squash), Size(r * 2f * squash, r * 2f / squash))
        drawOval(Color.White.copy(alpha = 0.18f), Offset(e.x - r * 0.55f, e.y - r * 0.75f), Size(r * 0.7f, r * 0.4f))
        // Eyes look down the lane.
        for (k in -1..1 step 2) {
            val ex = e.x + k * r * 0.38f
            drawCircle(Color.White, r * 0.27f, Offset(ex, e.y - r * 0.05f))
            drawCircle(DARK, r * 0.13f, Offset(ex, e.y + r * 0.08f))
        }
        drawLine(DARK, Offset(e.x - r * 0.3f, e.y + r * 0.5f), Offset(e.x + r * 0.3f, e.y + r * 0.5f), strokeWidth = 2f)
    }
}

private object DartArt : EnemyArt {
    override fun draw(scope: DrawScope, e: Enemy, body: Color, time: Int, path: Path) = with(scope) {
        val r = e.size * 0.55f
        // Engine trail above it.
        val flick = 0.6f + 0.4f * sin(time * 0.7f + e.phase)
        drawLine(Color(0xFFFFB74D).copy(alpha = 0.55f), Offset(e.x, e.y - r * 0.7f), Offset(e.x, e.y - r * (1.6f + flick)), strokeWidth = r * 0.5f)
        path.rewind()
        path.moveTo(e.x, e.y + r * 1.1f)
        path.lineTo(e.x - r * 0.95f, e.y - r * 0.8f)
        path.lineTo(e.x, e.y - r * 0.35f)
        path.lineTo(e.x + r * 0.95f, e.y - r * 0.8f)
        path.close()
        drawPath(path, body)
        path.rewind()
        path.moveTo(e.x, e.y + r * 0.55f)
        path.lineTo(e.x - r * 0.35f, e.y - r * 0.3f)
        path.lineTo(e.x + r * 0.35f, e.y - r * 0.3f)
        path.close()
        drawPath(path, Color.White.copy(alpha = 0.3f))
        drawCircle(EYE, r * 0.16f, Offset(e.x, e.y))
    }
}

private object TankArt : EnemyArt {
    override fun draw(scope: DrawScope, e: Enemy, body: Color, time: Int, path: Path) = with(scope) {
        val s = e.size
        val tl = Offset(e.x - s * 0.5f, e.y - s * 0.5f)
        drawRoundRect(lerp(body, DARK, 0.35f), tl, Size(s, s), CornerRadius(s * 0.18f))
        drawRoundRect(body, Offset(tl.x + s * 0.08f, tl.y + s * 0.08f), Size(s * 0.84f, s * 0.84f), CornerRadius(s * 0.14f))
        // Armour plates and rivets.
        drawLine(DARK.copy(alpha = 0.5f), Offset(tl.x + s * 0.1f, e.y), Offset(tl.x + s * 0.9f, e.y), strokeWidth = 2f)
        for (k in 0..3) {
            val rx = tl.x + s * (if (k % 2 == 0) 0.17f else 0.83f)
            val ry = tl.y + s * (if (k < 2) 0.17f else 0.83f)
            drawCircle(Color.White.copy(alpha = 0.55f), 1.8f, Offset(rx, ry))
        }
        // Glowing visor slit.
        drawRoundRect(Color(0xFFFF5252), Offset(e.x - s * 0.28f, e.y + s * 0.08f), Size(s * 0.56f, s * 0.12f), CornerRadius(3f))
    }
}

private object HullArt : EnemyArt {
    override fun draw(scope: DrawScope, e: Enemy, body: Color, time: Int, path: Path) = with(scope) {
        val r = e.size * 0.5f
        val pulse = 0.5f + 0.5f * sin(time * 0.1f)
        drawCircle(body.copy(alpha = 0.18f + 0.12f * pulse), r * 1.45f, Offset(e.x, e.y))
        // Crown of spikes.
        for (k in -2..2) {
            val bx = e.x + k * r * 0.4f
            path.rewind()
            path.moveTo(bx - r * 0.14f, e.y - r * 0.7f)
            path.lineTo(bx, e.y - r * (1.15f + 0.1f * (2 - kotlin.math.abs(k))))
            path.lineTo(bx + r * 0.14f, e.y - r * 0.7f)
            path.close()
            drawPath(path, lerp(body, DARK, 0.4f))
        }
        path.rewind()
        for (k in 0 until 6) {
            val a = (k * PI / 3.0 + PI / 6.0).toFloat()
            val px = e.x + cos(a) * r
            val py = e.y + sin(a) * r
            if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, body)
        drawCircle(lerp(body, DARK, 0.45f), r * 0.62f, Offset(e.x, e.y))
        for (k in -1..1) {
            drawCircle(Color(0xFFFF1744), r * 0.13f, Offset(e.x + k * r * 0.32f, e.y - r * 0.05f))
            drawCircle(Color.White, r * 0.05f, Offset(e.x + k * r * 0.32f, e.y - r * 0.05f))
        }
        drawLine(DARK, Offset(e.x - r * 0.35f, e.y + r * 0.3f), Offset(e.x + r * 0.35f, e.y + r * 0.3f), strokeWidth = 3f)
    }
}

private val ARTS: Array<EnemyArt> = Array(EnemyShape.entries.size) { artFor(EnemyShape.entries[it]) }

/** Exhaustive on purpose: a new [EnemyShape] cannot ship without art. */
private fun artFor(shape: EnemyShape): EnemyArt = when (shape) {
    EnemyShape.BLOB -> BlobArt
    EnemyShape.DART -> DartArt
    EnemyShape.TANK -> TankArt
    EnemyShape.HULL -> HullArt
}

fun enemyArt(shape: EnemyShape): EnemyArt = ARTS[shape.ordinal]
