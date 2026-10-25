package com.baraka.game.keyreader

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.baraka.game.ui.theme.GameTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.Locale
import android.nfc.tech.Ndef

class KeyReaderActivity : ComponentActivity() {

    private val repository by lazy { KeyRepository.getInstance(applicationContext) }
    private val viewModel: KeyReaderViewModel by viewModels<KeyReaderViewModel>(
        factoryProducer = { KeyReaderViewModelFactory(repository) }
    )
    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameTheme {
                val uiState by viewModel.uiState.collectAsState()
                KeyReaderScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onStartReading = { startListening() },
                    onCancelReading = { stopListening(manualCancel = true) },
                    onRenameEntry = viewModel::renameEntry,
                    onDeleteEntry = viewModel::deleteEntry,
                    onSelectEntry = viewModel::selectEntry
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val shouldListen = viewModel.uiState.value.isListening
        if (shouldListen) startReaderMode()
    }

    override fun onPause() {
        super.onPause()
        stopReaderMode()
    }

    private fun startListening() {
        if (!ensureNfcAvailable()) {
            viewModel.onNfcUnavailable()
            return
        }
        viewModel.startListening()
        startReaderMode()
    }

    private fun stopListening(manualCancel: Boolean) {
        stopReaderMode()
        if (manualCancel) {
            viewModel.cancelListening()
        }
    }

    private fun startReaderMode() {
        val adapter = nfcAdapter ?: return
        adapter.enableReaderMode(
            this,
            ::onTagDiscovered,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    private fun stopReaderMode() {
        nfcAdapter?.disableReaderMode(this)
    }

    private fun ensureNfcAvailable(): Boolean {
        val adapter = nfcAdapter ?: return false
        if (!adapter.isEnabled) {
            showNfcDisabledDialog()
            return false
        }
        return true
    }

    private fun showNfcDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("NFC Disabled")
            .setMessage("Please enable NFC to read keys.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onTagDiscovered(tag: Tag) {
        lifecycleScope.launch {
            val rawDump = withContext(Dispatchers.IO) { dumpTag(tag) }
            viewModel.onTagRead(rawDump)
            stopReaderMode()
        }
    }

    private fun dumpTag(tag: Tag): String {
        val builder = StringBuilder()
        builder.appendLine("Tag discovered")
        builder.append("Id: ")
        val idBytes = tag.id
        builder.appendLine(idBytes?.joinToString(separator = " ") { eachByte ->
            "%02X".format(Locale.US, eachByte)
        } ?: "Unknown")

        val techs = tag.techList.joinToString()
        builder.appendLine("Techs: $techs")

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val message = ndef.ndefMessage
                if (message != null) {
                    builder.appendLine("NDEF message present (${message.records.size} records)")
                    message.records.forEachIndexed { index, record ->
                        builder.appendLine("Record #$index")
                        builder.appendLine("  Type: ${record.type.decodeToStringSafe()}")
                        builder.appendLine("  ID: ${record.id.decodeToStringSafe()}")
                        builder.appendLine("  Payload: ${record.payload.toHexString()}")
                        val textPayload = record.payload.decodeToStringSafe()
                        if (textPayload.isNotBlank()) {
                            builder.appendLine("  Text: $textPayload")
                        }
                    }
                } else {
                    builder.appendLine("NDEF message empty.")
                }
            } catch (error: Exception) {
                builder.appendLine("Error reading NDEF: ${error.message}")
            } finally {
                try {
                    ndef.close()
                } catch (_: Exception) {
                }
            }
        } else {
            builder.appendLine("NDEF not supported. Raw technologies will need manual parsing.")
        }

        return builder.toString()
    }

    private fun ByteArray?.decodeToStringSafe(): String {
        if (this == null || isEmpty()) return ""
        return try {
            String(this, Charset.forName("UTF-8"))
        } catch (_: Exception) {
            toHexString()
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = " ") { byte ->
            "%02X".format(Locale.US, byte)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, KeyReaderActivity::class.java)
    }
}
