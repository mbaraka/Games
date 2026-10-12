package com.baraka.game.coop

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
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

private val BG = Color(0xFF12151C)
private val CARD = Color(0xFF1B1F29)
private val TEXT = Color(0xFFFFFFFF)
private val MUTED = Color(0xFFB0B8C6)
private val AMBER = Color(0xFFFFB300)
private val GOOD = Color(0xFF66BB6A)
private val BAD = Color(0xFFFF5252)
private val BUTTON = Color(0xFF2A3342)

/**
 * The lobby every game uses for two-phone co-op over Bluetooth: pair, host, join a paired phone, or practise against
 * a simulated partner. [heading] and [intro] describe the game's co-op; [practiceNote] says what the simulated partner does;
 * [simulate] builds it (a [SimulatedPartner]).
 */
@Composable
fun <M> CoopLobbyScreen(
    spec: CoopSpec<M>,
    heading: String,
    intro: String,
    practiceNote: String,
    simulate: () -> CoopLink<M>,
    onReady: (CoopLaunch<M>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var status by remember { mutableStateOf("Pair the two phones once in the system Bluetooth settings, then one phone hosts and the other joins.") }
    var hosting by remember { mutableStateOf<BluetoothHost<M>?>(null) }
    var joining by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var pairing by remember { mutableStateOf<BluetoothDevice?>(null) }
    var permitted by remember { mutableStateOf(hasBluetoothPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = hasBluetoothPermission(context)
        if (!permitted) status = "Bluetooth permission is needed to find and connect to your partner."
    }

    fun connect(device: BluetoothDevice) {
        val name = deviceName(device)
        status = "Connecting to $name..."
        CoopBluetooth.connectTo(spec, device) { link ->
            main.post {
                if (link != null) onReady(CoopLaunch(link, CoopSide.GUEST))
                else status = "Could not connect. Is $name hosting (Host pressed, and visible)?"
            }
        }
    }

    // While joining: find nearby phones (even unpaired ones), and connect as soon as a pairing request is accepted.
    DisposableEffect(scanning, pairing) {
        if (!scanning && pairing == null) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val device = deviceExtra(intent)
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> if (device != null && devices.none { it.address == device.address }) devices = devices + device
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scanning = false
                        if (pairing == null) status = if (devices.isEmpty()) "No phones found. On the host phone press Host and Make visible, then Scan again." else "Pick the phone that is hosting."
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val target = pairing
                        if (device != null && target != null && device.address == target.address) {
                            val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                            if (state == BluetoothDevice.BOND_BONDED) { pairing = null; connect(device) }
                            else if (state == BluetoothDevice.BOND_NONE) { pairing = null; status = "Pairing was cancelled or failed. Tap the phone to try again." }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    DisposableEffect(Unit) { onDispose { CoopBluetooth.stopScan() } }

    // The lobby is a list, so it reads best upright. Games that run in landscape get their orientation back on leaving.
    DisposableEffect(Unit) {
        val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        onDispose { if (previous != null) activity.requestedOrientation = previous }
    }

    fun startScan() {
        devices = CoopBluetooth.pairedDevices()
        scanning = CoopBluetooth.startScan()
        status = if (scanning) "Looking for nearby phones... (the host must press Host and Make visible)" else "Could not start scanning: showing paired phones only."
    }
    DisposableEffect(Unit) { onDispose { hosting?.cancel() } }
    BackHandler { hosting?.cancel(); onClose() }

    fun needBluetooth(): Boolean {
        if (!CoopBluetooth.hasAdapter()) { status = "This phone has no Bluetooth."; return false }
        if (!permitted) {
            permissionLauncher.launch(requiredPermissions())
            return false
        }
        if (!CoopBluetooth.isEnabled()) { status = "Turn Bluetooth on first."; return false }
        return true
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BG).windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(heading, color = TEXT, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            LobbyButton("Back", BUTTON, TEXT) { hosting?.cancel(); onClose() }
        }
        Text(intro, color = MUTED, fontSize = 12.sp)
        Text(status, color = AMBER, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        // Portrait lobby: the two steps on top, then a tall list of phones nearby, then practice.
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Step 1 (hosting phone): make it visible", color = TEXT, fontWeight = FontWeight.Bold)
                Text("The joining phone can only see this phone while it is visible, so press this on the host, then press Host below. You can also pair in the system Bluetooth settings.", color = MUTED, fontSize = 11.sp)
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
                                val host = BluetoothHost(spec)
                                hosting = host
                                joining = false
                                status = "Waiting for your partner to join..."
                                host.start { link ->
                                    main.post {
                                        hosting = null
                                        if (link != null) onReady(CoopLaunch(link, CoopSide.HOST)) else status = "Hosting stopped."
                                    }
                                }
                            }
                        }
                    } else {
                        LobbyButton("Stop waiting", BAD, Color.White) { hosting?.cancel(); hosting = null; status = "Hosting stopped." }
                    }
                    LobbyButton(if (joining) "Scan again" else "Join", AMBER, Color.Black) {
                        if (needBluetooth()) {
                            hosting?.cancel(); hosting = null
                            joining = true
                            startScan()
                        }
                    }
                }
            }
        }
        // Phones found nearby: one tall vertical list that takes all the spare height.
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Phones nearby", color = TEXT, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (!joining) {
                Text("Press Join on the phone that is NOT hosting. Phones it finds appear here.", color = MUTED, fontSize = 12.sp)
            } else {
                if (scanning) Text("Scanning...", color = MUTED, fontSize = 12.sp)
                else if (devices.isEmpty()) Text("Nothing found yet. Press Scan again.", color = MUTED, fontSize = 12.sp)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (device in devices) {
                        val paired = device.bondState == BluetoothDevice.BOND_BONDED
                        LobbyButton(deviceName(device) + if (paired) "  (paired)" else "  (tap to pair)", BUTTON, TEXT, Modifier.fillMaxWidth()) {
                            if (paired) connect(device)
                            else {
                                CoopBluetooth.stopScan()
                                scanning = false
                                pairing = device
                                status = "Pairing with ${deviceName(device)}: accept the code on BOTH phones..."
                                if (!device.createBond()) { pairing = null; status = "Could not start pairing. Pair in Bluetooth settings instead." }
                            }
                        }
                    }
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Practice on one phone", color = TEXT, fontWeight = FontWeight.Bold)
                Text(practiceNote, color = MUTED, fontSize = 11.sp)
                LobbyButton("Practice with a simulated partner", GOOD, Color.Black) { onReady(CoopLaunch(simulate(), CoopSide.HOST)) }
            }
    }
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) // needed to scan on Android 11 and older

private fun hasBluetoothPermission(context: Context): Boolean =
    requiredPermissions().all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

private fun deviceName(device: BluetoothDevice): String =
    try { device.name ?: device.address } catch (_: SecurityException) { device.address }

@Suppress("DEPRECATION")
private fun deviceExtra(intent: Intent): BluetoothDevice? =
    if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

@Composable
private fun LobbyButton(label: String, container: Color, content: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) { Text(label, fontWeight = FontWeight.Bold, maxLines = 1) }
}
