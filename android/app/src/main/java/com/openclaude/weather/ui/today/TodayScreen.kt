package com.openclaude.weather.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaude.weather.R
import com.openclaude.weather.data.local.SavedLocation
import com.openclaude.weather.domain.AirQuality
import com.openclaude.weather.domain.Forecast
import com.openclaude.weather.domain.HourPoint
import com.openclaude.weather.domain.WeatherAlert
import com.openclaude.weather.ui.components.DetailItem
import com.openclaude.weather.ui.components.GlassCard
import com.openclaude.weather.ui.components.SectionTitle
import com.openclaude.weather.ui.components.WeatherGlyph
import com.openclaude.weather.ui.components.WeatherSceneView
import com.openclaude.weather.util.Format
import com.openclaude.weather.util.Units
import com.openclaude.weather.util.WeatherCode

/** Soft drop shadow so white text reads cleanly over the animated sky. */
private val shadowed = TextStyle(
    shadow = Shadow(color = Color(0xB3000000), offset = Offset(0f, 2f), blurRadius = 6f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    location: SavedLocation,
    forecast: Forecast,
    alerts: List<WeatherAlert>,
    air: AirQuality?,
    animIntensity: Float
) {
    val current = forecast.current
    val tzOffset = forecast.utcOffsetSeconds
    var sheetHour by remember { mutableStateOf<HourPoint?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ---- Official weather warnings ----
        alerts.forEach { alert -> AlertBanner(alert) }

        // ---- Hero: animated weather sky with a dark scrim so the text stays readable. ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            WeatherSceneView(
                scene = current.condition.scene,
                isDay = current.isDay,
                modifier = Modifier.matchParentSize(),
                windKmh = current.windSpeedKmh,
                intensity = animIntensity
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x22000000), Color(0x40000000), Color(0x80000000))
                        )
                    )
            )
            Column(modifier = Modifier.matchParentSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        location.displayName,
                        color = Color.White, fontSize = 15.sp,
                        fontWeight = FontWeight.Medium, style = shadowed
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.updated_at, Format.clock(forecast.fetchedAtMillis)),
                        color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, style = shadowed
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${Math.round(Units.convertTemp(current.temperatureC))}°",
                    color = Color.White, fontSize = 72.sp,
                    fontWeight = FontWeight.Bold, style = shadowed
                )
                Text(
                    stringResource(current.condition.labelRes),
                    color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Medium, style = shadowed
                )
                Text(
                    stringResource(R.string.feels_like, Format.tempPrecise(current.apparentTemperatureC)),
                    color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, style = shadowed
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Rain in the next ~2 h (15-min data) ----
        RainSoonCard(forecast, tzOffset)

        Spacer(Modifier.height(12.dp))

        // ---- Quick stats ----
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickStat(
                    Icons.Filled.Air, stringResource(R.string.wind),
                    "${Format.wind(current.windSpeedKmh)} ${WeatherCode.windDirectionLabel(current.windDirectionDeg)}"
                )
                QuickStat(Icons.Filled.WaterDrop, stringResource(R.string.humidity), Format.percent(current.humidityPct))
                QuickStat(
                    Icons.Filled.WbSunny, stringResource(R.string.uv),
                    if (current.uvIndex >= 0.5) "${Math.round(current.uvIndex)}" else "—"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- "Today" divider with sunrise/sunset ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.today), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            SunTime(current.sunriseEpoch, tzOffset, rising = true)
            Spacer(Modifier.width(14.dp))
            SunTime(current.sunsetEpoch, tzOffset, rising = false)
        }

        Spacer(Modifier.height(8.dp))

        // ---- Hourly forecast (tap an hour for details) ----
        val nowSec = System.currentTimeMillis() / 1000
        val upcoming = forecast.hourly.filter { it.epochSeconds >= nowSec - 3600 }.take(24)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(upcoming) { hour -> HourCell(hour, tzOffset) { sheetHour = hour } }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Temperature chart ----
        SectionTitle(stringResource(R.string.temperature_24h))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TempChart(upcoming, tzOffset, modifier = Modifier.fillMaxWidth().height(120.dp))
        }

        Spacer(Modifier.height(16.dp))

        // ---- Air quality + pollen ----
        air?.let { aq ->
            if (aq.europeanAqi != null) {
                SectionTitle(stringResource(R.string.air_quality))
                AirQualityCard(aq)
                Spacer(Modifier.height(16.dp))
            }
        }

        // ---- Details ----
        SectionTitle(stringResource(R.string.details))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(
                        Icons.Filled.Compress, stringResource(R.string.pressure),
                        "${Math.round(current.pressureHpa)} hPa", Modifier.weight(1f)
                    )
                    DetailItem(
                        Icons.Filled.Grain, stringResource(R.string.precipitation),
                        Format.mm(current.precipitationMm), Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(
                        Icons.Filled.WbTwilight, stringResource(R.string.sunrise),
                        current.sunriseEpoch?.let { Format.hour(it, tzOffset) } ?: "—", Modifier.weight(1f)
                    )
                    DetailItem(
                        Icons.Filled.WbTwilight, stringResource(R.string.sunset),
                        current.sunsetEpoch?.let { Format.hour(it, tzOffset) } ?: "—", Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ---- Hour detail sheet ----
    sheetHour?.let { hour ->
        ModalBottomSheet(onDismissRequest = { sheetHour = null }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherGlyph(hour.condition.scene, hour.isDay, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Format.hour(hour.epochSeconds, tzOffset),
                            fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                        Text(stringResource(hour.condition.labelRes), fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(Format.tempPrecise(hour.temperatureC), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                SheetRow(Icons.Filled.Air, stringResource(R.string.wind), Format.wind(hour.windSpeedKmh))
                SheetRow(Icons.Filled.WaterDrop, stringResource(R.string.humidity), Format.percent(hour.humidityPct))
                SheetRow(Icons.Filled.Umbrella, stringResource(R.string.precipitation),
                    "${Format.mm(hour.precipitationMm)} · ${hour.precipitationProbabilityPct}%")
            }
        }
    }
}

@Composable
private fun AlertBanner(alert: WeatherAlert) {
    val bg = when (alert.severity) {
        "Extreme" -> Color(0xE6C62828)
        "Severe" -> Color(0xE6E65100)
        else -> Color(0xE6F9A825)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(alert.event, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val until = alert.expiresMillis?.let {
                " · " + stringResource(R.string.warning_until, Format.clock(it))
            } ?: ""
            Text(
                alert.severity + until,
                color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RainSoonCard(forecast: Forecast, tzOffset: Long) {
    val nowSec = System.currentTimeMillis() / 1000
    val upcoming = forecast.minutely.filter { it.epochSeconds >= nowSec - 900 }
    if (upcoming.isEmpty()) return
    val firstRain = upcoming.firstOrNull { it.precipitationMm > 0.05 }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Umbrella, null,
                tint = if (firstRain != null) Color(0xFF9FD0FF) else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (firstRain != null)
                    stringResource(R.string.rain_starting, Format.hour(firstRain.epochSeconds, tzOffset))
                else stringResource(R.string.no_rain_soon),
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AirQualityCard(aq: AirQuality) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val aqi = aq.europeanAqi ?: 0
                val (label, color) = when {
                    aqi < 20 -> R.string.aqi_good to Color(0xFF50CCAA)
                    aqi < 40 -> R.string.aqi_fair to Color(0xFF50CCAA)
                    aqi < 60 -> R.string.aqi_moderate to Color(0xFFF0E641)
                    aqi < 80 -> R.string.aqi_poor to Color(0xFFFF8C2B)
                    aqi < 100 -> R.string.aqi_very_poor to Color(0xFFE53935)
                    else -> R.string.aqi_extreme to Color(0xFF8E24AA)
                }
                Box(
                    modifier = Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${stringResource(label)} · AQI $aqi",
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                aq.pm25?.let { Meta("PM2.5", "${Math.round(it)}") }
                aq.pm10?.let { Meta("PM10", "${Math.round(it)}") }
                aq.grassPollen?.takeIf { it > 0 }?.let { Meta(stringResource(R.string.pollen_grass), "${Math.round(it)}") }
                aq.birchPollen?.takeIf { it > 0 }?.let { Meta(stringResource(R.string.pollen_birch), "${Math.round(it)}") }
                aq.alderPollen?.takeIf { it > 0 }?.let { Meta(stringResource(R.string.pollen_alder), "${Math.round(it)}") }
            }
        }
    }
}

@Composable
private fun Meta(label: String, value: String) {
    Column {
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun TempChart(hours: List<HourPoint>, tzOffset: Long, modifier: Modifier = Modifier) {
    if (hours.size < 2) return
    val temps = hours.map { Units.convertTemp(it.temperatureC) }
    val minT = temps.min()
    val maxT = temps.max()
    Column {
        Canvas(modifier = modifier) {
            val span = (maxT - minT).coerceAtLeast(1.0)
            val stepX = size.width / (temps.size - 1)
            val pad = size.height * 0.15f
            fun y(t: Double) = pad + ((maxT - t) / span).toFloat() * (size.height - 2 * pad)

            val line = Path()
            temps.forEachIndexed { i, t ->
                if (i == 0) line.moveTo(0f, y(t)) else line.lineTo(i * stepX, y(t))
            }
            // Gradient fill under the line.
            val fill = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(listOf(Color(0x66FFD54A), Color(0x00FFD54A)))
            )
            drawPath(
                line, Color(0xFFFFD54A),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, hours.size / 2, hours.lastIndex).forEach { i ->
                Text(
                    "${Format.hour(hours[i].epochSeconds, tzOffset)} · ${Math.round(temps[i])}°",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun QuickStat(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun SunTime(epoch: Long?, tzOffset: Long, rising: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.WbTwilight,
            contentDescription = if (rising) stringResource(R.string.sunrise) else stringResource(R.string.sunset),
            tint = Color(0xFFFFD54A),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "${if (rising) "↑" else "↓"} ${epoch?.let { Format.hour(it, tzOffset) } ?: "—"}",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun HourCell(hour: HourPoint, tzOffset: Long, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .width(58.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(Format.hour(hour.epochSeconds, tzOffset), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        WeatherGlyph(hour.condition.scene, hour.isDay, modifier = Modifier.size(32.dp))
        Text(Format.temp(hour.temperatureC), color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(
            if (hour.precipitationProbabilityPct > 0) "${hour.precipitationProbabilityPct}%" else " ",
            color = Color(0xFF9FD0FF),
            fontSize = 11.sp
        )
    }
}
