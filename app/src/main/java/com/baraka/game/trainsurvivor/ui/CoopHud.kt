package com.baraka.game.trainsurvivor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.trainsurvivor.coop.CoopSession
import com.baraka.game.trainsurvivor.model.HazardKind
import com.baraka.game.trainsurvivor.systems.TrainGame

/*
 * Co-op HUD, kept off the middle of the screen so the train stays visible:
 *   - CoopPartnerStrip  top-left: one thin line about the partner's train.
 *   - CoopAlert         top-centre: ONE line, the most urgent thing right now (fades when it is over).
 *   - CoopDock          right edge, above the ability buttons: round icon buttons with cooldown rings.
 *   - CoopHelpButton    bottom-centre: appears only while the partner needs us to clear a jam or gate.
 * Sending a passenger is the small arrow on each crew chip (see WeaponSwitcher in TrainSurvivorScreen.kt).
 */

private val ALERT_RED = Color(0xFFFF8A65)
private val ALERT_GOLD = Color(0xFFFFD54F)
private val ALERT_CYAN = Color(0xFF80DEEA)

/** The partner's train in one thin line: distance, route and health. */
@Composable
internal fun CoopPartnerStrip(session: CoopSession, tick: MutableLongState, modifier: Modifier) {
    tick.longValue
    val p = session.partner
    Row(
        modifier = modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ok = p.connected && !p.down && !p.away
        val text = when {
            !p.connected -> "PARTNER LEFT"
            p.down -> "PARTNER DOWN"
            p.away -> "NO SIGNAL"
            else -> "P2  ${p.distance} m"
        }
        Text(text, color = if (ok) ALERT_CYAN else ALERT_RED, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (ok) {
            Box(Modifier.width(44.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF37474F))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p.hpFrac.coerceIn(0f, 1f)).background(if (p.hpFrac > 0.4f) Color(0xFF66BB6A) else Color(0xFFEF5350)))
            }
            if (p.ourPassengerAway > 0f) Text("guest ${p.ourPassengerAway.toInt()}s", color = ALERT_GOLD, fontSize = 9.sp)
        }
    }
}

/** One line at the top edge with the most urgent co-op message, if there is one. */
@Composable
internal fun CoopAlert(game: TrainGame, session: CoopSession, tick: MutableLongState, modifier: Modifier) {
    tick.longValue
    val p = session.partner
    val text: String
    val color: Color
    when {
        game.lockedBy != null -> {
            text = if (game.jammed) "GUNS JAMMED - your partner can clear it" else "GATE CLOSED - your partner can open it"
            color = ALERT_RED
        }
        game.rescueTime > 0f -> {
            text = if (game.guestPresent) "RESCUING..." else "SURVIVOR STRANDED - ask your partner to send a passenger (${game.rescueTime.toInt() + 1}s)"
            color = ALERT_GOLD
        }
        game.partnerRescueTime > 0f && p.connected -> {
            text = "PARTNER NEEDS A RESCUE (${game.partnerRescueTime.toInt() + 1}s) - tap the arrow on a crew chip"
            color = ALERT_GOLD
        }
        game.comboCount > 0 && game.boss != null -> {
            text = "COMBO x${game.comboCount}"
            color = ALERT_CYAN
        }
        else -> return
    }
    Text(
        text,
        color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
        modifier = modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** Round co-op action buttons (give coins, send repair, send tow), stacked above the ability buttons. */
@Composable
internal fun CoopDock(game: TrainGame, session: CoopSession, tick: MutableLongState, modifier: Modifier) {
    tick.longValue
    val p = session.partner
    if (!p.connected || p.down) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
        if (game.towLevel > 0) {
            AbilityButton(tick, "TOW", Color(0xFF00897B), { game.towReady }, { 1f - game.towCooldown / game.towCooldownMax }, { game.towCooldown }, diameter = 50, fontSize = 10) { game.sendTow() }
        }
        if (game.gangwayLevel > 0) {
            AbilityButton(tick, "FIX", Color(0xFF2E7D32), { game.repairSendReady }, { 1f - game.repairSendCooldown / game.repairSendCooldownMax }, { game.repairSendCooldown }, diameter = 50, fontSize = 10) { game.sendRepair() }
        }
        AbilityButton(tick, "GIVE", Color(0xFFF9A825), { game.coinGiftReady }, { 1f - game.coinGiftCooldown / 4f }, { game.coinGiftCooldown }, diameter = 50, fontSize = 10) { game.sendCoins() }
    }
}

/** A large button that only exists while the partner is locked and we can free them. */
@Composable
internal fun CoopHelpButton(game: TrainGame, session: CoopSession, tick: MutableLongState, modifier: Modifier) {
    tick.longValue
    val help = game.partnerHelp ?: return
    if (!session.partner.connected || session.partner.down) return
    val label = (if (help == HazardKind.SIGNAL_JAM) "CLEAR PARTNER'S JAM " else "OPEN PARTNER'S GATE ") + (game.partnerHelpTime.toInt() + 1) + "s"
    Text(
        label,
        color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(ALERT_RED).clickable { game.helpPartner() }.padding(horizontal = 18.dp, vertical = 10.dp)
    )
}
