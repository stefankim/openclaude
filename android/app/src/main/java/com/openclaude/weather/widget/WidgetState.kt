package com.openclaude.weather.widget

import android.content.Context

/** Small SharedPreferences cache so the widget can render instantly between data refreshes. */
object WidgetState {
    private const val PREFS = "weather_widget"
    private const val KEY_TEMP = "temp"
    private const val KEY_CODE = "code"
    private const val KEY_IS_DAY = "is_day"
    private const val KEY_NAME = "name"
    private const val KEY_CONDITION = "condition"
    private const val KEY_UPDATED = "updated"

    data class Snapshot(
        val tempC: Double,
        val weatherCode: Int,
        val isDay: Boolean,
        val locationName: String,
        val conditionLabel: String,
        val updatedAtMillis: Long
    )

    fun save(context: Context, snapshot: Snapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putFloat(KEY_TEMP, snapshot.tempC.toFloat())
            putInt(KEY_CODE, snapshot.weatherCode)
            putBoolean(KEY_IS_DAY, snapshot.isDay)
            putString(KEY_NAME, snapshot.locationName)
            putString(KEY_CONDITION, snapshot.conditionLabel)
            putLong(KEY_UPDATED, snapshot.updatedAtMillis)
            apply()
        }
    }

    fun load(context: Context): Snapshot? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains(KEY_TEMP)) return null
        return Snapshot(
            tempC = p.getFloat(KEY_TEMP, 0f).toDouble(),
            weatherCode = p.getInt(KEY_CODE, 3),
            isDay = p.getBoolean(KEY_IS_DAY, true),
            locationName = p.getString(KEY_NAME, "—") ?: "—",
            conditionLabel = p.getString(KEY_CONDITION, "") ?: "",
            updatedAtMillis = p.getLong(KEY_UPDATED, 0L)
        )
    }
}
