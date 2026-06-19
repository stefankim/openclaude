package com.openclaude.weather.data.repository

import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.data.remote.GeoResult
import com.openclaude.weather.data.remote.Network
import com.openclaude.weather.data.remote.RadarFrame
import com.openclaude.weather.domain.CurrentWeather
import com.openclaude.weather.domain.DayPoint
import com.openclaude.weather.domain.Forecast
import com.openclaude.weather.domain.HourPoint
import com.openclaude.weather.util.WeatherCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coordinates remote weather/geocoding/radar sources and maps them to domain models. */
class WeatherRepository {

    private val weatherApi = Network.weatherApi
    private val geocodingApi = Network.geocodingApi
    private val rainViewerApi = Network.rainViewerApi

    private val cache = mutableMapOf<String, Forecast>()
    private val cacheTtlMillis = 10 * 60 * 1000L

    suspend fun forecast(location: SavedLocation, forceRefresh: Boolean = false): Forecast =
        withContext(Dispatchers.IO) {
            val cached = cache[location.id]
            if (!forceRefresh && cached != null &&
                System.currentTimeMillis() - cached.fetchedAtMillis < cacheTtlMillis
            ) {
                return@withContext cached
            }
            val dto = weatherApi.forecast(location.latitude, location.longitude)
            val mapped = mapForecast(dto)
            cache[location.id] = mapped
            mapped
        }

    suspend fun search(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList()
        else geocodingApi.search(query.trim()).results ?: emptyList()
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

    private fun mapForecast(dto: com.openclaude.weather.data.remote.ForecastResponse): Forecast {
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
            hourly = hourly,
            daily = days,
            timezone = dto.timezone ?: "auto",
            fetchedAtMillis = System.currentTimeMillis()
        )
    }
}
