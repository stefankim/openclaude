package com.openclaude.weather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.openclaude.weather.WeatherApp
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.util.Units
import kotlinx.coroutines.flow.firstOrNull

/** Fetches weather for every location any widget shows and stores snapshots for rendering. */
object WidgetUpdater {

    suspend fun refresh(context: Context) {
        val container = WeatherApp.container()
        val store = container.locationStore
        val locations = store.locations.firstOrNull() ?: emptyList()
        if (locations.isEmpty()) return
        val selectedId = store.selectedId.firstOrNull()
        val selected = locations.firstOrNull { it.id == selectedId } ?: locations.first()

        // Keep the widget's units in sync with app settings.
        store.settings.firstOrNull()?.let {
            Units.tempUnit = it.tempUnit
            Units.windUnit = it.windUnit
        }

        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))

        // The app-selected location always gets a snapshot (KEY_SELECTED), plus every
        // location a widget instance is explicitly pinned to.
        val needed = mutableMapOf<String, SavedLocation>(WidgetState.KEY_SELECTED to selected)
        WidgetState.referencedLocationKeys(context, ids).forEach { key ->
            if (key != WidgetState.KEY_SELECTED) {
                locations.firstOrNull { it.id == key }?.let { needed[key] = it }
            }
        }

        needed.forEach { (key, location) ->
            val forecast = runCatching { container.repository.forecast(location) }.getOrNull()
                ?: return@forEach
            val current = forecast.current
            WidgetState.save(
                context, key,
                WidgetState.Snapshot(
                    tempC = current.temperatureC,
                    weatherCode = current.condition.code,
                    isDay = current.isDay,
                    locationName = location.name,
                    conditionLabel = context.getString(current.condition.labelRes),
                    windKmh = current.windSpeedKmh,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
        notifyWidgets(context)
    }

    fun notifyWidgets(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            WeatherWidgetProvider.renderAll(context, mgr, ids, phase = 0f)
        }
    }
}
