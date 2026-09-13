package com.baraka.game.clashdecks.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.clashdecks.VfxEmission
import com.baraka.game.clashdecks.model.DuelVfx
import com.baraka.game.clashdecks.model.VfxSide
import com.baraka.game.ui.theme.GameColors
import kotlinx.coroutines.delay

/**
 * Full-screen overlay that plays the cosmetic beats coming out of the ViewModel: floating
 * combat numbers that rise off each combatant, and a short screen tint for the big hits
 * (freeze = blue, shatter = white, heavy hit = red). Owns its own transient list; each
 * batch of [effects] carries fresh ids so nothing plays twice.
 */
@Composable
fun DuelVfxLayer(effects: List<VfxEmission>) {
    val floaters = remember { mutableStateListOf<Floater>() }
    var tint by remember { mutableStateOf(Color.Transparent) }
    var tintKey by remember { mutableStateOf(0) }

    LaunchedEffect(effects) {
        effects.forEach { emission ->
            floaterFor(emission)?.let(floaters::add)
            when (val v = emission.vfx) {
                is DuelVfx.Frozen -> { tint = Color(0x554FC3F7); tintKey++ }
                is DuelVfx.BlockShattered -> { tint = Color(0x55FFFFFF); tintKey++ }
                is DuelVfx.Hit -> if (v.toHp >= 6) { tint = Color(0x33E53935); tintKey++ }
                else -> {}
            }
        }
    }

    LaunchedEffect(tintKey) {
        if (tintKey == 0) return@LaunchedEffect
        delay(260)
        tint = Color.Transparent
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (tint != Color.Transparent) {
            Box(modifier = Modifier.fillMaxSize().background(tint))
        }
        floaters.forEach { f ->
            key(f.id) { FloaterText(f) { floaters.remove(f) } }
        }
    }
}

@Composable
private fun FloaterText(f: Floater, onDone: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "floater"
    )
    LaunchedEffect(f.id) {
        delay(950)
        onDone()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (f.side == VfxSide.CPU) Alignment.TopCenter else Alignment.BottomCenter
    ) {
        Text(
            text = f.text,
            color = f.color,
            fontSize = f.sizeSp.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(y = (if (f.side == VfxSide.CPU) 96.dp else (-150).dp) + (-46 * progress).dp)
                .alpha((1f - progress).coerceIn(0f, 1f))
        )
    }
}

private data class Floater(
    val id: Long,
    val side: VfxSide,
    val text: String,
    val color: Color,
    val sizeSp: Int
)

private fun floaterFor(e: VfxEmission): Floater? = when (val v = e.vfx) {
    is DuelVfx.Hit -> {
        val text = if (v.blocked > 0) "-${v.toHp}  (${v.blocked} blocked)" else "-${v.toHp}"
        Floater(e.id, v.side, text, Color(0xFFE53935), if (v.toHp >= 6) 26 else 20)
    }
    is DuelVfx.Heal -> if (v.amount > 0) Floater(e.id, v.side, "+${v.amount}", Color(0xFF66BB6A), 20) else null
    is DuelVfx.BlockGain -> Floater(e.id, v.side, "+${v.amount} block", Color(0xFF64B5F6), 18)
    is DuelVfx.BlockShattered -> Floater(e.id, v.side, "SHATTER", Color(0xFFECEFF1), 22)
    is DuelVfx.StatusApplied -> Floater(e.id, v.side, v.kind.displayName.uppercase(), Color(v.kind.colorArgb), 18)
    is DuelVfx.Frozen -> Floater(e.id, v.side, "FROZEN", Color(0xFF4FC3F7), 24)
    is DuelVfx.CardPlayed -> null
}
