package com.baraka.game.trainsurvivor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.TrainShape
import com.baraka.game.trainsurvivor.model.ZombieType
import com.baraka.game.trainsurvivor.systems.TrainGame
import com.baraka.game.trainsurvivor.systems.Zombie

// The renderer is extended by registering art, never by editing the scene loop:
//   - an enemy that should not look like a plain zombie       -> MobArtRegistry   (render/MobArts.kt)
//   - a humanoid enemy that only needs accessories             -> HumanoidExtras   (render/MobArts.kt)
//   - a plain humanoid enemy with its own colours              -> MobLooks         (below)
//   - a route's scenery, weather, lighting and train gear      -> RouteArtRegistry (render/RouteArts.kt)
//   - a train body                                             -> TrainShapeArtRegistry (render/TrainShapes.kt)
// Anything left unregistered falls back to a sensible default, so new content never crashes the renderer.

/** Draws one enemy type. [dir] is +1 when it faces right, -1 when it faces left. */
fun interface MobArt {
    fun draw(scope: DrawScope, z: Zombie, dir: Float)
}

object MobArtRegistry {
    private val arts = HashMap<ZombieType, MobArt>()

    fun register(type: ZombieType, art: MobArt) {
        arts[type] = art
    }

    fun register(types: Collection<ZombieType>, art: MobArt) {
        for (type in types) arts[type] = art
    }

    operator fun get(type: ZombieType): MobArt? = arts[type]
}

/** Extra drawing for a humanoid enemy (accessories on the standard zombie body), in the body's local coordinates. */
fun interface HumanoidExtra {
    fun draw(scope: DrawScope, z: Zombie, headY: Float, bob: Float, skin: Color, shirt: Color)
}

object HumanoidExtras {
    private val extras = HashMap<ZombieType, HumanoidExtra>()

    fun register(type: ZombieType, extra: HumanoidExtra) {
        extras[type] = extra
    }

    fun draw(scope: DrawScope, z: Zombie, headY: Float, bob: Float, skin: Color, shirt: Color) {
        extras[z.type]?.draw(scope, z, headY, bob, skin, shirt)
    }
}

/** How big an enemy is drawn in the Field Guide portrait. Unlisted enemies use a standard humanoid size. */
object PortraitSizes {
    private val heights = HashMap<ZombieType, Float>()
    private val floatFraction = HashMap<ZombieType, Float>()

    /** [height] is the world height shown; [floating] (0..1) puts flyers that far down the frame instead of on the ground. */
    fun set(type: ZombieType, height: Float, floating: Float? = null) {
        heights[type] = height
        if (floating != null) floatFraction[type] = floating
    }

    fun heightOf(type: ZombieType): Float? = heights[type]
    fun floatingAt(type: ZombieType): Float? = floatFraction[type]
}

/** Skin and shirt colours of plain humanoid enemies (ARGB). Unlisted types use the walker's colours. */
object MobLooks {
    private class Look(val skin: Int, val shirt: Int)

    private val looks = HashMap<ZombieType, Look>()
    private val walkerShirts = intArrayOf(0xFF5D6D7E.toInt(), 0xFF6C5B3E.toInt(), 0xFF3F6C5B.toInt(), 0xFF7B4B4B.toInt())
    private val default = Look(0xFF86B864.toInt(), 0xFF5D6D7E.toInt())

    fun set(type: ZombieType, skin: Long, shirt: Long) {
        looks[type] = Look(skin.toInt(), shirt.toInt())
    }

    fun skin(type: ZombieType): Int = (looks[type] ?: default).skin

    fun shirt(type: ZombieType, seed: Float): Int =
        if (type == ZombieType.WALKER) walkerShirts[(seed * 4).toInt().coerceIn(0, 3)] else (looks[type] ?: default).shirt
}

/** How one route looks: scenery, weather, lighting and the gear the train sprouts while it is there. */
interface RouteArt {
    /** Sky/space, far scenery, ground and back scenery (drawn behind the rails). */
    fun backdrop(s: DrawScope, g: TrainGame, light: Float, sunset: Float)

    /** Scenery drawn in front of the action. */
    fun foreground(s: DrawScope, g: TrainGame, light: Float) {}

    /** Weather and particles over everything. */
    fun weather(s: DrawScope, g: TrainGame) {}

    /** Darkness overlay (0..1) given how dark the sky is outside (0 = day, 1 = night). */
    fun darkness(night: Float): Float = night * 0.5f

    /** How strongly lamps, lit windows and glowing eyes show (0 = not at all). */
    fun glowNight(night: Float): Float = night

    /** Lights drawn on top of the darkness overlay. */
    fun afterDarkness(s: DrawScope, g: TrainGame) {}

    /** Gear the train wears on this route; [alpha] fades it in and out while the train changes route. */
    fun trainKit(s: DrawScope, g: TrainGame, alpha: Float) {}
}

object RouteArtRegistry {
    private val arts = HashMap<Biome, RouteArt>()
    private var fallback: RouteArt? = null

    fun register(biome: Biome, art: RouteArt) {
        arts[biome] = art
    }

    fun setFallback(art: RouteArt) {
        fallback = art
    }

    /** The art for [biome]; routes without their own art use the plains look. */
    fun artFor(biome: Biome): RouteArt = arts[biome] ?: arts[Biome.PLAINS] ?: fallback!!
}

/** Draws a train body (locomotive, wagons, windows) in the player's current livery. */
interface TrainShapeArt {
    fun paintLoco(s: DrawScope, g: TrainGame, x0: Float)
    fun paintCar(s: DrawScope, g: TrainGame, x0: Float, index: Int, slot: Int)

    /** Window panes: called once with the glass colour and again with a warm glow colour at night. */
    fun paintWindows(s: DrawScope, g: TrainGame, color: Color)
}

object TrainShapeArtRegistry {
    private val arts = HashMap<TrainShape, TrainShapeArt>()

    fun register(shape: TrainShape, art: TrainShapeArt) {
        arts[shape] = art
    }

    /** The art for [shape]; shapes without art are drawn as the steam locomotive. */
    fun artFor(shape: TrainShape): TrainShapeArt = arts[shape] ?: arts[TrainShape.STEAM]!!
}
