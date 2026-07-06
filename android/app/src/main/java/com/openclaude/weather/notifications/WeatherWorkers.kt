package com.openclaude.weather.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.openclaude.weather.R
import com.openclaude.weather.WeatherApp
import com.openclaude.weather.util.Format
import com.openclaude.weather.util.Units
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val PREFS = "notif_state"

/**
 * Periodic check (every ~30 min) for imminent rain and new official warnings, honoring the
 * user's notification toggles. Runs regardless of whether a widget exists.
 */
class ConditionsWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = WeatherApp.container()
        val store = container.locationStore
        val settings = store.settings.firstOrNull() ?: return Result.success()
        if (!settings.notifRain && !settings.notifAlerts) return Result.success()

        val locations = store.locations.firstOrNull() ?: return Result.success()
        val selectedId = store.selectedId.firstOrNull()
        val location = locations.firstOrNull { it.id == selectedId } ?: locations.firstOrNull()
            ?: return Result.success()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (settings.notifRain) {
            runCatching {
                val forecast = container.repository.forecast(location)
                val nowSec = System.currentTimeMillis() / 1000
                val firstRain = forecast.minutely
                    .filter { it.epochSeconds in nowSec..(nowSec + 3600) }
                    .firstOrNull { it.precipitationMm > 0.1 }
                if (firstRain != null) {
                    // Don't repeat within 2 hours.
                    val last = prefs.getLong("last_rain_notif", 0)
                    if (System.currentTimeMillis() - last > 2 * 3600_000) {
                        prefs.edit().putLong("last_rain_notif", System.currentTimeMillis()).apply()
                        Notifier.notify(
                            applicationContext, 1001,
                            applicationContext.getString(R.string.notif_rain_title),
                            applicationContext.getString(
                                R.string.notif_rain_text,
                                Format.hour(firstRain.epochSeconds, forecast.utcOffsetSeconds)
                            )
                        )
                    }
                }
            }
        }

        if (settings.notifAlerts) {
            runCatching {
                val alerts = container.repository.alerts(location)
                val seen = prefs.getStringSet("seen_alerts", emptySet()) ?: emptySet()
                val fresh = alerts.filter { (it.event + it.severity).hashCode().toString() !in seen }
                if (fresh.isNotEmpty()) {
                    prefs.edit().putStringSet(
                        "seen_alerts",
                        (seen + alerts.map { (it.event + it.severity).hashCode().toString() }).take(50).toSet()
                    ).apply()
                    fresh.forEachIndexed { i, alert ->
                        Notifier.notify(
                            applicationContext, 2000 + i,
                            applicationContext.getString(R.string.notif_alert_title),
                            "${alert.event} · ${alert.severity}"
                        )
                    }
                }
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "conditions_check",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ConditionsWorker>(30, TimeUnit.MINUTES).build()
            )
        }
    }
}

/** Daily morning forecast summary (~07:00), if enabled. */
class MorningWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = WeatherApp.container()
        val store = container.locationStore
        val settings = store.settings.firstOrNull() ?: return Result.success()
        if (!settings.notifMorning) return Result.success()

        val locations = store.locations.firstOrNull() ?: return Result.success()
        val selectedId = store.selectedId.firstOrNull()
        val location = locations.firstOrNull { it.id == selectedId } ?: locations.firstOrNull()
            ?: return Result.success()

        // Sync display units before formatting.
        Units.tempUnit = settings.tempUnit
        Units.windUnit = settings.windUnit

        runCatching {
            val forecast = container.repository.forecast(location, forceRefresh = true)
            val today = forecast.daily.firstOrNull() ?: return Result.success()
            val text = buildString {
                append(applicationContext.getString(today.condition.labelRes))
                append(" · ")
                append(Format.temp(today.tempMinC)).append(" / ").append(Format.temp(today.tempMaxC))
                if (today.precipitationProbabilityPct > 0) {
                    append(" · ☔ ").append(today.precipitationProbabilityPct).append("%")
                }
            }
            Notifier.notify(
                applicationContext, 1000,
                "${applicationContext.getString(R.string.notif_morning_title)} — ${location.name}",
                text
            )
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            // First run at the next 07:00 local, then every 24 h.
            val now = Calendar.getInstance()
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delayMin = (next.timeInMillis - now.timeInMillis) / 60_000
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "morning_summary",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<MorningWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayMin, TimeUnit.MINUTES)
                    .build()
            )
        }
    }
}
