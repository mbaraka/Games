package com.baraka.game.keyreader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyReaderScreen(
    uiState: KeyReaderUiState,
    onBack: () -> Unit,
    onStartReading: () -> Unit,
    onCancelReading: () -> Unit,
    onRenameEntry: (Long, String) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onSelectEntry: (Long?) -> Unit
) {
    var renameTarget by remember { mutableStateOf<KeyEntry?>(null) }
    var renameText by remember { mutableStateOf("") }
    var detailTarget by remember { mutableStateOf<KeyEntry?>(null) }

    LaunchedEffect(renameTarget) {
        renameText = renameTarget?.name.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Key Reader") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusSection(
                statusMessage = uiState.statusMessage,
                isListening = uiState.isListening
            )

            ActionButtons(
                isListening = uiState.isListening,
                onStartReading = onStartReading,
                onCancelReading = onCancelReading
            )

            uiState.lastReadData?.let { data ->
                LastReadCard(data = data)
            }

            SavedKeysSection(
                entries = uiState.entries,
                onSelect = { entry ->
                    detailTarget = entry
                    onSelectEntry(entry.id)
                },
                onRename = { entry ->
                    renameTarget = entry
                },
                onDelete = { entry ->
                    onDeleteEntry(entry.id)
                }
            )
        }
    }

    if (detailTarget != null) {
        KeyDetailsDialog(
            entry = detailTarget!!,
            onDismiss = {
                detailTarget = null
                onSelectEntry(null)
            }
        )
    }

    if (renameTarget != null) {
        RenameDialog(
            currentName = renameText,
            onValueChange = { renameText = it },
            onDismiss = {
                renameTarget = null
                renameText = ""
            },
            onConfirm = {
                val entryId = renameTarget?.id
                if (entryId != null) {
                    onRenameEntry(entryId, renameText)
                }
                renameTarget = null
                renameText = ""
            }
        )
    }
}

@Composable
private fun StatusSection(
    statusMessage: String,
    isListening: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal,
            color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionButtons(
    isListening: Boolean,
    onStartReading: () -> Unit,
    onCancelReading: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onStartReading,
            enabled = !isListening,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Read Tag")
        }
        if (isListening) {
            OutlinedButton(
                onClick = onCancelReading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun LastReadCard(
    data: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Last Read Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = data,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SavedKeysSection(
    entries: List<KeyEntry>,
    onSelect: (KeyEntry) -> Unit,
    onRename: (KeyEntry) -> Unit,
    onDelete: (KeyEntry) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Saved Keys",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (entries.isEmpty()) {
            Text(
                text = "No keys saved yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(entries, key = { it.id }) { entry ->
                    SavedKeyRow(
                        entry = entry,
                        onSelect = onSelect,
                        onRename = onRename,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedKeyRow(
    entry: KeyEntry,
    onSelect: (KeyEntry) -> Unit,
    onRename: (KeyEntry) -> Unit,
    onDelete: (KeyEntry) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(entry) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTimestamp(entry.createdAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActionRow(
                onRename = { onRename(entry) },
                onDelete = { onDelete(entry) }
            )
        }
    }
}

@Composable
private fun ActionRow(
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onRename) {
            Text("Rename")
        }
        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}

@Composable
private fun KeyDetailsDialog(
    entry: KeyEntry,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Captured: ${formatTimestamp(entry.createdAtMillis)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = entry.rawData,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RenameDialog(
    currentName: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Key") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentName,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    return formatter.format(Date(timestamp))
}

