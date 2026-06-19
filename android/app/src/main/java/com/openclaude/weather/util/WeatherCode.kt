package com.openclaude.weather.util

import com.openclaude.weather.domain.WeatherCondition
import com.openclaude.weather.domain.WeatherScene

/**
 * Maps a WMO weather interpretation code (as returned by Open-Meteo / SHMU grids)
 * to a human label, an animation [WeatherScene], and an icon name.
 *
 * Reference: https://open-meteo.com/en/docs (WMO Weather interpretation codes)
 */
object WeatherCode {

    fun map(code: Int, isDay: Boolean): WeatherCondition {
        val (label, scene, icon) = when (code) {
            0 -> Triple("Clear sky", if (isDay) WeatherScene.CLEAR_DAY else WeatherScene.CLEAR_NIGHT,
                if (isDay) "sunny" else "clear_night")
            1 -> Triple("Mostly clear", if (isDay) WeatherScene.CLEAR_DAY else WeatherScene.CLEAR_NIGHT,
                if (isDay) "sunny" else "clear_night")
            2 -> Triple("Partly cloudy", WeatherScene.PARTLY_CLOUDY, "partly_cloudy")
            3 -> Triple("Overcast", WeatherScene.CLOUDY, "cloudy")
            45, 48 -> Triple("Fog", WeatherScene.FOG, "foggy")
            51 -> Triple("Light drizzle", WeatherScene.RAIN, "rainy")
            53 -> Triple("Drizzle", WeatherScene.RAIN, "rainy")
            55 -> Triple("Dense drizzle", WeatherScene.RAIN, "rainy")
            56, 57 -> Triple("Freezing drizzle", WeatherScene.RAIN, "rainy")
            61 -> Triple("Light rain", WeatherScene.RAIN, "rainy")
            63 -> Triple("Rain", WeatherScene.RAIN, "rainy")
            65 -> Triple("Heavy rain", WeatherScene.RAIN, "rainy")
            66, 67 -> Triple("Freezing rain", WeatherScene.RAIN, "rainy")
            71 -> Triple("Light snow", WeatherScene.SNOW, "snowy")
            73 -> Triple("Snow", WeatherScene.SNOW, "snowy")
            75 -> Triple("Heavy snow", WeatherScene.SNOW, "snowy")
            77 -> Triple("Snow grains", WeatherScene.SNOW, "snowy")
            80 -> Triple("Light showers", WeatherScene.RAIN, "rainy")
            81 -> Triple("Showers", WeatherScene.RAIN, "rainy")
            82 -> Triple("Violent showers", WeatherScene.RAIN, "rainy")
            85, 86 -> Triple("Snow showers", WeatherScene.SNOW, "snowy")
            95 -> Triple("Thunderstorm", WeatherScene.THUNDERSTORM, "thunderstorm")
            96, 99 -> Triple("Thunderstorm w/ hail", WeatherScene.THUNDERSTORM, "thunderstorm")
            else -> Triple("Unknown", WeatherScene.CLOUDY, "cloudy")
        }
        return WeatherCondition(code = code, label = label, scene = scene, icon = icon)
    }

    fun windDirectionLabel(deg: Int): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((deg % 360) / 45.0).toInt().coerceIn(0, 7)]
    }
}
