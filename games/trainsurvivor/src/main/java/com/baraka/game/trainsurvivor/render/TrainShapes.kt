package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.baraka.game.trainsurvivor.model.TrainShape
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.min

// Train bodies. To add a shape: add an entry to TrainShape (sizes, price, stat tweak), write a TrainShapeArt
// here that draws the locomotive, wagons and windows with the current livery (`palette`), and register it below.
// The steam locomotive's art lives in TrainRenderer.kt.

internal fun registerTrainShapes() {
    TrainShapeArtRegistry.register(TrainShape.DIESEL, DieselShapeArt)
    TrainShapeArtRegistry.register(TrainShape.BULLET, BulletShapeArt)
}

private val rim = Path()

/** A boxy diesel hauler: long hood with louvres, a tall cab and bogies. Sturdier (+20% locomotive health). */
private object DieselShapeArt : TrainShapeArt {
    override fun paintLoco(s: DrawScope, g: TrainGame, x0: Float) {
        with(s) {
            val w = g.shape.locoW
            box(Color(0xFF1A1A1F), x0, 480f, w, 8f)
            rbox(Color(palette.boiler), x0 + 86f, 418f, w - 86f, 62f, 8f)
            box(Color(palette.boilerHi), x0 + 90f, 424f, w - 94f, 10f)
            box(Color(palette.trim), x0 + 86f, 456f, w - 86f, 5f)
            for (k in 0..2) {
                box(Color(0xFF222831), x0 + 102f + k * 32f, 430f, 24f, 20f)
                for (l in 0..3) box(Color(0xFF3A4350), x0 + 104f + k * 32f, 433f + l * 4.5f, 20f, 1.6f)
            }
            rbox(Color(palette.cab), x0, 390f, 94f, 90f, 6f)
            box(Color(palette.cabDark), x0 - 4f, 382f, 102f, 10f)
            box(Color(0xFF37474F), x0 + 40f, 372f, 12f, 10f) // horn
            // Nose, bumper and plough.
            box(Color(0xFF3D4048), x0 + w - 6f, 462f, 10f, 32f)
            rim.rewind()
            rim.moveTo(x0 + w, 478f); rim.lineTo(x0 + w + g.shape.nose, 498f); rim.lineTo(x0 + w, 498f); rim.close()
            drawPath(rim, Color(0xFF3D4048))
            drawCircle(Color(0xFFFFF3B0), 6f, Offset(x0 + w + 2f, 440f))
            drawCircle(Color(0xFF3D4048), 7f, Offset(x0 + w + 2f, 440f), style = Stroke(2f))
            drawArmorPlates(x0, w, g.armorLevel)
            for (wx in floatArrayOf(34f, 66f, w - 70f, w - 38f)) drawWheel(x0 + wx, 485f, 16f, g.wheelAngle)
            for (k in 0 until min(g.spikeLevel, 5)) {
                val sy = 466f + k * 5f
                rim.rewind()
                rim.moveTo(x0 + w + g.shape.nose - 6f, sy); rim.lineTo(x0 + w + g.shape.nose + 12f, sy + 3f); rim.lineTo(x0 + w + g.shape.nose - 6f, sy + 6f); rim.close()
                drawPath(rim, Color(0xFFD7DBE2))
            }
        }
    }

    override fun paintCar(s: DrawScope, g: TrainGame, x0: Float, index: Int, slot: Int) {
        with(s) { drawCar(g, x0, index, slot, corner = 4f) }
    }

    override fun paintWindows(s: DrawScope, g: TrainGame, color: Color) {
        with(s) {
            box(color, g.locoLeft + 12f, 400f, 28f, 26f)
            box(color, g.locoLeft + 48f, 400f, 30f, 26f)
            for (i in 1 until g.slotCount) {
                if (!g.carAlive(i)) continue
                val x = g.carLeft(i)
                for (k in 0..2) box(color, x + 20f + k * 38f, 440f, 22f, 14f)
            }
        }
    }
}

/** A streamlined bullet train: long swept nose, skirted wheels and rounded wagons. 15% faster. */
private object BulletShapeArt : TrainShapeArt {
    override fun paintLoco(s: DrawScope, g: TrainGame, x0: Float) {
        with(s) {
            val w = g.shape.locoW
            rim.rewind()
            rim.moveTo(x0, 484f); rim.lineTo(x0, 412f); rim.lineTo(x0 + 70f, 392f); rim.lineTo(x0 + 150f, 398f)
            rim.lineTo(x0 + w - 18f, 436f); rim.lineTo(x0 + w + 4f, 470f); rim.lineTo(x0 + w + 4f, 484f); rim.close()
            drawPath(rim, Color(palette.boiler))
            rim.rewind()
            rim.moveTo(x0 + 4f, 414f); rim.lineTo(x0 + 70f, 396f); rim.lineTo(x0 + 148f, 402f); rim.lineTo(x0 + w - 24f, 436f)
            rim.lineTo(x0 + w - 40f, 436f); rim.lineTo(x0 + 140f, 410f); rim.lineTo(x0 + 70f, 404f); rim.lineTo(x0 + 4f, 424f); rim.close()
            drawPath(rim, Color(palette.boilerHi))
            box(Color(palette.trim), x0, 464f, w + 4f, 4f)
            box(Color(palette.cabDark), x0 + 8f, 474f, w - 14f, 12f) // wheel skirt
            // Pantograph on the roof.
            drawLine(Color(0xFF90A4AE), Offset(x0 + 40f, 394f), Offset(x0 + 56f, 378f), 2f)
            drawLine(Color(0xFF90A4AE), Offset(x0 + 56f, 378f), Offset(x0 + 72f, 394f), 2f)
            box(Color(0xFF90A4AE), x0 + 48f, 376f, 18f, 3f)
            drawCircle(Color(0xFFFFF3B0), 6f, Offset(x0 + w - 4f, 462f))
            drawArmorPlates(x0, w, g.armorLevel)
            for (wx in floatArrayOf(40f, 80f, 120f, 160f, 200f)) drawWheel(x0 + wx, 490f, 11f, g.wheelAngle * 1.2f)
            for (k in 0 until min(g.spikeLevel, 5)) {
                val sy = 470f + k * 3.5f
                rim.rewind()
                rim.moveTo(x0 + w + 2f, sy); rim.lineTo(x0 + w + 18f, sy + 2f); rim.lineTo(x0 + w + 2f, sy + 4f); rim.close()
                drawPath(rim, Color(0xFFD7DBE2))
            }
        }
    }

    override fun paintCar(s: DrawScope, g: TrainGame, x0: Float, index: Int, slot: Int) {
        with(s) { drawCar(g, x0, index, slot, corner = 22f) }
    }

    override fun paintWindows(s: DrawScope, g: TrainGame, color: Color) {
        with(s) {
            rim.rewind()
            val x0 = g.locoLeft
            rim.moveTo(x0 + 60f, 408f); rim.lineTo(x0 + 140f, 410f); rim.lineTo(x0 + g.shape.locoW - 44f, 438f); rim.lineTo(x0 + 60f, 438f); rim.close()
            drawPath(rim, color)
            for (i in 1 until g.slotCount) {
                if (!g.carAlive(i)) continue
                rbox(color, g.carLeft(i) + 14f, 438f, g.shape.carW - 28f, 16f, 8f)
            }
        }
    }
}

