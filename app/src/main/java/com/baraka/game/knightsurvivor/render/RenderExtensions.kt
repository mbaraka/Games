package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.geometry.Offset
import com.baraka.game.knightsurvivor.model.Vec2

/**
 * The one conversion point between the Android-free [Vec2] used throughout model/systems
 * and Compose's [Offset] used for actually drawing. Kept here, in render/, rather than on
 * Vec2 itself, so model/ stays free of Compose imports.
 */
fun Vec2.toOffset(): Offset = Offset(x, y)
