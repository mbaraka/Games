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
    }
)
