package com.baraka.game.tunnelcrew.model

/**
 * Look of a level: sky, backdrop silhouettes, and the colours the terrain bitmap is painted with.
 * Pure data so the terrain, the renderer and the level-select previews all read the same palette.
 * To add a look, add an entry to [CrewThemes] and set it on a [Level]; nothing else changes.
 */
class CrewTheme(
    val name: String,
    val skyTop: Long,
    val skyBottom: Long,
    val sun: Long,
    val farHills: Long,
    val nearHills: Long,
    val cloud: Long,
    val dirt: IntArray,
    val dirtDeep: Int,
    val grass: IntArray,
    val rim: Int,
    val frame: Long
)

object CrewThemes {
    val MEADOW = CrewTheme(
        name = "Meadow",
        skyTop = 0xFF4FA3E0, skyBottom = 0xFFBDE6F7, sun = 0xFFFFF3B0,
        farHills = 0xFF8FC9A5, nearHills = 0xFF6DB38A, cloud = 0xFFFFFFFF,
        dirt = intArrayOf(0xFFB07A48.toInt(), 0xFFA66F3E.toInt(), 0xFFBA8452.toInt(), 0xFFA0693A.toInt()),
        dirtDeep = 0xFF6F4526.toInt(),
        grass = intArrayOf(0xFF6CCB52.toInt(), 0xFF5DBA45.toInt(), 0xFF7BD861.toInt(), 0xFF55B03F.toInt()),
        rim = 0xFF4A2F18.toInt(), frame = 0xFF0E1620
    )
    val VOLCANO = CrewTheme(
        name = "Volcano",
        skyTop = 0xFF3B1620, skyBottom = 0xFFE0703A, sun = 0xFFFFD27A,
        farHills = 0xFF5A2A2A, nearHills = 0xFF3F1C1E, cloud = 0xFF7A3B34,
        dirt = intArrayOf(0xFF6B4B45.toInt(), 0xFF624440.toInt(), 0xFF74524B.toInt(), 0xFF5C3F3A.toInt()),
        dirtDeep = 0xFF35201E.toInt(),
        grass = intArrayOf(0xFFB5543A.toInt(), 0xFFA64A33.toInt(), 0xFFC25F42.toInt(), 0xFF9C432E.toInt()),
        rim = 0xFF1E0F0E.toInt(), frame = 0xFF1A0C0E
    )
    val DUSK = CrewTheme(
        name = "Dusk",
        skyTop = 0xFF2B2A63, skyBottom = 0xFFE59BB8, sun = 0xFFFFE6F0,
        farHills = 0xFF6A5C9E, nearHills = 0xFF4C4283, cloud = 0xFFF4C7DA,
        dirt = intArrayOf(0xFF8B6A7E.toInt(), 0xFF826176.toInt(), 0xFF947388.toInt(), 0xFF7A586D.toInt()),
        dirtDeep = 0xFF4B3548.toInt(),
        grass = intArrayOf(0xFF86D8B0.toInt(), 0xFF76C9A1.toInt(), 0xFF94E4BD.toInt(), 0xFF6CC096.toInt()),
        rim = 0xFF2A1B2B.toInt(), frame = 0xFF14122B
    )
}
