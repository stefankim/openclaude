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
    private val keyModel = stringPreferencesKey("forecast_model")

    val locations: Flow<List<SavedLocation>> = context.dataStore.data.map { prefs ->
        prefs[keyLocations]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: emptyList()
    }

    val selectedId: Flow<String?> = context.dataStore.data.map { it[keySelected] }

    /** Selected Open-Meteo forecast model id (default "best_match"). */
    val model: Flow<String> = context.dataStore.data.map { it[keyModel] ?: "best_match" }

    suspend fun setModel(model: String) {
        context.dataStore.edit { it[keyModel] = model }
    }

    // ---- Settings ----

    private val keyTempUnit = stringPreferencesKey("temp_unit")       // C | F
    private val keyWindUnit = stringPreferencesKey("wind_unit")       // kmh | ms | mph
    private val keyAnim = stringPreferencesKey("anim_intensity")       // low | normal | high
    private val keyNotifMorning = stringPreferencesKey("notif_morning") // "1"/"0"
    private val keyNotifRain = stringPreferencesKey("notif_rain")
    private val keyNotifAlerts = stringPreferencesKey("notif_alerts")

    data class Settings(
        val tempUnit: String = "C",
        val windUnit: String = "kmh",
        val animIntensity: String = "normal",
        val notifMorning: Boolean = false,
        val notifRain: Boolean = false,
        val notifAlerts: Boolean = true
    )

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            tempUnit = p[keyTempUnit] ?: "C",
            windUnit = p[keyWindUnit] ?: "kmh",
            animIntensity = p[keyAnim] ?: "normal",
            notifMorning = p[keyNotifMorning] == "1",
            notifRain = p[keyNotifRain] == "1",
            notifAlerts = (p[keyNotifAlerts] ?: "1") == "1"
        )
    }

    suspend fun setTempUnit(v: String) = context.dataStore.edit { it[keyTempUnit] = v }
    suspend fun setWindUnit(v: String) = context.dataStore.edit { it[keyWindUnit] = v }
    suspend fun setAnimIntensity(v: String) = context.dataStore.edit { it[keyAnim] = v }
    suspend fun setNotifMorning(v: Boolean) = context.dataStore.edit { it[keyNotifMorning] = if (v) "1" else "0" }
    suspend fun setNotifRain(v: Boolean) = context.dataStore.edit { it[keyNotifRain] = if (v) "1" else "0" }
    suspend fun setNotifAlerts(v: Boolean) = context.dataStore.edit { it[keyNotifAlerts] = if (v) "1" else "0" }

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
