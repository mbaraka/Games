package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.baraka.game.trainsurvivor.model.Hair
import com.baraka.game.trainsurvivor.model.Hitchhiker
import com.baraka.game.trainsurvivor.systems.Passenger
import com.baraka.game.trainsurvivor.systems.RAIL_Y
import com.baraka.game.trainsurvivor.systems.TrainGame
import com.baraka.game.trainsurvivor.systems.perchX
import com.baraka.game.trainsurvivor.systems.perchY
import kotlin.math.PI
import kotlin.math.sin

/**
 * Drawing for hitchhikers: the stranger waving by the track, and the passenger riding the roof (hopping on, shooting,
 * chatting, hopping off). Male and female characters share one body; [looks] adds each person's own touches (apron,
 * toque, pigeon...) and [props] their weapon. Add a passenger by adding an entry to each map.
 */
private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

private val WOOD = Color(0xFF6D4C41)
private val DARK = Color(0xFF263238)
private val STEEL = Color(0xFF90A4AE)

/** Per-person details in avatar space (feet at the origin, facing +x, head centre at (1, -42)). */
private val looks: Map<Hitchhiker, DrawScope.(Float) -> Unit> = mapOf(
    Hitchhiker.GERTRUDE to { _ ->
        box(Color.White, -7f, -30f, 14f, 13f) // apron
        box(Color(0xFFE1BEE7), -3f, -26f, 6f, 5f)
        drawCircle(DARK, 3.4f, Offset(4f, -43f), style = Stroke(1f)) // glasses
    },
    Hitchhiker.DAVE to { _ ->
        rbox(Color(0xFF8D6E63), -7f, -30f, 14f, 12f, 2f) // fishing vest
        box(Color(0xFFFFB74D), -5f, -27f, 4f, 4f)
        drawOval(Color(0xFF4FC3F7), Offset(-6f, -60f), Size(10f, 5f)) // a lure on the hat
    },
    Hitchhiker.MARCEL to { _ ->
        for (k in 0 until 4) box(Color(0xFF212121), -8f, -34f + k * 5f, 16f, 2f) // stripes
        box(Color(0xFFD81B60), 3f, -39f, 4f, 1.6f)
        drawCircle(Color(0xFF111111), 0.9f, Offset(5f, -45f))
    },
    Hitchhiker.SPARKY to { _ ->
        box(Color(0xFF3949AB), -5f, -33f, 3f, 12f) // dungaree straps
        box(Color(0xFF3949AB), 2f, -33f, 3f, 12f)
        drawCircle(Color(0xFF80DEEA), 3f, Offset(-2f, -51f)) // goggles pushed up
    },
    Hitchhiker.PIERRE to { _ ->
        rbox(Color.White, -6f, -67f, 14f, 14f, 6f) // chef's toque
        box(Color.White, -7f, -55f, 16f, 4f)
        box(Color(0xFF212121), 2f, -39f, 7f, 2f) // moustache
        box(Color.White, -7f, -30f, 14f, 14f)
    },
    Hitchhiker.BEA to { _ ->
        drawOval(Color(0xFF9E9E9E), Offset(4f, -37f), Size(11f, 7f)) // a pigeon on her shoulder
        drawCircle(Color(0xFF9E9E9E), 2.6f, Offset(15f, -38f))
        box(Color(0xFFFFB300), 17f, -38f, 3f, 1.4f)
    },
    Hitchhiker.CONFETTI to { _ ->
        box(Color(0xFFFFEB3B), -6f, -36f, 5f, 5f) // bow tie
        box(Color(0xFFFFEB3B), 1f, -36f, 5f, 5f)
        drawCircle(Color(0xFFE53935), 1.6f, Offset(-0.5f, -33.5f))
        drawCircle(Color(0xFFFF4081), 1.4f, Offset(-4f, -22f))
        drawCircle(Color(0xFF40C4FF), 1.4f, Offset(3f, -26f))
    },
    Hitchhiker.BRENDA to { _ ->
        for (k in 0 until 3) drawCircle(Color(0xFFFFC107), 1.7f, Offset(-4f + k * 3.6f, -28f)) // medals
        box(Color(0xFFFFC107), -8f, -34f, 4f, 2f) // epaulettes
        box(Color(0xFFFFC107), 5f, -34f, 4f, 2f)
    }
)

/** The weapon in the passenger's hand, drawn from the shoulder pointing along +x; [recoil] pulls it back. */
private val props: Map<Hitchhiker, DrawScope.(Float) -> Unit> = mapOf(
    Hitchhiker.GERTRUDE to { r ->
        val k = -r * 5f
        box(WOOD, 8f + k, -2f, 16f, 4f)
        drawCircle(Color(0xFF455A64), 11f, Offset(29f + k, 0f))
        drawCircle(Color(0xFF78909C), 8.5f, Offset(29f + k, 0f))
        drawCircle(Color(0xFFCFD8DC), 2.5f, Offset(26f + k, -3f))
    },
    Hitchhiker.DAVE to { r ->
        val k = -r * 5f
        box(DARK, 6f + k, -3f, 14f, 7f)
        box(STEEL, 18f + k, -2f, 34f, 4f)
        drawLine(Color(0xFFECEFF1), Offset(50f + k, 0f), Offset(62f + k, 0f), 3f)
        drawLine(Color(0xFFECEFF1), Offset(56f + k, 0f), Offset(51f + k, -5f), 2f)
        drawLine(Color(0xFFECEFF1), Offset(56f + k, 0f), Offset(51f + k, 5f), 2f)
        drawCircle(Color(0xFFFFB74D), 6f, Offset(10f, 8f), style = Stroke(2f)) // line spool
    },
    Hitchhiker.MARCEL to { r ->
        val a = 0.35f + 0.3f * sin(r * 6f + 1f)
        drawRect(Color(0xFF90CAF9).copy(alpha = a + 0.2f * r), Offset(14f, -18f), Size(26f, 32f), style = Stroke(1.6f))
        drawLine(Color(0xFF90CAF9).copy(alpha = a), Offset(14f, -18f), Offset(40f, 14f), 1f)
        drawLine(Color(0xFF90CAF9).copy(alpha = a), Offset(40f, -18f), Offset(14f, 14f), 1f)
    },
    Hitchhiker.SPARKY to { r ->
        val k = -r * 3f
        box(Color(0xFFE53935), 2f + k, -10f, 12f, 12f)
        box(Color(0xFF43A047), 12f + k, -5f, 24f, 9f)
        box(Color(0xFF9E9E9E), 34f + k, -4f, 14f, 7f)
        if (r > 0.05f) {
            for (i in 0 until 3) drawLine(Color(0xAAE1F5FE), Offset(50f, -4f + i * 4f), Offset(50f + 24f * r + i * 4f, -4f + i * 5f), 2f)
        }
    },
    Hitchhiker.PIERRE to { r ->
        val k = -r * 4f
        rbox(Color(0xFFB0BEC5), 4f + k, -13f, 22f, 20f, 4f)
        box(Color(0xFF455A64), 2f + k, -13f, 26f, 3f)
        box(Color(0xFF37474F), 26f + k, -4f, 22f, 8f)
        drawCircle(Color.White.copy(alpha = 0.4f), 4f + 3f * r, Offset(12f, -20f - 4f * r))
        drawCircle(Color.White.copy(alpha = 0.3f), 3f, Offset(20f, -26f))
    },
    Hitchhiker.BEA to { r ->
        rbox(WOOD, 4f, -14f, 22f, 18f, 3f)
        for (k in 0 until 4) drawLine(DARK, Offset(8f + k * 5f, -14f), Offset(8f + k * 5f, 4f), 1.2f)
        drawCircle(Color.White, 3f, Offset(10f, -6f - 3f * r))
        drawCircle(Color(0xFFB0BEC5), 3f, Offset(20f, -4f))
        drawCircle(Color(0xFFFFB300), 1f, Offset(13f, -6f - 3f * r))
    },
    Hitchhiker.CONFETTI to { r ->
        val k = -r * 4f
        path.rewind()
        path.moveTo(6f + k, -5f); path.lineTo(40f + k, -15f); path.lineTo(40f + k, 15f); path.lineTo(6f + k, 5f); path.close()
        drawPath(path, Color(0xFFE91E63))
        box(Color(0xFFFFEB3B), 18f + k, -9f, 5f, 18f)
        box(Color(0xFF40C4FF), 30f + k, -12f, 5f, 24f)
        drawCircle(Color(0xFFFFF176), 3f + 2f * r, Offset(42f + k, 0f))
    },
    Hitchhiker.BRENDA to { r ->
        val k = -r * 5f
        box(Color(0xFF556B2F), 4f + k, -8f, 28f, 16f)
        box(DARK, 30f + k, -10f, 8f, 20f)
        path.rewind()
        path.moveTo(2f, -18f); path.lineTo(16f, -26f); path.lineTo(16f, -10f); path.close() // megaphone
        drawPath(path, Color(0xFFBDBDBD))
    }
)

/** Long hair, bun or pigtails behind the head, and a skirt: the look of a female character, on top of the shared body. */
private fun DrawScope.drawFemaleTouches(who: Hitchhiker) {
    val hair = Color(who.look.hair)
    when (who.hair) {
        Hair.LONG -> rbox(hair, -9f, -49f, 9f, 22f, 3f)
        Hair.BUN -> drawCircle(hair, 4.5f, Offset(-4f, -53f))
        Hair.PIGTAILS -> {
            rbox(hair, -10f, -47f, 4f, 11f, 2f)
            rbox(hair, 9f, -47f, 4f, 11f, 2f)
            drawCircle(Color(0xFFE91E63), 2f, Offset(-8f, -48f))
            drawCircle(Color(0xFFE91E63), 2f, Offset(11f, -48f))
        }
        Hair.SHORT -> {}
    }
    if (who.hair != Hair.PIGTAILS) {
        // a skirt over the legs, in a darker shade of the shirt
        val shirt = Color(who.look.shirt)
        path.rewind()
        path.moveTo(-8f, -21f); path.lineTo(8f, -21f); path.lineTo(12f, -8f); path.lineTo(-12f, -8f); path.close()
        drawPath(path, Color(shirt.red * 0.75f, shirt.green * 0.75f, shirt.blue * 0.75f, 1f))
    }
    box(Color(0xFFD81B60), 3f, -40f, 4f, 1.5f) // lipstick
    drawLine(DARK, Offset(4f, -46f), Offset(7f, -47.5f), 1f) // lash
}

/** One person: shared body, female touches if any, their own details. */
private fun DrawScope.drawPerson(who: Hitchhiker, time: Float) {
    drawAvatarBody(who.look)
    if (who.female) drawFemaleTouches(who)
    looks[who]?.invoke(this, time)
}

// ---------------------------------------------------------------- the stranger waiting by the track

internal fun DrawScope.drawHitcher(g: TrainGame) {
    val h = g.hitcher ?: return
    val who = h.who
    val look = who.look
    val x = h.x
    val y = RAIL_Y + 30f
    val facing = -h.side
    val fade = (h.life / 1.5f).coerceIn(0f, 1f)
    val gradeColor = Color(h.grade.color)
    val pulse = 0.5f + 0.5f * sin(h.phase * 8f)
    drawCircle(gradeColor.copy(alpha = (0.14f + 0.14f * pulse) * fade), 54f, Offset(x, y - 40f))
    // a suitcase at their feet
    rbox(Color(0xFF795548), x - facing * 28f - 9f, y - 16f, 18f, 16f, 3f)
    box(Color(0xFF3E2723), x - facing * 28f - 3f, y - 20f, 6f, 4f)
    withTransform({ translate(x, y); scale(facing * look.width, look.height, Offset.Zero) }) {
        drawPerson(who, h.phase)
    }
    // the waving arm
    val wave = sin(h.phase * 10f) * 9f
    val skin = Color(look.skin)
    val sh = y - 30f * look.height
    drawLine(skin, Offset(x + facing * 5f, sh), Offset(x + facing * 16f, sh - 24f * look.height + wave), 4f)
    drawCircle(skin, 3.2f, Offset(x + facing * 16f, sh - 24f * look.height + wave))
    // time left, name and the weapon they carry
    drawArc(gradeColor.copy(alpha = fade), -90f, 360f * (h.life / 14f).coerceIn(0f, 1f), false, Offset(x - 36f, y - 120f), Size(72f, 72f), style = Stroke(4f))
    outlinedText("TAP!", x, y - 130f, 20f, 0xFFFFD54F.toInt(), fade)
    outlinedText(who.displayName, x, y - 150f, 13f, 0xFFFFFFFF.toInt(), fade)
    outlinedText(who.weaponName(h.grade), x, y - 164f, 11f, h.grade.color.toInt(), fade)
}

// ---------------------------------------------------------------- the passenger on the roof

internal fun DrawScope.drawPassenger(g: TrainGame) {
    val p = g.passenger ?: return
    val who = p.who
    val look = who.look
    val perchX = g.perchX()
    val perchY = g.perchY()
    val leaving = p.state == Passenger.LEAVING
    val boarding = p.state == Passenger.BOARDING
    var x = perchX
    var y = perchY
    var facing = p.facing
    if (boarding) {
        val t = (p.animT / Passenger.BOARD_TIME).coerceIn(0f, 1f)
        x = mix(p.fromX, perchX, t)
        y = mix(p.fromY, perchY, t) - 4f * t * (1f - t) * 150f
        facing = if (perchX >= p.fromX) 1 else -1
    } else if (leaving) {
        val t = (p.animT / Passenger.LEAVE_TIME).coerceIn(0f, 1f)
        val landX = (g.rearEnd - 80f).coerceAtLeast(60f)
        x = mix(perchX, landX, t)
        y = mix(perchY, RAIL_Y + 30f, t) - 4f * t * (1f - t) * 120f
        facing = -1
    }
    val gradeColor = Color(p.grade.color)
    val t = g.time

    // the seat: a small railed platform with a suitcase, so they read as riding the train
    if (!boarding && !leaving) {
        box(Color(0xFF455A64), perchX - 16f, perchY, 32f, 5f)
        box(STEEL, perchX - 16f, perchY - 10f, 2f, 10f)
        box(STEEL, perchX + 14f, perchY - 10f, 2f, 10f)
        rbox(Color(0xFF795548), perchX - 30f, perchY - 13f, 13f, 13f, 2f)
        if (p.grade.ordinal >= 2) {
            val pulse = 0.5f + 0.5f * sin(t * 5f)
            drawCircle(gradeColor.copy(alpha = (if (p.grade.ordinal == 3) 0.26f else 0.14f) * (0.6f + 0.4f * pulse)), 40f, Offset(x, y - 30f * look.height))
        }
    }

    withTransform({ translate(x, y); scale(facing * look.width, look.height, Offset.Zero) }) {
        drawPerson(who, t)
    }
    // wind: they ride into it, so streaks and hair trail back (towards -x on screen) whichever way they face
    if (!boarding && !leaving) {
        val hy = y - 42f * look.height
        for (k in 0 until 3) {
            val len = 12f + 6f * k + 3f * sin(t * 14f + k)
            drawLine(Color.White.copy(alpha = 0.32f), Offset(x - 9f, hy - 5f + k * 5f), Offset(x - 9f - len, hy - 5f + k * 5f), 1.4f)
        }
    }
    // the arm and the weapon (or a goodbye wave when leaving)
    val aimAngle = if (leaving) -1.1f + 0.35f * sin(t * 16f) else if (boarding) -0.15f else p.aim
    withTransform({
        translate(x, y - 30f * look.height)
        scale(facing.toFloat(), 1f, Offset.Zero)
        rotate(aimAngle * 180f / PI.toFloat(), Offset.Zero)
    }) {
        box(Color(look.skin), 0f, -2.5f, 14f, 5f)
        if (!leaving) props[who]?.invoke(this, p.recoil)
    }

    if (leaving && p.animT > Passenger.LEAVE_TIME * 0.8f) return

    // tag: name, weapon and a bar for the time left on board
    val top = y - 66f * look.height - 8f
    outlinedText(who.displayName, x, top - 14f, 12f, 0xFFFFFFFF.toInt())
    outlinedText(who.weaponName(p.grade), x, top - 2f, 11f, p.grade.color.toInt())
    if (!leaving) {
        box(Color.Black.copy(alpha = 0.6f), x - 20f, top + 3f, 40f, 4f)
        box(gradeColor, x - 20f, top + 3f, 40f * (p.left / p.total).coerceIn(0f, 1f), 4f)
    }

    // speech bubble
    val say = p.speech
    if (say != null) {
        val w = say.length * 6.2f + 18f
        val bx = (x - w / 2f).coerceIn(4f, g.worldW - w - 4f)
        val by = top - 50f
        val a = (p.speechT / 0.4f).coerceIn(0f, 1f)
        rbox(Color(0xFF10202C).copy(alpha = 0.82f * a), bx, by, w, 24f, 9f)
        box(Color(0xFF10202C).copy(alpha = 0.82f * a), x - 3f, by + 23f, 6f, 5f)
        outlinedText(say, bx + w / 2f, by + 17f, 13f, 0xFFFFFFFF.toInt(), a)
    }
}
