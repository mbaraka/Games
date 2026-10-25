package com.baraka.game.dreadnought.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.dreadnought.data.DreadSave
import com.baraka.game.dreadnought.model.MountKind
import com.baraka.game.dreadnought.model.Refit
import com.baraka.game.dreadnought.model.ShipClass
import com.baraka.game.dreadnought.model.ShipDesign
import com.baraka.game.dreadnought.model.Theatre
import com.baraka.game.dreadnought.render.drawShipPortrait
import com.baraka.game.dreadnought.systems.Ship

/**
 * The harbour: the player's ship, the sea to sail, permanent refits bought with coins, and a recognition guide
 * of every ship in the game (so the shapes can be studied before they are shot at).
 */
@Composable
fun HarborScreen(
    save: DreadSave,
    onSortie: () -> Unit,
    onBuy: (Refit) -> Unit,
    onTheatre: (Int) -> Unit,
    onShip: (Int) -> Unit,
    onBuyShip: (Int) -> Unit,
    designs: List<ShipDesign>,
    onNewDesign: () -> Unit,
    onEditDesign: (Int) -> Unit,
    tab: Int,
    onTab: (Int) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    val cls = save.shipClass()
    val owned = save.owns(save.ship)
    val player = remember(cls) { Ship(cls, 0) }
    var clock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) clock += (now - last) / 1e9f
                last = now
            }
        }
    }

    Row(
        Modifier.fillMaxSize().background(NavyColors.Bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // left: the ship and the sortie controls
        Column(
            Modifier.weight(0.45f).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    NavyLabel("DREADNOUGHT", size = 24.sp, bold = true, color = NavyColors.Accent)
                    NavyLabel("Command a battleship. Sink the fleet.", size = 12.sp, color = NavyColors.Muted)
                }
                NavyButton("Exit", onExit, fontSize = 12.sp)
            }
            Canvas(
                Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp)).background(NavyColors.Water)
            ) { drawShipPortrait(player, false, clock) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavyButton(if (owned) "SORTIE" else "LOCKED", onSortie, Modifier.weight(1f).height(52.dp), enabled = owned, accent = owned, fontSize = 18.sp)
                NavyButton("Settings", onSettings, Modifier.height(52.dp), fontSize = 13.sp)
            }
            NavyLabel(
                "Coins ${save.coins}   |   Best wave ${save.bestWave}   |   Best score ${save.bestScore}",
                size = 12.sp, color = NavyColors.Text
            )
            NavyLabel("Ships sunk ${save.kills}   |   Flagships ${save.flagships}", size = 12.sp, color = NavyColors.Muted)

            NavyLabel(
                "Ship: ${cls.label}" + if (owned) "   (change it in the SHIPS tab)" else "   - locked, unlock it in the SHIPS tab",
                size = 12.sp, color = NavyColors.Accent, bold = true
            )
            NavyLabel(loadout(cls), size = 11.sp, color = NavyColors.Muted)

            NavyLabel("SEA", size = 11.sp, color = NavyColors.Muted, bold = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (t in Theatre.entries) {
                    NavyButton(
                        t.label.substringAfterLast(' '),
                        { onTheatre(t.ordinal) },
                        Modifier.weight(1f),
                        enabled = save.theatreUnlocked(t),
                        accent = save.theatre == t.ordinal,
                        fontSize = 12.sp,
                        compact = true
                    )
                }
            }
            val sel = Theatre.entries[save.theatre]
            NavyLabel(sel.blurb, size = 12.sp, color = NavyColors.Muted)
            for (t in Theatre.entries) {
                if (!save.theatreUnlocked(t)) NavyLabel("${t.label}: reach wave ${t.unlockWave} to unlock", size = 11.sp, color = NavyColors.Muted)
            }
        }

        // right: tabs for refits, ships and the recognition guide
        Column(Modifier.weight(0.55f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val tabs = arrayOf("REFITS", "SHIPS", "GUIDE")
                for (i in tabs.indices) {
                    NavyButton(tabs[i], { onTab(i) }, Modifier.weight(1f), accent = tab == i, fontSize = 13.sp, compact = true)
                }
            }
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> {
                        item { NavyLabel("Permanent upgrades, bought with coins. They apply to whichever ship you sail.", size = 12.sp, color = NavyColors.Muted) }
                        items(Refit.entries) { r -> RefitRow(r, save, onBuy) }
                    }
                    1 -> {
                        item { NavyLabel("Choose the ship to sail. Tap a card to select it.", size = 12.sp, color = NavyColors.Muted) }
                        itemsIndexed(ShipClass.PLAYABLE) { i, c ->
                            val di = i - ShipClass.BUILTIN_PLAYABLE_COUNT
                            ShipCard(c, save.ship == i, save.owns(i), save.coins, { onShip(i) }, { onBuyShip(i) }, if (di >= 0) ({ onEditDesign(di) }) else null)
                        }
                        item {
                            NavyButton(
                                if (designs.size < ShipDesign.MAX_DESIGNS) "+ Design your own ship" else "Ship slots full (delete one to add)",
                                onNewDesign, Modifier.fillMaxWidth().height(48.dp), enabled = designs.size < ShipDesign.MAX_DESIGNS, accent = true, fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        item { NavyLabel("Every ship in the game, so you can learn to recognise them.", size = 12.sp, color = NavyColors.Muted) }
                        items(ShipClass.entries) { c -> GuideRow(c) }
                    }
                }
            }
        }
    }
}

/** One choosable ship: its picture, what it is good at, what it carries, and select / unlock. */
@Composable
private fun ShipCard(c: ShipClass, selected: Boolean, owned: Boolean, coins: Int, onSelect: () -> Unit, onUnlock: () -> Unit, onEdit: (() -> Unit)?) {
    val ship = remember(c) { Ship(c, 0) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(NavyColors.Card, shape)
            .border(if (selected) 2.dp else 0.dp, if (selected) NavyColors.Accent else NavyColors.Card, shape)
            .clickable { onSelect() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(Modifier.width(130.dp).height(56.dp).clip(RoundedCornerShape(8.dp)).background(NavyColors.Water)) {
            drawShipPortrait(ship, false, 0f)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NavyLabel(c.label + if (selected) "  (selected)" else "", size = 14.sp, bold = true, color = if (selected) NavyColors.Accent else NavyColors.Text)
            NavyLabel(c.heroBlurb, size = 11.sp, color = NavyColors.Text)
            NavyLabel(loadout(c), size = 10.sp, color = NavyColors.Muted)
        }
        if (!owned) {
            NavyButton("${c.unlockCost}", onUnlock, Modifier.width(78.dp), enabled = coins >= c.unlockCost, accent = coins >= c.unlockCost, fontSize = 13.sp, compact = true)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NavyButton(if (selected) "In use" else "Select", onSelect, Modifier.width(78.dp), enabled = !selected, fontSize = 12.sp, compact = true)
                if (onEdit != null) NavyButton("Edit", onEdit, Modifier.width(78.dp), accent = false, fontSize = 12.sp, compact = true)
            }
        }
    }
}

@Composable
private fun RefitRow(r: Refit, save: DreadSave, onBuy: (Refit) -> Unit) {
    val cls = save.shipClass()
    val level = save.level(r)
    val maxed = level >= r.maxLevel
    Row(
        Modifier.fillMaxWidth().background(NavyColors.Card, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            NavyLabel(r.label, size = 14.sp, bold = true)
            NavyLabel(r.blurb, size = 11.sp, color = NavyColors.Muted)
            NavyLabel(
                if (maxed) "Now: " + r.stat(cls, level) + " (maximum)" else "Now: " + r.stat(cls, level) + "   ->   Next: " + r.stat(cls, level + 1),
                size = 11.sp, color = NavyColors.Accent, bold = true
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 0 until r.maxLevel) {
                    Box(
                        Modifier.size(width = 16.dp, height = 5.dp)
                            .background(if (i < level) NavyColors.Accent else NavyColors.Bar, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
        val price = r.cost(level)
        NavyButton(
            if (maxed) "MAX" else "$price",
            { onBuy(r) },
            Modifier.width(84.dp),
            enabled = !maxed && save.coins >= price,
            accent = !maxed && save.coins >= price,
            fontSize = 13.sp
        )
    }
}

/** One line on what a ship carries when you sail it: its hull, speed and weapons. */
private fun loadout(c: ShipClass): String {
    val mains = c.mounts.filter { it.kind == MountKind.MAIN }
    val sec = c.mounts.size - mains.size
    return buildString {
        append("Hull ${c.heroHp.toInt()}  |  Speed ${c.maxSpeed.toInt()}  |  ")
        if (mains.isNotEmpty()) append("${mains.size} x ${mains[0].barrels} main guns")
        if (sec > 0) append(", $sec small guns")
        if (c.heroTubes > 0) append(", ${c.heroTubes} torpedoes")
        if (c.diver) append(", can dive")
    }
}

@Composable
private fun GuideRow(c: ShipClass) {
    val ship = remember(c) { Ship(c, if (c.player) 0 else 1) }
    val guns = remember(c) {
        val mains = c.mounts.filter { it.kind == MountKind.MAIN }
        val sec = c.mounts.size - mains.size
        buildString {
            if (mains.isNotEmpty()) append("${mains.size} x ${mains[0].barrels} main")
            if (sec > 0) append(if (isEmpty()) "$sec secondary" else ", $sec secondary")
            if (c.tubes > 0) append(if (isEmpty()) "${c.tubes} torpedo tubes" else ", ${c.tubes} tubes")
            if (isEmpty()) append("unarmed")
        }
    }
    Row(
        Modifier.fillMaxWidth().background(NavyColors.Card, RoundedCornerShape(10.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(Modifier.width(150.dp).height(48.dp).clip(RoundedCornerShape(8.dp)).background(NavyColors.Water)) {
            drawShipPortrait(ship, !c.player, 0f)
        }
        Column(Modifier.weight(1f)) {
            NavyLabel(c.label + "  -  " + c.role, size = 13.sp, bold = true)
            NavyLabel("${c.length.toInt()} m long, ${c.maxHp.toInt()} hull", size = 11.sp, color = NavyColors.Muted)
            NavyLabel(guns, size = 11.sp, color = NavyColors.Muted)
        }
    }
}
