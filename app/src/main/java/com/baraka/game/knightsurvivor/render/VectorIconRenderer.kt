package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.model.VectorIcon

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
        is VectorIcon.Face -> drawFace(center, radius, color, icon, outlineColor, outlineWidth)
    }
}
