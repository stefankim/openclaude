package com.openclaude.weather.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Time/number formatting helpers. Epoch values from Open-Meteo are local-time unix seconds. */
object Format {

    private fun fmt(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.getDefault()).apply {
            // Open-Meteo unixtime is already offset to the location's timezone, so render as UTC.
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun hour(epochSeconds: Long): String = fmt("HH:mm").format(Date(epochSeconds * 1000))

    fun weekday(epochSeconds: Long): String = fmt("EEE").format(Date(epochSeconds * 1000))

    fun fullDay(epochSeconds: Long): String = fmt("EEEE, d MMM").format(Date(epochSeconds * 1000))

    fun dayMonth(epochSeconds: Long): String = fmt("d MMM").format(Date(epochSeconds * 1000))

    fun temp(c: Double): String = "${Math.round(c)}°"

    fun tempPrecise(c: Double): String = "${Math.round(c)}°C"

    fun wind(kmh: Double): String = "${Math.round(kmh)} km/h"

    fun percent(p: Int): String = "$p%"

    fun mm(v: Double): String = String.format(Locale.getDefault(), "%.1f mm", v)

    fun clock(epochMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))
}
