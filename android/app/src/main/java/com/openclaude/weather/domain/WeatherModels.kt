package com.openclaude.weather.domain

/** Animation buckets used by the Compose weather scene and the home-screen widget. */
enum class WeatherScene {
    CLEAR_DAY, CLEAR_NIGHT, PARTLY_CLOUDY, CLOUDY, FOG, RAIN, SNOW, THUNDERSTORM
}

/** Result of mapping a WMO weather code. */
data class WeatherCondition(
    val code: Int,
    /** String resource id for the human-readable condition name (localizable). */
    val labelRes: Int,
    val scene: WeatherScene,
    /** Material symbol name used by [WeatherIcon]. */
    val icon: String
)

data class CurrentWeather(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val condition: WeatherCondition,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val humidityPct: Int,
    val pressureHpa: Double,
    val isDay: Boolean,
    val precipitationMm: Double,
    val uvIndex: Double,
    val sunriseEpoch: Long?,
    val sunsetEpoch: Long?
)

data class HourPoint(
    val epochSeconds: Long,
    val temperatureC: Double,
    val condition: WeatherCondition,
    val precipitationProbabilityPct: Int,
    val precipitationMm: Double,
    val humidityPct: Int,
    val windSpeedKmh: Double,
    val isDay: Boolean
)

/** One 15-minute precipitation point (next ~2 h), for the "rain soon" strip. */
data class MinutePoint(val epochSeconds: Long, val precipitationMm: Double)

/** Official weather warning (MeteoAlarm / SHMU). */
data class WeatherAlert(
    val event: String,
    val severity: String,
    val area: String,
    val expiresMillis: Long?
)

data class AirQuality(
    val europeanAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val alderPollen: Double?,
    val birchPollen: Double?,
    val grassPollen: Double?
)

data class DayPoint(
    val epochSeconds: Long,
    val condition: WeatherCondition,
    val tempMaxC: Double,
    val tempMinC: Double,
    val precipitationProbabilityPct: Int,
    val precipitationSumMm: Double,
    val windMaxKmh: Double,
    val sunriseEpoch: Long?,
    val sunsetEpoch: Long?,
    val uvIndexMax: Double
)

/** Full forecast for one place. */
data class Forecast(
    val current: CurrentWeather,
    val minutely: List<MinutePoint>,
    val hourly: List<HourPoint>,
    val daily: List<DayPoint>,
    val timezone: String,
    /** Seconds to add to the UTC epochs to obtain the location's local time. */
    val utcOffsetSeconds: Long,
    val fetchedAtMillis: Long
)
