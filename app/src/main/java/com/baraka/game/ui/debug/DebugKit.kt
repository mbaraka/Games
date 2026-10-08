package com.baraka.game.ui.debug

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Reusable developer tools for every game in the hub (see AGENTS.md: each game must ship a debug panel).
 *
 * A game describes its cheats as [DebugSection]s of [DebugItem]s and shows them with [DebugPanel]; the
 * panel itself, the unlock logic and the [FpsOverlay] are shared. Tools are visible in debuggable builds
 * and in release builds only after the player taps the version label seven times in Settings.
 */
object DebugKit {
    private const val PREFS = "debug_kit"
    private const val KEY_UNLOCKED = "developer_unlocked"

    fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun isUnlocked(context: Context): Boolean =
        isDebuggable(context) || context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_UNLOCKED, false)

    fun unlock(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_UNLOCKED, true).apply()
    }
}

sealed interface DebugItem {
    class Action(val label: String, val onClick: () -> Unit) : DebugItem
    class Toggle(val label: String, val checked: Boolean, val onChange: (Boolean) -> Unit) : DebugItem
    class Info(val text: String) : DebugItem
}

class DebugSection(val title: String, val items: List<DebugItem>)

private val PANEL = Color(0xFF1B1F29)
private val CHIP = Color(0xFF2A3342)

/** Full-screen overlay listing every [DebugSection]. Call it from a game's settings screen or pause menu. */
@Composable
fun DebugPanel(title: String, sections: List<DebugSection>, onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxHeight(0.92f)
                .background(PANEL, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$title - debug tools", color = Color(0xFFFFB300), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = CHIP, contentColor = Color.White)) { Text("Close") }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (section in sections) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(section.title.uppercase(), color = Color(0xFF9AA3B2), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    }
                    for (item in section.items) {
                        when (item) {
                            is DebugItem.Action -> item {
                                Button(
                                    onClick = item.onClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = CHIP, contentColor = Color.White)
                                ) { Text(item.label, fontSize = 12.sp, maxLines = 2) }
                            }
                            is DebugItem.Toggle -> item {
                                Row(
                                    Modifier.fillMaxWidth().background(CHIP, RoundedCornerShape(20.dp)).padding(start = 14.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Switch(checked = item.checked, onCheckedChange = item.onChange)
                                }
                            }
                            is DebugItem.Info -> item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(item.text, color = Color(0xFFB0BEC5), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Rolling frame-time statistics; [record] allocates nothing, so it is safe to call every frame. */
class FrameStats(private val capacity: Int = 120) {
    private val frameMs = FloatArray(capacity)
    private var count = 0
    private var index = 0

    fun record(ms: Float) {
        frameMs[index] = ms
        index = (index + 1) % capacity
        if (count < capacity) count++
    }

    fun summary(): String {
        if (count == 0) return "-- fps"
        var sum = 0f
        var worst = 0f
        for (i in 0 until count) {
            sum += frameMs[i]
            worst = max(worst, frameMs[i])
        }
        val avg = sum / count
        return "${(1000f / max(avg, 0.1f)).toInt()} fps  |  ${"%.1f".format(avg)} ms avg  |  ${"%.0f".format(worst)} ms worst"
    }
}

/** Small corner label showing live FPS from [stats]; refreshes four times a second. */
@Composable
fun FpsOverlay(stats: FrameStats, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(stats.summary()) }
    LaunchedEffect(stats) {
        while (true) {
            delay(250)
            text = stats.summary()
        }
    }
    Text(
        text,
        color = Color(0xFF69F0AE),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
