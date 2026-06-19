package com.openclaude.weather.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "weather_prefs")

/** Persists the user's pinned locations (max [MAX_LOCATIONS]) and the selected one. */
class LocationStore(private val context: Context) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, SavedLocation::class.java)
    private val adapter = moshi.adapter<List<SavedLocation>>(listType)

    private val keyLocations = stringPreferencesKey("locations_json")
    private val keySelected = stringPreferencesKey("selected_id")

    val locations: Flow<List<SavedLocation>> = context.dataStore.data.map { prefs ->
        prefs[keyLocations]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: emptyList()
    }

    val selectedId: Flow<String?> = context.dataStore.data.map { it[keySelected] }

    suspend fun current(): List<SavedLocation> {
        var result: List<SavedLocation> = emptyList()
        context.dataStore.edit { prefs ->
            result = prefs[keyLocations]?.let {
                runCatching { adapter.fromJson(it) }.getOrNull()
            } ?: emptyList()
        }
        return result
    }

    /** Adds a location if not already present and under the cap. Returns false if rejected. */
    suspend fun add(location: SavedLocation): Boolean {
        var added = false
        context.dataStore.edit { prefs ->
            val list = prefs[keyLocations]?.let {
                runCatching { adapter.fromJson(it) }.getOrNull()
            }?.toMutableList() ?: mutableListOf()
            val exists = list.any { it.id == location.id }
            if (!exists && list.size < MAX_LOCATIONS) {
                list.add(location)
                prefs[keyLocations] = adapter.toJson(list)
                if (prefs[keySelected] == null) prefs[keySelected] = location.id
                added = true
            }
        }
        return added
    }

    suspend fun remove(id: String) {
        context.dataStore.edit { prefs ->
            val list = prefs[keyLocations]?.let {
                runCatching { adapter.fromJson(it) }.getOrNull()
            }?.toMutableList() ?: mutableListOf()
            list.removeAll { it.id == id }
            prefs[keyLocations] = adapter.toJson(list)
            if (prefs[keySelected] == id) prefs[keySelected] = list.firstOrNull()?.id ?: ""
        }
    }

    suspend fun reorder(newOrder: List<SavedLocation>) {
        context.dataStore.edit { prefs ->
            prefs[keyLocations] = adapter.toJson(newOrder.take(MAX_LOCATIONS))
        }
    }

    suspend fun select(id: String) {
        context.dataStore.edit { it[keySelected] = id }
    }

    companion object {
        const val MAX_LOCATIONS = 10
    }
}
