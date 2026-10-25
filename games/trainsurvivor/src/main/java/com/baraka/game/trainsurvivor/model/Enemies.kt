package com.baraka.game.trainsurvivor.model

/**
 * Every enemy in the game is one entry here: stats plus behaviour flags the engine understands.
 *
 * To add an enemy: add an entry (use the flags below for new behaviour), list it in a route's spawn table
 * ([Biome.spawns]), give it a Field Guide note, and - only if it should not look like a plain zombie -
 * register its art in render/MobArts.kt. Nothing else needs to change.
 */
enum class ZombieType(
    val displayName: String,
    val hp: Float,
    val speed: Float,
    val damage: Float,
    val coins: Int,
    val size: Float,
    val flying: Boolean = false,
    val boss: Boolean = false,
    /** Flat damage removed from every hit (never below 25% of it), so small bullets are weak against armored targets. */
    val armor: Float = 0f,
    /** How high above the feet shots aim; low creatures are hit near the ground. */
    val aimHeight: Float = 28f,
    /** Jumps onto a car's roof instead of queueing at the end of the train. */
    val leaper: Boolean = false,
    /** Distance from the train at which this enemy stops and shoots (0 = melee only). */
    val ranged: Float = 0f,
    /** Doesn't walk: grabs a car from below and holds on until killed. */
    val stationary: Boolean = false,
    /** Flames from the Flamethrower can't hurt it. */
    val fireImmune: Boolean = false,
    /** Seconds between this enemy's special moves (smash or summon); 0 = none. */
    val specialEvery: Float = 0f,
    /** Extra damage multiplier of a "smash" special while clawing a car (0 = no smash). */
    val smash: Float = 0f,
    /** Enemy summoned by the special move, and how many appear. */
    val summon: ZombieType? = null,
    val summonCount: Int = 0,
    /** Floating text shown when the special triggers. */
    val callout: String = "",
    /** Creeps, then lunges forward in short fast bursts (crocodile). */
    val lunges: Boolean = false,
    /** Flits around erratically instead of flying straight (bats, piranhas, sprites). */
    val erratic: Boolean = false,
    /** Spawns as a pack of this many. */
    val packSize: Int = 1,
    /** Co-op bosses: your own guns only scratch it (10%); your partner's fire cracks it. Solo it takes full damage. */
    val partnerOnly: Boolean = false,
    /** Jumps between the two tracks of a shared world, so a gap in your partner's defence becomes your problem. */
    val hops: Boolean = false,
    /** Throws the track switch when it reaches a train (and is gone). */
    val switcher: Boolean = false,
    /** An elemental enemy (see model/Elements.kt): takes more or less damage depending on the weapon's damage kind. */
    val element: Element? = null,
    /** Changes element every few seconds (the Prism Drake). */
    val shifts: Boolean = false
) {
    WALKER("Walker", 30f, 38f, 5f, 2, 1.0f),
    RUNNER("Runner", 18f, 88f, 4f, 3, 0.9f),
    BRUTE("Brute", 170f, 28f, 14f, 9, 1.45f, armor = 5f),
    BLOATER("Bloater", 55f, 36f, 0f, 5, 1.2f),
    BAT("Zombie Bat", 14f, 125f, 3f, 2, 0.8f, flying = true, erratic = true),
    BALLOON("Balloon Zombie", 45f, 42f, 5f, 6, 1.0f, flying = true),
    CLOWN("Clown", 70f, 62f, 7f, 6, 1.1f),

    // Bosses.
    TITAN("Titan", 1000f, 22f, 32f, 150, 2.4f, boss = true, armor = 12f, specialEvery = 5f, smash = 22f, callout = "SMASH!"),
    GARGOYLE("Gargoyle King", 900f, 55f, 26f, 180, 2.0f, flying = true, boss = true, armor = 9f, specialEvery = 7f, summon = BAT, summonCount = 3, callout = "SCREECH!"),
    CLOWN_PRINCE("Clown Prince", 1200f, 34f, 22f, 200, 2.1f, boss = true, armor = 8f, specialEvery = 8f, summon = CLOWN, summonCount = 2, callout = "TA-DA!"),

    // Route signature enemies.
    ZOMBIE_DOG("Zombie Hound", 28f, 105f, 5f, 3, 0.95f, aimHeight = 14f, packSize = 2),
    YETI("Yeti", 230f, 26f, 15f, 12, 1.55f, armor = 4f),
    RAT("Giant Rat", 8f, 120f, 2f, 1, 0.65f, aimHeight = 9f, packSize = 4),
    SCORPION("Sand Scorpion", 70f, 72f, 8f, 5, 1.1f, armor = 3f, aimHeight = 14f),
    SKELETON("Skeleton", 40f, 62f, 6f, 4, 1.0f),
    CROCODILE("Crocodile", 130f, 52f, 16f, 10, 1.35f, armor = 3f, aimHeight = 13f, lunges = true),
    LEAPER("Moon Leaper", 60f, 75f, 8f, 6, 1.0f, leaper = true),
    DRONE("Hunter Drone", 40f, 95f, 6f, 5, 0.9f, flying = true, armor = 3f, ranged = 260f, packSize = 2),
    MAGMA_GOLEM("Magma Golem", 210f, 24f, 15f, 14, 1.6f, armor = 5f, fireImmune = true),
    ICE_WOLF("Ice Wolf", 40f, 112f, 6f, 4, 1.0f, aimHeight = 14f, packSize = 2),
    FROST_WRAITH("Frost Wraith", 60f, 60f, 7f, 7, 1.0f, flying = true),
    GUNSLINGER("Gunslinger", 55f, 40f, 9f, 8, 1.0f, ranged = 300f),
    JUGGLER("Juggler", 50f, 45f, 6f, 7, 1.05f, ranged = 220f),
    STRONGMAN("Strongman", 190f, 30f, 14f, 11, 1.5f, armor = 5f),
    MINI_CLOWN("Mini Clown", 22f, 85f, 4f, 2, 0.7f),
    DROWNED("Drowned", 60f, 36f, 7f, 5, 1.0f),
    TENTACLE("Tentacle", 90f, 0f, 14f, 6, 1.2f, aimHeight = 20f, stationary = true),
    RIOT("Riot Zombie", 150f, 30f, 12f, 11, 1.2f, armor = 6f),

    // Deep Sea Tube.
    PIRANHA("Zombie Piranha", 12f, 130f, 3f, 2, 0.7f, flying = true, erratic = true, packSize = 3),
    JELLYFISH("Jellyfish Zombie", 70f, 36f, 6f, 7, 1.1f, flying = true),
    DEEP_CRAB("Deep Crab", 80f, 44f, 9f, 6, 1.1f, armor = 4f, aimHeight = 16f),
    SHARK("Zombie Shark", 150f, 85f, 12f, 12, 1.5f, flying = true, armor = 3f),
    LEAPING_SHARK("Breaching Shark", 120f, 70f, 14f, 11, 1.5f, armor = 2f, aimHeight = 20f, leaper = true),

    // Solar Rail.
    EMBER("Ember Zombie", 60f, 44f, 7f, 6, 1.0f, fireImmune = true),
    SOLAR_SPRITE("Solar Sprite", 35f, 105f, 6f, 5, 0.8f, flying = true, erratic = true, fireImmune = true),
    SUN_PRIEST("Sun Priest", 70f, 38f, 8f, 9, 1.05f, ranged = 280f, fireImmune = true),

    // Area bosses: each route names one in Biome.boss (Titan, Gargoyle King and Clown Prince above are bosses too).
    YETI_KING("Yeti King", 1300f, 22f, 28f, 170, 2.6f, boss = true, armor = 8f, specialEvery = 6f, smash = 26f, callout = "ROAR!"),
    RAT_KING("Rat King", 900f, 40f, 12f, 160, 2.4f, boss = true, armor = 4f, aimHeight = 20f, specialEvery = 5f, summon = RAT, summonCount = 4, callout = "SQUEAK!"),
    SCORPION_QUEEN("Scorpion Queen", 1100f, 36f, 22f, 170, 2.2f, boss = true, armor = 8f, aimHeight = 24f, specialEvery = 8f, summon = SCORPION, summonCount = 2, callout = "SKITTER!"),
    SWAMP_TYRANT("Swamp Tyrant", 1400f, 34f, 30f, 180, 2.2f, boss = true, armor = 8f, aimHeight = 24f, lunges = true, specialEvery = 6f, smash = 30f, callout = "CHOMP!"),
    LUNAR_COLOSSUS("Lunar Colossus", 1200f, 30f, 26f, 180, 2.3f, boss = true, armor = 7f, leaper = true, specialEvery = 6f, smash = 26f, callout = "SLAM!"),
    OVERSEER("Overseer", 1000f, 50f, 20f, 180, 2.4f, flying = true, boss = true, armor = 6f, ranged = 340f, specialEvery = 7f, summon = DRONE, summonCount = 2, callout = "DEPLOY!"),
    MAGMA_TITAN("Magma Titan", 1500f, 20f, 30f, 190, 2.6f, boss = true, armor = 12f, fireImmune = true, specialEvery = 5.5f, smash = 28f, callout = "ERUPT!"),
    FROST_QUEEN("Frost Queen", 950f, 48f, 22f, 180, 2.3f, flying = true, boss = true, armor = 6f, specialEvery = 7f, summon = FROST_WRAITH, summonCount = 2, callout = "FREEZE!"),
    OUTLAW_KING("Outlaw King", 1000f, 34f, 22f, 190, 2.1f, boss = true, armor = 6f, ranged = 330f, specialEvery = 9f, summon = GUNSLINGER, summonCount = 1, callout = "DRAW!"),
    MEGALODON("Megalodon", 1300f, 55f, 32f, 200, 2.8f, boss = true, armor = 8f, aimHeight = 20f, leaper = true, specialEvery = 6f, smash = 30f, callout = "BREACH!"),
    ANGLERFISH("Abyssal Angler", 1000f, 45f, 22f, 190, 2.8f, flying = true, boss = true, armor = 5f, specialEvery = 6f, summon = PIRANHA, summonCount = 4, callout = "FEAST!"),
    COMPY("Zombie Compy", 14f, 118f, 3f, 2, 0.7f, aimHeight = 9f, packSize = 5),
    RAPTOR("Zombie Raptor", 48f, 100f, 8f, 6, 1.0f, aimHeight = 18f, leaper = true, packSize = 2),
    SPITTER("Zombie Spitter", 60f, 40f, 7f, 8, 1.1f, aimHeight = 24f, ranged = 280f),
    PTERODACTYL("Zombie Pterodactyl", 66f, 78f, 7f, 9, 1.5f, flying = true),
    TRICERATOPS("Zombie Triceratops", 380f, 26f, 18f, 22, 1.9f, armor = 9f, aimHeight = 24f, lunges = true),
    BRONTOSAURUS("Zombie Brontosaurus", 1100f, 14f, 40f, 70, 3.4f, armor = 12f, aimHeight = 40f),
    TREX("Zombie T-Rex", 1900f, 22f, 34f, 280, 3.3f, boss = true, armor = 8f, aimHeight = 36f, lunges = true, specialEvery = 5.5f, smash = 38f, callout = "ROAR!"),
    HOPPER("Rail Hopper", 40f, 105f, 7f, 7, 1.0f, aimHeight = 26f, hops = true),
    SIGNALMAN("Signalman", 90f, 48f, 6f, 20, 1.1f, switcher = true),
    RUNAWAY_ENGINE("Runaway Engine", 1700f, 30f, 36f, 260, 2.6f, boss = true, armor = 10f, aimHeight = 34f, lunges = true, hops = true, specialEvery = 5f, smash = 36f, callout = "DERAIL!"),
    DEMOLISHER("Demolisher", 1700f, 20f, 32f, 230, 2.5f, boss = true, armor = 12f, specialEvery = 5f, smash = 34f, callout = "DEMOLISH!"),
    CINDER_PUP("Cinder Pup", 40f, 112f, 6f, 5, 0.95f, aimHeight = 14f, packSize = 3, element = Element.FIRE),
    TIDE_SLUG("Tide Slug", 170f, 20f, 12f, 14, 1.4f, aimHeight = 20f, element = Element.WATER),
    THORNBACK("Thornback", 260f, 30f, 14f, 18, 1.6f, armor = 5f, aimHeight = 26f, element = Element.GRASS),
    SPARK_HARE("Spark Hare", 30f, 135f, 5f, 6, 0.9f, aimHeight = 16f, leaper = true, element = Element.ELECTRIC),
    WISP("Wisp", 55f, 62f, 6f, 9, 1.0f, flying = true, erratic = true, element = Element.SPIRIT),
    BOULDER_BEETLE("Boulder Beetle", 340f, 24f, 16f, 20, 1.5f, armor = 8f, aimHeight = 22f, element = Element.EARTH),
    PRISM_DRAKE("Prism Drake", 1800f, 40f, 30f, 290, 2.8f, boss = true, armor = 6f, aimHeight = 36f, lunges = true, specialEvery = 6f, smash = 30f, callout = "PRISM!", shifts = true, element = Element.FIRE),
    STRIKER("Striker", 70f, 120f, 9f, 8, 1.0f, armor = 3f, aimHeight = 26f),
    TWIN_BULWARK("Twin Bulwark", 1500f, 22f, 30f, 230, 2.4f, boss = true, armor = 8f, specialEvery = 7f, summon = STRIKER, summonCount = 3, callout = "STRIKERS!", partnerOnly = true),
    MIRROR_WARDEN("Mirror Warden", 1300f, 30f, 24f, 230, 2.3f, boss = true, armor = 6f, ranged = 320f, specialEvery = 6f, summon = STRIKER, summonCount = 2, callout = "ECHO!", partnerOnly = true),
    SUN_KING("Sun King", 1100f, 36f, 24f, 210, 2.3f, boss = true, armor = 6f, ranged = 330f, fireImmune = true, specialEvery = 7f, summon = SOLAR_SPRITE, summonCount = 3, callout = "BURN!"),

    // Pyramid Valley.
    SCARAB("Scarab Swarm", 9f, 115f, 3f, 2, 0.6f, aimHeight = 9f, packSize = 5),
    MUMMY("Mummy", 95f, 26f, 9f, 7, 1.05f, armor = 4f),
    ANUBIS_GUARD("Anubis Guard", 120f, 42f, 10f, 9, 1.15f, armor = 3f, ranged = 260f),
    SPHINX("The Sphinx", 1450f, 24f, 28f, 220, 2.8f, boss = true, armor = 10f, aimHeight = 30f, specialEvery = 7f, summon = SCARAB, summonCount = 3, callout = "RIDDLE!")
}

/** A random upgrade some zombies spawn with; elites are tougher, worth 2.5x coins, and each trait calls for a different answer. */
enum class EliteTrait(val displayName: String, val color: Long, val blurb: String, val counter: String) {
    SHIELDED("Shielded", 0xFF40C4FF, "Wears an energy shield worth 60% of its health.", "Burst damage: Rocket, Cannon, Mortar, Dynamite."),
    REGEN("Regenerating", 0xFF69F0AE, "Heals quickly whenever you stop hitting it for a moment.", "Keep constant fire on it: Minigun, Tesla, Flamethrower."),
    SWIFT("Swift", 0xFFFFEB3B, "Moves 60% faster.", "Kill it early: Sniper and Laser reach far."),
    VOLATILE("Volatile", 0xFFFF7043, "Explodes when it dies, hurting the train if it is close.", "Kill it before it reaches the train."),
    ARMORED("Plated", 0xFFB0BEC5, "Wears extra armor: 8 less damage on every hit.", "Big hitters: Sniper, Rocket, Cannon, Laser."),
    BULLETPROOF("Bulletproof", 0xFFE040FB, "Guns deal only 30% damage to it.", "Use train weapons and abilities: Tesla, Flamethrower, Mortar, Flak, Dynamite.")
}
