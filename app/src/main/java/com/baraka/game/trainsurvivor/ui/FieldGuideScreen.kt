package com.baraka.game.trainsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.TrainUpgrade
import com.baraka.game.trainsurvivor.model.WeaponType
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.render.drawWeaponPortrait
import com.baraka.game.trainsurvivor.render.drawZombiePortrait

private val BG = Color(0xFF10131A)
private val CARD = Color(0xFF1D222D)
private val AMBER = Color(0xFFFFB300)
private val MUTED = Color(0xFF9AA3B2)
private val PORTRAIT_BG = Color(0xFF2A3342)

private class ZombieNote(val appears: String, val about: String, val tip: String)

private val zombieNotes: Map<ZombieType, ZombieNote> = mapOf(
    ZombieType.WALKER to ZombieNote("From the start", "Slow shambler and the backbone of every horde. Many wear silly costumes - purely cosmetic.", "Cheap coins. Let your pistols and SMGs handle them."),
    ZombieType.RUNNER to ZombieNote("After ~20 s", "Fast and fragile. Reaches the train before slow guns can cut it down.", "Rapid-fire weapons shred them. Very common in the Scorched Desert."),
    ZombieType.BRUTE to ZombieNote("After ~70 s", "Huge, slow and very tanky. Claws hard once it reaches the train. Appears earlier in the Mountain Pass and Graveyard.", "Use high-damage guns like the Sniper, Rocket or Laser."),
    ZombieType.BLOATER to ZombieNote("After ~45 s", "Bursts on contact for heavy train damage. When shot, it pops and hurts zombies around it.", "Kill it at range - ideally while it is surrounded by other zombies."),
    ZombieType.CLOWN to ZombieNote("After ~30 s", "Bouncy and loud. Honks and showers confetti when it dies.", "Worth extra coins - don't let it reach the train."),
    ZombieType.BAT to ZombieNote("After ~25 s", "Flies fast and erratically, swooping over the cars to claw them from above. Common in the Tunnel and Graveyard.", "Flak Turret and any gun except the Cannon can hit it."),
    ZombieType.BALLOON to ZombieNote("After ~40 s", "Drifts in under party balloons and hovers over the roof. Pops into confetti. Common in the Mountain Pass.", "Great coin value. Flak Turret, Rocket and Tesla Coil all work well."),
    ZombieType.TITAN to ZombieNote("Boss: ~75 s, then every 80 s", "A giant with spikes and a glowing chest. Every few seconds it SMASHES the car it is clawing for extra damage.", "Stack Dynamite blasts, Tesla chains and your strongest guns."),
    ZombieType.GARGOYLE to ZombieNote("Boss: the 2nd, 5th, 8th...", "A stone-winged flyer that circles above the train and screeches up three bats at a time.", "Cannons can't hit it. Flak Turret, Rocket, Laser and Sniper can."),
    ZombieType.CLOWN_PRINCE to ZombieNote("Boss: the 3rd, 6th, 9th...", "A giant clown in a crown who summons two clowns every few seconds. Honk honk.", "Focus the Prince first - the clowns are weak. Mortar and splash weapons love the crowd.")
)

@Composable
fun FieldGuideScreen(onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) time.floatValue += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
            }
        }
    }
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = CARD, contentColor = Color.White)
            ) { Text("< Back", fontSize = 12.sp) }
            Text("FIELD GUIDE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            listOf("ZOMBIES", "WEAPONS", "ROUTES").forEachIndexed { i, label ->
                GuideTab(label, tab == i) { tab = i }
            }
        }
        when (tab) {
            0 -> ZombieGuide(time)
            1 -> WeaponGuide(time)
            else -> RouteGuide()
        }
    }
}

@Composable
private fun GuideTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AMBER else CARD)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ZombieGuide(time: androidx.compose.runtime.MutableFloatState) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ZombieType.entries.toList()) { type ->
            val note = zombieNotes.getValue(type)
            GuideCard(
                portrait = { Canvas(Modifier.fillMaxSize().padding(6.dp)) { drawZombiePortrait(type, time.floatValue) } }
            ) {
                Text(type.displayName + if (type.flying) "  (flying)" else "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "HP ${type.hp.toInt()}  -  speed ${type.speed.toInt()}  -  " +
                        (if (type == ZombieType.BLOATER) "bursts for 30" else "${type.damage.toInt()} dmg/s") +
                        "  -  ${type.coins} coins",
                    color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
                Text("Appears: ${note.appears}", color = MUTED, fontSize = 11.sp)
                Text(note.about, color = Color(0xFFD0D6E0), fontSize = 11.sp)
                Text("Tip: ${note.tip}", color = Color(0xFF81C784), fontSize = 11.sp)
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text(
                "Zombie health grows over each run (+1 base HP every 45 s) and their claws hit harder over time. Routes add their own multipliers - see the ROUTES tab.",
                color = MUTED, fontSize = 11.sp, modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun WeaponGuide(time: androidx.compose.runtime.MutableFloatState) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(WeaponType.entries.toList()) { weapon ->
            val dps = weapon.damage / weapon.interval
            GuideCard(portrait = { Canvas(Modifier.fillMaxSize().padding(8.dp)) { drawWeaponPortrait(weapon) } }) {
                Text(weapon.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("~${dps.toInt()} damage/s  -  ${"%.1f".format(1f / weapon.interval)} shots/s", color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${weapon.damage.toInt()} dmg  -  range ${weapon.range.toInt()}" +
                        (if (weapon.pierce > 1) "  -  pierces ${weapon.pierce}" else "") +
                        (if (weapon.splashRadius > 0f) "  -  splash ${weapon.splashRadius.toInt()}" else ""),
                    color = Color(0xFFD0D6E0), fontSize = 11.sp
                )
                Text(
                    (if (weapon.unlockCost == 0) "Starting weapon" else "Unlock: ${weapon.unlockCost} coins") +
                        if (weapon.groundOnly) "  -  ground targets only" else "  -  hits flyers",
                    color = MUTED, fontSize = 11.sp
                )
                Text(weapon.blurb, color = Color(0xFF81C784), fontSize = 11.sp)
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Reading the numbers", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Every passenger can own several weapons. On AUTO they pick the best one for the enemies in front of them (type, distance, how many, flyers or not) " +
                        "and swapping takes a moment. In a run, tap a passenger's chip on the left to force a weapon, or tap again to go back to AUTO. " +
                        "Each Power upgrade adds +30% damage to all of that passenger's weapons (up to +150%); it does not change fire rate or range. " +
                        "Damage/s is single-target - Shotgun, Cannon and Rocket also splash nearby zombies, and the Sniper and Laser pierce a line of zombies, so they beat their raw number against crowds.",
                    color = MUTED, fontSize = 11.sp
                )
                Text("Train gear", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                TrainUpgrade.entries.forEach {
                    Text(it.displayName + " (max level ${it.maxLevel}): " + it.blurb, color = MUTED, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RouteGuide() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Biome.entries.toList()) { biome ->
            val accent = Color(biome.accent)
            Column(
                Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(biome.displayName, color = accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(biome.tagline, color = Color(0xFFD0D6E0), fontSize = 11.sp)
                Text(
                    "Coins x${fmt(biome.coinMult)}  -  zombie HP x${fmt(biome.hpMult)}  -  spawns x${fmt(biome.spawnMult)}  -  damage x${fmt(biome.dmgMult)}",
                    color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text(
                "A junction appears every ~50 s of a run. Pick a route before the countdown ends, or you take the first one offered.",
                color = MUTED, fontSize = 11.sp, modifier = Modifier.padding(4.dp)
            )
        }
    }
}

private fun fmt(x: Float): String = "%.2f".format(x).trimEnd('0').trimEnd('.')

@Composable
private fun GuideCard(portrait: @Composable () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(width = 92.dp, height = 112.dp).clip(RoundedCornerShape(10.dp)).background(PORTRAIT_BG)
        ) { portrait() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { content() }
    }
}
