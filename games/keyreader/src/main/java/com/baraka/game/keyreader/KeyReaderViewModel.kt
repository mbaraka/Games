package com.baraka.game.keyreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class KeyReaderViewModel(
    private val repository: KeyRepository
) : ViewModel() {

    private val idGenerator = AtomicLong()

    private val _uiState = MutableStateFlow(KeyReaderUiState())
    val uiState: StateFlow<KeyReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.entries.collectLatest { entries ->
                val maxId = entries.maxOfOrNull { it.id } ?: 0L
                idGenerator.updateAndGet { current -> max(current, maxId) }
                _uiState.update { state ->
                    state.copy(entries = entries)
                }
            }
        }
    }

    fun startListening() {
        _uiState.update { state ->
            state.copy(
                isListening = true,
                statusMessage = "Hold the key near the device to read.",
                lastReadData = null
            )
        }
    }

    fun cancelListening() {
        _uiState.update { state ->
            state.copy(
                isListening = false,
                statusMessage = "Reading cancelled."
            )
        }
    }

    fun onNfcUnavailable() {
        _uiState.update { state ->
            state.copy(
                isListening = false,
                statusMessage = "NFC unavailable on this device."
            )
        }
    }

    fun onTagRead(rawData: String) {
        val generatedId = idGenerator.incrementAndGet()
        val entry = KeyEntry(
            id = generatedId,
            name = "Key $generatedId",
            rawData = rawData,
            createdAtMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.insert(entry)
        }

        _uiState.update { state ->
            state.copy(
                isListening = false,
                statusMessage = "Key data captured.",
                lastReadData = rawData
            )
        }
    }

    fun renameEntry(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateEntry(id) { entry -> entry.copy(name = trimmed) }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.remove(id)
        }
        _uiState.update { state ->
            state.copy(
                selectedEntryId = state.selectedEntryId.takeIf { it != id }
            )
        }
    }

    fun selectEntry(id: Long?) {
        _uiState.update { state ->
            state.copy(selectedEntryId = id)
        }
    }
}

