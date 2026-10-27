package com.baraka.game.arcshot.model

/** An axis-aligned obstacle the ball must clear by arcing over, in world units. */
data class ArcRect(val x: Float, val y: Float, val w: Float, val h: Float)

/**
 * One Arc Shot puzzle. The world is [WORLD_W] x [WORLD_H] units; the launcher pivot always sits
 * at ([WORLD_W] * 0.08, [GROUND_Y]). [parShots] is the shot count a 3-star clear needs.
 */
data class ArcLevel(
    val name: String,
    val targetX: Float,
    val targetY: Float,
    val targetRadius: Float,
    val obstacles: List<ArcRect>,
    val gravity: Float = 900f,
    val wind: Float = 0f,
    val parShots: Int = 1
)

const val WORLD_W = 1000f
const val WORLD_H = 560f
const val GROUND_Y = 520f
val LAUNCHER_X = WORLD_W * 0.1f

val ARC_LEVELS: List<ArcLevel> = listOf(
    ArcLevel(
        "Easy Lob",
        targetX = 420f, targetY = GROUND_Y - 20f, targetRadius = 34f,
        obstacles = emptyList(),
        parShots = 1
    ),
    ArcLevel(
        "Long Shot",
        targetX = 820f, targetY = GROUND_Y - 20f, targetRadius = 32f,
        obstacles = emptyList(),
        parShots = 1
    ),
    ArcLevel(
        "Over the Wall",
        targetX = 520f, targetY = GROUND_Y - 20f, targetRadius = 32f,
        obstacles = listOf(ArcRect(320f, GROUND_Y - 150f, 36f, 150f)),
        parShots = 2
    ),
    ArcLevel(
        "The Gap",
        targetX = 560f, targetY = GROUND_Y - 20f, targetRadius = 30f,
        obstacles = listOf(
            ArcRect(260f, GROUND_Y - 260f, 30f, 260f),
            ArcRect(420f, 0f, 30f, 260f)
        ),
        parShots = 2
    ),
    ArcLevel(
        "High Perch",
        targetX = 600f, targetY = GROUND_Y - 220f, targetRadius = 30f,
        obstacles = listOf(ArcRect(600f - 60f, GROUND_Y - 220f + 30f, 120f, 190f)),
        parShots = 2
    ),
    ArcLevel(
        "Double Trouble",
        targetX = 760f, targetY = GROUND_Y - 20f, targetRadius = 28f,
        obstacles = listOf(
            ArcRect(300f, GROUND_Y - 130f, 34f, 130f),
            ArcRect(540f, GROUND_Y - 190f, 34f, 190f)
        ),
        parShots = 2
    ),
    ArcLevel(
        "Windy Ridge",
        targetX = 680f, targetY = GROUND_Y - 20f, targetRadius = 30f,
        obstacles = listOf(ArcRect(420f, GROUND_Y - 170f, 34f, 170f)),
        gravity = 900f, wind = 60f,
        parShots = 2
    ),
    ArcLevel(
        "Narrow Tunnel",
        targetX = 640f, targetY = GROUND_Y - 150f, targetRadius = 26f,
        obstacles = listOf(
            ArcRect(500f, 0f, 30f, GROUND_Y - 230f),
            ArcRect(500f, GROUND_Y - 80f, 30f, 80f)
        ),
        parShots = 3
    ),
    ArcLevel(
        "Low Gravity Leap",
        targetX = 880f, targetY = GROUND_Y - 260f, targetRadius = 30f,
        obstacles = listOf(ArcRect(500f, GROUND_Y - 120f, 34f, 120f)),
        gravity = 480f,
        parShots = 2
    ),
    ArcLevel(
        "The Gauntlet",
        targetX = 900f, targetY = GROUND_Y - 30f, targetRadius = 26f,
        obstacles = listOf(
            ArcRect(260f, GROUND_Y - 120f, 30f, 120f),
            ArcRect(460f, 0f, 30f, 260f),
            ArcRect(460f, GROUND_Y - 60f, 30f, 60f),
            ArcRect(700f, GROUND_Y - 200f, 30f, 200f)
        ),
        gravity = 950f, wind = -40f,
        parShots = 4
    )
)
