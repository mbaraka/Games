package com.baraka.game.numbercorridor.model

/**
 * A purely cosmetic choice of how each squad member looks — same role as
 * knightsurvivor's KnightDesign. Picked before a run starts (see ui/AvatarPickerOverlay.kt),
 * persisted by data/CorridorSettingsRepository.kt, and rendered by render/AvatarRenderer.kt.
 */
enum class AvatarDesign(val displayName: String, val description: String, val colorArgb: Long) {
    RANGER("Ranger", "Green fatigues, steady rifle.", 0xFF4CAF50),
    COMMANDO("Commando", "Red beret, twin-mag SMG.", 0xFFE53935),
    RONIN("Ronin", "Violet coat, plasma carbine.", 0xFF8E24AA),
    VANGUARD("Vanguard", "Gold armor plating, heavy cannon.", 0xFFFFB300)
}
