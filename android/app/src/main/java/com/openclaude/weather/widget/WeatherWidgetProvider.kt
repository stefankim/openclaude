package com.openclaude.weather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.openclaude.weather.MainActivity
import com.openclaude.weather.R
import com.openclaude.weather.util.Format
import com.openclaude.weather.util.WeatherCode
import kotlin.math.roundToInt

/**
 * Home-screen widget showing live, animated weather. Static data is cached in [WidgetState];
 * the animation frames are driven by [WeatherWidgetService].
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        WeatherWidgetWorker.schedule(context)
        WeatherWidgetService.start(context)
        WeatherWidgetService.refreshData(context)
    }

    override fun onDisabled(context: Context) {
        WeatherWidgetWorker.cancel(context)
        WeatherWidgetService.stop(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        renderAll(context, manager, ids, phase = 0f)
        WeatherWidgetService.start(context)
        WeatherWidgetService.refreshData(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            WeatherWidgetService.refreshData(context)
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.openclaude.weather.ACTION_WIDGET_REFRESH"

        /** Renders the given [phase] of the animation onto every widget instance. */
        fun renderAll(context: Context, manager: AppWidgetManager, ids: IntArray, phase: Float) {
            val snapshot = WidgetState.load(context)
            val density = context.resources.displayMetrics.density

            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).coerceAtLeast(120)
                val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120).coerceAtLeast(80)
                // Keep the bitmap well under the RemoteViews memory limit.
                val wPx = (wDp * density).roundToInt().coerceIn(120, 720)
                val hPx = (hDp * density).roundToInt().coerceIn(80, 420)

                val views = RemoteViews(context.packageName, R.layout.widget_weather)

                if (snapshot != null) {
                    val condition = WeatherCode.map(snapshot.weatherCode, snapshot.isDay)
                    val bitmap = WidgetSceneRenderer.render(
                        scene = condition.scene,
                        isDay = snapshot.isDay,
                        phase = phase,
                        widthPx = wPx,
                        heightPx = hPx
                    )
                    views.setImageViewBitmap(R.id.widget_scene, bitmap)
                    views.setTextViewText(R.id.widget_temp, Format.tempPrecise(snapshot.tempC))
                    views.setTextViewText(R.id.widget_location, snapshot.locationName)
                    views.setTextViewText(R.id.widget_condition, snapshot.conditionLabel)
                    views.setTextViewText(
                        R.id.widget_updated,
                        if (snapshot.updatedAtMillis > 0) Format.clock(snapshot.updatedAtMillis) else ""
                    )
                } else {
                    val bitmap = WidgetSceneRenderer.render(
                        scene = com.openclaude.weather.domain.WeatherScene.CLEAR_DAY,
                        isDay = true, phase = phase, widthPx = wPx, heightPx = hPx
                    )
                    views.setImageViewBitmap(R.id.widget_scene, bitmap)
                    views.setTextViewText(R.id.widget_temp, "--°")
                    views.setTextViewText(R.id.widget_location, "Open app")
                    views.setTextViewText(R.id.widget_condition, "Add a location")
                    views.setTextViewText(R.id.widget_updated, "")
                }

                // Tap opens the app.
                val intent = Intent(context, MainActivity::class.java)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val pending = PendingIntent.getActivity(context, id, intent, flags)
                views.setOnClickPendingIntent(R.id.widget_root, pending)

                manager.updateAppWidget(id, views)
            }
        }
    }
}
