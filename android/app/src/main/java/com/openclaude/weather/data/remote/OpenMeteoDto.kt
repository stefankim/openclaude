package com.openclaude.weather.data.remote

import com.squareup.moshi.Json

data class ForecastResponse(
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Long?,
    @Json(name = "current") val current: CurrentDto?,
    @Json(name = "hourly") val hourly: HourlyDto?,
    @Json(name = "daily") val daily: DailyDto?
)

data class CurrentDto(
    @Json(name = "time") val time: Long?,
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "relative_humidity_2m") val humidity: Int?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "is_day") val isDay: Int?,
    @Json(name = "precipitation") val precipitation: Double?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "surface_pressure") val pressure: Double?,
    @Json(name = "wind_speed_10m") val windSpeed: Double?,
    @Json(name = "wind_direction_10m") val windDirection: Int?
)

data class HourlyDto(
    @Json(name = "time") val time: List<Long> = emptyList(),
    @Json(name = "temperature_2m") val temperature: List<Double> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "precipitation_probability") val precipProbability: List<Int?> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed: List<Double> = emptyList(),
    @Json(name = "is_day") val isDay: List<Int> = emptyList()
)

data class DailyDto(
    @Json(name = "time") val time: List<Long> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val tempMin: List<Double> = emptyList(),
    @Json(name = "sunrise") val sunrise: List<Long?> = emptyList(),
    @Json(name = "sunset") val sunset: List<Long?> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @Json(name = "precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipProbabilityMax: List<Int?> = emptyList(),
    @Json(name = "wind_speed_10m_max") val windMax: List<Double?> = emptyList()
)

// ---- Geocoding ----

data class GeocodingResponse(
    @Json(name = "results") val results: List<GeoResult>? = emptyList()
)

data class GeoResult(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "country") val country: String?,
    @Json(name = "country_code") val countryCode: String?,
    @Json(name = "admin1") val admin1: String?,
    @Json(name = "timezone") val timezone: String?
)

// ---- RainViewer radar ----

data class RainViewerMaps(
    @Json(name = "version") val version: String?,
    @Json(name = "host") val host: String,
    @Json(name = "radar") val radar: RadarFrames?,
    @Json(name = "satellite") val satellite: SatelliteFrames?
)

data class RadarFrames(
    @Json(name = "past") val past: List<RadarFrame> = emptyList(),
    @Json(name = "nowcast") val nowcast: List<RadarFrame> = emptyList()
)

data class SatelliteFrames(
    @Json(name = "infrared") val infrared: List<RadarFrame> = emptyList()
)

data class RadarFrame(
    @Json(name = "time") val time: Long,
    @Json(name = "path") val path: String
)

// ---- Precipitation forecast grid (one element per coordinate) ----

data class GridPointResponse(
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "hourly") val hourly: GridHourly?
)

data class GridHourly(
    @Json(name = "time") val time: List<Long> = emptyList(),
    @Json(name = "precipitation") val precipitation: List<Double?> = emptyList()
)
