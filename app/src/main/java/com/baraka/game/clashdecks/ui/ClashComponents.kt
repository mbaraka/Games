package com.baraka.game.clashdecks.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.clashdecks.VfxEmission
import com.baraka.game.clashdecks.model.Card
import com.baraka.game.clashdecks.model.DuelVfx
import com.baraka.game.clashdecks.model.Duelist
import com.baraka.game.clashdecks.model.StatusEffect
import com.baraka.game.clashdecks.model.VfxSide
import com.baraka.game.clashdecks.model.showsAmount
import com.baraka.game.knightsurvivor.model.KnightDesign
import com.baraka.game.knightsurvivor.model.VectorIcon
import com.baraka.game.knightsurvivor.render.drawKnightIcon
import com.baraka.game.knightsurvivor.render.drawVectorIcon
import com.baraka.game.ui.theme.GameColors
import kotlinx.coroutines.delay

internal val PanelBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
internal val ScreenBg: Color
    @Composable get() = MaterialTheme.colorScheme.background

@Composable
internal fun AvatarCanvas(design: KnightDesign, frozen: Boolean, flash: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (frozen) Color(0xFF16303D) else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            drawKnightIcon(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension * 0.44f,
                design = design,
                bodyColor = if (frozen) Color(0xFF80D8FF) else Color(0xFFCFD8DC)
            )
        }
        if (flash != Color.Transparent) {
            Box(modifier = Modifier.size(40.dp).background(flash))
        }
    }
}

@Composable
internal fun IconSwatch(icon: VectorIcon, tint: Color, size: Dp = 26.dp) {
    Canvas(modifier = Modifier.size(size)) {
        drawVectorIcon(
            icon = icon,
            center = Offset(this.size.width / 2f, this.size.height / 2f),
            radius = this.size.minDimension * 0.42f,
            color = tint
        )
    }
}

@Composable
internal fun HpBar(current: Int, max: Int, modifier: Modifier = Modifier) {
    val fraction = if (max <= 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(GameColors.Danger.copy(alpha = 0.22f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(18.dp)
                .background(GameColors.Danger)
        )
        Text(
            text = "$current / $max",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
internal fun EnergyPips(current: Int, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Energy", color = GameColors.TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        val shown = maxOf(max, current).coerceAtMost(8)
        repeat(shown) { i ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (i < current) GameColors.Accent else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("$current", color = GameColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun StatusRow(statuses: List<StatusEffect>) {
    if (statuses.isEmpty()) return
    // Scrollable, not just a Row: with the name spelled out each chip is wider, and 3+
    // statuses stacked on one duelist used to risk running off the edge of the panel.
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEach { st ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Color(st.kind.colorArgb).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                IconSwatch(st.kind.icon, Color(st.kind.colorArgb), size = 16.dp)
                Spacer(Modifier.width(3.dp))
                // Spelled out, not just the icon: Empower/Vulnerable are both small stars and
                // read identically at this size, so the icon alone wasn't enough to tell a
                // buff from a debuff you need to play around.
                val amount = if (st.kind.showsAmount) " ${st.amount}" else ""
                Text(
                    "${st.kind.shortLabel}$amount · ${st.turnsRemaining}",
                    color = Color(st.kind.colorArgb),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** A one-shot icon that pops out from behind the avatar and dissolves — the on-character echo of a status/shield/heal event, so the effect reads at a glance instead of only through the floating word and the status chip. */
private data class StatusBurst(val id: Long, val icon: VectorIcon, val color: Color, val big: Boolean = false)

private fun burstFor(e: VfxEmission): StatusBurst? = when (val v = e.vfx) {
    is DuelVfx.StatusApplied -> StatusBurst(e.id, v.kind.icon, Color(v.kind.colorArgb))
    is DuelVfx.Warded -> StatusBurst(e.id, VectorIcon.Shield, Color(0xFF4DD0E1))
    is DuelVfx.Cleansed -> StatusBurst(e.id, VectorIcon.Star(points = 7, innerRadiusRatio = 0.42f), Color(0xFF81C784))
    is DuelVfx.ReflectReady -> StatusBurst(e.id, VectorIcon.Shield, Color(0xFFB39DDB))
    is DuelVfx.Frozen -> StatusBurst(e.id, VectorIcon.Star(points = 6, innerRadiusRatio = 0.3f), Color(0xFF4FC3F7), big = true)
    is DuelVfx.BlockShattered -> StatusBurst(e.id, VectorIcon.Shield, Color(0xFFECEFF1), big = true)
    is DuelVfx.BlockGain -> StatusBurst(e.id, VectorIcon.Shield, Color(0xFF64B5F6))
    is DuelVfx.Heal -> if (v.amount > 0) StatusBurst(e.id, VectorIcon.Heart, Color(0xFF66BB6A)) else null
    else -> null
}

@Composable
private fun StatusBurstIcon(burst: StatusBurst, onDone: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 560, easing = LinearOutSlowInEasing),
        label = "statusBurst"
    )
    LaunchedEffect(burst.id) {
        delay(580)
        onDone()
    }
    Canvas(
        modifier = Modifier
            .size(if (burst.big) 64.dp else 48.dp)
            .scale(0.5f + 1.3f * progress)
            .alpha((1f - progress).coerceIn(0f, 1f))
    ) {
        drawVectorIcon(
            icon = burst.icon,
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension * 0.46f,
            color = burst.color
        )
    }
}

@Composable
internal fun DuelistPanel(
    duelist: Duelist,
    label: String,
    side: VfxSide,
    effects: List<VfxEmission>
) {
    val latest = effects.lastOrNull { it.vfx.side == side }
    val hurt = latest?.vfx is DuelVfx.Hit || latest?.vfx is DuelVfx.Frozen
    val healed = latest?.vfx is DuelVfx.Heal || latest?.vfx is DuelVfx.BlockGain

    val flash by animateColorAsState(
        targetValue = when {
            hurt -> GameColors.Danger.copy(alpha = 0.28f)
            healed -> GameColors.Success.copy(alpha = 0.22f)
            else -> Color.Transparent
        },
        animationSpec = tween(if (latest == null) 400 else 120),
        label = "panelFlash"
    )
    // A tiny one-shot horizontal nudge keyed to the latest hurt event.
    val jitter by animateFloatAsState(
        targetValue = if (hurt) 1f else 0f,
        animationSpec = tween(90),
        label = "jitter"
    )

    val bursts = remember { mutableStateListOf<StatusBurst>() }
    LaunchedEffect(effects) {
        effects.forEach { e -> if (e.vfx.side == side) burstFor(e)?.let { bursts.add(it) } }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = if (hurt) (jitter * 5f - 2.5f).dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                AvatarCanvas(
                    design = if (duelist.isCpu) KnightDesign.BERSERKER else KnightDesign.SWORN_BLADE,
                    frozen = duelist.hasStatus(com.baraka.game.clashdecks.model.StatusKind.FREEZE),
                    flash = flash
                )
                bursts.forEach { b -> key(b.id) { StatusBurstIcon(b) { bursts.remove(b) } } }
            }
            Spacer(Modifier.width(8.dp))
            Text(label, color = GameColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            if (duelist.block > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E3A4D))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconSwatch(VectorIcon.Shield, Color(0xFF64B5F6), size = 14.dp)
                    Spacer(Modifier.width(3.dp))
                    Text("${duelist.block}", color = Color(0xFF90CAF9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("Hand ${duelist.hand.size}", color = GameColors.TextMuted, fontSize = 11.sp)
        }
        HpBar(duelist.hp, duelist.maxHp)
        StatusRow(duelist.statuses)
    }
}

@Composable
internal fun CardView(
    card: Card,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stripe = Color(card.type.colorArgb)
    Column(
        modifier = modifier
            .width(120.dp)
            .height(164.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (enabled) stripe.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GameColors.Accent),
                contentAlignment = Alignment.Center
            ) {
                Text("${card.cost}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                card.name,
                color = if (enabled) GameColors.TextPrimary else GameColors.TextDisabled,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconSwatch(card.icon, if (enabled) stripe else GameColors.TextDisabled, size = 34.dp)
        }
        Text(
            card.summary,
            color = if (enabled) GameColors.TextSecondary else GameColors.TextDisabled,
            fontSize = 9.sp,
            maxLines = 5
        )
    }
}

@Composable
internal fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.Black else GameColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) GameColors.Accent else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
internal fun PillButton(text: String, onClick: () -> Unit, primary: Boolean = true, enabled: Boolean = true) {
    Text(
        text = text,
        color = if (!enabled) GameColors.TextDisabled else if (primary) Color.Black else GameColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (!enabled) MaterialTheme.colorScheme.surface
                else if (primary) GameColors.Accent else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    )
}
