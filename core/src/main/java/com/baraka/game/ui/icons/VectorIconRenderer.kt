package com.baraka.game.ui.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** The one place a [VectorIcon] descriptor turns into an actual draw call. */
fun DrawScope.drawVectorIcon(
    icon: VectorIcon,
    center: Offset,
    radius: Float,
    color: Color,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 1.5f
) {
    when (icon) {
        is VectorIcon.Polygon -> drawPolygon(center, radius, icon.sides, color, outlineColor, outlineWidth)
        is VectorIcon.Diamond -> drawDiamond(
            center,
            halfWidth = radius,
            halfHeight = radius * icon.aspectRatio,
            color = color,
            outlineColor = outlineColor,
            outlineWidth = outlineWidth
        )
        is VectorIcon.Star -> drawStar(
            center,
            outerRadius = radius,
            innerRadius = radius * icon.innerRadiusRatio,
            points = icon.points,
            color = color,
            outlineColor = outlineColor,
            outlineWidth = outlineWidth
        )
        VectorIcon.Shield -> drawShield(center, radius, color, outlineColor, outlineWidth)
        is VectorIcon.Blade -> drawBlade(center, radius, icon.lengthRatio, color, outlineColor, outlineWidth)
        is VectorIcon.Axe -> drawAxe(center, radius, icon.lengthRatio, color, outlineColor, outlineWidth)
        VectorIcon.Heart -> drawHeart(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Duck -> drawDuck(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.TurtleShell -> drawTurtleShell(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.FryingPan -> drawFryingPan(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Boomerang -> drawBoomerang(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Magnet -> drawMagnet(center, radius, outlineColor, outlineWidth)
        VectorIcon.Firecracker -> drawFirecracker(center, radius, outlineColor, outlineWidth)
        VectorIcon.Egg -> drawEgg(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Slime -> drawSlime(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Ghost -> drawGhost(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Imp -> drawImp(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Golem -> drawGolem(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Mimic -> drawMimic(center, radius, color, outlineColor, outlineWidth)
        VectorIcon.Bolt -> drawBolt(center, radius, color, outlineColor, outlineWidth)
        is VectorIcon.Face -> drawFace(center, radius, color, icon, outlineColor, outlineWidth)
    }
}
