package com.openclaude.weather.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url

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
        @Query("minutely_15") minutely15: String = "precipitation",
        @Query("forecast_minutely_15") forecastMinutely15: Int = 8,
        @Query("hourly") hourly: String =
            "temperature_2m,weather_code,precipitation_probability,precipitation," +
                "relative_humidity_2m,wind_speed_10m,is_day",
        @Query("daily") daily: String =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
                "precipitation_sum,precipitation_probability_max,wind_speed_10m_max",
        @Query("models") models: String = "best_match"
    ): ForecastResponse

    /** Light daily-only request used by the model comparison table. */
    @GET("v1/forecast")
    suspend fun dailyOnly(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String = "auto",
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("models") models: String = "best_match"
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

    /** Bulk current conditions for several coordinates — used for saved-place previews. */
    @GET("v1/forecast")
    suspend fun bulkCurrent(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("current") current: String = "temperature_2m,weather_code,is_day",
        @Query("timeformat") timeFormat: String = "unixtime",
        @Query("timezone") timezone: String = "GMT"
    ): List<BulkCurrentResponse>
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

interface AirQualityApi {
    @GET("v1/air-quality")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "european_aqi,pm2_5,pm10,alder_pollen,birch_pollen,grass_pollen",
        @Query("timezone") timezone: String = "auto"
    ): AirQualityResponse
}

/** Fetches raw feeds (MeteoAlarm CAP/Atom warnings) from an absolute URL. */
interface FeedApi {
    @GET
    suspend fun fetch(@Url url: String): ResponseBody
}

/**
 * SHMÚ (Slovak Hydrometeorological Institute) radar. Native Slovak radar composites at
 * 5-minute resolution — the same imagery shmu.sk shows. Products include maximum column
 * reflectivity, 2 km CAPPI, 1 h rainfall totals and lightning.
 *
 * NOTE: SHMÚ's terms restrict reuse of their imagery to personal use unless they grant
 * consent, so this source is opt-in and clearly attributed in the UI.
 */
interface ShmuApi {
    @GET("api/v1/meteo/getradardata")
    @Headers("X-Requested-With: XMLHttpRequest", "Referer: https://www.shmu.sk/sk/?page=2322")
    suspend fun radarData(): List<ShmuRadarProduct>
}
