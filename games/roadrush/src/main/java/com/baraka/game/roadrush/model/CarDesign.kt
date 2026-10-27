package com.baraka.game.roadrush.model

/**
 * A purely cosmetic choice of what the player's car looks like — same role as
 * numbercorridor's AvatarDesign. Picked before a run starts (see ui/CarPickerOverlay.kt),
 * persisted by data/RoadRushSettingsRepository.kt, and rendered by render/RoadRenderer.kt.
 */
enum class CarDesign(val displayName: String, val description: String, val colorArgb: Long) {
    CRIMSON("Crimson Bolt", "Fire-engine red, chrome spoiler.", 0xFFE53935),
    AZURE("Azure Streak", "Cool blue racer with a white stripe.", 0xFF1E88E5),
    VIPER("Viper Green", "Neon green with black racing stripes.", 0xFF43A047),
    SUNBURST("Sunburst", "Bright yellow taxi-cab paint job.", 0xFFFDD835)
}

/** The look of a rival car, picked per-car from [ObstacleCar.colorSeed] so the road doesn't look monotone. */
val RIVAL_CAR_COLORS = listOf(0xFF8E24AA, 0xFFFB8C00, 0xFF546E7A, 0xFF00897B)
