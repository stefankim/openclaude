package com.openclaude.weather.data.repository

import android.content.Context
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.data.remote.ForecastResponse
import com.openclaude.weather.data.remote.GeoResult
import com.openclaude.weather.data.remote.Network
import com.openclaude.weather.data.remote.RadarFrame
import com.openclaude.weather.domain.AirQuality
import com.openclaude.weather.domain.CurrentWeather
import com.openclaude.weather.domain.DayPoint
import com.openclaude.weather.domain.Forecast
import com.openclaude.weather.domain.HourPoint
import com.openclaude.weather.domain.MinutePoint
import com.openclaude.weather.domain.WeatherAlert
import com.openclaude.weather.util.MeteoAlarmParser
import com.openclaude.weather.util.WeatherCode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Coordinates remote weather/geocoding/radar sources and maps them to domain models. */
class WeatherRepository(private val appContext: Context) {

    private val weatherApi = Network.weatherApi
    private val geocodingApi = Network.geocodingApi
    private val rainViewerApi = Network.rainViewerApi
    private val airQualityApi = Network.airQualityApi
    private val feedApi = Network.feedApi

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val forecastAdapter = moshi.adapter(ForecastResponse::class.java)

    private val cache = mutableMapOf<String, Forecast>()
    private val cacheTtlMillis = 10 * 60 * 1000L

    // ---- Forecast (with disk cache for instant/offline starts) ----

    private fun cacheFile(locationId: String, model: String): File =
        File(appContext.filesDir, "forecast_${locationId.replace('/', '_')}_$model.json")

    /** Last successfully fetched forecast from disk, any age. Null if none cached. */
    suspend fun cachedForecast(location: SavedLocation, model: String): Forecast? =
        withContext(Dispatchers.IO) {
            val f = cacheFile(location.id, model)
            if (!f.exists()) return@withContext null
            runCatching {
                val dto = forecastAdapter.fromJson(f.readText()) ?: return@withContext null
                mapForecast(dto, fetchedAt = f.lastModified())
            }.getOrNull()
        }

    suspend fun forecast(
        location: SavedLocation,
        forceRefresh: Boolean = false,
        model: String = "best_match"
    ): Forecast = withContext(Dispatchers.IO) {
        val key = "${location.id}|$model"
        val cached = cache[key]
        if (!forceRefresh && cached != null &&
            System.currentTimeMillis() - cached.fetchedAtMillis < cacheTtlMillis
        ) {
            return@withContext cached
        }
        val dto = weatherApi.forecast(location.latitude, location.longitude, models = model)
        runCatching { cacheFile(location.id, model).writeText(forecastAdapter.toJson(dto)) }
        val mapped = mapForecast(dto, fetchedAt = System.currentTimeMillis())
        cache[key] = mapped
        mapped
    }

    suspend fun search(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList()
        else geocodingApi.search(query.trim()).results ?: emptyList()
    }

    // ---- Air quality + pollen ----

    suspend fun airQuality(location: SavedLocation): AirQuality = withContext(Dispatchers.IO) {
        val c = airQualityApi.current(location.latitude, location.longitude).current
        AirQuality(
            europeanAqi = c?.europeanAqi,
            pm25 = c?.pm25,
            pm10 = c?.pm10,
            alderPollen = c?.alderPollen,
            birchPollen = c?.birchPollen,
            grassPollen = c?.grassPollen
        )
    }

    // ---- Official warnings (MeteoAlarm) ----

    private var alertsCache: Pair<String, List<WeatherAlert>>? = null
    private var alertsCacheAt = 0L

    suspend fun alerts(location: SavedLocation): List<WeatherAlert> = withContext(Dispatchers.IO) {
        val url = MeteoAlarmParser.feedUrlFor(location.country) ?: return@withContext emptyList()
        alertsCache?.let { (cachedUrl, list) ->
            if (cachedUrl == url && System.currentTimeMillis() - alertsCacheAt < cacheTtlMillis) {
                return@withContext list
            }
        }
        val list = runCatching {
            MeteoAlarmParser.parse(feedApi.fetch(url).string())
        }.getOrDefault(emptyList())
        alertsCache = url to list
        alertsCacheAt = System.currentTimeMillis()
        list
    }

    // ---- Saved-place previews (bulk current conditions) ----

    data class PlacePreview(val temperatureC: Double, val weatherCode: Int, val isDay: Boolean)

    suspend fun placePreviews(locations: List<SavedLocation>): Map<String, PlacePreview> =
        withContext(Dispatchers.IO) {
            if (locations.isEmpty()) return@withContext emptyMap()
            // With a single coordinate the API returns an object, not an array; duplicating
            // the coordinate keeps the response shape consistent.
            val effective = if (locations.size == 1) locations + locations else locations
            val lats = effective.joinToString(",") { "%.4f".format(it.latitude) }
            val lons = effective.joinToString(",") { "%.4f".format(it.longitude) }
            val resp = weatherApi.bulkCurrent(lats, lons)
            buildMap {
                locations.forEachIndexed { i, loc ->
                    val cur = resp.getOrNull(i)?.current ?: return@forEachIndexed
                    put(
                        loc.id,
                        PlacePreview(
                            temperatureC = cur.temperature ?: return@forEachIndexed,
                            weatherCode = cur.weatherCode ?: 3,
                            isDay = (cur.isDay ?: 1) == 1
                        )
                    )
                }
            }
        }

    // ---- Model comparison ----

    data class ModelDaily(val model: String, val days: List<Long>, val tMax: List<Double?>, val tMin: List<Double?>)

    suspend fun compareModels(location: SavedLocation, models: List<String>): List<ModelDaily> =
        withContext(Dispatchers.IO) {
            models.mapNotNull { model ->
                runCatching {
                    val d = weatherApi.dailyOnly(location.latitude, location.longitude, models = model).daily
                    ModelDaily(
                        model = model,
                        days = d?.time ?: emptyList(),
                        tMax = d?.tempMax?.map { it } ?: emptyList(),
                        tMin = d?.tempMin?.map { it } ?: emptyList()
                    )
                }.getOrNull()
            }
        }

    // ---- Native precipitation forecast grid ----

    /** One grid cell of the native precipitation forecast (precip in mm/h per hour index). */
    data class PrecipCell(val lat: Double, val lon: Double, val precip: List<Double>)

    /** A grid of precipitation forecast cells sharing a common [times] axis (UTC seconds). */
    data class PrecipGrid(
        val times: List<Long>,
        val rows: Int,
        val cols: Int,
        val latStep: Double,
        val lonStep: Double,
        val cells: List<PrecipCell>
    ) {
        val isEmpty: Boolean get() = times.isEmpty() || cells.isEmpty()
    }

    private val precipCache = mutableMapOf<String, PrecipGrid>()

    /**
     * Builds a native, fully-licensed precipitation forecast grid (Open-Meteo, CC-BY)
     * centred on the given point. [fine] halves the cell size (used when zoomed in).
     */
    suspend fun precipForecast(centerLat: Double, centerLon: Double, fine: Boolean = false): PrecipGrid =
        withContext(Dispatchers.IO) {
            val key = "%.2f,%.2f,%b".format(centerLat, centerLon, fine)
            precipCache[key]?.let { return@withContext it }

            val rows = 16
            val cols = 18
            val latStep = if (fine) 0.11 else 0.22
            val lonStep = if (fine) 0.17 else 0.34
            val lats = ArrayList<String>(rows * cols)
            val lons = ArrayList<String>(rows * cols)
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val lat = centerLat + (r - rows / 2) * latStep
                    val lon = centerLon + (c - cols / 2) * lonStep
                    lats.add("%.3f".format(lat))
                    lons.add("%.3f".format(lon))
                }
            }
            val resp = weatherApi.precipitationGrid(lats.joinToString(","), lons.joinToString(","))
            val times = resp.firstOrNull { !it.hourly?.time.isNullOrEmpty() }?.hourly?.time ?: emptyList()
            val cells = resp.map { p ->
                PrecipCell(
                    lat = p.latitude,
                    lon = p.longitude,
                    precip = p.hourly?.precipitation?.map { it ?: 0.0 } ?: emptyList()
                )
            }
            val grid = PrecipGrid(times, rows, cols, latStep, lonStep, cells)
            precipCache[key] = grid
            grid
        }

    data class RadarData(val host: String, val frames: List<RadarFrame>, val nowcastFrom: Int)

    suspend fun radarFrames(): RadarData = withContext(Dispatchers.IO) {
        val maps = rainViewerApi.maps()
        val past = maps.radar?.past ?: emptyList()
        val nowcast = maps.radar?.nowcast ?: emptyList()
        RadarData(
            host = maps.host,
            frames = past + nowcast,
            nowcastFrom = past.size
        )
    }

    private fun mapForecast(dto: ForecastResponse, fetchedAt: Long): Forecast {
        val c = dto.current
        val daily = dto.daily
        val isDayNow = (c?.isDay ?: 1) == 1
        val current = CurrentWeather(
            temperatureC = c?.temperature ?: 0.0,
            apparentTemperatureC = c?.apparentTemperature ?: (c?.temperature ?: 0.0),
            condition = WeatherCode.map(c?.weatherCode ?: 3, isDayNow),
            windSpeedKmh = c?.windSpeed ?: 0.0,
            windDirectionDeg = c?.windDirection ?: 0,
            humidityPct = c?.humidity ?: 0,
            pressureHpa = c?.pressure ?: 0.0,
            isDay = isDayNow,
            precipitationMm = c?.precipitation ?: 0.0,
            uvIndex = daily?.uvIndexMax?.firstOrNull() ?: 0.0,
            sunriseEpoch = daily?.sunrise?.firstOrNull(),
            sunsetEpoch = daily?.sunset?.firstOrNull()
        )

        val minutely = buildList {
            val m = dto.minutely15 ?: return@buildList
            for (i in m.time.indices) {
                add(MinutePoint(m.time[i], m.precipitation.getOrNull(i) ?: 0.0))
            }
        }

        val hourly = buildList {
            val h = dto.hourly ?: return@buildList
            val n = h.time.size
            for (i in 0 until n) {
                val isDay = (h.isDay.getOrNull(i) ?: 1) == 1
                add(
                    HourPoint(
                        epochSeconds = h.time[i],
                        temperatureC = h.temperature.getOrNull(i) ?: 0.0,
                        condition = WeatherCode.map(h.weatherCode.getOrNull(i) ?: 3, isDay),
                        precipitationProbabilityPct = h.precipProbability.getOrNull(i) ?: 0,
                        precipitationMm = h.precipitation.getOrNull(i) ?: 0.0,
                        humidityPct = h.humidity.getOrNull(i) ?: 0,
                        windSpeedKmh = h.windSpeed.getOrNull(i) ?: 0.0,
                        isDay = isDay
                    )
                )
            }
        }

        val days = buildList {
            val d = dto.daily ?: return@buildList
            val n = d.time.size
            for (i in 0 until n) {
                add(
                    DayPoint(
                        epochSeconds = d.time[i],
                        condition = WeatherCode.map(d.weatherCode.getOrNull(i) ?: 3, true),
                        tempMaxC = d.tempMax.getOrNull(i) ?: 0.0,
                        tempMinC = d.tempMin.getOrNull(i) ?: 0.0,
                        precipitationProbabilityPct = d.precipProbabilityMax.getOrNull(i) ?: 0,
                        precipitationSumMm = d.precipitationSum.getOrNull(i) ?: 0.0,
                        windMaxKmh = d.windMax.getOrNull(i) ?: 0.0,
                        sunriseEpoch = d.sunrise.getOrNull(i),
                        sunsetEpoch = d.sunset.getOrNull(i),
                        uvIndexMax = d.uvIndexMax.getOrNull(i) ?: 0.0
                    )
                )
            }
        }

        return Forecast(
            current = current,
            minutely = minutely,
            hourly = hourly,
            daily = days,
            timezone = dto.timezone ?: "auto",
            utcOffsetSeconds = dto.utcOffsetSeconds ?: 0L,
            fetchedAtMillis = fetchedAt
        )
    }
}
