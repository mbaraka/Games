package com.baraka.game.dreadnought.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.dreadnought.model.HullForm
import com.baraka.game.dreadnought.model.Paints
import com.baraka.game.dreadnought.model.ShipDesign
import com.baraka.game.dreadnought.render.drawShipPortrait
import com.baraka.game.dreadnought.systems.Ship
import kotlin.math.min

private val WIDTH_NAMES = arrayOf("Slim", "Normal", "Wide")

/**
 * The shipyard: design your own ship. You choose a hull form, its size, armour and engines, the big guns, small guns,
 * torpedoes, aircraft and paint. Everything costs points out of a fixed budget, so a design is always a trade-off.
 * The picture on the left is the real ship, redrawn as you change things.
 */
@Composable
fun ShipyardScreen(initial: ShipDesign, isNew: Boolean, onSave: (ShipDesign) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var d by remember { mutableStateOf(initial.fixed()) }
    BackHandler(onBack = onClose)
    val ship = remember(d) { Ship(d.toShipClass(), 0) }
    var clock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) clock += (now - last) / 1e9f
                last = now
            }
        }
    }
    val cost = d.cost()
    val over = cost > ShipDesign.BUDGET
    val form = d.form
    fun change(nd: ShipDesign) {
        d = nd.fixed()
    }

    Row(
        Modifier.fillMaxSize().background(NavyColors.Bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // left: name, the ship itself, the budget and the finished numbers
        Column(Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavyButton("< Back", onClose, fontSize = 12.sp)
                NavyLabel("SHIPYARD", size = 20.sp, bold = true, color = NavyColors.Accent)
            }
            OutlinedTextField(
                value = d.name,
                onValueChange = { change(d.copy(name = it.take(14))) },
                singleLine = true,
                label = { Text("Ship name", color = NavyColors.Muted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NavyColors.Text, unfocusedTextColor = NavyColors.Text,
                    focusedBorderColor = NavyColors.Accent, unfocusedBorderColor = NavyColors.Muted,
                    focusedLabelColor = NavyColors.Accent, unfocusedLabelColor = NavyColors.Muted,
                    cursorColor = NavyColors.Accent,
                    focusedContainerColor = NavyColors.Card, unfocusedContainerColor = NavyColors.Card
                )
            )
            Canvas(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(NavyColors.Water)) {
                drawShipPortrait(ship, false, clock)
            }
            NavyLabel(
                "Points: $cost / ${ShipDesign.BUDGET}" + if (over) "   - over budget!" else "   (${ShipDesign.BUDGET - cost} left)",
                size = 14.sp, bold = true, color = if (over) NavyColors.Bad else NavyColors.Good
            )
            Box(Modifier.fillMaxWidth().height(8.dp).background(NavyColors.Bar, RoundedCornerShape(4.dp))) {
                Box(
                    Modifier.fillMaxWidth(min(1f, cost / ShipDesign.BUDGET.toFloat())).height(8.dp)
                        .background(if (over) NavyColors.Bad else NavyColors.Good, RoundedCornerShape(4.dp))
                )
            }
            NavyLabel(
                "Hull ${d.hullPoints.toInt()}  |  Speed ${d.speed.toInt()} m/s  |  Length ${d.length} m",
                size = 12.sp
            )
            if (d.mains > 0) {
                NavyLabel(
                    "Big guns: ${d.mains} x ${d.barrels}, ${d.gunDamage.toInt()} damage, every ${"%.1f".format(d.gunReload)} s, ${d.gunRange.toInt()} m reach",
                    size = 12.sp, color = NavyColors.Muted
                )
            }
            if (d.secondaries > 0) NavyLabel("Small guns: ${d.secondaries} (fire by themselves)", size = 12.sp, color = NavyColors.Muted)
            if (d.tubes > 0) NavyLabel("Torpedoes: ${d.tubes} tubes, a fan per TORP press", size = 12.sp, color = NavyColors.Muted)
            if (d.planes > 0) NavyLabel("Aircraft: ${d.planes}, press LAUNCH to bomb the target", size = 12.sp, color = NavyColors.Muted)
            if (d.dive) NavyLabel("Can dive: shells cannot hit you while under", size = 12.sp, color = NavyColors.Muted)
            if (d.mains == 0 && d.secondaries == 0 && d.tubes == 0 && d.planes == 0) {
                NavyLabel("No weapons yet - add some on the right.", size = 12.sp, color = NavyColors.Bad)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavyButton(
                    "Save ship", { onSave(d.copy(name = d.name.trim().ifBlank { "My ship" })) }, Modifier.weight(1f).height(48.dp),
                    enabled = !over && (d.mains + d.secondaries + d.tubes + d.planes) > 0, accent = true, fontSize = 16.sp
                )
                if (!isNew) NavyButton("Delete", onDelete, Modifier.height(48.dp), fontSize = 13.sp)
            }
        }

        // right: every choice, one row each
        LazyColumn(Modifier.weight(0.58f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Section("HULL") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val forms = HullForm.entries
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 0 until 3) FormChip(forms[i], form == forms[i], Modifier.weight(1f)) { change(startingDesign(d, forms[i])) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 3 until forms.size) FormChip(forms[i], form == forms[i], Modifier.weight(1f)) { change(startingDesign(d, forms[i])) }
                        Box(Modifier.weight(1f))
                    }
                    NavyLabel(form.blurb, size = 12.sp, color = NavyColors.Muted)
                }
            }
            item {
                Stepper("Length", "${d.length} m", "longer = more hull points, slower, 1 point per 8 m",
                    d.length > form.minLength, d.length < form.maxLength,
                    { change(d.copy(length = d.length - 10)) }, { change(d.copy(length = d.length + 10)) })
            }
            item {
                Stepper("Width", WIDTH_NAMES[d.width], "wide is tougher but slower",
                    d.width > 0, d.width < 2, { change(d.copy(width = d.width - 1)) }, { change(d.copy(width = d.width + 1)) })
            }
            item {
                Stepper("Armour", "level ${d.armor}  (${d.hullPoints.toInt()} hull)", "5 points per level",
                    d.armor > 1, d.armor < 5, { change(d.copy(armor = d.armor - 1)) }, { change(d.copy(armor = d.armor + 1)) })
            }
            item {
                Stepper("Engines", "level ${d.engine}  (${d.speed.toInt()} m/s)", "4 points per level",
                    d.engine > 1, d.engine < 5, { change(d.copy(engine = d.engine - 1)) }, { change(d.copy(engine = d.engine + 1)) })
            }

            if (form.maxMains > 0) {
                item { Section("BIG GUNS") }
                item {
                    Stepper("Gun turrets", "${d.mains}", "how many big turrets",
                        d.mains > 0, d.mains < form.maxMains, { change(d.copy(mains = d.mains - 1)) }, { change(d.copy(mains = d.mains + 1)) })
                }
                if (d.mains > 0) {
                    item {
                        Stepper("Barrels per turret", "${d.barrels}", "more barrels = more shells per salvo",
                            d.barrels > 1, d.barrels < 4, { change(d.copy(barrels = d.barrels - 1)) }, { change(d.copy(barrels = d.barrels + 1)) })
                    }
                    item {
                        Stepper("Gun size", ShipDesign.GUN_NAMES[d.caliber - 1] + "  (${d.gunDamage.toInt()} damage)", "bigger guns hit harder, reload slower, reach further, cost more",
                            d.caliber > 1, d.caliber < 5, { change(d.copy(caliber = d.caliber - 1)) }, { change(d.copy(caliber = d.caliber + 1)) })
                    }
                }
            }
            if (form.maxSecondaries > 0 || form.maxTubes > 0 || form.maxPlanes > 0 || form.canDive) {
                item { Section("OTHER WEAPONS") }
            }
            if (form.maxSecondaries > 0) {
                item {
                    Stepper("Small guns", "${d.secondaries}", "they shoot nearby ships and planes by themselves, 2 points each",
                        d.secondaries > 0, d.secondaries < form.maxSecondaries, { change(d.copy(secondaries = d.secondaries - 2)) }, { change(d.copy(secondaries = d.secondaries + 2)) })
                }
            }
            if (form.maxTubes > 0) {
                item {
                    Stepper("Torpedo tubes", "${d.tubes}", "TORP button fires them in a fan, 6 points each",
                        d.tubes > 0, d.tubes < form.maxTubes, { change(d.copy(tubes = d.tubes - 1)) }, { change(d.copy(tubes = d.tubes + 1)) })
                }
            }
            if (form.maxPlanes > 0) {
                item {
                    Stepper("Aircraft", "${d.planes}", "LAUNCH sends them to bomb, 12 points each",
                        d.planes > 0, d.planes < form.maxPlanes, { change(d.copy(planes = d.planes - 1)) }, { change(d.copy(planes = d.planes + 1)) })
                }
            }
            if (form.canDive) {
                item {
                    Stepper("Dive ability", if (d.dive) "On" else "Off", "20 points",
                        d.dive, !d.dive, { change(d.copy(dive = false)) }, { change(d.copy(dive = true)) })
                }
            }

            item { Section("LOOK") }
            if (form.hasFunnels) {
                item {
                    Stepper("Funnels", "${d.funnels}", "looks only",
                        d.funnels > 0, d.funnels < 3, { change(d.copy(funnels = d.funnels - 1)) }, { change(d.copy(funnels = d.funnels + 1)) })
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().background(NavyColors.Card, RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NavyLabel("Paint: " + Paints.names[d.paint], size = 14.sp, bold = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in Paints.hull.indices) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(Color(Paints.hull[i]))
                                    .border(if (d.paint == i) 3.dp else 1.dp, if (d.paint == i) NavyColors.Accent else NavyColors.Muted, CircleShape)
                                    .clickable { change(d.copy(paint = i)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A fresh design on a new hull form, keeping the name and paint but resetting everything the old form decided. */
private fun startingDesign(old: ShipDesign, f: HullForm): ShipDesign {
    val base = old.copy(form = f, length = f.defaultLength, width = 1, armor = 3, engine = 3, funnels = 2, dive = false, planes = 0, tubes = 0)
    return when (f) {
        HullForm.CARRIER -> base.copy(mains = 0, secondaries = 4, planes = 4)
        HullForm.SUBMARINE -> base.copy(mains = 1, barrels = 1, caliber = 2, secondaries = 0, tubes = 4, dive = true)
        HullForm.DESTROYER -> base.copy(mains = 3, barrels = 1, caliber = 2, secondaries = 0, tubes = 2)
        HullForm.CRUISER -> base.copy(mains = 3, barrels = 3, caliber = 3, secondaries = 4)
        HullForm.BATTLESHIP -> base.copy(mains = 3, barrels = 3, caliber = 4, secondaries = 6)
    }
}

@Composable
private fun Section(text: String) {
    NavyLabel(text, size = 12.sp, color = NavyColors.Accent, bold = true, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun FormChip(f: HullForm, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    NavyButton(f.label, onClick, modifier, accent = selected, fontSize = 12.sp, compact = true)
}

/** One adjustable value: what it is, what it is now, a hint about its cost, and - / + buttons. */
@Composable
private fun Stepper(
    label: String,
    value: String,
    hint: String,
    canMinus: Boolean,
    canPlus: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(NavyColors.Card, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            NavyLabel(label, size = 14.sp, bold = true)
            NavyLabel(hint, size = 10.sp, color = NavyColors.Muted)
        }
        NavyButton("-", onMinus, Modifier.width(44.dp), enabled = canMinus, fontSize = 18.sp, compact = true)
        Text(
            value, color = NavyColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(112.dp), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        NavyButton("+", onPlus, Modifier.width(44.dp), enabled = canPlus, fontSize = 18.sp, compact = true)
    }
}
