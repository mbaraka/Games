package com.baraka.game.trainsurvivor.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.trainsurvivor.coop.BluetoothHost
import com.baraka.game.trainsurvivor.coop.BluetoothLink
import com.baraka.game.trainsurvivor.coop.CoopLink
import com.baraka.game.trainsurvivor.coop.SimulatedLink
import com.baraka.game.trainsurvivor.systems.CoopRole

/** A connected partner, ready to depart together. */
class CoopLaunch(val link: CoopLink, val role: CoopRole)

private val BG = Color(0xFF12151C)
private val CARD = Color(0xFF1B1F29)
private val TEXT = Color(0xFFFFFFFF)
private val MUTED = Color(0xFFB0B8C6)
private val AMBER = Color(0xFFFFB300)
private val GOOD = Color(0xFF66BB6A)
private val BAD = Color(0xFFFF5252)
private val BUTTON = Color(0xFF2A3342)

/** Lobby for two-phone co-op over Bluetooth: host, join a paired phone, or practise against a simulated partner. */
@Composable
fun CoopLobbyScreen(onReady: (CoopLaunch) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var status by remember { mutableStateOf("Pair the two phones once in the system Bluetooth settings, then one phone hosts and the other joins.") }
    var hosting by remember { mutableStateOf<BluetoothHost?>(null) }
    var joining by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var permitted by remember { mutableStateOf(hasBluetoothPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permitted = ok
        if (!ok) status = "Bluetooth permission is needed to connect to your partner."
    }
    DisposableEffect(Unit) { onDispose { hosting?.cancel() } }
    BackHandler { hosting?.cancel(); onClose() }

    fun needBluetooth(): Boolean {
        if (BluetoothAdapter.getDefaultAdapter() == null) { status = "This phone has no Bluetooth."; return false }
        if (!permitted) {
            if (Build.VERSION.SDK_INT >= 31) permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return false
        }
        if (!BluetoothLink.isAvailable()) { status = "Turn Bluetooth on first."; return false }
        return true
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BG).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("CO-OP  -  TWO TRAINS", color = TEXT, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            LobbyButton("Back", BUTTON, TEXT) { hosting?.cancel(); onClose() }
        }
        Text(
            "Each player drives their own train through double the zombies. Bosses and routes are shared. " +
                "Build the Gangway upgrade to lend a passenger to your partner's train when they are in trouble.",
            color = MUTED, fontSize = 12.sp
        )
        Text(status, color = AMBER, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Step 1: pair (once)", color = TEXT, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LobbyButton("Bluetooth settings", BUTTON, TEXT) { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                    LobbyButton("Make visible", BUTTON, TEXT) {
                        if (needBluetooth()) context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120))
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Step 2: one phone hosts, the other joins", color = TEXT, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hosting == null) {
                        LobbyButton("Host", AMBER, Color.Black) {
                            if (needBluetooth()) {
                                val host = BluetoothHost()
                                hosting = host
                                joining = false
                                status = "Waiting for your partner to join..."
                                host.start { link ->
                                    main.post {
                                        hosting = null
                                        if (link != null) onReady(CoopLaunch(link, CoopRole.HOST)) else status = "Hosting stopped."
                                    }
                                }
                            }
                        }
                    } else {
                        LobbyButton("Stop waiting", BAD, Color.White) { hosting?.cancel(); hosting = null; status = "Hosting stopped." }
                    }
                    LobbyButton("Join", AMBER, Color.Black) {
                        if (needBluetooth()) {
                            hosting?.cancel(); hosting = null
                            devices = BluetoothLink.pairedDevices()
                            joining = true
                            status = if (devices.isEmpty()) "No paired phones yet: pair in Bluetooth settings first." else "Pick the phone that is hosting."
                        }
                    }
                }
                if (joining) {
                    for (device in devices) {
                        val name = try { device.name ?: device.address } catch (_: SecurityException) { device.address }
                        LobbyButton(name, BUTTON, TEXT, Modifier.fillMaxWidth()) {
                            status = "Connecting to $name..."
                            BluetoothLink.connectTo(device) { link ->
                                main.post {
                                    if (link != null) onReady(CoopLaunch(link, CoopRole.GUEST))
                                    else status = "Could not connect. Is $name hosting?"
                                }
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Practice on one phone", color = TEXT, fontWeight = FontWeight.Bold)
                Text("A simulated partner lends you a passenger now and then, so you can try the co-op rules alone.", color = MUTED, fontSize = 11.sp)
                LobbyButton("Practice with a simulated partner", GOOD, Color.Black) { onReady(CoopLaunch(SimulatedLink(), CoopRole.HOST)) }
            }
        }
    }
}

private fun hasBluetoothPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

@Composable
private fun LobbyButton(label: String, container: Color, content: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) { Text(label, fontWeight = FontWeight.Bold, maxLines = 1) }
}
