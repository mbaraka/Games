package com.baraka.game.trainsurvivor.model

/** Environmental dangers a route can have. Each kind is implemented once in systems/Hazards.kt and drawn in render/HazardArt.kt. */
enum class HazardKind(val displayName: String, val description: String) {
    METEORS("Meteor shower", "Rocks crash into a wagon: tap them in the air or shoot them down with the Flak Turret."),
    GEYSERS("Lava geysers", "Geysers erupt after a short warning, burning everything standing on them - zombies and wagons."),
    HULL_BREACH("Hull breaches", "A wagon springs a leak: tap it within 6 seconds or lose 12% of your run coins."),
    TENTACLES("Sea tentacles", "Tentacles rise from the water and squeeze a wagon until you kill them."),
    OVERPASS_DROPS("Overpass drops", "Zombies drop from overpasses onto your roofs."),
    SIGNAL_JAM("Signal jam", "Co-op: every so often your guns jam. Only your PARTNER can clear it, from their screen - and you clear theirs."),
    GATE_LOCK("Track gate", "Co-op: a gate drops across your rails and your train crawls until your PARTNER opens it."),
    SOLAR_FLARES("Solar flares", "A flare hits after a short warning: tap to raise the shields or every wagon is burned. Flying enemies burn either way.")
}

/** One enemy a route can spawn. Its spawn weight is `scale * (base + perSecond * time)` once `time >= from`. */
data class Spawn(
    val type: ZombieType,
    val from: Float,
    val base: Float,
    val perSecond: Float = 0f,
    val scale: Float = 1f,
    /** Signature enemies belong to the route's landscape; they are listed on the route cards and in the Field Guide. */
    val signature: Boolean = false
) {
    fun weightAt(time: Float): Float = if (time >= from) scale * (base + perSecond * time) else 0f
}

/**
 * The standard enemies every route shares, each scaled by how common it is on that route (0 = never).
 * Using this keeps new routes to one line of spawn data plus their signature enemies.
 */
fun commonSpawns(
    walker: Float = 1f,
    runner: Float = 1f,
    brute: Float = 1f,
    bloater: Float = 1f,
    bat: Float = 1f,
    balloon: Float = 1f,
    clown: Float = 1f,
    bruteFrom: Float = 70f
): List<Spawn> = listOf(
    Spawn(ZombieType.WALKER, 0f, 10f, scale = walker),
    Spawn(ZombieType.RUNNER, 20f, 4f, 1f / 30f, runner),
    Spawn(ZombieType.BRUTE, bruteFrom, 0.33f, 1f / 60f, brute),
    Spawn(ZombieType.BLOATER, 45f, 2.5f, 0f, bloater),
    Spawn(ZombieType.BAT, 25f, 3f, 1f / 40f, bat),
    Spawn(ZombieType.BALLOON, 40f, 2f, 1f / 80f, balloon),
    Spawn(ZombieType.CLOWN, 30f, 2f, 0f, clown)
)

/** A route's own enemy: shows up often there and nowhere else. */
fun signature(type: ZombieType, from: Float, weight: Float = 5f, perSecond: Float = 1f / 50f) =
    Spawn(type, from, weight, perSecond, 1f, signature = true)

/**
 * A stretch of track the player can route onto at a junction.
 *
 * To add a route: add an entry here (stats, spawn table, gravity/range/speed tweaks, hazards), then register
 * a `RouteArt` for it in render/RouteArts.kt (scenery, weather, train kit). Junctions, the Field Guide and
 * the debug tools pick it up automatically. Routes without art fall back to the plains look.
 */
enum class Biome(
    val displayName: String,
    val tagline: String,
    val coinMult: Float,
    val hpMult: Float,
    val spawnMult: Float,
    val dmgMult: Float,
    val accent: Long,
    /** The boss that rules this route: it appears whenever a boss is due while the train is here. */
    val boss: ZombieType,
    val spawns: List<Spawn>,
    /** Multiplies the Repair Crew's healing while on this route. */
    val repairMult: Float = 1f,
    /** Free healing per second on this route, even without a Repair Crew. */
    val flatRegen: Float = 0f,
    /** Multiplies every weapon's range on this route (a blizzard is < 1). */
    val rangeMult: Float = 1f,
    /** Multiplies how fast ground zombies move on this route. */
    val speedMult: Float = 1f,
    /** Low gravity makes debris, shells and leaping zombies float. */
    val gravityMult: Float = 1f,
    val hazards: List<HazardKind> = emptyList(),
    /** Only appears in two-phone co-op runs. */
    val coopOnly: Boolean = false
) {
    PLAINS(
        "Open Plains", "Calm and safe: fewer, weaker zombies and the train repairs itself",
        1.0f, 0.85f, 0.7f, 0.9f, 0xFF8BC34A, ZombieType.TITAN,
        commonSpawns(bloater = 0.6f, bat = 0.6f, balloon = 1.2f, clown = 1.5f) + signature(ZombieType.ZOMBIE_DOG, 8f),
        repairMult = 2f, flatRegen = 2.5f
    ),
    MOUNTAIN(
        "Mountain Pass", "Frozen, much tougher zombies",
        1.4f, 1.5f, 1.0f, 1.0f, 0xFF81D4FA, ZombieType.YETI_KING,
        commonSpawns(walker = 0.8f, runner = 0.6f, brute = 1.8f, bloater = 0.3f, bat = 0.3f, balloon = 1.6f, clown = 0.5f, bruteFrom = 40f) + signature(ZombieType.YETI, 35f)
    ),
    TUNNEL(
        "Dark Tunnel", "Dense hordes in the dark",
        1.35f, 1.0f, 1.5f, 1.1f, 0xFFFFB74D, ZombieType.RAT_KING,
        commonSpawns(walker = 0.8f, bat = 2.5f, balloon = 0f, clown = 0.5f) + signature(ZombieType.RAT, 8f, weight = 6f)
    ),
    DESERT(
        "Scorched Desert", "Fast swarming runners",
        1.3f, 0.9f, 1.4f, 1.0f, 0xFFFFD54F, ZombieType.SCORPION_QUEEN,
        commonSpawns(walker = 0.8f, runner = 2.5f, bloater = 0.5f, bat = 0.5f, balloon = 0.8f, clown = 0.8f) + signature(ZombieType.SCORPION, 12f, weight = 4f)
    ),
    GRAVEYARD(
        "Haunted Graveyard", "Brutes and bloaters that hit very hard",
        1.7f, 1.25f, 1.0f, 1.35f, 0xFFB39DDB, ZombieType.GARGOYLE,
        commonSpawns(walker = 0.8f, brute = 2.5f, bloater = 3f, bat = 2f, balloon = 0.3f, clown = 0.5f, bruteFrom = 30f) + signature(ZombieType.SKELETON, 12f)
    ),
    SWAMP(
        "Murky Swamp", "Crocodiles lurk in the bog and gas bloaters drift through the mist",
        1.45f, 1.1f, 1.1f, 1.1f, 0xFF26A69A, ZombieType.SWAMP_TYRANT,
        commonSpawns(walker = 0.8f, runner = 0.6f, bloater = 2f, balloon = 0f, clown = 0.5f, bruteFrom = 55f) + signature(ZombieType.CROCODILE, 10f)
    ),
    MOON(
        "Lunar Rails", "Low gravity: leapers hop onto your roofs and meteors rain down",
        1.5f, 1.2f, 1.1f, 1.1f, 0xFFB0BEC5, ZombieType.LUNAR_COLOSSUS,
        commonSpawns(walker = 0.8f, runner = 0.8f, bloater = 0.5f, bat = 0f, balloon = 0f, clown = 0f) + signature(ZombieType.LEAPER, 20f),
        gravityMult = 0.4f, hazards = listOf(HazardKind.METEORS)
    ),
    ORBITAL(
        "Orbital Station", "Zero-g: hunter drones swarm and hull breaches drain your coins",
        1.6f, 1.1f, 1.2f, 1.1f, 0xFF7C4DFF, ZombieType.OVERSEER,
        commonSpawns(walker = 0.6f, runner = 0.5f, brute = 0.8f, bloater = 0.3f, bat = 0f, balloon = 0f, clown = 0f) + signature(ZombieType.DRONE, 22f),
        gravityMult = 0.3f, hazards = listOf(HazardKind.HULL_BREACH)
    ),
    VOLCANO(
        "Volcano Line", "Lava geysers burn everything and golems shrug off flames",
        1.6f, 1.3f, 1.0f, 1.25f, 0xFFFF5722, ZombieType.MAGMA_TITAN,
        commonSpawns(walker = 0.8f, brute = 1.8f, bloater = 1.5f, bat = 0.8f, balloon = 0f, clown = 0f) + signature(ZombieType.MAGMA_GOLEM, 40f),
        hazards = listOf(HazardKind.GEYSERS)
    ),
    TUNDRA(
        "Frozen Tundra", "Blizzard cuts your range 20% and ice wolves slide in fast",
        1.5f, 1.2f, 1.2f, 1.1f, 0xFFB3E5FC, ZombieType.FROST_QUEEN,
        commonSpawns(walker = 0.8f, runner = 0.8f, brute = 1.5f, bloater = 0.3f, bat = 0f, balloon = 0.8f, clown = 0f) +
            signature(ZombieType.ICE_WOLF, 10f) + signature(ZombieType.FROST_WRAITH, 30f, weight = 3f, perSecond = 1f / 80f),
        rangeMult = 0.8f, speedMult = 1.2f
    ),
    GHOST_TOWN(
        "Ghost Town", "Gunslingers shoot from afar: outrange them",
        1.5f, 1.1f, 1.0f, 1.2f, 0xFFFFAB40, ZombieType.OUTLAW_KING,
        commonSpawns(runner = 1.2f, bloater = 0.8f, bat = 0.3f, balloon = 0f, clown = 0.3f) + signature(ZombieType.GUNSLINGER, 18f)
    ),
    CARNIVAL(
        "Carnival of Horrors", "Clowns split into minis when they pop; jugglers and strongmen",
        1.55f, 1.0f, 1.3f, 1.0f, 0xFFFF4081, ZombieType.CLOWN_PRINCE,
        commonSpawns(walker = 0.5f, runner = 0.8f, brute = 0.8f, bloater = 0.5f, bat = 0f, balloon = 1.5f, clown = 3f) +
            signature(ZombieType.JUGGLER, 15f) + signature(ZombieType.STRONGMAN, 45f, weight = 3f, perSecond = 1f / 80f)
    ),
    SEA_BRIDGE(
        "Sea Bridge", "Tentacles grab your wagons from below and sharks breach onto your roofs",
        1.6f, 1.15f, 0.9f, 1.15f, 0xFF29B6F6, ZombieType.MEGALODON,
        commonSpawns(walker = 0.9f, runner = 0.8f, bat = 1.5f, balloon = 0f, clown = 0.2f) + signature(ZombieType.DROWNED, 8f) +
            signature(ZombieType.LEAPING_SHARK, 30f, weight = 3f, perSecond = 1f / 80f),
        hazards = listOf(HazardKind.TENTACLES)
    ),
    CITY_RUINS(
        "City Ruins", "Zombies drop from overpasses; riot zombies carry shields",
        1.55f, 1.2f, 1.1f, 1.2f, 0xFF90A4AE, ZombieType.JUGGERNAUT,
        commonSpawns(runner = 1.2f, brute = 1.4f, bloater = 0.8f, bat = 0.5f, balloon = 0f, clown = 0.2f) + signature(ZombieType.RIOT, 25f),
        hazards = listOf(HazardKind.OVERPASS_DROPS)
    ),
    DEEP_SEA(
        "Deep Sea Tube", "A pressure tunnel on the ocean floor: piranha swarms, jellyfish, leaks and kraken tentacles",
        1.7f, 1.2f, 1.2f, 1.15f, 0xFF00ACC1, ZombieType.ANGLERFISH,
        commonSpawns(walker = 0.5f, runner = 0.6f, brute = 0.8f, bloater = 1f, bat = 0f, balloon = 0f, clown = 0f) +
            signature(ZombieType.PIRANHA, 8f, weight = 6f) + signature(ZombieType.JELLYFISH, 25f, weight = 3f, perSecond = 1f / 80f) +
            signature(ZombieType.DEEP_CRAB, 20f, weight = 4f) + signature(ZombieType.SHARK, 18f, weight = 3f, perSecond = 1f / 60f),
        gravityMult = 0.6f, speedMult = 0.9f, hazards = listOf(HazardKind.HULL_BREACH, HazardKind.TENTACLES)
    ),
    SOLAR(
        "Solar Rail", "Right next to the sun: fire-proof enemies and flares that burn the train unless you raise the shields",
        1.8f, 1.3f, 1.1f, 1.3f, 0xFFFFCA28, ZombieType.SUN_KING,
        commonSpawns(walker = 0.6f, runner = 1f, brute = 1.2f, bloater = 0.8f, bat = 0f, balloon = 0f, clown = 0f) +
            signature(ZombieType.EMBER, 8f) + signature(ZombieType.SOLAR_SPRITE, 22f, weight = 4f) +
            signature(ZombieType.SUN_PRIEST, 35f, weight = 3f, perSecond = 1f / 80f),
        hazards = listOf(HazardKind.SOLAR_FLARES)
    ),
    TWIN_BRIDGE(
        "Twin Bridge (CO-OP)", "Co-op only: parallel tracks over a gorge. Guns jam - your partner must clear them. The Twin Bulwark needs both trains.",
        2.0f, 1.2f, 1.2f, 1.2f, 0xFF26C6DA, ZombieType.TWIN_BULWARK,
        commonSpawns(walker = 0.7f, runner = 1f, brute = 1.2f, bloater = 0.6f, bat = 0.8f, balloon = 0.5f, clown = 0.3f) +
            signature(ZombieType.STRIKER, 10f, weight = 4f, perSecond = 1f / 60f),
        hazards = listOf(HazardKind.SIGNAL_JAM), coopOnly = true
    ),
    ECHO_MINE(
        "Echo Mine (CO-OP)", "Co-op only: a mine with a crushing gate. Your partner must open it. The Mirror Warden only falls to your partner's fire.",
        2.0f, 1.3f, 1.3f, 1.2f, 0xFFAB47BC, ZombieType.MIRROR_WARDEN,
        commonSpawns(walker = 0.8f, runner = 0.8f, brute = 1f, bloater = 0.8f, bat = 2f, balloon = 0f, clown = 0.2f) +
            signature(ZombieType.RAT, 8f, weight = 5f) + signature(ZombieType.STRIKER, 25f, weight = 3f, perSecond = 1f / 70f),
        hazards = listOf(HazardKind.GATE_LOCK), coopOnly = true
    );

    /** True when the route only makes sense with a partner connected. */
    val needsPartner: Boolean get() = coopOnly

    /** The enemies listed as this route's own (shown on junction cards and in the Field Guide). */
    val signatureEnemies: List<ZombieType> get() = spawns.filter { it.signature }.map { it.type }
}
