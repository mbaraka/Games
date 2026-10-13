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
import com.baraka.game.trainsurvivor.model.DangerLevel
import com.baraka.game.trainsurvivor.model.EliteTrait
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
    ZombieType.BRUTE to ZombieNote("After ~70 s", "Huge, slow and very tanky. Claws hard once it reaches the train. Appears earlier in the Mountain Pass and Graveyard.", "Armored: every hit loses 5 damage, so small bullets barely scratch it. Use the Sniper, Rocket, Cannon or Laser."),
    ZombieType.BLOATER to ZombieNote("After ~45 s", "Bursts on contact for heavy train damage. When shot, it pops and hurts zombies around it.", "Kill it at range - ideally while it is surrounded by other zombies."),
    ZombieType.CLOWN to ZombieNote("After ~30 s", "Bouncy and loud. Honks and showers confetti when it dies.", "Worth extra coins - don't let it reach the train."),
    ZombieType.BAT to ZombieNote("After ~25 s", "Flies fast and erratically, swooping over the cars to claw them from above. Common in the Tunnel and Graveyard.", "Flak Turret and any gun except the Cannon can hit it."),
    ZombieType.BALLOON to ZombieNote("After ~40 s", "Drifts in under party balloons and hovers over the roof. Pops into confetti. Common in the Mountain Pass.", "Great coin value. Flak Turret, Rocket and Tesla Coil all work well."),
    ZombieType.TITAN to ZombieNote("Boss: ~75 s, then every 80 s", "A giant with spikes and a glowing chest. Every few seconds it SMASHES the car it is clawing for extra damage.", "Armor 12: pistols, SMGs and miniguns do almost nothing. Stack Dynamite, Rockets, Sniper shots and Tesla chains."),
    ZombieType.GARGOYLE to ZombieNote("Boss: the 2nd, 5th, 8th...", "A stone-winged flyer that circles above the train and screeches up three bats at a time.", "Cannons can't hit it. Flak Turret, Rocket, Laser and Sniper can."),
    ZombieType.CLOWN_PRINCE to ZombieNote("Boss: the 3rd, 6th, 9th...", "A giant clown in a crown who summons two clowns every few seconds. Honk honk.", "Focus the Prince first - the clowns are weak. Mortar and splash weapons love the crowd."),
    ZombieType.ZOMBIE_DOG to ZombieNote("Open Plains, from ~8 s", "A fast, snarling hound that arrives in pairs. Weak but quick.", "SMGs and Pistols are perfect - don't waste Rockets on them."),
    ZombieType.YETI to ZombieNote("Mountain Pass, after ~35 s", "A huge shaggy brute. Armor 4 and a lot of health.", "Use heavier hits: Sniper, Rocket or Cannon."),
    ZombieType.RAT to ZombieNote("Dark Tunnel, from ~8 s", "Tiny and very fast; they swarm in packs of four.", "Splash and chain weapons shine: Shotgun, Tesla Coil, Mortar."),
    ZombieType.SCORPION to ZombieNote("Scorched Desert, from ~12 s", "A low, armored (3) crawler that skitters in quickly.", "Mid-damage weapons and the Flamethrower handle them."),
    ZombieType.SKELETON to ZombieNote("Haunted Graveyard, from ~12 s", "Brittle bones that clatter in fast.", "Anything works - save the big shots for Brutes."),
    ZombieType.LEAPER to ZombieNote("Lunar Rails, from ~20 s", "An astronaut zombie that JUMPS over the queue and lands on a wagon roof, then claws it from above. Low gravity makes the jump slow and floaty.", "Shoot it while it floats. Wagons nearer the roof gunners cover each other."),
    ZombieType.DRONE to ZombieNote("Orbital Station, from ~22 s", "An armored (3) flying gun platform. It hovers at a distance and shoots the train, arriving in pairs.", "Flak Turret, Rocket and Laser; the Cannon can't reach it."),
    ZombieType.MAGMA_GOLEM to ZombieNote("Volcano Line, after ~40 s", "A walking lava rock, armor 5. Fire cannot hurt it, so the Flamethrower does nothing.", "Sniper, Rocket, Cannon, Tesla Coil."),
    ZombieType.ICE_WOLF to ZombieNote("Frozen Tundra, from ~10 s", "A fast wolf that runs in pairs and slides in on the ice.", "Short-range fast guns; the blizzard cuts your range, so keep close weapons ready."),
    ZombieType.FROST_WRAITH to ZombieNote("Frozen Tundra, after ~30 s", "A translucent flying ghost that drifts over the train and claws from above.", "Flak Turret and any gun except the Cannon."),
    ZombieType.GUNSLINGER to ZombieNote("Ghost Town, from ~18 s", "Stops far from the train and SHOOTS it instead of walking up. It does not queue, so only range reaches it.", "Sniper, Laser and Rocket outrange it. Hawk's range perk helps."),
    ZombieType.JUGGLER to ZombieNote("Carnival, from ~15 s", "A clown that stands back and pelts the train with juggling balls. Pops into two mini clowns when killed.", "Kill it at range; splash weapons clear its minis."),
    ZombieType.STRONGMAN to ZombieNote("Carnival, after ~45 s", "A huge armored (5) circus strongman.", "Big hitters: Sniper, Rocket, Cannon."),
    ZombieType.MINI_CLOWN to ZombieNote("Carnival (from popped clowns)", "A small fast clown that appears when a clown or juggler pops.", "Shotgun, Tesla and Mortar clear them in bulk."),
    ZombieType.DROWNED to ZombieNote("Sea Bridge, from ~8 s", "A waterlogged walker draped in seaweed.", "Anything works; watch the sea for tentacles."),
    ZombieType.TENTACLE to ZombieNote("Sea Bridge hazard", "Rises from the water and squeezes a wagon until it is killed (or lets go after a while).", "Shoot it fast; it only attacks the wagon it holds."),
    ZombieType.RIOT to ZombieNote("Scorched Desert, from ~25 s", "A riot zombie with a shield, armor 6. Some of them drop from overpasses onto your roofs.", "Big hitters and the Tesla Coil."),
    ZombieType.PIRANHA to ZombieNote("Deep Sea Tube, from ~8 s", "A tiny, very fast swimmer that arrives in packs of three and nips at the wagons from above.", "Rapid-fire guns, Shotgun and Tesla Coil shred a pack."),
    ZombieType.JELLYFISH to ZombieNote("Deep Sea Tube, after ~25 s", "A slow, translucent drifter that hovers over a wagon and stings it.", "Flak Turret and anything except the Cannon."),
    ZombieType.DEEP_CRAB to ZombieNote("Deep Sea Tube, from ~20 s", "A low, armored (4) crab that scuttles in with a heavy claw.", "Sniper, Rocket and Cannon punch through its shell."),
    ZombieType.SHARK to ZombieNote("Deep Sea Tube, after ~18 s", "A big armored (3) swimmer with bandages and a red eye. It circles the train and bites a wagon from above.", "Sniper, Rocket and Laser; the Cannon can't reach a swimmer."),
    ZombieType.LEAPING_SHARK to ZombieNote("Sea Bridge, after ~30 s", "Slides along the deck, then BREACHES: it leaps onto a wagon roof and bites from there.", "Shoot it before it jumps, or focus it once it lands on a roof."),
    ZombieType.YETI_KING to ZombieNote("Boss of the Mountain Pass", "A towering frost giant. Every few seconds it ROARs and smashes the wagon it is clawing.", "Armor 8: bring big hitters and Dynamite."),
    ZombieType.RAT_KING to ZombieNote("Boss of the Dark Tunnel", "A huge rat that squeaks up a pack of four rats every few seconds.", "Splash and chain weapons clear the pack; focus the King between waves."),
    ZombieType.SCORPION_QUEEN to ZombieNote("Boss of the Scorched Desert", "A giant armored (8) scorpion that skitters out two scorpions at a time.", "Heavy single hits for the Queen, splash for her brood."),
    ZombieType.SWAMP_TYRANT to ZombieNote("Boss of the Murky Swamp", "A giant crocodile that lunges and then CHOMPS a wagon for big damage.", "Armor 8: Sniper, Rocket and Cannon, and keep wagons repaired."),
    ZombieType.LUNAR_COLOSSUS to ZombieNote("Boss of Lunar Rails", "A giant astronaut that leaps onto a wagon roof and SLAMS it.", "Shoot it while it floats; Overdrive and Dynamite help."),
    ZombieType.OVERSEER to ZombieNote("Boss of the Orbital Station", "A flying mother drone that shoots from afar and deploys two drones at a time.", "Flak Turret, Rocket and Laser; the Cannon can't reach it."),
    ZombieType.MAGMA_TITAN to ZombieNote("Boss of the Volcano Line", "A towering lava giant. Fire-proof, armor 12, and it ERUPTs on the wagon it holds.", "Skip the Flamethrower: Sniper, Rocket, Tesla and Dynamite."),
    ZombieType.FROST_QUEEN to ZombieNote("Boss of the Frozen Tundra", "A flying ice queen that FREEZEs two frost wraiths into the fight every few seconds.", "Flak Turret and long-range guns - the blizzard cuts your range."),
    ZombieType.OUTLAW_KING to ZombieNote("Boss of Ghost Town", "A gunslinging boss that stands far back, shoots the train and calls in gunslingers.", "Outrange him: Sniper, Laser and Rocket."),
    ZombieType.MEGALODON to ZombieNote("Boss of the Sea Bridge", "A giant shark that BREACHes onto a wagon roof and bites down.", "Shoot it before it jumps, then focus it on the roof."),
    ZombieType.HOPPER to ZombieNote("Switchyard (shared worlds)", "A rail hopper with a grappling hook that jumps between the two tracks, so a gap in your partner's defence becomes your problem.", "Kill it before it hops; your guns reach both tracks."),
    ZombieType.SIGNALMAN to ZombieNote("Switchyard (shared worlds), rare", "Carries a lantern and throws the track switch the moment it reaches a train, swapping the two trains' tracks.", "Shoot it down before it arrives, or be ready to face your partner's crowd."),
    ZombieType.RUNAWAY_ENGINE to ZombieNote("Boss of the Switchyard", "A ghost locomotive that lunges, hops between the tracks and smashes the wagon it reaches. Armor 10.", "Big hits from both trains; watch it hop and be ready on whichever track it lands."),
    ZombieType.DEMOLISHER to ZombieNote("Boss of the Broken Viaduct", "A hard-hatted giant that DEMOLISHes the wagon it reaches. Armor 12.", "Big hits: Rocket, Sniper, Laser, Dynamite; keep your wagons repaired."),
    ZombieType.COMPY to ZombieNote("Jurassic Valley, from ~6 s", "A tiny, fast dinosaur that always comes in a pack of five. Weak alone, but they swarm the ends of the train.", "Splash damage: Shotgun, Cannon, Mortar and the Flamethrower."),
    ZombieType.RAPTOR to ZombieNote("Jurassic Valley, from ~14 s", "A quick pack hunter that LEAPS onto your wagon roofs and claws from above.", "Range and rapid fire: kill them before they jump."),
    ZombieType.SPITTER to ZombieNote("Jurassic Valley, from ~30 s", "A frilled spitter that stays back and hits the train from a distance. Frail up close.", "Outrange it: Sniper, Laser and Rocket."),
    ZombieType.PTERODACTYL to ZombieNote("Jurassic Valley, from ~22 s", "A zombie pterodactyl that swoops over the train and claws the wagons from above.", "Flak Turret and the Tesla Coil; guns that can aim up."),
    ZombieType.TRICERATOPS to ZombieNote("Jurassic Valley, from ~45 s", "An armored (9) tank that creeps, then CHARGES in short bursts with a deadly bite.", "Big hits: Rocket, Cannon, Sniper and Dynamite."),
    ZombieType.BRONTOSAURUS to ZombieNote("Jurassic Valley, rare, from ~80 s", "A walking mountain: 1,100 health and armor 12. Slow, but it crushes a wagon fast.", "Focus it with everything; Dynamite and the Mortar help."),
    ZombieType.TREX to ZombieNote("Boss of the Jurassic Valley", "A zombie T-Rex that lunges and ROARs, smashing the wagon it reaches. Armor 8.", "Big hits, Blast when it lunges, and shoot down the meteors."),
    ZombieType.ANGLERFISH to ZombieNote("Boss of the Deep Sea Tube", "A giant glowing-lure angler that summons four piranhas at a time.", "Splash and rapid fire for the piranhas, big hits for the Angler."),
    ZombieType.SUN_KING to ZombieNote("Boss of the Solar Rail", "A fiery king that burns the train from far away and BURNs three sprites into the fight. Fire-proof.", "Outrange it and raise the shields when flares warn."),
    ZombieType.STRIKER to ZombieNote("Co-op routes", "A fast armored runner that charges the train. Bosses on co-op routes send them in groups.", "Rapid fire; they only ever attack the train they spawned on, so deal with them yourself."),
    ZombieType.TWIN_BULWARK to ZombieNote("Boss of Twin Bridge (co-op)", "An armored giant that STRIKERS in a pack. Your own guns only scratch it: it falls to your partner's fire.", "Keep shooting it anyway: your damage cracks the one on your partner's track. Kill your own Strikers."),
    ZombieType.MIRROR_WARDEN to ZombieNote("Boss of Echo Mine (co-op)", "A mine guardian that shoots from range and calls ECHO strikers. Only your partner's fire hurts it properly.", "Trade covering fire with your partner, and open each other's gates."),
    ZombieType.EMBER to ZombieNote("Solar Rail, from ~8 s", "A burning zombie with a flaming head. Fire cannot hurt it, so the Flamethrower does nothing.", "Any gun works; Tesla and Mortar are good too."),
    ZombieType.SOLAR_SPRITE to ZombieNote("Solar Rail, after ~22 s", "A flickering ball of plasma that darts around erratically. Fire-proof, and solar flares can't burn it.", "Rapid-fire guns and Flak Turret."),
    ZombieType.SUN_PRIEST to ZombieNote("Solar Rail, after ~35 s", "A robed priest that stays back and hurls fire at the train from a distance. Fire-proof.", "Outrange it: Sniper, Laser and Rocket."),
    ZombieType.CROCODILE to ZombieNote("Murky Swamp, from ~10 s", "Creeps out of the bog, then LUNGES in short bursts. Armor 3 and a brutal bite.", "Kill it before it lunges into range: Sniper and Rocket work best.")
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
            listOf("ZOMBIES", "WEAPONS", "ROUTES", "ELITES").forEachIndexed { i, label ->
                GuideTab(label, tab == i) { tab = i }
            }
        }
        when (tab) {
            0 -> ZombieGuide(time)
            1 -> WeaponGuide(time)
            2 -> RouteGuide()
            else -> EliteGuide()
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
                    "HP ${type.hp.toInt()}" + (if (type.armor > 0f) "  -  armor ${type.armor.toInt()}" else "") + "  -  speed ${type.speed.toInt()}  -  " +
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
                if (weapon.heatPerShot > 0f) {
                    val rate = weapon.heatPerShot / weapon.interval - 0.22f
                    Text(
                        if (rate > 0f) "Overheats after ~${"%.0f".format(1f / rate)} s of firing, then locks for ~3 s" else "Never overheats",
                        color = Color(0xFFFF8A65), fontSize = 11.sp
                    )
                }
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Reading the numbers", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Armored zombies (Brutes and bosses) subtract a flat amount from every hit, so use big hitters against them and fast, light guns against weak swarms. The SMG, Minigun and Laser overheat and lock for ~3 s - their heat bar shows above the gunner - so keep a second weapon ready. Weapons cool down while they are not in hand. " +
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
                Text("Signature enemies: " + biome.signatureEnemies.joinToString(" and ") { it.displayName }, color = Color(0xFF81C784), fontSize = 11.sp)
                Text("Boss: " + biome.boss.displayName, color = Color(0xFFE040FB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                for (hazard in biome.hazards) Text("Hazard - " + hazard.displayName + ": " + hazard.description, color = Color(0xFFFF8A65), fontSize = 11.sp)
                if (biome.rangeMult != 1f) Text("Weapon range x" + fmt(biome.rangeMult), color = Color(0xFFB0BEC5), fontSize = 11.sp)
                if (biome.gravityMult != 1f) Text("Low gravity: debris and shells float", color = Color(0xFFB0BEC5), fontSize = 11.sp)
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

@Composable
private fun EliteGuide() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text(
                "ELITE zombies have a glowing ring and a diamond over their head. They have 60% more health and pay 2.5x coins, and each carries a trait that calls for a different answer. They appear more often the longer you survive and the higher the danger level.",
                color = MUTED, fontSize = 11.sp, modifier = Modifier.padding(4.dp)
            )
        }
        items(EliteTrait.entries.toList()) { trait ->
            Column(
                Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.size(18.dp)) {
                        drawCircle(Color(trait.color).copy(alpha = 0.7f), size.minDimension / 2f - 1f, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                        drawCircle(Color(trait.color), size.minDimension / 5f)
                    }
                    Text(trait.displayName, color = Color(trait.color), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(trait.blurb, color = Color(0xFFD0D6E0), fontSize = 11.sp)
                Text("Counter: " + trait.counter, color = Color(0xFF81C784), fontSize = 11.sp)
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text("DANGER LEVELS", color = AMBER, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        }
        items(DangerLevel.entries.toList()) { level ->
            Column(
                Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(level.displayName, color = Color(level.color), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Zombie health x${fmt(level.hpMult)}  -  damage x${fmt(level.dmgMult)}  -  spawns x${fmt(level.spawnMult)}  -  elites x${fmt(level.eliteMult)}",
                    color = Color(0xFFD0D6E0), fontSize = 11.sp
                )
                Text("Coins x${fmt(level.coinMult)}", color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (level == DangerLevel.NORMAL) "Always available" else "Unlock: reach ${level.requirementMeters} m on ${DangerLevel.entries[level.ordinal - 1].displayName}" +
                        if (level == DangerLevel.INFERNO) " - elites can carry two traits" else "",
                    color = MUTED, fontSize = 11.sp
                )
            }
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
