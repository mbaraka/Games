package com.baraka.game.basedefender.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import com.baraka.game.basedefender.model.Tuning
import com.baraka.game.basedefender.systems.BaseDefenderEngine
import kotlin.math.PI
import kotlin.math.sin

/**
 * Draws a [BaseDefenderEngine] in world units (the scene is scaled to the canvas, so it looks the same on any
 * phone). It owns only reusable scratch objects (Path, Paint, a cached background); nothing here is gameplay.
 */
class BaseDefenderRenderer {
    private val path = Path()
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(3f, 0f, 1.5f, android.graphics.Color.BLACK)
    }

    // Cached per world height: the sky brush and the star field.
    private var cachedH = -1f
    private var sky: Brush? = null
    private val starCount = 46
    private val starX = FloatArray(starCount)
    private val starY = FloatArray(starCount)
    private val starSpeed = FloatArray(starCount)
    private val starSize = FloatArray(starCount)

    private fun prepare(h: Float, ground: Float) {
        if (h == cachedH) return
        cachedH = h
        sky = Brush.verticalGradient(0f to Color(0xFF070B1E), 0.7f to Color(0xFF16254A), 1f to Color(0xFF2B3C66), startY = 0f, endY = ground)
        var seed = 7
        fun next(): Float { seed = seed * 1103515245 + 12345; return ((seed ushr 8) and 0xFFFF) / 65535f }
        for (i in 0 until starCount) {
            starX[i] = next() * Tuning.WORLD_W
            starY[i] = next() * ground
            starSpeed[i] = 0.05f + next() * 0.25f
            starSize[i] = 0.7f + next() * 1.4f
        }
    }

    fun draw(scope: DrawScope, e: BaseDefenderEngine, showCrosshair: Boolean, shakeOn: Boolean) {
        val sc = scope.size.width / Tuning.WORLD_W
        val ground = e.groundY
        prepare(e.worldH, ground)
        val sx = if (shakeOn && e.shake > 0f) (kotlin.random.Random.nextFloat() - 0.5f) * e.shake else 0f
        val sy = if (shakeOn && e.shake > 0f) (kotlin.random.Random.nextFloat() - 0.5f) * e.shake else 0f
        with(scope) {
            scale(sc, sc, pivot = Offset.Zero) {
                translate(sx, sy) {
                    drawBackground(e, ground)
                    drawPickups(e)
                    drawEnemies(e)
                    drawTurrets(e, ground)
                    drawBullets(e)
                    drawParticles(e)
                    if (e.slowTicks > 0) drawSlowOverlay(e)
                    if (showCrosshair && e.aimTicks > 0) drawCrosshair(e)
                }
                drawTexts(e)
                drawBanner(e)
            }
        }
    }

    // ------------------------------------------------------------------ background

    private fun DrawScope.drawBackground(e: BaseDefenderEngine, ground: Float) {
        drawRect(sky ?: Brush.verticalGradient(listOf(Color.Black, Color.Black)), Offset.Zero, Size(Tuning.WORLD_W, ground))
        // Stars drift down at different speeds (cheap parallax) and twinkle.
        for (i in 0 until starCount) {
            val y = (starY[i] + e.time * starSpeed[i]) % ground
            val twinkle = 0.45f + 0.45f * sin(e.time * 0.05f + i)
            drawCircle(Color.White.copy(alpha = twinkle), starSize[i], Offset(starX[i], y))
        }
        // Far hills.
        path.rewind()
        path.moveTo(0f, ground)
        var x = 0f
        var k = 0
        while (x <= Tuning.WORLD_W + 50f) {
            path.lineTo(x, ground - (if (k % 2 == 0) 34f else 14f) - (k * 7 % 13))
            x += 50f
            k++
        }
        path.lineTo(Tuning.WORLD_W, ground)
        path.close()
        drawPath(path, Color(0xFF1A2640))
        // Ground slab with a lit top edge and a hazard stripe.
        drawRect(Color(0xFF2A3446), Offset(0f, ground), Size(Tuning.WORLD_W, e.worldH - ground))
        drawRect(Color(0xFF3C4A62), Offset(0f, ground), Size(Tuning.WORLD_W, 4f))
        var sxp = -(e.time % 24)
        while (sxp < Tuning.WORLD_W) {
            drawLine(Color(0xFF1E2636), Offset(sxp.toFloat(), ground + 8f), Offset(sxp + 10f, e.worldH), strokeWidth = 2f)
            sxp += 24
        }
        if (e.groundFlash > 0) {
            val a = e.groundFlash / 14f
            drawRect(Color(0xFFFF1744).copy(alpha = 0.45f * a), Offset(0f, ground - 10f), Size(Tuning.WORLD_W, e.worldH - ground + 10f))
        }
    }

    // ------------------------------------------------------------------ turrets

    private fun DrawScope.drawTurrets(e: BaseDefenderEngine, ground: Float) {
        val pivotY = e.pivotY
        for (i in e.turrets.indices) {
            val t = e.turrets[i]
            val role = t.role
            // Bunker.
            drawRoundRect(Color(0xFF263041), Offset(t.x - 25f, ground - 16f), Size(50f, 34f), CornerRadius(7f))
            drawRoundRect(Color(0xFF3B4A63), Offset(t.x - 25f, ground - 16f), Size(50f, 10f), CornerRadius(7f))
            drawRoundRect(role.color, Offset(t.x - 14f, ground - 2f), Size(28f, 4f), CornerRadius(2f))
            // Barrel, rotated about the pivot, pushed back by recoil.
            rotate(degrees = t.angle * (180f / PI.toFloat()), pivot = Offset(t.x, pivotY)) {
                drawRoundRect(
                    lerp(role.color, Color.Black, 0.35f),
                    Offset(t.x - role.barrelWidth * 0.5f - 1.5f, pivotY - role.barrel + t.recoil),
                    Size(role.barrelWidth + 3f, role.barrel), CornerRadius(2.5f)
                )
                drawRoundRect(
                    role.color,
                    Offset(t.x - role.barrelWidth * 0.5f, pivotY - role.barrel + t.recoil),
                    Size(role.barrelWidth, role.barrel * 0.82f), CornerRadius(2.5f)
                )
                if (t.flash > 0) {
                    val f = t.flash / 5f
                    drawCircle(Color(0xFFFFF59D).copy(alpha = 0.9f * f), 7f * f + 3f, Offset(t.x, pivotY - role.barrel - 2f))
                    drawCircle(Color.White.copy(alpha = f), 3f, Offset(t.x, pivotY - role.barrel - 2f))
                }
            }
            // Dome over the barrel root.
            drawCircle(Color(0xFF455872), 13f, Offset(t.x, pivotY))
            drawCircle(role.color, 6f, Offset(t.x, pivotY))
            drawCircle(Color.White.copy(alpha = 0.25f), 3f, Offset(t.x - 3f, pivotY - 3f))
            if (e.shieldTicks > 0) drawShield(e, t.x, pivotY + 8f)
        }
    }

    private fun DrawScope.drawShield(e: BaseDefenderEngine, x: Float, y: Float) {
        val a = 0.35f + 0.2f * sin(e.time * 0.15f)
        val low = if (e.shieldTicks < 90 && (e.time / 6) % 2 == 0) 0.4f else 1f
        drawCircle(Color(0xFF69F0AE).copy(alpha = a * 0.25f * low), 38f, Offset(x, y))
        drawCircle(Color(0xFF69F0AE).copy(alpha = a * low), 38f, Offset(x, y), style = Stroke(2.5f))
    }

    // ------------------------------------------------------------------ enemies, bullets, pickups

    private fun DrawScope.drawEnemies(e: BaseDefenderEngine) {
        for (i in e.enemies.indices) {
            val en = e.enemies[i]
            val ratio = (en.hp / en.maxHp).coerceIn(0f, 1f)
            var body = lerp(en.type.color, Color(0xFF1A0A0A), (1f - ratio) * 0.5f)
            if (en.flash > 0) body = lerp(body, Color.White, 0.75f)
            if (en.burn > 0) drawCircle(Color(0xFFFF7043).copy(alpha = 0.3f + 0.15f * sin(e.time * 0.4f + i)), en.size * 0.8f, Offset(en.x, en.y))
            enemyArt(en.type.shape).draw(this, en, body, e.time, path)
            if (en.freeze > 0) {
                drawCircle(Color(0xFF80DEEA).copy(alpha = 0.35f), en.size * 0.62f, Offset(en.x, en.y))
                drawCircle(Color(0xFFE0F7FA).copy(alpha = 0.8f), en.size * 0.62f, Offset(en.x, en.y), style = Stroke(1.6f))
            }
            if (ratio < 0.999f || en.type.leakDamage > 1) {
                val w = en.size
                val top = en.y - en.size * 0.5f - 9f
                drawRoundRect(Color(0xAA000000), Offset(en.x - w * 0.5f, top), Size(w, 4f), CornerRadius(2f))
                drawRoundRect(lerp(Color(0xFFEF5350), Color(0xFF66BB6A), ratio), Offset(en.x - w * 0.5f, top), Size(w * ratio, 4f), CornerRadius(2f))
            }
        }
    }

    private fun DrawScope.drawBullets(e: BaseDefenderEngine) {
        for (i in e.bullets.indices) {
            val b = e.bullets[i]
            val c = b.weapon.color
            val tail = if (b.mega) 5f else 3f
            drawLine(c.copy(alpha = 0.45f), Offset(b.x - b.vx * tail, b.y - b.vy * tail), Offset(b.x, b.y), strokeWidth = b.size * 1.3f)
            drawCircle(c.copy(alpha = 0.3f), b.size * 1.9f, Offset(b.x, b.y))
            drawCircle(c, b.size * 0.9f, Offset(b.x, b.y))
            drawCircle(Color.White, b.size * 0.45f, Offset(b.x, b.y))
        }
    }

    private fun DrawScope.drawPickups(e: BaseDefenderEngine) {
        for (i in e.pickups.indices) {
            val p = e.pickups[i]
            val c = p.type.color
            val pulse = 0.5f + 0.5f * sin(e.time * 0.15f + p.phase)
            drawCircle(c.copy(alpha = 0.18f + 0.18f * pulse), 20f + 3f * pulse, Offset(p.x, p.y))
            drawRoundRect(c, Offset(p.x - 11f, p.y - 11f), Size(22f, 22f), CornerRadius(6f))
            drawRoundRect(Color.White, Offset(p.x - 11f, p.y - 11f), Size(22f, 22f), CornerRadius(6f), style = Stroke(1.8f))
            textPaint.color = android.graphics.Color.WHITE
            textPaint.textSize = 14f
            drawIntoCanvas { it.nativeCanvas.drawText(p.type.letter, p.x, p.y + 5f, textPaint) }
        }
    }

    // ------------------------------------------------------------------ effects

    private fun DrawScope.drawParticles(e: BaseDefenderEngine) {
        val ps = e.particles
        for (i in 0 until ps.count) {
            val a = (ps.life[i].toFloat() / ps.maxLife[i]).coerceIn(0f, 1f)
            val c = Color(ps.color[i])
            if (ps.ring[i]) {
                val r = ps.size[i] * (1.1f - a)
                drawCircle(c.copy(alpha = a * 0.8f), r, Offset(ps.x[i], ps.y[i]), style = Stroke(2f))
            } else {
                drawCircle(c.copy(alpha = a), ps.size[i] * (0.4f + 0.6f * a), Offset(ps.x[i], ps.y[i]))
            }
        }
    }

    private fun DrawScope.drawSlowOverlay(e: BaseDefenderEngine) {
        val a = 0.08f + 0.04f * sin(e.time * 0.15f)
        drawRect(Color(0xFFBA68C8).copy(alpha = a), Offset.Zero, Size(Tuning.WORLD_W, e.worldH))
    }

    private fun DrawScope.drawCrosshair(e: BaseDefenderEngine) {
        val a = (e.aimTicks / 30f).coerceIn(0f, 1f)
        val c = Color.White.copy(alpha = 0.6f * a)
        drawCircle(c, 14f, Offset(e.aimX, e.aimY), style = Stroke(1.6f))
        drawLine(c, Offset(e.aimX - 20f, e.aimY), Offset(e.aimX - 8f, e.aimY), strokeWidth = 1.6f)
        drawLine(c, Offset(e.aimX + 8f, e.aimY), Offset(e.aimX + 20f, e.aimY), strokeWidth = 1.6f)
        drawLine(c, Offset(e.aimX, e.aimY - 20f), Offset(e.aimX, e.aimY - 8f), strokeWidth = 1.6f)
        drawLine(c, Offset(e.aimX, e.aimY + 8f), Offset(e.aimX, e.aimY + 20f), strokeWidth = 1.6f)
    }

    private fun DrawScope.drawTexts(e: BaseDefenderEngine) {
        val ts = e.texts
        for (i in 0 until ts.count) {
            val s = ts.text[i] ?: continue
            val a = (ts.life[i].toFloat() / ts.maxLife[i]).coerceIn(0f, 1f)
            val argb = ts.color[i]
            textPaint.color = (argb and 0x00FFFFFF) or ((a * 255f).toInt() shl 24)
            textPaint.textSize = if (ts.big[i]) 17f else 12f
            drawIntoCanvas { it.nativeCanvas.drawText(s, ts.x[i], ts.y[i], textPaint) }
        }
    }

    private fun DrawScope.drawBanner(e: BaseDefenderEngine) {
        if (e.bannerTicks <= 0 || e.banner.isEmpty()) return
        val a = (e.bannerTicks / 30f).coerceIn(0f, 1f)
        textPaint.color = android.graphics.Color.argb((a * 255f).toInt(), 255, 213, 79)
        textPaint.textSize = 22f
        drawIntoCanvas { it.nativeCanvas.drawText(e.banner, Tuning.WORLD_W / 2f, e.worldH * 0.32f, textPaint) }
    }
}
