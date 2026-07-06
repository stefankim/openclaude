package com.openclaude.weather.widget

import android.content.Context
import org.json.JSONObject

/**
 * SharedPreferences cache of weather snapshots so widgets render instantly between data
 * refreshes. Snapshots are stored per location id; the special key [KEY_SELECTED] tracks
 * the app's currently selected location. Each widget instance may pin its own location
 * via [widgetLocation]/[setWidgetLocation].
 */
object WidgetState {
    private const val PREFS = "weather_widget"
    const val KEY_SELECTED = "selected"

    data class Snapshot(
        val tempC: Double,
        val weatherCode: Int,
        val isDay: Boolean,
        val locationName: String,
        val conditionLabel: String,
        val windKmh: Double,
        val updatedAtMillis: Long
    )

    fun save(context: Context, locationKey: String, snapshot: Snapshot) {
        val json = JSONObject().apply {
            put("temp", snapshot.tempC)
            put("code", snapshot.weatherCode)
            put("isDay", snapshot.isDay)
            put("name", snapshot.locationName)
            put("cond", snapshot.conditionLabel)
            put("wind", snapshot.windKmh)
            put("updated", snapshot.updatedAtMillis)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("snap_$locationKey", json.toString())
            .apply()
    }

    fun load(context: Context, locationKey: String): Snapshot? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("snap_$locationKey", null) ?: return null
        return runCatching {
            val j = JSONObject(raw)
            Snapshot(
                tempC = j.getDouble("temp"),
                weatherCode = j.getInt("code"),
                isDay = j.getBoolean("isDay"),
                locationName = j.getString("name"),
                conditionLabel = j.getString("cond"),
                windKmh = j.optDouble("wind", 0.0),
                updatedAtMillis = j.getLong("updated")
            )
        }.getOrNull()
    }

    /** The location id a widget instance is pinned to, or [KEY_SELECTED] to follow the app. */
    fun widgetLocation(context: Context, appWidgetId: Int): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("widget_loc_$appWidgetId", KEY_SELECTED) ?: KEY_SELECTED

    fun setWidgetLocation(context: Context, appWidgetId: Int, locationKey: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("widget_loc_$appWidgetId", locationKey)
            .apply()
    }

    /** All distinct location keys referenced by any widget instance. */
    fun referencedLocationKeys(context: Context, appWidgetIds: IntArray): Set<String> =
        appWidgetIds.map { widgetLocation(context, it) }.toSet()
}
