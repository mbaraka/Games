package com.baraka.game.tunnelcrew.model

import com.baraka.game.tunnelcrew.systems.Terrain

/**
 * One puzzle. Terrain is drawn by [build] with `dirt` / `steel` / `air` rectangles on a 320x176
 * pixel world; a crew member is 8 px tall, steps up 3 px, and dies from a fall of more than 64 px.
 * [exit] and every entry of [hazards] are `[x, y, w, h]` boxes tested against a crew member's feet.
 *
 * To add a level, append to [LEVELS]; the level select grid, progress saving and debug jump list
 * follow the list.
 */
/** How hard a level is meant to be, shown on its card (same names as the classic game). */
enum class Rating(val label: String, val color: Long) {
    FUN("Fun", 0xFF69F0AE),
    TRICKY("Tricky", 0xFFFFD54F),
    TAXING("Taxing", 0xFFFFA040),
    MAYHEM("Mayhem", 0xFFFF6E6E)
}

class Level(
    val name: String,
    val hint: String,
    val total: Int,
    val needed: Int,
    val timeSec: Int,
    val spawnX: Int,
    val spawnY: Int,
    val skills: IntArray,
    val exit: IntArray,
    val hazards: Array<IntArray> = emptyArray(),
    /** Timed flame jets: `[x, y, w, h, period, onTicks, phase]`; lethal to anything inside while on. */
    val traps: Array<IntArray> = emptyArray(),
    val rating: Rating = Rating.FUN,
    val spawnEvery: Int = 40,
    val theme: CrewTheme = CrewThemes.MEADOW,
    val build: (Terrain) -> Unit
)

private fun skills(vararg p: Pair<Skill, Int>): IntArray {
    val a = IntArray(Skill.entries.size)
    for ((s, n) in p) a[s.ordinal] = n
    return a
}

/** Steel walls at both world edges so the crew turns around instead of walking out of the level. */
private fun Terrain.sideWalls(top: Int, height: Int, right: Int = top, rightHeight: Int = height) {
    steel(0, top, 8, height)
    steel(312, right, 8, rightHeight)
}

val LEVELS: List<Level> = listOf(
    Level(
        name = "Dig Deep",
        rating = Rating.FUN,
        hint = "Pick Dig, then tap a crew member on the grass. Tunnel down into the cave where the exit is.",
        total = 8, needed = 6, timeSec = 180, spawnX = 40, spawnY = 60,
        skills = skills(Skill.DIGGER to 1),
        exit = intArrayOf(272, 112, 14, 18)
    ) { t ->
        t.dirt(0, 76, 320, 100)
        t.air(16, 100, 288, 30)
        t.sideWalls(36, 40)
    },
    Level(
        name = "Mind the Gap",
        rating = Rating.FUN,
        hint = "Builders lay 12 bricks of staircase. Assign one right at the edge of the cliff - pause to time it.",
        total = 8, needed = 6, timeSec = 240, spawnX = 36, spawnY = 70,
        skills = skills(Skill.BUILDER to 3),
        exit = intArrayOf(276, 82, 14, 18)
    ) { t ->
        t.dirt(0, 100, 110, 76)
        t.dirt(130, 100, 190, 76)
        t.sideWalls(60, 40)
    },
    Level(
        name = "Break Through",
        rating = Rating.FUN,
        hint = "A big dirt wall blocks the way. A Basher tunnels straight through it.",
        total = 10, needed = 8, timeSec = 240, spawnX = 36, spawnY = 100,
        skills = skills(Skill.BASHER to 2),
        exit = intArrayOf(276, 102, 14, 18)
    ) { t ->
        t.dirt(0, 120, 320, 56)
        t.dirt(140, 50, 70, 70)
        t.sideWalls(70, 50)
    },
    Level(
        name = "Cliffhanger",
        rating = Rating.FUN,
        hint = "Climbers scale walls, but the skill is permanent per crew member - you need 6 of them.",
        total = 10, needed = 6, timeSec = 240, spawnX = 36, spawnY = 112,
        skills = skills(Skill.CLIMBER to 6),
        exit = intArrayOf(278, 52, 14, 18)
    ) { t ->
        t.dirt(0, 130, 320, 46)
        t.dirt(150, 70, 170, 60)
        t.steel(0, 90, 8, 40)
        t.steel(312, 30, 8, 40)
    },
    Level(
        name = "Parachute Drop",
        rating = Rating.FUN,
        hint = "The drop is far too long. Give Float to everyone who walks off - Block turns the rest back, Bomb removes the blocker.",
        total = 10, needed = 6, timeSec = 300, spawnX = 30, spawnY = 22,
        skills = skills(Skill.FLOATER to 6, Skill.BLOCKER to 1, Skill.BOMBER to 1),
        exit = intArrayOf(276, 132, 14, 18)
    ) { t ->
        t.dirt(0, 40, 110, 16)
        t.dirt(0, 150, 320, 26)
        t.steel(0, 4, 8, 36)
        t.steel(312, 110, 8, 40)
    },
    Level(
        name = "Slope Dive",
        rating = Rating.FUN,
        hint = "Miners tunnel diagonally. Everyone can follow the slope down into the sealed cave.",
        total = 8, needed = 6, timeSec = 300, spawnX = 36, spawnY = 44,
        skills = skills(Skill.MINER to 2),
        exit = intArrayOf(272, 142, 14, 18)
    ) { t ->
        t.dirt(0, 60, 320, 116)
        t.air(180, 120, 120, 40)
        t.sideWalls(20, 40)
    },
    Level(
        name = "Three Tiers",
        rating = Rating.TRICKY,
        hint = "The crew zig-zags down three floors. Bash the wall on the middle floor, bridge the pit on the bottom floor.",
        total = 12, needed = 9, timeSec = 300, spawnX = 24, spawnY = 34,
        skills = skills(Skill.BASHER to 2, Skill.BUILDER to 3),
        exit = intArrayOf(280, 132, 14, 18),
        theme = CrewThemes.DUSK
    ) { t ->
        t.dirt(0, 50, 296, 12)
        t.dirt(24, 100, 296, 12)
        t.dirt(0, 150, 150, 26)
        t.dirt(170, 150, 150, 26)
        t.dirt(150, 62, 24, 38)
        t.steel(0, 10, 8, 40)
        t.steel(312, 60, 8, 40)
        t.steel(0, 110, 8, 40)
        t.steel(312, 110, 8, 40)
    },
    Level(
        name = "Lava Lake",
        rating = Rating.TRICKY,
        hint = "Far too wide for one builder. Re-assign Build the moment a builder finishes his last brick.",
        total = 10, needed = 7, timeSec = 360, spawnX = 40, spawnY = 86,
        skills = skills(Skill.BUILDER to 4),
        exit = intArrayOf(278, 102, 14, 18),
        hazards = arrayOf(intArrayOf(100, 150, 64, 26)),
        theme = CrewThemes.VOLCANO
    ) { t ->
        t.dirt(0, 120, 100, 56)
        t.dirt(164, 120, 156, 56)
        t.sideWalls(80, 40)
    },
    Level(
        name = "Mountain Pass",
        rating = Rating.TRICKY,
        hint = "The first wall has a steel overhang, so climbers bounce off it. Bash through, then climb the cliff.",
        total = 10, needed = 6, timeSec = 300, spawnX = 30, spawnY = 120,
        skills = skills(Skill.BASHER to 1, Skill.CLIMBER to 6),
        exit = intArrayOf(278, 82, 14, 18)
    ) { t ->
        t.dirt(0, 140, 320, 36)
        t.dirt(80, 90, 30, 50)
        t.steel(76, 86, 38, 4)
        t.dirt(180, 100, 140, 40)
        t.steel(0, 100, 8, 40)
        t.steel(312, 60, 8, 40)
    },
    Level(
        name = "Grand Tour",
        rating = Rating.MAYHEM,
        hint = "Everything at once: bridge the top pit, bash the middle wall, then dig into the exit chamber under the floor.",
        total = 14, needed = 10, timeSec = 420, spawnX = 24, spawnY = 24,
        skills = skills(
            Skill.BUILDER to 3, Skill.BASHER to 2, Skill.DIGGER to 2,
            Skill.BLOCKER to 1, Skill.BOMBER to 2, Skill.MINER to 1
        ),
        exit = intArrayOf(280, 150, 14, 18),
        theme = CrewThemes.DUSK
    ) { t ->
        t.dirt(0, 40, 140, 12)
        t.dirt(160, 40, 136, 12)
        t.dirt(24, 92, 296, 12)
        t.dirt(60, 52, 24, 40)
        t.dirt(0, 144, 320, 32)
        t.air(200, 152, 100, 16)
        t.steel(0, 0, 8, 40)
        t.steel(312, 52, 8, 40)
        t.steel(0, 104, 8, 40)
        t.steel(312, 100, 8, 44)
    },
    Level(
        name = "Higher Ground",
        rating = Rating.TRICKY,
        hint = "The far side is 12 px higher. A builder's staircase climbs that much, so time it at the very edge.",
        total = 8, needed = 6, timeSec = 240, spawnX = 36, spawnY = 122,
        skills = skills(Skill.BUILDER to 3),
        exit = intArrayOf(276, 110, 14, 18)
    ) { t ->
        t.dirt(0, 140, 110, 36)
        t.dirt(130, 128, 190, 48)
        t.steel(0, 100, 8, 40)
        t.steel(312, 70, 8, 58)
    },
    Level(
        name = "Wrong Way",
        rating = Rating.TRICKY,
        hint = "The crew heads for a cliff and the exit is behind them! Block one of them near the edge to turn everyone around.",
        total = 10, needed = 8, timeSec = 300, spawnX = 70, spawnY = 104,
        skills = skills(Skill.BLOCKER to 1),
        exit = intArrayOf(14, 102, 14, 18),
        theme = CrewThemes.DUSK
    ) { t ->
        t.dirt(0, 120, 260, 56)
        t.steel(0, 70, 8, 50)
    },
    Level(
        name = "Blast Door",
        rating = Rating.TAXING,
        hint = "Stand a Blocker right beside the thin wall (within 7 px), then give that same member Bomb. The blast opens the door.",
        total = 10, needed = 8, timeSec = 300, spawnX = 36, spawnY = 104,
        skills = skills(Skill.BLOCKER to 1, Skill.BOMBER to 1),
        exit = intArrayOf(276, 102, 14, 18),
        theme = CrewThemes.VOLCANO
    ) { t ->
        t.dirt(0, 120, 320, 56)
        t.dirt(150, 30, 6, 90)
        t.steel(0, 70, 8, 50)
        t.steel(312, 70, 8, 50)
    },
    Level(
        name = "Layer Cake",
        rating = Rating.TRICKY,
        hint = "Two sealed caves, one below the other. Dig through the grass, then dig through the cave floor.",
        total = 8, needed = 6, timeSec = 300, spawnX = 40, spawnY = 24,
        skills = skills(Skill.DIGGER to 2),
        exit = intArrayOf(272, 122, 14, 18),
        theme = CrewThemes.DUSK
    ) { t ->
        t.dirt(0, 40, 320, 136)
        t.air(16, 60, 288, 30)
        t.air(16, 110, 288, 30)
        t.steel(0, 0, 8, 40)
        t.steel(312, 0, 8, 40)
    },
    Level(
        name = "Stepping Stones",
        rating = Rating.TAXING,
        hint = "Three gaps, three bridges. Block the crowd while you build so nobody walks off the edge.",
        total = 12, needed = 8, timeSec = 420, spawnX = 30, spawnY = 94, spawnEvery = 70,
        skills = skills(Skill.BUILDER to 4, Skill.BLOCKER to 2, Skill.BOMBER to 2),
        exit = intArrayOf(278, 92, 14, 18)
    ) { t ->
        t.dirt(0, 110, 80, 66)
        t.dirt(100, 110, 40, 66)
        t.dirt(160, 110, 40, 66)
        t.dirt(220, 110, 100, 66)
        t.steel(0, 70, 8, 40)
        t.steel(312, 70, 8, 40)
    },
    Level(
        name = "Armoured Wall",
        rating = Rating.TAXING,
        hint = "The wall is steel up high, but only dirt at the bottom. Bash low, then bridge the pit behind it.",
        total = 10, needed = 8, timeSec = 360, spawnX = 36, spawnY = 104,
        skills = skills(Skill.BASHER to 1, Skill.BUILDER to 2),
        exit = intArrayOf(276, 102, 14, 18),
        theme = CrewThemes.VOLCANO
    ) { t ->
        t.dirt(0, 120, 180, 56)
        t.dirt(200, 120, 120, 56)
        t.dirt(100, 108, 40, 12)
        t.steel(100, 40, 40, 68)
        t.steel(0, 70, 8, 50)
        t.steel(312, 70, 8, 50)
    },
    Level(
        name = "Up and Over",
        rating = Rating.TRICKY,
        hint = "Climb the cliff, then dig down through the plateau into the hidden cave with the exit.",
        total = 10, needed = 6, timeSec = 360, spawnX = 36, spawnY = 112,
        skills = skills(Skill.CLIMBER to 6, Skill.DIGGER to 1),
        exit = intArrayOf(276, 108, 14, 18)
    ) { t ->
        t.dirt(0, 130, 150, 46)
        t.dirt(150, 70, 170, 106)
        t.air(200, 100, 100, 26)
        t.steel(0, 90, 8, 40)
        t.steel(312, 30, 8, 40)
    },
    Level(
        name = "The Gauntlet",
        rating = Rating.MAYHEM,
        hint = "Bash under the steel overhang, bridge the pit, then climb the final cliff. Every skill has its moment.",
        total = 12, needed = 6, timeSec = 420, spawnX = 24, spawnY = 124,
        skills = skills(Skill.BASHER to 1, Skill.BUILDER to 2, Skill.CLIMBER to 6),
        exit = intArrayOf(278, 82, 14, 18),
        theme = CrewThemes.DUSK
    ) { t ->
        t.dirt(0, 140, 151, 36)
        t.dirt(171, 140, 149, 36)
        t.dirt(70, 90, 24, 50)
        t.steel(66, 86, 32, 4)
        t.dirt(220, 100, 100, 40)
        t.steel(0, 100, 8, 40)
        t.steel(312, 60, 8, 40)
    },
    Level(
        name = "One Way Street",
        rating = Rating.TAXING,
        hint = "Arrow dirt can only be bashed the way the arrows point. The first wall lets you through; the second needs a Blocker and a Bomb.",
        total = 10, needed = 7, timeSec = 360, spawnX = 36, spawnY = 104,
        skills = skills(Skill.BASHER to 1, Skill.BLOCKER to 1, Skill.BOMBER to 1),
        exit = intArrayOf(276, 102, 14, 18)
    ) { t ->
        t.dirt(0, 120, 320, 56)
        t.arrowDirt(80, 40, 30, 80, 1)
        t.arrowDirt(200, 40, 6, 80, -1)
        t.steel(0, 70, 8, 50)
        t.steel(312, 70, 8, 50)
    },
    Level(
        name = "Flame Alley",
        rating = Rating.TAXING,
        hint = "Flame jets fire on a timer and glow just before they ignite. Hold the crew back and let them through in the gaps.",
        total = 12, needed = 7, timeSec = 360, spawnX = 36, spawnY = 104, spawnEvery = 60,
        skills = skills(Skill.BLOCKER to 2, Skill.BOMBER to 2),
        exit = intArrayOf(276, 102, 14, 18),
        traps = arrayOf(intArrayOf(120, 100, 14, 20, 150, 45, 0), intArrayOf(200, 100, 14, 20, 150, 45, 80)),
        theme = CrewThemes.VOLCANO
    ) { t ->
        t.dirt(0, 120, 320, 56)
        t.steel(0, 70, 8, 50)
        t.steel(312, 70, 8, 50)
    }
)
