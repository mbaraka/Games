package com.baraka.game.keyreader

data class KeyReaderUiState(
    val isListening: Boolean = false,
    val statusMessage: String = "Tap 'Read Tag' to begin.",
    val lastReadData: String? = null,
    val entries: List<KeyEntry> = emptyList(),
    val selectedEntryId: Long? = null
)

data class KeyEntry(
    val id: Long,
    val name: String,
    val rawData: String,
    val createdAtMillis: Long
) {
    val preview: String
        get() = if (rawData.length <= 32) rawData else rawData.take(32) + "…"
}

