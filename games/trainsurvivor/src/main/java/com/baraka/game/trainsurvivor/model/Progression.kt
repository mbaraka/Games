package com.baraka.game.trainsurvivor.model

import java.time.LocalDate
import kotlin.math.max

enum class GoalType(val label: String, val perRun: Boolean) {
    KILLS("Kill %d zombies today", false),
    FLYERS("Shoot down %d flying enemies today", false),
    BOSSES("Defeat %d bosses today", false),
    DISTANCE("Travel %d m in a single run", true),
    COINS("Earn %d coins in a single run", true),
    ROUTES("Take %d route junctions today", false)
}

data class Goal(val type: GoalType, val target: Int, val reward: Int) {
    val title: String get() = type.label.format(target)
}

/** What a run achieved so far: deltas for "today" goals, totals for "single run" goals. */
class RunDelta(
    val kills: Int,
    val flyers: Int,
    val bosses: Int,
    val routes: Int,
    val runCoins: Int,
    val runDistance: Int
)

enum class Milestone(val title: String, val description: String, val reward: Int, val isDone: (SaveData) -> Boolean) {
    FIRST_BLOOD("First Blood", "Kill your first zombie", 50, { it.totalKills >= 1 }),
    HUNTER("Zombie Hunter", "Kill 500 zombies", 200, { it.totalKills >= 500 }),
    EXTERMINATOR("Exterminator", "Kill 5,000 zombies", 1000, { it.totalKills >= 5000 }),
    BOSS_SLAYER("Boss Slayer", "Defeat a boss", 150, { it.bossKills >= 1 }),
    BOSS_HUNTER("Boss Hunter", "Defeat 10 bosses", 800, { it.bossKills >= 10 }),
    SKY_GUARD("Sky Guard", "Shoot down 100 flying enemies", 300, { it.flyerKills >= 100 }),
    ROAD_TRIP("Road Trip", "Reach 500 m in one run", 100, { it.bestDistance >= 500 }),
    LONG_HAUL("Long Haul", "Reach 2,000 m in one run", 500, { it.bestDistance >= 2000 }),
    CROSS_COUNTRY("Cross Country", "Reach 5,000 m in one run", 2000, { it.bestDistance >= 5000 }),
    FULL_HOUSE("Full House", "Unlock all 5 seats", 300, { it.unlockedSlots >= MAX_SLOTS }),
    ARSENAL("Armed to the Teeth", "Own 5 weapons on one passenger", 400, { s -> s.slots.any { it.ownedWeapons.size >= 5 } }),
    NAVIGATOR("Navigator", "Take 25 route junctions", 250, { it.routesTaken >= 25 }),
    VETERAN("Veteran", "Prestige for the first time", 1000, { it.prestige >= 1 })
}

object Progression {
    private val targets = mapOf(
        GoalType.KILLS to intArrayOf(150, 300, 500),
        GoalType.FLYERS to intArrayOf(20, 40, 70),
        GoalType.BOSSES to intArrayOf(1, 2, 3),
        GoalType.DISTANCE to intArrayOf(800, 1500, 2500),
        GoalType.COINS to intArrayOf(400, 900, 1800),
        GoalType.ROUTES to intArrayOf(2, 4, 6)
    )

    fun today(): Long = LocalDate.now().toEpochDay()

    /** Three goals, the same for everyone on a given day. */
    fun goalsForDay(day: Long): List<Goal> {
        val random = java.util.Random(day * 7919L + 13L)
        return GoalType.entries.shuffled(random).take(3).map { type ->
            val tier = random.nextInt(3)
            Goal(type, targets.getValue(type)[tier], 120 + tier * 130 + random.nextInt(60))
        }
    }

    /** Starts a fresh day of goals when the date has changed. */
    fun sync(save: SaveData, day: Long = today()): SaveData =
        if (save.goalDay == day) save else save.copy(goalDay = day, goalProgress = listOf(0, 0, 0), goalClaimed = 0)

    fun applyProgress(save: SaveData, delta: RunDelta, day: Long = today()): SaveData {
        val synced = sync(save, day)
        val progress = goalsForDay(day).mapIndexed { i, goal ->
            val current = synced.goalProgress.getOrElse(i) { 0 }
            when (goal.type) {
                GoalType.KILLS -> current + delta.kills
                GoalType.FLYERS -> current + delta.flyers
                GoalType.BOSSES -> current + delta.bosses
                GoalType.ROUTES -> current + delta.routes
                GoalType.DISTANCE -> max(current, delta.runDistance)
                GoalType.COINS -> max(current, delta.runCoins)
            }
        }
        return synced.copy(goalProgress = progress)
    }

    fun canClaimGoal(save: SaveData, index: Int, day: Long = today()): Boolean {
        val synced = sync(save, day)
        val goal = goalsForDay(day).getOrNull(index) ?: return false
        return synced.goalProgress[index] >= goal.target && synced.goalClaimed and (1 shl index) == 0
    }

    fun claimGoal(save: SaveData, index: Int, day: Long = today()): SaveData? {
        if (!canClaimGoal(save, index, day)) return null
        val synced = sync(save, day)
        return synced.copy(coins = synced.coins + goalsForDay(day)[index].reward, goalClaimed = synced.goalClaimed or (1 shl index))
    }

    fun canClaimMilestone(save: SaveData, milestone: Milestone): Boolean =
        milestone.isDone(save) && save.milestonesClaimed and (1 shl milestone.ordinal) == 0

    fun claimMilestone(save: SaveData, milestone: Milestone): SaveData? {
        if (!canClaimMilestone(save, milestone)) return null
        return save.copy(coins = save.coins + milestone.reward, milestonesClaimed = save.milestonesClaimed or (1 shl milestone.ordinal))
    }

    /** True when something is waiting to be claimed (drives the badge on the Goals button). */
    fun hasClaimable(save: SaveData, day: Long = today()): Boolean =
        (0 until 3).any { canClaimGoal(save, it, day) } || Milestone.entries.any { canClaimMilestone(save, it) }

    fun addRecord(save: SaveData, record: RunRecord): SaveData =
        save.copy(records = (save.records + record).sortedByDescending { it.distance }.take(5))

    // ---- prestige ----
    fun prestigeRequirement(save: SaveData): Int = 1500 + 1000 * save.prestige

    fun canPrestige(save: SaveData): Boolean = save.bestDistance >= prestigeRequirement(save)

    /** Resets coins, upgrades, weapons and seats for a permanent +10% coin bonus; stats, goals and records stay. */
    fun prestige(save: SaveData): SaveData? {
        if (!canPrestige(save)) return null
        return save.copy(
            coins = 0,
            upgrades = emptyMap(),
            slots = List(MAX_SLOTS) { CrewSlot() },
            unlockedSlots = 2,
            prestige = save.prestige + 1
        )
    }
}
