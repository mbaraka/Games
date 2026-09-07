package com.baraka.game.knightsurvivor.ui

/** Shared mm:ss formatting for the HUD timer and the end-screen recap, so the two never drift apart. */
fun formatSurvivalDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
