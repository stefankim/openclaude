package com.openclaude.weather.util

/**
 * Process-wide display units, kept in sync with the persisted settings by whoever renders
 * (AppScaffold for the UI, WidgetUpdater for the widget). Values are always stored/computed
 * in °C and km/h; conversion happens only at display time in [Format].
 */
object Units {
    @Volatile var tempUnit: String = "C"   // C | F
    @Volatile var windUnit: String = "kmh" // kmh | ms | mph

    fun convertTemp(c: Double): Double = if (tempUnit == "F") c * 9.0 / 5.0 + 32.0 else c

    fun convertWind(kmh: Double): Double = when (windUnit) {
        "ms" -> kmh / 3.6
        "mph" -> kmh / 1.609344
        else -> kmh
    }

    fun windLabel(): String = when (windUnit) {
        "ms" -> "m/s"
        "mph" -> "mph"
        else -> "km/h"
    }
}
