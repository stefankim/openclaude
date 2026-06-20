package com.openclaude.weather.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo forecast API. Free, keyless, and resolves Slovak locations against the
 * same numerical-weather-prediction grids SHMU publishes against. The data layer is
 * isolated behind [WeatherRepository] so a dedicated SHMU endpoint can replace this later.
 */
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String = "auto",
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("forecast_days") forecastDays: Int = 14,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation," +
                "weather_code,surface_pressure,wind_speed_10m,wind_direction_10m",
        @Query("hourly") hourly: String =
            "temperature_2m,weather_code,precipitation_probability,wind_speed_10m,is_day",
        @Query("daily") daily: String =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
                "precipitation_sum,precipitation_probability_max,wind_speed_10m_max"
    ): ForecastResponse

    /**
     * Bulk precipitation forecast for a grid of coordinates (comma-separated). Open-Meteo
     * returns one JSON object per coordinate. Used to build the native forecast radar.
     */
    @GET("v1/forecast")
    suspend fun precipitationGrid(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("hourly") hourly: String = "precipitation",
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("timezone") timezone: String = "GMT",
        @Query("forecast_days") forecastDays: Int = 3
    ): List<GridPointResponse>
}

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

interface RainViewerApi {
    @GET("public/weather-maps.json")
    suspend fun maps(): RainViewerMaps
}
