package com.openclaude.weather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.openclaude.weather.WeatherApp
import kotlinx.coroutines.flow.firstOrNull

/** Fetches the selected location's weather and stores a [WidgetState.Snapshot] for the widget. */
object WidgetUpdater {

    suspend fun refresh(context: Context) {
        val container = WeatherApp.container()
        val locations = container.locationStore.locations.firstOrNull() ?: emptyList()
        val selectedId = container.locationStore.selectedId.firstOrNull()
        val location = locations.firstOrNull { it.id == selectedId } ?: locations.firstOrNull() ?: return

        val forecast = runCatching { container.repository.forecast(location) }.getOrNull() ?: return
        val current = forecast.current
        WidgetState.save(
            context,
            WidgetState.Snapshot(
                tempC = current.temperatureC,
                weatherCode = current.condition.code,
                isDay = current.isDay,
                locationName = location.name,
                conditionLabel = current.condition.label,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        notifyWidgets(context)
    }

    fun notifyWidgets(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            // Re-render immediately with cached data; the service animates afterwards.
            WeatherWidgetProvider.renderAll(context, mgr, ids, phase = 0f)
        }
    }
}
