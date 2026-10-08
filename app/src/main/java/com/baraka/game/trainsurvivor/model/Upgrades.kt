package com.baraka.game.trainsurvivor.model

/** Everything a passenger can carry, weakest to strongest. Damage is per shot (per pellet-group for the shotgun). */
enum class WeaponType(
    val displayName: String,
    val damage: Float,
    val interval: Float,
    val range: Float,
    val splashRadius: Float,
    val pierce: Int,
    val unlockCost: Int,
    val blurb: String,
    val groundOnly: Boolean = false,
    /** Heat added per shot (1.0 is full). Rapid-fire weapons overheat and lock out until they cool. */
    val heatPerShot: Float = 0f
) {
    PISTOL("Pistol", 10f, 0.55f, 330f, 0f, 1, 0, "Reliable sidearm"),
    SMG("SMG", 7f, 0.16f, 340f, 0f, 1, 150, "Rapid fire, great against weak swarms", heatPerShot = 0.05f),
    SHOTGUN("Shotgun", 45f, 0.9f, 230f, 70f, 1, 400, "Hits groups up close"),
    RIFLE("Sniper Rifle", 60f, 0.85f, 560f, 0f, 3, 900, "Long range, pierces 3"),
    MINIGUN("Minigun", 13f, 0.07f, 400f, 0f, 1, 2000, "Bullet hose that overheats", heatPerShot = 0.03f),
    CANNON("Cannon", 180f, 1.6f, 480f, 120f, 1, 4500, "Explosive shells (ground only)", groundOnly = true),
    ROCKET("Rocket Launcher", 190f, 1.9f, 540f, 150f, 1, 8000, "Big blasts, hits flyers too"),
    LASER("Laser Rifle", 42f, 0.22f, 620f, 0f, 5, 14000, "Piercing beam, very long range, overheats", heatPerShot = 0.1f)
}

enum class TrainUpgrade(
    val displayName: String,
    val blurb: String,
    val maxLevel: Int,
    val baseCost: Int,
    val growth: Double
) {
    ARMOR("Armor Plating", "+40 train health and +25 per wagon, per level", 10, 80, 1.5),
    REPAIR("Repair Crew", "Mechanics heal the train and its wagons over time", 8, 100, 1.55),
    SPIKES("Spiked Hull", "Zombies clinging to the train take damage", 8, 90, 1.55),
    BOUNTY("Bounty Hunter", "+12% coins from kills per level", 8, 120, 1.6),
    DYNAMITE("Dynamite", "Active: bigger blast, faster cooldown", 6, 150, 1.7),
    FLAK("Flak Turret", "Roof turret that shoots down flying enemies and meteors", 8, 200, 1.6),
    TESLA("Tesla Coil", "Chain lightning zaps groups of zombies", 8, 250, 1.65),
    FLAME("Flamethrower", "Cowcatcher flames scorch zombies at the front", 8, 220, 1.6),
    MORTAR("Mortar", "Lobs shells at the biggest ground crowd", 8, 300, 1.65),
    OVERDRIVE("Overdrive", "Active: every gun fires much faster for a few seconds", 6, 350, 1.7),
    FIELD_REPAIR("Field Repair", "Active: heals every car and revives wrecks", 6, 300, 1.7)
}

/**
 * Difficulty chosen before a run. Higher levels make every zombie stronger and more numerous, add more
 * elites, and pay more coins. Each level unlocks by reaching [requirementMeters] on the level below it.
 */
enum class DangerLevel(
    val displayName: String,
    val hpMult: Float,
    val dmgMult: Float,
    val spawnMult: Float,
    val eliteMult: Float,
    val coinMult: Float,
    val requirementMeters: Int,
    val color: Long
) {
    NORMAL("Normal", 1f, 1f, 1f, 1f, 1f, 0, 0xFF8BC34A),
    HARD("Hard", 1.35f, 1.2f, 1.15f, 1.5f, 1.35f, 800, 0xFFFFD54F),
    BRUTAL("Brutal", 1.8f, 1.45f, 1.3f, 2.2f, 1.8f, 1200, 0xFFFF9800),
    NIGHTMARE("Nightmare", 2.5f, 1.8f, 1.5f, 3f, 2.4f, 1600, 0xFFFF5252),
    INFERNO("Inferno", 3.5f, 2.3f, 1.7f, 4f, 3.2f, 2000, 0xFFE040FB)
}
