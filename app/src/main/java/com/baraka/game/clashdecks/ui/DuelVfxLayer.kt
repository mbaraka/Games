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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
 *
 * A single card can emit several vfx at once (Curse applies both Weaken and Vulnerable, a
 * lethal Shatter is a block-break plus a hit, etc.) — anchoring them all at the exact same
 * point made the result an unreadable pile-up, which was most of why effects were "hard to
 * track". [slotOf] staggers same-side floaters sideways by how many are already in flight
 * on that side, so simultaneous ones fan out instead of stacking.
 */
@Composable
fun DuelVfxLayer(effects: List<VfxEmission>) {
    val floaters = remember { mutableStateListOf<Floater>() }
    val liveCountBySide = remember { mutableStateMapOf<VfxSide, Int>() }
    var tint by remember { mutableStateOf(Color.Transparent) }
    var tintKey by remember { mutableStateOf(0) }

    fun release(floater: Floater) {
        floaters.remove(floater)
        liveCountBySide[floater.side] = ((liveCountBySide[floater.side] ?: 1) - 1).coerceAtLeast(0)
    }

    LaunchedEffect(effects) {
        effects.forEach { emission ->
            floaterFor(emission)?.let { base ->
                val slot = liveCountBySide.getOrDefault(base.side, 0)
                liveCountBySide[base.side] = slot + 1
                floaters.add(base.copy(slot = slot))
            }
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
            key(f.id) { FloaterText(f) { release(f) } }
        }
    }
}

@Composable
private fun FloaterText(f: Floater, onDone: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1050, easing = LinearEasing),
        label = "floater"
    )
    LaunchedEffect(f.id) {
        delay(1100)
        onDone()
    }
    // A quick pop-in (scale 1.3 -> 1.0 over the first ~15%) draws the eye before the slower rise/fade.
    val pop = (1.15f - 0.15f * (progress / 0.15f).coerceIn(0f, 1f)).coerceAtLeast(1f)
    val xOffset = ((f.slot % 3) - 1) * 58 // -58, 0, +58dp — fans out up to 3 concurrent floaters per side
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
                .offset(
                    x = xOffset.dp,
                    y = (if (f.side == VfxSide.CPU) 96.dp else (-150).dp) + (-50 * progress).dp
                )
                .scale(pop)
                .alpha((1f - progress).coerceIn(0f, 1f))
        )
    }
}

private data class Floater(
    val id: Long,
    val side: VfxSide,
    val text: String,
    val color: Color,
    val sizeSp: Int,
    val slot: Int = 0
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
    is DuelVfx.Cleansed -> Floater(e.id, v.side, "CLEANSED", Color(0xFF81C784), 18)
    is DuelVfx.ReflectReady -> Floater(e.id, v.side, "RIPOSTE READY", Color(0xFFB39DDB), 16)
    is DuelVfx.CardPlayed -> null
}
