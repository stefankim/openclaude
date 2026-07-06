package com.openclaude.weather.util

import com.openclaude.weather.R
import com.openclaude.weather.domain.WeatherCondition
import com.openclaude.weather.domain.WeatherScene

/**
 * Maps a WMO weather interpretation code (as returned by Open-Meteo / SHMU grids)
 * to a localizable label resource, an animation [WeatherScene], and an icon name.
 *
 * Reference: https://open-meteo.com/en/docs (WMO Weather interpretation codes)
 */
object WeatherCode {

    fun map(code: Int, isDay: Boolean): WeatherCondition {
        val (labelRes, scene, icon) = when (code) {
            0 -> Triple(R.string.cond_clear, if (isDay) WeatherScene.CLEAR_DAY else WeatherScene.CLEAR_NIGHT,
                if (isDay) "sunny" else "clear_night")
            1 -> Triple(R.string.cond_mostly_clear, if (isDay) WeatherScene.CLEAR_DAY else WeatherScene.CLEAR_NIGHT,
                if (isDay) "sunny" else "clear_night")
            2 -> Triple(R.string.cond_partly_cloudy, WeatherScene.PARTLY_CLOUDY, "partly_cloudy")
            3 -> Triple(R.string.cond_overcast, WeatherScene.CLOUDY, "cloudy")
            45, 48 -> Triple(R.string.cond_fog, WeatherScene.FOG, "foggy")
            51 -> Triple(R.string.cond_drizzle_light, WeatherScene.RAIN, "rainy")
            53 -> Triple(R.string.cond_drizzle, WeatherScene.RAIN, "rainy")
            55 -> Triple(R.string.cond_drizzle_dense, WeatherScene.RAIN, "rainy")
            56, 57 -> Triple(R.string.cond_freezing_drizzle, WeatherScene.RAIN, "rainy")
            61 -> Triple(R.string.cond_rain_light, WeatherScene.RAIN, "rainy")
            63 -> Triple(R.string.cond_rain, WeatherScene.RAIN, "rainy")
            65 -> Triple(R.string.cond_rain_heavy, WeatherScene.RAIN, "rainy")
            66, 67 -> Triple(R.string.cond_freezing_rain, WeatherScene.RAIN, "rainy")
            71 -> Triple(R.string.cond_snow_light, WeatherScene.SNOW, "snowy")
            73 -> Triple(R.string.cond_snow, WeatherScene.SNOW, "snowy")
            75 -> Triple(R.string.cond_snow_heavy, WeatherScene.SNOW, "snowy")
            77 -> Triple(R.string.cond_snow_grains, WeatherScene.SNOW, "snowy")
            80 -> Triple(R.string.cond_showers_light, WeatherScene.RAIN, "rainy")
            81 -> Triple(R.string.cond_showers, WeatherScene.RAIN, "rainy")
            82 -> Triple(R.string.cond_showers_violent, WeatherScene.RAIN, "rainy")
            85, 86 -> Triple(R.string.cond_snow_showers, WeatherScene.SNOW, "snowy")
            95 -> Triple(R.string.cond_thunderstorm, WeatherScene.THUNDERSTORM, "thunderstorm")
            96, 99 -> Triple(R.string.cond_thunderstorm_hail, WeatherScene.THUNDERSTORM, "thunderstorm")
            else -> Triple(R.string.cond_unknown, WeatherScene.CLOUDY, "cloudy")
        }
        return WeatherCondition(code = code, labelRes = labelRes, scene = scene, icon = icon)
    }

    fun windDirectionLabel(deg: Int): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((deg % 360) / 45.0).toInt().coerceIn(0, 7)]
    }
}
