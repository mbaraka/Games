package com.baraka.game.keyreader

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class KeyRepository private constructor(
    private val preferences: SharedPreferences
) {

    private val entriesFlow = MutableStateFlow(loadEntries())

    val entries: StateFlow<List<KeyEntry>> = entriesFlow.asStateFlow()

    suspend fun insert(entry: KeyEntry) {
        updateEntries { current -> listOf(entry) + current }
    }

    suspend fun updateEntry(id: Long, transform: (KeyEntry) -> KeyEntry) {
        updateEntries { current ->
            current.map { existing ->
                if (existing.id == id) transform(existing) else existing
            }
        }
    }

    suspend fun remove(id: Long) {
        updateEntries { current -> current.filterNot { it.id == id } }
    }

    private suspend fun updateEntries(transform: (List<KeyEntry>) -> List<KeyEntry>) {
        val updated = transform(entriesFlow.value)
        persist(updated)
    }

    private suspend fun persist(entries: List<KeyEntry>) {
        withContext(Dispatchers.IO) {
            preferences.edit()
                .putString(PREF_KEY_ENTRIES, entries.toJsonArray().toString())
                .apply()
        }
        entriesFlow.value = entries
    }

    private fun loadEntries(): List<KeyEntry> {
        val raw = preferences.getString(PREF_KEY_ENTRIES, null) ?: return emptyList()
        return raw.toKeyEntries()
    }

    private fun List<KeyEntry>.toJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("name", entry.name)
                put("rawData", entry.rawData)
                put("createdAtMillis", entry.createdAtMillis)
            }
            array.put(obj)
        }
        return array
    }

    private fun String.toKeyEntries(): List<KeyEntry> {
        return try {
            val array = JSONArray(this)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val id = obj.optLong("id", -1L)
                    val name = obj.optString("name", "Key $index")
                    val rawData = obj.optString("rawData", "")
                    val createdAt = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    if (id >= 0 && rawData.isNotEmpty()) {
                        add(
                            KeyEntry(
                                id = id,
                                name = name,
                                rawData = rawData,
                                createdAtMillis = createdAt
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREF_FILE = "key_reader_prefs"
        private const val PREF_KEY_ENTRIES = "key_entries"

        @Volatile
        private var INSTANCE: KeyRepository? = null

        fun getInstance(context: Context): KeyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeyRepository(
                    context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { INSTANCE = it }
            }
        }
    }
}

